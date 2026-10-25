package com.clanhub;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Where a row of the sidebar's recent cards gets its picture: the card's own, which the game's sprite and item managers
 * give and the wiki's pictures sometimes replace. The picture may come at once, later, or not at all, and a picture that
 * loads gives itself twice (blank, then drawn), so the row draws what it is given each time. The plugin backs it with the
 * managers; a test backs it with a source that gives nothing.
 */
interface IconSource
{
	/**
	 * Gives {@code onIcon} the picture of {@code card}, now or later, on any thread. A card with no picture gives
	 * nothing.
	 *
	 * @param card the card the row shows
	 * @param onIcon what takes the picture; it may be called more than once
	 */
	void iconFor(HoverCard card, Consumer<BufferedImage> onIcon);

	/**
	 * Gives {@code onIcon} the game's sprite {@code spriteId}, now or later, on any thread. The how-to's rows ask for the
	 * pictures of the kinds of card (the quest, total level and combat icons) this way. Gives nothing by default.
	 *
	 * @param spriteId the sprite's id
	 * @param onIcon   what takes the picture; it may be called more than once
	 */
	default void sprite(int spriteId, Consumer<BufferedImage> onIcon)
	{
	}

	/**
	 * Gives {@code onIcon} the picture of the item {@code itemId} at a stack of {@code quantity}, now or later, on any
	 * thread; an item's picture is handed out before it has loaded and again when it has. Gives nothing by default.
	 *
	 * @param itemId   the item's id
	 * @param quantity the stack the picture shows
	 * @param onIcon   what takes the picture; it may be called more than once
	 */
	default void item(int itemId, int quantity, Consumer<BufferedImage> onIcon)
	{
	}
}
