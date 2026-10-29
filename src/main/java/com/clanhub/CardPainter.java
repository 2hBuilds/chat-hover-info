package com.clanhub;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.FontManager;

/**
 * Draws a card and decides where it opens. Pure drawing: a {@link HoverCard}, a picture, a size and the description
 * text in, pixels and the card's size out. Two looks share one ground, a brown ground with a black edge and a lighter
 * line inside it. The split card is a header across the top, the picture (in a 36 x 32 box, or, for a card of a boss,
 * monster, raid or chest, the wiki's own picture in a 64 x 64 box), the bold title with the stack after it and the
 * facts of the first row at the right end of the same line (or, when they do not fit, under the title, wrapped onto as
 * many rows as they need); then, for an item, one grey line of drops, each source with its rate in white, flowing to the card's width; then
 * a rule and the description in white across the full width in at most the caller's number of lines; then a rule and the
 * hint in small grey type. It is as wide as its header, its description, its drops or its hint need, and never wider than
 * the caller's cap at the 16 px text size. The wide card is as wide as it is asked: the same header row, the drops (grey too) spread along
 * a row of their own, a rule, the description in white over the full width, and the hint. A drop with no rate ("and 2
 * more") is drawn as grey words alone in either card. The painter wraps the description itself, by pixel width in the
 * real font. The colours, the frame, the gap from the edge to the content and the rules come from a {@link CardStyle}, the
 * card's theme; a style that is an infobox (Wiki) hands the whole card to {@link WikiCard}, which lays it out as the
 * wiki's infobox with tables.
 */
final class CardPainter
{
	/** The Classic card's ground, solid: nothing of the game shows through it. */
	static final Color GROUND = new Color(70, 61, 50);
	/** The line inside the black edge, and the rules across the text. */
	static final Color LINE = new Color(118, 104, 82);
	/** The labels, the notes after a value, and the drops. */
	static final Color LABEL = new Color(165, 165, 165);
	/** The hint's grey. */
	static final Color HINT = new Color(135, 135, 135);
	/**
	 * The description's colour on every card this painter lays out (the Wiki infobox has its own): white, in every theme,
	 * so the paragraph reads the same way whatever lies under it.
	 */
	static final Color DESCRIPTION = Color.WHITE;

	/** The Classic card's gap from its edge to its content: the black edge and the line inside it, then the padding, in px. */
	static final int INSET = 2 + 7;

	/** The item sprite's box, in px. */
	private static final int SPRITE_WIDTH = 36;
	private static final int SPRITE_HEIGHT = 32;
	/**
	 * The wiki picture's square box, in px: the header row of a card with {@link HoverCard#wikiPicture} is at least this
	 * tall, and its picture is drawn centred in it, both ways.
	 */
	static final int PICTURE_BOX = 64;
	/** The gap between the sprite and the text, in px. */
	private static final int SPRITE_GAP = 8;
	/** The air above and below a rule, in px. */
	private static final int RULE_AIR = 2;
	/** A label and its value are set apart by this. */
	private static final String LABEL_GAP = " ";
	/** What a cut line ends in. */
	private static final String MORE = "...";
	/** The caption over the drops. */
	private static final String DROPS = "Drops";

	/** The size the widths below are given at, in px. */
	private static final float BASE_SIZE = 16f;
	/** The most lines the wide card's paragraph takes; the split card's is the caller's. */
	private static final int WIDE_LINES = 2;
	/** The most lines a numbered list takes, at any width; a paragraph keeps the Card width setting's count. */
	static final int LIST_LINES = 10;
	/** The least air between two facts, and between two drops of the wide card, at 16 px. */
	private static final int FACT_GAP = 16;
	private static final int DROP_GAP = 12;
	/** A slayer task's progress bar, at 16 px: its length, its thickness, and the air between it and the words beside it. */
	private static final int PROGRESS_BAR = 120;
	private static final int PROGRESS_THICK = 6;
	private static final int PROGRESS_GAP = 10;
	/** The label that follows the XP left, in the label colour. */
	private static final String XP_LEFT = " XP left";

	/** The card opens this far above the cursor; when it opens below instead, its top is this far under it. */
	private static final int OPEN_ABOVE = 6;
	private static final int OPEN_BELOW = 18;

	private CardPainter()
	{
	}

	/**
	 * Where a split card of {@code card} size opens for a cursor on a canvas of {@code canvas} size: centred on the
	 * cursor, its bottom edge 6 px above it. Its left edge is held between the canvas's left edge and the place where its
	 * right edge meets the canvas's right edge. If its top would pass the top of the canvas it opens below the cursor
	 * instead, its top 18 px under it, held so that its bottom does not pass the canvas's bottom.
	 */
	static Rectangle placeCentred(Point cursor, Dimension card, Dimension canvas)
	{
		final int x = Math.max(0, Math.min(cursor.x - card.width / 2, canvas.width - card.width));
		int y = cursor.y - OPEN_ABOVE - card.height;
		if (y < 0)
		{
			y = Math.min(cursor.y + OPEN_BELOW, canvas.height - card.height);
		}
		return new Rectangle(x, y, card.width, card.height);
	}

	/**
	 * Where a wide card of {@code card} size opens above the chatbox's frame {@code chat}: its left edge on the frame's,
	 * its bottom edge on the frame's top edge. A card taller than the room above the frame is held at the top of the
	 * canvas.
	 */
	static Rectangle placeAboveChat(Rectangle chat, Dimension card)
	{
		return new Rectangle(chat.x, Math.max(0, chat.y - card.height), card.width, card.height);
	}

