package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import org.junit.jupiter.api.Test;

class BackupHelpTest {
	@Test
	void runnableHelpLineRunsCommand() {
		MutableComponent line = BackupHelp.runnable(BackupSettings.defaults(), "/backup start", "help.start.description");

		ClickEvent clickEvent = line.getStyle().getClickEvent();

		assertInstanceOf(ClickEvent.RunCommand.class, clickEvent);
		assertEquals("/backup start", ((ClickEvent.RunCommand) clickEvent).command());
	}

	@Test
	void editableHelpLineSuggestsCommand() {
		MutableComponent line = BackupHelp.editable(BackupSettings.defaults(), "/backup restore <backup>", "/backup restore ", "help.restore.description");

		ClickEvent clickEvent = line.getStyle().getClickEvent();

		assertInstanceOf(ClickEvent.SuggestCommand.class, clickEvent);
		assertEquals("/backup restore ", ((ClickEvent.SuggestCommand) clickEvent).command());
	}
}
