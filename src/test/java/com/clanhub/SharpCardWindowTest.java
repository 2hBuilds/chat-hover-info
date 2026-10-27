package com.clanhub;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link SharpCardWindow}: when a card leaves the game's picture, and how many screen pixels each of its pixels takes. */
public class SharpCardWindowTest
{
	@Test
	public void theScaleIsAWholeNumberAtLeastOne()
	{
		assertEquals(1, SharpCardWindow.scaleFor(1, 1));
		assertEquals(1, SharpCardWindow.scaleFor(1.12, 1));
		assertEquals(1, SharpCardWindow.scaleFor(1.49, 1));
		assertEquals(2, SharpCardWindow.scaleFor(1.5, 1));
		assertEquals(2, SharpCardWindow.scaleFor(2, 1));
		assertEquals(1, SharpCardWindow.scaleFor(1, 1.25));
		assertEquals(2, SharpCardWindow.scaleFor(1.6, 1.25));
		assertEquals(1, SharpCardWindow.scaleFor(0.8, 1));

		assertFalse(SharpCardWindow.needsWindow(1, 1));
		assertFalse(SharpCardWindow.needsWindow(1.005, 1));
		assertTrue(SharpCardWindow.needsWindow(1.12, 1));
		assertTrue(SharpCardWindow.needsWindow(1, 1.25));
	}
}
