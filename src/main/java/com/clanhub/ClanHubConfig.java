package com.clanhub;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

/**
 * The plugin's settings. Flat on purpose: every item is declared here and this interface extends {@link Config}
 * alone, because RuneLite stores defaults only for the methods declared on the interface it is given. The group and
 * the keys are frozen at first release; renaming one silently discards every player's setting.
 */
@ConfigGroup(ClanHubConfig.GROUP)
public interface ClanHubConfig extends Config
{
	String GROUP = "chat-hover-info";

	/** When a card opens: as soon as the mouse is on its words, only while a key is held, or when the words are clicked. */
	enum CardTrigger
	{
		HOVER("On hover"),
		HOLD_KEY("While holding a key"),
		CLICK("On click");

		private final String text;

		CardTrigger(String text)
		{
			this.text = text;
		}

		/** The words the settings show for the choice. */
		@Override
		public String toString()
		{
			return text;
		}
	}

	/** Where a card opens: centred above the cursor as the split card, or as wide as the chatbox and pinned above it. */
	enum CardPlace
	{
		CURSOR("Above the cursor"),
		CHAT("Above the chat");

		private final String text;

		CardPlace(String text)
		{
			this.text = text;
		}

		/** The words the settings show for the choice. */
		@Override
		public String toString()
		{
			return text;
		}
	}

	/**
	 * How wide the card at the cursor may grow, and how many lines its paragraph may take: {@link #cap} is its greatest
	 * width at the 16 px text size (scaled with the text size), {@link #lines} the most paragraph lines. Short cards size
	 * to their content in every choice.
	 */
	enum CardWidth
	{
		WIDE("Wide", 480, 3),
		MEDIUM("Medium", 409, 5),
		NARROW("Narrow", 391, 5);

		private final String text;
		/** The card's greatest width at the 16 px text size, in px. */
		final int cap;
		/** The most lines the paragraph takes. */
		final int lines;

		CardWidth(String text, int cap, int lines)
		{
			this.text = text;
			this.cap = cap;
			this.lines = lines;
		}

		/** The words the settings show for the choice. */
		@Override
		public String toString()
		{
			return text;
		}
	}

	/**
	 * The look of the cards: Classic is the plain brown card the plugin first shipped, Stone the game's own stone frame
	 * with its orange title and yellow values, Void a purple card with stars, Wiki the wiki's own infobox, and the eight
	 * after it (Nebula to Cosmic edge) dark cards with a picture of space; {@link SpaceSky} paints those. The twelve after
	 * those (Forest to Lavender) are scenes: nature and craft pictures, which {@link SceneSky} paints. {@link CardStyle}
	 * holds what each paints with.
	 */
	enum CardTheme
	{
		CLASSIC("Classic"),
		STONE("Stone"),
		VOID("Void"),
		WIKI("Wiki"),
		NEBULA("Nebula"),
		AURORA("Aurora"),
		GALAXY("Galaxy"),
		PLANET("Planet rise"),
		ECLIPSE("Eclipse"),
		CONSTELLATION("Constellation"),
		BLACK_HOLE("Black hole"),
		COSMIC("Cosmic edge"),
		FOREST("Forest"),
		OCEAN("Ocean"),
		AUTUMN("Autumn"),
		SAKURA("Sakura"),
		STEAMPUNK("Steampunk"),
		CYBERPUNK("Cyberpunk"),
		SYNTHWAVE("Synthwave"),
		VOLCANO("Volcano"),
		RUNIC("Runic"),
		SPOOKY("Spooky"),
		GARDEN("Garden"),
		LAVENDER("Lavender");

		private final String text;

		CardTheme(String text)
		{
			this.text = text;
		}

		/** The words the settings show for the choice. */
		@Override
		public String toString()
		{
			return text;
		}
	}

