package com.clanhub;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Lot 15A: what the card does to a picture theme's picture and to the words laid over it: the stretch, the crisp edge ring,
 * the outline of light and of dark words, the frosted panel and its reach, and the focal title's dimming.
 */
public class PictureArtTest
{
	private static final int W = 409;
	private static final int H = 200;
	/** A line's box in the tests: x, y, width, height. */
	private static final int[] LINE = {120, 80, 150, 16};

	private static BufferedImage gradient()
	{
		final BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				image.setRGB(x, y, new Color(60 + x % 120, 80 + y % 100, 120 + (x + y) % 90).getRGB());
			}
		}
		return image;
	}

	private static int lightness(int rgb)
	{
		return (rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255);
	}

	private static BufferedImage copy(BufferedImage image)
	{
		final BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = out.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return out;
	}

	private static List<int[]> lines()
	{
		final List<int[]> lines = new ArrayList<>();
		lines.add(LINE);
		return lines;
	}

	@Test
	public void aPictureIsStretchedToTheExactSizeAndTheEdgeRingIsCrisp()
	{
		final BufferedImage art = PictureFixtures.plain(new Color(10, 200, 30), W, 200);
		final BufferedImage out = PictureArt.stretch(art, 437, 223);
		assertEquals(437, out.getWidth());
		assertEquals(223, out.getHeight());
		PictureArt.ring(out, new Color(255, 0, 0));
		assertEquals("the ring is the exact colour, not a blur of it", 0xFFFF0000, out.getRGB(0, 100));
		assertEquals(0xFFFF0000, out.getRGB(436, 100));
		assertEquals(0xFFFF0000, out.getRGB(200, 0));
		assertEquals(0xFFFF0000, out.getRGB(200, 222));
		assertEquals("one pixel in, the picture", new Color(10, 200, 30).getRGB(), out.getRGB(1, 100));
		assertEquals(new Color(10, 200, 30).getRGB(), out.getRGB(200, 1));
	}

	@Test
	public void theNearestHeightIsChosen()
	{
		final PictureTheme theme = PictureFixtures.theme("a", "Calm", "bright", false, 0, 0, false);
		assertEquals(150, theme.nearestHeight(100));
		assertEquals(150, theme.nearestHeight(170));
		assertEquals(200, theme.nearestHeight(180));
		assertEquals(200, theme.nearestHeight(212));
		assertEquals(250, theme.nearestHeight(226));
		assertEquals(250, theme.nearestHeight(400));
	}

	private static BufferedImage words(PictureTheme theme)
	{
		final BufferedImage image = new BufferedImage(60, 30, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
			PictureArt.edged(g, theme, "Wm", 10, 20);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	@Test
	public void lightWordsGetABlackOutlineAndDarkWordsAPaleHalo()
	{
		final BufferedImage light = words(PictureFixtures.theme("l", "Calm", "dark", false, 1, 0, false));
		final BufferedImage dark = words(PictureFixtures.theme("d", "Calm", "bright", true, 1, 0, false));
		boolean blackSeen = false;
		boolean paleSeen = false;
		for (int y = 0; y < 30; y++)
		{
			for (int x = 0; x < 60; x++)
			{
				final int a = light.getRGB(x, y);
				if (a >>> 24 != 0)
				{
					blackSeen = true;
					assertEquals("light words: only black, never a pale colour", 0, lightness(a));
				}
				final int b = dark.getRGB(x, y);
				if (b >>> 24 != 0)
				{
					paleSeen = true;
					assertTrue("dark words: only a pale halo, never black", lightness(b) > 600);
				}
			}
		}
		assertTrue("the outline was drawn", blackSeen);
		assertTrue("and the halo", paleSeen);
	}

	@Test
	public void outlineZeroDrawsNothingAndHalfIsFainterThanFull()
	{
		final BufferedImage none = words(PictureFixtures.theme("n", "Calm", "dark", false, 0, 0, false));
		for (int y = 0; y < 30; y++)
		{
			for (int x = 0; x < 60; x++)
			{
				assertEquals("no outline, no pixel", 0, none.getRGB(x, y) >>> 24);
			}
		}
		final BufferedImage half = words(PictureFixtures.theme("h", "Calm", "dark", false, 0.5, 0, false));
		final BufferedImage full = words(PictureFixtures.theme("f", "Calm", "dark", false, 1, 0, false));
		long halfAlpha = 0;
		long fullAlpha = 0;
		for (int y = 0; y < 30; y++)
		{
			for (int x = 0; x < 60; x++)
			{
				halfAlpha += half.getRGB(x, y) >>> 24;
				fullAlpha += full.getRGB(x, y) >>> 24;
			}
		}
		assertTrue("a half outline is fainter than a full one with its shadow", halfAlpha > 0 && halfAlpha < fullAlpha);
	}

	/** 1.1.0: at opacity 0 no pixel changes (the outline is the painter's, not the ground's); above it the band does. */
	@Test
	public void frostOpacityZeroChangesNoPixel()
	{
		final PictureTheme theme = PictureFixtures.theme("f", "Calm", "dark", false, 1, 0, false);
		final BufferedImage art = gradient();
		final BufferedImage off = copy(art);
		PictureArt.treat(off, theme, lines(), null, 0);
		final BufferedImage on = copy(art);
		PictureArt.treat(on, theme, lines(), null, 0.4);
		boolean changed = false;
		for (int y = 0; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				assertEquals(art.getRGB(x, y), off.getRGB(x, y));
				changed |= art.getRGB(x, y) != on.getRGB(x, y);
			}
		}
		assertTrue("above 0 the band changes the picture, on every theme", changed);
	}

	/** 1.1.0: the opacity scales the whole band: a quarter, a half and all of it move the picture further each time. */
	@Test
	public void frostOpacityScalesTheBand()
	{
		final PictureTheme theme = PictureFixtures.theme("s", "Calm", "dark", false, 1, 0.3, false);
		final BufferedImage art = gradient();
		final BufferedImage own = copy(art);
		PictureArt.treat(own, theme, lines(), null, 0.25);
		final BufferedImage twice = copy(art);
		PictureArt.treat(twice, theme, lines(), null, 1);
		long ownMoved = 0;
		long twiceMoved = 0;
		for (int y = 0; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				ownMoved += Math.abs((art.getRGB(x, y) & 0xFF) - (own.getRGB(x, y) & 0xFF));
				twiceMoved += Math.abs((art.getRGB(x, y) & 0xFF) - (twice.getRGB(x, y) & 0xFF));
			}
		}
		assertTrue("the band moves the picture", ownMoved > 0);
		assertTrue("the full band moves it much further than a quarter", twiceMoved > 3 * ownMoved);
	}

	/** Re-aimed for 1.1.0's opacity (the band is the player's now, the same on every theme): a theme with no focal title at opacity 0 changes no pixel. */
	@Test
	public void aPanelOfZeroChangesNoPixel()
	{
		final PictureTheme theme = PictureFixtures.theme("p", "Calm", "dark", false, 1, 0, false);
		final BufferedImage art = gradient();
		final BufferedImage out = copy(art);
		PictureArt.treat(out, theme, lines(), null, 0);
		for (int y = 0; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				assertEquals(art.getRGB(x, y), out.getRGB(x, y));
			}
		}
	}

	@Test
	public void aPanelChangesNothingMoreThanSixPixelsFromALineBox()
	{
		for (final boolean dark : new boolean[] {false, true})
		{
			final PictureTheme theme = PictureFixtures.theme("p", "Calm", "dark", dark, 1, 1, false);
			final BufferedImage art = gradient();
			final BufferedImage out = copy(art);
			PictureArt.treat(out, theme, lines(), null, 1);
			int changed = 0;
			for (int y = 0; y < H; y++)
			{
				for (int x = 0; x < W; x++)
				{
					final boolean near = x >= LINE[0] - 6 && x < LINE[0] + LINE[2] + 6 && y >= LINE[1] - 6 && y < LINE[1] + LINE[3] + 6;
					if (art.getRGB(x, y) != out.getRGB(x, y))
					{
						changed++;
						assertTrue("a pixel changed more than 6 px from the line box at " + x + "," + y, near);
					}
				}
			}
			assertTrue("the panel did change the pixels under the line", changed > 1000);
			final int inside = out.getRGB(LINE[0] + 75, LINE[1] + 8);
			final int before = art.getRGB(LINE[0] + 75, LINE[1] + 8);
			assertTrue(dark ? "dark ink lightens the picture under the words" : "light ink darkens it",
				dark ? lightness(inside) > lightness(before) : lightness(inside) < lightness(before));
		}
	}

	@Test
	public void twoLinesFourPixelsApartRunTogether()
	{
		final PictureTheme theme = PictureFixtures.theme("p", "Calm", "dark", false, 0, 1, false);
		final BufferedImage art = PictureFixtures.plain(new Color(200, 200, 200), W, H);
		final BufferedImage out = copy(art);
		final List<int[]> two = new ArrayList<>();
		two.add(new int[] {100, 60, 150, 16});
		two.add(new int[] {100, 80, 150, 16});
		PictureArt.treat(out, theme, two, null, 1);
		final int between = out.getRGB(175, 78);
		final int inside = out.getRGB(175, 68);
		assertTrue("the gap between the lines is as dark as the lines' own panel, give or take its fade", Math.abs(lightness(inside) - lightness(between)) < 20);
		assertTrue("and darker than the picture", lightness(between) < lightness(art.getRGB(175, 78)) - 100);
	}

	@Test
	public void theFocalTitleLowersOnlyWhatIsBrighterNearTheTitle()
	{
		final PictureTheme theme = PictureFixtures.theme("f", "Calm", "bright", false, 0, 0, true);
		final int[] title = {100, 40, 120, 16};
		final BufferedImage bright = PictureFixtures.plain(new Color(250, 250, 240), W, H);
		PictureArt.treat(bright, theme, Collections.<int[]>emptyList(), title, 1);
		final int near = bright.getRGB(150, 48);
		assertTrue("white under the title is held down: " + Integer.toHexString(near), lightness(near) < 3 * 190);
		assertEquals("far from the title (beyond 24 px) it is untouched", new Color(250, 250, 240).getRGB(), bright.getRGB(150, 120));

		final Color mid = new Color(60, 70, 90);
		final BufferedImage dim = PictureFixtures.plain(mid, W, H);
		PictureArt.treat(dim, theme, Collections.<int[]>emptyList(), title, 1);
		assertEquals("a pixel already under the ceiling keeps its colour", mid.getRGB(), dim.getRGB(150, 48));

		final PictureTheme off = PictureFixtures.theme("o", "Calm", "bright", false, 0, 0, false);
		final BufferedImage unchanged = PictureFixtures.plain(new Color(250, 250, 240), W, H);
		PictureArt.treat(unchanged, off, Collections.<int[]>emptyList(), title, 1);
		assertEquals("a theme with no focal title keeps its light", new Color(250, 250, 240).getRGB(), unchanged.getRGB(150, 48));
	}
}
