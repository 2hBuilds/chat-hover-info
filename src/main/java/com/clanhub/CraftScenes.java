package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The scenes of the six craft and night card themes, painted on the {@link Graphics2D} that {@link SceneSky} makes for a
 * card's ground. What each one holds:
 * <ul>
 * <li>Steampunk: a brown ground with a grain, a copper pipe along the top with three joints, four gears (brass and
 * copper) at the bottom, a brass pressure gauge with a red needle at the top right, wisps of steam, and a brass frame of
 * gradient bars studded with pale rivets.</li>
 * <li>Cyberpunk: a violet night sky with a pink and a cyan haze, a city of dark towers with lit windows, two neon signs,
 * thin rain, scan lines, two glitch bars, a neon double edge (pink outside, cyan inside) and cyan HUD brackets in the
 * corners.</li>
 * <li>Synthwave: a purple sky running to pink at the horizon, stars, a striped sun, a ridge of dark peaks with a pink
 * rim, a neon grid in perspective below, and an orange to pink to violet gradient edge that glows.</li>
 * <li>Volcano: a dark red ground with a honeycomb, a peak with a glowing crater under a column of smoke, two lava
 * veins down its side, a lava river along the bottom, cracks, drifting embers and a molten orange edge.</li>
 * <li>Runic: carved grey stone with a grain and cracks, a cyan magic circle at the right, a band of cyan runes along the
 * bottom and a chiselled edge with a faint cyan line inside it.</li>
 * <li>Spooky: a purple night with stars, a full moon with craters, a haunted house on a hill beside a bare tree, four
 * bats, a mist, a cobweb in the bottom left corner, a jack-o'-lantern, and an orange edge.</li>
 * </ul>
 *
 * <p>Every scene is painted inside the edge, 3 px in (4 for Steampunk) and with its corners rounded as its edge's are, in
 * a coordinate system that starts at the scene's own top left corner. Its numbers are px of the card at 1x. A soft blur is
 * a radial gradient (a glow) or a few strokes of falling alpha (a halo), never a pixel filter. The things that give light
 * (Spooky's moon, windows and lantern, Volcano's river core) are painted after the veil that keeps words readable, which
 * would put them out; Cyberpunk's windows are at full alpha for the same reason. The outermost ring of pixels is the
 * theme's edge colour, painted last.
 */
final class CraftScenes
{
	/** The fractions of a soft blob: where on its radius, 0 at the centre and 1 at its rim, each of its alphas lies. */
	private static final float[] BLOB_AT = {0f, 0.35f, 0.6f, 0.78f, 0.92f, 1f};

	/** The share of a soft blob's alpha at each of {@link #BLOB_AT}. */
	private static final float[] BLOB_ALPHA = {1f, 0.96f, 0.72f, 0.38f, 0.1f, 0f};

	/** The colours of Cyberpunk's lit windows: cyan, pink and a warm yellow. */
	private static final Color[] WINDOWS = {rgb(0, 230, 255), rgb(255, 60, 200), rgb(255, 220, 120)};

	private CraftScenes()
	{
	}

	/**
	 * Paints {@code theme}'s whole ground, {@code width} by {@code height} px with its top-left corner at the graphics'
	 * origin: the plain gradient of the theme under its scene, the scene with its veil and its border, and the edge, a
	 * 1 px ring of the theme's edge colour that is the last thing painted. A theme that is not one of the six is refused.
	 */
	static void paint(CardTheme theme, Graphics2D g, int width, int height)
	{
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setPaint(new GradientPaint(0f, 0f, SceneSky.groundTop(theme), 0f, Math.max(1, height - 1),
			SceneSky.groundBottom(theme)));
		g.fillRect(0, 0, width, height);
		switch (theme)
		{
			case STEAMPUNK:
				steampunk(g, width, height);
				break;
			case CYBERPUNK:
				cyberpunk(g, width, height);
				break;
			case SYNTHWAVE:
				synthwave(g, width, height);
				break;
			case VOLCANO:
				volcano(g, width, height);
				break;
			case RUNIC:
				runic(g, width, height);
				break;
			case SPOOKY:
				spooky(g, width, height);
				break;
			default:
				throw new IllegalArgumentException("not a craft theme: " + theme);
		}
		g.setColor(SceneSky.edge(theme));
		g.fillRect(0, 0, width, 1);
		g.fillRect(0, Math.max(0, height - 1), width, 1);
		g.fillRect(0, 1, 1, Math.max(0, height - 2));
		g.fillRect(Math.max(0, width - 1), 1, 1, Math.max(0, height - 2));
	}

	// ------------------------------------------------------------------------------------------------------ Steampunk

