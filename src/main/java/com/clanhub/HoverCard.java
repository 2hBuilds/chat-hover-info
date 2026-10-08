package com.clanhub;

import java.util.List;

/**
 * What the overlay, the painter and the menu need of any card: the words it shows, the line it came from with the
 * place of its words in that line, and its picture. The item card is one; the cards of later lots (quests, kinds of
 * broadcast, drop rates) are others. Pure model: it holds words and numbers and asks the client nothing.
 */
interface HoverCard
{
	/** One label with its value, and a note that follows the value in the label's grey. */
	final class Pair
	{
		/** The label, such as "Price". */
		final String label;
		/** The value, such as "121M". */
		final String value;
		/** What follows the value in the label's grey, such as " (460 each)"; empty when there is none. */
		final String note;

		Pair(String label, String value, String note)
		{
			this.label = label;
			this.value = value;
			this.note = note;
		}
	}

	/** The card's title, in bold at the top. */
	String title();

	/** The card's rows of label and value pairs, under the title; may be empty. */
	List<List<Pair>> rows();

	/**
	 * The description lines a card that is not an item's shows under its rows, already wrapped, one line each; empty for
	 * an item.
	 */
	List<String> notes();

	/** The card's last line: what a right-click does. */
	String hint();

	/** The wiki address "Open wiki" opens. */
	String wikiUrl();

	/**
	 * The title of the wiki page whose first paragraph the card shows under its rows: the name an item is called by, or
	 * a page's name with its underscores read as spaces; what is asked of the wiki, and the only thing that is.
	 */
	String wikiTitle();

	/** The chat line as plain text, what "Copy" puts on the clipboard. */
	String plainLine();

	/** Where the card's words start in {@link #plainLine()}. */
	int start();

	/** Just past the card's last word in {@link #plainLine()}. */
	int end();

	/** The id of the item whose sprite is the card's picture, or -1 when the picture is not an item's. */
	int itemId();

	/** How many of the item the picture shows; 1 when the card has no item. */
	int quantity();

	/** The game's own sprite id for the picture when it is not an item's, or -1. */
	int spriteId();

	/**
	 * Whether the card's words are coloured in the chat at rest. A card that is not marked is quiet: the line keeps the
	 * game's own colour, and the words show themselves only when the mouse is on the line, or under the "Always underline
	 * level-ups" setting.
	 */
	boolean marked();
}
