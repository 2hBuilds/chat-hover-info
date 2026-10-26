package com.clanhub;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * What a hovered row of the sidebar's recent cards asks for: its card shown beside the sidebar, as in game, and taken
 * down again when the mouse leaves the row; and what an armed theme in the settings menu asks for: a picture of that
 * theme shown beside the menu's list, the same window. The plugin backs it with a small window beside the sidebar; a
 * test backs it with a recorder. Both are called on the event thread.
 */
interface CardViewer
{
	/**
	 * Shows {@code card} beside the sidebar, level with the row that asked.
	 *
	 * @param card the card of the hovered row
	 * @param rowOnScreen the row's bounds in screen coordinates
	 */
	void show(HoverCard card, Rectangle rowOnScreen);

	/**
	 * Shows {@code picture}, as it is, beside the rectangle, level with it, on whichever side has room.
	 *
	 * @param picture the picture to show: a sample card or a small sidebar in a theme
	 * @param besideOnScreen the bounds, in screen coordinates, of the menu item that asked
	 */
	void showPicture(BufferedImage picture, Rectangle besideOnScreen);

	/** Takes the card, or the picture, down. */
	void hide();
}
