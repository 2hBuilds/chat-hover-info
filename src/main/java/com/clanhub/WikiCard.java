package com.clanhub;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays a card out as the wiki's own infobox, the look of the Wiki theme. Pure drawing: a {@link HoverCard}, a picture,
 * a size, a width and the description text in, pixels and the card's size out. {@link CardPainter} hands a card to it
 * when the card's {@link CardStyle} is an infobox; the colours that the style names are the style's, the rest of the
 * infobox's (the bar, the cells, the link blue) are this class's.
 *
 * <p>From the top: the title in a bar across the card, centred, with the stack after it; the picture in a cell at the
 * left with the facts as a two-column table beside it, one row to a fact; for an item, the drops as a table of the
 * source (in link blue) and its rate; the description (a paragraph, or the numbered list) across the card; and the hint,
 * its "Open wiki" a link. It is exactly as wide as it is asked, whatever it holds. The type is Java's own sans serif,
 * the wiki's own look, not the game's. Every size is given at 16 px and grows and shrinks with the text size.
 */
final class WikiCard
{
	/** The title bar, and the header row of the drops. */
	private static final Color BAR = new Color(194, 172, 136);
	/** The stack after the title. */
	private static final Color STACK = new Color(90, 75, 55);
	/** The picture's cell, a fact's value cell and every other row of the drops. */
	private static final Color CELL = new Color(244, 238, 226);
	/** A fact's label cell. */
	private static final Color LABEL_CELL = new Color(214, 202, 178);
	/** The edge of a drop's row. */
	private static final Color DROP_EDGE = new Color(205, 192, 165);
	/** A source, and the hint's "Open wiki". */
	private static final Color LINK = new Color(42, 92, 172);

	/** The air above and below the title in its bar, in px (at 16 px). */
	private static final int BAR_AIR = 4;
	/** The gap under the bar and between the picture's cell and the facts, in px. */
	private static final int GAP = 6;
	/** The gap above the drops and above the description, in px. */
	private static final int SECTION_GAP = 8;
	/** The gap above the hint, in px. */
	private static final int HINT_GAP = 6;
	/** What a fact row and the drops' header row are taller than a line of text, and a drop's row, in px. */
	private static final int ROW_PAD = 6;
	private static final int DROP_PAD = 5;
	/** The air at each side of a cell's text, and a column's extra width over its widest text, in px. */
	private static final int CELL_AIR = 6;
	private static final int COLUMN_PAD = 12;
	/** The picture's cell is this wide, in px: a card with the wiki's picture has the bigger one. */
	private static final int PICTURE_CELL = 52;
	private static final int PICTURE_CELL_BIG = 72;

	/** The words of the hint that are a link. */
	private static final String LINK_WORDS = "Open wiki";
	/** The drops' column heads. */
	private static final String SOURCE = "Source";
	private static final String RARITY = "Rarity";

	private WikiCard()
	{
	}

	/**
	 * The facts the infobox shows as rows: the card's own, and, for a slayer task, its progress as a plain "Progress" row
	 * and the XP left as an "XP left" row in front of them, since this look has no bar. Every other card gives its own
	 * facts, as {@link CardPainter#factsOf} does.
	 */
	static List<HoverCard.Pair> factsOf(HoverCard card)
	{
		final HoverCard.Progress progress = card.progress();
		if (progress == null)
		{
			return CardPainter.factsOf(card);
		}
		final List<HoverCard.Pair> facts = new ArrayList<>();
		facts.add(new HoverCard.Pair("Progress", progress.text, ""));
		if (!progress.xpLeft.isEmpty())
		{
			facts.add(new HoverCard.Pair("XP left", progress.xpLeft, ""));
		}
		facts.addAll(CardPainter.factsOf(card));
		return facts;
	}

