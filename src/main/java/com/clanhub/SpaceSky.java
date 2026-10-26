package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The ground of the eight space card themes (Nebula, Aurora, Galaxy, Planet rise, Eclipse, Constellation, Black hole and
 * Cosmic edge): a dark vertical gradient, the theme's picture over it (clouds, lights, a spiral, a planet's edge, a corona,
 * joined stars, a glowing disc), a scatter of stars and a thin edge, all painted into one image of the card's size. Pure:
 * a theme and a size in, an image out; nothing is read from the client.
 *
 * <p>Choice: the overlay paints the card every frame, so the last sixteen skies made are kept, by theme and size, and the
 * same image is handed back for the same key; the image is drawn from and never written to. Sixteen and not eight because
 * the sidebar paints from the same cache: its top card (two heights, folded or not), its how-to bar and its rows (two
 * heights) are four skies that stay in use, and the cards at the cursor come in as many sizes as there are cards, so the
 * sidebar's few must not be pushed out by them. The stars are laid from a
 * 64 px tile made once per theme, and the soft shapes of the mock-ups are radial gradients and strokes of falling alpha,
 * never a blur.
 */
final class SpaceSky
{
	/** How far in from the card's edge the content starts, for every space theme; Eclipse's corona is centred on the picture box. */
	static final int INSET = 10;

	/** The three colours of the Cosmic edge: its edge, its title and its rules run left to right through them. */
	static final Color COSMIC_PINK = new Color(255, 70, 200);
	static final Color COSMIC_VIOLET = new Color(150, 80, 255);
	static final Color COSMIC_CYAN = new Color(60, 220, 255);

	/** The side of the square tile the stars are laid with, in px. */
	private static final int TILE = 64;
	/** How many skies are kept; see the class comment for why it is sixteen. */
	private static final int KEPT = 16;
	/** The alpha of the line inside the edge. */
	private static final int INNER_ALPHA = 90;
	private static final float[] FADE = {0f, 1f};
	/** How a blob fades from its colour at the centre to nothing at its radii: a plateau, then a soft shoulder, as a blur gives. */
	private static final float[] BLOB_AT = {0f, 0.3f, 0.55f, 0.8f, 1f};
	private static final float[] BLOB_ALPHA = {1f, 0.92f, 0.6f, 0.24f, 0f};
	private static final float[] THIRDS = {0f, 0.5f, 1f};

	/** What each theme's ground, stars and edge are made of. */
	private static final Map<CardTheme, Look> LOOKS = new EnumMap<>(CardTheme.class);

