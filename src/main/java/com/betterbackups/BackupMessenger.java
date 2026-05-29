package com.betterbackups;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

public final class BackupMessenger {
	private static final int PREFIX_COLOR = 0x5B23DA;

	private BackupMessenger() {
	}

	public static void info(CommandSourceStack source, String message) {
		send(source, text(message, ChatFormatting.WHITE), false);
	}

	public static void success(CommandSourceStack source, String message, boolean broadcastToOps) {
		send(source, text(message, ChatFormatting.GREEN), broadcastToOps);
	}

	public static void warning(CommandSourceStack source, String message, boolean broadcastToOps) {
		send(source, text(message, ChatFormatting.GOLD), broadcastToOps);
	}

	public static void error(CommandSourceStack source, String message) {
		source.sendFailure(message(text(message, ChatFormatting.RED)));
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

	public static MutableComponent value(String value) {
		return text(value, ChatFormatting.AQUA);
	}

	public static MutableComponent muted(String value) {
		return text(value, ChatFormatting.GRAY);
	}

	public static MutableComponent successText(String value) {
		return text(value, ChatFormatting.GREEN);
	}

	public static MutableComponent warningText(String value) {
		return text(value, ChatFormatting.GOLD);
	}

	public static MutableComponent errorText(String value) {
		return text(value, ChatFormatting.RED);
	}

	public static MutableComponent label(String value) {
		return text(value, ChatFormatting.WHITE);
	}

	private static void send(CommandSourceStack source, Component body, boolean broadcastToOps) {
		source.sendSuccess(() -> message(body), broadcastToOps);
	}

	private static MutableComponent message(Component body) {
		return Component.literal("[Better Backups] ").withColor(PREFIX_COLOR).append(body);
	}
}
