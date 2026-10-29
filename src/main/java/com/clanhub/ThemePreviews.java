package com.clanhub;

import com.clanhub.SidebarSkin.Box;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The pictures the settings menu shows beside a theme list while a theme is armed: a sample card in a card theme, and a
 * small sidebar in a sidebar skin. Pure: a theme or a skin in, a picture out; the menu hands the picture to the card
 * viewer, which puts it beside the list.
 *
 * <p>The sample card is an item card, the Abyssal whip, painted by {@link CardPainter#paintSplit} at the card's usual
 * 16 px with the Medium width, so it is the card the player would see in that theme. The small sidebar is drawn with the
 * skin's own colours and boxes ({@link SidebarSkin#paintBox}) and the client's fonts, laid out as the real sidebar's top
 * card and first two rows are, only smaller.
 */
final class ThemePreviews
{
	/** The item whose picture is the sample card's: the abyssal whip. */
	static final int WHIP_ITEM_ID = 4151;

	/** The sample card's description, in the Medium width's five lines. */
	static final String DESCRIPTION = "A weapon from the abyss.";
	/** The sample card's text size, widest width at that size and most paragraph lines: the Medium card. */
	static final int CARD_FONT_SIZE = 16;
	static final int CARD_CAP = 409;
	static final int CARD_LINES = 5;

	/** The small sidebar's width, in px, the room round its boxes, and the sizes of the boxes and the words. */
	static final int SIDEBAR_WIDTH = 220;
	private static final int MARGIN = 6;
	private static final int BOX_WIDTH = SIDEBAR_WIDTH - 2 * MARGIN;
	private static final int TOP_HEIGHT = 74;
	private static final int BOX_INSET = 8;
	private static final int BAR_HEIGHT = 24;
	private static final int ROW_HEIGHT = 36;
	private static final int ROW_GAP = 2;
	private static final float TITLE_SIZE = 12f;
	private static final float TEXT_SIZE = 11f;
	private static final float TIME_SIZE = 9f;
	/** The space between the top card and "Recent cards", and the height that line takes, in px. */
	private static final int HEADER_ABOVE = 6;
	private static final int HEADER_HEIGHT = 16;

	/** The words of the small sidebar. */
	private static final String BAR_TEXT = "How to use";
	private static final String ACTION_TEXT = "Hover the coloured name for its card.";
	private static final String WIKI_TEXT = "Right-click it for the wiki.";
	private static final String RECENT_TEXT = "Recent cards";
	private static final String TIME_TEXT = "now";
	/** The two sample rows: the title, the name and the kind of each. */
	private static final List<List<String>> ROWS = Arrays.asList(
		Arrays.asList("Dragon Slayer II", "Iron Pete", "quest"),
		Arrays.asList("Barrows Novice", "GMucker", "easy task"));

	private ThemePreviews()
	{
	}

	/**
	 * The card the sample shows: the Abyssal whip at 1,500,000 coins and 72,000 high alchemy, with three drop sources.
	 */
	static HoverCard sampleCard()
	{
		return new ItemCard(WHIP_ITEM_ID, "Abyssal whip", 1, 1_500_000L, 72_000, "Abyssal whip", 0, 12, Arrays.asList(
			new DropRates.DropSource("Abyssal demon", "1/512", "Abyssal_demon"),
			new DropRates.DropSource("Greater abyssal demon", "1/170.7", "Greater_abyssal_demon"),
			new DropRates.DropSource("Unsired", "1/10.7", "Unsired")));
	}

	/**
	 * The sample card in {@code style} (an original theme's, or a picture theme's), painted at the card's top-left corner of
	 * a new image the size the painter measures: 16 px text, a cap of 409 px and five paragraph lines.
	 *
	 * @param picture the whip's item picture, or null while it has not come, which leaves the picture box empty
	 */
	static BufferedImage card(CardStyle style, BufferedImage picture)
	{
		final HoverCard card = sampleCard();
		final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D measure = scratch.createGraphics();
		final Dimension size;
		try
		{
			size = CardPainter.paintSplit(measure, card, picture, CARD_FONT_SIZE, 0, 0, DESCRIPTION, CARD_CAP, CARD_LINES, style);
		}
		finally
		{
			measure.dispose();
		}
		final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			CardPainter.paintSplit(g, card, picture, CARD_FONT_SIZE, 0, 0, DESCRIPTION, CARD_CAP, CARD_LINES, style);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/**
	 * A small sidebar, {@value #SIDEBAR_WIDTH} px wide, in {@code skin}: the page's ground, the top card with the how-to's
	 * bar and its two lines, "Recent cards" under it and two rows of recent cards, all in the skin's colours, the names in
	 * the brand orange.
	 */
	static BufferedImage sidebar(SidebarSkin skin)
	{
		final int rowsTop = MARGIN + TOP_HEIGHT + HEADER_ABOVE + HEADER_HEIGHT;
		final int height = rowsTop + ROWS.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP + MARGIN;
		final BufferedImage image = new BufferedImage(SIDEBAR_WIDTH, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(skin.page);
			g.fillRect(0, 0, SIDEBAR_WIDTH, height);
			topCard(g, skin);

			final Font bold = FontManager.getDefaultBoldFont().deriveFont(TITLE_SIZE);
			g.setFont(bold);
			g.setColor(skin.title);
			g.drawString(RECENT_TEXT, MARGIN + 2, MARGIN + TOP_HEIGHT + HEADER_ABOVE + g.getFontMetrics().getAscent());

			for (int i = 0; i < ROWS.size(); i++)
			{
				row(g, skin, ROWS.get(i), rowsTop + i * (ROW_HEIGHT + ROW_GAP));
			}
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/** The top card: its box, the how-to's bar with the title, and the action line and the wiki line under the bar. */
	private static void topCard(Graphics2D g, SidebarSkin skin)
	{
		box(g, skin, MARGIN, MARGIN, BOX_WIDTH, TOP_HEIGHT, Box.CARD);
		final int inner = BOX_WIDTH - 2 * BOX_INSET;
		final int barX = MARGIN + BOX_INSET;
		final int barY = MARGIN + BOX_INSET;
		box(g, skin, barX, barY, inner, BAR_HEIGHT, Box.BAR);

		final Font bold = FontManager.getDefaultBoldFont().deriveFont(TITLE_SIZE);
		final FontMetrics boldMetrics = g.getFontMetrics(bold);
		g.setFont(bold);
		g.setColor(skin.title);
		g.drawString(BAR_TEXT, barX + BOX_INSET, barY + (BAR_HEIGHT + boldMetrics.getAscent() - boldMetrics.getDescent()) / 2);

		final Font plain = fitted(g, ACTION_TEXT, inner);
		final FontMetrics metrics = g.getFontMetrics(plain);
		g.setFont(plain);
		g.setColor(skin.text);
		final int first = barY + BAR_HEIGHT + 6 + metrics.getAscent();
		g.drawString(ACTION_TEXT, barX, first);
		g.setColor(skin.grey);
		g.drawString(WIKI_TEXT, barX, first + metrics.getHeight());
	}

	/** One sample row at {@code y}: its box, the title in bold, the time at the right, the name and the kind under the title. */
	private static void row(Graphics2D g, SidebarSkin skin, List<String> words, int y)
	{
		box(g, skin, MARGIN, y, BOX_WIDTH, ROW_HEIGHT, Box.ROW);
		final int left = MARGIN + BOX_INSET;
		final Font bold = FontManager.getDefaultBoldFont().deriveFont(TITLE_SIZE);
		g.setFont(bold);
		g.setColor(skin.rowTitle);
		g.drawString(words.get(0), left, y + 4 + g.getFontMetrics().getAscent());

		final Font small = FontManager.getDefaultFont().deriveFont(TIME_SIZE);
		g.setFont(small);
		g.setColor(skin.time);
		g.drawString(TIME_TEXT, MARGIN + BOX_WIDTH - BOX_INSET - g.getFontMetrics().stringWidth(TIME_TEXT),
			y + 4 + g.getFontMetrics().getAscent());

		final Font plain = FontManager.getDefaultFont().deriveFont(TEXT_SIZE);
		final FontMetrics metrics = g.getFontMetrics(plain);
		final int baseline = y + ROW_HEIGHT - 6 - metrics.getDescent();
		g.setFont(plain);
		g.setColor(ColorScheme.BRAND_ORANGE);
		g.drawString(words.get(1), left, baseline);
		g.setColor(skin.grey);
		g.drawString("  " + words.get(2), left + metrics.stringWidth(words.get(1)), baseline);
	}

	/** Paints {@code kind}'s box of {@code skin} at ({@code x}, {@code y}), {@code width} by {@code height} px. */
	private static void box(Graphics2D g, SidebarSkin skin, int x, int y, int width, int height, Box kind)
	{
		final Graphics2D inBox = (Graphics2D) g.create(x, y, width, height);
		try
		{
			skin.paintBox(inBox, width, height, kind);
		}
		finally
		{
			inBox.dispose();
		}
	}

	/** The client's plain font at the largest size, {@value #TEXT_SIZE} px at most, at which {@code text} fits in {@code width} px. */
	private static Font fitted(Graphics2D g, String text, int width)
	{
		float size = TEXT_SIZE;
		Font font = FontManager.getDefaultFont().deriveFont(size);
		while (size > 8f && g.getFontMetrics(font).stringWidth(text) > width)
		{
			size -= 1f;
			font = font.deriveFont(size);
		}
		return font;
	}
}
