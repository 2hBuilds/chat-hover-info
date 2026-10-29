package com.clanhub;

import com.google.gson.Gson;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
		when(config.colourHoverables()).thenReturn(true);
		// Re-aimed by the lead, 2026-10-08 (lot 8Q): the cap and the line count are the caller's now.
		when(config.cardWidth()).thenReturn(ClanHubConfig.CardWidth.WIDE);

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
		final int cardWidth = CardPainter.paintSplit(scratchGraphics, claws, null, 16, 0, 0, "", 480, 3,
			CardStyle.CLASSIC).width;
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

	/** Lot 10C: the image the sidebar shows is the split card painted from the origin, in an image of the size it measures. */
	@Test
	public void theCardImageIsTheSplitCardAtTheOrigin()
	{
		final BufferedImage image = overlay.cardImage(claws);

		final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D scratchGraphics = scratch.createGraphics();
		final java.awt.Dimension size = CardPainter.paintSplit(scratchGraphics, claws, null, 16, 0, 0, "", 480, 3,
			CardStyle.CLASSIC);
		scratchGraphics.dispose();
		assertNotNull("a card gives an image", image);
		assertEquals("as wide as the painter says", size.width, image.getWidth());
		assertEquals("and as tall", size.height, image.getHeight());
		assertEquals("the card's black edge starts at the corner", 0xFF000000, image.getRGB(0, 0));
		assertNull("no card gives no image", overlay.cardImage(null));
	}

	@Test
	public void aCardWithSeveralNotesPaintsThemAsAList()
	{
		final String one = "Klar has reached Hitpoints level 99.";
		final String two = "Mossy has reached Hitpoints level 99.";
		final String second = "Buy the hitpoints cape and hood from Surgeon General Tafani in the Emir's Arena hospital, "
			+ "and wear them";
		final List<HoverCard.Pair> row = Collections.singletonList(new HoverCard.Pair("New unlocks", "2", ""));
		final InfoCard single = new InfoCard("Hitpoints 99", row, Collections.singletonList("Skill mastery"), "Hitpoints",
			one, one.indexOf("Hitpoints"), one.length() - 1, -1, -1, false);
		final InfoCard several = new InfoCard("Hitpoints 99", row, Arrays.asList("Skill mastery", second),
			"Hitpoints", two, two.indexOf("Hitpoints"), two.length() - 1, -1, -1, false);
		final CardStore store = new CardStore();
		store.put(one, Collections.<HoverCard>singletonList(single));
		store.put(two, Collections.<HoverCard>singletonList(several));
		overlay.attach(store);
		final Widget lineOne = lineSaying(one, new Rectangle(0, 400, 500, 14));
		final Widget lineTwo = lineSaying(two, new Rectangle(0, 420, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {lineOne, lineTwo});
		when(config.cardPlace()).thenReturn(ClanHubConfig.CardPlace.CURSOR);
		when(config.cardWidth()).thenReturn(ClanHubConfig.CardWidth.MEDIUM);
		final int bodyHeight = metrics().getAscent() + metrics().getDescent() + 2;

		assertSame("the first card is hit", single, frameAt(wordsX(one, one.indexOf("Hitpoints"), one.length() - 1, 0), 407));
		final Rectangle plain = paintedBox(414);
		assertTrue("the card with one note painted", plain != null);

		assertSame("the second card is hit", several,
			frameAt(wordsX(two, two.indexOf("Hitpoints"), two.length() - 1, 0), 427));
		final Rectangle list = paintedBox(434);
		assertTrue("the card with two notes painted", list != null);
		final int taller = list.height - plain.height;
		assertTrue("the second note takes two more lines: " + taller + " vs " + bodyHeight,
			taller >= 2 * bodyHeight && taller <= 3 * bodyHeight);
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
		// Re-aimed by the lead, 2026-10-08 (lot 8R): the hover-reveal underlines quiet words only.
		final String level = "Klar has reached Defence level 70.";
		final InfoCard quiet = new InfoCard("Defence level 70", Collections.singletonList(
			new HoverCard.Pair("Skill", "Defence", "")), Collections.<String>emptyList(), "Defence", level, 16, 33, 199,
			-1, false);
		final CardStore store = new CardStore();
		store.put(LINE, Collections.<HoverCard>singletonList(claws));
		store.put(level, Collections.<HoverCard>singletonList(quiet));
		overlay.attach(store);
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		final Widget levelLine = lineSaying(level, new Rectangle(0, 420, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible, levelLine});
		final int words = wordsX(LINE, 22, 34, 0);
		final int phrase = wordsX(level, 16, 33, 0);
		final int hover = new Color(59, 130, 246).getRGB();

		assertNull("on the claws line, left of the words, no card", frameAt(20, 407));
		assertNull("and nothing painted above the line, no card", paintedBox(414));
		assertEquals("a marked card's words carry no underline", 0, canvas.getRGB(words, 414) >>> 24);

		assertNull("on the level line, off the phrase, no card", frameAt(20, 427));
		assertEquals("the quiet phrase is underlined in the hover colour, on its line's bottom row", hover,
			canvas.getRGB(phrase, 434));
		assertEquals("only the phrase", 0, canvas.getRGB(10, 434) >>> 24);
		assertEquals("and nothing under the claws", 0, canvas.getRGB(words, 414) >>> 24);

		assertNull("outside the area", frameAt(100, 207));
		assertFalse("nothing painted anywhere", painted());

		// Re-aimed by the lead, 2026-10-08 (lot 8R): with the colouring off, a marked card's words are not blue, so the
		// hover-reveal underlines them too.
		when(config.colourHoverables()).thenReturn(false);
		assertNull("on the claws line with the colouring off, no card", frameAt(20, 407));
		assertEquals("the marked words are underlined when they are not blue", hover, canvas.getRGB(words, 414));
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
	public void aLineWithAStringVariableHitsOnTheDrawnWords()
	{
		final String plain = "Small Za has completed a quest: King's Ransom";
		final String raw = "<img=2> <str_quest_name_0=King's Ransom>Small Za has completed a quest: "
			+ "<str_quest_name_0>";
		final int start = plain.indexOf("King's");
		final InfoCard quest = new InfoCard("King's Ransom", Collections.singletonList(
			new HoverCard.Pair("Quest completed", "", "")), Collections.<String>emptyList(), "King's_Ransom", plain, start,
			plain.length(), SkillSprites.QUEST);
		final CardStore store = new CardStore();
		store.put(plain, Collections.<HoverCard>singletonList(quest));
		overlay.attach(store);
		final Widget drawn = lineSaying(raw, new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {drawn});

		final int completed = plain.indexOf("completed");
		assertSame("the mouse over the drawn quest name", quest, frameAt(wordsX(plain, start, start + 6, 0), 407));
		assertTrue("and the card painted", painted());
		assertNull("the mouse over \"completed\"", frameAt(wordsX(plain, completed, completed + 9, 0), 407));
		assertFalse("and nothing painted", painted());
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
		// Re-aimed by the lead, 2026-10-08 (lot 8R): the hover-reveal underlines quiet words only.
		// On a click only the underlines paint, so the pixels read below are the hover-reveal's alone.
		when(config.cardTrigger()).thenReturn(ClanHubConfig.CardTrigger.CLICK);
		final int hover = new Color(59, 130, 246).getRGB();
		final InfoCard quietClaws = new InfoCard("Dragon claws", Collections.<HoverCard.Pair>emptyList(),
			Collections.<String>emptyList(), "Dragon_claws", LINE, 22, 34, -1, -1, false);
		final CardStore quietStore = new CardStore();
		quietStore.put(LINE, Collections.<HoverCard>singletonList(quietClaws));
		overlay.attach(quietStore);

		// The text through "claws" fits the first row; "(121,453,000 coins)." is the second.
		final int firstWidth = spanRight(LINE, 34, 0) + 2;
		final Widget wrapped = line(new Rectangle(0, 400, firstWidth, 28));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {wrapped});
		final int words = wordsX(LINE, 22, 34, 0);

		assertSame("the mouse on the words, on their row", quietClaws, frameAt(words, 407));
		assertEquals("the words are underlined on their row", hover, canvas.getRGB(words, 414));
		assertEquals("and not on the row under it", 0, canvas.getRGB(words, 428) >>> 24);
		assertNull("the mouse on the second row, under the words", frameAt(words, 421));
		assertEquals("the line's hover-reveal stays", hover, canvas.getRGB(words, 414));

		// "Klar received a drop:" is the first row, "Dragon claws." the second.
		final String drop = "Klar received a drop: Dragon claws.";
		final InfoCard dragon = new InfoCard("Dragon claws", Collections.<HoverCard.Pair>emptyList(),
			Collections.<String>emptyList(), "Dragon_claws", drop, 22, 34, -1, -1, false);
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

	/** The wiki's page answer for Duke Sucellus: a PNG thumbnail on the wiki's own host. */
	private static final String DUKE_PAGE = "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":1,\"ns\":0,"
		+ "\"title\":\"Duke Sucellus\",\"thumbnail\":{\"source\":\"https://oldschool.runescape.wiki/images/thumb/"
		+ "Duke_Sucellus.png/128px-Duke_Sucellus.png?abcde\",\"width\":128,\"height\":142},"
		+ "\"pageimage\":\"Duke_Sucellus.png\"}]}}";

	/** A 200 answer of {@code type} holding {@code body}. */
	private static Response answer(String type, byte[] body)
	{
		return new Response.Builder()
			.request(WikiPictures.requestFor("Duke Sucellus"))
			.protocol(Protocol.HTTP_1_1)
			.code(200)
			.message("OK")
			.body(ResponseBody.create(MediaType.parse(type), body))
			.build();
	}

	/** The bytes of the 128 x 40 picture fixture. */
	private static byte[] fixtureBytes() throws Exception
	{
		try (InputStream in = ChatCardOverlayTest.class.getResourceAsStream("/com/clanhub/picture-fixture.png"))
		{
			assertNotNull("the fixture is on the test classpath", in);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] chunk = new byte[4096];
			for (int read = in.read(chunk); read >= 0; read = in.read(chunk))
			{
				out.write(chunk, 0, read);
			}
			return out.toByteArray();
		}
	}

	/** The pixels of the canvas that are plainly red (a blended edge is not), as a bounding box, or null when there are none. */
	private Rectangle redBox()
	{
		int left = Integer.MAX_VALUE;
		int top = Integer.MAX_VALUE;
		int right = -1;
		int bottom = -1;
		for (int y = 0; y < canvas.getHeight(); y++)
		{
			for (int x = 0; x < canvas.getWidth(); x++)
			{
				final Color pixel = new Color(canvas.getRGB(x, y), true);
				if (pixel.getAlpha() == 255 && pixel.getRed() > 200 && pixel.getGreen() < 60 && pixel.getBlue() < 60)
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
	public void aWikiPictureCardShowsThePictureWhenItComes() throws Exception
	{
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		overlay.pictures(new WikiPictures(factory, new Gson()));
		when(config.wikiPictures()).thenReturn(true);
		when(config.cardWidth()).thenReturn(ClanHubConfig.CardWidth.MEDIUM);
		when(config.cardPlace()).thenReturn(ClanHubConfig.CardPlace.CURSOR);

		final String line = "Klar received a drop: Dragon claws (121,453,000 coins) from Duke Sucellus.";
		final int start = line.indexOf("Duke Sucellus");
		final int end = start + "Duke Sucellus".length();
		final InfoCard duke = new InfoCard("Duke Sucellus", Collections.<HoverCard.Pair>emptyList(),
			Collections.<String>emptyList(), "Duke_Sucellus", line, start, end, SkillSprites.spriteOf("Combat"), -1, false,
			true);
		final String other = "Klar received a drop: Dragon claws (121,453,000 coins) from Zulrah.";
		final InfoCard zulrah = new InfoCard("Zulrah", Collections.<HoverCard.Pair>emptyList(),
			Collections.<String>emptyList(), "Zulrah", other, other.indexOf("Zulrah"), other.length() - 1,
			SkillSprites.spriteOf("Combat"), -1, false, true);
		final CardStore store = new CardStore();
		store.put(line, Collections.<HoverCard>singletonList(duke));
		store.put(other, Collections.<HoverCard>singletonList(zulrah));
		overlay.attach(store);
		final Widget dukeLine = lineSaying(line, new Rectangle(0, 400, 500, 14));
		final Widget zulrahLine = lineSaying(other, new Rectangle(0, 420, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {dukeLine, zulrahLine});
		final int words = wordsX(line, start, end, 0);

		assertSame("the words are hit", duke, frameAt(words, 407));
		assertNotNull("the box is painted", paintedBox(414));
		assertNull("no picture yet, the sprite manager has no sprite either: nothing red", redBox());
		verify(factory, times(1)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> page = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(page.capture());
		page.getValue().onResponse(call, answer("application/json", DUKE_PAGE.getBytes(StandardCharsets.UTF_8)));
		verify(factory, times(2)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> thumb = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(thumb.capture());
		thumb.getValue().onResponse(call, answer("image/png", fixtureBytes()));

		assertSame("the same card is still hit", duke, frameAt(words, 407));
		final Rectangle card = paintedBox(414);
		assertNotNull(card);
		final Rectangle red = redBox();
		assertNotNull("the picture's red shows in the box", red);
		assertTrue("at least 40 wide and 10 tall: " + red, red.width >= 40 && red.height >= 10);
		final int expected = card.x + CardPainter.INSET + 4;
		assertTrue("its left is the box's left + 9 + 4, within 2 px: " + red.x + " vs " + expected,
			Math.abs(red.x - expected) <= 2);
		assertTrue("it lies inside the 64 px box, which is 9 px in from the card's top: " + red,
			red.y >= card.y + CardPainter.INSET
				&& red.y + red.height <= card.y + CardPainter.INSET + CardPainter.PICTURE_BOX);
		verify(factory, times(2)).newCall(any(Request.class));

		when(config.wikiPictures()).thenReturn(false);
		assertSame("still hit with the setting off", duke, frameAt(words, 407));
		assertNotNull(paintedBox(414));
		assertNull("with the setting off no picture is drawn", redBox());

		assertSame("a fresh card is hit", zulrah,
			frameAt(wordsX(other, other.indexOf("Zulrah"), other.length() - 1, 0), 427));
		assertNotNull(paintedBox(434));
		verify(factory, times(2)).newCall(any(Request.class));
	}

	/**
	 * The chat moves a row: the claws line {@code visible} rises to y 386 and a new line, which has no card, sits where the
	 * mouse was (y 400 to 414); the area answers both, the new one last.
	 */
	private void theChatMovesUp(Widget visible)
	{
		when(visible.getBounds()).thenReturn(new Rectangle(0, 386, 500, 14));
		final Widget fresh = lineSaying("Someone: hello there", new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible, fresh});
	}

	@Test
	public void theCardHoldsWhileTheMouseIsStillWhenTheChatMoves()
	{
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int words = wordsX(LINE, 22, 34, 0);

		assertSame("on the words", claws, frameAt(words, 407));
		assertTrue("the card painted", painted());

		theChatMovesUp(visible);
		assertSame("the chat moved, the mouse did not: the card stays", claws, frameAt(words, 407));
		assertTrue("the held card painted", painted());
		assertSame("and on the next frame", claws, frameAt(words, 407));
		assertTrue("still painted", painted());
		assertSame("two pixels off, within the jitter", claws, frameAt(words + 2, 407));
		assertTrue("still painted within the jitter", painted());
		assertNull("three pixels from where it rested ends the hold", frameAt(words + 3, 407));
		assertFalse("nothing painted", painted());
		assertNull("back at the rested spot the hold is over, and the new line has no card", frameAt(words, 407));
		assertFalse("nothing painted back at the rested spot", painted());
	}

	@Test
	public void aWheelTurnEndsTheHold()
	{
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int words = wordsX(LINE, 22, 34, 0);

		assertSame("on the words", claws, frameAt(words, 407));
		theChatMovesUp(visible);
		assertSame("the card is held", claws, frameAt(words, 407));
		assertTrue("the held card painted", painted());

		overlay.wheeled();
		assertNull("the wheel turned: the hold ends", frameAt(words, 407));
		assertFalse("nothing painted", painted());
		assertNull("and the wheel does not need turning again", frameAt(words, 407));
		assertFalse("still nothing painted", painted());
	}

	@Test
	public void leavingTheChatEndsTheHold()
	{
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int words = wordsX(LINE, 22, 34, 0);

		assertSame("on the words", claws, frameAt(words, 407));
		theChatMovesUp(visible);
		assertSame("the card is held", claws, frameAt(words, 407));

		assertNull("outside the area", frameAt(100, 207));
		assertFalse("nothing painted outside the area", painted());
		assertNull("back at the rested point the hold is over", frameAt(words, 407));
		assertFalse("nothing painted", painted());
	}

	@Test
	public void aClickedCardHoldsThroughTheShiftToo()
	{
		when(config.cardTrigger()).thenReturn(ClanHubConfig.CardTrigger.CLICK);
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int words = wordsX(LINE, 22, 34, 0);

		overlay.clicked();
		frameAt(words, 407);
		assertTrue("the click opens the card", painted());

		theChatMovesUp(visible);
		frameAt(words, 407);
		assertTrue("the clicked card holds through the shift", painted());
		frameAt(words, 407);
		assertTrue("and on the next frame", painted());

		overlay.wheeled();
		frameAt(words, 407);
		assertFalse("the wheel turned: the card is gone", painted());
		frameAt(words, 407);
		assertFalse("and it needs a new click to come back", painted());
	}

	@Test
	public void theHoldDoesNotStartOnAMove()
	{
		final Widget visible = line(new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
		final int words = wordsX(LINE, 22, 34, 0);

		assertSame("on the words", claws, frameAt(words, 407));
		assertTrue("the card painted", painted());

		theChatMovesUp(visible);
		assertNull("the shift came with a move of a pixel: no hold", frameAt(words + 1, 407));
		assertFalse("nothing painted", painted());
	}

	@Test
	public void aLeadingIconMovesTheUnderlineAndTheHit()
	{
		final IndexedSprite icon = mock(IndexedSprite.class);
		when(icon.getWidth()).thenReturn(11);
		when(client.getModIcons()).thenReturn(new IndexedSprite[] {null, null, icon});
		final String level = "aetzli has reached Crafting level 50.";
		final int start = level.indexOf("Crafting");
		final InfoCard quiet = new InfoCard("Crafting level 50", Collections.singletonList(
			new HoverCard.Pair("Skill", "Crafting", "")), Collections.<String>emptyList(), "Crafting", level, start,
			start + "Crafting level 50".length(), 199, -1, false);
		final CardStore store = new CardStore();
		store.put(level, Collections.<HoverCard>singletonList(quiet));
		overlay.attach(store);
		final int lineX = 7;
		final Widget drawn = lineSaying("<img=2> " + level, new Rectangle(lineX, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {drawn});
		final Color orange = new Color(220, 138, 0);
		when(config.alwaysUnderlineLevels()).thenReturn(true);
		when(config.alwaysUnderlineColour()).thenReturn(orange);
		// The game draws the icon (11 px) and a space before "aetzli", which the plain text leaves out.
		final FontMetrics fm = metrics();
		final int from = lineX + 11 + fm.stringWidth(" ") + fm.stringWidth(level.substring(0, start));

		assertNull("the mouse outside the area", frameAt(100, 207));
		assertEquals("the underline starts after the icon and its space", orange.getRGB(), canvas.getRGB(from, 414));
		assertEquals("and not before", 0, canvas.getRGB(from - 1, 414) >>> 24);
		assertSame("the mouse a little inside the words", quiet, frameAt(from + 3, 407));
		assertNull("the mouse 11 px left of them, where the icon pushes them from", frameAt(from - 11, 407));
	}

	/**
	 * Lot 15A: while a picture theme is the card theme the card is Medium and opens at the cursor, whatever Card width and
	 * Card place say; with an original theme the player's own settings apply again, untouched.
	 */
	@Test
	public void aPictureThemeMakesTheCardMediumAtTheCursor()
	{
		final PictureTheme theme = PictureFixtures.theme("lock", "Calm", "bright", false, 0.5, 0.5, false);
		PictureThemes.install(PictureFixtures.catalogue(
			PictureFixtures.loader(new Color(30, 90, 60), new java.util.HashMap<String, Integer>(), new java.util.ArrayList<String>()),
			theme));
		try
		{
			final Widget frame = mock(Widget.class);
			when(frame.isHidden()).thenReturn(false);
			when(frame.getBounds()).thenReturn(new Rectangle(0, 340, 519, 142));
			when(client.getWidget(InterfaceID.Chatbox.UNIVERSE)).thenReturn(frame);
			final Widget visible = line(new Rectangle(0, 400, 500, 14));
			when(area.getDynamicChildren()).thenReturn(new Widget[] {visible});
			final int words = wordsX(LINE, 22, 34, 0);
			when(config.cardWidth()).thenReturn(ClanHubConfig.CardWidth.WIDE);
			when(config.cardPlace()).thenReturn(ClanHubConfig.CardPlace.CHAT);

			when(config.pictureTheme()).thenReturn("lock");
			assertSame("the card is hit", claws, frameAt(words, 407));
			final Rectangle picture = paintedBox(414);
			assertEquals("Medium (409 px) though Card width says Wide", 409, picture.width);
			assertEquals("at the cursor though Card place says above the chat", Math.max(0, words - 409 / 2), picture.x);
			assertEquals("its bottom row is 400, 6 px above the mouse", 400, picture.y + picture.height - 1);
			assertEquals("the picture's own edge ring is the card's corner", 0xFFFF0000, canvas.getRGB(picture.x, picture.y));
			assertEquals("the card image the sidebar shows is Medium too", 409, overlay.cardImage(claws).getWidth());

			when(config.pictureTheme()).thenReturn("");
			assertSame("the card is hit", claws, frameAt(words, 407));
			final Rectangle original = paintedBox(414);
			assertEquals("an original theme: the chatbox's width again", 519, original.width);
			assertEquals("and its left edge", 0, original.x);
		}
		finally
		{
			PictureThemes.install(PictureFixtures.catalogue(null));
		}
	}

	/** Lot 15A: a picture theme whose picture is missing makes a Classic card, without a throw. */
	@Test
	public void aMissingPictureMakesAClassicCard()
	{
		final PictureTheme theme = PictureFixtures.theme("gone", "Calm", "bright", false, 0.5, 0.5, false);
		PictureThemes.install(PictureFixtures.catalogue(PictureFixtures.loader(Color.BLACK, new java.util.HashMap<String, Integer>(),
			Arrays.asList(PictureFixtures.path("gone", 150), PictureFixtures.path("gone", 200), PictureFixtures.path("gone", 250))),
			theme));
		try
		{
			when(config.pictureTheme()).thenReturn("gone");
			final BufferedImage image = overlay.cardImage(claws);
			assertEquals("the Classic card's black edge", 0xFF000000, image.getRGB(0, 0));
		}
		finally
		{
			PictureThemes.install(PictureFixtures.catalogue(null));
		}
	}
}
