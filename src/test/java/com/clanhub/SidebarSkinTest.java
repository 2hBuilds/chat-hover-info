package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import com.clanhub.SidebarSkin.Box;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** {@link SidebarSkin}: what the sidebar paints with in each card theme. */
public class SidebarSkinTest
{
	/** Lot 10S: every theme has its skin; Classic's is the sidebar as it always was; the boxes are painted as the plan says. */
	@Test
	public void eachThemeHasItsSkin()
	{
		assertSame("Classic is the plain sidebar", SidebarSkin.PLAIN, SidebarSkin.of(CardTheme.CLASSIC));
		assertSame("and so is no theme at all", SidebarSkin.PLAIN, SidebarSkin.of(null));
		final SidebarSkin plain = SidebarSkin.PLAIN;
		assertEquals(ColorScheme.DARKER_GRAY_COLOR, plain.page);
		assertEquals(Color.WHITE, plain.title);
		assertEquals(Color.WHITE, plain.text);
		assertEquals(ColorScheme.LIGHT_GRAY_COLOR, plain.grey);
		assertEquals(new Color(125, 125, 125), plain.heading);
		assertEquals("a row's title is the text colour unless the skin sets one", Color.WHITE, plain.rowTitle);
		assertEquals(new Color(130, 130, 130), plain.time);
		assertEquals(ColorScheme.DARK_GRAY_HOVER_COLOR, plain.hot);

		final SidebarSkin stone = SidebarSkin.of(CardTheme.STONE);
		assertEquals(new Color(36, 31, 25), stone.page);
		assertEquals(new Color(255, 152, 31), stone.title);
		assertEquals(Color.WHITE, stone.text);
		assertEquals(new Color(205, 188, 150), stone.grey);
		assertEquals(new Color(175, 160, 128), stone.heading);
		assertEquals("Stone's row title is its text colour", Color.WHITE, stone.rowTitle);

		final SidebarSkin voidSkin = SidebarSkin.of(CardTheme.VOID);
		assertEquals(new Color(12, 6, 26), voidSkin.page);
		assertEquals(new Color(228, 194, 255), voidSkin.title);
		assertEquals(new Color(236, 226, 250), voidSkin.text);
		assertEquals(new Color(162, 142, 202), voidSkin.grey);
		assertEquals(new Color(142, 122, 182), voidSkin.heading);

		final SidebarSkin wiki = SidebarSkin.of(CardTheme.WIKI);
		assertEquals(new Color(222, 213, 193), wiki.page);
		assertEquals(new Color(38, 28, 16), wiki.title);
		assertEquals(new Color(30, 25, 15), wiki.text);
		assertEquals(new Color(110, 95, 70), wiki.grey);
		assertEquals(new Color(120, 105, 80), wiki.heading);
		assertEquals("Wiki's row title is the wiki's blue", new Color(42, 92, 172), wiki.rowTitle);

		final SidebarSkin nebula = SidebarSkin.of(CardTheme.NEBULA);
		assertEquals("a space theme's page is its ground's bottom colour", new Color(5, 3, 14), nebula.page);
		assertEquals(new Color(255, 215, 255), nebula.title);
		assertEquals("its text, grey and heading are its card's text, label and hint", CardStyle.NEBULA.text, nebula.text);
		assertEquals(CardStyle.NEBULA.label, nebula.grey);
		assertEquals(CardStyle.NEBULA.hint, nebula.heading);
		for (final CardTheme theme : CardTheme.values())
		{
			assertTrue(theme + " has a skin", SidebarSkin.of(theme) != null);
			if (theme != CardTheme.CLASSIC)
			{
				assertNotEquals(theme + " is not the plain skin", SidebarSkin.PLAIN, SidebarSkin.of(theme));
			}
		}

		final BufferedImage stoneCard = paint(stone, Box.CARD, 220, 120);
		assertEquals("Stone's card has its black edge in the corner", Color.BLACK, new Color(stoneCard.getRGB(0, 0)));
		assertEquals("and a rivet inside it", new Color(150, 132, 104), new Color(stoneCard.getRGB(3, 3)));

		final BufferedImage wikiCard = paint(wiki, Box.CARD, 220, 120);
		assertEquals("Wiki's card has a brown edge", new Color(150, 135, 105), new Color(wikiCard.getRGB(0, 0)));
		assertEquals("and a beige ground", new Color(234, 226, 210), new Color(wikiCard.getRGB(60, 60)));
	}

