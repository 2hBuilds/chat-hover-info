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
		final Dimension size = CardPainter.paintSplit(g, card, redSprite(), fontSize, 0, 0, description);
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
	public void theGroundIsSolid()
	{
		assertEquals("the ground's alpha is full", 255, CardPainter.GROUND.getAlpha());

		final BufferedImage image = new BufferedImage(520, 300, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		CardPainter.paintSplit(g, claws(), redSprite(), 16, 0, 0, NONE);
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

	@Test
	public void theFactsDropUnderTheTitleWhenTheyDoNotFit()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final Object[] quest = splitPainted(dragonSlayerTwo(), 16, NONE);
		final Dimension questSize = (Dimension) quest[1];
		final Object[] bare = splitPainted(claws(), 16, NONE);
		final Dimension clawsSize = (Dimension) bare[1];
		assertEquals("a long title with four facts is as wide as the card goes", 480, questSize.width);
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

	@Test
	public void aShortCardIsNarrow()
	{
		final int bodyHeight = bodyMetrics().getAscent() + bodyMetrics().getDescent() + 2;
		final String sentence = "Catch stymphikes (with The Blood Moon Rises completed).";
		final InfoCard hunter = new InfoCard("Hunter 82",
			Collections.singletonList(new HoverCard.Pair("New", "1 unlock", "")), Collections.<String>emptyList(), "Hunter",
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

	/** Whether the pixel is exactly {@code colour}, its alpha ignored. */
	private static boolean isColour(BufferedImage image, int x, int y, Color colour)
	{
		return (image.getRGB(x, y) & 0xFFFFFF) == (colour.getRGB() & 0xFFFFFF);
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
