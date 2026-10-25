package com.clanhub;

import com.clanhub.ClanHubConfig.CardTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The recent cards: the chat lines that gave a card over the whole session, newest first, each with the first card its
 * line gave, up to {@link #CAP} as a safety stop. The plugin adds a line as it finds its cards; the sidebar lists the
 * entries and listens for a change. Kept for the session only and written nowhere. Pure model: it holds lines, cards and
 * times and asks the client nothing.
 *
 * <p>Choice: every method is synchronized on the history itself, because a line arrives on the client thread and the
 * sidebar reads on the event thread; the listeners are told after the lock is let go, on the thread that changed the
 * history, so a listener never holds the lock while it works.
 */
final class CardHistory
{
	/** How many lines the history keeps at most: a safety cap, as the history is meant to hold the whole session. */
	static final int CAP = 5000;

	/** One chat line that gave a card: the card, the line's plain text and when it arrived. */
	static final class Entry
	{
		private final HoverCard card;
		private final String line;
		private final long at;

		private Entry(HoverCard card, String line, long at)
		{
			this.card = card;
			this.line = line;
			this.at = at;
		}

		/** The first card the line gave. */
		HoverCard card()
		{
			return card;
		}

		/** The line's plain text, without its colour tags. */
		String line()
		{
			return line;
		}

		/** When the line arrived, in milliseconds since the epoch. */
		long at()
		{
			return at;
		}
	}

	/** The entries, the newest first. */
	private final List<Entry> entries = new ArrayList<>();
	/** What is told after every change. */
	private final List<Runnable> listeners = new ArrayList<>();

	/**
	 * Keeps one entry for the line, holding its first card, as the newest; the oldest entry goes when more than
	 * {@link #CAP} are held. Every listener is told afterwards. A null or empty list of cards adds nothing and tells
	 * nobody, and a null line is kept as an empty one.
	 */
	void add(String line, List<HoverCard> cards, long at)
	{
		if (cards == null || cards.isEmpty())
		{
			return;
		}
		synchronized (this)
		{
			entries.add(0, new Entry(cards.get(0), line == null ? "" : line, at));
			if (entries.size() > CAP)
			{
				entries.remove(entries.size() - 1);
			}
		}
		changed();
	}

	/** A copy of the entries, the newest first. */
	synchronized List<Entry> entries()
	{
		return new ArrayList<>(entries);
	}

	/** How many entries are held. */
	synchronized int size()
	{
		return entries.size();
	}

	/** Empties the history and tells every listener, whether or not it held anything. */
	void clear()
	{
		synchronized (this)
		{
			entries.clear();
		}
		changed();
	}

	/** Has {@code listener} told after every {@link #add} that kept a line and every {@link #clear}, on the changing thread. */
	synchronized void listen(Runnable listener)
	{
		listeners.add(listener);
	}

	/** Tells every listener, on the caller's thread, with no lock held. */
	private void changed()
	{
		final List<Runnable> told;
		synchronized (this)
		{
			told = new ArrayList<>(listeners);
		}
		for (final Runnable listener : told)
		{
			listener.run();
		}
	}

	/**
	 * How long ago a line arrived, in the words a row shows: "just now" under a minute, "Nm ago" under an hour, else
	 * "Nh ago" (hours, however many). A time in the future, a clock set back, reads as "just now".
	 *
	 * @param now the present, in milliseconds since the epoch
	 * @param then when the line arrived, in the same terms
	 */
	static String ago(long now, long then)
	{
		final long seconds = Math.max(0L, (now - then) / 1000L);
		if (seconds < 60)
		{
			return "just now";
		}
		final long minutes = seconds / 60;
		if (minutes < 60)
		{
			return minutes + "m ago";
		}
		return (minutes / 60) + "h ago";
	}

	/**
	 * A row's time in the way the player chose: {@link CardTime#AGO} reads as {@link #ago}, the clocks as the moment the
	 * line arrived in {@code zone}, "14:32" in 24 hours or "2:32 PM" in 12 (English words, whatever the player's language).
	 *
	 * @param at when the line arrived, in milliseconds since the epoch
	 * @param style how the time reads
	 * @param zone the zone the clocks read in: the player's own
	 * @param now the present, in the terms of {@code at}, for the "ago" words
	 */
	static String timeOf(long at, CardTime style, ZoneId zone, long now)
	{
		switch (style)
		{
			case CLOCK_24:
				return DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).format(Instant.ofEpochMilli(at).atZone(zone));
			case CLOCK_12:
				return DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).format(Instant.ofEpochMilli(at).atZone(zone));
			default:
				return ago(now, at);
		}
	}
}
