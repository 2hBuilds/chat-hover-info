package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The ground of the twelve scene card themes (Forest, Ocean, Autumn, Sakura, Steampunk, Cyberpunk, Synthwave, Volcano,
 * Runic, Spooky, Garden and Lavender): a vertical gradient from the theme's top colour to its bottom colour, the theme's
 * scene over it and a thin edge, all painted into one image of the card's size. Pure: a theme and a size in, an image
 * out; nothing is read from the client.
 *
 * <p>The scene itself is not painted here: {@link NatureScenes} paints the six nature themes (Forest, Ocean, Autumn,
 * Sakura, Garden and Lavender) and {@link CraftScenes} the other six, each on the {@link Graphics2D} this class hands it.
 * This class holds what they share: the table of colours, the cache and {@link #veil}.
 *
 * <p>Choice: the overlay paints the card every frame, so the last sixteen grounds made are kept, by theme and size, and
 * the same image is handed back for the same key; the image is drawn from and never written to. Sixteen for the reason
 * {@link SpaceSky} gives: the sidebar paints from the cache too, and its few boxes must not be pushed out by the cards at
 * the cursor.
 */
final class SceneSky
{
	/** How many grounds are kept; see the class comment. */
	private static final int KEPT = 16;

	/** What each theme's ground is made of. */
	private static final Map<CardTheme, Look> LOOKS = new EnumMap<>(CardTheme.class);

	/** The grounds kept, the least recently asked for first; guarded by this class's lock. */
	private static final Map<List<Object>, BufferedImage> SKIES = new LinkedHashMap<List<Object>, BufferedImage>(16, 0.75f,
		true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<List<Object>, BufferedImage> eldest)
		{
			return size() > KEPT;
		}
	};

	static
	{
		LOOKS.put(CardTheme.FOREST, new Look(rgb(22, 54, 40), rgb(6, 16, 10), rgb(150, 110, 60)));
		LOOKS.put(CardTheme.OCEAN, new Look(rgb(30, 120, 170), rgb(2, 16, 40), rgb(150, 230, 255)));
		LOOKS.put(CardTheme.AUTUMN, new Look(rgb(150, 70, 26), rgb(34, 14, 8), rgb(230, 140, 50)));
		LOOKS.put(CardTheme.SAKURA, new Look(rgb(40, 22, 64), rgb(16, 8, 22), rgb(255, 196, 224)));
		LOOKS.put(CardTheme.STEAMPUNK, new Look(rgb(70, 46, 28), rgb(34, 20, 10), rgb(200, 150, 70)));
		LOOKS.put(CardTheme.CYBERPUNK, new Look(rgb(28, 8, 46), rgb(6, 2, 12), rgb(255, 60, 210)));
		LOOKS.put(CardTheme.SYNTHWAVE, new Look(rgb(26, 6, 54), rgb(16, 4, 30), rgb(255, 60, 180)));
		LOOKS.put(CardTheme.VOLCANO, new Look(rgb(40, 18, 14), rgb(12, 5, 4), rgb(255, 140, 40)));
		LOOKS.put(CardTheme.RUNIC, new Look(rgb(58, 60, 66), rgb(28, 30, 34), rgb(110, 112, 120)));
		LOOKS.put(CardTheme.SPOOKY, new Look(rgb(46, 22, 60), rgb(10, 6, 14), rgb(255, 140, 40)));
		LOOKS.put(CardTheme.GARDEN, new Look(rgb(70, 110, 70), rgb(22, 40, 24), rgb(240, 236, 220)));
		LOOKS.put(CardTheme.LAVENDER, new Look(rgb(58, 40, 96), rgb(34, 20, 60), rgb(220, 190, 255)));
	}

	private SceneSky()
	{
	}

	/** The colour at the top of {@code theme}'s ground, which is the card style's ground colour. */
	static Color groundTop(CardTheme theme)
	{
		return look(theme).top;
	}

	/** The colour at the bottom of {@code theme}'s ground. */
	static Color groundBottom(CardTheme theme)
	{
		return look(theme).bottom;
	}

	/** The colour of {@code theme}'s edge. */
	static Color edge(CardTheme theme)
	{
		return look(theme).edge;
	}

	/**
	 * The whole ground of {@code theme}, {@code width} by {@code height} px, an opaque ARGB image: the gradient, the
	 * theme's scene and the edge. The same image comes back for the same theme and size, so the caller draws it and never
	 * writes to it. A theme that is not a scene theme is refused.
	 */
	static synchronized BufferedImage of(CardTheme theme, int width, int height)
	{
		final Look look = look(theme);
		final int w = Math.max(1, width);
		final int h = Math.max(1, height);
		final List<Object> key = Arrays.<Object>asList(theme, w, h);
		BufferedImage sky = SKIES.get(key);
		if (sky == null)
		{
			sky = paint(theme, look, w, h);
			SKIES.put(key, sky);
		}
		return sky;
	}

	/**
	 * Lays a black veil over the whole {@code width} by {@code height} image, a gradient from {@code leftAlpha} at its
	 * left edge to {@code rightAlpha} at its right edge (0 to 255 each), so that words over a scene stay easy to read.
	 */
	static void veil(Graphics2D g, int width, int height, int leftAlpha, int rightAlpha)
	{
		g.setPaint(new GradientPaint(0f, 0f, new Color(0, 0, 0, leftAlpha), Math.max(1, width - 1), 0f,
			new Color(0, 0, 0, rightAlpha)));
		g.fillRect(0, 0, width, height);
	}

	private static Look look(CardTheme theme)
	{
		final Look look = theme == null ? null : LOOKS.get(theme);
		if (look == null)
		{
			throw new IllegalArgumentException("not a scene theme: " + theme);
		}
		return look;
	}

	private static BufferedImage paint(CardTheme theme, Look look, int w, int h)
	{
		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			if (nature(theme))
			{
				NatureScenes.paint(theme, g, w, h);
			}
			else
			{
				CraftScenes.paint(theme, g, w, h);
			}
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/** Whether {@code theme} is one of the six {@link NatureScenes} paints; the other six are {@link CraftScenes}'. */
	private static boolean nature(CardTheme theme)
	{
		return theme == CardTheme.FOREST || theme == CardTheme.OCEAN || theme == CardTheme.AUTUMN
			|| theme == CardTheme.SAKURA || theme == CardTheme.GARDEN || theme == CardTheme.LAVENDER;
	}

	private static Color rgb(int red, int green, int blue)
	{
		return new Color(red, green, blue);
	}

	/** What a theme's ground is made of: its colour at the top and at the bottom, and its edge. */
	private static final class Look
	{
		final Color top;
		final Color bottom;
		final Color edge;

		Look(Color top, Color bottom, Color edge)
		{
			this.top = top;
			this.bottom = bottom;
			this.edge = edge;
		}
	}
}