	/** The skies kept, the least recently asked for first; guarded by this class's lock. */
	private static final Map<List<Object>, BufferedImage> SKIES = new LinkedHashMap<List<Object>, BufferedImage>(16, 0.75f,
		true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<List<Object>, BufferedImage> eldest)
		{
			return size() > KEPT;
		}
	};

	/** The star tile of each theme, made when the theme's first sky is; guarded by this class's lock. */
	private static final Map<CardTheme, BufferedImage> TILES = new EnumMap<>(CardTheme.class);

	static
	{
		final Color white = Color.WHITE;
		LOOKS.put(CardTheme.NEBULA, new Look(rgb(14, 8, 30), rgb(5, 3, 14), rgb(215, 160, 255), true, 420, 1, 3, white,
			rgb(200, 220, 255), rgb(255, 200, 240)));
		LOOKS.put(CardTheme.AURORA, new Look(rgb(3, 12, 26), rgb(2, 4, 12), rgb(96, 255, 206), true, 600, 2, 0, white,
			rgb(200, 255, 240)));
		LOOKS.put(CardTheme.GALAXY, new Look(rgb(8, 7, 22), rgb(2, 2, 9), rgb(176, 166, 255), true, 500, 3, 0, white,
			rgb(210, 200, 255)));
		LOOKS.put(CardTheme.PLANET, new Look(rgb(5, 5, 12), rgb(2, 2, 6), rgb(120, 190, 255), true, 380, 4, 2, white,
			rgb(190, 220, 255)));
		LOOKS.put(CardTheme.ECLIPSE, new Look(rgb(12, 5, 8), rgb(3, 1, 3), rgb(255, 156, 84), true, 650, 5, 0,
			rgb(255, 240, 220), rgb(255, 210, 180)));
		LOOKS.put(CardTheme.CONSTELLATION, new Look(rgb(14, 20, 48), rgb(5, 8, 22), rgb(180, 192, 255, 140), false, 520, 6,
			0, white, rgb(220, 228, 255)));
		LOOKS.put(CardTheme.BLACK_HOLE, new Look(rgb(4, 2, 8), rgb(1, 1, 3), rgb(255, 140, 120), true, 450, 7, 0, white,
			rgb(255, 220, 200)));
		LOOKS.put(CardTheme.COSMIC, new Look(rgb(6, 4, 14), rgb(2, 1, 6), null, true, 260, 8, 2, white, rgb(255, 200, 250),
			rgb(190, 240, 255)));
	}

	private SpaceSky()
	{
	}

	/** The colour at the top of {@code theme}'s ground, which is the card style's ground colour. */
	static Color ground(CardTheme theme)
	{
		return look(theme).top;
	}

	/** The colour at the bottom of {@code theme}'s ground. */
	static Color groundBottom(CardTheme theme)
	{
		return look(theme).bottom;
	}

	/** The colour of {@code theme}'s edge; for Cosmic edge, whose edge is a gradient, the middle colour of it. */
	static Color edge(CardTheme theme)
	{
		final Color edge = look(theme).edge;
		return edge == null ? COSMIC_VIOLET : edge;
	}

	/**
	 * The whole ground of {@code theme}, {@code width} by {@code height} px, an opaque ARGB image: the gradient, the
	 * theme's picture, the stars and the edge. The same image comes back for the same theme and size, so the caller draws
	 * it and never writes to it. A theme that is not a space theme is refused.
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

	private static Look look(CardTheme theme)
	{
		final Look look = theme == null ? null : LOOKS.get(theme);
		if (look == null)
		{
			throw new IllegalArgumentException("not a space theme: " + theme);
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
			g.setPaint(new GradientPaint(0f, 0f, look.top, 0f, Math.max(1, h - 1), look.bottom));
			g.fillRect(0, 0, w, h);
			under(g, theme, w, h);
			stars(g, theme, look, w, h);
			sparkles(g, look, w, h);
			over(g, theme, w, h);
			edge(g, look, w, h);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/** What a theme paints over its ground and under its stars: Nebula's clouds, Aurora's lights, Eclipse's haze. */
	private static void under(Graphics2D g, CardTheme theme, int w, int h)
	{
		if (theme == CardTheme.NEBULA)
		{
			blob(g, w - 70, 18, 95, 42, rgb(196, 44, 176, 120));
			blob(g, w - 30, h - 30, 85, 60, rgb(44, 92, 236, 115));
			blob(g, w - 150, h / 2.0, 80, 45, rgb(126, 52, 216, 95));
			blob(g, 70, h - 10, 100, 30, rgb(84, 32, 160, 70));
		}
		else if (theme == CardTheme.AURORA)
		{
			for (int x = 0; x < w; x += 2)
			{
				final double top = 6 + 9 * Math.sin(x / 37.0) + 4 * Math.sin(x / 11.0);
				final double length = 34 + 14 * Math.sin(x / 23.0 + 1);
				final int alpha = (int) (70 + 50 * (0.5 + 0.5 * Math.sin(x / 17.0)));
				g.setPaint(new GradientPaint(0f, (float) (top - 8), rgb(150, 90, 255, 0), 0f, (float) top,
					rgb(150, 90, 255, alpha / 2)));
				g.fill(new Rectangle2D.Double(x, top - 8, 2, 8));
				g.setPaint(new GradientPaint(0f, (float) (top - 3), rgb(46, 255, 172, 0), 0f, (float) top,
					rgb(46, 255, 172, alpha)));
				g.fill(new Rectangle2D.Double(x, top - 3, 2, 3));
				g.setPaint(new GradientPaint(0f, (float) top, rgb(46, 255, 172, alpha), 0f, (float) (top + length),
					rgb(46, 255, 172, 0)));
				g.fill(new Rectangle2D.Double(x, top, 2, length));
			}
		}
		else if (theme == CardTheme.ECLIPSE)
		{
			blob(g, w - 40, h - 20, 110, 50, rgb(150, 32, 24, 70));
		}
	}

	/** What a theme paints over its stars: the spiral, the planet, the corona, the joined stars, the disc. */
	private static void over(Graphics2D g, CardTheme theme, int w, int h)
	{
		if (theme == CardTheme.GALAXY)
		{
			galaxy(g, w - 78, h / 2.0 + 4);
		}
		else if (theme == CardTheme.PLANET)
		{
			planet(g, w - 10, h + 165, 210);
		}
		else if (theme == CardTheme.ECLIPSE)
		{
			corona(g, INSET + 18, INSET + 16);
		}
		else if (theme == CardTheme.CONSTELLATION)
		{
			constellation(g, w);
		}
		else if (theme == CardTheme.BLACK_HOLE)
		{
			blackHole(g, w - 70, h / 2.0 + 6);
		}
	}

	/** Two arms of dots winding out from a bright core, the second turned half a round from the first. */
	private static void galaxy(Graphics2D g, double cx, double cy)
	{
		final Random random = new Random(9);
		for (int arm = 0; arm < 2; arm++)
		{
			for (int i = 0; i < 900; i++)
			{
				final double t = i / 900.0 * 3.4 * Math.PI;
				final double r = 3 * Math.exp(0.24 * t);
				final double angle = t + arm * Math.PI + (random.nextDouble() * 0.7 - 0.35);
				final int x = (int) Math.round(cx + r * Math.cos(angle));
				final int y = (int) Math.round(cy + 0.55 * r * Math.sin(angle));
				final Color tint = random.nextInt(10) < 7 ? rgb(200, 190, 255) : rgb(255, 200, 240);
				g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 60 + random.nextInt(141)));
				g.fillRect(x, y, 1, 1);
			}
		}
		blob(g, cx, cy, 34, 20, rgb(170, 140, 255, 90));
		blob(g, cx, cy, 16, 10, rgb(255, 240, 214, 230));
	}

	/** The edge of a big planet, lit blue along its rim, rising from the card's bottom right corner. */
	private static void planet(Graphics2D g, double cx, double cy, double r)
	{
		g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r, FADE,
			new Color[] {rgb(12, 14, 40), rgb(52, 84, 190)}));
		final Ellipse2D.Double circle = new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
		g.fill(circle);
		g.setStroke(new BasicStroke(7f));
		g.setColor(rgb(110, 200, 255, 60));
		g.draw(circle);
		g.setStroke(new BasicStroke(3f));
		g.setColor(rgb(110, 200, 255, 230));
		g.draw(circle);
		g.setStroke(new BasicStroke(1f));
	}

	/** A glow, a bright ring and a dark disc, centred where the picture box's middle is. */
	private static void corona(Graphics2D g, double cx, double cy)
	{
		blob(g, cx, cy, 34, 34, rgb(255, 128, 44, 120));
		final Ellipse2D.Double ring = new Ellipse2D.Double(cx - 22, cy - 22, 44, 44);
		g.setStroke(new BasicStroke(5f));
		g.setColor(rgb(255, 214, 150, 80));
		g.draw(ring);
		g.setStroke(new BasicStroke(2f));
		g.setColor(rgb(255, 214, 150));
		g.draw(ring);
		g.setStroke(new BasicStroke(1f));
		g.setColor(rgb(4, 2, 4));
		g.fill(new Ellipse2D.Double(cx - 20, cy - 20, 40, 40));
	}

	/** Eight stars at the right, joined by a thin line, each a white dot in a glow. */
	private static void constellation(Graphics2D g, int w)
	{
		final int[][] points = {{w - 170, 22}, {w - 128, 40}, {w - 96, 30}, {w - 60, 52}, {w - 34, 34}, {w - 104, 74},
			{w - 66, 100}, {w - 150, 96}};
		final int[] path = {0, 1, 2, 3, 4, 2, 5, 6, 5, 7};
		for (final int[] point : points)
		{
			blob(g, point[0], point[1], 4, 4, rgb(190, 210, 255, 150));
		}
		final Path2D.Double line = new Path2D.Double();
		for (int i = 0; i < path.length; i++)
		{
			if (i == 0)
			{
				line.moveTo(points[path[i]][0], points[path[i]][1]);
			}
			else
			{
				line.lineTo(points[path[i]][0], points[path[i]][1]);
			}
		}
		g.setStroke(new BasicStroke(1f));
		g.setColor(rgb(200, 214, 255, 80));
		g.draw(line);
		g.setColor(Color.WHITE);
		for (final int[] point : points)
		{
			g.fill(new Ellipse2D.Double(point[0] - 1.5, point[1] - 1.5, 3, 3));
		}
	}

	/** Fourteen glowing rings round a dark disc, from gold to pink, and a brighter ring close in. */
	private static void blackHole(Graphics2D g, double cx, double cy)
	{
		for (int i = 0; i < 14; i++)
		{
			final double t = i / 13.0;
			final int red = (int) Math.round(255 + (220 - 255) * t);
			final int green = (int) Math.round(176 + (54 - 176) * t);
			final int blue = (int) Math.round(80 + (170 - 80) * t);
			final int alpha = 150 - 8 * i;
			final double rx = 34 + 3 * i;
			final double ry = 8 + 0.9 * i;
			final Ellipse2D.Double ring = new Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry);
			g.setStroke(new BasicStroke(3f));
			g.setColor(rgb(red, green, blue, alpha / 3));
			g.draw(ring);
			g.setStroke(new BasicStroke(1f));
			g.setColor(rgb(red, green, blue, alpha));
			g.draw(ring);
		}
		final Ellipse2D.Double near = new Ellipse2D.Double(cx - 24, cy - 24, 48, 48);
		g.setStroke(new BasicStroke(5f));
		g.setColor(rgb(255, 200, 130, 57));
		g.draw(near);
		g.setStroke(new BasicStroke(2f));
		g.setColor(rgb(255, 200, 130, 170));
		g.draw(near);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Color.BLACK);
		g.fill(new Ellipse2D.Double(cx - 17, cy - 17, 34, 34));
	}

	/** Lays the theme's star tile over the whole image. */
	private static void stars(Graphics2D g, CardTheme theme, Look look, int w, int h)
	{
		BufferedImage tile = TILES.get(theme);
		if (tile == null)
		{
			tile = starTile(look);
			TILES.put(theme, tile);
		}
		g.setPaint(new TexturePaint(tile, new Rectangle(0, 0, TILE, TILE)));
		g.fillRect(0, 0, w, h);
	}

	/** The star tile: one dot to each {@code density} px, of 1 px and a few of 2 px, in the theme's tints, alpha 70 to 235. */
	private static BufferedImage starTile(Look look)
	{
		final BufferedImage tile = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
		final Random random = new Random(look.seed);
		final int count = Math.max(1, Math.round((float) TILE * TILE / look.density));
		for (int i = 0; i < count; i++)
		{
			final int x = random.nextInt(TILE);
			final int y = random.nextInt(TILE);
			final int size = random.nextInt(8) == 0 ? 2 : 1;
			final Color tint = look.tints[random.nextInt(look.tints.length)];
			final int argb = ((70 + random.nextInt(166)) << 24) | (tint.getRGB() & 0xFFFFFF);
			for (int dy = 0; dy < size; dy++)
			{
				for (int dx = 0; dx < size; dx++)
				{
					tile.setRGB((x + dx) % TILE, (y + dy) % TILE, argb);
				}
			}
		}
		return tile;
	}

	/** The theme's four-point stars, in the right 55 % of the image, each in a soft round glow. */
	private static void sparkles(Graphics2D g, Look look, int w, int h)
	{
		final Random random = new Random(look.seed * 7919 + 13);
		for (int i = 0; i < look.sparkles; i++)
		{
			final double x = w * 0.45 + random.nextDouble() * Math.max(1, w * 0.55 - 8);
			final double y = 6 + random.nextDouble() * Math.max(1, h - 12);
			final double s = 3 + random.nextDouble() * 2;
			blob(g, x, y, 4, 4, rgb(255, 255, 255, 140));
			final Path2D.Double star = new Path2D.Double();
			star.moveTo(x, y - s);
			star.lineTo(x + s / 4, y);
			star.lineTo(x, y + s);
			star.lineTo(x - s / 4, y);
			star.closePath();
			star.moveTo(x - s, y);
			star.lineTo(x, y - s / 4);
			star.lineTo(x + s, y);
			star.lineTo(x, y + s / 4);
			star.closePath();
			g.setColor(rgb(255, 255, 255, 235));
			g.fill(star);
		}
	}

	/**
	 * A soft blob: {@code colour} at its centre ({@code cx}, {@code cy}) fading to the same colour at alpha 0 at the radii
	 * {@code rx} and {@code ry}, drawn through a scaled radial gradient.
	 */
	private static void blob(Graphics2D g, double cx, double cy, double rx, double ry, Color colour)
	{
		final AffineTransform before = g.getTransform();
		final Paint paint = g.getPaint();
		g.translate(cx, cy);
		g.scale(rx, ry);
		final Color[] colours = new Color[BLOB_ALPHA.length];
		for (int i = 0; i < colours.length; i++)
		{
			colours[i] = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(),
				Math.round(colour.getAlpha() * BLOB_ALPHA[i]));
		}
		g.setPaint(new RadialGradientPaint(0f, 0f, 1f, BLOB_AT, colours));
		g.fill(new Rectangle2D.Double(-1, -1, 2, 2));
		g.setTransform(before);
		g.setPaint(paint);
	}

	/** The edge: a 1 px line in the theme's edge colour and one inside it at alpha 90; Cosmic edge a 2 px gradient line. */
	private static void edge(Graphics2D g, Look look, int w, int h)
	{
		if (look.edge == null)
		{
			ring(g, across(w, 255), w, h, 0, 2);
			ring(g, across(w, INNER_ALPHA), w, h, 2, 1);
			return;
		}
		ring(g, look.edge, w, h, 0, 1);
		if (look.inner)
		{
			ring(g, new Color(look.edge.getRed(), look.edge.getGreen(), look.edge.getBlue(), INNER_ALPHA), w, h, 1, 1);
		}
	}

	/** The three Cosmic colours, pink to violet to cyan, across {@code width} px at {@code alpha}. */
	private static Paint across(int width, int alpha)
	{
		final Color[] colours = {withAlpha(COSMIC_PINK, alpha), withAlpha(COSMIC_VIOLET, alpha),
			withAlpha(COSMIC_CYAN, alpha)};
		return new LinearGradientPaint(0f, 0f, Math.max(2, width), 0f, THIRDS, colours);
	}

	private static Color withAlpha(Color colour, int alpha)
	{
		return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
	}

	/** Fills the outline of a rectangle {@code thickness} px thick, {@code in} px inside the image's edge. */
	private static void ring(Graphics2D g, Paint paint, int width, int height, int in, int thickness)
	{
		final int ringWidth = width - 2 * in;
		final int ringHeight = height - 2 * in;
		g.setPaint(paint);
		g.fillRect(in, in, ringWidth, thickness);
		g.fillRect(in, in + ringHeight - thickness, ringWidth, thickness);
		g.fillRect(in, in + thickness, thickness, ringHeight - 2 * thickness);
		g.fillRect(in + ringWidth - thickness, in + thickness, thickness, ringHeight - 2 * thickness);
	}

	private static Color rgb(int red, int green, int blue)
	{
		return new Color(red, green, blue);
	}

	private static Color rgb(int red, int green, int blue, int alpha)
	{
		return new Color(red, green, blue, alpha);
	}

	/** What a theme's ground, stars and edge are made of. */
	private static final class Look
	{
		/** The ground's colour at the top and at the bottom. */
		final Color top;
		final Color bottom;
		/** The edge colour, or null for Cosmic edge's gradient. */
		final Color edge;
		/** Whether a line at alpha 90 runs inside the edge. */
		final boolean inner;
		/** One star to each this many px of area. */
		final int density;
		/** The star tile's seed. */
		final long seed;
		/** How many four-point stars. */
		final int sparkles;
		/** The colours the stars are taken from. */
		final Color[] tints;

		Look(Color top, Color bottom, Color edge, boolean inner, int density, long seed, int sparkles, Color... tints)
		{
			this.top = top;
			this.bottom = bottom;
			this.edge = edge;
			this.inner = inner;
			this.density = density;
			this.seed = seed;
			this.sparkles = sparkles;
			this.tints = tints;
		}
	}
}
