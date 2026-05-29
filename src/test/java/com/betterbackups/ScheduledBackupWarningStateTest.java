package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScheduledBackupWarningStateTest {
	@Test
	void warnsOnceWhenCountdownReachesWarningWindow() {
		ScheduledBackupWarningState warningState = new ScheduledBackupWarningState();

		warningState.reset();

		assertFalse(warningState.shouldWarn(31 * 20, 30));
		assertTrue(warningState.shouldWarn(30 * 20, 30));
		assertFalse(warningState.shouldWarn(29 * 20, 30));
	}

	@Test
	void warnsImmediatelyWhenIntervalIsShorterThanWarningWindow() {
		ScheduledBackupWarningState warningState = new ScheduledBackupWarningState();

		warningState.reset();

		assertTrue(warningState.shouldWarn(10 * 20, 30));
		assertFalse(warningState.shouldWarn(9 * 20, 30));
	}

	@Test
	void doesNotWarnWhenDisabled() {
		ScheduledBackupWarningState warningState = new ScheduledBackupWarningState();

		warningState.reset();

		assertFalse(warningState.shouldWarn(30 * 20, 0));
	}
}
