package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CronBackupScheduleTest {
	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

	@Test
	void warnsOnceThenRunsAtNextCronTime() {
		CronBackupSchedule schedule = new CronBackupSchedule();
		schedule.reset("0 4 * * *", millis("2026-05-30T19:59:00Z"), ZONE);

		assertEquals(CronBackupSchedule.Event.none(), schedule.poll(millis("2026-05-30T19:59:20Z"), true, 30));
		assertEquals(new CronBackupSchedule.Event(CronBackupSchedule.EventType.WARN, 30, millis("2026-05-30T20:00:00Z")), schedule.poll(millis("2026-05-30T19:59:30Z"), true, 30));
		assertEquals(CronBackupSchedule.Event.none(), schedule.poll(millis("2026-05-30T19:59:40Z"), true, 30));
		assertEquals(new CronBackupSchedule.Event(CronBackupSchedule.EventType.RUN, 0, millis("2026-05-30T20:00:00Z")), schedule.poll(millis("2026-05-30T20:00:00Z"), true, 30));
		schedule.rescheduleAfterRun(millis("2026-05-30T20:00:00Z"), ZONE);
		assertEquals(CronBackupSchedule.Event.none(), schedule.poll(millis("2026-05-30T20:00:01Z"), true, 30));
		assertEquals(new CronBackupSchedule.Event(CronBackupSchedule.EventType.WARN, 30, millis("2026-05-31T20:00:00Z")), schedule.poll(millis("2026-05-31T19:59:30Z"), true, 30));
	}

	@Test
	void skipsMissedRunWhenActiveServerWasPaused() {
		CronBackupSchedule schedule = new CronBackupSchedule();
		schedule.reset("0 4 * * *", millis("2026-05-30T19:59:00Z"), ZONE);

		schedule.skipMissedRuns(millis("2026-05-30T20:02:00Z"), ZONE, 1000);

		assertEquals(CronBackupSchedule.Event.none(), schedule.poll(millis("2026-05-30T20:02:00Z"), true, 30));
		assertEquals(new CronBackupSchedule.Event(CronBackupSchedule.EventType.WARN, 30, millis("2026-05-31T20:00:00Z")), schedule.poll(millis("2026-05-31T19:59:30Z"), true, 30));
	}

	private static long millis(String instant) {
		return Instant.parse(instant).toEpochMilli();
	}
}