	private static void steampunk(Graphics2D g, int w, int h)
	{
		final int iw = w - 8;
		final int ih = h - 8;
		final Graphics2D s = inside(g, w, h, 4, 3);
		vertical(s, iw, ih, new float[] {0f, 1f}, rgb(70, 46, 28), rgb(34, 20, 10));
		grain(s, iw, ih, new Random(13), 0.6, 38, rgb(90, 60, 36), rgb(30, 18, 8));
		final Color brass = rgb(190, 140, 62);
		final Color dark = rgb(70, 44, 20);
		final Color light = rgb(240, 200, 120);
		final Color copper = rgb(176, 96, 52);
		final Color copperLight = rgb(230, 160, 110);
		gear(s, iw - 30, ih - 24, 30, 14, brass, dark, light, 0.2);
		gear(s, iw - 76, ih - 10, 18, 10, copper, dark, copperLight, 0.5);
		gear(s, iw - 64, ih - 46, 12, 8, brass, dark, light, 0.1);
		gear(s, 22, ih - 6, 16, 10, copper, dark, copperLight, 0.4);
		s.setColor(copper);
		s.fill(new Rectangle2D.Double(0, 12, iw - 30, 5));
		s.setColor(copperLight);
		s.fill(new Rectangle2D.Double(0, 13, iw - 30, 1));
		s.setColor(rgb(150, 80, 40));
		for (final int x : new int[] {60, 160, 260})
		{
			s.fill(new Rectangle2D.Double(x - 3, 10, 6, 9));
		}
		gauge(s, iw - 20, 18);
		final double[][] steam = {{iw - 44, 6, 6}, {iw - 52, 2, 8}, {iw - 62, -2, 10}, {iw - 40, 0, 5}};
		for (final double[] puff : steam)
		{
			blob(s, puff[0], puff[1], puff[2] + 5, puff[2] + 5, rgba(240, 240, 240, 64));
		}
		SceneSky.veil(s, iw, ih, 80, 20);
		s.dispose();

		bar(g, 1, 1, w - 1, 4, rgb(250, 210, 130), rgb(150, 100, 40), false);
		bar(g, 1, h - 4, w - 1, h - 1, rgb(150, 100, 40), rgb(90, 56, 20), false);
		bar(g, 1, 1, 4, h - 1, rgb(230, 180, 100), rgb(120, 80, 30), true);
		bar(g, w - 4, 1, w - 1, h - 1, rgb(120, 80, 30), rgb(200, 150, 70), true);
		g.setColor(rgb(250, 230, 170));
		for (int x = 10; x < w - 6; x += 26)
		{
			g.fill(circle(x, 2.5, 1.4));
			g.fill(circle(x, h - 2.5, 1.4));
		}
		for (int y = 14; y < h - 10; y += 26)
		{
			g.fill(circle(2.5, y, 1.4));
			g.fill(circle(w - 2.5, y, 1.4));
		}
	}

	/** A toothed wheel: its shadow, its body, a light ring, five spokes and a hub. */
	private static void gear(Graphics2D g, double cx, double cy, double r, int teeth, Color fill, Color dark, Color light,
		double angle)
	{
		final int n = teeth * 4;
		final double[] xy = new double[n * 2];
		final double[] shadow = new double[n * 2];
		for (int i = 0; i < n; i++)
		{
			final double a = angle + (double) i / n * 2 * Math.PI;
			final double rr = i % 4 == 1 || i % 4 == 2 ? r : r * 0.82;
			xy[2 * i] = cx + rr * Math.cos(a);
			xy[2 * i + 1] = cy + rr * Math.sin(a);
			shadow[2 * i] = xy[2 * i] + 0.8;
			shadow[2 * i + 1] = xy[2 * i + 1] + 0.8;
		}
		g.setColor(dark);
		g.fill(path(true, shadow));
		g.setColor(fill);
		g.fill(path(true, xy));
		g.setColor(light);
		g.setStroke(new BasicStroke(0.8f));
		g.draw(circle(cx, cy, r * 0.68));
		g.setColor(dark);
		g.setStroke(new BasicStroke((float) Math.max(1.0, r * 0.12)));
		for (int i = 0; i < 5; i++)
		{
			final double a = angle + i / 5.0 * 2 * Math.PI + 0.3;
			g.draw(new Line2D.Double(cx + r * 0.2 * Math.cos(a), cy + r * 0.2 * Math.sin(a), cx + r * 0.6 * Math.cos(a),
				cy + r * 0.6 * Math.sin(a)));
		}
		g.fill(circle(cx, cy, r * 0.18));
		g.setColor(light);
		g.fill(circle(cx, cy, r * 0.08));
	}

	/** The pressure gauge: a brass rim, a pale face with nine ticks, a red needle and a dark hub. */
	private static void gauge(Graphics2D g, double gx, double gy)
	{
		g.setColor(rgb(190, 140, 62));
		g.fill(circle(gx, gy, 14));
		g.setColor(rgb(236, 226, 200));
		g.fill(circle(gx, gy, 11));
		g.setColor(rgb(40, 30, 20));
		g.setStroke(new BasicStroke(1f));
		for (int i = 0; i < 9; i++)
		{
			final double a = Math.PI * 0.8 + i / 8.0 * Math.PI * 1.4;
			g.draw(new Line2D.Double(gx + 8 * Math.cos(a), gy + 8 * Math.sin(a), gx + 10 * Math.cos(a), gy + 10 * Math.sin(a)));
		}
		g.setColor(rgb(170, 30, 20));
		g.draw(new Line2D.Double(gx, gy, gx + 8 * Math.cos(-0.5), gy + 8 * Math.sin(-0.5)));
		g.setColor(rgb(40, 30, 20));
		g.fill(circle(gx, gy, 1.5));
	}

	// ------------------------------------------------------------------------------------------------------ Cyberpunk

