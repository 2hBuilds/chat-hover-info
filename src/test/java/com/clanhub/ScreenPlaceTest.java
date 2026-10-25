package com.clanhub;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** {@link ScreenPlace}: where a window opens beside a point, held on the screen. */
public class ScreenPlaceTest
{
	/** Lot 10M: the window's right edge is 8 px left of the point and its top 20 px above, kept inside the screen. */
	@Test
	public void leftOfKeepsTheWindowOnScreen()
	{
		final Rectangle screen = new Rectangle(0, 0, 1920, 1040);
		final Dimension size = new Dimension(300, 400);

		assertEquals("room all round: 8 px left, 20 px up", new Point(1192, 280),
			ScreenPlace.leftOf(new Point(1500, 300), size, screen));
		assertEquals("too near the left edge: held at the edge", new Point(0, 280),
			ScreenPlace.leftOf(new Point(100, 300), size, screen));
		assertEquals("too near the bottom: held above the bottom edge", new Point(1192, 640),
			ScreenPlace.leftOf(new Point(1500, 900), size, screen));
		assertEquals("too near the top: held at the top edge", new Point(1192, 0),
			ScreenPlace.leftOf(new Point(1500, 5), size, screen));
	}
}
