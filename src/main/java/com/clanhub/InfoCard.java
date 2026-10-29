package com.clanhub;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the card says about one thing a broadcast line names that is not an item: a quest, a combat achievement task, a
 * pet, a level, a diary tier, a personal best or a kill count. Pure model: it holds the title, one row of facts, the
 * notes (the description, whole), the wiki page, and the line it came from with the place of the thing's name in it; it draws
 * nothing and asks the client nothing. It is the {@link HoverCard} of these: a game sprite for its picture, or an item
 * (a pet's, the clue watch's), or none; the card of a boss, monster, raid or chest also says it wants the wiki's own
 * picture of its page ({@link #wikiPicture}), the sprite or item standing behind it until that comes.
 */
final class InfoCard implements HoverCard
{
	/** The card's last line, the item card's. */
	private static final String HINT = "Right-click: Open wiki | Copy";
	/** The wiki's address for a page, to which the page's name is added. */
	private static final String WIKI = "https://oldschool.runescape.wiki/w/";

	private final String title;
	private final List<List<Pair>> rows;
	private final List<String> notes;
	private final String page;
	private final String line;
	private final int start;
	private final int end;
	private final int spriteId;
	private final int itemId;
	private final boolean marked;
	private final boolean wikiPicture;
	private final Progress progress;

	/**
	 * The card with its title, its one row of facts, its notes (the task description, or the unlocks one to a note, for the
	 * painter to set; may be empty), the wiki page to open (the part after {@code /w/}), the plain chat line, where the
	 * thing's name starts and ends in that line, and the game sprite of its picture or -1 for none; the card has no item
	 * for a picture.
	 */
	InfoCard(String title, List<Pair> row, List<String> notes, String page, String line, int start, int end,
		int spriteId)
	{
		this(title, row, notes, page, line, start, end, spriteId, -1);
	}

	/**
	 * The same card with an item for its picture: {@code itemId} is the id of the item whose sprite it shows (a pet's, the
	 * clue watch's), or -1 for none.
	 */
	InfoCard(String title, List<Pair> row, List<String> notes, String page, String line, int start, int end,
		int spriteId, int itemId)
	{
		this(title, row, notes, page, line, start, end, spriteId, itemId, true);
	}

	/**
	 * The same card with {@code marked} said: false makes it a quiet one, whose words the chat does not colour (the level
	 * and XP phrases); the other constructors make a marked card.
	 */
	InfoCard(String title, List<Pair> row, List<String> notes, String page, String line, int start, int end,
		int spriteId, int itemId, boolean marked)
	{
		this(title, row, notes, page, line, start, end, spriteId, itemId, marked, false);
	}

	/**
	 * The fullest card: {@code wikiPicture} true makes it show the wiki's own picture of its page in the big picture box
	 * (the card of a boss, monster, raid or chest), with the item or the sprite behind it until the picture comes; the
	 * other constructors make a card that does not.
	 */
	InfoCard(String title, List<Pair> row, List<String> notes, String page, String line, int start, int end,
		int spriteId, int itemId, boolean marked, boolean wikiPicture)
	{
		this(title, row, Collections.<Pair>emptyList(), null, notes, page, line, start, end, spriteId, itemId, marked,
			wikiPicture);
	}

	/**
	 * The slayer task's card: the fullest card with its drops and its progress. {@code drops} are the card's own drops
	 * line, each pair a source (a drop's name) and its rate, as an item card's drop sources are (the rows after the first);
	 * {@code progress} is the bar under the title, or null for none.
	 */
	InfoCard(String title, List<Pair> row, List<Pair> drops, Progress progress, List<String> notes, String page, String line,
		int start, int end, int spriteId, int itemId, boolean marked, boolean wikiPicture)
	{
		this.progress = progress;
		this.wikiPicture = wikiPicture;
		this.marked = marked;
		this.itemId = itemId;
		this.title = title;
		final List<List<Pair>> rows = new ArrayList<>();
		rows.add(Collections.unmodifiableList(new ArrayList<>(row)));
		for (final Pair drop : drops)
		{
			rows.add(Collections.singletonList(drop));
		}
		this.rows = Collections.unmodifiableList(rows);
		this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
		this.page = page;
		this.line = line;
		this.start = start;
		this.end = end;
		this.spriteId = spriteId;
	}

	/**
	 * The wiki page for a name: spaces as underscores, then percent-encoded as UTF-8 so an apostrophe, an ampersand or a
	 * bracket reaches the wiki whole ("Lumbridge & Draynor Diary" is "Lumbridge_%26_Draynor_Diary"); a dot is left as it
	 * is, as the wiki's own addresses leave it.
	 */
	static String pageFor(String name)
	{
		return URLEncoder.encode(name.replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "%20");
	}

	/** The card's title. */
	@Override
	public String title()
	{
		return title;
	}

	/** The card's one row of facts. */
	@Override
	public List<List<Pair>> rows()
	{
		return rows;
	}

	/** The notes under the row: the task description, or the level unlocks one to a note, set by the painter as a
	 * paragraph or a numbered list; empty for most kinds. */
	@Override
	public List<String> notes()
	{
		return notes;
	}

	/** The card's last line: what a right-click does, the same as the item card's. */
	@Override
	public String hint()
	{
		return HINT;
	}

	/** The wiki's page for the thing the card is about. */
	@Override
	public String wikiUrl()
	{
		return WIKI + page;
	}

	/**
	 * The wiki page's title for the card's paragraph: the page with its percent-encoding undone and its underscores read
	 * as spaces ("Dragon_Slayer_II" is "Dragon Slayer II", "Why_Cook%3F" is "Why Cook?").
	 *
	 * <p>Choice: a page whose percent-encoding is malformed keeps it as written, with its underscores read as spaces.
	 */
	@Override
	public String wikiTitle()
	{
		try
		{
			return URLDecoder.decode(page, StandardCharsets.UTF_8).replace('_', ' ');
		}
		catch (IllegalArgumentException e)
		{
			return page.replace('_', ' ');
		}
	}

	/** The chat line as plain text, what "Copy" puts on the clipboard. */
	@Override
	public String plainLine()
	{
		return line;
	}

	/** Where the thing's name starts in the line. */
	@Override
	public int start()
	{
		return start;
	}

	/** Just past the thing's name in the line. */
	@Override
	public int end()
	{
		return end;
	}

	/** The id of the item whose sprite is the card's picture (a pet's, the clue watch's), or -1 when it has none. */
	@Override
	public int itemId()
	{
		return itemId;
	}

	/** No item, so no stack: 1. */
	@Override
	public int quantity()
	{
		return 1;
	}

	/** The game sprite of the card's picture, or -1 when it has none. */
	@Override
	public int spriteId()
	{
		return spriteId;
	}

	/** Whether the card shows the wiki's own picture of its page: true for a boss, monster, raid or chest card. */
	@Override
	public boolean wikiPicture()
	{
		return wikiPicture;
	}

	/** The slayer task's progress, painted as a bar under the title; null for every other kind of card. */
	@Override
	public Progress progress()
	{
		return progress;
	}

	/** Whether the card's words are coloured in the chat; false for the quiet level and XP cards. */
	@Override
	public boolean marked()
	{
		return marked;
	}
}
