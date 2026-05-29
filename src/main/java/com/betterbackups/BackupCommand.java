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
					.then(literal("off").executes(context -> setClearRequiresConfirm(context, false)))))
			.then(literal("config").executes(BackupCommand::config)));
	}

	private static int createBackup(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		BackupManager manager = BetterBackupsMod.manager();
		BackupMessenger.warning(source, "Saving the world and starting a backup. The server may pause briefly.", false);
		manager.startBackup(source.getServer()).whenComplete((entry, throwable) -> source.getServer().execute(() -> {
			if (throwable == null) {
				BackupMessenger.success(source, "Backup completed: " + entry.name(), true);
			} else if (BackupManager.unwrap(throwable) instanceof BackupManager.BackupAlreadyRunningException) {
				BackupMessenger.error(source, "A backup is already running.");
			} else {
				BackupMessenger.error(source, "Backup failed: " + BackupManager.unwrap(throwable).getMessage());
			}
		}));
		return 1;
	}

	private static int listBackups(CommandContext<CommandSourceStack> context) {
		try {
			List<BackupEntry> entries = BetterBackupsMod.manager().listBackups();
			if (entries.isEmpty()) {
				BackupMessenger.info(context.getSource(), "No backups found.");
				return 0;
			}

			BackupMessenger.info(context.getSource(), "Available backups:");
			for (BackupEntry entry : entries) {
				BackupMessenger.line(context.getSource(), BackupMessenger.value(entry.name())
					.append(BackupMessenger.muted(" (" + formatBytes(entry.sizeBytes()) + ")")));
			}
			return entries.size();
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not list backups: " + exception.getMessage());
			return 0;
		}
	}

	private static int clearBackups(CommandContext<CommandSourceStack> context) {
		try {
			BackupManager manager = BetterBackupsMod.manager();
			BackupSettings settings = manager.loadSettings();
			manager.requireCanClearBackups();
			if (!settings.shouldConfirmBeforeClear()) {
				return deleteBackups(context);
			}

			int count = manager.listBackups().size();
			if (count == 0) {
				BackupMessenger.info(context.getSource(), "No backups found.");
				return 0;
			}

			MutableComponent message = BackupMessenger.warningText("This will delete " + count + pluralizeBackup(count) + ". ");
			message.append(BackupMessenger.commandButton("[Confirm]", "/backup clear confirm", "Delete all backups"));
			BackupMessenger.line(context.getSource(), message);
			return count;
		} catch (Exception exception) {
			showClearError(context, exception, "Could not prepare backup cleanup: ");
			return 0;
		}
	}

	private static int confirmClearBackups(CommandContext<CommandSourceStack> context) {
		return deleteBackups(context);
	}

	private static int restoreBackup(CommandContext<CommandSourceStack> context) {
		try {
			String backupName = StringArgumentType.getString(context, "backup");
			BackupManager manager = BetterBackupsMod.manager();
			BackupSettings settings = manager.loadSettings();
			if (settings.shouldDelayRestore()) {
				manager.scheduleRestore(backupName, settings.shouldStopAfterRestore(), settings.restoreDelaySeconds());
				MutableComponent message = BackupMessenger.warningText("Restore prepared for " + backupName + ". ");
				if (settings.shouldStopAfterRestore()) {
					message.append(BackupMessenger.warningText("The server will stop in " + DurationParser.formatSeconds(settings.restoreDelaySeconds()) + ". "));
				} else {
					message.append(BackupMessenger.warningText("Restore will be prepared in " + DurationParser.formatSeconds(settings.restoreDelaySeconds()) + ". "));
				}
				message.append(BackupMessenger.commandButton("[Cancel]", "/backup restore cancel", "Cancel this restore"));
				BackupMessenger.broadcastAndNotifySource(context.getSource(), message);
				return 1;
			}
			if (settings.shouldStopAfterRestore()) {
				manager.restoreAfterServerStop(context.getSource().getServer(), backupName);
				BackupMessenger.broadcastAndNotifySource(context.getSource(), BackupMessenger.warningText("Restore prepared for " + backupName + ". The server is stopping now."));
				context.getSource().getServer().halt(false);
			} else {
				manager.setPendingRestore(backupName);
				BackupMessenger.warning(context.getSource(), "Restore prepared for " + backupName + ". Stop and start the server to restore.", true);
			}
			return 1;
		} catch (Exception exception) {
			if (exception instanceof BackupManager.RestoreAlreadyRunningException) {
				BackupMessenger.error(context.getSource(), "A restore is already running.");
			} else {
				BackupMessenger.error(context.getSource(), "Could not prepare restore: " + exception.getMessage());
			}
			return 0;
		}
	}

	private static int cancelRestore(CommandContext<CommandSourceStack> context) {
		if (BetterBackupsMod.manager().cancelScheduledRestore()) {
			BackupMessenger.success(context.getSource(), "Restore cancelled.", true);
			return 1;
		}
		BackupMessenger.info(context.getSource(), "No restore is waiting to start.");
		return 0;
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		try {
			BackupManager manager = BetterBackupsMod.manager();
			BackupSettings settings = manager.loadSettings();
			if (manager.isBackupRunning()) {
				BackupMessenger.warning(context.getSource(), "Backup in progress.", false);
			} else {
				BackupEntry latest = manager.latestBackup();
				if (latest == null) {
					BackupMessenger.info(context.getSource(), "No backup has completed since this server started.");
				} else {
					BackupMessenger.line(context.getSource(), BackupMessenger.label("Latest backup: ").append(BackupMessenger.value(latest.name())));
				}
			}
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Scheduled backups: ").append(onOff(settings.scheduleEnabled())));
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not read backup status: " + exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		try {
			BetterBackupsMod.manager().setScheduleEnabled(enabled);
			BackupMessenger.success(context.getSource(), "Scheduled backups turned " + (enabled ? "on." : "off."), true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update schedule: " + exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleInterval(CommandContext<CommandSourceStack> context) {
		try {
			Duration duration = DurationParser.parse(StringArgumentType.getString(context, "duration"));
			BetterBackupsMod.manager().setScheduleInterval(duration);
			BackupMessenger.success(context.getSource(), "Backup interval set to " + DurationParser.formatMinutes(duration.toMinutes()) + ".", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update backup interval: " + exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleWarningEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		try {
			BetterBackupsMod.manager().setScheduleWarningEnabled(enabled);
			BackupMessenger.success(context.getSource(), "Schedule warning turned " + (enabled ? "on." : "off."), true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update schedule warning: " + exception.getMessage());
			return 0;
		}
	}

	private static int setScheduleWarningTime(CommandContext<CommandSourceStack> context) {
		try {
			Duration duration = DurationParser.parseWithSeconds(StringArgumentType.getString(context, "duration"));
			BetterBackupsMod.manager().setScheduleWarningTime(duration);
			BackupMessenger.success(context.getSource(), "Schedule warning time set to " + DurationParser.formatSeconds(duration.toSeconds()) + ".", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update schedule warning time: " + exception.getMessage());
			return 0;
		}
	}

	private static int setBackupsToKeep(CommandContext<CommandSourceStack> context) {
		try {
			int count = IntegerArgumentType.getInteger(context, "count");
			BetterBackupsMod.manager().setBackupsToKeep(count);
			BackupMessenger.success(context.getSource(), "Backup limit set to " + count + ". The latest backups will be kept.", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update backup limit: " + exception.getMessage());
			return 0;
		}
	}

	private static int setStopAfterRestore(CommandContext<CommandSourceStack> context, boolean enabled) {
		try {
			BetterBackupsMod.manager().setStopAfterRestore(enabled);
			BackupMessenger.success(context.getSource(), "Stop after restore turned " + (enabled ? "on." : "off."), true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update restore setting: " + exception.getMessage());
			return 0;
		}
	}

	private static int setRestoreDelayEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
		try {
			BetterBackupsMod.manager().setRestoreDelayEnabled(enabled);
			BackupMessenger.success(context.getSource(), "Restore delay turned " + (enabled ? "on." : "off."), true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update restore delay: " + exception.getMessage());
			return 0;
		}
	}

	private static int setRestoreDelayTime(CommandContext<CommandSourceStack> context) {
		try {
			Duration duration = DurationParser.parseWithSeconds(StringArgumentType.getString(context, "duration"));
			BetterBackupsMod.manager().setRestoreDelayTime(duration);
			BackupMessenger.success(context.getSource(), "Restore delay time set to " + DurationParser.formatSeconds(duration.toSeconds()) + ".", true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update restore delay time: " + exception.getMessage());
			return 0;
		}
	}

	private static int setClearRequiresConfirm(CommandContext<CommandSourceStack> context, boolean enabled) {
		try {
			BetterBackupsMod.manager().setClearRequiresConfirm(enabled);
			BackupMessenger.success(context.getSource(), "Clear confirmation turned " + (enabled ? "on." : "off."), true);
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not update cleanup setting: " + exception.getMessage());
			return 0;
		}
	}

	private static int config(CommandContext<CommandSourceStack> context) {
		try {
			BackupManager manager = BetterBackupsMod.manager();
			BackupSettings settings = manager.loadSettings();
			BackupMessenger.info(context.getSource(), "Backup config:");
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Scheduled backups: ").append(onOff(settings.scheduleEnabled())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Backup interval: ").append(BackupMessenger.value(DurationParser.formatMinutes(settings.intervalMinutes()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Schedule warning: ").append(onOff(settings.shouldWarnBeforeScheduledBackup())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Warning time: ").append(BackupMessenger.value(DurationParser.formatSeconds(settings.scheduleWarningSeconds()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Maximum backups: ").append(BackupMessenger.value(String.valueOf(settings.backupsToKeep()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Stop after restore: ").append(onOff(settings.shouldStopAfterRestore())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Restore delay: ").append(onOff(settings.shouldDelayRestore())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Restore delay time: ").append(BackupMessenger.value(DurationParser.formatSeconds(settings.restoreDelaySeconds()))));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Clear confirmation: ").append(onOff(settings.shouldConfirmBeforeClear())));
			BackupMessenger.line(context.getSource(), BackupMessenger.label("Backup folder: ").append(BackupMessenger.value(manager.resolveBackupDirectory(settings).toString())));
			if (!settings.pendingRestore().isBlank()) {
				BackupMessenger.line(context.getSource(), BackupMessenger.label("Restore on next start: ").append(BackupMessenger.warningText(settings.pendingRestore())));
			}
			return 1;
		} catch (Exception exception) {
			BackupMessenger.error(context.getSource(), "Could not read backup config: " + exception.getMessage());
			return 0;
		}
	}

	private static MutableComponent onOff(boolean enabled) {
		return enabled ? BackupMessenger.successText("On") : BackupMessenger.muted("Off");
	}

	private static int deleteBackups(CommandContext<CommandSourceStack> context) {
		try {
			int deleted = BetterBackupsMod.manager().clearBackups();
			if (deleted == 0) {
				BackupMessenger.info(context.getSource(), "No backups found.");
			} else {
				BackupMessenger.success(context.getSource(), "Deleted " + deleted + pluralizeBackup(deleted) + ".", true);
			}
			return deleted;
		} catch (Exception exception) {
			showClearError(context, exception, "Could not clear backups: ");
			return 0;
		}
	}

	private static void showClearError(CommandContext<CommandSourceStack> context, Exception exception, String fallbackPrefix) {
		if (exception instanceof BackupManager.BackupAlreadyRunningException) {
			BackupMessenger.error(context.getSource(), "A backup is already running.");
		} else if (exception instanceof BackupManager.RestoreAlreadyRunningException) {
			BackupMessenger.error(context.getSource(), "A restore is already running.");
		} else {
			BackupMessenger.error(context.getSource(), fallbackPrefix + exception.getMessage());
		}
	}

	private static String pluralizeBackup(int count) {
		return count == 1 ? " backup" : " backups";
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
