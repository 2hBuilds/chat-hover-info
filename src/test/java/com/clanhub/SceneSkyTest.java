package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/** {@link SceneSky}: the ground of the twelve scene card themes. */
public class SceneSkyTest
{
	private static final int W = 420;
	private static final int H = 160;

	/** The twelve scene themes, with each one's ground top, ground bottom and edge from the plan's table. */
	private static final Object[][] TABLE = {
		{CardTheme.FOREST, new Color(22, 54, 40), new Color(6, 16, 10), new Color(150, 110, 60)},
		{CardTheme.OCEAN, new Color(30, 120, 170), new Color(2, 16, 40), new Color(150, 230, 255)},
		{CardTheme.AUTUMN, new Color(150, 70, 26), new Color(34, 14, 8), new Color(230, 140, 50)},
		{CardTheme.SAKURA, new Color(40, 22, 64), new Color(16, 8, 22), new Color(255, 196, 224)},
		{CardTheme.STEAMPUNK, new Color(70, 46, 28), new Color(34, 20, 10), new Color(200, 150, 70)},
		{CardTheme.CYBERPUNK, new Color(28, 8, 46), new Color(6, 2, 12), new Color(255, 60, 210)},
		{CardTheme.SYNTHWAVE, new Color(26, 6, 54), new Color(16, 4, 30), new Color(255, 60, 180)},
		{CardTheme.VOLCANO, new Color(40, 18, 14), new Color(12, 5, 4), new Color(255, 140, 40)},
		{CardTheme.RUNIC, new Color(58, 60, 66), new Color(28, 30, 34), new Color(110, 112, 120)},
		{CardTheme.SPOOKY, new Color(46, 22, 60), new Color(10, 6, 14), new Color(255, 140, 40)},
		{CardTheme.GARDEN, new Color(70, 110, 70), new Color(22, 40, 24), new Color(240, 236, 220)},
		{CardTheme.LAVENDER, new Color(58, 40, 96), new Color(34, 20, 60), new Color(220, 190, 255)}};

	/**
	 * Lot 10T: each of the twelve grounds has its edge in the corner, its top colour near the top and its bottom colour near
	 * the bottom, and the same size gives the same image while another size gives an image of that size.
	 */
	@Test
	public void theStandInsAndTheCache()
	{
		for (final Object[] row : TABLE)
		{
			final CardTheme theme = (CardTheme) row[0];
			final Color top = (Color) row[1];
			final Color bottom = (Color) row[2];
			final Color edge = (Color) row[3];
			assertEquals(theme + " top", top, SceneSky.groundTop(theme));
			assertEquals(theme + " bottom", bottom, SceneSky.groundBottom(theme));
			assertEquals(theme + " edge", edge, SceneSky.edge(theme));

			final BufferedImage sky = SceneSky.of(theme, W, H);
			assertEquals(theme + " width", W, sky.getWidth());
			assertEquals(theme + " height", H, sky.getHeight());
			assertEquals(theme + " edge in the corner", edge.getRGB(), sky.getRGB(0, 0));
			// Re-aimed by the lead, 2026-10-26 (lots 10U and 10V): the scenes cover the ground, so its colours are no
			// longer checked here; the edge and the cache are.

			assertSame(theme + " is the same image again", sky, SceneSky.of(theme, W, H));
			final BufferedImage again = SceneSky.of(theme, W, H);
			for (int y = 0; y < H; y += 7)
			{
				for (int x = 0; x < W; x += 13)
				{
					assertEquals(theme + " at " + x + ", " + y, sky.getRGB(x, y), again.getRGB(x, y));
				}
			}
			final BufferedImage other = SceneSky.of(theme, 300, 120);
			assertEquals(theme + " other width", 300, other.getWidth());
			assertEquals(theme + " other height", 120, other.getHeight());
			assertNotSame(theme + " is another image", sky, other);
		}
	}
}
