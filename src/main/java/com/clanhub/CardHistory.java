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
 *
 * <p>Choice: one row per event. The same broadcast reaches the clan, guest clan, group and friends channels, and the
 * player's own game line for a level-up or a drop comes with its clan broadcast a second apart, so a line whose key
 * (who it is about, the player reading as "you", and the first card's title) equals that of an entry added within
 * {@link #SAME_EVENT_MS} of it adds nothing; ten seconds holds them all, and a real second event of the same thing by
 * the same player within ten seconds is not a case worth a second row. Every chat line still gets its cards in the
 * chat: only the history keeps one row.
 */
final class CardHistory
{
	/** How many lines the history keeps at most: a safety cap, as the history is meant to hold the whole session. */
	static final int CAP = 5000;

	/** How close in time, either way, two lines of the same key are one event: ten seconds. */
	static final long SAME_EVENT_MS = 10_000L;

	/** What {@link #ago} says under a minute, and the word it ends with after a minute or an hour. */
	private static final String JUST_NOW = "just now";
	private static final String AGO_WORD = " ago";

	/** One chat line that gave a card: the card, the line's plain text and when it arrived. */
	static final class Entry
	{
		private final HoverCard card;
		private final String line;
		private final long at;
		/** Who the line is about and the card's title: what {@link CardHistory#add} compares to keep one row per event. */
		private final String key;

		private Entry(HoverCard card, String line, long at, String key)
		{
			this.card = card;
			this.line = line;
			this.at = at;
			this.key = key;
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
	 * nobody, and a null line is kept as an empty one. The same as {@link #add(String, List, long, String)} with no
	 * name for the player.
	 */
	void add(String line, List<HoverCard> cards, long at)
	{
		add(line, cards, at, null);
	}

	/**
	 * Keeps one entry for the line as {@link #add(String, List, long)} does, unless it is the same event as an entry
	 * already held: the same key, within {@link #SAME_EVENT_MS} of it, either way in time. Such a line adds nothing and
	 * tells nobody. The key is who the line is about ({@link RowWords#who}), read as "you" when that is "You" or when it
	 * is {@code self} once both are cleaned (a no-break space as a space, the spaces trimmed and collapsed, lower case),
	 * and the first card's title.
	 *
	 * @param self the local player's name, or null when it is not known
	 */
	void add(String line, List<HoverCard> cards, long at, String self)
	{
		if (cards == null || cards.isEmpty())
		{
			return;
		}
		final String text = line == null ? "" : line;
		final String key = keyOf(text, cards.get(0), self);
		synchronized (this)
		{
			// The newest come first, so the look stops at the first entry older than the window.
			for (final Entry entry : entries)
			{
				if (at - entry.at > SAME_EVENT_MS)
				{
					break;
				}
				if (entry.key.equals(key) && Math.abs(entry.at - at) <= SAME_EVENT_MS)
				{
					return;
				}
			}
			entries.add(0, new Entry(cards.get(0), text, at, key));
			if (entries.size() > CAP)
			{
				entries.remove(entries.size() - 1);
			}
		}
		changed();
	}

	/** What two lines share when they are one event: see {@link #add(String, List, long, String)}. */
	private static String keyOf(String line, HoverCard card, String self)
	{
		final String named = RowWords.who(line);
		final String who = cleaned(named);
		final String me = self == null ? "" : cleaned(self);
		final boolean you = RowWords.YOU.equals(named) || (!me.isEmpty() && who.equals(me));
		return (you ? "you" : who) + "|" + card.title();
	}

	/** A name as it is compared: a no-break space read as a space, the spaces trimmed and collapsed, in lower case. */
	private static String cleaned(String name)
	{
		return name.replace((char) 160, ' ').trim().replaceAll(" +", " ").toLowerCase(Locale.ROOT);
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
			return JUST_NOW;
		}
		final long minutes = seconds / 60;
		if (minutes < 60)
		{
			return minutes + "m" + AGO_WORD;
		}
		return (minutes / 60) + "h" + AGO_WORD;
	}

	/**
	 * The row's short form of {@link #ago}: "just now" reads "now", and "4m ago" or "2h ago" lose their " ago" ("4m",
	 * "2h"), which is all a row's small corner has room for.
	 */
	private static String shortAgo(String words)
	{
		return JUST_NOW.equals(words) ? "now" : words.substring(0, words.length() - AGO_WORD.length());
	}

	/**
	 * A row's time in the way the player chose: {@link CardTime#AGO} reads as {@link #ago} in its short form ("now", "4m",
	 * "2h"), the clocks as the moment the line arrived in {@code zone}, "14:32" in 24 hours or "2:32 PM" in 12 (English
	 * words, whatever the player's language).
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
				return shortAgo(ago(now, at));
		}
	}

	/**
	 * The widest readings {@link #timeOf} can give in {@code style}, each built with {@code digit} for every digit, so that
	 * a row can be measured beside the widest time it will ever show: {@link CardTime#AGO} gives "now", two digits and "m"
	 * (up to 59 minutes) and two digits and "h" (up to 99 hours; a session longer than that reads "100h", one digit wider,
	 * which this does not allow for), {@link CardTime#CLOCK_24} two digits, a colon and two digits, and
	 * {@link CardTime#CLOCK_12} the same and " AM" or " PM". The caller passes the digit its font draws widest and takes the
	 * widest of the forms in that font. It must change with the formats of {@link #timeOf}.
	 *
	 * @param style how the time reads
	 * @param digit the digit every digit of a form is built with
	 */
	static List<String> widestTimes(CardTime style, char digit)
	{
		final String two = "" + digit + digit;
		switch (style)
		{
			case CLOCK_24:
				return List.of(two + ":" + two);
			case CLOCK_12:
				return List.of(two + ":" + two + " AM", two + ":" + two + " PM");
			default:
				return List.of("now", two + "m", two + "h");
		}
	}
}
