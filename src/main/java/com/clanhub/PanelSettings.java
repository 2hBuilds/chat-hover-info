package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import java.awt.Color;
import net.runelite.client.config.Keybind;

/**
 * The settings the panel's menu shows and writes: what it reads when it opens, and what it writes when a control
 * changes. The plugin backs it with the config and the config manager; a test backs it with lists.
 */
interface PanelSettings
{
	/** Whether the hover cards show. */
	boolean showCards();

	/** Writes whether the hover cards show. */
	void setShowCards(boolean show);

	/** Whether the words a card is about are coloured in the chat. */
	boolean colourHoverables();

	/** Writes whether the words a card is about are coloured in the chat. */
	void setColourHoverables(boolean colour);

	/** Whether a card shows the first paragraph of its wiki page. */
	boolean wikiDescriptions();

	/** Writes whether a card shows the first paragraph of its wiki page. */
	void setWikiDescriptions(boolean on);

	/** Whether the card of a boss, monster, raid or chest shows the wiki's own picture of it. */
	boolean wikiPictures();

	/** Writes whether the card of a boss, monster, raid or chest shows the wiki's own picture of it. */
	void setWikiPictures(boolean on);

	/** Whether a drop line that says where the drop came from gives the boss, monster, raid or chest a card of its own. */
	boolean bossCards();

	/** Writes whether a drop line that says where the drop came from gives the boss, monster, raid or chest a card of its own. */
	void setBossCards(boolean on);

	/** When a card opens. */
	CardTrigger cardTrigger();

	/** Writes when a card opens. */
	void setCardTrigger(CardTrigger trigger);

	/** The key to hold when a card opens only while a key is held. */
	Keybind cardKey();

	/** Writes the key to hold when a card opens only while a key is held. */
	void setCardKey(Keybind key);

	/** Where a card opens. */
	CardPlace cardPlace();

	/** Writes where a card opens. */
	void setCardPlace(CardPlace place);

	/** How wide the card at the cursor may grow. */
	CardWidth cardWidth();

	/** Writes how wide the card at the cursor may grow. */
	void setCardWidth(CardWidth width);

	/** Whether the level and XP phrases that have a card carry a line under them at all times. */
	boolean alwaysUnderlineLevels();

	/** Writes whether the level and XP phrases that have a card carry a line under them at all times. */
	void setAlwaysUnderlineLevels(boolean on);

	/** The colour of the words a card is about, and of the underline under the ones hovered. */
	Color hoverColour();

	/** Writes the colour of the words a card is about, and of the underline under the ones hovered. */
	void setHoverColour(Color colour);

	/** The colour of the line kept under the quiet phrases. */
	Color alwaysUnderlineColour();

	/** Writes the colour of the line kept under the quiet phrases. */
	void setAlwaysUnderlineColour(Color colour);

	/** How long the mouse rests on the words before the card opens, in milliseconds. */
	int cardDelayMs();

	/** Writes how long the mouse rests on the words before the card opens, in milliseconds. */
	void setCardDelayMs(int delayMs);

	/** The card text's size, in pixels. */
	int cardFontSize();

	/** Writes the card text's size, in pixels. */
	void setCardFontSize(int size);
}
