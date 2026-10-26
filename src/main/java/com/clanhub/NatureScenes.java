package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.AlphaComposite;
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
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The scenes of the six nature card themes, painted on the {@link Graphics2D} that {@link SceneSky} makes for a card's
 * ground. Each is a picture inside a border, the picture 3 px in from the card's edge with rounded corners, laid in
 * layers from the back to the front, with a dark veil over it (heavier at the left, where the words are) at the layer
 * the scene says:
 *
 * <ul>
 * <li>Forest: a green night with four soft rays of light from the top right, two rows of pines and a mist along the
 * bottom, vines hanging in the corners at the top, fireflies in the right two thirds; a wooden frame with a leaf in
 * each corner.</li>
 * <li>Ocean: a blue deep that darkens downwards, five soft rays, a few ripples of light at the top, a school of fish
 * and a kelp forest at the right, coral at the left, bubbles at the right; a glowing pale blue frame with a pearl in
 * each corner.</li>
 * <li>Autumn: a copper dusk with a low sun at the right, a bare branch reaching in from the top right, a pile of maple
 * leaves along the bottom and a few falling; an orange frame with a maple leaf in each corner.</li>
 * <li>Sakura: a purple night with a pale moon at the top right, a torii gate at the bottom, a branch heavy with
 * blossom reaching in from the top right and petals drifting down; a glowing pink frame.</li>
 * <li>Garden: a green day with a warm sun at the top right, soft rays, a white picket fence along the bottom with
 * tulips and daisies in front of it, a rose vine up the right side and two butterflies; a white frame with a rose in
 * each corner.</li>
 * <li>Lavender: a dusk sky that lightens toward the horizon, a few stars, a lone tree on the horizon, a field of
 * lavender in rows that grow larger toward the front, a few poppies and fireflies; a glowing lilac frame.</li>
 * </ul>
 *
 * <p>Choice: no blur is used. A soft edge or a glow is a radial gradient shaped as the blur would shape it, or a few
 * strokes of falling alpha, so each scene is plain Java2D. Where the picture is laid with an {@link AlphaComposite#Src
 * Src} layer, shapes that overlap replace each other and so do not add up their alpha. The random places (the pines, the
 * leaves, the fireflies, the petals) are laid from fixed seeds, so the same size gives the same picture.
 */
final class NatureScenes
{
	/** How far in from the card's edge the picture starts, in px, for the border to frame it. */
	private static final int INSET = 3;

	/** The least width or height of a picture, in px; a card smaller than that and its inset is a plain gradient. */
	private static final int MIN_SIDE = 24;

	private static final double TAU = 2 * Math.PI;

	private NatureScenes()
	{
	}

	/**
	 * Paints {@code theme}'s whole ground, {@code width} by {@code height} px with its top-left corner at the graphics'
	 * origin: the picture with its gradient and its layers, its veil, the border and a 1 px edge in the theme's edge
	 * colour. A card too small for the picture gets the gradient and the edge alone.
	 */
	static void paint(CardTheme theme, Graphics2D g, int width, int height)
	{
		final int w = width - 2 * INSET;
		final int h = height - 2 * INSET;
		if (w < MIN_SIDE || h < MIN_SIDE)
		{
			g.setPaint(new GradientPaint(0f, 0f, SceneSky.groundTop(theme), 0f, Math.max(1, height - 1),
				SceneSky.groundBottom(theme)));
			g.fillRect(0, 0, width, height);
			edge(g, theme, width, height);
			return;
		}
		final AffineTransform origin = g.getTransform();
		final Shape clip = g.getClip();
		g.translate(INSET, INSET);
		switch (theme)
		{
			case FOREST:
				forest(g, w, h);
				break;
			case OCEAN:
				ocean(g, w, h);
				break;
			case AUTUMN:
				autumn(g, w, h);
				break;
			case SAKURA:
				sakura(g, w, h);
				break;
			case GARDEN:
				garden(g, w, h);
				break;
			default:
				lavender(g, w, h);
				break;
		}
		g.setClip(clip);
		g.setTransform(origin);
		switch (theme)
		{
			case FOREST:
				forestBorder(g, width, height);
				break;
			case OCEAN:
				oceanBorder(g, width, height);
				break;
			case AUTUMN:
				autumnBorder(g, width, height);
				break;
			case SAKURA:
				sakuraBorder(g, width, height);
				break;
			case GARDEN:
				gardenBorder(g, width, height);
				break;
			default:
				lavenderBorder(g, width, height);
				break;
		}
		edge(g, theme, width, height);
	}

	// ------------------------------------------------------------------------------------------------ the six scenes