	private static void cyberpunk(Graphics2D g, int w, int h)
	{
		final int iw = w - 6;
		final int ih = h - 6;
		final Graphics2D s = inside(g, w, h, 3, 0);
		vertical(s, iw, ih, new float[] {0f, 0.6f, 1f}, rgb(28, 8, 46), rgb(16, 4, 30), rgb(6, 2, 12));
		glow(s, iw - 70, ih - 40, 70, 30, rgba(255, 40, 200, 90), 18);
		glow(s, iw - 170, ih - 30, 60, 26, rgba(0, 220, 255, 70), 16);
		final Random random = new Random(17);
		double x = 0;
		while (x < iw)
		{
			final double bw = 14 + 16 * random.nextDouble();
			final double bh = 18 + 28 * random.nextDouble();
			s.setColor(rgba(10, 4, 20, 245));
			s.fill(new Rectangle2D.Double(x, ih - bh, bw, bh));
			for (int wy = (int) (ih - bh + 4); wy < ih - 2; wy += 5)
			{
				for (int wx = (int) (x + 3); wx < (int) (x + bw - 2); wx += 4)
				{
					if (random.nextDouble() < 0.28)
					{
						s.setColor(WINDOWS[random.nextInt(WINDOWS.length)]);
						s.fill(new Rectangle2D.Double(wx, wy, 1.5, 2));
					}
				}
			}
			x += bw + 1 + 3 * random.nextDouble();
		}
		neon(s, new Rectangle2D.Double(iw - 64, ih - 58, 34, 6), rgb(255, 40, 200), 1f, 4f, 0.5);
		neon(s, new Rectangle2D.Double(iw - 140, ih - 50, 22, 4), rgb(0, 230, 255), 1f, 4f, 0.5);
		final Path2D.Double rain = new Path2D.Double();
		for (int i = 0; i < 120; i++)
		{
			final double rx = random.nextDouble() * iw;
			final double ry = -10 + (ih + 10) * random.nextDouble();
			rain.moveTo(rx, ry);
			rain.lineTo(rx - 2, ry + 7);
		}
		s.setStroke(new BasicStroke(1f));
		s.setColor(rgba(150, 200, 255, 22));
		s.draw(rain);
		SceneSky.veil(s, iw, ih, 95, 30);
		s.setColor(rgba(0, 0, 0, 12));
		for (int y = 0; y < ih; y += 3)
		{
			s.fillRect(0, y, iw, 1);
		}
		s.dispose();

		ring(g, rgba(255, 40, 200, 160), w, h, 1, 3, 0, true);
		ring(g, rgb(255, 60, 210), w, h, 1, 1, 0, false);
		ring(g, rgba(0, 230, 255, 200), w, h, 3, 1, 0, false);
		final int[][] corners = {{2, 2, 1, 1}, {w - 2, 2, -1, 1}, {2, h - 2, 1, -1}, {w - 2, h - 2, -1, -1}};
		g.setColor(rgb(0, 255, 255));
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
		for (final int[] c : corners)
		{
			g.draw(path(false, c[0], c[1] + c[3] * 11, c[0], c[1], c[0] + c[2] * 11, c[1]));
		}
		final Random glitch = new Random(19);
		for (int i = 0; i < 5; i++)
		{
			final double y = 8 + 22 * glitch.nextDouble();
			final double bx = 200 + (w - 60 - 200) * glitch.nextDouble();
			final double len = 16 + 34 * glitch.nextDouble();
			g.setColor(glitch.nextBoolean() ? rgba(255, 40, 200, 160) : rgba(0, 230, 255, 160));
			g.fill(new Rectangle2D.Double(Math.round(bx), Math.round(y), len, 1));
		}
	}

	// ------------------------------------------------------------------------------------------------------ Synthwave

