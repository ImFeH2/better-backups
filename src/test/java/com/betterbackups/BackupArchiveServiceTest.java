package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BackupArchiveServiceTest {
	@TempDir
	Path tempDir;

	@Test
	void createsTimestampedBackupArchiveWithManifest() throws Exception {
		Path world = tempDir.resolve("world");
		Files.createDirectories(world.resolve("region"));
		Files.writeString(world.resolve("level.dat"), "level");
		Files.writeString(world.resolve("region").resolve("r.0.0.mca"), "region");
		Files.writeString(world.resolve("session.lock"), "lock");
		BackupArchiveService service = new BackupArchiveService(Clock.fixed(Instant.parse("2026-05-29T13:34:08Z"), ZoneOffset.UTC));

		BackupEntry entry = service.createBackup(world, tempDir.resolve("backups"), "26.1.2", "1.0.0");

		assertEquals("2026-05-29_13-34-08", entry.name());
		assertTrue(Files.exists(entry.path()));
		try (ZipFile zip = new ZipFile(entry.path().toFile())) {
			assertTrue(zip.getEntry("manifest.json") != null);
			assertTrue(zip.getEntry("level.dat") != null);
			assertTrue(zip.getEntry("region/r.0.0.mca") != null);
			assertFalse(zip.getEntry("session.lock") != null);
		}
	}

	@Test
	void restoresBackupIntoEmptyWorldDirectory() throws Exception {
		Path world = tempDir.resolve("world");
		Files.createDirectories(world);
		Files.writeString(world.resolve("level.dat"), "level");
		BackupArchiveService service = new BackupArchiveService(Clock.fixed(Instant.parse("2026-05-29T13:34:08Z"), ZoneOffset.UTC));
		BackupEntry entry = service.createBackup(world, tempDir.resolve("backups"), "26.1.2", "1.0.0");
		Path restored = tempDir.resolve("restored");

		service.restore(entry.path(), restored);

		assertEquals("level", Files.readString(restored.resolve("level.dat")));
	}

	@Test
	void keepsNewestBackupsOnly() throws Exception {
		Path backupDir = tempDir.resolve("backups");
		Files.createDirectories(backupDir);
		Files.writeString(backupDir.resolve("2026-05-29_10-00-00.zip"), "old");
		Files.writeString(backupDir.resolve("2026-05-29_11-00-00.zip"), "middle");
		Files.writeString(backupDir.resolve("2026-05-29_12-00-00.zip"), "new");
		BackupRepository repository = new BackupRepository(backupDir);

		repository.keepNewest(2);

		List<BackupEntry> entries = repository.listBackups();
		assertEquals(List.of("2026-05-29_12-00-00", "2026-05-29_11-00-00"), entries.stream().map(BackupEntry::name).toList());
		assertFalse(Files.exists(backupDir.resolve("2026-05-29_10-00-00.zip")));
	}
}
