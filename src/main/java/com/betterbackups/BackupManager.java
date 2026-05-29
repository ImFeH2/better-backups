package com.betterbackups;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

public final class BackupManager {
	private final BackupSettingsStore settingsStore;
	private final BackupArchiveService archiveService;
	private final PendingRestoreService pendingRestoreService;
	private final Path gameDirectory;
	private final String minecraftVersion;
	private final String modVersion;
	private final Logger logger;
	private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "better-backups-worker");
		thread.setDaemon(true);
		return thread;
	});
	private final AtomicBoolean backupRunning = new AtomicBoolean(false);
	private final AtomicBoolean restoreRunning = new AtomicBoolean(false);

	private volatile long ticksUntilScheduledBackup = Long.MAX_VALUE;
	private volatile BackupEntry latestBackup;

	public BackupManager(
		BackupSettingsStore settingsStore,
		Path gameDirectory,
		String minecraftVersion,
		String modVersion,
		Logger logger,
		Clock clock
	) {
		this.settingsStore = settingsStore;
		this.archiveService = new BackupArchiveService(clock);
		this.pendingRestoreService = new PendingRestoreService(archiveService, clock);
		this.gameDirectory = gameDirectory;
		this.minecraftVersion = minecraftVersion;
		this.modVersion = modVersion;
		this.logger = logger;
	}

	public CompletableFuture<BackupEntry> startBackup(MinecraftServer server) {
		if (!backupRunning.compareAndSet(false, true)) {
			return CompletableFuture.failedFuture(new BackupAlreadyRunningException());
		}

		Path worldDirectory = server.getWorldPath(LevelResource.ROOT);
		server.saveEverything(false, true, true);

		return CompletableFuture.supplyAsync(() -> {
			try {
				BackupSettings settings = settingsStore.load();
				Path backupDirectory = resolveBackupDirectory(settings);
				BackupEntry entry = archiveService.createBackup(worldDirectory, backupDirectory, minecraftVersion, modVersion);
				new BackupRepository(backupDirectory).keepNewest(settings.backupsToKeep());
				latestBackup = entry;
				return entry;
			} catch (IOException exception) {
				throw new BackupOperationException(exception);
			} finally {
				backupRunning.set(false);
			}
		}, executor);
	}

	public List<BackupEntry> listBackups() throws IOException {
		return new BackupRepository(resolveBackupDirectory(settingsStore.load())).listBackups();
	}

	public BackupEntry requireBackup(String name) throws IOException {
		return new BackupRepository(resolveBackupDirectory(settingsStore.load())).requireBackup(name);
	}

	public void applyPendingRestoreBeforeServerStart() throws IOException {
		BackupSettings settings = settingsStore.load();
		pendingRestoreService.applyPendingRestore(settingsStore, resolveWorldDirectory(), resolveBackupDirectory(settings));
	}

	public BackupSettings loadSettings() throws IOException {
		return settingsStore.load();
	}

	public void saveSettings(BackupSettings settings) throws IOException {
		settingsStore.save(settings);
	}

	public void setScheduleEnabled(boolean enabled) throws IOException {
		BackupSettings settings = settingsStore.load().withScheduleEnabled(enabled);
		settingsStore.save(settings);
		resetSchedule(settings);
	}

	public void setScheduleInterval(Duration interval) throws IOException {
		BackupSettings settings = settingsStore.load().withIntervalMinutes(interval.toMinutes());
		settingsStore.save(settings);
		resetSchedule(settings);
	}

	public void setBackupsToKeep(int count) throws IOException {
		BackupSettings settings = settingsStore.load().withBackupsToKeep(count);
		settingsStore.save(settings);
		new BackupRepository(resolveBackupDirectory(settings)).keepNewest(count);
	}

	public void setStopAfterRestore(boolean enabled) throws IOException {
		settingsStore.save(settingsStore.load().withStopAfterRestore(enabled));
	}

	public void setClearRequiresConfirm(boolean enabled) throws IOException {
		settingsStore.save(settingsStore.load().withClearRequiresConfirm(enabled));
	}

	public int clearBackups() throws IOException {
		requireCanClearBackups();
		return new BackupRepository(resolveBackupDirectory(settingsStore.load())).clearBackups();
	}

	public void requireCanClearBackups() {
		if (backupRunning.get()) {
			throw new BackupAlreadyRunningException();
		}
		if (restoreRunning.get()) {
			throw new RestoreAlreadyRunningException();
		}
	}

	public void setPendingRestore(String backupName) throws IOException {
		BackupSettings settings = settingsStore.load();
		requireBackup(backupName);
		settingsStore.save(settings.withPendingRestore(backupName));
	}

	public void restoreAfterServerStop(MinecraftServer server, String backupName) throws IOException {
		BackupSettings settings = settingsStore.load();
		requireBackup(backupName);
		settingsStore.save(settings.withPendingRestore(backupName));
		if (!restoreRunning.compareAndSet(false, true)) {
			throw new RestoreAlreadyRunningException();
		}

		Path worldDirectory = server.getWorldPath(LevelResource.ROOT);
		Path backupDirectory = resolveBackupDirectory(settings);
		Thread serverThread = server.getRunningThread();
		Thread restoreThread = new Thread(() -> {
			try {
				serverThread.join();
				pendingRestoreService.applyPendingRestore(settingsStore, worldDirectory, backupDirectory);
				logger.info("Restore completed: {}", backupName);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				logger.error("Restore interrupted", exception);
			} catch (Exception exception) {
				logger.error("Restore failed", exception);
			} finally {
				restoreRunning.set(false);
			}
		}, "better-backups-restore");
		restoreThread.setDaemon(false);
		restoreThread.start();
	}

	public void resetSchedule(BackupSettings settings) {
		if (settings.scheduleEnabled()) {
			ticksUntilScheduledBackup = minutesToTicks(settings.intervalMinutes());
		} else {
			ticksUntilScheduledBackup = Long.MAX_VALUE;
		}
	}

	public void tick(MinecraftServer server) {
		if (ticksUntilScheduledBackup == Long.MAX_VALUE) {
			return;
		}
		if (ticksUntilScheduledBackup > 0) {
			ticksUntilScheduledBackup--;
		}
		if (ticksUntilScheduledBackup > 0) {
			return;
		}

		try {
			BackupSettings settings = settingsStore.load();
			if (!settings.scheduleEnabled()) {
				ticksUntilScheduledBackup = Long.MAX_VALUE;
				return;
			}

			ticksUntilScheduledBackup = minutesToTicks(settings.intervalMinutes());
			startBackup(server).whenComplete((entry, throwable) -> {
				if (throwable == null) {
					logger.info("Scheduled backup completed: {}", entry.name());
				} else if (!(throwable instanceof BackupAlreadyRunningException)) {
					logger.error("Scheduled backup failed", unwrap(throwable));
				}
			});
		} catch (IOException exception) {
			logger.error("Could not read backup settings", exception);
			ticksUntilScheduledBackup = minutesToTicks(1);
		}
	}

	public boolean isBackupRunning() {
		return backupRunning.get();
	}

	public BackupEntry latestBackup() {
		return latestBackup;
	}

	public void shutdown() {
		executor.shutdownNow();
	}

	public Path resolveBackupDirectory(BackupSettings settings) {
		Path configured = Path.of(settings.backupDirectory());
		if (configured.isAbsolute()) {
			return configured;
		}
		return gameDirectory.resolve(configured).normalize();
	}

	private Path resolveWorldDirectory() throws IOException {
		Properties properties = new Properties();
		Path propertiesPath = gameDirectory.resolve("server.properties");
		if (Files.isRegularFile(propertiesPath)) {
			try (InputStream input = Files.newInputStream(propertiesPath)) {
				properties.load(input);
			}
		}

		String levelName = properties.getProperty("level-name", "world");
		if (levelName.isBlank()) {
			levelName = "world";
		}
		return gameDirectory.resolve(levelName).normalize();
	}

	private long minutesToTicks(long minutes) {
		return Duration.ofMinutes(minutes).toSeconds() * 20;
	}

	public static Throwable unwrap(Throwable throwable) {
		if (throwable instanceof BackupOperationException operationException) {
			return operationException.getCause();
		}
		if (throwable.getCause() instanceof BackupOperationException operationException) {
			return operationException.getCause();
		}
		return throwable;
	}

	public static final class BackupAlreadyRunningException extends RuntimeException {
	}

	public static final class RestoreAlreadyRunningException extends RuntimeException {
	}

	public static final class BackupOperationException extends RuntimeException {
		public BackupOperationException(Throwable cause) {
			super(cause);
		}
	}
}