	private static void synthwave(Graphics2D g, int w, int h)
	{
		final int iw = w - 6;
		final int ih = h - 6;
		final Graphics2D s = inside(g, w, h, 3, 5);
		vertical(s, iw, ih, new float[] {0f, 0.45f, 0.72f, 0.78f, 1f}, rgb(26, 6, 54), rgb(96, 20, 110), rgb(220, 70, 120),
			rgb(40, 8, 60), rgb(16, 4, 30));
		final double hor = ih * 0.78;
		stars(s, iw, (int) hor, (int) (iw * ih / 700.0), new Random(23));
		final double cx = iw - 96;
		final double cy = ih * 0.72;
		final double r = 46;
		glow(s, cx, cy, 80, 60, rgba(255, 90, 160, 110), 22);
		final Area sun = new Area(circle(cx, cy, r));
		for (int k = 0; cy - r * 0.15 + 7 * k < cy + r; k++)
		{
			final double top = cy - r * 0.15 + 7 * k;
			sun.subtract(new Area(new Rectangle2D.Double(cx - r, top, 2 * r, 1.6 + 0.06 * (7 * k + 1))));
		}
		s.setPaint(new GradientPaint(0f, (float) (cy - r), rgb(255, 236, 110), 0f, (float) (cy + r), rgb(255, 60, 150)));
		s.fill(sun);

		final Random random = new Random(29);
		final List<double[]> ridge = new ArrayList<>();
		ridge.add(new double[] {0, hor});
		double x = 0;
		while (x < iw)
		{
			x += 18 + 22 * random.nextDouble();
			ridge.add(new double[] {x, hor - (6 + 20 * random.nextDouble())});
		}
		final double[] peaks = new double[ridge.size() * 2];
		for (int i = 0; i < ridge.size(); i++)
		{
			peaks[2 * i] = ridge.get(i)[0];
			peaks[2 * i + 1] = ridge.get(i)[1];
		}
		final Path2D.Double mountains = path(false, peaks);
		mountains.lineTo(iw, hor);
		mountains.lineTo(iw, hor + 2);
		mountains.lineTo(0, hor + 2);
		mountains.closePath();
		s.setColor(rgb(30, 6, 50));
		s.fill(mountains);
		s.setStroke(new BasicStroke(1f));
		s.setColor(rgba(255, 110, 220, 220));
		s.draw(path(false, peaks));

		final Path2D.Double grid = new Path2D.Double();
		final double vx = cx - 40;
		for (int i = -16; i <= 16; i++)
		{
			grid.moveTo(vx + i * 6, hor);
			grid.lineTo(vx + i * 46, ih);
		}
		double y = hor;
		double step = 1.5;
		while (y < ih)
		{
			grid.moveTo(0, y);
			grid.lineTo(iw, y);
			y += step;
			step *= 1.45;
		}
		neon(s, grid, rgba(255, 80, 220, 225), 1.2f, 3f, 0.4);
		SceneSky.veil(s, iw, ih, 80, 10);
		s.dispose();

		final Paint across = new LinearGradientPaint(0f, 0f, w, 0f, new float[] {0f, 0.5f, 1f},
			new Color[] {rgb(255, 120, 60), rgb(255, 60, 180), rgb(140, 80, 255)});
		final Paint acrossSoft = new LinearGradientPaint(0f, 0f, w, 0f, new float[] {0f, 0.5f, 1f},
			new Color[] {rgba(255, 120, 60, 36), rgba(255, 60, 180, 36), rgba(140, 80, 255, 36)});
		final Shape edge = ringShape(w, h, 2, 2, 5);
		for (int k = 4; k >= 1; k--)
		{
			g.setPaint(acrossSoft);
			g.setStroke(new BasicStroke(2f + 2f * k * 1.5f / 2f));
			g.draw(edge);
		}
		g.setPaint(across);
		g.setStroke(new BasicStroke(2f));
		g.draw(edge);
	}

	// -------------------------------------------------------------------------------------------------------- Volcano

	private static void volcano(Graphics2D g, int w, int h)
	{
		final int iw = w - 6;
		final int ih = h - 6;
		final Graphics2D s = inside(g, w, h, 3, 4);
		vertical(s, iw, ih, new float[] {0f, 0.6f, 1f}, rgb(40, 18, 14), rgb(22, 10, 8), rgb(12, 5, 4));
		final double side = 9;
		final Path2D.Double hexes = new Path2D.Double();
		for (int row = -1; row < ih / (side * 1.5) + 2; row++)
		{
			for (int col = -1; col < iw / (side * 1.73) + 2; col++)
			{
				final double hx = col * side * 1.73 + Math.floorMod(row, 2) * side * 0.865;
				final double hy = row * side * 1.5;
				for (int i = 0; i <= 6; i++)
				{
					final double a = Math.PI / 6 + i * Math.PI / 3;
					if (i == 0)
					{
						hexes.moveTo(hx + side * Math.cos(a), hy + side * Math.sin(a));
					}
					else
					{
						hexes.lineTo(hx + side * Math.cos(a), hy + side * Math.sin(a));
					}
				}
			}
		}
		s.setStroke(new BasicStroke(1f));
		s.setColor(rgba(0, 0, 0, 70));
		s.draw(hexes);
		final double vx = iw - 80;
		final double vb = ih - 18;
		for (int i = 0; i < 10; i++)
		{
			final double rx = 12 + 3.5 * i;
			blob(s, vx + 2 + 0.5 * i, vb - 40 - 8 * i, rx + 8, 18, rgba(70, 50, 46, 40));
		}
		glow(s, vx, vb - 30, 90, 46, rgba(200, 50, 20, 120), 20);
		final Path2D.Double cone = path(false, vx - 90, ih, vx - 10, vb - 46, vx + 10, vb - 46, vx + 90, ih);
		s.setColor(rgb(20, 9, 7));
		s.fill(cone);
		s.setStroke(new BasicStroke(1f));
		s.setColor(rgba(150, 60, 30, 200));
		s.draw(cone);
		glow(s, vx, vb - 46, 22, 10, rgba(255, 140, 30, 220), 7);

		final Color lava = rgb(255, 120, 20);
		final Path2D.Double veinLeft = path(false, vx - 2, vb - 44, (vx - 2 + vx - 26) / 2 + 3, vb - 22, vx - 26, ih);
		final Path2D.Double veinRight = path(false, vx + 2, vb - 44, (vx + 2 + vx + 30) / 2 + 3, vb - 22, vx + 30, ih);
		neon(s, veinLeft, lava, 2f, 6f, 0.7);
		neon(s, veinRight, lava, 2f, 6f, 0.7);
		final Path2D.Double river = new Path2D.Double();
		for (int px = 0; px <= iw; px += 2)
		{
			final double py = ih - 8 + 3 * Math.sin(px / 13.0);
			if (px == 0)
			{
				river.moveTo(px, py);
			}
			else
			{
				river.lineTo(px, py);
			}
		}
		neon(s, river, rgb(255, 150, 30), 4f, 7f, 0.75);
		final Random random = new Random(31);
		final Path2D.Double cracks = new Path2D.Double();
		for (int i = 0; i < 6; i++)
		{
			double cx = random.nextDouble() * iw * 0.7;
			double cy = ih - 10;
			cracks.moveTo(cx, cy);
			for (int k = 0; k < 4; k++)
			{
				cx += -8 + 16 * random.nextDouble();
				cy -= 4 + 5 * random.nextDouble();
				cracks.lineTo(cx, cy);
			}
		}
		neon(s, cracks, rgba(255, 110, 20, 210), 1f, 4f, 0.45);
		SceneSky.veil(s, iw, ih, 80, 20);
		s.setStroke(new BasicStroke(1f));
		s.setColor(rgb(255, 230, 120));
		s.draw(river);
		for (int i = 0; i < 26; i++)
		{
			final double ex = iw * 0.35 + (iw - 6 - iw * 0.35) * random.nextDouble();
			final double ey = 4 + (ih - 14 - 4) * random.nextDouble();
			blob(s, ex, ey, 5.5, 5.5, rgba(255, 120, 30, 70));
			s.setColor(rgb(255, 220, 120));
			s.fill(circle(ex, ey, 0.8));
		}
		s.dispose();

		ring(g, rgba(255, 110, 20, 180), w, h, 2, 3, 4, true);
		ring(g, rgb(40, 16, 10), w, h, 1, 1, 4, false);
		ring(g, rgb(255, 140, 40), w, h, 2, 1, 4, false);
	}

