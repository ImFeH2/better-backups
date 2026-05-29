package com.betterbackups;

import java.time.Duration;

public final class DurationParser {
	private DurationParser() {
	}

	public static Duration parse(String value) {
		return parse(value, false);
	}

	public static Duration parseWithSeconds(String value) {
		return parse(value, true);
	}

	private static Duration parse(String value, boolean allowSeconds) {
		if (value == null || value.length() < 2) {
			throw new IllegalArgumentException(exampleMessage(allowSeconds));
		}

		String numberPart = value.substring(0, value.length() - 1);
		char unit = value.charAt(value.length() - 1);
		long amount;
		try {
			amount = Long.parseLong(numberPart);
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(exampleMessage(allowSeconds), exception);
		}

		if (amount <= 0) {
			throw new IllegalArgumentException("Duration must be greater than zero.");
		}

		return switch (unit) {
			case 's' -> {
				if (!allowSeconds) {
					throw new IllegalArgumentException("Use m, h, or d for the duration.");
				}
				yield Duration.ofSeconds(amount);
			}
			case 'm' -> Duration.ofMinutes(amount);
			case 'h' -> Duration.ofHours(amount);
			case 'd' -> Duration.ofDays(amount);
			default -> throw new IllegalArgumentException(allowSeconds ? "Use s, m, h, or d for the duration." : "Use m, h, or d for the duration.");
		};
	}

	private static String exampleMessage(boolean allowSeconds) {
		return allowSeconds ? "Use a duration like 30s, 5m, 2h, or 1d." : "Use a duration like 30m, 2h, or 1d.";
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

	public static String formatSeconds(long seconds) {
		if (seconds == 1) {
			return "1 second";
		}
		if (seconds < 60 || seconds % 60 != 0) {
			return seconds + " seconds";
		}
		long minutes = seconds / 60;
		if (minutes == 1) {
			return "1 minute";
		}
		return minutes + " minutes";
	}
}