	/**
	 * Paints the split card with its top-left corner at ({@code x}, {@code y}) and returns its size. From the top: the
	 * header (the picture, the bold title with the stack in grey after it, the facts of the first row at the right end of
	 * that line, or under the title in as many rows as they need when they do not fit), the drops as one grey line of words with each rate in
	 * white (a drop with no rate is its grey words alone), the {@code description}, one text ("" for none), in white
	 * across the full width in at most {@code paragraphLines} lines with "..." ending the last when it goes on, and,
	 * under a rule, the hint. The description is a paragraph, or a numbered list when it holds line breaks (see
	 * {@link #layout}), a list taking up to {@link #LIST_LINES} lines whatever {@code paragraphLines} is. The card's own
	 * notes are not drawn, the caller passes them as the description. The width is
	 * as much as the widest of the header, the whole description on one line (the widest item of a list), the whole drops
	 * line on one line and the
	 * hint ask for, never more than {@code capAt16} px at {@code fontSize} 16 and growing and shrinking with it; a short
	 * card is narrow. The title
	 * is in the client's bold game font, everything else in its plain game font, the hint in its small one, all derived
	 * to {@code fontSize}. The picture's box is 36 x 32, drawn from its top-left corner as it comes, or, for a card with
	 * {@link HoverCard#wikiPicture}, {@link #PICTURE_BOX} square with the picture centred in it (scaled down to fit
	 * when it is bigger), which makes the header row at least that tall and moves the text to its right. A null picture
	 * leaves the box empty. The text is antialiased.
	 *
	 * <p>Choice: the measuring pass decides the width and the drawing pass lays out again at that width, so the one
	 * layout serves both.
	 *
	 * <p>The colours, the frame and the inset are {@code style}'s (the words above are the Classic card's): the card is
	 * {@code 2 * style.inset} wider and taller than its content, and the edge, the strings, the rules and the hint are
	 * all in the style's own. The card is {@link CardStyle#bottomExtra} px taller (at 16 px, scaled with {@code fontSize})
	 * under the hint when the style has room there.
	 */
	static Dimension paintSplit(Graphics2D g, HoverCard card, BufferedImage picture, int fontSize, int x, int y,
		String description, int capAt16, int paragraphLines, CardStyle style)
	{
		if (style.infobox)
		{
			return WikiCard.paint(g, card, picture, fontSize, x, y, description, scaled(capAt16, fontSize), paragraphLines);
		}
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final Faces faces = new Faces(g, fontSize);
		final Dimension content = splitContent(new Pen(g, false, style), faces, card, picture, fontSize, 0, 0, description,
			capAt16, paragraphLines);
		final int width = content.width + 2 * style.inset;
		final int height = 2 * style.inset + content.height + RULE_AIR + 1 + RULE_AIR + faces.hintHeight
			+ scaled(style.bottomExtra, fontSize);

		style.frame(g, x, y, width, height);
		final Pen pen = new Pen(g, true, style, x, width);
		splitContent(pen, faces, card, picture, fontSize, x, y, description, capAt16, paragraphLines);
		int lineTop = y + style.inset + content.height + RULE_AIR;
		pen.rule(x + style.inset, lineTop, width - 2 * style.inset, 1);
		lineTop += 1 + RULE_AIR;
		pen.string(card.hint(), faces.hint, style.hint, x + style.inset, lineTop + faces.hintMetrics.getAscent());
		pen.finish(x, y, width, height);
		return new Dimension(width, height);
	}

	/**
	 * Paints the wide card, {@code width} px wide, with its top-left corner at ({@code x}, {@code y}) and returns its
	 * size. First row: the picture, the bold title with the stack in grey after it, and at the row's right end the facts
	 * of the first row; when they do not fit beside the title they go under it, in as many rows as they need. The picture's
	 * box is the split card's: 36 x 32, or {@link #PICTURE_BOX} square with the picture centred for a card with
	 * {@link HoverCard#wikiPicture}. Second row, when the card has drop rows: the grey caption and the drops spread evenly
	 * over the rest of the width, each its rate then its source, all in the label grey (a drop with no rate is its source
	 * alone, counting no gap for the rate), wrapping to a new row when they do not fit. Then a rule, the
	 * {@code description} ("" for none), a paragraph, or a numbered list when the description holds line breaks (see
	 * {@link #layout}), wrapped to the full width in white, a paragraph in at most two lines and a list in at most
	 * {@link #LIST_LINES}, the last line ending in "..." when the text goes on, and the hint. Fonts and colours as the
	 * split card's. The {@code accent} no longer colours anything, the drops being grey now; the parameter stays only so
	 * that the caller's signature does. The colours, the frame, the inset and the room under the hint are {@code style}'s.
	 */
	static Dimension paintWide(Graphics2D g, HoverCard card, BufferedImage picture, int fontSize, int width, int x, int y,
		String description, Color accent, CardStyle style)
	{
		if (style.infobox)
		{
			return WikiCard.paint(g, card, picture, fontSize, x, y, description, width, WIDE_LINES);
		}
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		final Faces faces = new Faces(g, fontSize);
		final int content = wideContent(new Pen(g, false, style), faces, card, picture, fontSize, width, 0, 0, description,
			accent);
		final int height = 2 * style.inset + content + faces.hintHeight + scaled(style.bottomExtra, fontSize);

		style.frame(g, x, y, width, height);
		final Pen pen = new Pen(g, true, style, x, width);
		wideContent(pen, faces, card, picture, fontSize, width, x, y, description, accent);
		pen.string(card.hint(), faces.hint, style.hint, x + style.inset,
			y + style.inset + content + faces.hintMetrics.getAscent());
		pen.finish(x, y, width, height);
		return new Dimension(width, height);
	}

	/** A width given at 16 px, at {@code fontSize}. */
	static int scaled(int atBase, int fontSize)
	{
		return Math.round(atBase * fontSize / BASE_SIZE);
	}

