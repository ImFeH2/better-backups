package com.betterbackups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Comparator;

public final class PendingRestoreService {
	private final BackupArchiveService archiveService;
	private final Clock clock;

	public PendingRestoreService(BackupArchiveService archiveService, Clock clock) {
		this.archiveService = archiveService;
		this.clock = clock;
	}

	public boolean applyPendingRestore(BackupSettingsStore settingsStore, Path worldDirectory, Path backupDirectory) throws IOException {
		BackupSettings settings = settingsStore.load();
		if (settings.pendingRestore().isBlank()) {
			return false;
		}

		worldDirectory = worldDirectory.toAbsolutePath().normalize();
		String backupName = settings.pendingRestore();
		BetterBackupsMod.LOGGER.info("Restoring backup {} into {}", backupName, worldDirectory);
		BackupEntry backup = new BackupRepository(backupDirectory).requireBackup(backupName);
		Path previousWorld = worldDirectory.resolveSibling(worldDirectory.getFileName() + ".before-restore-" + BackupTimeNames.now(clock));
		if (Files.exists(worldDirectory)) {
			Files.move(worldDirectory, previousWorld, StandardCopyOption.ATOMIC_MOVE);
		}

		try {
			archiveService.restore(backup.path(), worldDirectory);
			settingsStore.save(settings.withoutPendingRestore());
			BetterBackupsMod.LOGGER.info("Restore completed: {}", backupName);
			return true;
		} catch (IOException exception) {
			BetterBackupsMod.LOGGER.error("Restore failed, rolling back current world", exception);
			if (Files.exists(worldDirectory)) {
				deleteRecursively(worldDirectory);
			}
			if (Files.exists(previousWorld)) {
				Files.move(previousWorld, worldDirectory, StandardCopyOption.ATOMIC_MOVE);
			}
			throw exception;
		}
	}

	private void deleteRecursively(Path directory) throws IOException {
		try (var paths = Files.walk(directory)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}
}
