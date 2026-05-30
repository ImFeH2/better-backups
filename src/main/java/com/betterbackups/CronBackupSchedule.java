package com.betterbackups;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public final class CronBackupSchedule {
	public static final String DEFAULT_EXPRESSION = "0 4 * * *";
	private static final CronParser PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

	private ExecutionTime executionTime;
	private long nextRunMillis = Long.MAX_VALUE;
	private boolean warningSent;

	public void reset(String expression, long nowMillis, ZoneId zoneId) {
		executionTime = parse(normalizeExpression(expression));
		nextRunMillis = nextRunAfter(nowMillis, zoneId);
		warningSent = false;
	}

	public void disable() {
		executionTime = null;
		nextRunMillis = Long.MAX_VALUE;
		warningSent = false;
	}

	public Event poll(long nowMillis, boolean warningEnabled, long warningSeconds) {
		if (executionTime == null || nextRunMillis == Long.MAX_VALUE) {
			return Event.none();
		}
		if (warningEnabled && !warningSent) {
			long millisUntilRun = nextRunMillis - nowMillis;
			if (millisUntilRun > 0 && millisUntilRun <= warningSeconds * 1000) {
				warningSent = true;
				return new Event(EventType.WARN, Math.max(1, (millisUntilRun + 999) / 1000), nextRunMillis);
			}
		}
		if (nowMillis >= nextRunMillis) {
			return new Event(EventType.RUN, 0, nextRunMillis);
		}
		return Event.none();
	}

	public void rescheduleAfterRun(long scheduledRunMillis, ZoneId zoneId) {
		if (executionTime == null) {
			disable();
			return;
		}
		nextRunMillis = nextRunAfter(scheduledRunMillis, zoneId);
		warningSent = false;
	}

	public void skipMissedRuns(long nowMillis, ZoneId zoneId, long graceMillis) {
		if (executionTime == null || nextRunMillis == Long.MAX_VALUE || nowMillis - nextRunMillis <= graceMillis) {
			return;
		}
		nextRunMillis = nextRunAfter(nowMillis, zoneId);
		warningSent = false;
	}

	public static String normalizeExpression(String expression) {
		if (expression == null || expression.isBlank()) {
			return DEFAULT_EXPRESSION;
		}
		String normalized = expression.trim().replaceAll("\\s+", " ");
		parse(normalized);
		return normalized;
	}

	private static ExecutionTime parse(String expression) {
		Cron cron = PARSER.parse(expression);
		cron.validate();
		return ExecutionTime.forCron(cron);
	}

	private long nextRunAfter(long millis, ZoneId zoneId) {
		ZonedDateTime dateTime = Instant.ofEpochMilli(millis).atZone(zoneId);
		Optional<ZonedDateTime> next = executionTime.nextExecution(dateTime);
		return next.map(value -> value.toInstant().toEpochMilli()).orElse(Long.MAX_VALUE);
	}

	public record Event(EventType type, long secondsUntilRun, long scheduledRunMillis) {
		private static final Event NONE = new Event(EventType.NONE, 0, Long.MAX_VALUE);

		public static Event none() {
			return NONE;
		}
	}

	public enum EventType {
		NONE,
		WARN,
		RUN
	}
}
