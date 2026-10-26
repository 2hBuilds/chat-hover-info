package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link ThemePreviews}: the sample card and the small sidebar the menu shows beside a theme list. */
public class ThemePreviewsTest
{
	/**
	 * Lot 10W: the sample card is the whip's item card painted by the card painter in the theme (Stone's black edge in the
	 * corner, the width the painter measures for the same sample); the small sidebar is 220 px wide, its ground the skin's
	 * page and its top card's edge the skin's own.
	 */
	@Test
	public void theSampleCardAndTheSmallSidebar()
	{
		final HoverCard sample = ThemePreviews.sampleCard();
		assertEquals("the sample is the whip", "Abyssal whip", sample.title());
		assertEquals("its price row and three drop sources", 4, sample.rows().size());

		final BufferedImage stone = ThemePreviews.card(CardTheme.STONE, null);
		assertEquals("Stone's black edge in the corner", Color.BLACK, new Color(stone.getRGB(0, 0)));
		final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = scratch.createGraphics();
		final Dimension measured;
		try
		{
			measured = CardPainter.paintSplit(g, sample, null, ThemePreviews.CARD_FONT_SIZE, 0, 0, ThemePreviews.DESCRIPTION,
				ThemePreviews.CARD_CAP, ThemePreviews.CARD_LINES, CardStyle.STONE);
		}
		finally
		{
			g.dispose();
		}
		assertEquals("as wide as the painter makes the same sample", measured.width, stone.getWidth());
		assertEquals("and as tall", measured.height, stone.getHeight());

		final BufferedImage garden = ThemePreviews.sidebar(SidebarSkin.of(CardTheme.GARDEN));
		assertEquals("the small sidebar is 220 px wide", 220, garden.getWidth());
		assertEquals("its ground is Garden's page", new Color(22, 40, 24), new Color(garden.getRGB(2, 2)));
		assertEquals("the top card's edge is Garden's", new Color(240, 236, 220), new Color(garden.getRGB(6, 6)));

		final BufferedImage plain = ThemePreviews.sidebar(SidebarSkin.PLAIN);
		assertEquals("RuneLite's own page colour", ColorScheme.DARKER_GRAY_COLOR, new Color(plain.getRGB(2, 2)));
	}
}
