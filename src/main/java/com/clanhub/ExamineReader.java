package com.clanhub;

import com.clanhub.ClanHubConfig.ExamineLine;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.util.Text;

/**
 * The reader of the player's own examine lines: the text the game writes when the player examines an item or a monster, a
 * card for the thing examined, its name or its whole text the hover words. The text names nothing ("A pristine mole skin." is what an
 * immaculate mole skin says), so the line alone cannot give the card. RuneLite tells a plugin what the player clicked
 * Examine on, as its own Examine plugin learns what to price: the plugin passes each such click to {@link #clicked}, this
 * reader remembers the one thing last examined and the game tick it was clicked on, and the examine line that follows
 * within {@link #FRESH_TICKS} ticks gets that thing's card. An item gives the {@link ItemCards} card of its id (a noted
 * item or a placeholder gives the item itself); a monster gives the marked card of its name, with the wiki's own picture
 * and, for a boss the {@link CardTables} know, its facts. An object gives none.
 *
 * <p>Choice: the text names nothing, so with the "Examine line" setting at "Item name first" (the default) the plugin
 * writes the thing's name into the line before the readers see it, "Law rune: Used for teleport spells.", and {@link
 * #read} then covers the name alone, so the hover words are the name and the description stays plain beside it; at
 * "Description only" the line stays as the game wrote it and the whole text is the hover words. {@link #label} makes the
 * line as it should read, and remembers and forgets nothing, so {@link #read} still answers the click.
 *
 * <p>Choice: one click gives one card. The thing is forgotten once its line has been read, because RuneLite's Examine plugin
 * writes a price line of its own after the game's, and that line is not an examine line of the player's.
 *
 * <p>Choice: only the player's own examine lines are read, the {@code ITEM_EXAMINE} and {@code NPC_EXAMINE} types, and only
 * the line of the kind that was clicked; any other line, and a line of the other kind, gives nothing and forgets nothing.
 *
 * <p>Choice: the click and the chat line both arrive on the client thread, so the reader keeps its one remembered thing
 * without a lock.
 */
final class ExamineReader implements LineReader
{
	/** How many game ticks after the click an examine line still counts as its answer. */
	static final int FRESH_TICKS = 2;

	/** The game's non-breaking space, which a menu target may carry in a name. */
	private static final char NBSP = '\u00a0';
	/** What stands between the name written in front of an examine line and the text. */
	private static final String NAME_END = ": ";
	/** The menu option of an examine click, tags removed. */
	private static final String EXAMINE = "Examine";
	/** " (level-732)": the level a monster's menu target ends with. */
	private static final Pattern LEVEL = Pattern.compile("\\s*\\(level-\\d+\\)\\s*$");

	/** What the last examine click was on. */
	private enum Kind
	{
		NONE,
		ITEM,
		MONSTER
	}

	private final ItemCards itemCards;
	private final CardTables tables;
	private final BooleanSupplier on;
	private final IntSupplier tick;
	private final Supplier<ExamineLine> style;

	/** The kind of thing last examined; {@link Kind#NONE} when nothing is remembered. */
	private Kind kind = Kind.NONE;
	/** The id of the item last examined, while {@link #kind} is an item. */
	private int itemId;
	/** The name of the monster last examined, while {@link #kind} is a monster. */
	private String monster = "";
	/** The game tick the remembered click was made on. */
	private int clickedAt;

	/**
	 * @param itemCards makes the item card
	 * @param tables the bosses' facts, which may not have loaded yet
	 * @param on whether the "Examine cards" setting is on, asked for each line
	 * @param tick the client's game tick count, asked for each click and each line
	 * @param style how an examine line reads, asked for each line
	 */
	ExamineReader(ItemCards itemCards, CardTables tables, BooleanSupplier on, IntSupplier tick, Supplier<ExamineLine> style)
	{
		this.itemCards = itemCards;
		this.tables = tables;
		this.on = on;
		this.tick = tick;
		this.style = style;
	}

	/** Remembers that the item with this id was just examined, and when; it replaces whatever was remembered. */
	void itemExamined(int id)
	{
		kind = Kind.ITEM;
		itemId = id;
		clickedAt = tick.getAsInt();
	}

	/** Remembers that the monster with this name was just examined, and when; it replaces whatever was remembered. */
	void npcExamined(String name)
	{
		kind = Kind.MONSTER;
		monster = name;
		clickedAt = tick.getAsInt();
	}

	/** Forgets what was remembered: the player examined something that gives no card, an object. */
	void otherExamined()
	{
		kind = Kind.NONE;
	}

	/**
	 * The line as it should read: with "Examine cards" on, the style {@link ExamineLine#NAME_FIRST} and a line that answers
	 * the click remembered (the kind and the freshness {@link #read} asks for), the thing's name, ": " and the message as it
	 * came, such as "Law rune: Used for teleport spells." (an item's name is its canonical item's, {@link
	 * ItemCards#nameOf}); otherwise the message unchanged, as it is too when the thing has no name. It remembers and forgets
	 * nothing. A null message gives null.
	 */
	String label(ChatMessageType type, String message)
	{
		if (message == null || !on.getAsBoolean() || style.get() != ExamineLine.NAME_FIRST || !asked(type))
		{
			return message;
		}
		final String name = examinedName();
		return name == null || name.isEmpty() ? message : name + NAME_END + message;
	}