	/** Forest: rays, two rows of pines, mist, the veil, vines and fireflies. */
	private static void forest(Graphics2D g, int w, int h)
	{
		ground(g, w, h, 5, new float[] {0f, 0.55f, 1f}, rgb(22, 54, 40), rgb(12, 34, 22), rgb(6, 16, 10));
		final double[] rays = {w - 40, w - 90, w - 140, w - 200};
		for (int i = 0; i < rays.length; i++)
		{
			final double x = rays[i];
			soft(g, poly(x - 8, 0, x + 14, 0, x - 30, h, x - 70, h), rgb(210, 240, 150, i % 2 == 1 ? 26 : 38), 3);
		}
		final Random random = new Random(3);
		final Layer far = new Layer(w, h);
		for (int x = -10; x < w + 20; x += 14)
		{
			pine(far.g, x + u(random, -4, 4), h + 4, u(random, 34, 52), u(random, 9, 13), rgb(28, 70, 54, 150));
		}
		far.over(g);
		final double top = h - 44;
		final float[] at = new float[9];
		final Color[] colours = new Color[9];
		for (int i = 0; i < 9; i++)
		{
			at[i] = i / 8f;
			final double y = top + i / 8.0 * 36;
			colours[i] = rgb(150, 200, 170, i == 0 ? 0 : (int) Math.round(60 * phi((y - (h - 26)) / 8)));
		}
		g.setPaint(new LinearGradientPaint(0f, (float) top, 0f, (float) (top + 36), at, colours));
		g.fill(new Rectangle2D.Double(0, top, w, h - top));
		final Layer near = new Layer(w, h);
		for (int x = -6; x < 60; x += 16)
		{
			pine(near.g, x + u(random, -3, 3), h + 6, u(random, 46, 70), u(random, 12, 17), rgb(8, 26, 16, 240));
		}
		for (int x = w - 70; x < w + 10; x += 15)
		{
			pine(near.g, x + u(random, -3, 3), h + 6, u(random, 46, 70), u(random, 12, 17), rgb(8, 26, 16, 240));
		}
		near.over(g);
		SceneSky.veil(g, w, h, 70, 20);
		final Layer vines = new Layer(w, h);
		final Color[] greens = {rgb(46, 120, 52, 240), rgb(70, 150, 60, 230), rgb(32, 96, 44, 245),
			rgb(100, 170, 70, 220)};
		final double[] hangs = {w - 22, w - 48, w - 78, w - 104, 18, 40};
		for (final double vx : hangs)
		{
			final double length = u(random, 14, 34);
			vines.g.setColor(rgb(40, 70, 30, 230));
			vines.g.setStroke(new BasicStroke(1f));
			vines.g.draw(new Line2D.Double(vx, 0, vx + u(random, -3, 3), length));
			for (int k = 0; k < (int) (length / 4); k++)
			{
				leaf(vines.g, vx + (random.nextBoolean() ? -3 : 3), 2 + k * 4, u(random, 3.5, 5.5), u(random, 0.3, 2.8),
					greens[random.nextInt(greens.length)]);
			}
		}
		for (int i = 0; i < 18; i++)
		{
			leaf(vines.g, u(random, w - 120, w), u(random, -4, 8), u(random, 5, 9), u(random, 0, Math.PI),
				greens[random.nextInt(greens.length)]);
		}
		vines.over(g);
		final double[][] flies = new double[22][];
		for (int i = 0; i < flies.length; i++)
		{
			flies[i] = new double[] {u(random, w * 0.3, w - 6), u(random, h * 0.35, h - 8)};
			glow(g, flies[i][0], flies[i][1], 4, 4, rgb(220, 255, 110, 140), 2.2);
		}
		g.setColor(rgb(250, 255, 170));
		for (final double[] fly : flies)
		{
			g.fill(new Ellipse2D.Double(fly[0] - 0.8, fly[1] - 0.8, 1.6, 1.6));
		}
	}

	/** Ocean: rays, ripples, a school of fish, kelp and coral, the veil and bubbles. */
	private static void ocean(Graphics2D g, int w, int h)
	{
		ground(g, w, h, 5, new float[] {0f, 0.35f, 1f}, rgb(30, 120, 170), rgb(12, 70, 120), rgb(2, 16, 40));
		final double[] rayAt = {w * 0.12, w * 0.32, w * 0.55, w * 0.74, w * 0.92};
		for (int i = 0; i < rayAt.length; i++)
		{
			final double x = rayAt[i];
			soft(g, poly(x - 6, 0, x + 8, 0, x + 46, h, x + 4, h), rgb(190, 240, 255, i % 2 == 1 ? 34 : 48), 4);
		}
		final Random random = new Random(5);
		for (int i = 0; i < 46; i++)
		{
			final double x = u(random, 0, w);
			final double y = u(random, 0, 26);
			final Path2D.Double ripple = new Path2D.Double();
			for (int k = 0; k < 7; k++)
			{
				final double t = k / 6.0 * TAU;
				final double px = x + u(random, -1, 1) + 6 * Math.cos(t);
				final double py = y + 3 * Math.sin(t);
				if (k == 0)
				{
					ripple.moveTo(px, py);
				}
				else
				{
					ripple.lineTo(px, py);
				}
			}
			// Two strokes, a faint wide one and a finer one on it, give the ripple the soft edge a blur would.
			g.setColor(rgb(220, 250, 255, 28));
			g.setStroke(new BasicStroke(2.4f));
			g.draw(ripple);
			g.setColor(rgb(220, 250, 255, 52));
			g.setStroke(new BasicStroke(1f));
			g.draw(ripple);
		}
		final Layer fish = new Layer(w, h);
		fish.g.setColor(rgb(10, 40, 70, 170));
		for (int i = 0; i < 14; i++)
		{
			final double fx = w - 150 + (i % 7) * 13 + u(random, -3, 3);
			final double fy = 38 + (i / 7) * 10 + 0.8 * (i % 7) + u(random, -2, 2);
			fish.g.fill(new Ellipse2D.Double(fx - 4, fy - 1.5, 8, 3));
			fish.g.fill(poly(fx + 3.5, fy, fx + 7, fy - 2.5, fx + 7, fy + 2.5));
		}
		fish.over(g);
		final Layer floor = new Layer(w, h);
		floor.g.setStroke(new BasicStroke(2f));
		for (int kx = w - 60; kx < w - 4; kx += 7)
		{
			final Path2D.Double stalk = new Path2D.Double();
			final List<double[]> points = new ArrayList<>();
			final int stop = (int) (h - u(random, 40, 70));
			for (int y = h; y > stop; y -= 3)
			{
				points.add(new double[] {kx + 3 * Math.sin(y / 7.0 + kx), y});
			}
			for (int i = 0; i < points.size(); i++)
			{
				if (i == 0)
				{
					stalk.moveTo(points.get(i)[0], points.get(i)[1]);
				}
				else
				{
					stalk.lineTo(points.get(i)[0], points.get(i)[1]);
				}
			}
			floor.g.setColor(rgb(20, 110, 70, 220));
			floor.g.draw(stalk);
			for (int i = 0; i < points.size(); i += 3)
			{
				leaf(floor.g, points.get(i)[0] + 3, points.get(i)[1], 3, 0.5, rgb(40, 150, 90, 200));
			}
		}
		floor.g.setColor(rgb(240, 110, 120, 230));
		for (final int cx : new int[] {14, 34, 52})
		{
			for (int k = 0; k < 5; k++)
			{
				final double a = -Math.PI / 2 + (k - 2) * 0.45;
				floor.g.draw(new Line2D.Double(cx, h, cx + 14 * Math.cos(a), h + 14 * Math.sin(a)));
			}
		}
		floor.over(g);
		SceneSky.veil(g, w, h, 70, 15);
		final Layer bubbles = new Layer(w, h);
		bubbles.g.setStroke(new BasicStroke(1f));
		for (int i = 0; i < 22; i++)
		{
			final double x = u(random, w - 90, w - 6);
			final double y = u(random, 6, h - 6);
			final double s = u(random, 1.2, 4.5);
			bubbles.g.setColor(rgb(210, 245, 255, 190));
			bubbles.g.draw(new Ellipse2D.Double(x - s + 0.5, y - s + 0.5, 2 * s - 1, 2 * s - 1));
			bubbles.g.setColor(rgb(255, 255, 255, 220));
			bubbles.g.fill(new Ellipse2D.Double(x - s * 0.55, y - s * 0.6, s * 0.4, s * 0.4));
		}
		bubbles.over(g);
	}

