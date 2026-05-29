package com.betterbackups;

public final class ScheduledBackupWarningState {
	private boolean warned;

	public void reset() {
		warned = false;
	}

	public boolean shouldWarn(long ticksUntilBackup, long warningSeconds) {
		if (warned || warningSeconds <= 0) {
			return false;
		}
		if (ticksUntilBackup <= warningSeconds * 20) {
			warned = true;
			return true;
		}
		return false;
	}
}
