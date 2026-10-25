package com.clanhub;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link ExamineGlyph}, in memory: its size, its transparent corner and the colour of its ink. */
public class ExamineGlyphTest
{
	private static final Color ORANGE = new Color(220, 138, 0);

	/** How many pixels of {@code img} are fully opaque and exactly {@code colour}. */
	private static int inkOf(BufferedImage img, Color colour)
	{
		int count = 0;
		for (int y = 0; y < img.getHeight(); y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if (img.getRGB(x, y) == (0xFF000000 | colour.getRGB()))
				{
					count++;
				}
			}
		}
		return count;
	}

	/** Lot 10E: the glass is as big as asked, has some pixels of the colour, and its corner is transparent. */
	@Test
	public void theGlassIsDrawnInTheSizeAskedFor()
	{
		final ImageIcon icon = ExamineGlyph.icon(20, ORANGE);
		assertEquals(20, icon.getIconWidth());
		assertEquals(20, icon.getIconHeight());
		final BufferedImage img = (BufferedImage) icon.getImage();
		assertEquals(20, img.getWidth());
		assertEquals(20, img.getHeight());
		assertTrue("some pixels of the colour", inkOf(img, ORANGE) > 10);
		assertEquals("the top right corner is transparent", 0, img.getRGB(19, 0) >>> 24);
		assertEquals("so is the top left", 0, img.getRGB(0, 0) >>> 24);
		assertEquals("so is the bottom left", 0, img.getRGB(0, 19) >>> 24);
		assertEquals("so is the bottom right", 0, img.getRGB(19, 19) >>> 24);
	}

	@Test
	public void noColourMeansTheSidebarsGreyAndTheSizeHasAFloor()
	{
		final BufferedImage grey = (BufferedImage) ExamineGlyph.icon(20, null).getImage();
		assertTrue("the quiet grey", inkOf(grey, ColorScheme.LIGHT_GRAY_COLOR) > 10);
		assertEquals(ExamineGlyph.MIN_SIZE, ExamineGlyph.icon(2, ORANGE).getIconWidth());
	}
}
