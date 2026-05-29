package com.betterbackups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PendingRestoreRequestTest {
	@Test
	void countsDownAndBecomesReadyOnce() {
		PendingRestoreRequest request = new PendingRestoreRequest("backup-name", true, 2);

		assertEquals("backup-name", request.backupName());
		assertTrue(request.shouldStopAfterRestore());
		assertFalse(request.tick());
		assertTrue(request.tick());
		assertFalse(request.tick());
	}

	@Test
	void canBeCancelledBeforeReady() {
		PendingRestoreRequest request = new PendingRestoreRequest("backup-name", false, 2);

		request.cancel();

		assertTrue(request.isCancelled());
		assertFalse(request.tick());
	}

	@Test
	void zeroDelayIsReadyImmediately() {
		PendingRestoreRequest request = new PendingRestoreRequest("backup-name", true, 0);

		assertTrue(request.tick());
	}
}
