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
	String scheduleMode,
	String pendingRestore
) {
	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, true, 30, true, 30, BackupTranslations.DEFAULT_LANGUAGE, "active", "");
	}

	public BackupSettings(boolean scheduleEnabled, long intervalMinutes, int backupsToKeep, String backupDirectory, String pendingRestore) {
		this(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, true, true, true, 30, true, 30, BackupTranslations.DEFAULT_LANGUAGE, "active", pendingRestore);
	}

	public static BackupSettings defaults() {
		return new BackupSettings(false, 60, 10, "backups", true, true, true, 30, true, 30, BackupTranslations.DEFAULT_LANGUAGE, "active", "");
	}

	public BackupSettings withScheduleEnabled(boolean enabled) {
		return new BackupSettings(enabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withIntervalMinutes(long minutes) {
		return new BackupSettings(scheduleEnabled, minutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withBackupsToKeep(int count) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, count, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withStopAfterRestore(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, enabled, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withClearRequiresConfirm(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, enabled, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withScheduleWarningEnabled(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, enabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withScheduleWarningSeconds(long seconds) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, seconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withRestoreDelayEnabled(boolean enabled) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, enabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withRestoreDelaySeconds(long seconds) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, seconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withLanguage(String language) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withScheduleMode(String scheduleMode) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, pendingRestore);
	}

	public BackupSettings withPendingRestore(String backupName) {
		return new BackupSettings(scheduleEnabled, intervalMinutes, backupsToKeep, backupDirectory, stopAfterRestore, clearRequiresConfirm, scheduleWarningEnabled, scheduleWarningSeconds, restoreDelayEnabled, restoreDelaySeconds, language, scheduleMode, backupName);
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

	public boolean isActiveScheduleMode() {
		return "active".equals(scheduleMode);
	}

	public boolean isRealtimeScheduleMode() {
		return "realtime".equals(scheduleMode);
	}
}
