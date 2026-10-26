package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link WikiCard}: the Wiki theme's infobox, as {@link CardPainter} hands it a card. */
public class WikiCardTest
{
	private static final Color EDGE = new Color(150, 135, 105);
	private static final Color BAR = new Color(194, 172, 136);
	private static final Color LABEL_CELL = new Color(214, 202, 178);
	private static final Color VALUE_CELL = new Color(244, 238, 226);
	private static final Color TITLE = new Color(38, 28, 16);
	private static final Color LINK = new Color(42, 92, 172);
	private static final Color ACCENT = new Color(59, 130, 246);

	/** The info card of the tests: a short title, two facts, no notes. */
	private static InfoCard twoFacts()
	{
		final List<HoverCard.Pair> row = Arrays.asList(new HoverCard.Pair("Price", "655", ""),
			new HoverCard.Pair("High alch", "240", ""));
		return new InfoCard("Bandos page 2", row, Collections.<String>emptyList(), "Bandos_page_2",
			"Klar received a drop: Bandos page 2.", 24, 37, -1);
	}

	/** A level card: one fact, no notes (the description is the painter's argument). */
	private static InfoCard level()
	{
		final List<HoverCard.Pair> row = Collections.singletonList(new HoverCard.Pair("Level", "99", ""));
		return new InfoCard("Attack", row, Collections.<String>emptyList(), "Attack",
			"Klar has reached Attack level 99.", 21, 27, -1);
	}

	/** The bandos page's item card with its five drop sources, or with none. */
	private static ItemCard page(boolean withDrops)
	{
		final List<DropRates.DropSource> sources = withDrops
			? Arrays.asList(new DropRates.DropSource("Reward casket (hard)", "1/650", "Reward_casket_(hard)"),
				new DropRates.DropSource("Reward casket (master)", "1/702.6", "Reward_casket_(master)"),
				new DropRates.DropSource("Reward casket (elite)", "1/775", "Reward_casket_(elite)"),
				new DropRates.DropSource("Reward casket (medium)", "1/818.4", "Reward_casket_(medium)"),
				new DropRates.DropSource("Reward casket (easy)", "1/864", "Reward_casket_(easy)"))
			: Collections.<DropRates.DropSource>emptyList();
		return new ItemCard(12_000, "Bandos page 2", 1, 655L, 240, "Klar received a drop: Bandos page 2.", 24, 37,
			sources);
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

	/** The card painted in Wiki, split, at the top-left of a clear image; the image and the size it reports. */
	private static Object[] split(HoverCard card, String description, int cap, int lines)
	{
		final BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		final Dimension size = CardPainter.paintSplit(g, card, redSprite(), 16, 0, 0, description, cap, lines,
			CardStyle.of(CardTheme.WIKI));
		g.dispose();
		return new Object[]{image, size};
	}

	private static Dimension splitSize(HoverCard card, String description)
	{
		return (Dimension) split(card, description, 409, 5)[1];
	}

	/** Whether the pixel is exactly {@code colour}, its alpha ignored. */
	private static boolean isColour(BufferedImage image, int x, int y, Color colour)
	{
		return (image.getRGB(x, y) & 0xFFFFFF) == (colour.getRGB() & 0xFFFFFF);
	}

	/** Whether any pixel of the rows from {@code top} up to {@code bottom} of the {@code width} wide image is exactly {@code colour}. */
	private static boolean hasColour(BufferedImage image, int width, int top, int bottom, Color colour)
	{
		for (int y = top; y < bottom; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if (isColour(image, x, y, colour))
				{
					return true;
				}
			}
		}
		return false;
	}

	/** A line of the table font at 16 px, the height the rows of a table are made from: its ascent and its descent. */
	private static int tableLine()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final FontMetrics metrics = g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		g.dispose();
		return metrics.getAscent() + metrics.getDescent();
	}

	/** The height of the hint's line at 16 px, with the inset under it: where the hint begins, counted up from the card's bottom. */
	private static int hintBand()
	{
		final BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		final FontMetrics metrics = g.getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		g.dispose();
		return metrics.getAscent() + metrics.getDescent() + 2 + CardStyle.of(CardTheme.WIKI).inset;
	}

	/** Lot 10Q: the infobox's bar across the top, the picture's cell and the facts as a table of label and value cells. */
	@Test
	public void theTitleBarAndTheTableOfFacts()
	{
		final Object[] painted = split(twoFacts(), "Bandos page 2 is a potential reward.", 409, 5);
		final BufferedImage image = (BufferedImage) painted[0];
		final Dimension size = (Dimension) painted[1];

		assertEquals("as wide as the cap", 409, size.width);
		assertTrue("the corner is the brown edge", isColour(image, 0, 0, EDGE));
		assertTrue("the bar is under the edge", isColour(image, 3, 3, BAR));
		assertTrue("the far corner is the edge too", isColour(image, size.width - 1, size.height - 1, EDGE));
		assertTrue("the label cells are there", hasColour(image, size.width, 0, size.height, LABEL_CELL));
		assertTrue("the value cells are there", hasColour(image, size.width, 0, size.height, VALUE_CELL));
		assertTrue("the title is dark brown", hasColour(image, size.width, 0, size.height, TITLE));
	}

	/** Lot 10Q: the drops are a table, the sources in link blue, and the card is taller by the header and a row each. */
	@Test
	public void theDropsAreATableWithLinks()
	{
		final Object[] with = split(page(true), "", 409, 5);
		final Object[] without = split(page(false), "", 409, 5);
		final BufferedImage withImage = (BufferedImage) with[0];
		final BufferedImage withoutImage = (BufferedImage) without[0];
		final Dimension withSize = (Dimension) with[1];
		final Dimension withoutSize = (Dimension) without[1];

		assertTrue("a source is link blue above the hint",
			hasColour(withImage, withSize.width, 0, withSize.height - hintBand(), LINK));
		assertFalse("with no drops the only blue is the hint's",
			hasColour(withoutImage, withoutSize.width, 0, withoutSize.height - hintBand(), LINK));
		assertTrue("the hint's link is blue", hasColour(withoutImage, withoutSize.width, withoutSize.height - hintBand(),
			withoutSize.height, LINK));
		assertTrue("the header and five rows are taller than six lines: " + withSize.height + " vs " + withoutSize.height,
			withSize.height - withoutSize.height >= 6 * tableLine());
	}

	/** Lot 10Q: the wide card is as wide as it is asked and has the same edge. */
	@Test
	public void theWideCardIsAsWideAsAsked()
	{
		final BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		final Dimension size = CardPainter.paintWide(g, page(true), redSprite(), 16, 519, 0, 0,
			"Bandos page 2 is a potential reward from all tiers of Treasure Trails except beginner.", ACCENT,
			CardStyle.of(CardTheme.WIKI));
		g.dispose();

		assertEquals("as wide as asked", 519, size.width);
		assertTrue("the corner is the brown edge", isColour(image, 0, 0, EDGE));
		assertTrue("the far edge too", isColour(image, 518, 0, EDGE));
		assertTrue("the bar is under the edge", isColour(image, 3, 3, BAR));
	}

	/** Lot 10Q: a description with line breaks is still a numbered list, one line to an item. */
	@Test
	public void aListStaysNumbered()
	{
		final Dimension one = splitSize(level(), "a");
		final Dimension three = splitSize(level(), "a\nb\nc");

		assertTrue("three items are two lines taller than one: " + three.height + " vs " + one.height,
			three.height - one.height >= 2 * tableLine());
	}
}
