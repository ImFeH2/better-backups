package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

class BackupMessengerTest {
	@Test
	void commandButtonRunsCommand() {
		MutableComponent button = BackupMessenger.commandButton("[Run]", "/backup start", "Start backup");

		ClickEvent clickEvent = button.getStyle().getClickEvent();

		assertInstanceOf(ClickEvent.RunCommand.class, clickEvent);
		assertEquals("/backup start", ((ClickEvent.RunCommand) clickEvent).command());
	}

	@Test
	void suggestButtonSuggestsCommand() {
		MutableComponent button = BackupMessenger.suggestButton("[Edit]", "/backup set max-backups 10", "Edit value");

		ClickEvent clickEvent = button.getStyle().getClickEvent();

		assertInstanceOf(ClickEvent.SuggestCommand.class, clickEvent);
		assertEquals("/backup set max-backups 10", ((ClickEvent.SuggestCommand) clickEvent).command());
	}
}