	/** Autumn: the low sun, a bare branch, a pile of maple leaves, the veil and leaves in the air. */
	private static void autumn(Graphics2D g, int w, int h)
	{
		ground(g, w, h, 5, new float[] {0f, 0.45f, 1f}, rgb(150, 70, 26), rgb(90, 38, 16), rgb(34, 14, 8));
		glow(g, w - 50, h * 0.55, 90, 60, rgb(255, 180, 70, 150), 26);
		glow(g, w - 50, h * 0.55, 30, 30, rgb(255, 230, 150, 200), 10);
		final Layer tree = new Layer(w, h);
		final Color wood = rgb(36, 16, 8, 245);
		branch(tree.g, new double[][] {{w + 4, -4}, {w - 30, 14}, {w - 70, 22}, {w - 120, 18}, {w - 160, 30}}, 6, 1.5, wood);
		branch(tree.g, new double[][] {{w - 30, 14}, {w - 46, 36}, {w - 52, 52}}, 3, 1, wood);
		branch(tree.g, new double[][] {{w - 70, 22}, {w - 86, 8}, {w - 100, -2}}, 2.5, 1, wood);
		branch(tree.g, new double[][] {{w - 120, 18}, {w - 130, 40}}, 2, 1, wood);
		tree.over(g);
		final Random random = new Random(7);
		final Color[] leaves = {rgb(224, 80, 30, 240), rgb(240, 160, 40, 240), rgb(190, 40, 30, 240),
			rgb(250, 200, 70, 230), rgb(210, 110, 40, 240)};
		final Layer pile = new Layer(w, h);
		for (int i = 0; i < 60; i++)
		{
			maple(pile.g, u(random, 0, w), h - u(random, -2, 8), u(random, 3, 5), u(random, 0, 6.3),
				leaves[random.nextInt(leaves.length)]);
		}
		pile.over(g);
		SceneSky.veil(g, w, h, 90, 25);
		final Layer fall = new Layer(w, h);
		for (int i = 0; i < 16; i++)
		{
			maple(fall.g, u(random, w * 0.45, w - 6), u(random, 8, h - 16), u(random, 4, 6.5), u(random, 0, 6.3),
				leaves[random.nextInt(leaves.length)]);
		}
		fall.over(g);
	}

	/** Sakura: the moon, a torii gate, the veil, the blossoming branch and petals. */
	private static void sakura(Graphics2D g, int w, int h)
	{
		ground(g, w, h, 6, new float[] {0f, 0.6f, 1f}, rgb(40, 22, 64), rgb(34, 16, 44), rgb(16, 8, 22));
		final double mx = w - 64;
		final double my = 30;
		glow(g, mx, my, 44, 44, rgb(255, 220, 236, 90), 16);
		final Layer gate = new Layer(w, h);
		final double gx = w - 150;
		final double gb = h;
		gate.g.setColor(rgb(12, 6, 16, 230));
		gate.g.fill(new Rectangle2D.Double(gx - 16, gb - 30, 3, 30));
		gate.g.fill(new Rectangle2D.Double(gx + 13, gb - 30, 3, 30));
		gate.g.fill(poly(gx - 24, gb - 32, gx + 24, gb - 32, gx + 21, gb - 29, gx - 21, gb - 29));
		gate.g.fill(new Rectangle2D.Double(gx - 19, gb - 25, 38, 2));
		gate.over(g);
		SceneSky.veil(g, w, h, 85, 25);
		// The moon is laid over the veil, not under it: the veil would dim it below the white it is meant to be.
		g.setColor(rgb(255, 240, 246));
		g.fill(new Ellipse2D.Double(mx - 20, my - 20, 40, 40));
		final Layer tree = new Layer(w, h);
		final Color wood = rgb(28, 12, 16, 255);
		branch(tree.g, new double[][] {{w + 6, 2}, {w - 26, 10}, {w - 58, 8}, {w - 92, 18}, {w - 128, 14}, {w - 160, 22}}, 5,
			1.3, wood);
		branch(tree.g, new double[][] {{w - 26, 10}, {w - 40, 28}, {w - 44, 44}}, 2.5, 1, wood);
		branch(tree.g, new double[][] {{w - 92, 18}, {w - 104, 36}}, 2, 1, wood);
		final Random random = new Random(11);
		final double[][] spots = {{w - 8, 4}, {w - 18, 12}, {w - 30, 6}, {w - 44, 12}, {w - 54, 4}, {w - 66, 12},
			{w - 78, 16}, {w - 90, 10}, {w - 100, 22}, {w - 112, 14}, {w - 124, 8}, {w - 138, 18}, {w - 152, 24},
			{w - 38, 24}, {w - 46, 38}, {w - 102, 32}, {w - 162, 18}};
		for (final double[] spot : spots)
		{
			blossom(tree.g, spot[0] + u(random, -2, 2), spot[1] + u(random, -2, 2), u(random, 3.5, 5.5),
				rgb(255, 186, 214, 250), rgb(255, 236, 170, 255), u(random, 0, 1.2));
			if (random.nextDouble() < 0.5)
			{
				tree.g.setColor(rgb(255, 140, 190, 255));
				tree.g.fill(new Ellipse2D.Double(spot[0] + 5, spot[1] + 3, 2.5, 2.5));
			}
		}
		tree.over(g);
		final Layer petals = new Layer(w, h);
		for (int i = 0; i < 26; i++)
		{
			leaf(petals.g, u(random, w * 0.3, w - 4), u(random, 24, h - 4), u(random, 1.6, 2.6), u(random, 0, 3.1),
				rgb(255, 196, 222, 150 + random.nextInt(91)));
		}
		petals.over(g);
	}

