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

	/** Where a card opens. */
	@ConfigItem(
		keyName = "cardPlace",
		name = "Card place",
		description = "Where a card opens: centred above the cursor, or as wide as the chatbox and pinned above it.",
		position = 9
	)
	default CardPlace cardPlace()
	{
		return CardPlace.CURSOR;
	}

	/** Whether the level, total level and XP phrases that have a card carry a line under them at all times. */
	@ConfigItem(
		keyName = "alwaysUnderlineLevels",
		name = "Always underline level-ups",
		description = "Keep a line under the level, total level and XP phrases that have a card, whether or not the mouse is on them.",
		position = 10
	)
	default boolean alwaysUnderlineLevels()
	{
		return false;
	}

	/** The colour of the line kept under the quiet phrases. */
	@ConfigItem(
		keyName = "alwaysUnderlineColour",
		name = "Always underline colour",
		description = "The colour of that line.",
		position = 11
	)
	default Color alwaysUnderlineColour()
	{
		return new Color(150, 150, 150);
	}
}