	/**
	 * Paints the infobox with its top-left corner at ({@code x}, {@code y}), exactly {@code width} px wide, and returns
	 * its size. The title bar, the picture and the facts, the drops, the {@code description} ("" for none: a paragraph,
	 * or a numbered list when it holds line breaks, see {@link CardPainter#layout}, in at most {@code maxLines} lines, a
	 * list taking its own cap) and the hint, in that order; the ground and the one pixel edge are the Wiki style's. The
	 * title is cut with "..." where it would not fit the bar, a source where it would pass its column. The text is
	 * antialiased.
	 *
	 * <p>Choice: the measuring pass and the drawing pass are one layout, run twice: the first draws nothing and gives the
	 * height the ground and the edge need, the second draws.
	 */
	static Dimension paint(Graphics2D g, HoverCard card, BufferedImage picture, int fontSize, int x, int y,
		String description, int width, int maxLines)
	{
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final Sheet sheet = new Sheet(g, card, picture, fontSize, width, description, maxLines);
		final int height = sheet.lay(false, x, y);
		CardStyle.WIKI.frame(g, x, y, width, height);
		sheet.lay(true, x, y);
		return new Dimension(width, height);
	}

	/** A sans serif font whose size, given at 16 px, is scaled to {@code fontSize}. */
	private static Font sans(int style, int sizeAt16, int fontSize)
	{
		return new Font(Font.SANS_SERIF, style, Math.max(1, Math.round(sizeAt16 * fontSize / 16f)));
	}

	/** What one infobox is laid out and drawn with: its fonts and metrics, its parts, and the pen. */
	private static final class Sheet
	{
		private final Graphics2D g;
		private final BufferedImage picture;
		private final int fontSize;
		private final int width;
		private final CardStyle style = CardStyle.WIKI;
		private final String name;
		private final String stack;
		private final String hint;
		private final boolean widePicture;
		private final List<HoverCard.Pair> facts;
		private final List<HoverCard.Pair> drops;
		private final List<CardPainter.TextLine> lines;
		private final Font title;
		private final Font bold;
		private final Font plain;
		private final Font text;
		private final Font small;
		private final FontMetrics titleMetrics;
		private final FontMetrics boldMetrics;
		private final FontMetrics plainMetrics;
		private final FontMetrics textMetrics;
		private final FontMetrics smallMetrics;
		/** The card's content width, between the insets, in px. */
		private final int inner;
		/** A line of the table font, ascent and descent; of the paragraph font and of the hint, with 2 more. */
		private final int tableLine;
		private final int textLine;
		private final int hintLine;
		private boolean draw;

		Sheet(Graphics2D g, HoverCard card, BufferedImage picture, int fontSize, int width, String description,
			int maxLines)
		{
			this.g = g;
			this.picture = picture;
			this.fontSize = fontSize;
			this.width = width;
			this.inner = Math.max(1, width - 2 * style.inset);
			this.name = CardPainter.nameOf(card);
			final String quantity = CardPainter.stackOf(card);
			this.stack = quantity == null ? "" : " " + quantity;
			this.hint = card.hint();
			this.widePicture = card.wikiPicture();
			this.facts = factsOf(card);
			this.drops = CardPainter.dropsOf(card);
			this.title = sans(Font.BOLD, 15, fontSize);
			this.bold = sans(Font.BOLD, 13, fontSize);
			this.plain = sans(Font.PLAIN, 13, fontSize);
			this.text = sans(Font.PLAIN, 14, fontSize);
			this.small = sans(Font.PLAIN, 12, fontSize);
			this.titleMetrics = g.getFontMetrics(title);
			this.boldMetrics = g.getFontMetrics(bold);
			this.plainMetrics = g.getFontMetrics(plain);
			this.textMetrics = g.getFontMetrics(text);
			this.smallMetrics = g.getFontMetrics(small);
			this.tableLine = plainMetrics.getAscent() + plainMetrics.getDescent();
			this.textLine = textMetrics.getAscent() + textMetrics.getDescent() + 2;
			this.hintLine = smallMetrics.getAscent() + smallMetrics.getDescent() + 2;
			this.lines = CardPainter.layout(description, textMetrics, inner, maxLines);
		}

		/** A size given at 16 px, at this card's text size. */
		private int sc(int atBase)
		{
			return CardPainter.scaled(atBase, fontSize);
		}