	/** Garden: the sun and rays, a picket fence, the veil, the bed of tulips and daisies, a rose vine and butterflies. */
	private static void garden(Graphics2D g, int w, int h)
	{
		ground(g, w, h, 6, new float[] {0f, 0.5f, 1f}, rgb(70, 110, 70), rgb(40, 74, 44), rgb(22, 40, 24));
		glow(g, w - 40, 10, 110, 50, rgb(255, 230, 150, 120), 24);
		final double[] rayAt = {w - 30, w - 80, w - 130};
		for (final double x : rayAt)
		{
			soft(g, poly(x - 6, 0, x + 10, 0, x - 40, h, x - 80, h), rgb(255, 240, 180, 30), 4);
		}
		final Layer fence = new Layer(w, h);
		final double fy = h - 34;
		fence.g.setColor(rgb(226, 222, 206, 200));
		fence.g.fill(new Rectangle2D.Double(0, fy + 8, w, 3));
		fence.g.fill(new Rectangle2D.Double(0, fy + 22, w, 3));
		for (int x = 4; x < w; x += 12)
		{
			fence.g.setColor(rgb(236, 232, 216, 220));
			fence.g.fill(poly(x, fy + 4, x + 3, fy, x + 6, fy + 4, x + 6, h, x, h));
			fence.g.setColor(rgb(170, 160, 140, 200));
			fence.g.setStroke(new BasicStroke(1f));
			fence.g.draw(new Line2D.Double(x + 6, fy + 4, x + 6, h));
		}
		fence.over(g);
		SceneSky.veil(g, w, h, 85, 20);
		final Random random = new Random(43);
		final Layer bed = new Layer(w, h);
		final Color green = rgb(50, 120, 50, 255);
		for (int x = 2; x < w; x += 6)
		{
			leaf(bed.g, x + u(random, -2, 2), h - u(random, 1, 5), u(random, 3, 5), u(random, -2.2, -0.9),
				rgb(60, 140, 60, 255));
		}
		final Color[] tulips = {rgb(230, 50, 70, 255), rgb(250, 200, 60, 255), rgb(250, 120, 170, 255),
			rgb(190, 90, 220, 255)};
		for (int x = 6; x < w; x += 11)
		{
			tulip(bed.g, x + u(random, -2, 2), h, u(random, 10, 16), tulips[random.nextInt(tulips.length)], green);
		}
		for (int x = 10; x < w; x += 23)
		{
			daisy(bed.g, x + u(random, -3, 3), h - u(random, 5, 9), u(random, 3, 4));
		}
		bed.over(g);
		final Layer vine = new Layer(w, h);
		final double vx = w - 10;
		final List<double[]> points = new ArrayList<>();
		final Path2D.Double stem = new Path2D.Double();
		for (int y = 0; y < h - 20; y += 3)
		{
			final double[] point = {vx + 3 * Math.sin(y / 9.0), y};
			if (points.isEmpty())
			{
				stem.moveTo(point[0], point[1]);
			}
			else
			{
				stem.lineTo(point[0], point[1]);
			}
			points.add(point);
		}
		vine.g.setColor(rgb(40, 90, 40, 255));
		vine.g.setStroke(new BasicStroke(1f));
		vine.g.draw(stem);
		for (int i = 0; i * 3 < points.size(); i++)
		{
			final double[] point = points.get(i * 3);
			leaf(vine.g, point[0] - 3, point[1], 3.5, i % 2 == 1 ? 0.6 : -0.6, rgb(60, 140, 60, 255));
			if (i % 2 == 0)
			{
				rose(vine.g, point[0] + 1, point[1] + 2, 3.2, rgb(210, 30, 60, 255), rgb(120, 10, 30, 255));
			}
		}
		butterfly(vine.g, w - 60, 26, 6, rgb(90, 170, 255, 240), rgb(255, 255, 255, 230));
		butterfly(vine.g, w - 112, 44, 5, rgb(255, 150, 40, 240), rgb(40, 20, 10, 230));
		vine.over(g);
	}

