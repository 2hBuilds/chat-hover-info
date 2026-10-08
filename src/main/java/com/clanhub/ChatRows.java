package com.clanhub;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The rows of one chat line widget, the game's word wrap done again. The game does not split a message that is wider
 * than the chatbox into widgets: it stays ONE widget whose text has no break in it and whose height is the rows' (28 px
 * for two, 14 for each), and the game draws the words row by row, breaking at the spaces. Pure: the plain text, a width
 * function and the widget's bounds in, rows and places out; no RuneLite state. The wrap is greedy: a row takes words
 * while the width of the row's text so far, from its first character to the end of the word, is at most the bounds'
 * width; a word wider than the row stands alone on its own row, uncut; an empty text is one empty row. With the rows
 * known, the plugin can say which row a word is on and where on that row it stands, which is what the card's hit and the
 * underline need on a line that wrapped; a line of one row comes out as the plain, one-row reading of old.
 *
 * <p>Choice: the break space belongs to the row before, so an index in it is on the row it ends and the next row starts
 * at the next word's first character; the underline of words that run across a break then has a piece on each row and
 * none under the space itself.
 */
final class ChatRows
{
	/** The plain text the rows are made of. */
	private final String text;
	/** The width of a text in the chat font, in pixels. */
	private final ToIntFunction<String> width;
	/** The widget's bounds: the left edge, the top, the width the rows wrap to and the whole height. */
	private final Rectangle bounds;
	/** Where each row starts in the text. */
	private final int[] starts;
	/** The index after each row's last word, the break space left out. */
	private final int[] ends;
	/** The height of one row: the bounds' height over the rows, in whole pixels. */
	private final int rowHeight;

	private ChatRows(String text, ToIntFunction<String> width, Rectangle bounds, List<int[]> rows)
	{
		this.text = text;
		this.width = width;
		this.bounds = bounds;
		this.starts = new int[rows.size()];
		this.ends = new int[rows.size()];
		for (int row = 0; row < rows.size(); row++)
		{
			starts[row] = rows.get(row)[0];
			ends[row] = rows.get(row)[1];
		}
		this.rowHeight = Math.max(1, bounds.height / rows.size());
	}

	/**
	 * The rows the text breaks into inside {@code bounds}, {@code width} giving the width in pixels of a text in the
	 * chat font.
	 */
	static ChatRows of(String text, ToIntFunction<String> width, Rectangle bounds)
	{
		final List<int[]> rows = new ArrayList<>();
		final int length = text.length();
		int rowStart = 0;
		int rowEnd = 0;
		boolean any = false;
		int at = 0;
		while (at < length)
		{
			if (text.charAt(at) == ' ')
			{
				at++;
				continue;
			}
			final int wordStart = at;
			while (at < length && text.charAt(at) != ' ')
			{
				at++;
			}
			if (!any)
			{
				rowEnd = at;
				any = true;
			}
			else if (width.applyAsInt(text.substring(rowStart, at)) <= bounds.width)
			{
				rowEnd = at;
			}
			else
			{
				rows.add(new int[] {rowStart, rowEnd});
				rowStart = wordStart;
				rowEnd = at;
			}
		}
		rows.add(new int[] {rowStart, rowEnd});
		return new ChatRows(text, width, bounds, Collections.unmodifiableList(rows));
	}

	/** How many rows the text takes, at least one. */
	int rows()
	{
		return starts.length;
	}

	/** Where the row starts in the text: the first character of its first word. */
	int start(int row)
	{
		return starts[row];
	}

	/** The index after the row's last word; the break space that follows it is not in the row. */
	int end(int row)
	{
		return ends[row];
	}

	/**
	 * The row an index in the text is on: the row whose start to end holds it, a break space being on the row before it,
	 * and an index past the text on the last row.
	 */
	int rowOf(int index)
	{
		for (int row = rows() - 1; row > 0; row--)
		{
			if (index >= start(row))
			{
				return row;
			}
		}
		return 0;
	}

	/** The x on screen of the place before the character at {@code index}, on the row that index is on. */
	int x(int index)
	{
		final int row = rowOf(index);
		final int at = Math.max(start(row), Math.min(index, text.length()));
		return bounds.x + width.applyAsInt(text.substring(start(row), at));
	}

	/** The row the height {@code y} falls on, the first row for a y above the widget and the last for one below it. */
	int rowAt(int y)
	{
		return Math.max(0, Math.min(rows() - 1, (y - bounds.y) / rowHeight));
	}

	/** The y of the underline of a row: 1 px under the row's text box, which for a line of one row is its bottom. */
	int underlineY(int row)
	{
		return bounds.y + (row + 1) * rowHeight;
	}

	/**
	 * The pieces the words {@code [from, to)} take on the rows they touch, as {@code {left x, right x, underline y}}, the
	 * left end up to, not including, the right end, the way the underline fills them; a row where the words have nothing
	 * left out, so a break space alone has none.
	 */
	List<int[]> spans(int from, int to)
	{
		final List<int[]> pieces = new ArrayList<>();
		for (int row = 0; row < rows(); row++)
		{
			final int first = Math.max(from, start(row));
			final int last = Math.min(to, end(row));
			if (first < last)
			{
				pieces.add(new int[] {x(first), x(last), underlineY(row)});
			}
		}
		return pieces;
	}

	/**
	 * The card of the line whose words the mouse is on: for each card, its words {@code [offset + start(), offset +
	 * end())} are looked at on the row the mouse's y is on, and the first card with a part there that is not empty and
	 * whose x range holds {@code mouseX} is the one; null when the mouse is on none.
	 *
	 * <p>Choice: the card belongs to its words on their own row, so the mouse on the other row of a wrapped line, under
	 * the words or over them, is on no card.
	 */
	HoverCard pick(List<HoverCard> cards, int offset, int mouseX, int mouseY)
	{
		final int row = rowAt(mouseY);
		for (final HoverCard card : cards)
		{
			final int first = Math.max(offset + card.start(), start(row));
			final int last = Math.min(offset + card.end(), end(row));
			if (first < last && mouseX >= x(first) && mouseX < x(last))
			{
				return card;
			}
		}
		return null;
	}
}
