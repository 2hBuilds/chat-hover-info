package com.clanhub;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.ui.FontManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link CardPainter}: where the two looks open, the split and the wide card's widths, sprite, rule and growth, and the
 * description wrapped by pixel width under the header.
 */
public class CardPainterTest
{
	private static final Dimension CANVAS = new Dimension(765, 503);
	private static final Color ACCENT = new Color(59, 130, 246);
	// Re-aimed by the lead, 2026-10-08 (lot 8i): the painter wraps the description itself.
	// Re-aimed by the lead, 2026-10-08 (lot 8Q): the cap and the line count are the caller's now.
	private static final String NONE = "";
	private static final String THREE_LINES = "The dragon claws are a rare drop from the abyssal demon.";
	private static final String PARAGRAPH = words(400);

	/** {@code count} words, "word0 word1 word2 ...". */
	private static String words(int count)
	{
		final StringBuilder text = new StringBuilder();
		for (int word = 0; word < count; word++)
		{
			text.append(word == 0 ? "" : " ").append("word").append(word);
		}
		return text.toString();
	}

	/** The body font's metrics at 16 px. */
	private static FontMetrics bodyMetrics()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final FontMetrics metrics = g.getFontMetrics(FontManager.getRunescapeFont().deriveFont(16f));
		g.dispose();
		return metrics;
	}

	/** The quest card of the problem case: a long title, four facts, no notes, no drops. */
	private static InfoCard dragonSlayerTwo()
	{
		final List<HoverCard.Pair> row = Arrays.asList(new HoverCard.Pair("Difficulty", "Grandmaster", ""),
			new HoverCard.Pair("Length", "Very Long", ""), new HoverCard.Pair("Points", "5", ""),
			new HoverCard.Pair("Series", "Dragon Slayer", ""));
		return new InfoCard("Dragon Slayer II: The Legacy of Elvarg", row, Collections.<String>emptyList(),
			"Dragon_Slayer_II", "Klar has completed a quest: Dragon Slayer II: The Legacy of Elvarg.", 33, 70, -1);
	}

	private static ItemCard claws()
	{
		return new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000,
			"Klar received a drop: Dragon claws (121,453,000 coins).", 24, 36);
	}

	/** The claws card with three drop sources, which makes it three drop rows. */
	private static ItemCard clawsWithDrops()
	{
		return new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000,
			"Klar received a drop: Dragon claws (121,453,000 coins).", 24, 36, threeSources());
	}

	/** The whip card of {@link ItemCardTest}: three drop sources, the table's order. */
	private static ItemCard whip()
	{
		return new ItemCard(4151, "Abyssal whip", 1, 1_500_000L, 72_000,
			"Klar received a drop: Abyssal whip (1,500,000 coins).", 22, 34, threeSources());
	}

	private static List<DropRates.DropSource> threeSources()
	{
		return Arrays.asList(new DropRates.DropSource("Unsired", "1/10.7", "Unsired"),
			new DropRates.DropSource("Greater abyssal demon", "1/170.7", "Greater_abyssal_demon"),
			new DropRates.DropSource("Abyssal demon", "1/512", "Abyssal_demon"));
	}

	/** A 36 x 32 stand-in for the item sprite, solid red. */
	private static BufferedImage redSprite()
	{
		final BufferedImage sprite = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = sprite.createGraphics();
		g.setColor(Color.RED);
		g.fillRect(0, 0, 36, 32);
		g.dispose();
		return sprite;
	}

	/** The split card at the top-left of a black image at {@code fontSize}; the image and the size it reports. */
	private static Object[] splitPainted(HoverCard card, int fontSize, String description)
	{
		final BufferedImage image = new BufferedImage(520, 300, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final Dimension size = CardPainter.paintSplit(g, card, redSprite(), fontSize, 0, 0, description, 480, 3);
		g.dispose();
		return new Object[]{image, size};
	}

	/** The size of a split card at 16 px. */
	private static Dimension splitSize(HoverCard card, String description)
	{
		return (Dimension) splitPainted(card, 16, description)[1];
	}

	/** The size of a wide card {@code width} wide at 16 px. */
	private static Dimension wideSize(HoverCard card, int width, String description)
	{
		final BufferedImage image = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final Dimension size = CardPainter.paintWide(g, card, redSprite(), 16, width, 0, 0, description, ACCENT);
		g.dispose();
		return size;
	}

	@Test
	public void theSplitCardIsCentredAboveTheCursor()
	{
		final Dimension card = new Dimension(360, 150);
		assertEquals("centred on the cursor, 6 px above it", new Rectangle(120, 244, 360, 150),
			CardPainter.placeCentred(new Point(300, 400), card, CANVAS));
		assertEquals("at the left edge it starts on the canvas's edge", new Rectangle(0, 244, 360, 150),
			CardPainter.placeCentred(new Point(100, 400), card, CANVAS));
		assertEquals("at the right edge it ends on the canvas's edge", new Rectangle(405, 244, 360, 150),
			CardPainter.placeCentred(new Point(700, 400), card, CANVAS));
		assertEquals("at the top it opens below the cursor", new Rectangle(120, 118, 360, 150),
			CardPainter.placeCentred(new Point(300, 100), card, CANVAS));

		final Rectangle chat = new Rectangle(0, 340, 519, 142);
		assertEquals("the wide card's bottom edge is on the chatbox's top edge", new Rectangle(0, 227, 519, 113),
			CardPainter.placeAboveChat(chat, new Dimension(519, 113)));
		assertEquals("a card taller than the room above the chatbox is held at the top", new Rectangle(0, 0, 519, 400),
			CardPainter.placeAboveChat(chat, new Dimension(519, 400)));
	}

	@Test
	public void theHeaderCardHasItsWidthsAndGrows()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final Object[] sixteen = splitPainted(claws(), 16, PARAGRAPH);
		final BufferedImage image = (BufferedImage) sixteen[0];
		final Dimension size = (Dimension) sixteen[1];
		// Re-aimed by the lead, 2026-10-08 (lot 8O): the header across the top, the paragraph under.
		assertEquals("480 wide at 16 px with a paragraph", 480, size.width);
		assertEquals("600 wide at 20 px", 600, ((Dimension) splitPainted(claws(), 20, PARAGRAPH)[1]).width);
		assertEquals("240 wide at 8 px", 240, ((Dimension) splitPainted(claws(), 8, PARAGRAPH)[1]).width);
		final Dimension bare = splitSize(claws(), NONE);
		assertTrue("with no paragraph it is narrower than 480: " + bare.width, bare.width < 480);
		assertTrue("and at least 18 px wider than its hint: " + bare.width + " vs " + hintWidth(claws()),
			bare.width >= hintWidth(claws()) + 18);

		// The ground is solid, rgb(70, 61, 50), nothing of the black shows through.
		final Color ground = new Color(image.getRGB(3, 3));
		assertEquals(70, ground.getRed());
		assertEquals(61, ground.getGreen());
		assertEquals(50, ground.getBlue());

		// The sprite's box starts 9 px in: the black edge, the line inside it and the 7 px padding.
		final Color sprite = new Color(image.getRGB(9, 9));
		assertTrue("the sprite's top-left pixel is red: " + sprite, sprite.getRed() > 200 && sprite.getGreen() < 40
			&& sprite.getBlue() < 40);
		final Color before = new Color(image.getRGB(8, 8));
		assertTrue("and the pixel before it is the ground: " + before, before.getRed() < 100);

		// There are no columns: no 1 px column of the rule colour stands at x 120 through the content (the paragraph's rule
		// and the hint's cross it, a pixel each).
		int rule = 0;
		for (int y = 5; y < size.height - 6; y++)
		{
			if (image.getRGB(120, y) == (CardPainter.LINE.getRGB() | 0xFF000000))
			{
				rule++;
			}
		}
		assertTrue("no column rule at x 120 (" + rule + " px) in a card " + size.height + " tall", rule < 10);

		// Growth: a paragraph takes up to three lines and its rule; the drops take one line however many sources there are.
		assertEquals("three paragraph lines and the rule", 3 * bodyHeight + 5,
			splitSize(claws(), PARAGRAPH).height - bare.height);
		assertEquals("one paragraph line and the rule", bodyHeight + 5, splitSize(claws(), "a b c").height - bare.height);
		assertEquals("the drops are one line", bodyHeight, splitSize(whip(), NONE).height - bare.height);
		assertTrue("six sources are never shorter than three: " + splitSize(whipWith(6), NONE).height + " vs "
			+ splitSize(whip(), NONE).height, splitSize(whipWith(6), NONE).height >= splitSize(whip(), NONE).height);
	}

	/** The width of the card's hint in the hint font at 16 px. */
	private static int hintWidth(HoverCard card)
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final int width = g.getFontMetrics(FontManager.getRunescapeSmallFont().deriveFont(16f)).stringWidth(card.hint());
		g.dispose();
		return width;
	}

	@Test
	public void theNarrowerCapsGiveMoreLines()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final Dimension wide = splitSized(claws(), PARAGRAPH, 480, 3);
		final Dimension medium = splitSized(claws(), PARAGRAPH, 409, 5);
		final Dimension narrow = splitSized(claws(), PARAGRAPH, 391, 5);

		assertEquals("480 wide at the wide cap", 480, wide.width);
		assertEquals("409 wide at the medium cap", 409, medium.width);
		assertEquals("391 wide at the narrow cap", 391, narrow.width);
		assertEquals("two more paragraph lines at the medium cap", 2 * bodyHeight, medium.height - wide.height);
		assertEquals("and the narrow cap gives five lines too", medium.height, narrow.height);

		final int bare = splitSized(claws(), NONE, 480, 3).height;
		assertEquals("with no paragraph the medium cap gives the same height", bare, splitSized(claws(), NONE, 409, 5).height);
		assertEquals("and the narrow cap too", bare, splitSized(claws(), NONE, 391, 5).height);

		// Re-aimed by the lead, 2026-10-08 (lot 8T): the facts wrap, so a card with no paragraph is as wide as its widest fact asks.
		assertEquals("a long title with no paragraph is as wide as its title asks at the medium cap: the picture, the "
			+ "title (247 px) and the padding", 309, splitSized(dragonSlayerTwo(), NONE, 409, 5).width);
	}

	/** The size of a split card at 16 px under the cap {@code capAt16} and the line count {@code lines}. */
	private static Dimension splitSized(HoverCard card, String description, int capAt16, int lines)
	{
		final BufferedImage image = new BufferedImage(520, 300, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final Dimension size = CardPainter.paintSplit(g, card, redSprite(), 16, 0, 0, description, capAt16, lines);
		g.dispose();
		return size;
	}

	@Test
	public void theGroundIsSolid()
	{
		assertEquals("the ground's alpha is full", 255, CardPainter.GROUND.getAlpha());

		final BufferedImage image = new BufferedImage(520, 300, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		CardPainter.paintSplit(g, claws(), redSprite(), 16, 0, 0, NONE, 480, 3);
		g.dispose();
		// (4, 4) is inside the black edge and the line inside it, and before any text.
		assertEquals("a pixel of the ground on a clear canvas is opaque", 255, image.getRGB(4, 4) >>> 24);
	}

	@Test
	public void theWideCardIsAsWideAsAsked()
	{
		final Dimension wide = wideSize(claws(), 519, NONE);
		assertEquals("as wide as asked", 519, wide.width);
		assertTrue("a height above 60: " + wide.height, wide.height > 60);

		final Dimension narrow = wideSize(claws(), 300, NONE);
		assertEquals("as wide as asked, narrow", 300, narrow.width);
		assertTrue("the facts wrap under the title, so it is taller than at 519: " + narrow.height + " vs " + wide.height,
			narrow.height > wide.height);

		assertTrue("with no drop rows it is shorter than with three: " + wide.height + " vs "
			+ wideSize(clawsWithDrops(), 519, NONE).height, wide.height < wideSize(clawsWithDrops(), 519, NONE).height);
		assertTrue("the description lines add height: " + wideSize(claws(), 519, THREE_LINES).height,
			wideSize(claws(), 519, THREE_LINES).height > wide.height);
	}

	@Test
	public void wrapToWidthFillsLinesAndEndsWithTheMark()
	{
		final FontMetrics metrics = bodyMetrics();
		final String sixty = words(60);

		final List<String> five = CardPainter.wrapToWidth(sixty, metrics, 220, 5);
		assertEquals("five lines", 5, five.size());
		for (final String line : five)
		{
			assertTrue("no line wider than 220 px: " + line, metrics.stringWidth(line) <= 220);
		}
		assertTrue("the last ends in the mark: " + five.get(4), five.get(4).endsWith("..."));

		final List<String> many = CardPainter.wrapToWidth(sixty, metrics, 220, 50);
		assertTrue("fewer than fifty lines: " + many.size(), many.size() < 50);
		for (final String line : many)
		{
			assertTrue("no line wider than 220 px: " + line, metrics.stringWidth(line) <= 220);
			assertTrue("none ends in the mark: " + line, !line.endsWith("..."));
		}

		assertEquals("a short text is one line", 1, CardPainter.wrapToWidth("Wear Barrows armour", metrics, 220, 3).size());
		assertTrue("no text, no lines", CardPainter.wrapToWidth("", metrics, 220, 3).isEmpty());
		final List<String> longWord = CardPainter.wrapToWidth("abcdefghijklmnopqrstuvwxyzabcdefghijklmn", metrics, 220, 1);
		assertEquals("a word wider than the line is cut to one line", 1, longWord.size());
		assertTrue("and it fits: " + longWord.get(0), metrics.stringWidth(longWord.get(0)) <= 220);
	}

	/** The texts of the laid-out lines, in order. */
	private static List<String> textsOf(List<CardPainter.TextLine> lines)
	{
		final List<String> texts = new ArrayList<>();
		for (final CardPainter.TextLine line : lines)
		{
			texts.add(line.text);
		}
		return texts;
	}

	// Amended by the lead, 2026-10-08 (lot 8S): a list has its own cap of ten lines, not the card's paragraph count.
	@Test
	public void aListIsNumberedOnePerLineWithAHangingIndent()
	{
		final FontMetrics metrics = bodyMetrics();

		final List<CardPainter.TextLine> hitpoints = CardPainter.layout("Skill mastery\nBuy the hitpoints cape and hood from "
			+ "Surgeon General Tafani in the Emir's Arena hospital, and wear them", metrics, 391, 5);
		final int indent = metrics.stringWidth("2. ");
		assertEquals("the first item", "1. Skill mastery", hitpoints.get(0).text);
		assertEquals("at no indent", 0, hitpoints.get(0).indent);
		assertTrue("the second item begins with its marker: " + hitpoints.get(1).text,
			hitpoints.get(1).text.startsWith("2. Buy"));
		assertEquals("at no indent", 0, hitpoints.get(1).indent);
		for (int i = 2; i < hitpoints.size(); i++)
		{
			assertEquals("a continuation hangs as wide as the marker", indent, hitpoints.get(i).indent);
			assertTrue("and does not start with a digit: " + hitpoints.get(i).text,
				!Character.isDigit(hitpoints.get(i).text.charAt(0)));
		}
		for (final CardPainter.TextLine line : hitpoints)
		{
			assertTrue("no line wider than the room it has: " + line.text,
				metrics.stringWidth(line.text) <= 391 - line.indent);
			assertTrue("none ends in the mark: " + line.text, !line.text.endsWith("..."));
		}
		assertTrue("three to five lines: " + hitpoints.size(), hitpoints.size() >= 3 && hitpoints.size() <= 5);

		// The five unlocks of Defence 70 come whole under a ten-line cap, though the card's paragraph count is five.
		final List<CardPainter.TextLine> defence = CardPainter.layout(
			"Use Piety (with 70 Prayer and completing the Knight Waves Training Grounds)\n"
				+ "Use Rigour (with 74 Prayer and unlocked using a dexterous prayer scroll)\n"
				+ "Use Augury (with 77 Prayer and unlocked using an arcane prayer scroll)\n"
				+ "Wear Neitiznot faceguard (with Fremennik Exiles completed)\n"
				+ "Wear Barrows armour", metrics, 391, 5);
		assertTrue("six to ten lines: " + defence.size(), defence.size() >= 6 && defence.size() <= 10);
		assertTrue("the first line is the first item cut at a word: " + defence.get(0).text,
			defence.get(0).text.startsWith("1. Use Piety")
				&& "1. Use Piety (with 70 Prayer and completing the Knight Waves Training Grounds)"
					.startsWith(defence.get(0).text));
		final int fiveIndent = metrics.stringWidth("5. ");
		for (final CardPainter.TextLine line : defence)
		{
			assertTrue("none ends in the mark: " + line.text, !line.text.endsWith("..."));
			assertTrue("no line wider than the room it has: " + line.text,
				metrics.stringWidth(line.text) <= 391 - line.indent);
			assertTrue("a line at no indent begins with a marker, one at the hang does not: " + line.text,
				(line.indent == 0) == Character.isDigit(line.text.charAt(0)));
		}
		for (int number = 1; number <= 5; number++)
		{
			boolean found = false;
			for (final CardPainter.TextLine line : defence)
			{
				found |= line.text.startsWith(number + ". ");
			}
			assertTrue("item " + number + " is there", found);
		}
		final CardPainter.TextLine last = defence.get(defence.size() - 1);
		assertTrue("the last line is item five, or its continuation: " + last.text,
			("5. Wear Barrows armour".equals(last.text) && last.indent == 0)
				|| (last.indent == fiveIndent && "5. Wear Barrows armour".endsWith(last.text)));

		// Eleven items: ten lines, the tenth cut with the mark.
		final StringBuilder eleven = new StringBuilder();
		final String[] numbers = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven"};
		for (int i = 0; i < numbers.length; i++)
		{
			eleven.append(i == 0 ? "" : "\n").append("Item ").append(numbers[i]);
		}
		final List<CardPainter.TextLine> many = CardPainter.layout(eleven.toString(), metrics, 391, 5);
		assertEquals("ten lines", 10, many.size());
		assertTrue("the last ends in the mark: " + many.get(9).text, many.get(9).text.endsWith("..."));
		assertTrue("and keeps its marker: " + many.get(9).text, many.get(9).text.startsWith("10. "));
		assertTrue("the ninth does not: " + many.get(8).text, !many.get(8).text.endsWith("..."));

		// No line break: the paragraph, wrapToWidth's lines, no indent.
		final String paragraph = words(60);
		final List<CardPainter.TextLine> flat = CardPainter.layout(paragraph, metrics, 220, 5);
		assertEquals("the paragraph's lines", CardPainter.wrapToWidth(paragraph, metrics, 220, 5), textsOf(flat));
		for (final CardPainter.TextLine line : flat)
		{
			assertEquals("at no indent", 0, line.indent);
		}
		assertTrue("no text, no lines", CardPainter.layout("", metrics, 391, 5).isEmpty());
	}

	@Test
	public void theFactsDropUnderTheTitleWhenTheyDoNotFit()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final Object[] quest = splitPainted(dragonSlayerTwo(), 16, NONE);
		final Dimension questSize = (Dimension) quest[1];
		final Object[] bare = splitPainted(claws(), 16, NONE);
		final Dimension clawsSize = (Dimension) bare[1];
		// Re-aimed by the lead, 2026-10-08 (lot 8T): the facts wrap, so the card no longer needs the cap for them.
		assertTrue("a long title with four facts is as wide as its widest fact asks, at most the cap: " + questSize.width,
			questSize.width <= 480);
		assertTrue("and taller than the claws card: " + questSize.height + " vs " + clawsSize.height,
			questSize.height > clawsSize.height);

		// The claws card's facts sit on the title's row: under it there is nothing but the hint's rule and the hint.
		final BufferedImage clawsImage = (BufferedImage) bare[0];
		for (int y = CardPainter.INSET + 32; y < clawsSize.height - 20; y++)
		{
			for (int x = 0; x < clawsSize.width; x++)
			{
				assertTrue("nothing in the label grey under the claws card's header, at " + x + ", " + y,
					!isColour(clawsImage, x, y, CardPainter.LABEL));
			}
		}

		// The quest card's facts took the row under the title: their labels are grey right of the picture.
		final BufferedImage questImage = (BufferedImage) quest[0];
		boolean grey = false;
		for (int y = CardPainter.INSET + 18; y < CardPainter.INSET + 36 + bodyHeight; y++)
		{
			for (int x = 45; x < questSize.width; x++)
			{
				grey |= isColour(questImage, x, y, CardPainter.LABEL);
			}
		}
		assertTrue("the facts' labels stand under the title", grey);
	}

	/** The split card at the top-left of a black image under the cap {@code capAt16} and {@code lines}: the image and its size. */
	private static Object[] splitPaintedUnder(HoverCard card, String description, int capAt16, int lines)
	{
		final BufferedImage image = new BufferedImage(520, 300, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final Dimension size = CardPainter.paintSplit(g, card, redSprite(), 16, 0, 0, description, capAt16, lines);
		g.dispose();
		return new Object[]{image, size};
	}

	@Test
	public void theFactsWrapUnderTheTitleWhenTooWide()
	{
		final FontMetrics body = bodyMetrics();
		final int bodyHeight = body.getAscent() + body.getDescent() + 2;
		final List<HoverCard.Pair> facts = dragonSlayerTwo().rows().get(0);

		// The row builder: greedy, in order, as many as fit.
		final List<List<HoverCard.Pair>> rows = CardPainter.factRows(facts, body, 380, 16);
		assertEquals("two rows at 380 px: " + rows.size(), 2, rows.size());
		assertEquals("all four facts, in order", facts, concat(rows));
		for (final List<HoverCard.Pair> row : rows)
		{
			assertTrue("a row is at most 380 px: " + CardPainter.rowWidth(row, body, 16),
				CardPainter.rowWidth(row, body, 16) <= 380);
		}
		final List<HoverCard.Pair> firstAndNext = new ArrayList<>(rows.get(0));
		firstAndNext.add(rows.get(1).get(0));
		assertTrue("the first row holds as many as fit: with the next fact it would be "
			+ CardPainter.rowWidth(firstAndNext, body, 16), CardPainter.rowWidth(firstAndNext, body, 16) > 380);

		final List<List<HoverCard.Pair>> alone = CardPainter.factRows(Collections.singletonList(facts.get(0)), body, 100, 16);
		assertTrue("the fact is wider than 100 px: " + CardPainter.rowWidth(alone.get(0), body, 16),
			CardPainter.rowWidth(alone.get(0), body, 16) > 100);
		assertEquals("a fact wider than the width stands alone: one row", 1, alone.size());
		assertEquals("of one fact", 1, alone.get(0).size());
		assertEquals("a wide fact between two small ones breaks the rows round it", 3,
			CardPainter.factRows(Arrays.asList(facts.get(2), facts.get(0), facts.get(2)), body, 100, 16).size());
		assertTrue("no facts, no rows", CardPainter.factRows(new ArrayList<HoverCard.Pair>(), body, 380, 16).isEmpty());

		// Painted under the medium cap with a paragraph, which makes the card as wide as the cap: the facts wrap inside
		// it and nothing of them runs into the right padding.
		final Object[] painted = splitPaintedUnder(dragonSlayerTwo(), PARAGRAPH, 409, 5);
		final BufferedImage image = (BufferedImage) painted[0];
		final Dimension size = (Dimension) painted[1];
		assertEquals("409 wide at the medium cap", 409, size.width);
		int rule = CardPainter.INSET;
		while (rule < size.height && !isColour(image, CardPainter.INSET + 5, rule, CardPainter.LINE))
		{
			rule++;
		}
		assertTrue("the first rule is found below the header: " + rule, rule > CardPainter.INSET + 32 && rule < size.height);
		for (int y = CardPainter.INSET; y < rule; y++)
		{
			for (int x = 409 - CardPainter.INSET; x < 409; x++)
			{
				assertTrue("only the ground, the edge or the line in the right padding, at " + x + ", " + y,
					isColour(image, x, y, CardPainter.GROUND) || isColour(image, x, y, Color.BLACK)
						|| isColour(image, x, y, CardPainter.LINE));
			}
		}

		// A second row of facts makes the header one body line taller: three facts fit one row at this width, four do not.
		final InfoCard threeFacts = new InfoCard("Dragon Slayer II: The Legacy of Elvarg", facts.subList(0, 3),
			Collections.<String>emptyList(), "Dragon_Slayer_II",
			"Klar has completed a quest: Dragon Slayer II: The Legacy of Elvarg.", 33, 70, -1);
		assertEquals("the four-fact card is one body line taller than the three-fact card", bodyHeight,
			size.height - ((Dimension) splitPaintedUnder(threeFacts, PARAGRAPH, 409, 5)[1]).height);
	}

	/** The facts of every row, in order. */
	private static List<HoverCard.Pair> concat(List<List<HoverCard.Pair>> rows)
	{
		final List<HoverCard.Pair> all = new ArrayList<>();
		for (final List<HoverCard.Pair> row : rows)
		{
			all.addAll(row);
		}
		return all;
	}

	@Test
	public void aShortCardIsNarrow()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final String sentence = "Catch stymphikes (with The Blood Moon Rises completed).";
		final InfoCard hunter = new InfoCard("Hunter 82",
			Collections.singletonList(new HoverCard.Pair("New unlocks", "1", "")), Collections.<String>emptyList(), "Hunter",
			"Mossy Rock has reached Hunter level 82.", 24, 35, -1);

		final Dimension with = splitSize(hunter, sentence);
		final Dimension without = splitSize(hunter, NONE);
		assertTrue("narrower than 480: " + with.width, with.width < 480);
		assertTrue("at least 18 px wider than its hint: " + with.width + " vs " + hintWidth(hunter),
			with.width >= hintWidth(hunter) + 18);
		assertTrue("with no sentence it is no wider: " + without.width + " vs " + with.width, without.width <= with.width);
		final int grew = with.height - without.height;
		assertTrue("the sentence adds one line or two and the rule: " + grew,
			grew == bodyHeight + 5 || grew == 2 * bodyHeight + 5);
	}

	@Test
	public void theWideCardKeepsTwoLines()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final int bare = wideSize(claws(), 519, NONE).height;
		final int filled = wideSize(claws(), 519, PARAGRAPH).height;
		assertTrue("two body lines more, within 4 px: " + (filled - bare) + " vs " + 2 * bodyHeight,
			Math.abs(filled - bare - 2 * bodyHeight) <= 4);
	}

	/** The whip card with {@code count} drop sources in the table's order: the three of {@link #threeSources}, then more. */
	private static ItemCard whipWith(int count)
	{
		final List<DropRates.DropSource> sources = new ArrayList<>(threeSources());
		for (int number = sources.size(); number < count; number++)
		{
			sources.add(new DropRates.DropSource("Source " + number, "1/" + 1000 * number, "Source_" + number));
		}
		return new ItemCard(4151, "Abyssal whip", 1, 1_500_000L, 72_000,
			"Klar received a drop: Abyssal whip (1,500,000 coins).", 22, 34, sources);
	}

	/** The whip card with the first {@code count} of six sources, "Source one" ("1/10") to "Source six" ("1/60"). */
	private static ItemCard whipWithNamed(int count)
	{
		final String[] numbers = {"one", "two", "three", "four", "five", "six"};
		final List<DropRates.DropSource> sources = new ArrayList<>();
		for (int i = 0; i < count; i++)
		{
			sources.add(new DropRates.DropSource("Source " + numbers[i], "1/" + 10 * (i + 1), "Source_" + numbers[i]));
		}
		return new ItemCard(4151, "Abyssal whip", 1, 1_500_000L, 72_000,
			"Klar received a drop: Abyssal whip (1,500,000 coins).", 22, 34, sources);
	}

	@Test
	public void sixDropsFlowOntoMoreLines()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final ItemCard three = whipWithNamed(3);
		final ItemCard six = whipWithNamed(6);
		assertEquals("the six-source card has the price row and six drop rows", 7, six.rows().size());

		final Dimension threeSize = splitSized(three, NONE, 409, 5);
		final Dimension sixSize = splitSized(six, NONE, 409, 5);
		assertTrue("six sources take at least one body line more than three: " + sixSize.height + " vs " + threeSize.height,
			sixSize.height >= threeSize.height + bodyHeight);
	}

	/** Whether the pixel is exactly {@code colour}, its alpha ignored. */
	private static boolean isColour(BufferedImage image, int x, int y, Color colour)
	{
		return (image.getRGB(x, y) & 0xFFFFFF) == (colour.getRGB() & 0xFFFFFF);
	}

	/** Where the cards of {@link #aWikiPictureCardHasTheBigBox} are painted: their top-left corner. */
	private static final int LEFT = 10;
	private static final int TOP = 7;
	private static final String ZILYANA_LINE = "Klar received a drop: Dragon claws (121,453,000 coins) from Commander Zilyana.";

	/** The Commander Zilyana card: two facts, no notes; a wiki picture card, or the plain 8-argument card. */
	private static InfoCard zilyana(boolean wikiPicture)
	{
		final List<HoverCard.Pair> facts = Arrays.asList(new HoverCard.Pair("Combat", "596", ""),
			new HoverCard.Pair("Hitpoints", "255", ""));
		final int start = ZILYANA_LINE.indexOf("Commander Zilyana");
		final int end = start + "Commander Zilyana".length();
		return wikiPicture
			? new InfoCard("Commander Zilyana", facts, Collections.<String>emptyList(), "Commander_Zilyana", ZILYANA_LINE,
				start, end, -1, -1, false, true)
			: new InfoCard("Commander Zilyana", facts, Collections.<String>emptyList(), "Commander_Zilyana", ZILYANA_LINE,
				start, end, -1);
	}

	/** A solid red picture of {@code width} x {@code height}. */
	private static BufferedImage redPicture(int width, int height)
	{
		final BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = picture.createGraphics();
		g.setColor(Color.RED);
		g.fillRect(0, 0, width, height);
		g.dispose();
		return picture;
	}

	/** The split card at ({@link #LEFT}, {@link #TOP}) of a black image at 16 px, no description: the image and the size. */
	private static Object[] paintedWith(HoverCard card, BufferedImage picture)
	{
		final BufferedImage image = new BufferedImage(520, 300, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final Dimension size = CardPainter.paintSplit(g, card, picture, 16, LEFT, TOP, NONE, 480, 3);
		g.dispose();
		return new Object[]{image, size};
	}

	/** The bounding box of the image's pure red pixels, or null when it has none. */
	private static Rectangle redBox(BufferedImage image)
	{
		int left = Integer.MAX_VALUE;
		int top = Integer.MAX_VALUE;
		int right = -1;
		int bottom = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if (isColour(image, x, y, Color.RED))
				{
					left = Math.min(left, x);
					top = Math.min(top, y);
					right = Math.max(right, x);
					bottom = Math.max(bottom, y);
				}
			}
		}
		return right < 0 ? null : new Rectangle(left, top, right - left + 1, bottom - top + 1);
	}

	/** The x of the leftmost pure white pixel in the rows {@code fromY} up to {@code toY}, or -1 when there is none. */
	private static int leftmostWhite(BufferedImage image, int fromY, int toY)
	{
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = fromY; y < toY; y++)
			{
				if (isColour(image, x, y, Color.WHITE))
				{
					return x;
				}
			}
		}
		return -1;
	}

	@Test
	public void aWikiPictureCardHasTheBigBox()
	{
		final Object[] wikiPainted = paintedWith(zilyana(true), redPicture(64, 31));
		final Object[] plainPainted = paintedWith(zilyana(false), redPicture(36, 32));
		final BufferedImage wikiImage = (BufferedImage) wikiPainted[0];
		final BufferedImage plainImage = (BufferedImage) plainPainted[0];
		final Dimension wikiSize = (Dimension) wikiPainted[1];
		final Dimension plainSize = (Dimension) plainPainted[1];

		assertEquals("the 64 box against the 32: exactly 32 px taller", 32, wikiSize.height - plainSize.height);
		final int plainWhite = leftmostWhite(plainImage, TOP + CardPainter.INSET, TOP + CardPainter.INSET + 32);
		final int wikiWhite = leftmostWhite(wikiImage, TOP + CardPainter.INSET, TOP + CardPainter.INSET + 64);
		assertTrue("the plain card has a white pixel in its header: " + plainWhite, plainWhite > 0);
		assertEquals("the text starts 28 px further right: the 64 box and its gap against the 36 and its gap", 28,
			wikiWhite - plainWhite);

		// A 64 x 31 picture is centred in the box: 9 is the inset, 25 is 9 + (64 - 31) / 2.
		final Rectangle flat = redBox(wikiImage);
		assertEquals("the flat picture's left", LEFT + 9, flat.x);
		assertEquals("its right", LEFT + 72, flat.x + flat.width - 1);
		assertEquals("its top", TOP + 25, flat.y);
		assertEquals("its bottom", TOP + 55, flat.y + flat.height - 1);

		// A 21 x 64 picture: 30 is 9 + (64 - 21) / 2.
		final Rectangle tall = redBox((BufferedImage) paintedWith(zilyana(true), redPicture(21, 64))[0]);
		assertEquals("the tall picture's left", LEFT + 30, tall.x);
		assertEquals("its right", LEFT + 50, tall.x + tall.width - 1);
		assertEquals("its top", TOP + 9, tall.y);
		assertEquals("its bottom", TOP + 72, tall.y + tall.height - 1);

		// No picture: the same size, nothing red.
		final Object[] none = paintedWith(zilyana(true), null);
		assertEquals("the same size with no picture", wikiSize, none[1]);
		assertTrue("and nothing red", redBox((BufferedImage) none[0]) == null);

		// The plain card draws its 36 x 32 picture from its corner, as before.
		final Rectangle sprite = redBox(plainImage);
		assertEquals("the sprite's left", LEFT + 9, sprite.x);
		assertEquals("its right", LEFT + 44, sprite.x + sprite.width - 1);
		assertEquals("its top", TOP + 9, sprite.y);
		assertEquals("its bottom", TOP + 40, sprite.y + sprite.height - 1);

		// A picture bigger than the box is drawn scaled down to fit it (a guard: the overlay hands fitted pictures).
		final Rectangle big = redBox((BufferedImage) paintedWith(zilyana(true), redPicture(128, 64))[0]);
		assertTrue("a 128 x 64 picture fits the 64 box: " + big, big.width <= 64 && big.height <= 64 && big.width >= 62);
	}

	@Test
	public void dropsAreGreyWithWhiteRatesAndTheParagraphWhite()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final Object[] painted = splitPainted(whip(), 16, PARAGRAPH);
		final BufferedImage image = (BufferedImage) painted[0];
		final Dimension size = (Dimension) painted[1];

		// Re-aimed by the lead, 2026-10-08 (lot 8O): the header across the top, the paragraph under.
		// Down the card: the header's 32 px, the drops line (grey sources, white rates), the rule (5 px with its air) and the
		// paragraph.
		final int dropsTop = CardPainter.INSET + 32;
		boolean grey = false;
		boolean rate = false;
		for (int y = dropsTop; y < dropsTop + bodyHeight; y++)
		{
			for (int x = CardPainter.INSET; x < size.width - CardPainter.INSET; x++)
			{
				grey |= isColour(image, x, y, CardPainter.LABEL);
				rate |= isColour(image, x, y, Color.WHITE);
			}
		}
		assertTrue("the drops line has the label grey", grey);
		assertTrue("and the rates in pure white", rate);

		boolean white = false;
		for (int y = dropsTop + bodyHeight + 5; y < size.height - 25; y++)
		{
			for (int x = CardPainter.INSET; x < size.width - CardPainter.INSET; x++)
			{
				white |= isColour(image, x, y, Color.WHITE);
			}
		}
		assertTrue("the paragraph is pure white", white);

		// The wide card's rates lose the accent.
		final BufferedImage wide = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = wide.createGraphics();
		final Dimension wideCard = CardPainter.paintWide(g, whip(), redSprite(), 16, 519, 0, 0, PARAGRAPH, ACCENT);
		g.dispose();
		for (int y = 0; y < wideCard.height; y++)
		{
			for (int x = 0; x < wideCard.width; x++)
			{
				assertTrue("no accent blue in the wide card, at " + x + ", " + y, !isColour(wide, x, y, ACCENT));
			}
		}

		// The drops line is one line of words, so more sources never make the split card shorter, and the wide card likewise.
		assertTrue("six sources are never shorter than four with a paragraph", splitSize(whipWith(6), PARAGRAPH).height
			>= splitSize(whipWith(4), PARAGRAPH).height);
		assertTrue("and in the wide card it is never shorter", wideSize(whipWith(6), 519, PARAGRAPH).height
			>= wideSize(whipWith(4), 519, PARAGRAPH).height);
	}
}