	/**
	 * Lays out the split card's content from ({@code ox}, {@code oy}), the card's top-left corner, drawing it when
	 * {@code pen} draws; returns the width and the height of the content inside the card's inset, the height ending at
	 * the description's last line, which the hint's rule follows. In order: the header row, the drops line, and, when
	 * there is a description, a rule and the description in white.
	 *
	 * <p>Choice: the width is the widest need among the header, the whole description on one line, the whole drops line
	 * on one line and the hint, held to {@code capAt16} px at 16 px less the inset; so a short card is narrow, and a long
	 * text is cut or flows to the line below at the widest. A paragraph takes at most {@code paragraphLines} lines, a
	 * numbered list at most {@link #LIST_LINES}, and the list's width is the widest of its items on a line each.
	 */
	private static Dimension splitContent(Pen pen, Faces faces, HoverCard card, BufferedImage picture, int fontSize, int ox,
		int oy, String description, int capAt16, int paragraphLines)
	{
		final FontMetrics body = faces.bodyMetrics;
		final CardStyle style = pen.style;
		final int innerMax = scaled(capAt16, fontSize) - 2 * style.inset;
		final int left = ox + style.inset;
		final int top = oy + style.inset;
		final List<Word> dropWords = dropWords(card, style);

		// The width: the widest of what the four parts ask for, held to the most.
		final int paragraphNeed = Math.min(innerMax, descriptionNeed(description, body));
		final int dropsNeed = dropWords.isEmpty() ? 0
			: Math.min(innerMax, lineWidth(flow(dropWords, body, Integer.MAX_VALUE).get(0), body));
		final int hintNeed = faces.hintMetrics.stringWidth(card.hint());
		final int need = Math.max(Math.max(headerNeed(faces, card, fontSize, innerMax), paragraphNeed),
			Math.max(dropsNeed, hintNeed));
		final int inner = style.picture != null ? innerMax : Math.min(innerMax, need);

		// The header, then the drops: "Drops:" and each source in grey, each rate in white, one comma after each drop but the last.
		int y = top + headerRow(pen, faces, card, picture, fontSize, left, top, inner);
		if (!dropWords.isEmpty())
		{
			for (final List<Word> line : flow(dropWords, body, inner))
			{
				int x = left;
				for (final Word word : line)
				{
					pen.string(word.text, faces.body, word.colour, x, y + body.getAscent());
					pen.string(word.tail, faces.body, style.label, x + body.stringWidth(word.text), y + body.getAscent());
					x += word.width(body) + body.stringWidth(LABEL_GAP);
				}
				y += faces.bodyHeight;
			}
		}

		// The description, under a rule across the card; with none, no rule and no air.
		final List<TextLine> lines = layout(description, body, inner, paragraphLines);
		if (!lines.isEmpty())
		{
			y += RULE_AIR;
			pen.rule(left, y, inner, 1);
			y += 1 + RULE_AIR;
			for (final TextLine line : lines)
			{
				pen.string(line.text, faces.body, style.picture != null ? style.text : DESCRIPTION, left + line.indent,
					y + body.getAscent());
				y += faces.bodyHeight;
			}
		}
		return new Dimension(inner, y - top);
	}

	/**
	 * Lays out the wide card from ({@code ox}, {@code oy}), the card's top-left corner, drawing it when {@code pen}
	 * draws; returns the height from the top of the content to the end of the description, which the hint follows. The
	 * description is laid out to the inner width: a paragraph in at most {@link #WIDE_LINES} lines, a numbered list in at
	 * most {@link #LIST_LINES}.
	 */
	private static int wideContent(Pen pen, Faces faces, HoverCard card, BufferedImage picture, int fontSize, int width,
		int ox, int oy, String description, Color accent)
	{
		final CardStyle style = pen.style;
		final int left = ox + style.inset;
		final int innerWidth = Math.max(1, width - 2 * style.inset);
		final int right = left + innerWidth;
		final int top = oy + style.inset;
		final int dropGap = scaled(DROP_GAP, fontSize);
		final FontMetrics body = faces.bodyMetrics;

		// Row 1: the picture, the title and the stack, the facts at the right end or in rows under the title.
		int y = top + headerRow(pen, faces, card, picture, fontSize, left, top, innerWidth);

		// Row 2: the caption and the drops, spread evenly over the rest of the width, wrapping when they do not fit.
		final List<HoverCard.Pair> drops = dropsOf(card);
		if (!drops.isEmpty())
		{
			pen.string(DROPS, faces.body, style.label, left, y + body.getAscent());
			final int dropsLeft = left + body.stringWidth(DROPS) + dropGap;
			final int available = Math.max(1, right - dropsLeft);
			final int spaceWidth = body.stringWidth(LABEL_GAP);
			int next = 0;
			while (next < drops.size())
			{
				final List<HoverCard.Pair> rowDrops = new ArrayList<>();
				final List<Integer> widths = new ArrayList<>();
				int sum = 0;
				while (next < drops.size())
				{
					final HoverCard.Pair drop = drops.get(next);
					final int wanted = drop.value.isEmpty() ? body.stringWidth(drop.label)
						: body.stringWidth(drop.value) + spaceWidth + body.stringWidth(drop.label);
					final int w = Math.min(wanted, available);
					if (!rowDrops.isEmpty() && sum + w + rowDrops.size() * dropGap > available)
					{
						break;
					}
					rowDrops.add(drop);
					widths.add(w);
					sum += w;
					next++;
				}
				final float gap = rowDrops.size() > 1 ? (available - sum) / (float) (rowDrops.size() - 1) : 0f;
				int before = 0;
				for (int i = 0; i < rowDrops.size(); i++)
				{
					final HoverCard.Pair drop = rowDrops.get(i);
					final int w = widths.get(i);
					final int dropX = i == rowDrops.size() - 1 && i > 0 ? right - w
						: dropsLeft + before + Math.round(i * gap);
					if (drop.value.isEmpty())
					{
						pen.string(fit(drop.label, body, Math.max(1, w)), faces.body, style.label, dropX, y + body.getAscent());
					}
					else
					{
						final int rateWidth = body.stringWidth(drop.value);
						pen.string(drop.value, faces.body, style.wideRate, dropX, y + body.getAscent());
						pen.string(fit(drop.label, body, Math.max(1, w - rateWidth - spaceWidth)), faces.body, style.label,
							dropX + rateWidth + spaceWidth, y + body.getAscent());
					}
					before += w;
				}
				y += faces.bodyHeight;
			}
		}

		y += RULE_AIR;
		pen.rule(left, y, innerWidth, 1);
		y += 1 + RULE_AIR;
		for (final TextLine line : layout(description, body, innerWidth, WIDE_LINES))
		{
			pen.string(line.text, faces.body, style.picture != null ? style.text : DESCRIPTION, left + line.indent,
				y + body.getAscent());
			y += faces.bodyHeight;
		}
		return y - top;
	}

