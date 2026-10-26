package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/** {@link CraftScenes}: the six craft and night scenes, each found by the colour of a thing it must hold. */
public class CraftScenesTest
{
	private static final int W = 420;
	private static final int H = 190;

	/** A test of one pixel's colour. */
	private abstract static class Probe
	{
		abstract boolean hits(int red, int green, int blue);
	}

	/** A pixel's colour, the three channels between {@code low} and {@code high} (each a red, green, blue triple). */
	private static Probe between(final int[] low, final int[] high)
	{
		return new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return red >= low[0] && red <= high[0] && green >= low[1] && green <= high[1] && blue >= low[2]
					&& blue <= high[2];
			}
		};
	}

	/**
	 * Whether some pixel of the rectangle from ({@code x0}, {@code y0}) up to but not including ({@code x1}, {@code y1}),
	 * and at least {@code near} but at most {@code far} px from ({@code cx}, {@code cy}), is one {@code probe} hits.
	 */
	private static boolean any(BufferedImage sky, int x0, int y0, int x1, int y1, double cx, double cy, double near,
		double far, Probe probe)
	{
		for (int y = Math.max(0, y0); y < Math.min(sky.getHeight(), y1); y++)
		{
			for (int x = Math.max(0, x0); x < Math.min(sky.getWidth(), x1); x++)
			{
				final double distance = Math.hypot(x - cx, y - cy);
				if (distance < near || distance > far)
				{
					continue;
				}
				final int argb = sky.getRGB(x, y);
				if (probe.hits((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF))
				{
					return true;
				}
			}
		}
		return false;
	}

	/** Whether some pixel of the rectangle is one {@code probe} hits. */
	private static boolean any(BufferedImage sky, int x0, int y0, int x1, int y1, Probe probe)
	{
		return any(sky, x0, y0, x1, y1, 0, 0, 0, Double.MAX_VALUE, probe);
	}

	/** Whether some pixel within {@code radius} px of ({@code cx}, {@code cy}) is one {@code probe} hits. */
	private static boolean near(BufferedImage sky, double cx, double cy, double radius, Probe probe)
	{
		return any(sky, (int) (cx - radius), (int) (cy - radius), (int) (cx + radius) + 1, (int) (cy + radius) + 1, cx, cy,
			0, radius, probe);
	}

	/** Lot 10V: Steampunk holds a brass frame, a gear at the bottom right and a pale gauge face at the top right. */
	@Test
	public void steampunk()
	{
		final BufferedImage sky = SceneSky.of(CardTheme.STEAMPUNK, W, H);
		assertTrue("brass in the left 4 px", any(sky, 0, 0, 4, H, new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return red > 150 && red - blue > 60;
			}
		}));
		// The gear, the window and the grid are looked for inside the 4 px border, whose bars, brackets and edge
		// have colours of their own.
		assertTrue("a gear at the bottom right", any(sky, W - 70, H - 56, W - 4, H - 4, new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return red > 150 && green > 100 && blue < 90;
			}
		}));
		assertTrue("the gauge face", near(sky, W - 20, 21, 9, new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return red + green + blue > 600;
			}
		}));
	}

	/** Lot 10V: Cyberpunk holds lit windows in the city, HUD brackets in its corner and a dark sky over the words. */
	@Test
	public void cyberpunk()
	{
		final BufferedImage sky = SceneSky.of(CardTheme.CYBERPUNK, W, H);
		assertTrue("a lit window in the bottom 46 px",
			any(sky, 4, H - 46, W - 4, H - 4, between(new int[] {0, 171, 191}, new int[] {89, 255, 255})));
		assertTrue("a bracket in the top left 14 px",
			any(sky, 0, 0, 14, 14, between(new int[] {0, 221, 221}, new int[] {59, 255, 255})));
		long sum = 0;
		int count = 0;
		// Inside the 4 px border: its rows and columns are the neon edge, bright by design.
		for (int y = 4; y < 20; y++)
		{
			for (int x = 4; x < W / 2; x++)
			{
				final int argb = sky.getRGB(x, y);
				sum += ((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF);
				count++;
			}
		}
		assertTrue("the sky over the words is dark: " + (double) sum / count, (double) sum / count < 70);
	}

	/** Lot 10V: Synthwave holds a sun at the right and a neon grid along the bottom. */
	@Test
	public void synthwave()
	{
		final BufferedImage sky = SceneSky.of(CardTheme.SYNTHWAVE, W, H);
		assertTrue("the sun", near(sky, W - 96, H * 0.66, 30, new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return red > 220 && green > 100 && blue < 170;
			}
		}));
		assertTrue("the grid in the bottom 14 px", any(sky, 4, H - 14, W - 4, H - 4, new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return red > 200 && blue > 170 && green < 140;
			}
		}));
	}

	/** Lot 10V: Volcano holds a lava river along the bottom and the glow of a crater. */
	@Test
	public void volcano()
	{
		final BufferedImage sky = SceneSky.of(CardTheme.VOLCANO, W, H);
		assertTrue("lava in the bottom 14 px",
			any(sky, 0, H - 14, W, H, between(new int[] {241, 171, 0}, new int[] {255, 255, 255})));
		assertTrue("the crater glow",
			near(sky, W - 80, H - 64, 20, between(new int[] {201, 80, 0}, new int[] {255, 200, 255})));
	}

	/** Lot 10V: Runic holds a band of cyan runes along the bottom and a magic circle at the right. */
	@Test
	public void runic()
	{
		final BufferedImage sky = SceneSky.of(CardTheme.RUNIC, W, H);
		assertTrue("a rune in the bottom 14 px",
			any(sky, 0, H - 14, W, H, between(new int[] {0, 181, 201}, new int[] {169, 255, 255})));
		assertTrue("the magic circle", any(sky, W - 120, 0, W, H, W - 70, H / 2.0 + 4, 40, 48, new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return blue > 160 && blue - red > 40;
			}
		}));
	}

	/** Lot 10V: Spooky holds a moon, a lit window in the house at the right and a lantern at the bottom left. */
	@Test
	public void spooky()
	{
		final BufferedImage sky = SceneSky.of(CardTheme.SPOOKY, W, H);
		assertTrue("the moon", near(sky, W - 56, 30, 14, new Probe()
		{
			@Override
			boolean hits(int red, int green, int blue)
			{
				return red + green + blue > 650;
			}
		}));
		assertTrue("a lit window in the right 140 px of the bottom 70 px",
			any(sky, W - 140, H - 70, W, H, between(new int[] {231, 151, 0}, new int[] {255, 255, 109})));
		assertTrue("the lantern",
			near(sky, 54, H - 9, 10, between(new int[] {201, 80, 0}, new int[] {255, 160, 59})));
	}
}
