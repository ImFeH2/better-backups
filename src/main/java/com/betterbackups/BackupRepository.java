package com.betterbackups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class BackupRepository {
	private final Path backupDirectory;

	public BackupRepository(Path backupDirectory) {
		this.backupDirectory = backupDirectory;
	}

	public List<BackupEntry> listBackups() throws IOException {
		if (!Files.exists(backupDirectory)) {
			return List.of();
		}

		try (Stream<Path> paths = Files.list(backupDirectory)) {
			return paths
				.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip"))
				.map(this::toEntry)
				.sorted(Comparator.comparing(BackupEntry::name).reversed())
				.toList();
		}
	}

	public BackupEntry requireBackup(String name) throws IOException {
		return listBackups().stream()
			.filter(entry -> entry.name().equals(name))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Backup not found: " + name));
	}

	public void keepNewest(int count) throws IOException {
		List<BackupEntry> entries = listBackups();
		for (int index = count; index < entries.size(); index++) {
			Files.deleteIfExists(entries.get(index).path());
		}
	}

	public int clearBackups() throws IOException {
		List<BackupEntry> entries = listBackups();
		for (BackupEntry entry : entries) {
			Files.deleteIfExists(entry.path());
		}
		return entries.size();
	}

	private BackupEntry toEntry(Path path) {
		try {
			String fileName = path.getFileName().toString();
			String name = fileName.substring(0, fileName.length() - ".zip".length());
			return new BackupEntry(name, path, Files.size(path), Files.getLastModifiedTime(path).toInstant());
		} catch (IOException exception) {
			throw new IllegalStateException("Could not read backup: " + path, exception);
		}
	}
}
