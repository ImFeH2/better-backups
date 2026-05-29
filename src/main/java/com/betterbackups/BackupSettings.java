package com.betterbackups;

public record BackupSettings(
	boolean scheduleEnabled,
	long intervalMinutes,
	int backupsToKeep,
	String backupDirectory,
	Boolean stopAfterRestore,
	Boolean clearRequiresConfirm,
	Boolean scheduleWarningEnabled,
	long scheduleWarningSeconds,
	String pendingRestore
) {
	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, true, 30, "");
	}

	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory, String pendingRestore) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, true, 30, pendingRestore);
	}

	public static BackupSettings defaults() {
		return new BackupSettings(false, 60, 10, "backups", true, true, true, 30, "");
	}

	public BackupSettings withScheduleEnabled(boolean enabled) {
		return new BackupSettings(enabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, pendingRestore);
	}

	public BackupSettings withIntervalMinutes(long minutes) {
		return new BackupSettings(scheduleEnabled, minutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, pendingRestore);
	}

	public BackupSettings withBackupsToKeep(int count) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, count, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, pendingRestore);
	}

	public BackupSettings withStopAfterRestore(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, enabled, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, pendingRestore);
	}

	public BackupSettings withClearRequiresConfirm(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, enabled, scheduleWarningEnabled, scheduleWarningSeconds, pendingRestore);
	}

	public BackupSettings withScheduleWarningEnabled(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, enabled, scheduleWarningSeconds, pendingRestore);
	}

	public BackupSettings withScheduleWarningSeconds(long seconds) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, seconds, pendingRestore);
	}

	public BackupSettings withPendingRestore(String backupName) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, backupName);
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

	public boolean shouldWarnBeforeScheduledBackup() {
		return scheduleWarningEnabled == null || scheduleWarningEnabled;
	}
}
