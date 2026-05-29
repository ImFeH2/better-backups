package com.betterbackups;

public record BackupSettings(
	boolean scheduleEnabled,
	long intervalMinutes,
	int backupsToKeep,
	String backupDirectory,
	Boolean stopAfterRestore,
	Boolean clearRequiresConfirm,
	String pendingRestore
) {
	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, "");
	}

	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory, String pendingRestore) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, pendingRestore);
	}

	public static BackupSettings defaults() {
		return new BackupSettings(false, 60, 10, "backups", true, true, "");
	}

	public BackupSettings withScheduleEnabled(boolean enabled) {
		return new BackupSettings(enabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, pendingRestore);
	}

	public BackupSettings withIntervalMinutes(long minutes) {
		return new BackupSettings(scheduleEnabled, minutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, pendingRestore);
	}

	public BackupSettings withBackupsToKeep(int count) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, count, backupDirectory, stopAfterRestore, clearRequiresConfirm, pendingRestore);
	}

	public BackupSettings withStopAfterRestore(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, enabled, clearRequiresConfirm, pendingRestore);
	}

	public BackupSettings withClearRequiresConfirm(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, enabled, pendingRestore);
	}

	public BackupSettings withPendingRestore(String backupName) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, backupName);
	}

	public BackupSettings withoutPendingRestore() {
		return withPendingRestore("");
	}

	public boolean shouldStopAfterRestore() {
		return stopAfterRestore == null || stopAfterRestore;
	}

	public boolean shouldConfirmBeforeClear() {
		return clearRequiresConfirm == null || clearRequiresConfirm;
	}
}