	/**
	 * Lays out the first row both cards open with, from ({@code left}, {@code top}) across {@code innerWidth} px, drawing
	 * it when {@code pen} draws; returns the row's height. The picture at the left, then the bold title, cut with "..."
	 * where it would pass the right edge, with the stack in grey after it; the facts of the first row at the right end of
	 * the line, or, when they would not fit beside the title and the stack, under the title at the text's left, set in
	 * rows by {@link #factRows} (as many facts to a row as fit between the text's left and the right edge), which makes
	 * the row taller by a body line for each fact row. A slayer task's card has a progress row first under the title, the
	 * facts after it, always under the title ({@link #drawProgress}).
	 */
	private static int headerRow(Pen pen, Faces faces, HoverCard card, BufferedImage picture, int fontSize, int left,
		int top, int innerWidth)
	{
		final FontMetrics body = faces.bodyMetrics;
		final int right = left + innerWidth;
		final int factGap = scaled(FACT_GAP, fontSize);
		final int boxWidth = card.wikiPicture() ? PICTURE_BOX : SPRITE_WIDTH;
		final int boxHeight = card.wikiPicture() ? PICTURE_BOX : SPRITE_HEIGHT;
		final int textLeft = left + boxWidth + SPRITE_GAP;
		final String stack = stackOf(card);
		final int stackWidth = stack == null ? 0 : body.stringWidth(LABEL_GAP + stack);
		final String name = fit(nameOf(card), faces.titleMetrics, Math.max(1, right - textLeft - stackWidth));
		final int nameWidth = faces.titleMetrics.stringWidth(name);
		final List<HoverCard.Pair> facts = factsOf(card);
		final int factsWidth = rowWidth(facts, body, factGap);
		final HoverCard.Progress progress = card.progress();
		final boolean under = progress != null
			|| !facts.isEmpty() && textLeft + nameWidth + stackWidth + factGap + factsWidth > right;
		final List<List<HoverCard.Pair>> underRows = under ? factRows(facts, body, right - textLeft, factGap)
			: new ArrayList<List<HoverCard.Pair>>();
		final int progressHeight = progress == null ? 0 : faces.bodyHeight;
		final int rowHeight = Math.max(boxHeight,
			under ? faces.titleHeight + progressHeight + underRows.size() * faces.bodyHeight : faces.titleHeight);
		if (card.wikiPicture())
		{
			pen.pictureInBox(picture, left, top, PICTURE_BOX);
		}
		else
		{
			pen.picture(picture, left, top);
		}
		final int titleTop = under ? top : top + (rowHeight - faces.titleHeight) / 2;
		pen.title(name, faces.title, textLeft, titleTop + faces.titleMetrics.getAscent());
		if (stack != null)
		{
			pen.string(LABEL_GAP + stack, faces.body, pen.style.label, textLeft + nameWidth,
				titleTop + faces.titleMetrics.getAscent());
		}
		if (under)
		{
			int baseline = top + faces.titleHeight + body.getAscent();
			if (progress != null)
			{
				drawProgress(pen, faces, progress, textLeft, baseline, fontSize);
				baseline += faces.bodyHeight;
			}
			for (final List<HoverCard.Pair> factRow : underRows)
			{
				drawRow(pen, faces, factRow, textLeft, baseline, factGap);
				baseline += faces.bodyHeight;
			}
		}
		else if (!facts.isEmpty())
		{
			drawRow(pen, faces, facts, right - factsWidth, top + (rowHeight - faces.bodyHeight) / 2 + body.getAscent(),
				factGap);
		}
		return rowHeight;
	}

	/**
	 * The width the header row asks for, in px: the picture, its gap, the whole title, the stack and, when there are
	 * facts, the gap and the facts, on one line. When that passes {@code innerMax} the facts go under the title, as
	 * {@link #headerRow} puts them, and the row asks for the picture, its gap and the wider of the title (cut to
	 * {@code innerMax}, with the stack) and the widest single fact.
	 *
	 * <p>Choice: the facts under the title wrap onto rows ({@link #factRows}), so the card need not be as wide as the
	 * whole row of them: it asks for the widest fact, and the rows fill the width the other parts of the card give it.
	 */
	private static int headerNeed(Faces faces, HoverCard card, int fontSize, int innerMax)
	{
		final FontMetrics body = faces.bodyMetrics;
		final int factGap = scaled(FACT_GAP, fontSize);
		final int textLeft = (card.wikiPicture() ? PICTURE_BOX : SPRITE_WIDTH) + SPRITE_GAP;
		final String stack = stackOf(card);
		final int stackWidth = stack == null ? 0 : body.stringWidth(LABEL_GAP + stack);
		final int titleWidth = faces.titleMetrics.stringWidth(nameOf(card));
		final List<HoverCard.Pair> facts = factsOf(card);
		final int factsWidth = rowWidth(facts, body, factGap);
		final int beside = textLeft + titleWidth + stackWidth + (facts.isEmpty() ? 0 : factGap + factsWidth);
		final HoverCard.Progress progress = card.progress();
		if (progress == null && (facts.isEmpty() || beside <= innerMax))
		{
			return beside;
		}
		final String fitted = fit(nameOf(card), faces.titleMetrics, Math.max(1, innerMax - textLeft - stackWidth));
		int widestFact = progress == null ? 0 : progressWidth(faces, progress, fontSize);
		for (final HoverCard.Pair fact : facts)
		{
			widestFact = Math.max(widestFact, rowWidth(Collections.singletonList(fact), body, factGap));
		}
		return textLeft + Math.max(faces.titleMetrics.stringWidth(fitted) + stackWidth, widestFact);
	}

