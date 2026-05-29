package com.betterbackups;

import java.time.Clock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterBackupsMod implements ModInitializer {
	public static final String MOD_ID = "better-backups";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static BackupManager manager;

	@Override
	public void onInitialize() {
		var loader = FabricLoader.getInstance();
		var clock = Clock.systemDefaultZone();
		var modVersion = loader.getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
		manager = new BackupManager(
			new BackupSettingsStore(loader.getConfigDir().resolve("better-backups.json")),
			loader.getGameDir(),
			loader.getModContainer("minecraft")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown"),
			modVersion,
			LOGGER,
			clock
		);
		try {
			manager.applyPendingRestoreBeforeServerStart();
		} catch (Exception exception) {
			throw new IllegalStateException("Could not restore backup", exception);
		}

		CommandRegistrationCallback.EVENT.register(BackupCommand::register);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				manager.resetSchedule(manager.loadSettings());
			} catch (Exception exception) {
				LOGGER.error("Could not load backup settings", exception);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> manager.shutdown());
		ServerTickEvents.END_SERVER_TICK.register(server -> manager.tick(server));
	}

	public static BackupManager manager() {
		return manager;
	}
}
