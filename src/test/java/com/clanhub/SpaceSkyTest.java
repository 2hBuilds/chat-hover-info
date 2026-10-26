package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/** {@link SpaceSky}: the ground of the eight space card themes. */
public class SpaceSkyTest
{
	private static final int W = 420;
	private static final int H = 160;

	/** Lot 10R: each sky has its edge in the corner and its own picture where the mock-up puts it. */
	@Test
	public void eachSkyHasItsPicture()
	{
		edge(CardTheme.NEBULA, new Color(215, 160, 255));
		edge(CardTheme.AURORA, new Color(96, 255, 206));
		edge(CardTheme.GALAXY, new Color(176, 166, 255));
		edge(CardTheme.PLANET, new Color(120, 190, 255));
		edge(CardTheme.ECLIPSE, new Color(255, 156, 84));
		edge(CardTheme.BLACK_HOLE, new Color(255, 140, 120));

		// Constellation's edge is alone and see-through (alpha 140): it is that colour laid over the ground's top.
		final BufferedImage constellation = sky(CardTheme.CONSTELLATION);
		final Color top = new Color(14, 20, 48);
		final Color edge = new Color(180, 192, 255);
		final Color corner = new Color(constellation.getRGB(0, 0));
		assertEquals("Constellation's edge, red", over(top.getRed(), edge.getRed()), corner.getRed(), 2);
		assertEquals("Constellation's edge, green", over(top.getGreen(), edge.getGreen()), corner.getGreen(), 2);
		assertEquals("Constellation's edge, blue", over(top.getBlue(), edge.getBlue()), corner.getBlue(), 2);
		assertTrue("no second line inside it: the pixel inside is still the dark ground",
			new Color(constellation.getRGB(1, 80)).getRed() < 40);
		assertTrue("while another theme has one at alpha 90: " + new Color(sky(CardTheme.NEBULA).getRGB(1, 80)),
			new Color(sky(CardTheme.NEBULA).getRGB(1, 80)).getRed() > 60);

		final BufferedImage cosmic = sky(CardTheme.COSMIC);
		final Color pink = new Color(cosmic.getRGB(0, 0));
		final Color cyan = new Color(cosmic.getRGB(W - 1, 0));
		assertTrue("Cosmic edge starts pink: " + pink, pink.getRed() > pink.getBlue());
		assertTrue("and ends cyan: " + cyan, cyan.getBlue() > cyan.getRed());

		final BufferedImage nebula = sky(CardTheme.NEBULA);
		boolean cloud = false;
		for (int y = 0; y < H / 4; y++)
		{
			for (int x = W / 2; x < W; x++)
			{
				final Color pixel = new Color(nebula.getRGB(x, y));
				cloud |= pixel.getRed() > 60 && pixel.getRed() > pixel.getBlue();
			}
		}
		assertTrue("Nebula has a pink cloud in the top right", cloud);

		final BufferedImage aurora = sky(CardTheme.AURORA);
		boolean lights = false;
		for (int y = 0; y < H / 3; y++)
		{
			for (int x = 0; x < W; x++)
			{
				final Color pixel = new Color(aurora.getRGB(x, y));
				lights |= pixel.getGreen() - pixel.getRed() >= 60;
			}
		}
		assertTrue("Aurora has green lights across the top third", lights);

		final BufferedImage galaxy = sky(CardTheme.GALAXY);
		assertTrue("Galaxy's core is brighter than the empty sky", sum(galaxy, W - 78, 84) > sum(galaxy, 60, 84));

		final Color planet = new Color(sky(CardTheme.PLANET).getRGB(408, 154));
		assertTrue("Planet rise has a blue planet in the bottom right: " + planet, planet.getBlue() - planet.getRed() >= 40);

		final BufferedImage eclipse = sky(CardTheme.ECLIPSE);
		assertTrue("Eclipse has a dark disc behind the picture", sum(eclipse, 10 + 18, 10 + 16) < 30);
		assertTrue("with a bright ring round it", brightNear(eclipse, 10 + 18, 10 + 16, 26, 200, 150));

		assertTrue("Constellation has a star at its point", sum(constellation, W - 128, 40) > 400);

		final BufferedImage hole = sky(CardTheme.BLACK_HOLE);
		assertTrue("Black hole has a black disc", sum(hole, 350, 86) < 10);
		assertTrue("with a glowing ring round it", brightNear(hole, 350, 86, 50, 180, 0));
	}

	/** The same theme and size give the same image, pixel for pixel; another size gives an image of that size. */
	@Test
	public void theSameSizeGivesTheSameSky()
	{
		for (final CardTheme theme : new CardTheme[] {CardTheme.NEBULA, CardTheme.AURORA, CardTheme.GALAXY,
			CardTheme.PLANET, CardTheme.ECLIPSE, CardTheme.CONSTELLATION, CardTheme.BLACK_HOLE, CardTheme.COSMIC})
		{
			final BufferedImage first = SpaceSky.of(theme, 380, 140);
			final BufferedImage second = SpaceSky.of(theme, 380, 140);
			assertEquals(theme + " width", 380, first.getWidth());
			assertEquals(theme + " height", 140, first.getHeight());
			assertEquals(theme + " again", first.getWidth(), second.getWidth());
			for (int y = 0; y < first.getHeight(); y++)
			{
				for (int x = 0; x < first.getWidth(); x++)
				{
					assertEquals(theme + " at " + x + ", " + y, first.getRGB(x, y), second.getRGB(x, y));
				}
			}
			final BufferedImage other = SpaceSky.of(theme, 300, 120);
			assertEquals(theme + " other width", 300, other.getWidth());
			assertEquals(theme + " other height", 120, other.getHeight());
			assertNotSame(theme + " is another image", first, other);
		}
	}

	private static BufferedImage sky(CardTheme theme)
	{
		return SpaceSky.of(theme, W, H);
	}

	/** The corner of {@code theme}'s sky is exactly {@code edge}. */
	private static void edge(CardTheme theme, Color edge)
	{
		assertEquals(theme + " edge", edge.getRGB() & 0xFFFFFF, sky(theme).getRGB(0, 0) & 0xFFFFFF);
		assertEquals(theme + " is opaque", 255, sky(theme).getRGB(0, 0) >>> 24);
	}

	/** {@code edge} at alpha 140 over {@code ground}, one channel. */
	private static double over(int ground, int edge)
	{
		return ground + (edge - ground) * 140 / 255.0;
	}

	private static int sum(BufferedImage image, int x, int y)
	{
		final Color pixel = new Color(image.getRGB(x, y));
		return pixel.getRed() + pixel.getGreen() + pixel.getBlue();
	}

	/** Whether a pixel within {@code radius} px of ({@code cx}, {@code cy}) has red above {@code red} and green above {@code green}. */
	private static boolean brightNear(BufferedImage image, int cx, int cy, int radius, int red, int green)
	{
		for (int y = Math.max(0, cy - radius); y <= Math.min(image.getHeight() - 1, cy + radius); y++)
		{
			for (int x = Math.max(0, cx - radius); x <= Math.min(image.getWidth() - 1, cx + radius); x++)
			{
				if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= radius * radius)
				{
					final Color pixel = new Color(image.getRGB(x, y));
					if (pixel.getRed() > red && pixel.getGreen() > green)
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}