	/** Lavender: the dusk sky, stars, a lone tree, the field in nine rows, the veil, poppies and fireflies. */
	private static void lavender(Graphics2D g, int w, int h)
	{
		ground(g, w, h, 6, new float[] {0f, 0.55f, 0.79f, 0.83f, 1f}, rgb(58, 40, 96), rgb(150, 90, 150),
			rgb(236, 150, 140), rgb(70, 44, 110), rgb(34, 20, 60));
		final double horizon = h * 0.81;
		glow(g, w - 80, horizon, 70, 24, rgb(255, 200, 150, 150), 14);
		final Random stars = new Random(47);
		final Color[] tints = {rgb(255, 255, 255), rgb(255, 236, 214), rgb(214, 226, 255)};
		for (int i = 0; i < w * h / 1400; i++)
		{
			final Color tint = tints[stars.nextInt(tints.length)];
			final int x = stars.nextInt(w);
			final int y = stars.nextInt(h);
			final int size = stars.nextInt(5) == 0 ? 2 : 1;
			g.setColor(rgb(tint.getRed(), tint.getGreen(), tint.getBlue(), 70 + stars.nextInt(166)));
			g.fillRect(x, y, size, size);
		}
		final Layer tree = new Layer(w, h);
		final double tx = w - 150;
		tree.g.setColor(rgb(30, 18, 40, 255));
		tree.g.setStroke(new BasicStroke(2f));
		tree.g.draw(new Line2D.Double(tx, horizon, tx, horizon - 14));
		final double[][] crown = {{0, -20, 9}, {-6, -16, 6}, {6, -16, 6}, {0, -26, 5}};
		for (final double[] ball : crown)
		{
			tree.g.fill(new Ellipse2D.Double(tx + ball[0] - ball[2], horizon + ball[1] - ball[2], 2 * ball[2], 2 * ball[2]));
		}
		tree.over(g);
		final Random random = new Random(53);
		final Layer field = new Layer(w, h);
		final int rows = 9;
		for (int row = 0; row < rows; row++)
		{
			final double t = (row + 1) / (double) rows;
			final double y = horizon + (h - horizon) * Math.pow(t, 1.6);
			final double size = 0.6 + 2.6 * Math.pow(t, 1.4);
			final double step = 1.5 + 5 * t;
			// The bud's blue is a little above the picture's 220, so that it is still a clear blue under the veil.
			final Color bud = rgb(150 + (int) (60 * t), 100 + (int) (40 * t), 235, 255);
			for (double x = -10; x < w + 10; x += step + u(random, -0.5, 0.5))
			{
				field.g.setColor(rgb(70, 110, 70, 255));
				field.g.setStroke(new BasicStroke(0.6f));
				field.g.draw(new Line2D.Double(x, y, x, y - size * 3));
				field.g.setColor(bud);
				for (int k = 0; k < 3; k++)
				{
					field.g.fill(new Ellipse2D.Double(x - size * 0.45, y - size * (3 + k * 0.9), size * 0.9, size * 0.8));
				}
			}
		}
		field.over(g);
		SceneSky.veil(g, w, h, 90, 25);
		final Layer poppies = new Layer(w, h);
		for (int i = 0; i < 9; i++)
		{
			final double x = u(random, w * 0.4, w - 8);
			final double y = u(random, horizon + 18, h - 4);
			final double s = u(random, 2, 3.4);
			poppies.g.setColor(rgb(230, 40, 40, 255));
			poppies.g.fill(new Ellipse2D.Double(x - s, y - s, 2 * s, 2 * s));
			poppies.g.setColor(rgb(30, 10, 10, 255));
			poppies.g.fill(new Ellipse2D.Double(x - s * 0.3, y - s * 0.3, s * 0.6, s * 0.6));
		}
		poppies.over(g);
		final Random flies = new Random(54);
		for (int i = 0; i < 10; i++)
		{
			final double x = u(flies, w * 0.45, w);
			final double y = u(flies, h * 0.35, h * 0.9);
			final double s = u(flies, 0.8, 1.3);
			glow(g, x, y, 4, 4, rgb(255, 240, 170, 130), 2.2);
			g.setColor(rgb(255, 240, 170));
			g.fill(new Ellipse2D.Double(x - s / 2, y - s / 2, s, s));
		}
	}

	// ----------------------------------------------------------------------------------------------- the six borders

	/** Forest's frame: a dark wooden band with a light line and a darker one in it, and a leaf in each corner. */
	private static void forestBorder(Graphics2D g, int w, int h)
	{
		ring(g, rgb(60, 40, 22), w, h, 1, 3, 5);
		ring(g, rgb(150, 110, 60), w, h, 2, 1, 5);
		ring(g, rgb(40, 26, 14), w, h, 3, 1, 5);
		final double[][] corners = {{8, 8, -0.6}, {w - 8, 8, 0.6 + Math.PI}, {8, h - 8, 0.6}, {w - 8, h - 8, -0.6 + Math.PI}};
		for (final double[] corner : corners)
		{
			leaf(g, corner[0], corner[1], 6, corner[2], rgb(110, 180, 70));
		}
	}

	/** Ocean's frame: a pale blue line in a glow of the same blue, and a pearl in each corner. */
	private static void oceanBorder(Graphics2D g, int w, int h)
	{
		glowRing(g, rgb(120, 220, 255, 170), w, h, 2, 3, 5, 2.5);
		ring(g, rgb(150, 230, 255), w, h, 2, 1, 5);
		final double[][] corners = {{5, 5}, {w - 5, 5}, {5, h - 5}, {w - 5, h - 5}};
		for (final double[] corner : corners)
		{
			g.setColor(rgb(240, 250, 255));
			g.fill(new Ellipse2D.Double(corner[0] - 2.5, corner[1] - 2.5, 5, 5));
			g.setColor(rgb(255, 255, 255));
			g.fill(new Rectangle2D.Double(corner[0] - 1, corner[1] - 2, 1, 1));
		}
	}

	/** Autumn's frame: a dark band with an orange line in it, and a maple leaf in each corner. */
	private static void autumnBorder(Graphics2D g, int w, int h)
	{
		ring(g, rgb(60, 26, 10), w, h, 1, 3, 5);
		ring(g, rgb(230, 140, 50), w, h, 2, 1, 5);
		final double[][] corners = {{8, 8, 0.4}, {w - 8, 8, -0.4}, {8, h - 8, 2.6}, {w - 8, h - 8, -2.6}};
		for (final double[] corner : corners)
		{
			maple(g, corner[0], corner[1], 5, corner[2], rgb(240, 120, 40));
		}
	}