	/**
	 * The look of the sidebar: the card theme's own ({@link #SAME}), RuneLite's own greys ({@link #RUNELITE}), or a theme of
	 * its own, one value for each {@link CardTheme} in the same order and with the same words. {@link #card} names the card
	 * theme whose look the sidebar then wears, or is null for the first two.
	 */
	enum SidebarTheme
	{
		SAME("Same as cards", null),
		RUNELITE("RuneLite", null),
		CLASSIC("Classic", CardTheme.CLASSIC),
		STONE("Stone", CardTheme.STONE),
		VOID("Void", CardTheme.VOID),
		WIKI("Wiki", CardTheme.WIKI),
		NEBULA("Nebula", CardTheme.NEBULA),
		AURORA("Aurora", CardTheme.AURORA),
		GALAXY("Galaxy", CardTheme.GALAXY),
		PLANET("Planet rise", CardTheme.PLANET),
		ECLIPSE("Eclipse", CardTheme.ECLIPSE),
		CONSTELLATION("Constellation", CardTheme.CONSTELLATION),
		BLACK_HOLE("Black hole", CardTheme.BLACK_HOLE),
		COSMIC("Cosmic edge", CardTheme.COSMIC),
		FOREST("Forest", CardTheme.FOREST),
		OCEAN("Ocean", CardTheme.OCEAN),
		AUTUMN("Autumn", CardTheme.AUTUMN),
		SAKURA("Sakura", CardTheme.SAKURA),
		STEAMPUNK("Steampunk", CardTheme.STEAMPUNK),
		CYBERPUNK("Cyberpunk", CardTheme.CYBERPUNK),
		SYNTHWAVE("Synthwave", CardTheme.SYNTHWAVE),
		VOLCANO("Volcano", CardTheme.VOLCANO),
		RUNIC("Runic", CardTheme.RUNIC),
		SPOOKY("Spooky", CardTheme.SPOOKY),
		GARDEN("Garden", CardTheme.GARDEN),
		LAVENDER("Lavender", CardTheme.LAVENDER);

		private final String text;
		private final CardTheme card;

		SidebarTheme(String text, CardTheme card)
		{
			this.text = text;
			this.card = card;
		}

		/** The card theme this look wears, or null for "Same as cards" and "RuneLite". */
		CardTheme card()
		{
			return card;
		}

		/** The words the settings show for the choice. */
		@Override
		public String toString()
		{
			return text;
		}
	}

	/**
	 * How an examine line you asked for reads: with the thing's name written in front of its text, the name alone the
	 * hover words, or the text alone, all of it the hover words.
	 */
	enum ExamineLine
	{
		NAME_FIRST("Item name first"),
		DESCRIPTION("Description only");

		private final String text;

		ExamineLine(String text)
		{
			this.text = text;
		}

		/** The words the settings show for the choice. */
		@Override
		public String toString()
		{
			return text;
		}
	}

	/**
	 * How a recent card's time reads in the sidebar: how long ago, or the clock time of the player's own zone in 24 or
	 * 12 hours. The words are an example of each.
	 */
	enum CardTime
	{
		AGO("4m ago"),
		CLOCK_24("14:32"),
		CLOCK_12("2:32 PM");

		private final String text;

		CardTime(String text)
		{
			this.text = text;
		}

		/** The words the settings show for the choice. */
		@Override
		public String toString()
		{
			return text;
		}
	}

	/** Whether the item card shows when the mouse is on a recognised chat line. */
	@ConfigItem(
		keyName = "showCards",
		name = "Show hover cards",
		description = "Show a card at the mouse when you hover an item in a clan or friends chat line.",
		position = 1
	)
	default boolean showCards()
	{
		return true;
	}

	/** The size of the card's text, in pixels. */
	@ConfigItem(
		keyName = "cardFontSize",
		name = "Card text size",
		description = "The size of the card's text, in pixels.",
		position = 2
	)
	@Range(min = 8, max = 24)
	default int cardFontSize()
	{
		return 16;
	}

	/** Whether the words a card is about are coloured in the chat line. */
	@ConfigItem(
		keyName = "colourHoverables",
		name = "Colour hoverable words",
		description = "Colour the item, quest, achievement and pet names the cards are about in the chat.",
		position = 3
	)
	default boolean colourHoverables()
	{
		return true;
	}

