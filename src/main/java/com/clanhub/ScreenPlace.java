package com.clanhub;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Where a window goes on the screen, worked out apart from any window so that it can be tested: nothing here touches
 * the screen itself, the caller hands over the room it has.
 */
final class ScreenPlace
{
	/** The gap between a window's right edge and the point it opens beside. */
	static final int LEFT_GAP = 8;

	/** How far above the point a window's top edge sits, so that the point is near the window's title. */
	static final int RAISE = 20;

	private ScreenPlace()
	{
	}

	/**
	 * The top-left corner for a window of {@code size} that opens to the left of {@code at}: its right edge
	 * {@link #LEFT_GAP} px left of {@code at.x} and its top {@link #RAISE} px above {@code at.y}, held inside
	 * {@code screen} (at its left and top edges when the window is bigger than the room).
	 *
	 * @param at the point on the screen the window opens beside, the centre of a swatch just pressed
	 * @param size the window's size
	 * @param screen the usable area of the screen the point is on
	 */
	static Point leftOf(Point at, Dimension size, Rectangle screen)
	{
		final int x = at.x - LEFT_GAP - size.width;
		final int y = at.y - RAISE;
		return new Point(
			Math.max(screen.x, Math.min(x, screen.x + screen.width - size.width)),
			Math.max(screen.y, Math.min(y, screen.y + screen.height - size.height)));
	}
}
