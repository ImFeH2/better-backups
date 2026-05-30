package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BackupTranslationsTest {
	@Test
	void translatesEnglishMessagesWithArguments() {
		assertEquals("Backup completed: 2026-05-30_12-00-00+0800", BackupTranslations.translate("en_us", "backup.completed", "2026-05-30_12-00-00+0800"));
		assertEquals("Scheduled backup completed: 2026-05-30_12-00-00+0800", BackupTranslations.translate("en_us", "schedule.backup.completed", "2026-05-30_12-00-00+0800"));
	}

	@Test
	void translatesChineseMessagesWithArguments() {
		assertEquals("备份完成：2026-05-30_12-00-00+0800", BackupTranslations.translate("zh_cn", "backup.completed", "2026-05-30_12-00-00+0800"));
		assertEquals("定时备份完成：2026-05-30_12-00-00+0800", BackupTranslations.translate("zh_cn", "schedule.backup.completed", "2026-05-30_12-00-00+0800"));
	}

	@Test
	void fallsBackToEnglishForUnsupportedLanguage() {
		assertEquals("Backup completed: test", BackupTranslations.translate("missing", "backup.completed", "test"));
	}

	@Test
	void exposesSupportedLanguages() {
		assertTrue(BackupTranslations.supportedLanguages().contains("en_us"));
		assertTrue(BackupTranslations.supportedLanguages().contains("zh_cn"));
	}

	@Test
	void formatsDurationsForSelectedLanguage() {
		assertEquals("30 seconds", BackupTranslations.formatSeconds("en_us", 30));
		assertEquals("2 minutes", BackupTranslations.formatSeconds("en_us", 120));
		assertEquals("30 秒", BackupTranslations.formatSeconds("zh_cn", 30));
		assertEquals("2 分钟", BackupTranslations.formatSeconds("zh_cn", 120));
	}
}
