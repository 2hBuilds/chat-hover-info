package com.clanhub;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * The sidebar button's icon, 16 x 16 px: the 2hBuilds coin top-left ({@link CoinGlyph}) and the plugin's own
 * picture clear of the coin. The picture is a strip of the chatbox across the bottom, its clan chat line with one
 * word of it lit in the hover blue and the arrow cursor's tip on that word, and the card standing above it in the
 * right-hand column with a row of air between. It is sheet 14's number 19 of {@code docs/pictures}, picked
 * 2026-10-08. Drawn in code, so it needs no image file.
 */
public final class NavIcon
{
	/** The sidebar icon size. */
	public static final int SIZE = 16;

	/** The picture's colours: the chatbox's parchment, the clan's dark blue, the hover blue, white and the ink. */
	private static final int PARCHMENT = new Color(205, 190, 158).getRGB();
	private static final int CLAN = new Color(0, 0, 176).getRGB();
	private static final int HOVER = new Color(59, 130, 246).getRGB();
	private static final int WHITE = new Color(240, 240, 240).getRGB();
	private static final int INK = new Color(22, 22, 26).getRGB();

	/**
	 * The picture, one character a pixel, sixteen rows of sixteen: P parchment, C the clan's blue, B the hover blue,
	 * W white, I ink, '.' nothing (the sidebar's own grey shows).
	 */
	private static final String[] PICTURE = {
		"................",
		"...........BBBB.",
		"..........BBBBBB",
		"..........WWWWWW",
		"..........WIIIIW",
		"..........WWWWWW",
		"..........WIIIWW",
		"..........WWWWWW",
		"...........WWWW.",
		"................",
		"PPPPPPPPPPPIIIPP",
		"PCCCCCCCCBBIWIIP",
		"PPPPPPPPPPPIWWII",
		"...........IWWWI",
		"...........IWWWW",
		"...........IWIWI",
	};

	private NavIcon()
	{
	}

	/** A new 16 x 16 ARGB image of the icon. */
	public static BufferedImage create()
	{
		final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			CoinGlyph.paint(g);
		}
		finally
		{
			g.dispose();
		}
		for (int y = 0; y < SIZE; y++)
		{
			for (int x = 0; x < SIZE; x++)
			{
				final int argb = colour(PICTURE[y].charAt(x));
				if (argb != 0)
				{
					img.setRGB(x, y, argb);
				}
			}
		}
		return img;
	}

	/** The opaque ARGB value a picture character stands for, or 0 for nothing. */
	private static int colour(char c)
	{
		switch (c)
		{
			case 'P':
				return PARCHMENT;
			case 'C':
				return CLAN;
			case 'B':
				return HOVER;
			case 'W':
				return WHITE;
			case 'I':
				return INK;
			default:
				return 0;
		}
	}
}
