package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link NatureScenes}: the six nature scenes, each looked at in the pixels of {@link SceneSky#of} at 420 by 190. */
public class NatureScenesTest
{
	private static final int W = 420;
	private static final int H = 190;

	private static BufferedImage sky(CardTheme theme)
	{
		return SceneSky.of(theme, W, H);
	}

	private static int red(BufferedImage image, int x, int y)
	{
		return (image.getRGB(x, y) >> 16) & 0xFF;
	}

	private static int green(BufferedImage image, int x, int y)
	{
		return (image.getRGB(x, y) >> 8) & 0xFF;
	}

	private static int blue(BufferedImage image, int x, int y)
	{
		return image.getRGB(x, y) & 0xFF;
	}

	private static int sum(BufferedImage image, int x, int y)
	{
		return red(image, x, y) + green(image, x, y) + blue(image, x, y);
	}

	/** The mean of red, green and blue added up over the columns {@code x0} up to {@code x1} and the rows {@code y0} up to {@code y1}. */
	private static double meanSum(BufferedImage image, int x0, int x1, int y0, int y1)
	{
		long total = 0;
		for (int y = y0; y < y1; y++)
		{
			for (int x = x0; x < x1; x++)
			{
				total += sum(image, x, y);
			}
		}
		return total / (double) ((x1 - x0) * (y1 - y0));
	}

	/**
	 * Lot 10U: Forest has leaves in the top right, fireflies over most of the card and dark pines at the bottom left.
	 */
	@Test
	public void forest()
	{
		final BufferedImage sky = sky(CardTheme.FOREST);
		boolean leaf = false;
		for (int y = 0; y < 14; y++)
		{
			for (int x = W * 2 / 3; x < W; x++)
			{
				leaf |= green(sky, x, y) - red(sky, x, y) > 40 && green(sky, x, y) > 90;
			}
		}
		assertTrue("a leaf-green pixel in the top 14 px of the right third", leaf);

		boolean firefly = false;
		for (int y = 0; y < H; y++)
		{
			for (int x = (int) (W * 0.3); x < W; x++)
			{
				firefly |= red(sky, x, y) > 200 && green(sky, x, y) > 200 && blue(sky, x, y) < 170;
			}
		}
		assertTrue("a firefly pixel in the right 70 %", firefly);

		assertTrue("the bottom left is dark", meanSum(sky, 0, 60, H - 16, H) < 120);
	}

	/** Lot 10U: Ocean has a bubble at the right, kelp at the bottom right, and is lighter at the top than the bottom. */
	@Test
	public void ocean()
	{
		final BufferedImage sky = sky(CardTheme.OCEAN);
		boolean bubble = false;
		for (int y = 0; y < H; y++)
		{
			for (int x = W - 90; x < W; x++)
			{
				bubble |= red(sky, x, y) > 200 && green(sky, x, y) > 200 && blue(sky, x, y) > 200;
			}
		}
		assertTrue("a bubble pixel in the right 90 px", bubble);

		boolean kelp = false;
		for (int y = H - 60; y < H; y++)
		{
			for (int x = (int) (W * 0.3); x < W; x++)
			{
				kelp |= green(sky, x, y) - red(sky, x, y) > 40 && green(sky, x, y) > blue(sky, x, y);
			}
		}
		assertTrue("a kelp pixel in the bottom 60 px of the right 70 %", kelp);

		assertTrue("the top is brighter than the bottom", meanSum(sky, 0, W, 0, 20) > meanSum(sky, 0, W, H - 20, H));
	}

	/** Lot 10U: Autumn has a leaf at the bottom and the low sun at the right. */
	@Test
	public void autumn()
	{
		final BufferedImage sky = sky(CardTheme.AUTUMN);
		boolean leaf = false;
		for (int y = H - 8; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				leaf |= red(sky, x, y) > 180 && green(sky, x, y) < 170 && blue(sky, x, y) < 80;
			}
		}
		assertTrue("a maple-leaf pixel in the bottom 8 px", leaf);

		final int y = (int) (H * 0.55);
		assertTrue("the sun is at the right", sum(sky, W - 50, y) > sum(sky, 60, y));
	}

	/** Lot 10U: Sakura has blossom at the top right and a pale moon. */
	@Test
	public void sakura()
	{
		final BufferedImage sky = sky(CardTheme.SAKURA);
		boolean blossom = false;
		for (int y = 0; y < 34; y++)
		{
			for (int x = W - 170; x < W; x++)
			{
				blossom |= red(sky, x, y) > 230 && green(sky, x, y) >= 150 && green(sky, x, y) <= 215
					&& blue(sky, x, y) > 190;
			}
		}
		assertTrue("a blossom pixel in the top 34 px of the right 170 px", blossom);

		boolean moon = false;
		for (int y = 30 - 12; y <= 30 + 12; y++)
		{
			for (int x = W - 64 - 12; x <= W - 64 + 12; x++)
			{
				moon |= (x - (W - 64)) * (x - (W - 64)) + (y - 30) * (y - 30) <= 144 && sum(sky, x, y) > 650;
			}
		}
		assertTrue("a moon pixel near the top right", moon);
	}

	/** Lot 10U: Garden has a fence, tulips at the bottom and a rose at the right. */
	@Test
	public void garden()
	{
		final BufferedImage sky = sky(CardTheme.GARDEN);
		boolean fence = false;
		for (int y = H - 34; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				fence |= red(sky, x, y) > 200 && green(sky, x, y) > 200 && blue(sky, x, y) > 200;
			}
		}
		assertTrue("a fence pixel in the bottom 34 px", fence);

		boolean tulip = false;
		for (int y = H - 22; y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				tulip |= red(sky, x, y) > 200 && green(sky, x, y) < 90;
			}
		}
		assertTrue("a tulip pixel in the bottom 22 px", tulip);

		boolean rose = false;
		for (int y = 0; y < H; y++)
		{
			for (int x = W - 16; x < W; x++)
			{
				rose |= red(sky, x, y) > 180 && green(sky, x, y) < 60 && blue(sky, x, y) < 90;
			}
		}
		assertTrue("a rose pixel in the right 16 px", rose);
	}

	/** Lot 10U: each of the six is painted at any size, down to a single pixel, with its edge colour in the corner. */
	@Test
	public void everySizeIsPainted()
	{
		final CardTheme[] themes = {CardTheme.FOREST, CardTheme.OCEAN, CardTheme.AUTUMN, CardTheme.SAKURA,
			CardTheme.GARDEN, CardTheme.LAVENDER};
		final int[][] sizes = {{1, 1}, {10, 10}, {30, 30}, {200, 40}, {100, 300}, {900, 600}};
		for (final CardTheme theme : themes)
		{
			for (final int[] size : sizes)
			{
				final BufferedImage image = SceneSky.of(theme, size[0], size[1]);
				assertEquals(theme + " width", size[0], image.getWidth());
				assertEquals(theme + " height", size[1], image.getHeight());
				assertEquals(theme + " edge in the corner at " + size[0] + " by " + size[1], SceneSky.edge(theme).getRGB(),
					image.getRGB(0, 0));
			}
		}
	}

	/** Lot 10U: Lavender has a field at the bottom with poppies in it, and a sky that lightens toward the horizon. */
	@Test
	public void lavender()
	{
		final BufferedImage sky = sky(CardTheme.LAVENDER);
		boolean lavender = false;
		for (int y = (int) (H * 0.85); y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				lavender |= blue(sky, x, y) > 200 && red(sky, x, y) > 140 && green(sky, x, y) < 175;
			}
		}
		assertTrue("a lavender pixel in the bottom 15 %", lavender);

		boolean poppy = false;
		for (int y = (int) (H * 0.8); y < H; y++)
		{
			for (int x = 0; x < W; x++)
			{
				poppy |= red(sky, x, y) > 200 && green(sky, x, y) < 70 && blue(sky, x, y) < 70;
			}
		}
		assertTrue("a poppy pixel in the bottom 20 %", poppy);

		assertTrue("dusk: the sky lightens toward the horizon", sum(sky, W / 2, 4) < sum(sky, W / 2, (int) (H * 0.75)));
	}
}