	/** Lot 10S: a row, a bar and a card are painted edge to edge, each in its theme's own look. */
	@Test
	public void theBoxesArePaintedInEachLook()
	{
		final BufferedImage plainCard = paint(SidebarSkin.PLAIN, Box.CARD, 220, 120);
		assertEquals(ColorScheme.DARK_GRAY_COLOR, new Color(plainCard.getRGB(0, 0)));
		assertEquals(ColorScheme.DARK_GRAY_COLOR, new Color(plainCard.getRGB(219, 119)));
		assertEquals("the plain bar", new Color(50, 50, 50), new Color(paint(SidebarSkin.PLAIN, Box.BAR, 200, 30).getRGB(5, 5)));
		assertEquals("the plain row", ColorScheme.DARK_GRAY_COLOR, new Color(paint(SidebarSkin.PLAIN, Box.ROW, 200, 44).getRGB(5, 5)));

		final SidebarSkin stone = SidebarSkin.of(CardTheme.STONE);
		final BufferedImage stoneRow = paint(stone, Box.ROW, 220, 44);
		assertEquals("Stone's row: the black edge", Color.BLACK, new Color(stoneRow.getRGB(0, 0)));
		assertEquals("then the light line", new Color(110, 98, 80), new Color(stoneRow.getRGB(1, 1)));
		assertNotEquals("and no rivet", new Color(150, 132, 104), new Color(stoneRow.getRGB(3, 3)));
		final BufferedImage stoneBar = paint(stone, Box.BAR, 200, 30);
		assertEquals("Stone's bar", new Color(45, 39, 31), new Color(stoneBar.getRGB(100, 10)));
		assertEquals("with a dark edge", new Color(30, 26, 20), new Color(stoneBar.getRGB(0, 10)));
		assertEquals("and a light line along its bottom", new Color(100, 90, 72), new Color(stoneBar.getRGB(100, 29)));

		final SidebarSkin voidSkin = SidebarSkin.of(CardTheme.VOID);
		final BufferedImage voidBar = paint(voidSkin, Box.BAR, 200, 30);
		assertEquals("Void's bar has a light purple edge", new Color(176, 118, 246), new Color(voidBar.getRGB(100, 0)));
		final Color voidTop = new Color(voidBar.getRGB(100, 1));
		final Color voidBottom = new Color(voidBar.getRGB(100, 28));
		assertTrue("and a gradient that darkens downwards: " + voidTop + " to " + voidBottom,
			voidTop.getRed() > voidBottom.getRed());
		assertEquals("Void's card is the card's frame", new Color(176, 118, 246), new Color(paint(voidSkin, Box.CARD, 220, 120).getRGB(0, 0)));

		final SidebarSkin wiki = SidebarSkin.of(CardTheme.WIKI);
		assertEquals("Wiki's row is lighter than its card", new Color(244, 238, 226), new Color(paint(wiki, Box.ROW, 200, 44).getRGB(60, 20)));
		assertEquals("with its own edge", new Color(180, 165, 135), new Color(paint(wiki, Box.ROW, 200, 44).getRGB(0, 0)));
		final BufferedImage wikiBar = paint(wiki, Box.BAR, 200, 30);
		assertEquals("Wiki's bar", new Color(194, 172, 136), new Color(wikiBar.getRGB(100, 15)));
		assertEquals("with the card's edge", new Color(150, 135, 105), new Color(wikiBar.getRGB(0, 0)));

		final SidebarSkin nebula = SidebarSkin.of(CardTheme.NEBULA);
		final BufferedImage nebulaBar = paint(nebula, Box.BAR, 200, 30);
		assertEquals("a space bar is its ground's top colour", new Color(14, 8, 30), new Color(nebulaBar.getRGB(100, 15)));
		assertEquals("with its edge colour round it", new Color(215, 160, 255), new Color(nebulaBar.getRGB(0, 0)));
		// Re-aimed by the lead, 2026-10-26: a sky row is calm (the ground's gradient, a corner glow, the edge), not the sky.
		final BufferedImage nebulaRow = paint(nebula, Box.ROW, 226, 44);
		assertEquals("a space row has its edge in the corner", new Color(215, 160, 255), new Color(nebulaRow.getRGB(0, 0)));
		assertTrue("and is lit in its top right corner", sum(nebulaRow.getRGB(222, 3)) > sum(nebulaRow.getRGB(4, 3)));
		final BufferedImage planetRow = paint(SidebarSkin.of(CardTheme.PLANET), Box.ROW, 200, 40);
		assertTrue("no planet in a row", new Color(planetRow.getRGB(180, 30)).getBlue() < 60);
		for (final CardTheme theme : CardTheme.values())
		{
			final SidebarSkin skin = SidebarSkin.of(theme);
			for (final Box box : Box.values())
			{
				final BufferedImage image = paint(skin, box, 120, 40);
				assertEquals(theme + " " + box + " is opaque in its corner", 255, image.getRGB(60, 20) >>> 24);
			}
		}
	}

