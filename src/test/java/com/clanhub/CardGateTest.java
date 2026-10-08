package com.clanhub;

import com.clanhub.ClanHubConfig.CardTrigger;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link CardGate}: a card shows by the show mode, whether the key is held, and how long the mouse has been on the words. */
public class CardGateTest
{
	private static final long MS = 1_000_000L;

	// Re-aimed by the lead, 2026-10-08 (lot 7): the gate also takes whether the words were clicked.
	@Test
	public void showsByTriggerKeyAndDelay()
	{
		final long since = 5_000_000_000L;

		assertTrue("on hover with no delay: at once", CardGate.shows(CardTrigger.HOVER, false, false, since, since, 0));

		assertFalse("on hover, delay 300, 100 ms after the hit",
			CardGate.shows(CardTrigger.HOVER, false, false, since, since + 100 * MS, 300));
		assertTrue("on hover, delay 300, 400 ms after the hit",
			CardGate.shows(CardTrigger.HOVER, false, false, since, since + 400 * MS, 300));

		assertFalse("holding a key, key not held, a second after",
			CardGate.shows(CardTrigger.HOLD_KEY, false, false, since, since + 1000 * MS, 0));
		assertTrue("holding a key, key held, no delay",
			CardGate.shows(CardTrigger.HOLD_KEY, true, false, since, since, 0));
	}

	@Test
	public void onClickShowsOnlyAfterAClick()
	{
		final long since = 5_000_000_000L;

		assertFalse("on click, not clicked, a second after the hit",
			CardGate.shows(CardTrigger.CLICK, false, false, since, since + 1000 * MS, 0));
		assertTrue("on click, clicked, delay 300, at the hit's instant: no wait",
			CardGate.shows(CardTrigger.CLICK, false, true, since, since, 300));
		assertTrue("holding a key, key held, clicked means nothing",
			CardGate.shows(CardTrigger.HOLD_KEY, true, false, since, since, 0));
		assertFalse("on hover, clicked means nothing: delay 300, 100 ms after the hit",
			CardGate.shows(CardTrigger.HOVER, false, true, since, since + 100 * MS, 300));
	}
}
