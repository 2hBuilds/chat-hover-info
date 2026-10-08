package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTrigger;

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

	/** When a card opens. */
	CardTrigger cardTrigger();

	/** Writes when a card opens. */
	void setCardTrigger(CardTrigger trigger);

	/** Where a card opens. */
	CardPlace cardPlace();

	/** Writes where a card opens. */
	void setCardPlace(CardPlace place);

	/** Whether the level, total level and XP phrases that have a card carry a line under them at all times. */
	boolean alwaysUnderlineLevels();

	/** Writes whether the level, total level and XP phrases that have a card carry a line under them at all times. */
	void setAlwaysUnderlineLevels(boolean on);

	/** How long the mouse rests on the words before the card opens, in milliseconds. */
	int cardDelayMs();

	/** Writes how long the mouse rests on the words before the card opens, in milliseconds. */
	void setCardDelayMs(int delayMs);

	/** The card text's size, in pixels. */
	int cardFontSize();

	/** Writes the card text's size, in pixels. */
	void setCardFontSize(int size);
}
