package com.betterbackups;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public final class BackupHelp {
	private BackupHelp() {
	}

	public static MutableComponent section(BackupSettings settings, String key) {
		return BackupMessenger.text(settings, key, ChatFormatting.GOLD);
	}

	public static MutableComponent runnable(BackupSettings settings, String command, String descriptionKey) {
		return command(command, command)
			.append(BackupMessenger.muted(" - "))
			.append(BackupMessenger.label(settings, descriptionKey));
	}

	public static MutableComponent editable(BackupSettings settings, String displayCommand, String suggestedCommand, String descriptionKey) {
		return suggestion(displayCommand, suggestedCommand)
			.append(BackupMessenger.muted(" - "))
			.append(BackupMessenger.label(settings, descriptionKey));
	}

	private static MutableComponent command(String label, String command) {
		return BackupMessenger.commandButton(label, command, command);
	}

	private static MutableComponent suggestion(String label, String command) {
		return BackupMessenger.suggestButton(label, command, command);
	}
}