		/**
		 * Lays the card out from its top-left corner ({@code x}, {@code y}), drawing it when {@code drawing}; returns the
		 * card's height, the inset under the hint included.
		 */
		int lay(boolean drawing, int x, int y)
		{
			this.draw = drawing;
			final int left = x + style.inset;
			// The last pixel column a table's border is drawn on: as far from the card's edge as the first is.
			final int right = left + inner - 1;

			// The title bar, across the card inside the edge.
			final int barHeight = titleMetrics.getAscent() + titleMetrics.getDescent() + 2 * sc(BAR_AIR);
			int cursor = y + 1;
			fill(x + 1, cursor, width - 2, barHeight, BAR);
			final int stackWidth = stack.isEmpty() ? 0 : titleMetrics.stringWidth(stack);
			final String shown = CardPainter.fit(name, titleMetrics, Math.max(1, inner - stackWidth));
			final int titleLeft = x + (width - titleMetrics.stringWidth(shown) - stackWidth) / 2;
			final int titleBaseline = cursor + sc(BAR_AIR) + titleMetrics.getAscent();
			string(shown, title, style.title, titleLeft, titleBaseline);
			string(stack, title, STACK, titleLeft + titleMetrics.stringWidth(shown), titleBaseline);
			cursor += barHeight + sc(GAP);

			cursor += pictureAndFacts(left, right, cursor);
			if (!drops.isEmpty())
			{
				cursor += sc(SECTION_GAP);
				cursor += dropTable(left, right, cursor);
			}
			if (!lines.isEmpty())
			{
				cursor += sc(SECTION_GAP);
				for (final CardPainter.TextLine line : lines)
				{
					string(line.text, text, style.text, left + line.indent, cursor + textMetrics.getAscent());
					cursor += textLine;
				}
			}
			cursor += sc(HINT_GAP);
			drawHint(left, cursor + smallMetrics.getAscent());
			cursor += hintLine;
			return cursor + style.inset - y;
		}

		/**
		 * The picture's cell at the left and the facts as a table beside it, from ({@code left}, {@code top}); returns
		 * the block's height: the facts' rows, or the cell's own width when that is more. A card with no facts has the
		 * cell alone.
		 */
		private int pictureAndFacts(int left, int right, int top)
		{
			final int cellWidth = sc(widePicture ? PICTURE_CELL_BIG : PICTURE_CELL);
			final int rowHeight = tableLine + sc(ROW_PAD);
			final int height = Math.max(facts.size() * rowHeight, cellWidth);
			cell(left, top, cellWidth, height, CELL, style.rule);
			fitted(left + 1, top + 1, cellWidth - 1, height - 1);
			if (facts.isEmpty())
			{
				return height;
			}

			final int tableLeft = left + cellWidth + sc(GAP);
			int labelWidth = 0;
			for (final HoverCard.Pair fact : facts)
			{
				labelWidth = Math.max(labelWidth, boldMetrics.stringWidth(fact.label));
			}
			labelWidth = Math.min(labelWidth + sc(COLUMN_PAD), Math.max(1, (right - tableLeft) / 2));
			final int air = sc(CELL_AIR);
			for (int k = 0; k < facts.size(); k++)
			{
				final HoverCard.Pair fact = facts.get(k);
				final int rowTop = top + k * rowHeight;
				final int baseline = rowTop + (rowHeight - tableLine) / 2 + plainMetrics.getAscent();
				cell(tableLeft, rowTop, labelWidth, rowHeight, LABEL_CELL, style.rule);
				cell(tableLeft + labelWidth, rowTop, right - tableLeft - labelWidth, rowHeight, CELL, style.rule);
				string(CardPainter.fit(fact.label, boldMetrics, Math.max(1, labelWidth - air)), bold, style.label,
					tableLeft + air, baseline);

				final int valueLeft = tableLeft + labelWidth + air;
				final int room = Math.max(1, right - valueLeft - air);
				final String value = CardPainter.fit(fact.value, plainMetrics, room);
				string(value, plain, style.value, valueLeft, baseline);
				final int used = plainMetrics.stringWidth(value);
				if (value.equals(fact.value) && !fact.note.isEmpty())
				{
					final String note = CardPainter.fit(fact.note, plainMetrics, Math.max(1, room - used));
					if (used + plainMetrics.stringWidth(note) <= room)
					{
						string(note, plain, style.hint, valueLeft + used, baseline);
					}
				}
			}
			return height;
		}