	// ---------------------------------------------------------------------------------------------------------- Runic

	private static void runic(Graphics2D g, int w, int h)
	{
		final int iw = w - 6;
		final int ih = h - 6;
		final Graphics2D s = inside(g, w, h, 3, 2);
		vertical(s, iw, ih, new float[] {0f, 1f}, rgb(58, 60, 66), rgb(28, 30, 34));
		final Random random = new Random(37);
		grain(s, iw, ih, random, 0.7, 40, rgb(80, 82, 90), rgb(24, 26, 30));
		final Path2D.Double cracks = new Path2D.Double();
		for (int i = 0; i < 5; i++)
		{
			double x = random.nextDouble() * iw;
			double y = random.nextDouble() * ih;
			cracks.moveTo(x, y);
			for (int k = 0; k < 5; k++)
			{
				x += -12 + 24 * random.nextDouble();
				y += -6 + 16 * random.nextDouble();
				cracks.lineTo(x, y);
			}
		}
		s.setStroke(new BasicStroke(1f));
		s.setColor(rgba(18, 18, 22, 160));
		s.draw(cracks);

		final double cx = iw - 70;
		final double cy = ih / 2.0 + 4;
		final Path2D.Double magic = new Path2D.Double();
		for (final double rr : new double[] {44, 36, 22})
		{
			magic.append(circle(cx, cy, rr), false);
		}
		for (int i = 0; i < 6; i++)
		{
			final double a = i / 6.0 * 2 * Math.PI;
			final double b = (i + 2) / 6.0 * 2 * Math.PI;
			magic.moveTo(cx + 36 * Math.cos(a), cy + 36 * Math.sin(a));
			magic.lineTo(cx + 36 * Math.cos(b), cy + 36 * Math.sin(b));
		}
		for (int i = 0; i < 16; i++)
		{
			final double a = i / 16.0 * 2 * Math.PI;
			final double gx = cx + 40 * Math.cos(a);
			final double gy = cy + 40 * Math.sin(a);
			for (int k = 0; k < 2; k++)
			{
				magic.moveTo(gx + (random.nextInt(3) - 1) * 2, gy + (random.nextInt(3) - 1) * 2);
				magic.lineTo(gx + (random.nextInt(3) - 1) * 2, gy + (random.nextInt(3) - 1) * 2);
			}
		}
		neon(s, magic, rgba(90, 220, 255, 190), 1f, 5f, 0.6);
		SceneSky.veil(s, iw, ih, 80, 20);

		final Path2D.Double runes = new Path2D.Double();
		final int[] across = {0, 3, 6};
		final int[] down = {1, 5, 9};
		final double top = ih - 13;
		for (int x = 10; x < iw - 12; x += 11)
		{
			final int strokes = 2 + random.nextInt(3);
			for (int k = 0; k < strokes; k++)
			{
				runes.moveTo(x + across[random.nextInt(3)], top + down[random.nextInt(3)]);
				runes.lineTo(x + across[random.nextInt(3)], top + down[random.nextInt(3)]);
			}
		}
		neon(s, runes, rgba(110, 230, 255, 245), 1f, 5f, 0.7);
		s.dispose();

		ring(g, rgb(110, 112, 120), w, h, 1, 1, 2, false);
		ring(g, rgb(20, 20, 24), w, h, 3, 1, 2, false);
		ring(g, rgba(90, 220, 255, 120), w, h, 3, 2, 2, true);
	}

	// --------------------------------------------------------------------------------------------------------- Spooky

