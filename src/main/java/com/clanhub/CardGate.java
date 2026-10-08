package com.clanhub;

import com.clanhub.ClanHubConfig.CardTrigger;

/**
 * Whether a card (and the underline under its words) shows yet: the show mode, the key and the delay settings against
 * how long the mouse has been on the words. Pure: the times come in as numbers, so no clock is read here.
 */
final class CardGate
{
	/** Nanoseconds in a millisecond. */
	private static final long NANOS_PER_MILLI = 1_000_000L;

	private CardGate()
	{
	}

	/**
	 * Whether the card shows. With {@link CardTrigger#CLICK} it shows exactly when the words were clicked ({@code clicked}),
	 * with no wait. Otherwise: not when the trigger is {@link CardTrigger#HOLD_KEY} and the key is not held; else once the
	 * mouse has been on the words for {@code delayMs} (the gap between {@code hoveredSinceNanos} and {@code nowNanos} is
	 * at least that), at once for a delay of 0. {@code clicked} means nothing to the other two modes.
	 */
	static boolean shows(CardTrigger trigger, boolean keyHeld, boolean clicked, long hoveredSinceNanos, long nowNanos,
		int delayMs)
	{
		if (trigger == CardTrigger.CLICK)
		{
			return clicked;
		}
		if (trigger == CardTrigger.HOLD_KEY && !keyHeld)
		{
			return false;
		}
		return nowNanos - hoveredSinceNanos >= delayMs * NANOS_PER_MILLI;
	}
}
