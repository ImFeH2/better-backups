package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BackupTimeNamesTest {
	@Test
	void omitsOffsetWhenClockUsesUtc() {
		Clock clock = Clock.fixed(Instant.parse("2026-05-29T13:34:08Z"), ZoneOffset.UTC);

		assertEquals("2026-05-29_13-34-08", BackupTimeNames.now(clock));
	}

	@Test
	void appendsOffsetWhenClockUsesLocalZone() {
		Clock clock = Clock.fixed(Instant.parse("2026-05-29T13:34:08Z"), ZoneId.of("Asia/Shanghai"));

		assertEquals("2026-05-29_21-34-08+0800", BackupTimeNames.now(clock));
	}
}