	private static void spooky(Graphics2D g, int w, int h)
	{
		final int iw = w - 6;
		final int ih = h - 6;
		final Graphics2D s = inside(g, w, h, 3, 5);
		vertical(s, iw, ih, new float[] {0f, 0.6f, 1f}, rgb(46, 22, 60), rgb(24, 12, 34), rgb(10, 6, 14));
		stars(s, iw, ih, (int) (iw * ih / 900.0), new Random(41));
		final double mx = iw - 56;
		final double my = 30;
		glow(s, mx, my, 50, 50, rgba(255, 236, 180, 110), 16);
		glow(s, iw - 105, ih - 40, 65, 30, rgba(150, 120, 190, 70), 10);

		final Color col = rgb(14, 8, 18);
		s.setColor(col);
		s.fill(new Ellipse2D.Double(iw - 200, ih - 26, 240, 86));
		final double hx = iw - 110;
		final double hb = ih - 20;
		s.fill(new Rectangle2D.Double(hx - 18, hb - 22, 36, 22));
		s.fill(path(true, hx - 22, hb - 22, hx, hb - 38, hx + 22, hb - 22));
		s.fill(new Rectangle2D.Double(hx + 8, hb - 44, 6, 16));
		s.fill(path(true, hx + 6, hb - 44, hx + 11, hb - 52, hx + 16, hb - 44));
		branch(s, new double[] {iw - 30, ih - 22, iw - 32, ih - 50, iw - 24, ih - 64}, 3, 1, col);
		branch(s, new double[] {iw - 32, ih - 44, iw - 46, ih - 56, iw - 52, ih - 54}, 1.6, 0.8, col);
		branch(s, new double[] {iw - 31, ih - 52, iw - 18, ih - 58}, 1.4, 0.8, col);
		glow(s, hx, hb - 14, 26, 14, rgba(255, 160, 50, 70), 8);
		final double[][] bats = {{iw - 64, 24, 0.9}, {iw - 92, 14, 0.7}, {iw - 40, 46, 0.6}, {iw - 120, 30, 0.55}};
		s.setColor(rgb(6, 2, 8));
		for (final double[] bat : bats)
		{
			bat(s, bat[0], bat[1], bat[2]);
		}
		mist(s, iw, ih, ih - 16, rgba(140, 110, 170, 70), 7);
		s.setStroke(new BasicStroke(1f));
		final Path2D.Double spokes = new Path2D.Double();
		final Path2D.Double rungs = new Path2D.Double();
		for (int i = 0; i < 6; i++)
		{
			final double a = -i / 5.0 * (Math.PI / 2);
			spokes.moveTo(0, ih);
			spokes.lineTo(40 * Math.cos(a), ih + 40 * Math.sin(a));
		}
		for (final int rr : new int[] {10, 18, 27, 36})
		{
			for (int i = 0; i < 6; i++)
			{
				final double a = -i / 5.0 * (Math.PI / 2);
				if (i == 0)
				{
					rungs.moveTo(rr * Math.cos(a), ih + rr * Math.sin(a));
				}
				else
				{
					rungs.lineTo(rr * Math.cos(a), ih + rr * Math.sin(a));
				}
			}
		}
		s.setColor(rgba(220, 220, 236, 110));
		s.draw(spokes);
		s.setColor(rgba(220, 220, 236, 90));
		s.draw(rungs);
		SceneSky.veil(s, iw, ih, 70, 10);

		s.setColor(rgb(252, 240, 196));
		s.fill(circle(mx, my, 20));
		s.setColor(rgb(222, 206, 160));
		for (final double[] crater : new double[][] {{-7, -5, 4}, {5, 4, 5}, {-2, 9, 3}, {8, -8, 2.5}})
		{
			s.fill(circle(mx + crater[0], my + crater[1], crater[2]));
		}
		s.setColor(rgb(255, 190, 70));
		for (final double[] window : new double[][] {{hx - 10, hb - 16}, {hx + 4, hb - 16}, {hx - 3, hb - 28}})
		{
			s.fill(new Rectangle2D.Double(window[0], window[1], 4, 5));
		}
		final double px = 54;
		final double py = ih - 9;
		s.setColor(rgb(230, 110, 20));
		s.fill(new Ellipse2D.Double(px - 8, py - 6, 16, 12));
		s.setColor(rgb(40, 90, 20));
		s.fill(new Rectangle2D.Double(px - 1, py - 9, 2, 3));
		s.setColor(rgb(255, 230, 120));
		s.fill(path(true, px - 5, py - 2, px - 3, py - 4, px - 1, py - 2));
		s.fill(path(true, px + 1, py - 2, px + 3, py - 4, px + 5, py - 2));
		s.fill(path(true, px - 5, py + 1, px + 5, py + 1, px + 3, py + 3, px - 3, py + 3));
		glow(s, px, py, 16, 10, rgba(255, 150, 40, 90), 6);
		s.dispose();

		ring(g, rgba(255, 120, 20, 170), w, h, 2, 3, 5, true);
		ring(g, rgb(255, 140, 40), w, h, 2, 1, 5, false);
	}