	/**
	 * The progress row of a slayer task's card, on {@code baseline} from {@code left}: the bar, its track in the style's rule
	 * colour and its fill, to the killed of the total, in its value colour (so that every look has its own), and beside it
	 * the words ("10/155") in the value colour and the XP left ("16,820" in the value colour, then "XP left" in the label
	 * colour). With no total there is no bar: the words ("145 left") and the XP left, from {@code left}.
	 */
	private static void drawProgress(Pen pen, Faces faces, HoverCard.Progress progress, int left, int baseline, int fontSize)
	{
		final int gap = scaled(PROGRESS_GAP, fontSize);
		int x = left;
		if (progress.hasBar())
		{
			final int length = scaled(PROGRESS_BAR, fontSize);
			final int thick = scaled(PROGRESS_THICK, fontSize);
			final int filled = progress.fraction <= 0 ? 0
				: Math.min(length, Math.max(1, (int) Math.round(length * progress.fraction)));
			pen.bar(x, baseline - faces.bodyMetrics.getAscent() + (faces.bodyHeight - thick) / 2, length, thick, filled);
			x += length + gap;
		}
		pen.string(progress.text, faces.body, pen.style.value, x, baseline);
		x += faces.bodyMetrics.stringWidth(progress.text);
		if (!progress.xpLeft.isEmpty())
		{
			x += scaled(FACT_GAP, fontSize) / 2;
			pen.string(progress.xpLeft, faces.body, pen.style.value, x, baseline);
			x += faces.bodyMetrics.stringWidth(progress.xpLeft);
			pen.string(XP_LEFT, faces.body, pen.style.label, x, baseline);
		}
	}

	/** The width {@link #drawProgress} draws in, in px. */
	private static int progressWidth(Faces faces, HoverCard.Progress progress, int fontSize)
	{
		int width = faces.bodyMetrics.stringWidth(progress.text);
		if (progress.hasBar())
		{
			width += scaled(PROGRESS_BAR, fontSize) + scaled(PROGRESS_GAP, fontSize);
		}
		if (!progress.xpLeft.isEmpty())
		{
			width += scaled(FACT_GAP, fontSize) / 2 + faces.bodyMetrics.stringWidth(progress.xpLeft + XP_LEFT);
		}
		return width;
	}

	/**
	 * The facts set in rows, in order, each row as many facts as fit {@code width} px with {@code gap} px between them
	 * (greedily: a fact that does not fit after the ones before it starts the next row); a fact wider than the width
	 * stands alone on its row. No facts give no rows, and no row is empty.
	 */
	static List<List<HoverCard.Pair>> factRows(List<HoverCard.Pair> facts, FontMetrics metrics, int width, int gap)
	{
		final List<List<HoverCard.Pair>> rows = new ArrayList<>();
		List<HoverCard.Pair> row = new ArrayList<>();
		int used = 0;
		for (final HoverCard.Pair fact : facts)
		{
			final int wanted = rowWidth(Collections.singletonList(fact), metrics, gap);
			if (!row.isEmpty() && (long) used + gap + wanted > width)
			{
				rows.add(row);
				row = new ArrayList<>();
				used = 0;
			}
			used += row.isEmpty() ? wanted : gap + wanted;
			row.add(fact);
		}
		if (!row.isEmpty())
		{
			rows.add(row);
		}
		return rows;
	}

	/**
	 * The drops line as words, in order: "Drops:", then for each drop the words of its source in the label grey and its
	 * rate in white (none for a drop with no rate), a comma in the label grey after every drop but the last, attached to
	 * the word before it. No words when the card has no drops.
	 */
	private static List<Word> dropWords(HoverCard card, CardStyle style)
	{
		final List<Word> words = new ArrayList<>();
		final List<HoverCard.Pair> drops = dropsOf(card);
		if (drops.isEmpty())
		{
			return words;
		}
		words.add(new Word(DROPS + ":", style.label, ""));
		for (int i = 0; i < drops.size(); i++)
		{
			final HoverCard.Pair drop = drops.get(i);
			for (final String part : drop.label.trim().split("\\s+"))
			{
				if (!part.isEmpty())
				{
					words.add(new Word(part, style.label, ""));
				}
			}
			if (!drop.value.isEmpty())
			{
				words.add(new Word(drop.value, style.value, ""));
			}
			if (i < drops.size() - 1)
			{
				final Word last = words.get(words.size() - 1);
				words.set(words.size() - 1, new Word(last.text, last.colour, ","));
			}
		}
		return words;
	}

	/**
	 * The words set in lines, a space apart, a word that would pass {@code width} px starting the next line; a word
	 * wider than the width stands on a line of its own. At least one line when there are words.
	 */
	private static List<List<Word>> flow(List<Word> words, FontMetrics metrics, int width)
	{
		final int space = metrics.stringWidth(LABEL_GAP);
		final List<List<Word>> lines = new ArrayList<>();
		List<Word> line = new ArrayList<>();
		int used = 0;
		for (final Word word : words)
		{
			final int wordWidth = word.width(metrics);
			if (!line.isEmpty() && (long) used + space + wordWidth > width)
			{
				lines.add(line);
				line = new ArrayList<>();
				used = 0;
			}
			used += line.isEmpty() ? wordWidth : space + wordWidth;
			line.add(word);
		}
		if (!line.isEmpty())
		{
			lines.add(line);
		}
		return lines;
	}

	/** One line of words as {@link #flow} sets it: the words and the spaces between, in px. */
	private static int lineWidth(List<Word> line, FontMetrics metrics)
	{
		int width = 0;
		for (final Word word : line)
		{
			width += word.width(metrics);
		}
		return width + Math.max(0, line.size() - 1) * metrics.stringWidth(LABEL_GAP);
	}

	/** The card's title without the stack ("Dragon platelegs x7" is "Dragon platelegs"). */
	static String nameOf(HoverCard card)
	{
		final String title = card.title();
		final String stack = stackOf(card);
		return stack != null && title.endsWith(LABEL_GAP + stack)
			? title.substring(0, title.length() - stack.length() - 1) : title;
	}

	/** The stack as "x7", or null for a card that is not a stack. */
	static String stackOf(HoverCard card)
	{
		return card.quantity() > 1 ? "x" + card.quantity() : null;
	}

	/** The pairs of the card's first row, the facts; none when it has no rows. */
	static List<HoverCard.Pair> factsOf(HoverCard card)
	{
		final List<List<HoverCard.Pair>> rows = card.rows();
		return rows.isEmpty() ? new ArrayList<HoverCard.Pair>() : rows.get(0);
	}

