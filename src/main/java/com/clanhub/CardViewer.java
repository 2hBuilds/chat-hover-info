package com.clanhub;

import java.awt.Rectangle;

/**
 * What a hovered row of the sidebar's recent cards asks for: its card shown beside the sidebar, as in game, and taken
 * down again when the mouse leaves the row. The plugin backs it with a small window beside the sidebar; a test backs it
 * with a recorder. Both are called on the event thread.
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

	/** Takes the card down. */
	void hide();
}