	/** The name of the thing remembered: the item's, null when it has none, or the monster's. */
	private String examinedName()
	{
		return kind == Kind.ITEM ? itemCards.nameOf(itemId) : monster;
	}

	/**
	 * The card for the thing last examined, or none. With "Examine cards" off, nothing. An {@code ITEM_EXAMINE} line while
	 * an item is remembered from at most {@link #FRESH_TICKS} ticks ago gives the item's card, and an {@code NPC_EXAMINE}
	 * line while a monster is remembered from as recently gives the monster's; the thing is then forgotten. The card's
	 * words are the name alone when the text starts with the thing's name and ": " (the line {@link #label} wrote), else the
	 * whole text. A remembered thing from the future (the tick count went back, as at a new login) or older than the window
	 * gives nothing.
	 *
	 * <p>Choice: a monster the table lacks, or a table that has not loaded, still gives its card: no facts, and the wiki's
	 * picture, which every name is asked for.
	 */
	@Override
	public List<HoverCard> read(ChatMessageType type, String message)
	{
		if (message == null || !on.getAsBoolean() || !asked(type))
		{
			return Collections.emptyList();
		}
		final String plain = ChatText.plain(message);
		if (plain.isEmpty())
		{
			return Collections.emptyList();
		}
		final Kind examined = kind;
		final String name = examinedName();
		final int end = name != null && !name.isEmpty() && plain.startsWith(name + NAME_END) ? name.length() : plain.length();
		kind = Kind.NONE;
		final HoverCard card;
		if (examined == Kind.ITEM)
		{
			card = itemCards.forId(itemId, 1, plain, 0, end);
		}
		else
		{
			final CardTables.BossInfo info = tables.boss(monster);
			card = new InfoCard(monster, BossCards.facts(info), Collections.<String>emptyList(),
				BossCards.page(info, monster), plain, 0, end, SkillSprites.spriteOf("Combat"), -1, true, true);
		}
		return card == null ? Collections.<HoverCard>emptyList() : Collections.singletonList(card);
	}

	/** Whether a line of this type answers the click remembered: the kind matches and the click is fresh. */
	private boolean asked(ChatMessageType type)
	{
		final boolean matches = kind == Kind.ITEM && type == ChatMessageType.ITEM_EXAMINE
			|| kind == Kind.MONSTER && type == ChatMessageType.NPC_EXAMINE;
		if (!matches)
		{
			return false;
		}
		final int age = tick.getAsInt() - clickedAt;
		return age >= 0 && age <= FRESH_TICKS;
	}

	/**
	 * What the plugin calls on every menu click: an Examine click is told to the reader, any other click is left alone. Only
	 * an option whose words, tags removed, are "Examine" counts. An examine of an item on the ground or in a widget the game
	 * sends as an item action gives the id of the click; of a monster, its name, from the menu entry's NPC when it has one,
	 * else from the menu target with its tags and its trailing "(level-N)" cut; of an object, nothing is remembered and what
	 * was remembered is forgotten. Any other action that carries an item (the inventory, the bank, the worn items, which the
	 * game sends as a widget option) is an item examine too.
	 *
	 * <p>Choice: an Examine click that names nothing the reader can use (a monster with no name, an action with no item)
	 * forgets what was remembered, as an object does, since it is a newer examine than the one remembered.
	 */
	static void clicked(ExamineReader reader, MenuOptionClicked event)
	{
		final String option = event.getMenuOption();
		if (option == null || !EXAMINE.equals(Text.removeTags(option).replace(NBSP, ' ').trim()))
		{
			return;
		}
		final MenuAction action = event.getMenuAction();
		if (action == MenuAction.EXAMINE_ITEM_GROUND)
		{
			reader.itemExamined(event.getId());
		}
		else if (action == MenuAction.EXAMINE_NPC)
		{
			final String name = monsterName(event);
			if (name.isEmpty())
			{
				reader.otherExamined();
			}
			else
			{
				reader.npcExamined(name);
			}
		}
		else if (action == MenuAction.EXAMINE_OBJECT || event.getItemId() < 0)
		{
			reader.otherExamined();
		}
		else
		{
			reader.itemExamined(event.getItemId());
		}
	}

	/**
	 * The name of the monster an examine click was on: the menu entry's NPC's name when it has one, else the menu target
	 * without its tags and its trailing " (level-N)"; trimmed, with a non-breaking space read as a space; empty for none.
	 */
	private static String monsterName(MenuOptionClicked event)
	{
		final MenuEntry entry = event.getMenuEntry();
		final NPC npc = entry == null ? null : entry.getNpc();
		final String named = npc == null ? null : npc.getName();
		if (named != null)
		{
			return Text.removeTags(named).replace(NBSP, ' ').trim();
		}
		final String target = event.getMenuTarget();
		if (target == null)
		{
			return "";
		}
		return LEVEL.matcher(Text.removeTags(target).replace(NBSP, ' ')).replaceFirst("").trim();
	}
}