	/** The drops: the first pair of each row after the first, the source as its label and the rate as its value. */
	static List<HoverCard.Pair> dropsOf(HoverCard card)
	{
		final List<HoverCard.Pair> drops = new ArrayList<>();
		final List<List<HoverCard.Pair>> rows = card.rows();
		for (int i = 1; i < rows.size(); i++)
		{
			if (!rows.get(i).isEmpty())
			{
				drops.add(rows.get(i).get(0));
			}
		}
		return drops;
	}

	/**
	 * The description set in lines for a card {@code width} px wide: a paragraph, or, when it holds line breaks, a
	 * numbered list.
	 *
	 * <p>With no line break in it the description is a paragraph: {@link #wrapToWidth}'s lines, at most {@code maxLines},
	 * every indent 0. With one it is a list: the items are the text between the breaks, trimmed, a blank one dropped,
	 * and item k begins "k. ". Each item's first line is its marker then its words, wrapped to {@code width}; each
	 * continuation line is wrapped to {@code width} less the indent and carries that indent, the width of the widest
	 * marker ({@code items.size() + ". "}), so a long item wraps under its own text. No full stop is added. The list
	 * takes at most {@code Math.max(maxLines, LIST_LINES)} lines, and when lines run out the last one kept ends in "..."
	 * (the marker kept), as a paragraph's does. An item wider than the line is cut as {@link #wrapToWidth} cuts a
	 * word. No text, or no item, gives no lines.
	 *
	 * <p>Choice: a list is not cut at the card's paragraph line count, so a level's unlocks show whole at any width;
	 * the wide card's two lines are for a paragraph, and a list there takes the same ten.
	 */
	static List<TextLine> layout(String description, FontMetrics metrics, int width, int maxLines)
	{
		final List<TextLine> lines = new ArrayList<>();
		if (description.indexOf('\n') < 0)
		{
			for (final String text : wrapToWidth(description, metrics, width, maxLines))
			{
				lines.add(new TextLine(text, 0));
			}
			return lines;
		}
		final List<String> items = itemsOf(description);
		final int cap = Math.max(maxLines, LIST_LINES);
		final int indent = metrics.stringWidth(items.size() + ". ");
		boolean cut = false;
		for (int k = 0; k < items.size() && !cut; k++)
		{
			final int room = cap - lines.size();
			final String full = (k + 1) + ". " + items.get(k);
			final String first = wrapToWidth(full, metrics, width, 2).get(0);
			lines.add(new TextLine(first, 0));
			final String rest = full.substring(first.length()).trim();
			final List<String> under = rest.isEmpty() ? new ArrayList<String>()
				: wrapToWidth(rest, metrics, Math.max(1, width - indent), room);
			for (int i = 0; i < under.size() && i < room - 1; i++)
			{
				lines.add(new TextLine(under.get(i), indent));
			}
			cut = under.size() > room - 1 || (lines.size() >= cap && k < items.size() - 1);
		}
		if (cut)
		{
			final TextLine last = lines.remove(lines.size() - 1);
			lines.add(new TextLine(withMore(last.text, metrics, Math.max(1, width - last.indent)), last.indent));
		}
		return lines;
	}

	/** The list's items: the description split at its line breaks, each trimmed with its spaces made single, blanks dropped. */
	private static List<String> itemsOf(String description)
	{
		final List<String> items = new ArrayList<>();
		for (final String part : description.split("\n"))
		{
			final String item = part.trim().replaceAll("\\s+", LABEL_GAP);
			if (!item.isEmpty())
			{
				items.add(item);
			}
		}
		return items;
	}

	/**
	 * The width the description asks for on one line, in px: a paragraph's whole text, or, for a list, its widest item
	 * with its marker.
	 */
	private static int descriptionNeed(String description, FontMetrics metrics)
	{
		if (description.indexOf('\n') < 0)
		{
			return metrics.stringWidth(description.trim().replaceAll("\\s+", LABEL_GAP));
		}
		final List<String> items = itemsOf(description);
		int need = 0;
		for (int k = 0; k < items.size(); k++)
		{
			need = Math.max(need, metrics.stringWidth((k + 1) + ". " + items.get(k)));
		}
		return need;
	}

	/**
	 * The text broken into lines of whole words no wider than {@code width} px in {@code metrics}, at most
	 * {@code maxLines}; a word wider than the width is broken. When words are left over the last line ends in "..." and
	 * still fits: whole words are dropped from its end until it does. An empty text, or a count under one, gives no
	 * lines. The wrapping stops as soon as it has one line more than it keeps, so a long paragraph costs no more than
	 * the lines shown.
	 */
	static List<String> wrapToWidth(String text, FontMetrics metrics, int width, int maxLines)
	{
		final List<String> lines = new ArrayList<>();
		final String trimmed = text.trim();
		if (trimmed.isEmpty() || maxLines < 1)
		{
			return lines;
		}
		String current = "";
		for (String word : trimmed.split("\\s+"))
		{
			final String joined = current.isEmpty() ? word : current + " " + word;
			if (metrics.stringWidth(joined) <= width)
			{
				current = joined;
				continue;
			}
			if (!current.isEmpty())
			{
				lines.add(current);
				current = "";
			}
			while (lines.size() <= maxLines && metrics.stringWidth(word) > width && word.length() > 1)
			{
				int cut = word.length() - 1;
				while (cut > 1 && metrics.stringWidth(word.substring(0, cut)) > width)
				{
					cut--;
				}
				lines.add(word.substring(0, cut));
				word = word.substring(cut);
			}
			if (lines.size() > maxLines)
			{
				break;
			}
			current = word;
		}
		if (lines.size() <= maxLines && !current.isEmpty())
		{
			lines.add(current);
		}
		if (lines.size() <= maxLines)
		{
			return lines;
		}
		final List<String> kept = new ArrayList<>(lines.subList(0, maxLines));
		kept.set(maxLines - 1, withMore(kept.get(maxLines - 1), metrics, width));
		return kept;
	}

	/** The line with "..." after it: whole words are dropped from its end until it fits {@code width} px, else it is cut. */
	private static String withMore(String line, FontMetrics metrics, int width)
	{
		String kept = line;
		while (metrics.stringWidth(kept + MORE) > width && kept.indexOf(' ') >= 0)
		{
			kept = kept.substring(0, kept.lastIndexOf(' '));
		}
		return fit(kept + MORE, metrics, width);
	}

