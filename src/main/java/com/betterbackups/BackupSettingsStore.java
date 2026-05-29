package com.betterbackups;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BackupSettingsStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path path;

	public BackupSettingsStore(Path path) {
		this.path = path;
	}

	public BackupSettings load() throws IOException {
		if (!Files.exists(path)) {
			BackupSettings defaults = BackupSettings.defaults();
			save(defaults);
			return defaults;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			BackupSettings settings = GSON.fromJson(reader, BackupSettings.class);
			if (settings == null) {
				settings = BackupSettings.defaults();
			}
			return normalize(settings);
		}
	}

	public void save(BackupSettings settings) throws IOException {
		Files.createDirectories(path.getParent());
		try (Writer writer = Files.newBufferedWriter(path)) {
			GSON.toJson(normalize(settings), writer);
		}
	}

	private BackupSettings normalize(BackupSettings settings) {
		BackupSettings defaults = BackupSettings.defaults();
		long intervalMinutes = settings.intervalMinutes() > 0 ? settings.intervalMinutes() : defaults.intervalMinutes();
		int backupsToKeep = settings.backupsToKeep() > 0 ? settings.backupsToKeep() : defaults.backupsToKeep();
		String backupDirectory = isBlank(settings.backupDirectory()) ? defaults.backupDirectory() : settings.backupDirectory();
		String pendingRestore = settings.pendingRestore() == null ? "" : settings.pendingRestore();
		return new BackupSettings(settings.scheduleEnabled(), intervalMinutes, backupsToKeep, backupDirectory, settings.shouldStopAfterRestore(), pendingRestore);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