	/** A bat in the current colour: two mirrored wings and a small body, {@code s} times the size of its pattern. */
	private static void bat(Graphics2D g, double x, double y, double s)
	{
		final double[] wing = {0, 0, 3, -3, 5, -2, 7, -5, 9, -2, 12, -4, 11, 0, 9, 1, 7, 0, 5, 2, 3, 1};
		for (final int sign : new int[] {1, -1})
		{
			final double[] xy = new double[wing.length];
			for (int i = 0; i < wing.length; i += 2)
			{
				xy[i] = x + sign * wing[i] * s;
				xy[i + 1] = y + wing[i + 1] * s;
			}
			g.fill(path(true, xy));
		}
		g.fill(new Ellipse2D.Double(x - 1.2 * s, y - 1.5 * s, 2.4 * s, 3.5 * s));
	}

	/** A limb: the polyline {@code xy} drawn segment by segment, its width running from {@code w0} to {@code w1}. */
	private static void branch(Graphics2D g, double[] xy, double w0, double w1, Color colour)
	{
		final int segments = xy.length / 2 - 1;
		g.setColor(colour);
		for (int i = 0; i < segments; i++)
		{
			final double width = w0 + (w1 - w0) * i / Math.max(1, segments - 1);
			g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Line2D.Double(xy[2 * i], xy[2 * i + 1], xy[2 * i + 2], xy[2 * i + 3]));
		}
	}

	// -------------------------------------------------------------------------------------------------------- helpers

	/**
	 * A copy of {@code g} whose origin is the scene's own top left corner, {@code inset} px in, and which paints only
	 * inside the scene's {@code w - 2 * inset} by {@code h - 2 * inset} area, its corners rounded to {@code radius}.
	 */
	private static Graphics2D inside(Graphics2D g, int w, int h, int inset, int radius)
	{
		final Graphics2D s = (Graphics2D) g.create();
		s.translate(inset, inset);
		final int iw = w - 2 * inset;
		final int ih = h - 2 * inset;
		s.clip(radius > 0 ? new RoundRectangle2D.Double(0, 0, iw, ih, 2 * radius, 2 * radius)
			: new Rectangle2D.Double(0, 0, iw, ih));
		return s;
	}

	/** Fills {@code w} by {@code h} with a gradient running down through {@code colours} at the fractions {@code at}. */
	private static void vertical(Graphics2D g, int w, int h, float[] at, Color... colours)
	{
		g.setPaint(new LinearGradientPaint(0f, 0f, 0f, Math.max(1, h - 1), at, colours));
		g.fillRect(0, 0, w, h);
	}

	/** A bar of the brass frame: the gradient from {@code from} to {@code to} across it, sideways or downwards. */
	private static void bar(Graphics2D g, int x0, int y0, int x1, int y1, Color from, Color to, boolean sideways)
	{
		g.setPaint(sideways ? new GradientPaint(x0, y0, from, x1, y0, to) : new GradientPaint(x0, y0, from, x0, y1, to));
		g.fillRect(x0, y0, x1 - x0, y1 - y0);
	}

	/** Specks over {@code w} by {@code h}: {@code density} of one to each px, in one of two colours at {@code alpha}. */
	private static void grain(Graphics2D g, int w, int h, Random random, double density, int alpha, Color first,
		Color second)
	{
		final Color a = new Color(first.getRed(), first.getGreen(), first.getBlue(), alpha);
		final Color b = new Color(second.getRed(), second.getGreen(), second.getBlue(), alpha);
		final int count = (int) (w * h * density);
		for (int i = 0; i < count; i++)
		{
			final int x = (int) (random.nextDouble() * w);
			final int y = (int) (random.nextDouble() * h);
			g.setColor(random.nextInt(2) == 0 ? a : b);
			g.fillRect(x, y, 1, 1);
		}
	}

	/** {@code count} stars in the top {@code height} px of a {@code w} wide area: dots of 1 px, a few of 2, alpha 70 to 235. */
	private static void stars(Graphics2D g, int w, int height, int count, Random random)
	{
		for (int i = 0; i < count; i++)
		{
			final int x = random.nextInt(Math.max(1, w));
			final int y = random.nextInt(Math.max(1, height));
			final int size = random.nextInt(8) == 0 ? 2 : 1;
			g.setColor(new Color(235, 235, 255, 70 + random.nextInt(166)));
			g.fillRect(x, y, size, size);
		}
	}

	/**
	 * A rim of {@code thickness} px, {@code in} px inside the edge of a {@code w} by {@code h} card and with its corners
	 * rounded to {@code radius}, in {@code colour}; with {@code soft} it is only the faint halo such a rim throws.
	 */
	private static void ring(Graphics2D g, Color colour, int w, int h, int in, int thickness, int radius, boolean soft)
	{
		final Shape shape = ringShape(w, h, in, thickness, radius);
		if (soft)
		{
			halo(g, shape, colour, thickness, 2.5f, 0.4);
		}
		else
		{
			g.setColor(colour);
			g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
			g.draw(shape);
		}
	}

	/** The middle line of a rim of {@code thickness} px that lies {@code in} px inside the edge. */
	private static Shape ringShape(int w, int h, int in, int thickness, int radius)
	{
		final double half = thickness / 2.0;
		return new RoundRectangle2D.Double(in + half, in + half, w - 2 * in - thickness, h - 2 * in - thickness,
			Math.max(0, 2 * radius - thickness), Math.max(0, 2 * radius - thickness));
	}

	/** A line with its glow: the halo under it, then the line itself, {@code width} px, in {@code colour}. */
	private static void neon(Graphics2D g, Shape shape, Color colour, float width, float spread, double strength)
	{
		halo(g, shape, colour, width, spread, strength);
		g.setStroke(new BasicStroke(width));
		g.setColor(colour);
		g.draw(shape);
	}

	/**
	 * The soft light round a line, in four strokes of one alpha each, the widest first, so that it is {@code strength}
	 * times {@code colour}'s alpha at the line and falls to a quarter of that {@code spread} px out.
	 */
	private static void halo(Graphics2D g, Shape shape, Color colour, float width, float spread, double strength)
	{
		final int steps = 4;
		final int each = (int) Math.round(colour.getAlpha() * strength * 1.2 / steps);
		g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), each));
		for (int k = steps; k >= 1; k--)
		{
			g.setStroke(new BasicStroke(width + 2f * spread * k / steps, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(shape);
		}
	}

	/**
	 * A blurred ellipse of {@code rx} by {@code ry} px with its centre at ({@code cx}, {@code cy}): what a gaussian blur of
	 * {@code sigma} px would leave of it, painted as a radial gradient out to {@code 2 * sigma} px past its edge whose alphas
	 * follow the blur's own fall (the normal curve), {@code colour}'s alpha times how much of it the blur keeps at the centre.
	 */
	private static void glow(Graphics2D g, double cx, double cy, double rx, double ry, Color colour, double sigma)
	{
		final double root = sigma * Math.sqrt(2);
		final double peak = erf(rx / root) * erf(ry / root);
		final double mean = (rx + ry) / 2;
		final double reach = mean + 2 * sigma;
		final float[] at = new float[8];
		final Color[] colours = new Color[8];
		for (int i = 0; i < 8; i++)
		{
			final double u = i / 7.0;
			final double kept = 0.5 * (1 + erf((mean - u * reach) / root)) / (0.5 * (1 + erf(mean / root)));
			at[i] = (float) u;
			colours[i] = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(),
				i == 7 ? 0 : (int) Math.round(colour.getAlpha() * peak * Math.min(1.0, kept)));
		}
		radial(g, cx, cy, rx + 2 * sigma, ry + 2 * sigma, at, colours);
	}

	/** A soft blob: {@code colour} at its centre fading to alpha 0 at the radii {@code rx} and {@code ry}. */
	private static void blob(Graphics2D g, double cx, double cy, double rx, double ry, Color colour)
	{
		final Color[] colours = new Color[BLOB_ALPHA.length];
		for (int i = 0; i < colours.length; i++)
		{
			colours[i] = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(),
				Math.round(colour.getAlpha() * BLOB_ALPHA[i]));
		}
		radial(g, cx, cy, rx, ry, BLOB_AT, colours);
	}

	/** Fills the ellipse of radii {@code rx} and {@code ry} round ({@code cx}, {@code cy}) with a scaled radial gradient. */
	private static void radial(Graphics2D g, double cx, double cy, double rx, double ry, float[] at, Color[] colours)
	{
		final AffineTransform before = g.getTransform();
		final Paint paint = g.getPaint();
		g.translate(cx, cy);
		g.scale(rx, ry);
		g.setPaint(new RadialGradientPaint(0f, 0f, 1f, at, colours));
		g.fill(new Rectangle2D.Double(-1, -1, 2, 2));
		g.setTransform(before);
		g.setPaint(paint);
	}

	/** A band of {@code colour} that shows from {@code 2 * sigma} px above {@code top} and is whole {@code 2 * sigma} below it. */
	private static void mist(Graphics2D g, int w, int h, double top, Color colour, double sigma)
	{
		final float[] at = {0f, 0.25f, 0.5f, 0.75f, 1f};
		final float[] share = {0f, 0.16f, 0.5f, 0.84f, 1f};
		final Color[] colours = new Color[at.length];
		for (int i = 0; i < colours.length; i++)
		{
			colours[i] = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(),
				Math.round(colour.getAlpha() * share[i]));
		}
		final double from = top - 2 * sigma;
		g.setPaint(new LinearGradientPaint(0f, (float) from, 0f, (float) (top + 2 * sigma), at, colours));
		g.fill(new Rectangle2D.Double(0, from, w, h - from));
	}

	/** The error function, to 1.5e-7 (Abramowitz and Stegun 7.1.26). */
	private static double erf(double x)
	{
		final double t = 1.0 / (1.0 + 0.3275911 * Math.abs(x));
		final double y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t
			+ 0.254829592) * t * Math.exp(-x * x);
		return x < 0 ? -y : y;
	}

	private static Ellipse2D.Double circle(double cx, double cy, double r)
	{
		return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
	}

	/** The polygon (closed) or polyline (open) through the points {@code xy} given as x, y, x, y and so on. */
	private static Path2D.Double path(boolean closed, double... xy)
	{
		final Path2D.Double path = new Path2D.Double();
		path.moveTo(xy[0], xy[1]);
		for (int i = 2; i + 1 < xy.length; i += 2)
		{
			path.lineTo(xy[i], xy[i + 1]);
		}
		if (closed)
		{
			path.closePath();
		}
		return path;
	}

	private static Color rgb(int red, int green, int blue)
	{
		return new Color(red, green, blue);
	}

	private static Color rgba(int red, int green, int blue, int alpha)
	{
		return new Color(red, green, blue, alpha);
	}
}
