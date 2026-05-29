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
	Boolean restoreDelayEnabled,
	long restoreDelaySeconds,
	String language,
	String pendingRestore
) {
	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, true, 30, true, 30, BackupTranslations.DEFAULT_LANGUAGE, "");
	}

	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory, String pendingRestore) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, true, 30, true, 30, BackupTranslations.DEFAULT_LANGUAGE, pendingRestore);
	}

	public static BackupSettings defaults() {
		return new BackupSettings(false, 60, 10, "backups", true, true, true, 30, true, 30, BackupTranslations.DEFAULT_LANGUAGE, "");
	}

	public BackupSettings withScheduleEnabled(boolean enabled) {
		return new BackupSettings(enabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withIntervalMinutes(long minutes) {
		return new BackupSettings(scheduleEnabled, minutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withBackupsToKeep(int count) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, count, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withStopAfterRestore(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, enabled, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withClearRequiresConfirm(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, enabled, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withScheduleWarningEnabled(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, enabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withScheduleWarningSeconds(long seconds) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, seconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withRestoreDelayEnabled(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, enabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withRestoreDelaySeconds(long seconds) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, seconds, language, pendingRestore);
	}

	public BackupSettings withLanguage(String language) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, pendingRestore);
	}

	public BackupSettings withPendingRestore(String backupName) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, backupName);
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

	public boolean shouldDelayRestore() {
		return restoreDelayEnabled == null || restoreDelayEnabled;
	}
}