	/** Sakura's frame: a pink line in a pink glow. */
	private static void sakuraBorder(Graphics2D g, int w, int h)
	{
		glowRing(g, rgb(255, 170, 210, 150), w, h, 2, 3, 6, 2.5);
		ring(g, rgb(255, 196, 224), w, h, 2, 1, 6);
	}

	/** Garden's frame: a green band with a white line in it, and a rose in each corner. */
	private static void gardenBorder(Graphics2D g, int w, int h)
	{
		ring(g, rgb(60, 110, 50), w, h, 1, 3, 6);
		ring(g, rgb(240, 236, 220), w, h, 2, 1, 6);
		final double[][] corners = {{7, 7}, {w - 7, 7}, {7, h - 7}, {w - 7, h - 7}};
		for (final double[] corner : corners)
		{
			rose(g, corner[0], corner[1], 3.6, rgb(220, 40, 70), rgb(120, 10, 30));
		}
	}

	/** Lavender's frame: a lilac line in a lilac glow. */
	private static void lavenderBorder(Graphics2D g, int w, int h)
	{
		glowRing(g, rgb(200, 160, 255, 150), w, h, 2, 3, 6, 2.5);
		ring(g, rgb(220, 190, 255), w, h, 2, 1, 6);
	}

	/** The outermost ring of the card, 1 px in the theme's edge colour, over everything else. */
	private static void edge(Graphics2D g, CardTheme theme, int width, int height)
	{
		g.setColor(SceneSky.edge(theme));
		g.fillRect(0, 0, width, 1);
		g.fillRect(0, height - 1, width, 1);
		g.fillRect(0, 1, 1, Math.max(0, height - 2));
		g.fillRect(width - 1, 1, 1, Math.max(0, height - 2));
	}

	// ------------------------------------------------------------------------------------------------------- the ground

	/**
	 * Fills the whole card (this graphics' origin is 3 px in from its corner) with the vertical gradient through
	 * {@code colours} at {@code at}, over the picture's {@code w} by {@code h}, then limits the graphics to the picture's
	 * rounded rectangle, its corners {@code radius} px.
	 */
	private static void ground(Graphics2D g, int w, int h, double radius, float[] at, Color... colours)
	{
		g.setPaint(new LinearGradientPaint(0f, 0f, 0f, Math.max(1, h - 1), at, colours));
		g.fillRect(-INSET, -INSET, w + 2 * INSET, h + 2 * INSET);
		g.clip(new RoundRectangle2D.Double(0, 0, w, h, 2 * radius, 2 * radius));
	}

	/**
	 * A layer the size of the picture that its shapes are drawn into with {@link AlphaComposite#Src}, so that shapes
	 * overlapping each other replace the pixels and do not add their alpha; {@link #over} then lays it on the picture.
	 */
	private static final class Layer
	{
		final BufferedImage image;
		final Graphics2D g;

		Layer(int w, int h)
		{
			image = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
			g = image.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setComposite(AlphaComposite.Src);
		}

		/** Lays the layer on {@code target} at its origin and lets go of the layer's graphics. */
		void over(Graphics2D target)
		{
			g.dispose();
			target.drawImage(image, 0, 0, null);
		}
	}

	// -------------------------------------------------------------------------------------------- soft edges and glows

	/**
	 * A soft round glow: {@code colour} at its centre ({@code cx}, {@code cy}) and as far out as {@code rx} and
	 * {@code ry} nearly, fading to nothing about three {@code sigma} further, as a blur of that ellipse would fade it
	 * (a small one is dimmer at its middle, as a blur would leave it), drawn through a scaled radial gradient.
	 */
	private static void glow(Graphics2D g, double cx, double cy, double rx, double ry, Color colour, double sigma)
	{
		final double ex = rx + 3 * sigma;
		final double ey = ry + 3 * sigma;
		final double inner = (rx / ex + ry / ey) / 2;
		final double spread = (sigma / ex + sigma / ey) / 2;
		final double reach = (rx + ry) / 2;
		final double peak = 1 - Math.exp(-reach * reach / (2 * sigma * sigma));
		final int steps = 12;
		final float[] at = new float[steps + 1];
		final Color[] colours = new Color[steps + 1];
		for (int i = 0; i <= steps; i++)
		{
			final double t = i / (double) steps;
			at[i] = (float) t;
			final double share = i == steps ? 0 : phi((inner - t) / spread);
			colours[i] = rgb(colour.getRed(), colour.getGreen(), colour.getBlue(),
				(int) Math.round(colour.getAlpha() * peak * share));
		}
		final AffineTransform before = g.getTransform();
		final Paint paint = g.getPaint();
		g.translate(cx, cy);
		g.scale(ex, ey);
		g.setPaint(new RadialGradientPaint(0f, 0f, 1f, at, colours));
		g.fill(new Rectangle2D.Double(-1, -1, 2, 2));
		g.setTransform(before);
		g.setPaint(paint);
	}

	/**
	 * A shape with a soft edge: {@code shape} filled in {@code colour}, and outside it three strokes of its outline, each
	 * wider and fainter, so that the colour falls away over about {@code sigma} px as a blur of the shape would let it.
	 */
	private static void soft(Graphics2D g, Shape shape, Color colour, double sigma)
	{
		g.setColor(colour);
		g.fill(shape);
		final Shape saved = g.getClip();
		final Area outside = new Area(new Rectangle2D.Double(-100, -100, 4000, 4000));
		outside.subtract(new Area(shape));
		g.clip(outside);
		final double[] widths = {3 * sigma, 2 * sigma, sigma};
		final double[] shares = {0.12, 0.15, 0.2};
		for (int i = 0; i < widths.length; i++)
		{
			g.setColor(rgb(colour.getRed(), colour.getGreen(), colour.getBlue(),
				(int) Math.round(colour.getAlpha() * shares[i])));
			g.setStroke(new BasicStroke((float) widths[i]));
			g.draw(shape);
		}
		g.setStroke(new BasicStroke(1f));
		g.setClip(saved);
	}

