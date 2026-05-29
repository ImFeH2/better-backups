package com.betterbackups;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class BackupTimeNames {
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
	private static final DateTimeFormatter OFFSET_FORMATTER = DateTimeFormatter.ofPattern("xx");

	private BackupTimeNames() {
	}

	public static String now(Clock clock) {
		ZonedDateTime dateTime = ZonedDateTime.now(clock);
		String timestamp = TIMESTAMP_FORMATTER.format(dateTime);
		if (dateTime.getOffset().equals(ZoneOffset.UTC)) {
			return timestamp;
		}
		return timestamp + OFFSET_FORMATTER.format(dateTime);
	}
}
