package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RealtimeBackupScheduleTest {
	@Test
	void warnsOnceThenRunsAndReschedules() {
		RealtimeBackupSchedule schedule = new RealtimeBackupSchedule();
		schedule.reset(0, 10);

		assertEquals(RealtimeBackupSchedule.Event.none(), schedule.poll(minutes(8), true, 30));
		assertEquals(new RealtimeBackupSchedule.Event(RealtimeBackupSchedule.EventType.WARN, 30, minutes(10)), schedule.poll(minutes(10) - seconds(30), true, 30));
		assertEquals(RealtimeBackupSchedule.Event.none(), schedule.poll(minutes(10) - seconds(20), true, 30));
		assertEquals(new RealtimeBackupSchedule.Event(RealtimeBackupSchedule.EventType.RUN, 0, minutes(10)), schedule.poll(minutes(10), true, 30));
		schedule.rescheduleAfterRun(minutes(10), 10);
		assertEquals(RealtimeBackupSchedule.Event.none(), schedule.poll(minutes(10) + seconds(1), true, 30));
		assertEquals(new RealtimeBackupSchedule.Event(RealtimeBackupSchedule.EventType.WARN, 30, minutes(20)), schedule.poll(minutes(20) - seconds(30), true, 30));
	}

	@Test
	void runsWithoutWarningWhenWarningIsDisabled() {
		RealtimeBackupSchedule schedule = new RealtimeBackupSchedule();
		schedule.reset(0, 10);

		assertEquals(RealtimeBackupSchedule.Event.none(), schedule.poll(minutes(10) - seconds(30), false, 30));
		assertEquals(new RealtimeBackupSchedule.Event(RealtimeBackupSchedule.EventType.RUN, 0, minutes(10)), schedule.poll(minutes(10), false, 30));
	}

	@Test
	void doesNothingWhenDisabled() {
		RealtimeBackupSchedule schedule = new RealtimeBackupSchedule();
		schedule.reset(0, 10);

		schedule.disable();

		assertEquals(RealtimeBackupSchedule.Event.none(), schedule.poll(minutes(10), true, 30));
	}

	private static long minutes(long minutes) {
		return seconds(minutes * 60);
	}

	private static long seconds(long seconds) {
		return seconds * 1000;
	}
}