	/** The text as it is when it fits in {@code width} px, else cut from its end and ending in "...". */
	static String fit(String text, FontMetrics metrics, int width)
	{
		if (metrics.stringWidth(text) <= width)
		{
			return text;
		}
		String cut = text;
		while (cut.length() > 1 && metrics.stringWidth(cut + MORE) > width)
		{
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut.trim() + MORE;
	}

	/** A row's width: each pair's label, value and note, with {@code gap} px between pairs. */
	static int rowWidth(List<HoverCard.Pair> row, FontMetrics metrics, int gap)
	{
		int width = 0;
		for (int i = 0; i < row.size(); i++)
		{
			final HoverCard.Pair pair = row.get(i);
			width += metrics.stringWidth(pair.label + LABEL_GAP) + metrics.stringWidth(pair.value)
				+ metrics.stringWidth(pair.note);
			if (i < row.size() - 1)
			{
				width += gap;
			}
		}
		return width;
	}

	/** Draws a row on {@code baseline}: each label in grey, its value in white, its note in grey, {@code gap} px between pairs. */
	private static void drawRow(Pen pen, Faces faces, List<HoverCard.Pair> row, int left, int baseline, int gap)
	{
		int x = left;
		for (final HoverCard.Pair pair : row)
		{
			final String head = pair.label + LABEL_GAP;
			pen.string(head, faces.body, pen.style.label, x, baseline);
			x += faces.bodyMetrics.stringWidth(head);
			pen.string(pair.value, faces.body, pen.style.value, x, baseline);
			x += faces.bodyMetrics.stringWidth(pair.value);
			pen.string(pair.note, faces.body, pen.style.label, x, baseline);
			x += faces.bodyMetrics.stringWidth(pair.note) + gap;
		}
	}

	/** One line of the description as {@link #layout} sets it: its text, and how far in from the left it starts, in px. */
	static final class TextLine
	{
		final String text;
		final int indent;

		TextLine(String text, int indent)
		{
			this.text = text;
			this.indent = indent;
		}
	}

	/**
	 * One word of the drops line: its text in one colour, and a tail in the label grey set against it with no space, the
	 * comma after a drop, so that a line never breaks between a rate and its comma.
	 */
	private static final class Word
	{
		final String text;
		final Color colour;
		final String tail;

		Word(String text, Color colour, String tail)
		{
			this.text = text;
			this.colour = colour;
			this.tail = tail;
		}

		int width(FontMetrics metrics)
		{
			return metrics.stringWidth(text) + metrics.stringWidth(tail);
		}
	}

	/** The three fonts at one size, with their metrics and their line heights: ascent and descent, and 2. */
	private static final class Faces
	{
		final Font title;
		final Font body;
		final Font hint;
		final FontMetrics titleMetrics;
		final FontMetrics bodyMetrics;
		final FontMetrics hintMetrics;
		final int titleHeight;
		final int bodyHeight;
		final int hintHeight;

		Faces(Graphics2D g, int fontSize)
		{
			title = FontManager.getRunescapeBoldFont().deriveFont((float) fontSize);
			body = FontManager.getRunescapeFont().deriveFont((float) fontSize);
			hint = FontManager.getRunescapeSmallFont().deriveFont((float) fontSize);
			titleMetrics = g.getFontMetrics(title);
			bodyMetrics = g.getFontMetrics(body);
			hintMetrics = g.getFontMetrics(hint);
			titleHeight = lineHeight(titleMetrics);
			bodyHeight = lineHeight(bodyMetrics);
			hintHeight = lineHeight(hintMetrics);
		}

		private static int lineHeight(FontMetrics metrics)
		{
			return metrics.getAscent() + metrics.getDescent() + 2;
		}
	}

	/**
	 * What a layout draws with: draws when it is on, and when it is off only lets the layout learn its height, so one
	 * layout serves both the measuring and the painting.
	 */
	private static final class Pen
	{
		/** The offsets of the eight copies a title's halo is drawn at, in px. */
		private static final int[][] AROUND = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
		/** The offsets of the five copies a title's outline is drawn at, in px: left, right, up, down and down-right. */
		private static final int[][] OUTLINE = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {1, 1}};
		/** How far to the left and to the right of the title its two split copies are drawn, in px. */
		private static final int SPLIT = 2;

		private final Graphics2D g;
		private final boolean draw;
		/** The colours and the shadow this pen draws in. */
		final CardStyle style;
		/** The card's left edge and its width, in px, which a style's gradient runs across. */
		private final int cardLeft;
		private final int cardWidth;
		/**
		 * For a picture theme, what is drawn waits here until {@link #finish} has put the picture down, and the boxes of the
		 * strings (and the title's) are kept for the picture's treatment; null for every other style.
		 */
		private final List<Runnable> queue;
		private final List<int[]> boxes = new ArrayList<>();
		private int[] titleBox;
		private boolean inTitle;

		Pen(Graphics2D g, boolean draw, CardStyle style)
		{
			this(g, draw, style, 0, 1);
		}

		Pen(Graphics2D g, boolean draw, CardStyle style, int cardLeft, int cardWidth)
		{
			this.g = g;
			this.draw = draw;
			this.style = style;
			this.cardLeft = cardLeft;
			this.cardWidth = cardWidth;
			this.queue = draw && style.picture != null ? new ArrayList<Runnable>() : null;
		}

		/** Runs {@code step} now, or keeps it for {@link #finish} when this pen draws a picture theme; nothing when it does not draw. */
		private void later(Runnable step)
		{
			if (queue != null)
			{
				queue.add(step);
			}
			else if (draw)
			{
				step.run();
			}
		}

		/**
		 * For a picture theme, puts the card's ground down (its picture, treated under the words laid out, or the theme's
		 * first sidebar colour when the picture cannot be had), then draws everything kept over it. Nothing for any other style.
		 */
		void finish(int x, int y, int width, int height)
		{
			if (queue == null)
			{
				return;
			}
			final List<int[]> lines = new ArrayList<>();
			for (final int[] box : boxes)
			{
				lines.add(new int[] {box[0] - x, box[1] - y, box[2], box[3]});
			}
			final int[] title = titleBox == null ? null : new int[] {titleBox[0] - x, titleBox[1] - y, titleBox[2], titleBox[3]};
			final BufferedImage ground = PictureThemes.current().ground(style.picture, width, height, lines, title);
			if (ground == null)
			{
				g.setColor(style.ground);
				g.fillRect(x, y, width, height);
			}
			else
			{
				g.drawImage(ground, x, y, null);
			}
			for (final Runnable step : queue)
			{
				step.run();
			}
		}

		/**
		 * Draws the string, first a black outline round it when the style has one (the space and scene looks), else a black
		 * copy 1 px to the right and down when it asks for a shadow.
		 */
		void string(String text, Font font, Color colour, int x, int baseline)
		{
			paintString(text, font, colour, x, baseline, true);
		}

		/** Draws the string in {@code paint}; {@code edged} false leaves out the outline or the shadow. */
		private void paintString(String text, Font font, Paint paint, int x, int baseline, boolean edged)
		{
			if (queue != null && !text.trim().isEmpty())
			{
				final FontMetrics metrics = g.getFontMetrics(font);
				final int[] box = {x, baseline - metrics.getAscent(), metrics.stringWidth(text),
					metrics.getAscent() + metrics.getDescent()};
				boxes.add(box);
				titleBox = inTitle ? box : titleBox;
			}
			later(() ->
			{
				final Paint before = g.getPaint();
				g.setFont(font);
				if (edged && style.picture != null)
				{
					PictureArt.edged(g, style.picture, text, x, baseline);
				}
				else if (edged && style.outline)
				{
					g.setColor(Color.BLACK);
					g.drawString(text, x - 1, baseline);
					g.drawString(text, x + 1, baseline);
					g.drawString(text, x, baseline - 1);
					g.drawString(text, x, baseline + 1);
					g.drawString(text, x + 1, baseline + 1);
				}
				else if (edged && style.shadow)
				{
					g.setColor(Color.BLACK);
					g.drawString(text, x + 1, baseline + 1);
				}
				g.setPaint(paint);
				g.drawString(text, x, baseline);
				g.setPaint(before);
			});
		}

		/**
		 * Draws the card's title in the style's title colour, or in its gradient across the card when it has one. Behind it,
		 * in this order, the style's halo, its outline and its split, each when it has one.
		 */
		void title(String text, Font font, int x, int baseline)
		{
			if (draw && style.titleGlow != null)
			{
				g.setFont(font);
				g.setColor(style.titleGlow);
				for (final int[] offset : AROUND)
				{
					g.drawString(text, x + offset[0], baseline + offset[1]);
				}
			}
			if (draw && style.titleOutline != null)
			{
				g.setFont(font);
				g.setColor(style.titleOutline);
				for (final int[] offset : OUTLINE)
				{
					g.drawString(text, x + offset[0], baseline + offset[1]);
				}
			}
			if (draw && style.titleSplit != null)
			{
				g.setFont(font);
				g.setColor(style.titleSplit[0]);
				g.drawString(text, x - SPLIT, baseline);
				g.setColor(style.titleSplit[1]);
				g.drawString(text, x + SPLIT, baseline);
			}
			if (draw)
			{
				// A title with an outline of its own (Steampunk's brown, Synthwave's plum) takes no black one over it.
				inTitle = true;
				paintString(text, font, style.titleGradient == null ? style.title : across(), x, baseline,
					style.titleOutline == null);
				inTitle = false;
			}
		}

		/**
		 * Draws a progress bar whose top-left corner is ({@code x}, {@code y}): a track {@code width} by {@code height} px in
		 * the style's rule colour, and over its left {@code filled} px the fill in the style's value colour.
		 */
		void bar(int x, int y, int width, int height, int filled)
		{
			later(() ->
			{
				final Paint before = g.getPaint();
				g.setColor(style.rule);
				g.fillRect(x, y, width, height);
				g.setColor(style.value);
				g.fillRect(x, y, filled, height);
				g.setPaint(before);
			});
		}

		/**
		 * Draws a rule in the style's rule colour, or in its gradient across the card when its rules take it, and the style's
		 * light rule on the pixel row under it when it has one.
		 */
		void rule(int x, int y, int width, int height)
		{
			later(() ->
			{
				final Paint before = g.getPaint();
				g.setPaint(style.gradientRules ? across() : style.rule);
				g.fillRect(x, y, width, height);
				if (style.ruleLight != null)
				{
					g.setColor(style.ruleLight);
					g.fillRect(x, y + height, width, 1);
				}
				g.setPaint(before);
			});
		}

		/** The style's gradient colours, left to right across the card, evenly spaced. */
		private Paint across()
		{
			final Color[] colours = style.titleGradient;
			final float[] fractions = new float[colours.length];
			for (int i = 0; i < colours.length; i++)
			{
				fractions[i] = i / (float) (colours.length - 1);
			}
			return new LinearGradientPaint(cardLeft, 0f, cardLeft + Math.max(2, cardWidth), 0f, fractions, colours);
		}

		void picture(BufferedImage image, int x, int y)
		{
			if (image != null)
			{
				later(() -> g.drawImage(image, x, y, null));
			}
		}

		/**
		 * Draws the picture centred, both ways, in the square box of {@code box} px whose top-left corner is ({@code x},
		 * {@code y}); a picture wider or taller than the box is drawn scaled down to fit it, its ratio kept (a guard: the
		 * overlay hands pictures already fitted).
		 */
		void pictureInBox(BufferedImage image, int x, int y, int box)
		{
			if (image == null)
			{
				return;
			}
			final int width = image.getWidth();
			final int height = image.getHeight();
			if (width <= box && height <= box)
			{
				later(() -> g.drawImage(image, x + (box - width) / 2, y + (box - height) / 2, null));
				return;
			}
			final double scale = Math.min(box / (double) width, box / (double) height);
			final int fittedWidth = Math.max(1, (int) Math.round(width * scale));
			final int fittedHeight = Math.max(1, (int) Math.round(height * scale));
			later(() -> g.drawImage(image, x + (box - fittedWidth) / 2, y + (box - fittedHeight) / 2, fittedWidth, fittedHeight, null));
		}
	}
}
