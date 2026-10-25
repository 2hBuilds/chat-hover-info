package com.clanhub;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;

/**
 * The how-to's "Examine" icon, drawn in code beside {@link GearGlyph}: a magnifying glass, a ring for the lens and a short
 * round-capped handle going down and to the right. The game has no sprite for it, and the picture is the same shape at any
 * size, every length being a fraction of the size.
 */
final class ExamineGlyph
{
	/** The smallest side drawn: below it the lens closes. */
	static final int MIN_SIZE = 7;

	private ExamineGlyph()
	{
	}

	/**
	 * A new square icon, transparent except for the lens and its handle.
	 *
	 * @param size   the icon's side in px; anything under {@value #MIN_SIZE} is raised to it
	 * @param colour the ink; null falls back to the sidebar's quiet grey, {@link ColorScheme#LIGHT_GRAY_COLOR}
	 */
	static ImageIcon icon(int size, Color colour)
	{
		final int s = Math.max(MIN_SIZE, size);
		final BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setColor(colour == null ? ColorScheme.LIGHT_GRAY_COLOR : colour);
			// The lens: a ring whose centre line has this radius, in the upper left; the handle starts on the ring's
			// edge, on the diagonal, and ends short of the corner so that its round cap lands inside the icon.
			final double lensCentre = s * 0.40;
			final double lens = s * 0.27;
			final float lensWidth = (float) Math.max(1.5, s * 0.11);
			final float handleWidth = (float) Math.max(1.8, s * 0.15);
			final double edge = lens + lensWidth / 2.0;
			final double start = lensCentre + edge * Math.cos(Math.PI / 4.0);
			final double end = s - handleWidth / 2.0 - 1.0;
			g.setStroke(new BasicStroke(lensWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Ellipse2D.Double(lensCentre - lens, lensCentre - lens, lens * 2.0, lens * 2.0));
			g.setStroke(new BasicStroke(handleWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Line2D.Double(start, start, end, end));
		}
		finally
		{
			g.dispose();
		}
		return new ImageIcon(img);
	}
}
