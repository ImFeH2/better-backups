package com.betterbackups;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class BackupArchiveService {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Clock clock;

	public BackupArchiveService(Clock clock) {
		this.clock = clock;
	}

	public BackupEntry createBackup(Path worldDirectory, Path backupDirectory, String minecraftVersion, String modVersion) throws IOException {
		Files.createDirectories(backupDirectory);
		String name = nextAvailableName(backupDirectory);
		Path temporaryArchive = backupDirectory.resolve(name + ".zip.tmp");
		Path archive = backupDirectory.resolve(name + ".zip");
		Instant createdAt = clock.instant();

		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporaryArchive))) {
			writeManifest(zip, new BackupManifest(name, createdAt.toString(), worldDirectory.getFileName().toString(), minecraftVersion, modVersion));
			writeWorldFiles(zip, worldDirectory);
		}

		Files.move(temporaryArchive, archive, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		return new BackupEntry(name, archive, Files.size(archive), Files.getLastModifiedTime(archive).toInstant());
	}

	public void restore(Path archive, Path targetDirectory) throws IOException {
		targetDirectory = targetDirectory.toAbsolutePath().normalize();
		if (Files.exists(targetDirectory)) {
			if (!Files.isDirectory(targetDirectory)) {
				throw new IOException("Restore target is not a directory: " + targetDirectory);
			}
			try (var children = Files.list(targetDirectory)) {
				if (children.findAny().isPresent()) {
					throw new IOException("Restore target must be empty: " + targetDirectory);
				}
			}
		}

		Files.createDirectories(targetDirectory);
		try (ZipFile zip = new ZipFile(archive.toFile())) {
			var entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || entry.getName().equals("manifest.json")) {
					continue;
				}

				Path output = targetDirectory.resolve(entry.getName()).normalize();
				if (!output.startsWith(targetDirectory)) {
					throw new IOException("Backup contains an unsafe path: " + entry.getName());
				}

				Files.createDirectories(output.getParent());
				try (var input = zip.getInputStream(entry)) {
					Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private String nextAvailableName(Path backupDirectory) {
		String baseName = BackupTimeNames.now(clock);
		String name = baseName;
		int suffix = 2;
		while (Files.exists(backupDirectory.resolve(name + ".zip")) || Files.exists(backupDirectory.resolve(name + ".zip.tmp"))) {
			name = baseName + "-" + suffix;
			suffix++;
		}
		return name;
	}

	private void writeManifest(ZipOutputStream zip, BackupManifest manifest) throws IOException {
		zip.putNextEntry(new ZipEntry("manifest.json"));
		zip.write(GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private void writeWorldFiles(ZipOutputStream zip, Path worldDirectory) throws IOException {
		try (var paths = Files.walk(worldDirectory)) {
			var iterator = paths.iterator();
			while (iterator.hasNext()) {
				Path path = iterator.next();
				if (Files.isDirectory(path) || shouldSkip(path)) {
					continue;
				}

				Path relative = worldDirectory.relativize(path);
				zip.putNextEntry(new ZipEntry(relative.toString().replace('\\', '/')));
				Files.copy(path, zip);
				zip.closeEntry();
			}
		}
	}

	private boolean shouldSkip(Path path) {
		String fileName = path.getFileName().toString();
		return fileName.equals("session.lock");
	}

	private record BackupManifest(String name, String createdAt, String worldName, String minecraftVersion, String modVersion) {
	}
}
