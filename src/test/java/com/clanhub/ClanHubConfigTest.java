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
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

	/** Lot 10S: the sidebar is dressed in the card theme from the start. */
	// Re-aimed by the lead, 2026-10-26 (lot 10W): a Sidebar theme choice in place of the switch.
	@Test
	public void theSidebarFollowsTheThemeByDefault()
	{
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertEquals("the sidebar wears the card theme", SidebarTheme.SAME, config.sidebarLook());
		final SidebarTheme[] looks = SidebarTheme.values();
		final CardTheme[] themes = CardTheme.values();
		assertEquals("Same as cards, RuneLite, then every card theme", 2 + themes.length, looks.length);
		assertEquals("the first one's words", "Same as cards", looks[0].toString());
		assertEquals("the second one's words", "RuneLite", looks[1].toString());
		assertEquals("the first one", SidebarTheme.SAME, looks[0]);
		assertEquals("the second one", SidebarTheme.RUNELITE, looks[1]);
		assertNull("the first has no card theme", looks[0].card());
		assertNull("nor the second", looks[1].card());
		for (int i = 0; i < themes.length; i++)
		{
			assertEquals("the same words in the same order", themes[i].toString(), looks[2 + i].toString());
			assertEquals("and its card theme", themes[i], looks[2 + i].card());
		}
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

	/** Lot 10P: the cards open in the game's own stone frame until the player picks Classic or Void. */
	// Re-aimed by the lead, 2026-10-25 (lot 10Q): a fourth theme, Wiki.
	@Test
	public void theCardThemeIsStone()
	{
		// Re-aimed by the lead, 2026-10-26: Classic is the default, the user's word; the name stays.
		// Re-aimed by the lead, 2026-10-26 (lot 10R): eight space themes after Wiki.
		// Re-aimed by the lead, 2026-10-26 (lot 10T): twelve scene themes after Cosmic edge.
		final ClanHubConfig config = new ClanHubConfig()
		{
		};

		assertEquals("the card is Classic by default", CardTheme.CLASSIC, config.cardTheme());
		assertArrayEquals("Classic, Stone, Void, Wiki, the eight space looks, then the twelve scene looks, in that order",
			new CardTheme[] {CardTheme.CLASSIC, CardTheme.STONE, CardTheme.VOID, CardTheme.WIKI, CardTheme.NEBULA,
				CardTheme.AURORA, CardTheme.GALAXY, CardTheme.PLANET, CardTheme.ECLIPSE, CardTheme.CONSTELLATION,
				CardTheme.BLACK_HOLE, CardTheme.COSMIC, CardTheme.FOREST, CardTheme.OCEAN, CardTheme.AUTUMN,
				CardTheme.SAKURA, CardTheme.STEAMPUNK, CardTheme.CYBERPUNK, CardTheme.SYNTHWAVE, CardTheme.VOLCANO,
				CardTheme.RUNIC, CardTheme.SPOOKY, CardTheme.GARDEN, CardTheme.LAVENDER}, CardTheme.values());
		assertEquals("Classic", CardTheme.CLASSIC.toString());
		assertEquals("Stone", CardTheme.STONE.toString());
		assertEquals("Void", CardTheme.VOID.toString());
		assertEquals("Wiki", CardTheme.WIKI.toString());
		assertEquals("Nebula", CardTheme.NEBULA.toString());
		assertEquals("Aurora", CardTheme.AURORA.toString());
		assertEquals("Galaxy", CardTheme.GALAXY.toString());
		assertEquals("Planet rise", CardTheme.PLANET.toString());
		assertEquals("Eclipse", CardTheme.ECLIPSE.toString());
		assertEquals("Constellation", CardTheme.CONSTELLATION.toString());
		assertEquals("Black hole", CardTheme.BLACK_HOLE.toString());
		assertEquals("Cosmic edge", CardTheme.COSMIC.toString());
		assertEquals("Forest", CardTheme.FOREST.toString());
		assertEquals("Ocean", CardTheme.OCEAN.toString());
		assertEquals("Autumn", CardTheme.AUTUMN.toString());
		assertEquals("Sakura", CardTheme.SAKURA.toString());
		assertEquals("Steampunk", CardTheme.STEAMPUNK.toString());
		assertEquals("Cyberpunk", CardTheme.CYBERPUNK.toString());
		assertEquals("Synthwave", CardTheme.SYNTHWAVE.toString());
		assertEquals("Volcano", CardTheme.VOLCANO.toString());
		assertEquals("Runic", CardTheme.RUNIC.toString());
		assertEquals("Spooky", CardTheme.SPOOKY.toString());
		assertEquals("Garden", CardTheme.GARDEN.toString());
		assertEquals("Lavender", CardTheme.LAVENDER.toString());
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
