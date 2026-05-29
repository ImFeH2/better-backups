package com.betterbackups;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class BackupTranslations {
	public static final String DEFAULT_LANGUAGE = "en_us";
	private static final List<String> LANGUAGES = List.of("en_us", "zh_cn");
	private static final Map<String, Properties> TRANSLATIONS = loadTranslations();

	private BackupTranslations() {
	}

	public static List<String> supportedLanguages() {
		return LANGUAGES;
	}

	public static boolean isSupportedLanguage(String language) {
		return language != null && TRANSLATIONS.containsKey(language.toLowerCase());
	}

	public static String normalizeLanguage(String language) {
		if (language == null || language.isBlank()) {
			return DEFAULT_LANGUAGE;
		}
		String normalized = language.toLowerCase();
		if (TRANSLATIONS.containsKey(normalized)) {
			return normalized;
		}
		return DEFAULT_LANGUAGE;
	}

	public static String translate(String language, String key, Object... arguments) {
		String pattern = get(normalizeLanguage(language), key);
		for (int i = 0; i < arguments.length; i++) {
			pattern = pattern.replace("{" + i + "}", String.valueOf(arguments[i]));
		}
		return pattern;
	}

	public static String formatSeconds(String language, long seconds) {
		if (seconds == 1) {
			return translate(language, "duration.second", seconds);
		}
		if (seconds < 60 || seconds % 60 != 0) {
			return translate(language, "duration.seconds", seconds);
		}
		long minutes = seconds / 60;
		if (minutes == 1) {
			return translate(language, "duration.minute", minutes);
		}
		return translate(language, "duration.minutes", minutes);
	}

	private static String get(String language, String key) {
		Properties properties = TRANSLATIONS.get(language);
		if (properties != null && properties.containsKey(key)) {
			return properties.getProperty(key);
		}
		Properties defaults = TRANSLATIONS.get(DEFAULT_LANGUAGE);
		if (defaults != null && defaults.containsKey(key)) {
			return defaults.getProperty(key);
		}
		return key;
	}

	private static Map<String, Properties> loadTranslations() {
		Map<String, Properties> translations = new java.util.LinkedHashMap<>();
		for (String language : LANGUAGES) {
			translations.put(language, loadLanguage(language));
		}
		return Map.copyOf(translations);
	}

	private static Properties loadLanguage(String language) {
		String path = "/assets/better-backups/lang/" + language + ".properties";
		try (InputStream input = BackupTranslations.class.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("Missing language file: " + path);
			}
			Properties properties = new Properties();
			properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
			return properties;
		} catch (IOException exception) {
			throw new IllegalStateException("Could not read language file: " + path, exception);
		}
	}
}
