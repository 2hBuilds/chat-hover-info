package com.clanhub;

import com.google.gson.Gson;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ChatCardOverlay} and the slayer task answer (lot 14B): a line RuneLite's Slayer plugin wrote after it arrived, the
 * demo's among them, is read from the drawn widget when the mouse reaches it, and gives its card on the task's name only.
 */
public class SlayerCardOverlayTest
{
	private static final String DRAWN = DemoLines.SLAYER.get(0).name + ": " + DemoLines.SLAYER.get(0).text;

	private Client client;
	private ChatCardOverlay overlay;
	private CardStore store;
	private Widget area;
	private BufferedImage canvas;

	@Before
	public void setUp() throws Exception
	{
		client = mock(Client.class);
		area = mock(Widget.class);
		when(area.isHidden()).thenReturn(false);
		when(area.getBounds()).thenReturn(new Rectangle(0, 340, 519, 140));
		when(client.getWidget(InterfaceID.Chatbox.SCROLLAREA)).thenReturn(area);
		when(client.getCanvasWidth()).thenReturn(765);
		when(client.getCanvasHeight()).thenReturn(503);

		final ClanHubConfig config = mock(ClanHubConfig.class);
		when(config.showCards()).thenReturn(true);
		when(config.cardTrigger()).thenReturn(ClanHubConfig.CardTrigger.HOVER);
		when(config.cardDelayMs()).thenReturn(0);
		when(config.cardFontSize()).thenReturn(16);
		when(config.hoverColour()).thenReturn(new Color(59, 130, 246));
		when(config.colourHoverables()).thenReturn(true);
		when(config.cardWidth()).thenReturn(ClanHubConfig.CardWidth.WIDE);

		store = new CardStore();
		overlay = new ChatCardOverlay(client, config, mock(ItemManager.class), mock(SpriteManager.class));
		overlay.attach(store);
		final CardTables tables = CardTables.parse(new StringReader("{}"), new Gson());
		try (Reader in = new InputStreamReader(SlayerCardOverlayTest.class.getResourceAsStream(
			"/com/clanhub/slayer-fixture.json"), StandardCharsets.UTF_8))
		{
			tables.readSlayer(in, new Gson());
		}
		overlay.tables(tables);
		canvas = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
	}

	/** The x of the middle of the words {@code start} to {@code end} of {@code line}, drawn from x 0. */
	private int wordsX(String line, int start, int end)
	{
		final Graphics2D g = canvas.createGraphics();
		final FontMetrics fm = g.getFontMetrics(FontManager.getRunescapeFont());
		g.dispose();
		return fm.stringWidth(line.substring(0, start)) + fm.stringWidth(line.substring(start, end)) / 2;
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

	@Test
	public void aSlayerTaskLineIsReadWhenTheMouseReachesIt()
	{
		assertNull("the store holds nothing for the line: it is no chat event's", store.find(DRAWN));
		final Widget rewritten = lineSaying(DRAWN, new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {rewritten});

		final int words = DRAWN.indexOf("Hellhounds");
		final HoverCard card = frameAt(wordsX(DRAWN, words, words + "Hellhounds".length()), 407);
		assertNotNull("a card was read from the drawn text", card);
		assertEquals("Hellhounds", card.title());
		// Re-aimed by the lead, 2026-10-28: the user's layout pick: the progress is the bar's words, not a fact.
		assertNotNull(card.progress());
		assertEquals("10/155", card.progress().text);
		final CardStore.Match stored = store.find(DRAWN);
		assertNotNull("and it is stored from now on", stored);
		assertEquals(0, stored.offset);

		// x 120, not 20: a move under the walk threshold off the old widget's words would keep the old hit.
		final Widget other = lineSaying("Klar: Slayer Task: Banana men: 1/2 killed", new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {other});
		assertNull("a task the table lacks has no card", frameAt(120, 407));
		assertNull("and nothing is stored for it", store.find("Klar: Slayer Task: Banana men: 1/2 killed"));
	}

	@Test
	public void theCardIsOnTheTasksNameOnly()
	{
		final Widget rewritten = lineSaying(DRAWN, new Rectangle(0, 400, 500, 14));
		when(area.getDynamicChildren()).thenReturn(new Widget[] {rewritten});
		assertNull("the sender's name is not the task", frameAt(wordsX(DRAWN, 0, 4), 407));
		final int killed = DRAWN.indexOf("killed");
		assertNull("and neither is the count", frameAt(wordsX(DRAWN, killed, killed + 6), 407));
	}
}
