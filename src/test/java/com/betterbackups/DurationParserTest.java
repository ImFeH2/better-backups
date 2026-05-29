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
	void parsesSecondsForWarnings() {
		assertEquals(Duration.ofSeconds(30), DurationParser.parseWithSeconds("30s"));
		assertEquals(Duration.ofMinutes(5), DurationParser.parseWithSeconds("5m"));
	}

	@Test
	void rejectsInvalidDurations() {
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0m"));
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("10s"));
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("abc"));
	}

	@Test
	void rejectsInvalidSecondDurations() {
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parseWithSeconds("0s"));
		assertThrows(IllegalArgumentException.class, () -> DurationParser.parseWithSeconds("abc"));
	}

	@Test
	void formatsSecondsPrecisely() {
		assertEquals("30 seconds", DurationParser.formatSeconds(30));
		assertEquals("1 minute", DurationParser.formatSeconds(60));
		assertEquals("90 seconds", DurationParser.formatSeconds(90));
		assertEquals("2 minutes", DurationParser.formatSeconds(120));
	}
}