	/**
	 * A rounded outline {@code thickness} px thick, {@code in} px inside the card's edge, the corners {@code radius} px
	 * on its outer side, in {@code colour}.
	 */
	private static void ring(Graphics2D g, Color colour, int width, int height, int in, int thickness, double radius)
	{
		outline(g, colour, width, height, in + thickness / 2.0, thickness, radius - thickness / 2.0);
	}

	/**
	 * A rounded outline as {@link #ring} draws it, blurred: five strokes of it, each wider and fainter, so that the
	 * colour (whose alpha is the line's) spreads about {@code sigma} px each way and is under half the line's alpha at
	 * its middle.
	 */
	private static void glowRing(Graphics2D g, Color colour, int width, int height, int in, int thickness, double radius,
		double sigma)
	{
		final double[] reach = {2.4, 1.8, 1.2, 0.6, 0};
		final double[] shares = {0.02, 0.05, 0.10, 0.14, 0.11};
		for (int i = 0; i < reach.length; i++)
		{
			outline(g, rgb(colour.getRed(), colour.getGreen(), colour.getBlue(),
				(int) Math.round(colour.getAlpha() * shares[i])), width, height, in + thickness / 2.0,
				thickness + 2 * reach[i] * sigma, radius - thickness / 2.0);
		}
	}

	/** A rounded rectangle's outline, drawn with a stroke {@code across} px wide along a line {@code middle} px inside the edge. */
	private static void outline(Graphics2D g, Color colour, int width, int height, double middle, double across,
		double radius)
	{
		final double corner = 2 * Math.max(0, radius);
		g.setColor(colour);
		g.setStroke(new BasicStroke((float) across));
		g.draw(new RoundRectangle2D.Double(middle, middle, width - 2 * middle, height - 2 * middle, corner, corner));
		g.setStroke(new BasicStroke(1f));
	}

	// ------------------------------------------------------------------------------------------------- the small shapes

	/** A pine: four tiers of triangle, wider toward the bottom, on a thin trunk, standing on {@code base}. */
	private static void pine(Graphics2D d, double x, double base, double height, double width, Color colour)
	{
		final int tiers = 4;
		d.setColor(colour);
		for (int i = 0; i < tiers; i++)
		{
			final double t = i / (double) tiers;
			final double top = base - height + height * t * 0.75;
			final double bottom = base - height * (1 - t) * 0.25 - height * 0.15 * (tiers - i) / tiers + height * 0.22;
			final double half = width * (0.35 + 0.65 * (i + 1) / tiers);
			d.fill(poly(x, top, x + half, bottom, x - half, bottom));
		}
		d.fill(new Rectangle2D.Double(x - 1, base - height * 0.1, 2, height * 0.1));
	}

	/** A leaf: a pointed oval {@code s} px from its middle to its tip, turned by {@code angle}. */
	private static void leaf(Graphics2D d, double x, double y, double s, double angle, Color colour)
	{
		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final Path2D.Double path = new Path2D.Double();
		for (int i = 0; i < 25; i++)
		{
			final double t = i / 24.0 * TAU;
			final double px = Math.cos(t) * s;
			final double py = Math.sin(t) * s * 0.4 * (1 - 0.3 * Math.cos(t));
			final double qx = x + px * cos - py * sin;
			final double qy = y + px * sin + py * cos;
			if (i == 0)
			{
				path.moveTo(qx, qy);
			}
			else
			{
				path.lineTo(qx, qy);
			}
		}
		path.closePath();
		d.setColor(colour);
		d.fill(path);
	}

	/** A maple leaf of five lobes about {@code s} px from the middle to a tip, and its stalk, turned by {@code angle}. */
	private static void maple(Graphics2D d, double x, double y, double s, double angle, Color colour)
	{
		final double cos = Math.cos(angle);
		final double sin = Math.sin(angle);
		final Path2D.Double path = new Path2D.Double();
		final int n = 60;
		for (int i = 0; i < n; i++)
		{
			final double t = i / (double) n * TAU;
			final double lobe = Math.abs(Math.cos(2.5 * (t - Math.PI / 2)));
			final double spike = 0.12 * Math.abs(Math.sin(7.5 * (t - Math.PI / 2)));
			double r = s * (0.45 + 0.5 * lobe * lobe + spike);
			if (Math.sin(t - Math.PI / 2) > 0.85)
			{
				r *= 0.55;
			}
			final double px = Math.cos(t) * r;
			final double py = Math.sin(t) * r;
			final double qx = x + px * cos - py * sin;
			final double qy = y + px * sin + py * cos;
			if (i == 0)
			{
				path.moveTo(qx, qy);
			}
			else
			{
				path.lineTo(qx, qy);
			}
		}
		path.closePath();
		d.setColor(colour);
		d.fill(path);
		d.setStroke(new BasicStroke(1f));
		d.draw(new Line2D.Double(x, y, x + s * 0.9 * Math.cos(angle + Math.PI / 2), y + s * 0.9 * Math.sin(angle + Math.PI / 2)));
	}

	/** A blossom: five round petals round a centre, {@code s} px for a petal's size, turned by {@code rot}. */
	private static void blossom(Graphics2D d, double x, double y, double s, Color petal, Color centre, double rot)
	{
		d.setColor(petal);
		for (int i = 0; i < 5; i++)
		{
			final double a = rot + i / 5.0 * TAU;
			final double px = x + Math.cos(a) * s * 0.6;
			final double py = y + Math.sin(a) * s * 0.6;
			d.fill(new Ellipse2D.Double(px - s * 0.5, py - s * 0.5, s, s));
		}
		d.setColor(centre);
		d.fill(new Ellipse2D.Double(x - s * 0.28, y - s * 0.28, s * 0.56, s * 0.56));
	}

