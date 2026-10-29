package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTheme;
import com.clanhub.ClanHubConfig.CardTime;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.clanhub.ClanHubConfig.ExamineLine;
import com.clanhub.ClanHubConfig.SidebarTheme;
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

	/** How much frosted glass sits behind the words on picture themes, in percent (0 to 100). */
	int frostOpacity();

	/** Writes how much frosted glass sits behind the words on picture themes, in percent. */
	void setFrostOpacity(int percent);

	/** Whether a drop line that says where the drop came from gives the boss, monster, raid or chest a card of its own. */
	boolean bossCards();

	/** Writes whether a drop line that says where the drop came from gives the boss, monster, raid or chest a card of its own. */
	void setBossCards(boolean on);

	/** Whether the examine text of an item or a monster you examined gives a card. */
	boolean examineCards();

	/** Writes whether the examine text of an item or a monster you examined gives a card. */
	void setExamineCards(boolean on);

	/** How an examine line you asked for reads: the thing's name in front of its text, or the text alone. */
	ExamineLine examineLine();

	/** Writes how an examine line you asked for reads. */
	void setExamineLine(ExamineLine line);

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

	/** The look of the cards. */
	CardTheme cardTheme();

	/** Writes the look of the cards. */
	void setCardTheme(CardTheme theme);

	/** The look of the sidebar: the cards' theme, RuneLite's own, or a theme of its own. */
	SidebarTheme sidebarLook();

	/** Writes the look of the sidebar. */
	void setSidebarLook(SidebarTheme look);

	/** The id of the picture theme the cards wear, or empty for the original theme of {@link #cardTheme}. */
	String pictureTheme();

	/** Writes the id of the picture theme the cards wear; empty for none. */
	void setPictureTheme(String id);

	/** The id of the picture theme the sidebar wears, or empty for the look of {@link #sidebarLook}. */
	String sidebarPicture();

	/** Writes the id of the picture theme the sidebar wears; empty for none. */
	void setSidebarPicture(String id);

	/** How a recent card's time reads in the sidebar. */
	CardTime cardTime();

	/** Writes how a recent card's time reads in the sidebar. */
	void setCardTime(CardTime time);

	/** Whether the level and XP phrases that have a card carry a line under them at all times. */
	boolean alwaysUnderlineLevels();

	/** Writes whether the level and XP phrases that have a card carry a line under them at all times. */
	void setAlwaysUnderlineLevels(boolean on);

	/** The colour of the words a card is about, and of the underline under the ones hovered. */
	Color hoverColour();

	/** Writes the colour of the words a card is about, and of the underline under the ones hovered. */
	void setHoverColour(Color colour);

	/** The colour of the player's name in the sidebar's recent cards. */
	Color nameColour();

	/** Writes the colour of the player's name in the sidebar's recent cards. */
	void setNameColour(Color colour);

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

	/** Whether the sidebar's "How to use" box is folded to its header. */
	boolean howToFolded();

	/** Writes whether the sidebar's "How to use" box is folded to its header. */
	void setHowToFolded(boolean folded);
}
