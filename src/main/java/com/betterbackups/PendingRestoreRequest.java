package com.betterbackups;

public final class PendingRestoreRequest {
	private final String backupName;
	private final boolean stopAfterRestore;
	private long ticksRemaining;
	private boolean cancelled;
	private boolean completed;

	public PendingRestoreRequest(String backupName, boolean stopAfterRestore, long ticksRemaining) {
		this.backupName = backupName;
		this.stopAfterRestore = stopAfterRestore;
		this.ticksRemaining = Math.max(0, ticksRemaining);
	}

	public String backupName() {
		return backupName;
	}

	public boolean shouldStopAfterRestore() {
		return stopAfterRestore;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		cancelled = true;
	}

	public boolean tick() {
		if (cancelled || completed) {
			return false;
		}
		if (ticksRemaining > 0) {
			ticksRemaining--;
		}
		if (ticksRemaining <= 0) {
			completed = true;
			return true;
		}
		return false;
	}
}
