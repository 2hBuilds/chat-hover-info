package com.clanhub;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Where a card goes when the game is stretched or scaled: not into the game's own picture, which the stretch smooths, but
 * into a small window over the game, drawn at screen size. The overlay calls it on the client thread; the plugin backs it
 * with {@link SharpCardWindow}, a test with a recorder.
 */
interface SharpCards
{
	/**
	 * Shows {@code card}, each of its pixels drawn {@code scale} device pixels wide and tall, in the window at
	 * {@code onScreen}. Called only when the card or the rectangle changed since the last call.
	 *
	 * @param card the card as painted at 1x
	 * @param scale device pixels per card pixel, a whole number of 1 or more
	 * @param onScreen the window's bounds in AWT screen coordinates
	 */
	void show(BufferedImage card, int scale, Rectangle onScreen);

	/** Takes the window down. */
	void hide();
}
