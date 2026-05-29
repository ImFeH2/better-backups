package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationParserTest {
	@Test
	void parsesMinutesHoursAndDays() {
		assertEquals(Duration.ofMinutes(30), DurationParser.parse("30m"));
		assertEquals(Duration.ofHours(2), DurationParser.parse("2h"));
		assertEquals(Duration.ofDays(1), DurationParser.parse("1d"));
	}

	@Test
	void rejectsInvalidDurations() {
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0m"));
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("10s"));
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("abc"));
	}
}
