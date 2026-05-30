package com.betterbackups;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.hasPermission;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class BackupCommand {
	private static final SuggestionProvider<CommandSourceStack> BACKUP_SUGGESTIONS = (context, builder) -> {
		try {
			List<BackupEntry> entries = BetterBackupsMod.manager().listBackups().stream()
				.sorted(Comparator.comparing(BackupEntry::modifiedAt).reversed())
				.toList();
			for (BackupEntry entry : entries) {
				builder.suggest(entry.name());
			}
		} catch (Exception exception) {
			BetterBackupsMod.LOGGER.warn("Could not suggest backups", exception);
		}
		return builder.buildFuture();
	};
	private static final SuggestionProvider<CommandSourceStack> LANGUAGE_SUGGESTIONS = (context, builder) -> {
		for (String language : BackupTranslations.supportedLanguages()) {
			builder.suggest(language);
		}
		return builder.buildFuture();
	};
	private static final SuggestionProvider<CommandSourceStack> SCHEDULE_MODE_SUGGESTIONS = (context, builder) -> {
		builder.suggest("active");
		builder.suggest("realtime");
		return builder.buildFuture();
	};

	private BackupCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
		dispatcher.register(literal("backup")
			.requires(hasPermission(Commands.LEVEL_OWNERS))
			.then(literal("start").executes(BackupCommand::createBackup))
			.then(literal("list").executes(BackupCommand::listBackups))
			.then(literal("clear")
				.executes(BackupCommand::clearBackups)
				.then(literal("confirm").executes(BackupCommand::confirmClearBackups)))
			.then(literal("restore")
				.then(literal("cancel").executes(BackupCommand::cancelRestore))
				.then(argument("backup", StringArgumentType.word())
					.suggests(BACKUP_SUGGESTIONS)
					.executes(BackupCommand::restoreBackup)))
			.then(literal("status").executes(BackupCommand::status))
			.then(literal("set")
				.then(literal("schedule")
					.then(literal("on").executes(context -> setScheduleEnabled(context, true)))
					.then(literal("off").executes(context -> setScheduleEnabled(context, false)))
					.then(literal("every")
						.then(argument("duration", StringArgumentType.word()).executes(BackupCommand::setScheduleInterval)))
					.then(literal("mode")
						.then(argument("mode", StringArgumentType.word())
							.suggests(SCHEDULE_MODE_SUGGESTIONS)
							.executes(BackupCommand::setScheduleMode)))
					.then(literal("warning")
						.then(literal("on").executes(context -> setScheduleWarningEnabled(context, true)))
						.then(literal("off").executes(context -> setScheduleWarningEnabled(context, false)))
						.then(literal("before")
							.then(argument("duration", StringArgumentType.word()).executes(BackupCommand::setScheduleWarningTime)))))
				.then(literal("max-backups")
					.then(argument("count", IntegerArgumentType.integer(1)).executes(BackupCommand::setBackupsToKeep)))
				.then(literal("stop-after-restore")
					.then(literal("on").executes(context -> setStopAfterRestore(context, true)))
					.then(literal("off").executes(context -> setStopAfterRestore(context, false))))
				.then(literal("restore-delay")
					.then(literal("on").executes(context -> setRestoreDelayEnabled(context, true)))
					.then(literal("off").executes(context -> setRestoreDelayEnabled(context, false)))
					.then(literal("time")
						.then(argument("duration", StringArgumentType.word()).executes(BackupCommand::setRestoreDelayTime))))
				.then(literal("clear-confirm")
					.then(literal("on").executes(context -> setClearRequiresConfirm(context, true)))
					.then(literal("off").executes(context -> setClearRequiresConfirm(context, false))))
				.then(literal("language")
					.then(argument("language", StringArgumentType.word())
						.suggests(LANGUAGE_SUGGESTIONS)
						.executes(BackupCommand::setLanguage))))
			.then(literal("config").executes(BackupCommand::config)));
	}

	private static int createBackup(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		BackupManager manager = BetterBackupsMod.manager();
		BackupSettings settings = loadSettingsOrDefault(manager);
		BackupMessenger.warning(source, settings, "backup.starting", false);
		manager.startBackup(source.getServer()).whenComplete((entry, throwable) -> source.getServer().execute(() -> {
			if (throwable == null) {
				BackupMessenger.success(source, settings, "backup.completed", true, entry.name());
			} else if (BackupManager.unwrap(throwable) instanceof BackupManager.BackupAlreadyRunningException) {
				BackupMessenger.error(source, settings, "backup.alreadyRunning");
			} else {
				BackupMessenger.error(source, settings, "backup.failed", BackupManager.unwrap(throwable).getMessage());
			}
		}));
		return 1;
	}

	private static int listBackups(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			List<BackupEntry> entries = BetterBackupsMod.manager().listBackups();
			if (entries.isEmpty()) {
				BackupMessenger.info(context.getSource(), settings, "backup.noneFound");
				return 0;
			}

			BackupMessenger.info(context.getSource(), settings, "backup.available");
			for (BackupEntry entry : entries) {
				BackupMessenger.line(context.getSource(), BackupMessenger.value(entry.name())
					.append(BackupMessenger.muted(" (" + formatBytes(entry.sizeBytes()) + ")")));
			}
			return entries.size();
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "backup.listFailed", exception.getMessage());
			return 0;
		}
	}

	private static int clearBackups(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BackupManager manager = BetterBackupsMod.manager();
			settings = manager.loadSettings();
			manager.requireCanClearBackups();
			if (!settings.shouldConfirmBeforeClear()) {
				return deleteBackups(context);
			}

			int count = manager.listBackups().size();
			if (count == 0) {
				BackupMessenger.info(context.getSource(), settings, "backup.noneFound");
				return 0;
			}

			MutableComponent message = BackupMessenger.warningText(settings, "clear.confirm", count, pluralizeBackup(settings, count));
			message.append(BackupMessenger.commandButton(settings, "button.confirm", "/backup clear confirm", "clear.confirm.hover"));
			BackupMessenger.line(context.getSource(), message);
			return count;
		} catch (Exception exception) {
			showClearError(context, settings, exception, "clear.prepareFailed");
			return 0;
		}
	}

	private static int confirmClearBackups(CommandContext<CommandSourceStack> context) {
		return deleteBackups(context);
	}

	private static int restoreBackup(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			String backupName = StringArgumentType.getString(context, "backup");
			BackupManager manager = BetterBackupsMod.manager();
			settings = manager.loadSettings();
			if (settings.shouldDelayRestore()) {
				manager.scheduleRestore(backupName, settings.shouldStopAfterRestore(), settings.restoreDelaySeconds());
				MutableComponent message = BackupMessenger.warningText(settings, "restore.prepared", backupName);
				if (settings.shouldStopAfterRestore()) {
					message.append(BackupMessenger.warningText(settings, "restore.delay.stop", BackupTranslations.formatSeconds(settings.language(), settings.restoreDelaySeconds())));
				} else {
					message.append(BackupMessenger.warningText(settings, "restore.delay.prepare", BackupTranslations.formatSeconds(settings.language(), settings.restoreDelaySeconds())));
				}
				message.append(BackupMessenger.commandButton(settings, "button.cancel", "/backup restore cancel", "restore.cancel.hover"));
				BackupMessenger.broadcastAndNotifySource(context.getSource(), message);
				return 1;
			}
			if (settings.shouldStopAfterRestore()) {
				manager.restoreAfterServerStop(context.getSource().getServer(), backupName);
				BackupMessenger.broadcastAndNotifySource(context.getSource(), BackupMessenger.warningText(settings, "restore.prepared.stopNow", backupName));
				context.getSource().getServer().halt(false);
			} else {
				manager.setPendingRestore(backupName);
				BackupMessenger.warning(context.getSource(), settings, "restore.prepared.manualRestart", true, backupName);
			}
			return 1;
		} catch (Exception exception) {
			if (exception instanceof BackupManager.RestoreAlreadyRunningException) {
				BackupMessenger.error(context.getSource(), settings, "restore.alreadyRunning");
			} else if (exception instanceof IllegalArgumentException) {
				BackupMessenger.error(context.getSource(), settings, "backup.notFound", StringArgumentType.getString(context, "backup"));
			} else {
				BackupMessenger.error(context.getSource(), settings, "restore.prepareFailed", exception.getMessage());
			}
			return 0;
		}
	}

	private static int cancelRestore(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		if (BetterBackupsMod.manager().cancelScheduledRestore()) {
			BackupMessenger.success(context.getSource(), settings, "restore.cancelled", true);
			return 1;
		}
		BackupMessenger.info(context.getSource(), settings, "restore.noneWaiting");
		return 0;
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BackupManager manager = BetterBackupsMod.manager();
			settings = manager.loadSettings();
			if (manager.isBackupRunning()) {
				BackupMessenger.warning(context.getSource(), settings, "backup.inProgress", false);
			} else {
				BackupEntry latest = manager.latestBackup();
				if (latest == null) {
					BackupMessenger.info(context.getSource(), settings, "status.noCompletedBackup");
				} else {
					BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "status.latestBackup").append(BackupMessenger.value(latest.name())));
				}
			}
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.scheduleEnabled").append(onOff(settings, settings.scheduleEnabled())));
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "status.readFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BetterBackupsMod.manager().setScheduleEnabled(enabled);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, enabled ? "schedule.enabled" : "schedule.disabled", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "schedule.updateFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleInterval(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			Duration duration = DurationParser.parse(StringArgumentType.getString(context, "duration"));
			BetterBackupsMod.manager().setScheduleInterval(duration);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, "schedule.intervalSet", true, DurationParser.formatMinutes(duration.toMinutes()));
			return 1;
		} catch (Exception exception) {
			if (exception instanceof IllegalArgumentException) {
				BackupMessenger.error(context.getSource(), settings, "duration.invalidInterval");
			} else {
				BackupMessenger.error(context.getSource(), settings, "schedule.intervalFailed", exception.getMessage());
			}
			return 0;
		}
	}

	private static int setScheduleMode(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		String mode = StringArgumentType.getString(context, "mode");
		if (!"active".equals(mode) && !"realtime".equals(mode)) {
			BackupMessenger.error(context.getSource(), settings, "schedule.modeUnsupported", mode);
			return 0;
		}
		try {
			BetterBackupsMod.manager().setScheduleMode(mode);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, "schedule.modeSet", true, settings.scheduleMode());
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "schedule.modeFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleWarningEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BetterBackupsMod.manager().setScheduleWarningEnabled(enabled);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, enabled ? "schedule.warningEnabled" : "schedule.warningDisabled", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "schedule.warningFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleWarningTime(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			Duration duration = DurationParser.parseWithSeconds(StringArgumentType.getString(context, "duration"));
			BetterBackupsMod.manager().setScheduleWarningTime(duration);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, "schedule.warningTimeSet", true, BackupTranslations.formatSeconds(settings.language(), duration.toSeconds()));
			return 1;
		} catch (Exception exception) {
			if (exception instanceof IllegalArgumentException) {
				BackupMessenger.error(context.getSource(), settings, "duration.invalidWithSeconds");
			} else {
				BackupMessenger.error(context.getSource(), settings, "schedule.warningTimeFailed", exception.getMessage());
			}
			return 0;
		}
	}

	private static int setBackupsToKeep(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			int count = IntegerArgumentType.getInteger(context, "count");
			BetterBackupsMod.manager().setBackupsToKeep(count);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, "backup.limitSet", true, count);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "backup.limitFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setStopAfterRestore(CommandContext<CommandSourceStack> context, boolean enabled) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BetterBackupsMod.manager().setStopAfterRestore(enabled);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, enabled ? "restore.stopAfterEnabled" : "restore.stopAfterDisabled", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "restore.settingFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setRestoreDelayEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BetterBackupsMod.manager().setRestoreDelayEnabled(enabled);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, enabled ? "restore.delayEnabled" : "restore.delayDisabled", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "restore.delayFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setRestoreDelayTime(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			Duration duration = DurationParser.parseWithSeconds(StringArgumentType.getString(context, "duration"));
			BetterBackupsMod.manager().setRestoreDelayTime(duration);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, "restore.delayTimeSet", true, BackupTranslations.formatSeconds(settings.language(), duration.toSeconds()));
			return 1;
		} catch (Exception exception) {
			if (exception instanceof IllegalArgumentException) {
				BackupMessenger.error(context.getSource(), settings, "duration.invalidWithSeconds");
			} else {
				BackupMessenger.error(context.getSource(), settings, "restore.delayTimeFailed", exception.getMessage());
			}
			return 0;
		}
	}

	private static int setClearRequiresConfirm(CommandContext<CommandSourceStack> context, boolean enabled) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BetterBackupsMod.manager().setClearRequiresConfirm(enabled);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, enabled ? "clear.confirmEnabled" : "clear.confirmDisabled", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "clear.settingFailed", exception.getMessage());
			return 0;
		}
	}

	private static int setLanguage(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		String language = StringArgumentType.getString(context, "language");
		if (!BackupTranslations.isSupportedLanguage(language)) {
			BackupMessenger.error(context.getSource(), settings, "language.unsupported", language, String.join(", ", BackupTranslations.supportedLanguages()));
			return 0;
		}
		try {
			BetterBackupsMod.manager().setLanguage(language);
			settings = BetterBackupsMod.manager().loadSettings();
			BackupMessenger.success(context.getSource(), settings, "language.set", true, settings.language());
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "language.updateFailed", exception.getMessage());
			return 0;
		}
	}

	private static int config(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			BackupManager manager = BetterBackupsMod.manager();
			settings = manager.loadSettings();
			BackupMessenger.info(context.getSource(), settings, "config.title");
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.scheduleEnabled").append(onOff(settings, settings.scheduleEnabled())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.scheduleMode").append(BackupMessenger.value(settings.scheduleMode())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.interval").append(BackupMessenger.value(DurationParser.formatMinutes(settings.intervalMinutes()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.scheduleWarning").append(onOff(settings, settings.shouldWarnBeforeScheduledBackup())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.warningTime").append(BackupMessenger.value(BackupTranslations.formatSeconds(settings.language(), settings.scheduleWarningSeconds()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.maxBackups").append(BackupMessenger.value(String.valueOf(settings.backupsToKeep()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.stopAfterRestore").append(onOff(settings, settings.shouldStopAfterRestore())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.restoreDelay").append(onOff(settings, settings.shouldDelayRestore())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.restoreDelayTime").append(BackupMessenger.value(BackupTranslations.formatSeconds(settings.language(), settings.restoreDelaySeconds()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.clearConfirm").append(onOff(settings, settings.shouldConfirmBeforeClear())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.language").append(BackupMessenger.value(settings.language())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.backupFolder").append(BackupMessenger.value(manager.resolveBackupDirectory(settings).toString())));
			if (!settings.pendingRestore().isBlank()) {
				BackupMessenger.line(context.getSource(), BackupMessenger.label(settings, "config.pendingRestore").append(BackupMessenger.warningText(settings.pendingRestore())));
			}
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), settings, "config.readFailed", exception.getMessage());
			return 0;
		}
	}

	private static MutableComponent onOff(BackupSettings settings, boolean enabled) {
		return enabled ? BackupMessenger.successText(settings, "state.on") : BackupMessenger.muted(BackupMessenger.t(settings, "state.off"));
	}

	private static int deleteBackups(CommandContext<CommandSourceStack> context) {
		BackupSettings settings = loadSettingsOrDefault(BetterBackupsMod.manager());
		try {
			int deleted = BetterBackupsMod.manager().clearBackups();
			if (deleted == 0) {
				BackupMessenger.info(context.getSource(), settings, "backup.noneFound");
			} else {
				BackupMessenger.success(context.getSource(), settings, "clear.deleted", true, deleted, pluralizeBackup(settings, deleted));
			}
			return deleted;
		} catch (Exception exception) {
			showClearError(context, settings, exception, "clear.failed");
			return 0;
		}
	}

	private static void showClearError(CommandContext<CommandSourceStack> context, BackupSettings settings, Exception exception, String fallbackKey) {
		if (exception instanceof BackupManager.BackupAlreadyRunningException) {
			BackupMessenger.error(context.getSource(), settings, "backup.alreadyRunning");
		} else if (exception instanceof BackupManager.RestoreAlreadyRunningException) {
			BackupMessenger.error(context.getSource(), settings, "restore.alreadyRunning");
		} else {
			BackupMessenger.error(context.getSource(), settings, fallbackKey, exception.getMessage());
		}
	}

	private static String pluralizeBackup(BackupSettings settings, int count) {
		return BackupMessenger.t(settings, count == 1 ? "backup.singular" : "backup.plural");
	}

	private static BackupSettings loadSettingsOrDefault(BackupManager manager) {
		try {
			return manager.loadSettings();
		} catch (Exception exception) {
			return BackupSettings.defaults();
		}
	}

	private static String formatBytes(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		}
		long kib = bytes / 1024;
		if (kib < 1024) {
			return kib + " KiB";
		}
		return kib / 1024 + " MiB";
	}
}
