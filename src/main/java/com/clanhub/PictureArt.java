package com.clanhub;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * What the card does to a picture theme's picture, and to the words laid over it. Pure pixel work, in plain sRGB: a
 * picture stretched to the card, the crisp edge ring, the frosted panel behind each line of words, the dimming of a bright
 * thing by the title, and the outline of the words. A line's box is {@code {x, y, width, height}} in px of the card.
 *
 * <p>The treatment is the studio's (its {@code StudioText}) in cheaper terms: the panel's blur is three box passes, its
 * pull is a multiplication or a mix in sRGB, and the dimming is worked in Oklab lightness, where scaling the linear colour
 * by k scales the lightness by the cube root of k, so a pixel is brought to its ceiling with its colour kept. Every size
 * is given at the Medium card width (409 px) and scales with the card.
 */
final class PictureArt
{
	/** The width the sizes below are given at, in px. */
	static final double BASE_WIDTH = 409;
	/** The panel: air round a line, how far it fades out, its corner radius, its blur radius, a line gap that starts a new block, px. */
	private static final double PANEL_GROW = 1;
	private static final double PANEL_FEATHER = 5;
	private static final double PANEL_RADIUS = 3;
	private static final double PANEL_BLUR = 2.6;
	private static final double PANEL_SPLIT = 14;
	/** How far the full band pulls the lightness toward the ink's opposite, under the blur. */
	private static final double PANEL_PULL = 0.5;
	/** The title's lightness ceiling, its soft knee, and the distances it holds and eases back over, px. */
	private static final double DIM_CAP = 0.485;
	private static final double DIM_KNEE = 0.012;
	private static final double DIM_NEAR = 6;
	private static final double DIM_FAR = 24;
	/** The pale halo of dark words, as an RGB. */
	private static final int HALO = 0xF8F6F0;
	/** The offsets of the eight copies an outline is drawn at, px. */
	private static final int[][] AROUND = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
	/** The linear value of each sRGB byte. */
	private static final float[] LINEAR = new float[256];