	/** The colour of the words a card is about, and of the underline under the ones hovered. */
	@ConfigItem(
		keyName = "hoverColour",
		name = "Hover colour",
		description = "The colour of the words a card is about, and of the underline under the ones you hover.",
		position = 4
	)
	default Color hoverColour()
	{
		return new Color(59, 130, 246);
	}

	/** When a card opens. */
	@ConfigItem(
		keyName = "cardTrigger",
		name = "Show a card",
		description = "When a card opens: as soon as you hover, only while you hold the key below, or when you click the words.",
		position = 5
	)
	default CardTrigger cardTrigger()
	{
		return CardTrigger.HOVER;
	}

	/** The key held when the card opens only while a key is held. */
	@ConfigItem(
		keyName = "cardKey",
		name = "Card key",
		description = "The key to hold when 'Show a card' is 'While holding a key'.",
		position = 6
	)
	default Keybind cardKey()
	{
		return Keybind.ALT;
	}

	/** How long the mouse rests on the words before the card opens, in milliseconds. */
	@ConfigItem(
		keyName = "cardDelayMs",
		name = "Card delay",
		description = "How long the mouse rests on the words before the card opens.",
		position = 7
	)
	@Range(min = 0, max = 1000)
	@Units(Units.MILLISECONDS)
	default int cardDelayMs()
	{
		return 0;
	}

	/** Whether a card shows the first paragraph of its wiki page, which asks the wiki for it. */
	@ConfigItem(
		keyName = "wikiDescriptions",
		name = "Wiki descriptions",
		description = "Show the first paragraph of the wiki page on each card. Sends only the thing's name to oldschool.runescape.wiki.",
		position = 8
	)
	default boolean wikiDescriptions()
	{
		return true;
	}

	/** Whether the card of a boss, monster, raid or chest shows the wiki's own picture of it, which asks the wiki for it. */
	@ConfigItem(
		keyName = "wikiPictures",
		name = "Wiki pictures",
		description = "Show the wiki's own picture of a boss, monster, raid or chest on its card. Sends only the thing's name to oldschool.runescape.wiki, then fetches the picture the wiki names.",
		position = 9
	)
	default boolean wikiPictures()
	{
		return true;
	}

	/** Whether a drop line that says where the drop came from gives the boss, monster, raid or chest a card of its own. */
	@ConfigItem(
		keyName = "bossCards",
		name = "Boss cards",
		description = "On a drop line that says where the drop came from, give the boss, monster, raid or chest its own card, with the wiki's first paragraph.",
		position = 10
	)
	default boolean bossCards()
	{
		return true;
	}

	/** Whether the examine text of an item or a monster you examined gives a card. */
	@ConfigItem(
		keyName = "examineCards",
		name = "Examine cards",
		description = "Hover the text of an item or monster you examined for its card.",
		position = 11
	)
	default boolean examineCards()
	{
		return true;
	}

	/** How an examine line you asked for reads: the thing's name in front of its text, or the text alone. */
	@ConfigItem(
		keyName = "examineLine",
		name = "Examine line",
		description = "Put the examined item's or monster's name in front of its examine text, and hover the name; or keep the text alone and hover all of it.",
		position = 12
	)
	default ExamineLine examineLine()
	{
		return ExamineLine.NAME_FIRST;
	}

	/** Where a card opens. */
	@ConfigItem(
		keyName = "cardPlace",
		name = "Card place",
		description = "Where a card opens: centred above the cursor, or as wide as the chatbox and pinned above it.",
		position = 13
	)
	default CardPlace cardPlace()
	{
		return CardPlace.CURSOR;
	}

	/** How wide the card at the cursor may grow. */
	@ConfigItem(
		keyName = "cardWidth",
		name = "Card width",
		description = "How wide the card at the cursor may grow: wide, or two narrower widths that give the paragraph up to five lines.",
		position = 14
	)
	default CardWidth cardWidth()
	{
		return CardWidth.MEDIUM;
	}

