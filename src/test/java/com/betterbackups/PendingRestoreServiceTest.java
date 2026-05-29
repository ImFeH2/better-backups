package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PendingRestoreServiceTest {
	@TempDir
	Path tempDir;

	@Test
	void restoresWhenWorldPathEndsWithCurrentDirectorySegment() throws Exception {
		Clock clock = Clock.fixed(Instant.parse("2026-05-29T15:06:37Z"), ZoneOffset.UTC);
		BackupArchiveService archiveService = new BackupArchiveService(clock);
		Path sourceWorld = tempDir.resolve("source-world");
		Files.createDirectories(sourceWorld);
		Files.writeString(sourceWorld.resolve("level.dat"), "restored");
		BackupEntry backup = archiveService.createBackup(sourceWorld, tempDir.resolve("backups"), "26.1.2", "1.0.0");
		Path world = tempDir.resolve("world");
		Files.createDirectories(world);
		Files.writeString(world.resolve("level.dat"), "current");
		BackupSettingsStore settingsStore = new BackupSettingsStore(tempDir.resolve("better-backups.json"));
		settingsStore.save(BackupSettings.defaults().withPendingRestore(backup.name()));
		PendingRestoreService service = new PendingRestoreService(archiveService, clock);

		boolean restored = service.applyPendingRestore(settingsStore, world.resolve("."), tempDir.resolve("backups"));

		assertTrue(restored);
		assertEquals("restored", Files.readString(world.resolve("level.dat")));
		assertEquals("current", Files.readString(tempDir.resolve("world.before-restore-2026-05-29_15-06-37").resolve("level.dat")));
		assertTrue(settingsStore.load().pendingRestore().isBlank());
	}

	@Test
	void ignoresBlankPendingRestore() throws Exception {
		BackupSettingsStore settingsStore = new BackupSettingsStore(tempDir.resolve("better-backups.json"));
		settingsStore.save(BackupSettings.defaults());
		PendingRestoreService service = new PendingRestoreService(new BackupArchiveService(Clock.systemUTC()), Clock.systemUTC());

		boolean restored = service.applyPendingRestore(settingsStore, tempDir.resolve("world").resolve("."), tempDir.resolve("backups"));

		assertFalse(restored);
	}
}