	static
	{
		for (int i = 0; i < 256; i++)
		{
			final double s = i / 255.0;
			LINEAR[i] = (float) (s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4));
		}
	}

	private PictureArt()
	{
	}

	/** {@code art} scaled bilinearly to exactly {@code width} by {@code height} px, opaque. */
	static BufferedImage stretch(BufferedImage art, int width, int height)
	{
		final BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = out.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(art, 0, 0, width, height, null);
		}
		finally
		{
			g.dispose();
		}
		return out;
	}

	/** Draws the one pixel ring of {@code colour} round the edge of {@code image}, on the stretched picture, so it is crisp. */
	static void ring(BufferedImage image, Color colour)
	{
		final Graphics2D g = image.createGraphics();
		try
		{
			g.setComposite(AlphaComposite.Src);
			g.setColor(colour);
			g.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);
		}
		finally
		{
			g.dispose();
		}
	}

	/**
	 * Applies the theme's treatment to {@code image}: the title's light held down when the theme has a focal title, then the
	 * frosted band behind every line of words at the player's opacity. With neither, no pixel changes.
	 *
	 * @param lines the boxes of the strings drawn on the card
	 * @param title the title's box, or null
	 * @param opacity the player's Blur behind text setting, 0 to 1: how much of the full frosted band (the blur
	 *                and the pull together) is laid over the picture behind the words; 0 leaves the picture as it is
	 */
	static void treat(BufferedImage image, PictureTheme theme, List<int[]> lines, int[] title, double opacity)
	{
		final boolean dim = theme.focalTitle && title != null;
		final double panel = Math.max(0, Math.min(1, opacity));
		final boolean frost = panel > 0 && !lines.isEmpty();
		if (!dim && !frost)
		{
			return;
		}
		final int w = image.getWidth();
		final int h = image.getHeight();
		final int[] px = image.getRGB(0, 0, w, h, null, 0, w);
		final double scale = w / BASE_WIDTH;
		if (dim)
		{
			dim(px, w, h, title, scale);
		}
		if (frost)
		{
			frost(px, w, h, lines, theme.darkInk(), panel, scale);
		}
		image.setRGB(0, 0, w, h, px, 0, w);
	}

	/**
	 * Holds the picture's lightness under {@link #DIM_CAP} within 6 px of the title's box, the ceiling rising by a smoothstep
	 * to 1 at 24 px; only what is brighter is touched, pressed under the ceiling by a soft knee with its colour kept.
	 */
	private static void dim(int[] px, int w, int h, int[] title, double scale)
	{
		final double near = DIM_NEAR * scale;
		final double far = DIM_FAR * scale;
		final int reach = (int) Math.ceil(far);
		for (int y = Math.max(0, title[1] - reach); y < Math.min(h, title[1] + title[3] + reach); y++)
		{
			final double dy = Math.max(Math.max(title[1] - y, y - (title[1] + title[3] - 1)), 0);
			for (int x = Math.max(0, title[0] - reach); x < Math.min(w, title[0] + title[2] + reach); x++)
			{
				final double dx = Math.max(Math.max(title[0] - x, x - (title[0] + title[2] - 1)), 0);
				final double d = Math.sqrt(dx * dx + dy * dy);
				if (d >= far)
				{
					continue;
				}
				final int p = px[y * w + x];
				final float r = LINEAR[p >> 16 & 255];
				final float g = LINEAR[p >> 8 & 255];
				final float b = LINEAR[p & 255];
				final double lightness = lightness(r, g, b);
				final double ceiling = DIM_CAP + (1 - DIM_CAP) * smooth(near, far, d);
				if (lightness <= ceiling - 4 * DIM_KNEE)
				{
					continue;
				}
				final double pressed = -DIM_KNEE * Math.log(Math.exp(-lightness / DIM_KNEE) + Math.exp(-ceiling / DIM_KNEE));
				if (pressed >= lightness - 0.0005)
				{
					continue;
				}
				final double k = Math.pow(pressed / lightness, 3);
				px[y * w + x] = (p & 0xFF000000) | encode(r * k) << 16 | encode(g * k) << 8 | encode(b * k);
			}
		}
	}

	/** The Oklab lightness of a linear colour. */
	private static double lightness(double r, double g, double b)
	{
		final double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
		final double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
		final double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
		return 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
	}

	/** A linear value as an sRGB byte. */
	private static int encode(double linear)
	{
		final double v = linear <= 0.0031308 ? 12.92 * linear : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
		return (int) Math.max(0, Math.min(255, Math.round(v * 255)));
	}

	private static double smooth(double from, double to, double x)
	{
		final double t = Math.max(0, Math.min(1, (x - from) / (to - from)));
		return t * t * (3 - 2 * t);
	}

	/**
	 * The frosted panel: behind each block of words (the strings of one line, apart where a gap of 14 px starts a new one)
	 * the picture blurred and pulled toward the ink's opposite by {@code 0.4 x strength} (darker for light ink, lighter for
	 * dark ink), in a rounded block fading out over 5 px, so no pixel more than 6 px from a string's box is touched.
	 */
	private static void frost(int[] px, int w, int h, List<int[]> lines, boolean dark, double opacity, double scale)
	{
		final double grow = PANEL_GROW * scale;
		final double feather = PANEL_FEATHER * scale;
		final List<int[]> blocks = blocks(lines, PANEL_SPLIT * scale);
		int x0 = w;
		int y0 = h;
		int x1 = 0;
		int y1 = 0;
		final int reach = (int) Math.ceil(grow + feather) + 1;
		for (final int[] b : blocks)
		{
			x0 = Math.min(x0, b[0] - reach);
			y0 = Math.min(y0, b[1] - reach);
			x1 = Math.max(x1, b[2] + reach);
			y1 = Math.max(y1, b[3] + reach);
		}
		x0 = Math.max(0, x0);
		y0 = Math.max(0, y0);
		x1 = Math.min(w, x1);
		y1 = Math.min(h, y1);
		final int rw = x1 - x0;
		final int rh = y1 - y0;
		if (rw <= 0 || rh <= 0)
		{
			return;
		}
		final float[] cover = new float[rw * rh];
		for (final int[] b : blocks)
		{
			lay(cover, rw, rh, x0, y0, b, grow, feather, PANEL_RADIUS * scale);
		}
		final float[][] blur = new float[3][rw * rh];
		for (int y = 0; y < rh; y++)
		{
			for (int x = 0; x < rw; x++)
			{
				final int p = px[(y0 + y) * w + x0 + x];
				blur[0][y * rw + x] = p >> 16 & 255;
				blur[1][y * rw + x] = p >> 8 & 255;
				blur[2][y * rw + x] = p & 255;
			}
		}
		final int radius = Math.max(1, (int) Math.round(PANEL_BLUR * scale));
		for (final float[] channel : blur)
		{
			for (int pass = 0; pass < 3; pass++)
			{
				box(channel, rw, rh, radius, true);
				box(channel, rw, rh, radius, false);
			}
		}
		final double pull = PANEL_PULL;
		final double darker = Math.pow(1 - pull, 1.37);
		for (int y = 0; y < rh; y++)
		{
			for (int x = 0; x < rw; x++)
			{
				final double a = cover[y * rw + x] * opacity;
				if (a <= 0.0005)
				{
					continue;
				}
				final int at = (y0 + y) * w + x0 + x;
				final int p = px[at];
				int out = p & 0xFF000000;
				for (int c = 0; c < 3; c++)
				{
					final int shift = 16 - 8 * c;
					final double was = p >> shift & 255;
					final double soft = blur[c][y * rw + x];
					final double target = dark ? soft + (255 - soft) * pull : soft * darker;
					out |= (int) Math.max(0, Math.min(255, Math.round(was + (target - was) * a))) << shift;
				}
				px[at] = out;
			}
		}
	}

	/** The blocks of words: boxes on one line, no further than {@code split} px apart, joined into one {x0, y0, x1, y1}. */
	private static List<int[]> blocks(List<int[]> lines, double split)
	{
		final List<int[]> blocks = new ArrayList<>();
		for (final int[] box : lines)
		{
			int[] join = null;
			for (final int[] block : blocks)
			{
				final int overlap = Math.min(block[3], box[1] + box[3]) - Math.max(block[1], box[1]);
				final int gap = Math.max(block[0] - (box[0] + box[2]), box[0] - block[2]);
				if (overlap * 2 > box[3] && gap < split)
				{
					join = block;
					break;
				}
			}
			if (join == null)
			{
				blocks.add(new int[] {box[0], box[1], box[0] + box[2], box[1] + box[3]});
			}
			else
			{
				join[0] = Math.min(join[0], box[0]);
				join[1] = Math.min(join[1], box[1]);
				join[2] = Math.max(join[2], box[0] + box[2]);
				join[3] = Math.max(join[3], box[1] + box[3]);
			}
		}
		return blocks;
	}

	/** Lays one block's coverage, the largest where blocks meet: 1 inside its rounded rectangle, fading to 0 over {@code feather}. */
	private static void lay(float[] cover, int rw, int rh, int x0, int y0, int[] b, double grow, double feather, double round)
	{
		final double left = b[0] - grow;
		final double right = b[2] + grow;
		final double top = b[1] - grow;
		final double bottom = b[3] + grow;
		final double hx = 0.5 * (right - left);
		final double hy = 0.5 * (bottom - top);
		final double radius = Math.min(round, Math.min(hx, hy));
		for (int y = 0; y < rh; y++)
		{
			final double dy = Math.max(Math.abs(y0 + y + 0.5 - 0.5 * (top + bottom)) - (hy - radius), 0);
			for (int x = 0; x < rw; x++)
			{
				final double dx = Math.max(Math.abs(x0 + x + 0.5 - 0.5 * (left + right)) - (hx - radius), 0);
				final double d = Math.sqrt(dx * dx + dy * dy) - radius;
				if (d < feather)
				{
					final float a = d <= 0 ? 1f : (float) (1 - smooth(0, feather, d));
					cover[y * rw + x] = Math.max(cover[y * rw + x], a);
				}
			}
		}
	}

	/** One box-blur pass of {@code radius} along the rows or the columns of a {@code w} by {@code h} channel, edges held. */
	private static void box(float[] a, int w, int h, int radius, boolean rows)
	{
		final int lines = rows ? h : w;
		final int length = rows ? w : h;
		final int step = rows ? 1 : w;
		final float[] copy = new float[length];
		final float scale = 1f / (2 * radius + 1);
		for (int line = 0; line < lines; line++)
		{
			final int base = rows ? line * w : line;
			for (int i = 0; i < length; i++)
			{
				copy[i] = a[base + i * step];
			}
			float sum = 0;
			for (int i = -radius; i <= radius; i++)
			{
				sum += copy[Math.max(0, Math.min(length - 1, i))];
			}
			for (int i = 0; i < length; i++)
			{
				a[base + i * step] = sum * scale;
				sum += copy[Math.min(length - 1, i + radius + 1)] - copy[Math.max(0, i - radius)];
			}
		}
	}

	/**
	 * Draws the outline and the soft shadow behind the string at ({@code x}, {@code baseline}), before the string itself: black
	 * round light words, a pale halo round dark ones. Outline 0 draws nothing; 0.5 a 1 px outline at about half strength; 1 a
	 * full 1 px outline and a soft shadow 2 px down.
	 */
	static void edged(Graphics2D g, PictureTheme theme, String text, int x, int baseline)
	{
		if (theme.outline <= 0)
		{
			return;
		}
		final int rgb = theme.darkInk() ? HALO : 0;
		final double soft = Math.max(0, (theme.outline - 0.5) * 2);
		if (soft > 0)
		{
			g.setColor(new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (int) Math.round(90 * soft)));
			g.drawString(text, x, baseline + 2);
			g.drawString(text, x + 1, baseline + 2);
		}
		final double each = theme.outline >= 1 ? 1 : theme.outline * 0.6;
		g.setColor(new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (int) Math.round(255 * each)));
		for (final int[] offset : AROUND)
		{
			g.drawString(text, x + offset[0], baseline + offset[1]);
		}
	}
}
