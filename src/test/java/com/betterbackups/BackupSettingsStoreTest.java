package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupSettingsStoreTest {
	@TempDir
	Path tempDir;

	@Test
	void createsDefaultSettingsWhenFileIsMissing() throws Exception {
		BackupSettingsStore store = new BackupSettingsStore(tempDir.resolve("better-backups.json"));

		BackupSettings settings = store.load();

		assertFalse(settings.scheduleEnabled());
		assertEquals(60, settings.intervalMinutes());
		assertEquals(10, settings.backupsToKeep());
		assertEquals("backups", settings.backupDirectory());
		assertTrue(settings.shouldStopAfterRestore());
		assertTrue(settings.shouldConfirmBeforeClear());
		assertTrue(tempDir.resolve("better-backups.json").toString().endsWith("better-backups.json"));
	}

	@Test
	void savesAndLoadsSettings() throws Exception {
		BackupSettingsStore store = new BackupSettingsStore(tempDir.resolve("better-backups.json"));
		BackupSettings settings = new BackupSettings(true, 120, 4, "server-backups");

		store.save(settings);

		assertEquals(settings, store.load());
	}

	@Test
	void missingStopAfterRestoreDefaultsToEnabled() throws Exception {
		Path config = tempDir.resolve("better-backups.json");
		Files.writeString(config, """
			{
			  "scheduleEnabled": false,
			  "intervalMinutes": 60,
			  "backupsToKeep": 10,
			  "backupDirectory": "backups",
			  "pendingRestore": ""
			}
			""");
		BackupSettingsStore store = new BackupSettingsStore(config);

		BackupSettings settings = store.load();

		assertTrue(settings.shouldStopAfterRestore());
	}

	@Test
	void missingClearConfirmationDefaultsToEnabled() throws Exception {
		Path config = tempDir.resolve("better-backups.json");
		Files.writeString(config, """
			{
			  "scheduleEnabled": false,
			  "intervalMinutes": 60,
			  "backupsToKeep": 10,
			  "backupDirectory": "backups",
			  "stopAfterRestore": true,
			  "pendingRestore": ""
			}
			""");
		BackupSettingsStore store = new BackupSettingsStore(config);

		BackupSettings settings = store.load();

		assertTrue(settings.shouldConfirmBeforeClear());
	}
}
