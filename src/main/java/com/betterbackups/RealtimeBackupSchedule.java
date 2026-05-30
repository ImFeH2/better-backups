package com.betterbackups;

public final class RealtimeBackupSchedule {
	private long nextRunMillis = Long.MAX_VALUE;
	private boolean warned;

	public void reset(long nowMillis, long intervalMinutes) {
		nextRunMillis = nowMillis + intervalMinutes * 60_000;
		warned = false;
	}

	public void disable() {
		nextRunMillis = Long.MAX_VALUE;
		warned = false;
	}

	public Event poll(long nowMillis, boolean warningEnabled, long warningSeconds) {
		if (nextRunMillis == Long.MAX_VALUE) {
			return Event.none();
		}
		long remainingMillis = nextRunMillis - nowMillis;
		if (remainingMillis <= 0) {
			long previousRunMillis = nextRunMillis;
			nextRunMillis = Long.MAX_VALUE;
			warned = false;
			return new Event(EventType.RUN, 0, previousRunMillis);
		}
		if (warningEnabled && !warned && remainingMillis <= warningSeconds * 1000) {
			warned = true;
			return new Event(EventType.WARN, Math.max(1, (remainingMillis + 999) / 1000), nextRunMillis);
		}
		return Event.none();
	}

	public void rescheduleAfterRun(long previousRunMillis, long intervalMinutes) {
		nextRunMillis = previousRunMillis + intervalMinutes * 60_000;
		warned = false;
	}

	public enum EventType {
		NONE,
		WARN,
		RUN
	}

	public record Event(EventType type, long secondsUntilRun, long scheduledRunMillis) {
		public Event(EventType type, long secondsUntilRun) {
			this(type, secondsUntilRun, Long.MAX_VALUE);
		}

		public static Event none() {
			return new Event(EventType.NONE, 0, Long.MAX_VALUE);
		}
	}
}