	/**
	 * A branch through {@code points}, {@code w0} px thick at the first and {@code w1} at the last, a round end on each
	 * joint.
	 */
	private static void branch(Graphics2D d, double[][] points, double w0, double w1, Color colour)
	{
		final int n = points.length - 1;
		d.setColor(colour);
		for (int i = 0; i < n; i++)
		{
			final double wd = w0 + (w1 - w0) * i / Math.max(1, n - 1);
			d.setStroke(new BasicStroke((float) Math.max(0.5, wd)));
			d.draw(new Line2D.Double(points[i][0], points[i][1], points[i + 1][0], points[i + 1][1]));
			final double r = wd / 2;
			d.fill(new Ellipse2D.Double(points[i + 1][0] - r, points[i + 1][1] - r, 2 * r, 2 * r));
		}
		d.setStroke(new BasicStroke(1f));
	}

	/** A tulip: a stalk with a leaf and a cup of three points, {@code hgt} px tall from {@code base}. */
	private static void tulip(Graphics2D d, double x, double base, double hgt, Color colour, Color stalk)
	{
		d.setColor(stalk);
		d.setStroke(new BasicStroke(1f));
		d.draw(new Line2D.Double(x, base, x, base - hgt));
		leaf(d, x - 2.5, base - hgt * 0.35, 4, -1.1, stalk);
		final double top = base - hgt;
		d.setColor(colour);
		d.fill(poly(x - 3.2, top, x - 2, top - 5, x - 0.8, top - 2.5, x, top - 5.5, x + 0.8, top - 2.5, x + 2, top - 5,
			x + 3.2, top, x + 2.4, top + 2.6, x - 2.4, top + 2.6));
	}

	/** A daisy: ten white petals round a yellow middle, {@code s} px across. */
	private static void daisy(Graphics2D d, double x, double y, double s)
	{
		for (int i = 0; i < 10; i++)
		{
			final double a = i / 10.0 * TAU;
			leaf(d, x + Math.cos(a) * s * 0.55, y + Math.sin(a) * s * 0.55, s * 0.5, a, rgb(250, 250, 240, 255));
		}
		d.setColor(rgb(250, 200, 40, 255));
		d.fill(new Ellipse2D.Double(x - s * 0.3, y - s * 0.3, s * 0.6, s * 0.6));
	}

	/** A rose: a disc of {@code colour}, {@code s} px from the middle, with three curls of {@code dark} on it. */
	private static void rose(Graphics2D d, double x, double y, double s, Color colour, Color dark)
	{
		d.setColor(colour);
		d.fill(new Ellipse2D.Double(x - s, y - s, 2 * s, 2 * s));
		d.setColor(dark);
		d.setStroke(new BasicStroke(1f));
		for (int i = 0; i < 3; i++)
		{
			final double r = s * (0.75 - i * 0.22);
			d.draw(new Arc2D.Double(x - r, y - r, 2 * r, 2 * r, -(20 + i * 70), -270, Arc2D.OPEN));
		}
	}

	/** A butterfly seen from above, {@code s} px for a wing, its wings in {@code wing} and a spot on each in {@code spot}. */
	private static void butterfly(Graphics2D d, double x, double y, double s, Color wing, Color spot)
	{
		for (int sign = -1; sign <= 1; sign += 2)
		{
			d.setColor(wing);
			final double upperLeft = sign > 0 ? x + 0.5 * s : x - 0.5 * s - 0.9 * s;
			final double upperRight = sign > 0 ? x + 0.5 * s + 0.9 * s : x - 0.5 * s;
			d.fill(new Ellipse2D.Double(upperLeft, y - s, upperRight - upperLeft, 1.15 * s));
			final double lowerLeft = sign > 0 ? x + 0.4 * s : x - 0.4 * s - 0.65 * s;
			final double lowerRight = sign > 0 ? x + 0.4 * s + 0.65 * s : x - 0.4 * s;
			d.fill(new Ellipse2D.Double(lowerLeft, y - 0.05 * s, lowerRight - lowerLeft, 0.8 * s));
			d.setColor(spot);
			d.fill(new Ellipse2D.Double(x + sign * s * 0.75 - s * 0.18, y - s * 0.55, s * 0.36, s * 0.35));
		}
		d.setColor(rgb(30, 20, 20, 255));
		d.setStroke(new BasicStroke(1f));
		d.draw(new Line2D.Double(x, y - s * 0.7, x, y + s * 0.6));
	}

	// ----------------------------------------------------------------------------------------------------------- tools

	/** A closed polygon through the points {@code x0, y0, x1, y1, ...}. */
	private static Path2D.Double poly(double... xy)
	{
		final Path2D.Double path = new Path2D.Double();
		path.moveTo(xy[0], xy[1]);
		for (int i = 2; i + 1 < xy.length; i += 2)
		{
			path.lineTo(xy[i], xy[i + 1]);
		}
		path.closePath();
		return path;
	}

	/** A number from {@code low} up to {@code high} from {@code random}. */
	private static double u(Random random, double low, double high)
	{
		return low + (high - low) * random.nextDouble();
	}

	/** The normal curve's running total at {@code z} standard deviations, near enough for a soft edge. */
	private static double phi(double z)
	{
		return 1 / (1 + Math.exp(-1.702 * z));
	}

	private static Color rgb(int red, int green, int blue)
	{
		return new Color(red, green, blue);
	}

	private static Color rgb(int red, int green, int blue, int alpha)
	{
		return new Color(red, green, blue, alpha);
	}
}