	/** The look of the cards. */
	@ConfigItem(
		keyName = "cardTheme",
		name = "Card theme",
		description = "The look of the cards: Classic, Stone (the game's own frame), Void, Wiki (the wiki's infobox) or one of twenty scene looks.",
		position = 15
	)
	default CardTheme cardTheme()
	{
		return CardTheme.CLASSIC;
	}

	/** The look of the sidebar: the cards' theme, RuneLite's own, or a theme of its own. */
	@ConfigItem(
		keyName = "sidebarLook",
		name = "Sidebar theme",
		description = "The look of the sidebar: the cards' theme, RuneLite's own, or a theme of its own.",
		position = 16
	)
	default SidebarTheme sidebarLook()
	{
		return SidebarTheme.SAME;
	}

	/** How a recent card's time reads in the sidebar. */
	@ConfigItem(
		keyName = "cardTime",
		name = "Card time",
		description = "How a recent card's time reads in the sidebar: how long ago, or the clock time in 24 or 12 hours.",
		position = 17
	)
	default CardTime cardTime()
	{
		return CardTime.AGO;
	}

	/** The colour of the player's name in the sidebar's recent cards: the 2h orange, the brand's own. */
	@ConfigItem(
		keyName = "nameColour",
		name = "Name colour",
		description = "The colour of the player's name in the sidebar's recent cards.",
		position = 18
	)
	default Color nameColour()
	{
		return new Color(220, 138, 0);
	}

	/** Whether the level and XP phrases that have a card carry a line under them at all times. */
	@ConfigItem(
		keyName = "alwaysUnderlineLevels",
		name = "Underline level-ups",
		description = "Keep a line under the level and XP phrases that have a card, whether or not the mouse is on them.",
		position = 19
	)
	default boolean alwaysUnderlineLevels()
	{
		return false;
	}

	/** The colour of the line kept under the quiet phrases. */
	@ConfigItem(
		keyName = "alwaysUnderlineColour",
		name = "Underline colour",
		description = "The colour of that line.",
		position = 20
	)
	default Color alwaysUnderlineColour()
	{
		return new Color(150, 150, 150);
	}

	/** How much frosted glass sits behind the words on picture themes, in percent: 0 none (unless set), 100 the full band. */
	@ConfigItem(
		keyName = "frostOpacity",
		name = "Blur behind text",
		description = "Softens the picture behind the words so they're easier to read. 0 is off, 100 is the most.",
		position = 21
	)
	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	default int frostOpacity()
	{
		return 0;
	}

	/**
	 * The picture theme the cards wear, by id, or empty for the original theme in {@link #cardTheme}. The settings menu's own
	 * memory (RuneLite's settings page cannot list the picture themes), hidden from the settings page, so the settings stay
	 * twenty; the Card theme setting keeps the player's original theme under it.
	 */
	@ConfigItem(
		keyName = "pictureTheme",
		name = "Picture theme",
		description = "The picture theme of the cards, set from the settings menu in the sidebar.",
		position = 98,
		hidden = true
	)
	default String pictureTheme()
	{
		return "";
	}

	/** The picture theme the sidebar wears, by id, or empty for the look in {@link #sidebarLook}; hidden, as {@link #pictureTheme} is. */
	@ConfigItem(
		keyName = "sidebarPicture",
		name = "Sidebar picture theme",
		description = "The picture theme of the sidebar, set from the settings menu in the sidebar.",
		position = 99,
		hidden = true
	)
	default String sidebarPicture()
	{
		return "";
	}

	/**
	 * Whether the sidebar's "How to use" box is folded to its header. The panel's own memory, not a setting the player
	 * sees: hidden from RuneLite's settings page, so the settings stay twenty.
	 */
	@ConfigItem(
		keyName = "howToFolded",
		name = "How to use folded",
		description = "Whether the sidebar's How to use box is folded.",
		hidden = true
	)
	default boolean howToFolded()
	{
		return false;
	}
}
