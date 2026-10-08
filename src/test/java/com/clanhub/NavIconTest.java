package com.clanhub;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link NavIcon}, in memory: the size, the coin exactly as {@link CoinGlyph} draws it, the picture clear of it. */
public class NavIconTest
{
	/** The coin's centre and the farthest its jittered rim reaches from it, with a pixel of margin. */
	private static final double COIN_CENTRE = 4.5;
	private static final double CLEAR_DISTANCE = 6.0;

	private static BufferedImage coinAlone()
	{
		final BufferedImage img = new BufferedImage(NavIcon.SIZE, NavIcon.SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	@Test
	public void theIconIsSixteenBySixteenArgb()
	{
		final BufferedImage icon = NavIcon.create();
		assertEquals(16, icon.getWidth());
		assertEquals(16, icon.getHeight());
		assertEquals(BufferedImage.TYPE_INT_ARGB, icon.getType());
	}

	@Test
	public void theCoinIsDrawnPixelForPixelAsTheCoinGlyphDrawsIt()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int coinPixels = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if ((coin.getRGB(x, y) >>> 24) != 0)
				{
					coinPixels++;
					assertEquals("coin pixel " + x + "," + y, coin.getRGB(x, y), icon.getRGB(x, y));
				}
			}
		}
		assertTrue("the coin drew something", coinPixels > 0);
	}

	@Test
	public void thePictureLiesClearOfTheCoinsCircle()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int pictureOnly = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if (icon.getRGB(x, y) != coin.getRGB(x, y))
				{
					pictureOnly++;
					final double dx = x + 0.5 - COIN_CENTRE;
					final double dy = y + 0.5 - COIN_CENTRE;
					assertTrue("picture pixel " + x + "," + y + " is too near the coin",
						Math.sqrt(dx * dx + dy * dy) >= CLEAR_DISTANCE);
				}
			}
		}
		assertTrue("the picture drew something", pictureOnly > 0);
	}

	private static final int PARCHMENT = 0xFFCDBE9E;
	private static final int CLAN = 0xFF0000B0;
	private static final int HOVER = 0xFF3B82F6;
	private static final int WHITE = 0xFFF0F0F0;
	private static final int INK = 0xFF16161A;

	private static void assertPixel(String what, int expected, BufferedImage icon, int x, int y)
	{
		assertEquals(what + " at " + x + "," + y, expected, icon.getRGB(x, y));
	}

	@Test
	public void thePictureIsTheHoveredChatLineAndTheCardAbove()
	{
		final BufferedImage icon = NavIcon.create();
		final BufferedImage coin = coinAlone();
		int differing = 0;
		for (int y = 0; y < NavIcon.SIZE; y++)
		{
			for (int x = 0; x < NavIcon.SIZE; x++)
			{
				if (icon.getRGB(x, y) != coin.getRGB(x, y))
				{
					differing++;
				}
			}
		}
		assertEquals("the picture's pixels", 107, differing);

		for (int x = 0; x < NavIcon.SIZE; x++)
		{
			// Rows 0 and 9 hold only the coin's own rim (left of x 10): the picture adds nothing, the row of air.
			assertEquals("row 0 is the coin alone at " + x, coin.getRGB(x, 0), icon.getRGB(x, 0));
			assertEquals("row 9 is the coin alone at " + x, coin.getRGB(x, 9), icon.getRGB(x, 9));
			if (x >= 10)
			{
				assertEquals("row 0 transparent at " + x, 0, icon.getRGB(x, 0) >>> 24);
				assertEquals("row 9 transparent at " + x, 0, icon.getRGB(x, 9) >>> 24);
			}
			for (int y = 10; y <= 12; y++)
			{
				assertEquals("row " + y + " opaque at " + x, 255, icon.getRGB(x, y) >>> 24);
			}
		}

		// The strip's parchment at its two top corners, the clan's blue along the line, the lit word.
		assertPixel("parchment", PARCHMENT, icon, 0, 10);
		assertPixel("parchment", PARCHMENT, icon, 15, 10);
		assertPixel("clan blue", CLAN, icon, 1, 11);
		assertPixel("clan blue", CLAN, icon, 8, 11);
		assertPixel("hover blue", HOVER, icon, 9, 11);
		assertPixel("hover blue", HOVER, icon, 10, 11);
		// The title bar and its two cut corners, the window's cut bottom corner.
		assertPixel("hover blue", HOVER, icon, 12, 1);
		assertPixel("hover blue", HOVER, icon, 10, 2);
		assertEquals("title bar corner cut at 10,1", 0, icon.getRGB(10, 1) >>> 24);
		assertEquals("title bar corner cut at 15,1", 0, icon.getRGB(15, 1) >>> 24);
		assertEquals("window corner cut at 10,8", 0, icon.getRGB(10, 8) >>> 24);
		// The window's white and the ink of its two lines.
		assertPixel("white", WHITE, icon, 12, 3);
		assertPixel("white", WHITE, icon, 10, 5);
		assertPixel("ink", INK, icon, 11, 4);
		assertPixel("ink", INK, icon, 14, 4);
		assertPixel("ink", INK, icon, 13, 6);
		assertPixel("white", WHITE, icon, 14, 6);
		// The arrow's tip on the word, and the foot of the arrow.
		assertPixel("ink", INK, icon, 11, 11);
		assertPixel("white", WHITE, icon, 12, 11);
		assertPixel("white", WHITE, icon, 12, 15);
		assertPixel("ink", INK, icon, 13, 15);
		assertPixel("ink", INK, icon, 15, 15);
	}
}