		/**
		 * The drops as a table across the card, from ({@code left}, {@code top}): a header row, then a row to a drop, the
		 * source at the left in link blue and the rate in a column of its own at the right; returns the table's height.
		 */
		private int dropTable(int left, int right, int top)
		{
			int rateWidth = boldMetrics.stringWidth(RARITY);
			for (final HoverCard.Pair drop : drops)
			{
				rateWidth = Math.max(rateWidth, plainMetrics.stringWidth(drop.value));
			}
			rateWidth += sc(COLUMN_PAD);
			final int air = sc(CELL_AIR);
			final int rateLeft = left + inner - rateWidth;
			final int headHeight = tableLine + sc(ROW_PAD);
			final int rowHeight = tableLine + sc(DROP_PAD);

			cell(left, top, right - left, headHeight, BAR, style.rule);
			final int headBaseline = top + (headHeight - tableLine) / 2 + boldMetrics.getAscent();
			string(SOURCE, bold, style.title, left + air, headBaseline);
			string(RARITY, bold, style.title, rateLeft + air, headBaseline);

			int rowTop = top + headHeight;
			for (int i = 0; i < drops.size(); i++)
			{
				final HoverCard.Pair drop = drops.get(i);
				cell(left, rowTop, right - left, rowHeight, i % 2 == 0 ? CELL : style.ground, DROP_EDGE);
				final int baseline = rowTop + (rowHeight - tableLine) / 2 + plainMetrics.getAscent();
				if (drop.value.isEmpty())
				{
					string(CardPainter.fit(drop.label, plainMetrics, Math.max(1, inner - 2 * air)), plain, LINK,
						left + air, baseline);
				}
				else
				{
					string(CardPainter.fit(drop.label, plainMetrics, Math.max(1, rateLeft - left - 2 * air)), plain, LINK,
						left + air, baseline);
					string(drop.value, plain, style.value, rateLeft + air, baseline);
				}
				rowTop += rowHeight;
			}
			return rowTop - top;
		}

		/** The hint on {@code baseline}, its "Open wiki" in link blue with a line under it. */
		private void drawHint(int left, int baseline)
		{
			final int at = hint.indexOf(LINK_WORDS);
			if (at < 0)
			{
				string(hint, small, style.hint, left, baseline);
				return;
			}
			final String before = hint.substring(0, at);
			final String after = hint.substring(at + LINK_WORDS.length());
			final int linkLeft = left + smallMetrics.stringWidth(before);
			final int linkWidth = smallMetrics.stringWidth(LINK_WORDS);
			string(before, small, style.hint, left, baseline);
			string(LINK_WORDS, small, LINK, linkLeft, baseline);
			fill(linkLeft, baseline + 2, linkWidth, 1, LINK);
			string(after, small, style.hint, linkLeft + linkWidth, baseline);
		}

		/** A cell: {@code fill} over the rectangle and a 1 px {@code edge} round it, the edge on the last column and row too. */
		private void cell(int x, int y, int w, int h, Color fill, Color edge)
		{
			if (draw)
			{
				g.setColor(fill);
				g.fillRect(x, y, w + 1, h + 1);
				g.setColor(edge);
				g.drawRect(x, y, w, h);
			}
		}

		private void fill(int x, int y, int w, int h, Color colour)
		{
			if (draw)
			{
				g.setColor(colour);
				g.fillRect(x, y, w, h);
			}
		}

		private void string(String words, Font font, Color colour, int x, int baseline)
		{
			if (draw && !words.isEmpty())
			{
				g.setFont(font);
				g.setColor(colour);
				g.drawString(words, x, baseline);
			}
		}

		/**
		 * Draws the picture centred, both ways, in the {@code w} by {@code h} rectangle at ({@code x}, {@code y}); a
		 * picture wider or taller than it is drawn scaled down to fit, its ratio kept. A null picture leaves it empty.
		 */
		private void fitted(int x, int y, int w, int h)
		{
			if (!draw || picture == null)
			{
				return;
			}
			final int pictureWidth = picture.getWidth();
			final int pictureHeight = picture.getHeight();
			if (pictureWidth <= w && pictureHeight <= h)
			{
				g.drawImage(picture, x + (w - pictureWidth) / 2, y + (h - pictureHeight) / 2, null);
				return;
			}
			final double scale = Math.min(w / (double) pictureWidth, h / (double) pictureHeight);
			final int fittedWidth = Math.max(1, (int) Math.round(pictureWidth * scale));
			final int fittedHeight = Math.max(1, (int) Math.round(pictureHeight * scale));
			g.drawImage(picture, x + (w - fittedWidth) / 2, y + (h - fittedHeight) / 2, fittedWidth, fittedHeight, null);
		}
	}
}
