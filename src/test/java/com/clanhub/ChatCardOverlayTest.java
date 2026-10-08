package com.clanhub;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** {@link ChatCardOverlay}: the card keeps to the visible chat, whatever the lines scrolled out of it say. */
public class ChatCardOverlayTest
{
	// Re-aimed by the lead, 2026-10-08 (lot 8d): the card shows only over its words, so every hovering frame sits on them.
	private static final String LINE = "Klar received a drop: Dragon claws (121,453,000 coins).";

	private final ItemCard claws = new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, LINE, 22, 34);

	private Client client;
	private ClanHubConfig config;
	private Widget area;
	private ChatCardOverlay overlay;
	private BufferedImage canvas;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		area = mock(Widget.class);
		when(area.isHidden()).thenReturn(false);
		when(area.getBounds()).thenReturn(new Rectangle(0, 340, 519, 140));
		when(client.getWidget(InterfaceID.Chatbox.SCROLLAREA)).thenReturn(area);
		when(client.getCanvasWidth()).thenReturn(765);
		when(client.getCanvasHeight()).thenReturn(503);

		config = mock(ClanHubConfig.class);
		when(config.showCards()).thenReturn(true);
		when(config.cardTrigger()).thenReturn(ClanHubConfig.CardTrigger.HOVER);
		when(config.cardDelayMs()).thenReturn(0);
		when(config.cardFontSize()).thenReturn(16);
		when(config.hoverColour()).thenReturn(new Color(59, 130, 246));

		final CardStore store = new CardStore();
		store.put(LINE, Collections.<HoverCard>singletonList(claws));
		overlay = new ChatCardOverlay(client, config, mock(ItemManager.class), mock(SpriteManager.class));
		overlay.attach(store);
		canvas = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
	}

	/** The x of the middle of the words {@code start} to {@code end} of {@code line}, drawn from {@code lineX}. */
	private int wordsX(String line, int start, int end, int lineX)
	{
		final FontMetrics fm = metrics();
		return lineX + fm.stringWidth(line.substring(0, start)) + fm.stringWidth(line.substring(start, end)) / 2;
	}

	/** The x just past the words that end at {@code end} in {@code line}, drawn from {@code lineX}. */
	private int spanRight(String line, int end, int lineX)
	{
		return lineX + metrics().stringWidth(line.substring(0, end));
	}

	/** The overlay's own font's metrics on the canvas. */
	private FontMetrics metrics()
	{
		final Graphics2D g = canvas.createGraphics();
		final FontMetrics fm = g.getFontMetrics(FontManager.getRunescapeFont());
		g.dispose();
		return fm;
	}

	/** A chat line widget: shown, saying the claws drop, at {@code bounds}. */
	private static Widget line(Rectangle bounds)
	{
		final Widget line = mock(Widget.class);
		when(line.isHidden()).thenReturn(false);
		when(line.getText()).thenReturn(LINE);
		when(line.getBounds()).thenReturn(bounds);
		return line;
	}

	/** A chat line widget: shown, saying {@code text}, at {@code bounds}. */
	private static Widget lineSaying(String text, Rectangle bounds)
	{
		final Widget line = mock(Widget.class);
		when(line.isHidden()).thenReturn(false);
		when(line.getText()).thenReturn(text);
		when(line.getBounds()).thenReturn(bounds);
		return line;
	}

	/** One frame with the mouse at (x, y); the overlay's hovered card after it. */
	private HoverCard frameAt(int x, int y)
	{
		when(client.getMouseCanvasPosition()).thenReturn(new Point(x, y));
		final Graphics2D g = canvas.createGraphics();
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		g.setComposite(AlphaComposite.SrcOver);
		overlay.render(g);
		g.dispose();
		return overlay.hovered();
	}

	/** Whether the last frame painted anything: some pixel of the canvas has a non-zero alpha. */
	private boolean painted()
	{
		// Re-aimed by the lead, 2026-10-08 (lot 8j): "painted" means the card. The line under the mouse now underlines its
		// cards' words on the line's bottom row (414 for the lines these tests draw), so that row is left out here.
		for (int y = 0; y < canvas.getHeight(); y++)
		{
			if (y == 414)
			{
				continue;
			}
			for (int x = 0; x < canvas.getWidth(); x++)
			{
				if ((canvas.getRGB(x, y) >>> 24) != 0)
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * The bounding box of the pixels the last frame painted, leaving out the row {@code skipRow} (the underline's), or null
	 * when it painted none.
	 */
	private Rectangle paintedBox(int skipRow)
	{
		int left = Integer.MAX_VALUE;
		int top = Integer.MAX_VALUE;
		int right = -1;
		int bottom = -1;
		for (int y = 0; y < canvas.getHeight(); y++)
		{
			for (int x = 0; x < canvas.getWidth(); x++)
			{
				if (y != skipRow && (canvas.getRGB(x, y) >>> 24) != 0)
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

	@Test
	public void theCardOpensWhereTheSettingSays()
	{
		final Widget frame = mock(Widget.class);
		when(frame.isHidden()).thenReturn(false);
		when(frame.getBounds()).thenReturn(new Rectangle(0, 340, 519, 142));
		when(client.getWidget(InterfaceID.Chatbox.UNIVERSE)).thenReturn(frame);
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		// The underline sits in the row under the line's box, 414; it is left out of the boxes.
		final int underline = 414;
		final int words = wordsX(LINE, 22, 34, 0);

		when(config.cardPlace()).thenReturn(ClanHubConfig.CardPlace.CURSOR);
		assertSame("the card is hit", claws, frameAt(words, 407));
		final Rectangle atCursor = paintedBox(underline);
		assertTrue("the split card painted", atCursor != null);
		// Re-aimed by the lead, 2026-10-08 (lot 8O): the header across the top, the paragraph under; the card is as wide as
		// the painter says for the same card at 16 px with no description, not a fixed 360.
		final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D scratchGraphics = scratch.createGraphics();
		final int cardWidth = CardPainter.paintSplit(scratchGraphics, claws, null, 16, 0, 0, "").width;
		scratchGraphics.dispose();
		final int left = Math.max(0, words - cardWidth / 2);
		assertEquals("centred on the words' middle, held at the canvas's left edge", left, atCursor.x);
		assertEquals("and as wide as the painter says", cardWidth, atCursor.width);
		assertEquals("its bottom row is 400, 6 px above the mouse", 400, atCursor.y + atCursor.height - 1);

		when(config.cardPlace()).thenReturn(ClanHubConfig.CardPlace.CHAT);
		for (final int mouseX : new int[] {words, words - 10})
		{
			assertSame("the card is hit", claws, frameAt(mouseX, 407));
			final Rectangle aboveChat = paintedBox(underline);
			assertTrue("the wide card painted", aboveChat != null);
			assertEquals("its left is the chatbox's, wherever the mouse is on the words", 0, aboveChat.x);
			assertEquals("its right is 518, as wide as the chatbox", 518, aboveChat.x + aboveChat.width - 1);
			assertEquals("its bottom row is 339, the row above the chatbox's top", 339,
				aboveChat.y + aboveChat.height - 1);
		}
	}

	@Test
	public void aLineScrolledOutOfTheChatNeverHits()
	{
		final Widget scrolled = line(new Rectangle(0, 200, 500, 14));
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {scrolled, visible});
		final int words = wordsX(LINE, 22, 34, 0);

		assertSame("on the visible line", claws, frameAt(words, 407));
		assertNull("on the scrolled line's rectangle, outside the area", frameAt(100, 207));
		assertSame("back on the visible line, the next frame walks again", claws, frameAt(words, 407));

		when(area.isHidden()).thenReturn(true);
		assertNull("the area hidden", frameAt(words, 407));
	}

	@Test
	public void aNameWithTheOtherSpaceStillHits()
	{
		final String stored = "self ctrl received a drop: 154 x Dragon knife (618,310 coins).";
		final ItemCard knife = new ItemCard(22804, "Dragon knife", 154, 4_015L, 1_000, stored, 33, 45);
		final CardStore store = new CardStore();
		store.put(stored, Collections.<HoverCard>singletonList(knife));
		overlay.attach(store);
		final Widget visible = mock(Widget.class);
		when(visible.isHidden()).thenReturn(false);
		when(visible.getText()).thenReturn("self\u00a0ctrl received a drop: 154 x Dragon knife (618,310 coins).");
		when(visible.getBounds()).thenReturn(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});

		assertSame("the drawn name has a non-breaking space, the stored one a plain space", knife,
			frameAt(wordsX(stored, 33, 45, 0), 407));
	}

	@Test
	public void onlyTheVisiblePartOfALineHits()
	{
		final Widget half = line(new Rectangle(0, 333, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {half});

		assertSame("in the half inside the area", claws, frameAt(wordsX(LINE, 22, 34, 0), 345));
		assertNull("in the half above the area's top edge", frameAt(100, 336));
	}

	@Test
	public void onClickTheCardWaitsForTheClick()
	{
		when(config.cardTrigger()).thenReturn(ClanHubConfig.CardTrigger.CLICK);
		final Widget scrolled = line(new Rectangle(0, 200, 500, 14));
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		final int words = wordsX(LINE, 22, 34, 0);
		when(area.getDynamicChildren()).thenReturn(new Widget[] {scrolled, visible});

		assertSame("on the words, before any click", claws, frameAt(words, 407));
		assertFalse("no card before the click", painted());

		overlay.clicked();
		frameAt(words, 407);
		assertTrue("the click opens the card", painted());
		frameAt(words, 407);
		assertTrue("the click holds while the mouse stays on the words", painted());

		assertNull("outside the area", frameAt(100, 207));
		assertFalse("nothing painted outside the area", painted());
		frameAt(words, 407);
		assertFalse("back on the words, the card needs a new click", painted());

		overlay.clicked();
		frameAt(words, 407);
		assertTrue("a new click opens it again", painted());
	}

	@Test
	public void aClickElsewhereClosesTheCard()
	{
		when(config.cardTrigger()).thenReturn(ClanHubConfig.CardTrigger.CLICK);
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		final int words = wordsX(LINE, 22, 34, 0);
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});

		overlay.clicked();
		frameAt(words, 407);
		assertTrue("the click on the words opens the card", painted());

		overlay.clicked();
		frameAt(100, 470);
		assertFalse("a click inside the area but off the line closes it", painted());
		frameAt(words, 407);
		assertFalse("back on the words, the click elsewhere has cleared it", painted());
	}

	@Test
	public void theCardShowsOnlyOverTheWords()
	{
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});

		assertNull("on the line, left of the words", frameAt(20, 407));
		assertFalse("nothing painted left of the words", painted());
		assertSame("on the words", claws, frameAt(wordsX(LINE, 22, 34, 0), 407));
		assertTrue("the card painted over the words", painted());
		assertNull("on the line, right of the text", frameAt(480, 407));
		assertFalse("nothing painted right of the text", painted());
	}

	@Test
	public void theLineUnderTheMouseUnderlinesItsCardsWords()
	{
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int words = wordsX(LINE, 22, 34, 0);
		final int hover = new Color(59, 130, 246).getRGB();

		assertNull("on the line, left of the words, no card", frameAt(20, 407));
		assertNull("and nothing painted above the line, no card", paintedBox(414));
		assertEquals("the words are underlined in the hover colour, on the line's bottom row", hover,
			canvas.getRGB(words, 414));
		assertEquals("only the words", 0, canvas.getRGB(10, 414) >>> 24);

		assertNull("outside the area", frameAt(100, 207));
		assertFalse("nothing painted anywhere", painted());
	}

	@Test
	public void aQuietPhraseIsUnderlinedAlwaysWhenTheSettingIsOn()
	{
		final String level = "Klar has reached Defence level 70.";
		final InfoCard quiet = new InfoCard("Defence level 70", Collections.singletonList(
			new HoverCard.Pair("Skill", "Defence", "")), Collections.<String>emptyList(), "Defence", level, 16, 33, 199,
			-1, false);
		final CardStore store = new CardStore();
		store.put(level, Collections.<HoverCard>singletonList(quiet));
		overlay.attach(store);
		final Widget visible = lineSaying(level, new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int words = wordsX(level, 16, 33, 0);
		final Color orange = new Color(220, 138, 0);
		when(config.alwaysUnderlineColour()).thenReturn(orange);

		when(config.alwaysUnderlineLevels()).thenReturn(true);
		assertNull("the mouse outside the area", frameAt(100, 207));
		assertEquals("the phrase carries its line in the always colour", orange.getRGB(), canvas.getRGB(words, 414));
		assertNull("and nothing else is painted", paintedBox(414));

		when(config.alwaysUnderlineLevels()).thenReturn(false);
		assertNull("the mouse outside the area", frameAt(100, 207));
		assertFalse("the setting off, nothing painted", painted());

		when(config.alwaysUnderlineLevels()).thenReturn(true);
		assertNull("the mouse on the line, off the phrase", frameAt(20, 407));
		assertEquals("the hover-reveal draws over it in the hover colour", new Color(59, 130, 246).getRGB(),
			canvas.getRGB(words, 414));
	}

	@Test
	public void aQuietPhraseOnALineScrolledOutOfTheChatGetsNoLine()
	{
		final String level = "Klar has reached Defence level 70.";
		final InfoCard quiet = new InfoCard("Defence level 70", Collections.singletonList(
			new HoverCard.Pair("Skill", "Defence", "")), Collections.<String>emptyList(), "Defence", level, 16, 33, 199,
			-1, false);
		final CardStore store = new CardStore();
		store.put(level, Collections.<HoverCard>singletonList(quiet));
		overlay.attach(store);
		final Widget scrolled = lineSaying(level, new Rectangle(0, 200, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {scrolled});
		when(config.alwaysUnderlineLevels()).thenReturn(true);
		when(config.alwaysUnderlineColour()).thenReturn(new Color(220, 138, 0));

		frameAt(100, 207);
		assertFalse("a line whose rectangle sits over the game world is not underlined", painted());
	}

	@Test
	public void aLineRuneLiteRewroteIsReadWhenTheMouseReachesIt()
	{
		final String text = "Brutus kill count: 343";
		final CardStore store = new CardStore();
		overlay.attach(store);
		assertNull("the store holds nothing for the text", store.find(text));
		final Widget rewritten = lineSaying(text, new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {rewritten});

		final HoverCard card = frameAt(wordsX(text, 0, 6, 0), 407);
		assertTrue("a card was read from the drawn text", card != null);
		assertEquals("Brutus kill count", card.title());
		assertEquals("the words are underlined in the hover colour", new Color(59, 130, 246).getRGB(),
			canvas.getRGB(wordsX(text, 0, 6, 0), 414));
		final CardStore.Match stored = store.find(text);
		assertTrue("and it is stored from now on", stored != null);
		assertEquals(0, stored.offset);

		final Widget other = lineSaying("Klar: gz", new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {other});
		// x 120, not 20: a move under the walk threshold off the old widget's words would keep the old hit.
		assertNull("a line that is no answer has no card", frameAt(120, 407));
		assertFalse("and nothing is painted", painted());
		assertNull("and nothing is stored for it", store.find("Klar: gz"));
	}

	@Test
	public void aSmallMoveOffTheWordsDropsTheCard()
	{
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int right = spanRight(LINE, 34, 0);

		assertSame("on the last pixel of the words", claws, frameAt(right - 1, 407));
		assertTrue("the card painted", painted());
		assertNull("two pixels on, off the words and under the walk threshold", frameAt(right + 1, 407));
		assertFalse("nothing painted off the words", painted());
	}

	@Test
	public void aWrappedLineHitsAndUnderlinesOnTheRowTheWordsFallOn()
	{
		// On a click only the underlines paint, so the pixels read below are the hover-reveal's alone.
		when(config.cardTrigger()).thenReturn(ClanHubConfig.CardTrigger.CLICK);
		final int hover = new Color(59, 130, 246).getRGB();

		// The text through "claws" fits the first row; "(121,453,000 coins)." is the second.
		final int firstWidth = spanRight(LINE, 34, 0) + 2;
		final Widget wrapped = line(new Rectangle(0, 400, firstWidth, 28));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {wrapped});
		final int words = wordsX(LINE, 22, 34, 0);

		assertSame("the mouse on the words, on their row", claws, frameAt(words, 407));
		assertEquals("the words are underlined on their row", hover, canvas.getRGB(words, 414));
		assertEquals("and not on the row under it", 0, canvas.getRGB(words, 428) >>> 24);
		assertNull("the mouse on the second row, under the words", frameAt(words, 421));
		assertEquals("the line's hover-reveal stays", hover, canvas.getRGB(words, 414));

		// "Klar received a drop:" is the first row, "Dragon claws." the second.
		final String drop = "Klar received a drop: Dragon claws.";
		final ItemCard dragon = new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, drop, 22, 34);
		final CardStore fresh = new CardStore();
		fresh.put(drop, Collections.<HoverCard>singletonList(dragon));
		overlay.attach(fresh);
		final int secondWidth = spanRight(drop, 21, 0) + 2;
		final Widget second = lineSaying(drop, new Rectangle(0, 400, secondWidth, 28));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {second});
		final int onSecond = wordsX("Dragon claws.", 0, 12, 0);

		assertSame("the words on the second row, the mouse on it", dragon, frameAt(onSecond, 421));
		assertEquals("the words are underlined on the second row", hover, canvas.getRGB(onSecond, 428));
		assertEquals("and not on the first", 0, canvas.getRGB(onSecond, 414) >>> 24);
		assertNull("the mouse on the first row, over the words", frameAt(onSecond, 407));
		assertEquals("the hover-reveal stays on the second row", hover, canvas.getRGB(onSecond, 428));
	}
}
