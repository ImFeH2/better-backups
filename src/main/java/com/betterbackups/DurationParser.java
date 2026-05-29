package com.betterbackups;

import java.time.Duration;

public final class DurationParser {
	private DurationParser() {
	}

	public static Duration parse(String value) {
		if (value == null || value.length() < 2) {
			throw new IllegalArgumentException("Use a duration like 30m, 2h, or 1d.");
		}

		String numberPart = value.substring(0, value.length() - 1);
		char unit = value.charAt(value.length() - 1);
		long amount;
		try {
			amount = Long.parseLong(numberPart);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Use a duration like 30m, 2h, or 1d.", exception);
		}

		if (amount <= 0) {
			throw new IllegalArgumentException("Duration must be greater than zero.");
		}

		return switch (unit) {
			case 'm' -> Duration.ofMinutes(amount);
			case 'h' -> Duration.ofHours(amount);
			case 'd' -> Duration.ofDays(amount);
			default -> throw new IllegalArgumentException("Use m, h, or d for the duration.");
		};
	}

	public static String formatMinutes(long minutes) {
		if (minutes % (24 * 60) == 0) {
			return minutes / (24 * 60) + "d";
		}
		if (minutes % 60 == 0) {
			return minutes / 60 + "h";
		}
		return minutes + "m";
	}
}
