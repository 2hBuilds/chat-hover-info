package com.clanhub;

import java.util.function.Consumer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;

/**
 * The two entries a right-click on a chat line that holds a card adds to the game's menu: "Open wiki" and "Copy".
 * Both are the client's own kind of entry ({@code MenuAction.RUNELITE}): nothing is sent to the game. What they do is
 * handed in, so the entries can be checked without a browser or a clipboard.
 */
final class CardMenu
{
	private static final String OPEN_WIKI = "Open wiki";
	private static final String COPY = "Copy";
	/** The card's title in the menu's orange, the way the game shows a target. */
	private static final String TARGET_OPEN = "<col=ff9040>";
	private static final String TARGET_CLOSE = "</col>";

	private CardMenu()
	{
	}

	/**
	 * Adds "Open wiki" and "Copy" to the menu for {@code card}, or nothing when there is no card. The target of both is
	 * the card's title (an item's name, with its "x10" for a stack). "Open wiki" hands the card's wiki address to
	 * {@code browse} and "Copy" hands the line's plain text to {@code copy}. "Copy" is made first and "Open wiki" last,
	 * each with {@code createMenuEntry(-1)}: the client adds an entry at the end of its list and draws the end at the
	 * top, so the one made last is the one on top.
	 */
	static void addEntries(Menu menu, HoverCard card, Consumer<String> browse, Consumer<String> copy)
	{
		if (card == null)
		{
			return;
		}
		final String target = TARGET_OPEN + card.title() + TARGET_CLOSE;
		menu.createMenuEntry(-1)
			.setOption(COPY)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> copy.accept(card.plainLine()));
		menu.createMenuEntry(-1)
			.setOption(OPEN_WIKI)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> browse.accept(card.wikiUrl()));
	}
}
