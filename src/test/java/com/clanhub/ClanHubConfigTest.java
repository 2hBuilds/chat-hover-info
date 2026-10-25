package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTime;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.clanhub.ClanHubConfig.ExamineLine;
import java.awt.Color;
import net.runelite.client.config.Keybind;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link ClanHubConfig}: what a player who has never opened the settings gets. */
public class ClanHubConfigTest
{
	@Test
	public void theDefaultsAreOnAndSixteen()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertTrue("the cards are on", config.showCards());
		assertEquals("the card's text is 16 px", 16, config.cardFontSize());
	}

	@Test
	public void theNewDefaults()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertTrue("the words are coloured", config.colourHoverables());
		assertEquals("the colour is blue", new Color(59, 130, 246), config.hoverColour());
		assertEquals("a card opens on hover", CardTrigger.HOVER, config.cardTrigger());
		assertEquals("the card key is Alt", Keybind.ALT, config.cardKey());
		assertEquals("no delay", 0, config.cardDelayMs());
	}

	@Test
	public void theWikiDescriptionsAreOn()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertTrue("the wiki's first paragraph shows on the cards", config.wikiDescriptions());
	}

	@Test
	public void wikiPicturesAreOnByDefault()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertTrue("the wiki's own picture shows on a boss, monster, raid or chest card", config.wikiPictures());
	}

	@Test
	public void bossCardsAreOnByDefault()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertTrue("a drop line's boss gets its own card", config.bossCards());
	}

	@Test
	public void examineCardsAreOnByDefault()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertTrue("the examine text of an item or a monster gives a card", config.examineCards());
	}

	@Test
	public void theHowToStartsOpen()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertFalse("the sidebar's how-to starts open", config.howToFolded());
	}

	@Test
	public void theExamineLineIsNameFirstByDefault()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertEquals("the examined thing's name goes in front of its examine line", ExamineLine.NAME_FIRST,
			config.examineLine());
		assertArrayEquals("item name first, description only, in that order",
			new ExamineLine[] {ExamineLine.NAME_FIRST, ExamineLine.DESCRIPTION}, ExamineLine.values());
		assertEquals("Item name first", ExamineLine.NAME_FIRST.toString());
		assertEquals("Description only", ExamineLine.DESCRIPTION.toString());
	}

	@Test
	public void theCardTimeIsAgoByDefault()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertEquals("a recent card's time reads as how long ago", CardTime.AGO, config.cardTime());
		assertArrayEquals("ago, the 24-hour clock, the 12-hour clock, in that order",
			new CardTime[] {CardTime.AGO, CardTime.CLOCK_24, CardTime.CLOCK_12}, CardTime.values());
		assertEquals("4m ago", CardTime.AGO.toString());
		assertEquals("14:32", CardTime.CLOCK_24.toString());
		assertEquals("2:32 PM", CardTime.CLOCK_12.toString());
	}

	@Test
	public void theAlwaysUnderlineIsOffAndGrey()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertFalse("the quiet phrases are not underlined at all times", config.alwaysUnderlineLevels());
		assertEquals("the line is grey", new Color(150, 150, 150), config.alwaysUnderlineColour());
	}

	/** Lot 10L: the player's name in a recent card's row is the brand orange until the player picks another. */
	@Test
	public void theNameColourIsTheBrandOrange()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		// Re-aimed by the lead, 2026-10-25 (lot 10M): the 2h orange, the user's word.
		assertEquals("the name is the 2h orange", new Color(220, 138, 0), config.nameColour());
	}

	@Test
	public void theCardPlaceIsAboveTheCursor()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertEquals("a card opens above the cursor", CardPlace.CURSOR, config.cardPlace());
		assertArrayEquals("above the cursor, above the chat, in that order",
			new CardPlace[] {CardPlace.CURSOR, CardPlace.CHAT}, CardPlace.values());
		assertEquals("Above the cursor", CardPlace.CURSOR.toString());
		assertEquals("Above the chat", CardPlace.CHAT.toString());
	}

	@Test
	public void theCardWidthIsMediumByDefault()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertEquals("the card is medium by default", CardWidth.MEDIUM, config.cardWidth());
		assertEquals("wide caps at 480 px", 480, CardWidth.WIDE.cap);
		assertEquals("wide gives the paragraph three lines", 3, CardWidth.WIDE.lines);
		assertEquals("medium caps at 409 px", 409, CardWidth.MEDIUM.cap);
		assertEquals("medium gives the paragraph five lines", 5, CardWidth.MEDIUM.lines);
		assertEquals("narrow caps at 391 px", 391, CardWidth.NARROW.cap);
		assertEquals("narrow gives the paragraph five lines", 5, CardWidth.NARROW.lines);
		assertArrayEquals("wide, medium, narrow, in that order",
			new CardWidth[] {CardWidth.WIDE, CardWidth.MEDIUM, CardWidth.NARROW}, CardWidth.values());
		assertEquals("Wide", CardWidth.WIDE.toString());
		assertEquals("Medium", CardWidth.MEDIUM.toString());
		assertEquals("Narrow", CardWidth.NARROW.toString());
	}

	@Test
	public void theShowModesAreThree()
	{
		assertArrayEquals("on hover, while holding a key, on click, in that order",
			new CardTrigger[] {CardTrigger.HOVER, CardTrigger.HOLD_KEY, CardTrigger.CLICK}, CardTrigger.values());
		assertEquals("On hover", CardTrigger.HOVER.toString());
		assertEquals("While holding a key", CardTrigger.HOLD_KEY.toString());
		assertEquals("On click", CardTrigger.CLICK.toString());
	}
}