	/**
	 * Lot 10T: a scene theme's skin is as a space theme's is: the page its ground's bottom colour, the words its card's
	 * colours, a row its sky with the edge in the corner, the bar its ground's top colour with the edge round it.
	 */
	@Test
	public void theSceneThemesHaveSkins()
	{
		final SidebarSkin forest = SidebarSkin.of(CardTheme.FOREST);
		assertEquals("Forest's page is its ground's bottom colour", new Color(6, 16, 10), forest.page);
		assertEquals(new Color(220, 245, 160), forest.title);
		assertEquals("its grey and heading are its card's label and hint", CardStyle.FOREST.label, forest.grey);
		assertEquals(CardStyle.FOREST.hint, forest.heading);

		assertEquals("Cyberpunk's text is its card's text", new Color(238, 232, 252), SidebarSkin.of(CardTheme.CYBERPUNK).text);

		final SidebarSkin garden = SidebarSkin.of(CardTheme.GARDEN);
		final BufferedImage row = paint(garden, Box.ROW, 200, 40);
		assertEquals("Garden's row has its edge in the corner", new Color(240, 236, 220), new Color(row.getRGB(0, 0)));
		final BufferedImage bar = paint(garden, Box.BAR, 200, 30);
		assertEquals("its bar is the ground's top colour", new Color(70, 110, 70), new Color(bar.getRGB(100, 15)));
		assertEquals("with the edge round it", new Color(240, 236, 220), new Color(bar.getRGB(0, 0)));
		// Re-aimed by the lead, 2026-10-26: a scene row is calm, so no fence or flowers sit behind the row's words.
		for (int x = 2; x < 198; x++)
		{
			for (int y = 2; y < 38; y++)
			{
				final Color at = new Color(row.getRGB(x, y));
				assertTrue("no fence in a Garden row at " + x + ", " + y,
					at.getRed() < 200 || at.getGreen() < 200 || at.getBlue() < 200);
			}
		}
	}

	/** The sum of a pixel's red, green and blue. */
	private static int sum(int rgb)
	{
		final Color c = new Color(rgb);
		return c.getRed() + c.getGreen() + c.getBlue();
	}

	private static BufferedImage paint(SidebarSkin skin, Box kind, int width, int height)
	{
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			skin.paintBox(g, width, height, kind);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}
}
