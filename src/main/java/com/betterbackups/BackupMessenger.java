package com.betterbackups;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

public final class BackupMessenger {
	private static final int PREFIX_COLOR = 0x38BDF8;

	private BackupMessenger() {
	}

	public static void info(CommandSourceStack source, String message) {
		send(source, text(message, ChatFormatting.WHITE), false);
	}

	public static void info(CommandSourceStack source, BackupSettings settings, String key, Object... arguments) {
		info(source, t(settings, key, arguments));
	}

	public static void success(CommandSourceStack source, String message, boolean broadcastToOps) {
		send(source, text(message, ChatFormatting.GREEN), broadcastToOps);
	}

	public static void success(CommandSourceStack source, BackupSettings settings, String key, boolean broadcastToOps, Object... arguments) {
		success(source, t(settings, key, arguments), broadcastToOps);
	}

	public static void warning(CommandSourceStack source, String message, boolean broadcastToOps) {
		send(source, text(message, ChatFormatting.GOLD), broadcastToOps);
	}

	public static void warning(CommandSourceStack source, BackupSettings settings, String key, boolean broadcastToOps, Object... arguments) {
		warning(source, t(settings, key, arguments), broadcastToOps);
	}

	public static void error(CommandSourceStack source, String message) {
		source.sendFailure(message(text(message, ChatFormatting.RED)));
	}

	public static void error(CommandSourceStack source, BackupSettings settings, String key, Object... arguments) {
		error(source, t(settings, key, arguments));
	}

	public static void line(CommandSourceStack source, Component body) {
		send(source, body, false);
	}

	public static void broadcast(MinecraftServer server, Component body) {
		server.getPlayerList().broadcastSystemMessage(message(body), false);
	}

	public static void broadcastAndNotifySource(CommandSourceStack source, Component body) {
		broadcast(source.getServer(), body);
		if (!source.isPlayer()) {
			send(source, body, false);
		}
	}

	public static MutableComponent text(String value, ChatFormatting formatting) {
		return Component.literal(value).withStyle(formatting);
	}

	public static MutableComponent text(BackupSettings settings, String key, ChatFormatting formatting, Object... arguments) {
		return text(t(settings, key, arguments), formatting);
	}

	public static MutableComponent value(String value) {
		return text(value, ChatFormatting.AQUA);
	}

	public static MutableComponent muted(String value) {
		return text(value, ChatFormatting.GRAY);
	}

	public static MutableComponent successText(String value) {
		return text(value, ChatFormatting.GREEN);
	}

	public static MutableComponent successText(BackupSettings settings, String key, Object... arguments) {
		return text(settings, key, ChatFormatting.GREEN, arguments);
	}

	public static MutableComponent warningText(String value) {
		return text(value, ChatFormatting.GOLD);
	}

	public static MutableComponent warningText(BackupSettings settings, String key, Object... arguments) {
		return text(settings, key, ChatFormatting.GOLD, arguments);
	}

	public static MutableComponent errorText(String value) {
		return text(value, ChatFormatting.RED);
	}

	public static MutableComponent errorText(BackupSettings settings, String key, Object... arguments) {
		return text(settings, key, ChatFormatting.RED, arguments);
	}

	public static MutableComponent label(String value) {
		return text(value, ChatFormatting.WHITE);
	}

	public static MutableComponent label(BackupSettings settings, String key) {
		return text(settings, key, ChatFormatting.WHITE);
	}

	public static MutableComponent commandButton(String label, String command, String hoverText) {
		return Component.literal(label)
			.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
			.withStyle(style -> style
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
	}

	public static MutableComponent commandButton(BackupSettings settings, String labelKey, String command, String hoverKey) {
		return commandButton(t(settings, labelKey), command, t(settings, hoverKey));
	}

	public static MutableComponent suggestButton(String label, String command, String hoverText) {
		return Component.literal(label)
			.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
			.withStyle(style -> style
				.withClickEvent(new ClickEvent.SuggestCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
	}

	public static MutableComponent suggestButton(BackupSettings settings, String labelKey, String command, String hoverKey) {
		return suggestButton(t(settings, labelKey), command, t(settings, hoverKey));
	}

	public static String t(BackupSettings settings, String key, Object... arguments) {
		return BackupTranslations.translate(settings.language(), key, arguments);
	}

	private static void send(CommandSourceStack source, Component body, boolean broadcastToOps) {
		source.sendSuccess(() -> message(body), broadcastToOps);
	}

	private static MutableComponent message(Component body) {
		return Component.literal("[Better Backups] ").withColor(PREFIX_COLOR).append(body);
	}
}
