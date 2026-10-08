package com.clanhub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The plugin's cards by plain chat line, the last 200 lines. The plugin fills it as chat messages arrive and the
 * overlay asks it about the text of the line under the mouse; shut-down clears it from another thread, so every method
 * is synchronized on the store itself.
 */
final class CardStore
{
	/** How many chat lines keep their cards. */
	private static final int MAX_LINES = 200;

	/** The cards by plain line, the oldest line first. */
	private final Map<String, List<HoverCard>> lines = new LinkedHashMap<>();

	/**
	 * What {@link #find} found: the cards of a stored line, and where that line starts inside the text it was found
	 * in.
	 */
	static final class Match
	{
		/** The cards the stored line holds, in the order their words stand in it. */
		final List<HoverCard> cards;
		/** Where the stored line starts inside the widget's text. */
		final int offset;

		Match(List<HoverCard> cards, int offset)
		{
			this.cards = cards;
			this.offset = offset;
		}
	}

	/**
	 * Keeps the cards for a plain chat line, and lets go of the oldest line when more than 200 are held. An empty list
	 * stores nothing.
	 *
	 * <p>Choice: a null or empty line stores nothing either, since every text contains the empty line and it would match
	 * everything; a line stored again becomes the newest, so "the most recent" holds for a repeated line.
	 */
	synchronized void put(String plainLine, List<HoverCard> cards)
	{
		if (plainLine == null || plainLine.isEmpty() || cards == null || cards.isEmpty())
		{
			return;
		}
		lines.remove(plainLine);
		lines.put(plainLine, Collections.unmodifiableList(new ArrayList<>(cards)));
		if (lines.size() > MAX_LINES)
		{
			final String oldest = lines.keySet().iterator().next();
			lines.remove(oldest);
		}
	}

	/**
	 * The most recently stored line that {@code widgetText} contains, with its cards and the place it starts inside
	 * {@code widgetText}; null when no stored line is in it. The chatbox puts the channel's prefix, and a timestamp
	 * another plugin adds, in the same widget, so the match is by containing, not by equal text.
	 */
	synchronized Match find(String widgetText)
	{
		if (widgetText == null)
		{
			return null;
		}
		Match newest = null;
		for (final Map.Entry<String, List<HoverCard>> entry : lines.entrySet())
		{
			final int at = widgetText.indexOf(entry.getKey());
			if (at >= 0)
			{
				newest = new Match(entry.getValue(), at);
			}
		}
		return newest;
	}

	/** Forgets every line. */
	synchronized void clear()
	{
		lines.clear();
	}
}
