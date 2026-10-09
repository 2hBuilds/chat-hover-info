package com.clanhub;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the card while the mouse is on the card's words in a chat line, and, while the mouse is anywhere on a line that
 * holds cards, underlines the words of every quiet card on it in the hover colour (the hover-reveal; the quiet level, total
 * level and XP phrases, the kill count and personal best answers and the boss of a drop line show themselves this way; a
 * blue word is its own signal and carries no underline). With "Underline level-ups" on, those quiet phrases carry a line in
 * the underline colour on every visible chat line, mouse or no mouse. The "Card place" setting says where: the split card centred above the cursor, or the wide card as wide as
 * the chatbox and pinned above its frame. It finds the line by walking the chatbox's line widgets for the one under the mouse
 * whose plain text holds a line the plugin has cards for, and a card shows only while the mouse is on that card's own
 * words, not elsewhere on the row, with one exception, the hold: when the chat moves under a mouse that has not moved
 * (a new line arrives and the rows shift up) the card stays where it is until the mouse moves more than {@link #HOLD_JITTER}
 * px from where it rested, the mouse wheel turns, or the mouse leaves the chat's area. A line the game wrapped into rows is one widget, so it is read row by row
 * ({@link ChatRows}): a card's words are hit and underlined on the row they fall on, a card whose words break across
 * the rows gets a piece on each. Only while the mouse is inside the chat's scroll area: a line scrolled out of the
 * chat is not hidden, its rectangle sits over the game world, so outside the area the card and the hover-reveal are off
 * whatever the lines say, and the always-underline keeps to the lines whose underline row is inside the area. The card
 * waits on {@link CardGate}: for the show mode (on hover; while the plugin's key is held, which the plugin
 * tells through {@link #keyHeld}; or on a click, which it tells through {@link #clicked}: the card then opens on the
 * words that were clicked and stays while the mouse is on them) and for the delay since the mouse came onto the words.
 * A card with notes of its own shows them as its description; a card with none gets the first paragraph of its wiki page
 * while "Wiki descriptions" is on, and asks for it again if it has not come yet. A line RuneLite rewrites after it arrives
 * (the Chat Commands plugin's kill count and personal best answers, which no chat event ever carries) is read as drawn
 * when the mouse reaches it ({@link ChatCommandReader}), and from then on it is a stored line like any other, so the
 * always-underline reaches it only after that. The card of a boss, monster, raid or chest shows the wiki's own picture of
 * its page in its big picture box while "Wiki pictures" is on, asked for as the card is hovered if the line's arrival
 * has not asked yet, with the card's own item or sprite in the box until the picture comes. It never changes the chat text.
 * Everything here runs on the client thread, in the draw call.
 */
public class ChatCardOverlay extends Overlay
{
	/** A chat line is one text row at most this tall; a taller widget is rows, or a line the game broke where we did not. */
	private static final int ONE_ROW = 16;
	/** The mouse has to move more than this many px before the line widgets are walked again. */
	private static final int MOVE_THRESHOLD = 3;
	/** A place the mouse never is, so the first frame always walks. */
	private static final int NOWHERE = -1000;
	/** A held card lets the mouse drift this many px, on either axis, from where it rested, before the hold ends. */
	private static final int HOLD_JITTER = 2;

	private final Client client;
	private final ClanHubConfig config;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	/** A scrap of canvas the card is painted on to learn its size before it is placed. */
	private final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	/**
	 * The plugin's cards by plain line. The store matches the text that HOLDS a card's line, because the chatbox puts the
	 * channel's prefix, and a timestamp another plugin adds, in the same widget.
	 *
	 * <p>Choice: an empty store of its own until the plugin hands over its own, so the overlay never meets null.
	 */
	private CardStore store = new CardStore();
	/**
	 * The wiki's first paragraphs, handed over by the plugin at start-up; null means the cards show no paragraph.
	 *
	 * <p>Choice: volatile, because start-up writes it on a thread other than the client thread that reads it.
	 */
	private volatile WikiSummaries summaries;
	/**
	 * The wiki's pictures of the boss, monster, raid and chest pages, handed over by the plugin at start-up; null means the
	 * cards show their own icon.
	 *
	 * <p>Choice: volatile, like {@link #summaries}.
	 */
	private volatile WikiPictures pictures;
	/**
	 * The bosses' facts, handed over by the plugin at start-up, for the cards of the Chat Commands answers.
	 *
	 * <p>Choice: a table of its own that has not loaded until the plugin hands over its own, so an answer read before then
	 * gives its card without facts and the overlay never meets null; volatile, like {@link #summaries}.
	 */
	private volatile CardTables tables = new CardTables();
	/** The card under the mouse, for the menu; null when the mouse is off a card line. */
	private volatile HoverCard hovered;
	/** Where the line widgets were last walked from, and what the walk found. */
	private int walkedX = NOWHERE;
	private int walkedY = NOWHERE;
	private Widget hitLine;
	private CardStore.Match hitMatch;
	private HoverCard hitCard;
	/** The line the mouse is on that holds cards, whether or not the mouse is on a card's words; null when there is none. */
	private Widget rowLine;
	private CardStore.Match rowMatch;
	/** When the mouse came onto the hit card's words, in {@link System#nanoTime} terms. */
	private long hitSince;
	/** Whether the plugin's card key is held, set from the key listener's thread. */
	private volatile boolean keyHeld;
	/** Whether a click has landed since the last frame, set from the mouse listener's thread. */
	private volatile boolean clickPending;
	/** The card whose words were clicked last, kept while the mouse stays on them; null when nothing is clicked open. */
	private HoverCard clickedCard;
	/** Where the mouse was at the previous frame inside the chat's area, {@link #NOWHERE} when it was outside or off. */
	private int lastX = NOWHERE;
	private int lastY = NOWHERE;
	/** The card shown at the last frame that showed one, null when the mouse has been off every card since. */
	private HoverCard held;
	/** When the mouse came onto the held card's words, as {@link #hitSince} counts it. */
	private long heldSince;
	/** Where the mouse was when the held card was last hit on its words. */
	private int heldX;
	private int heldY;
	/** Whether the card the last frame showed was the held one, the chat having moved off its words under a still mouse. */
	private boolean holding;
	/**
	 * Whether the mouse wheel has turned since the last frame, set from the wheel listener's thread.
	 *
	 * <p>Choice: volatile, because the listener writes it on a thread other than the client thread that reads it.
	 */
	private volatile boolean wheelTurned;

	@Inject
	ChatCardOverlay(Client client, ClanHubConfig config, ItemManager itemManager, SpriteManager spriteManager)
	{
		this.client = client;
		this.config = config;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGH);
	}

	/** Hands the overlay the plugin's cards and forgets what it found before: called when the plugin starts. */
	void attach(CardStore store)
	{
		this.store = store;
		forget();
		walkedX = NOWHERE;
		walkedY = NOWHERE;
	}

	/**
	 * Hands the overlay the wiki's first paragraphs, which a card shows under its rows while "Wiki descriptions" is on;
	 * null for none: called when the plugin starts.
	 */
	void summaries(WikiSummaries summaries)
	{
		this.summaries = summaries;
	}

	/**
	 * Hands the overlay the wiki's pictures, which the cards of a boss, monster, raid or chest show in their big picture box
	 * while "Wiki pictures" is on; null for none: called when the plugin starts, beside {@link #summaries}.
	 */
	void pictures(WikiPictures pictures)
	{
		this.pictures = pictures;
	}

	/**
	 * Hands the overlay the bosses' table, whose facts the cards of the Chat Commands answers carry: called when
	 * the plugin starts, beside {@link #attach} and {@link #summaries}.
	 */
	void tables(CardTables tables)
	{
		this.tables = tables;
	}

	/** The card under the mouse as of the last frame, or null; what the menu reads. */
	HoverCard hovered()
	{
		return hovered;
	}

	/** Tells the overlay whether the card key is held: the plugin's card key listener calls it on press and release, and when the window loses focus. */
	void keyHeld(boolean held)
	{
		keyHeld = held;
	}

	/** Tells the overlay a left click landed: the plugin's mouse listener calls it, and the next frame reads where. */
	void clicked()
	{
		clickPending = true;
	}

	/**
	 * Tells the overlay the mouse wheel turned, anywhere: the plugin's wheel listener calls it, and the next frame ends the
	 * hold of a card, a wheel turn counting as a move of the mouse.
	 */
	void wheeled()
	{
		wheelTurned = true;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!config.showCards())
		{
			forget();
			return null;
		}
		final Widget area = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		final Rectangle areaBounds = area == null || area.isHidden() ? null : area.getBounds();
		// The chat font's metrics: the card is picked and the underline drawn with the same ones, so the two agree.
		final FontMetrics metrics = g.getFontMetrics(FontManager.getRunescapeFont());
		// Choice: the always-underline is drawn before the mouse is looked at, so it shows with the mouse anywhere, or
		// nowhere; the frame then goes on as it always did.
		if (areaBounds != null && config.alwaysUnderlineLevels())
		{
			underlineQuiet(g, area, areaBounds, metrics);
		}
		final net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		if (mouse == null)
		{
			forget();
			return null;
		}
		final int mouseX = mouse.getX();
		final int mouseY = mouse.getY();
		// Choice: nothing outside the chat's scroll area. The lines scrolled out of the chat are still its children, with
		// rectangles over the game world, and NOWHERE makes the first frame back inside walk again.
		if (areaBounds == null || !areaBounds.contains(mouseX, mouseY))
		{
			forget();
			walkedX = NOWHERE;
			walkedY = NOWHERE;
			return null;
		}

		final boolean moved = Math.abs(mouseX - walkedX) > MOVE_THRESHOLD || Math.abs(mouseY - walkedY) > MOVE_THRESHOLD;
		if (moved || (hitLine != null && !stillHit(mouseX, mouseY, metrics))
			|| (rowLine != null && !stillRow(mouseX, mouseY)))
		{
			final HoverCard before = hitCard;
			final long beforeSince = hitSince;
			walkedX = mouseX;
			walkedY = mouseY;
			walk(area, mouseX, mouseY, metrics);
			// Choice: the delay counts from when the mouse came onto the words, so a walk that finds the same card again
			// keeps the time the first one began, and a small move on the words does not start the wait over.
			hitSince = hitCard != null && hitCard == before ? beforeSince : System.nanoTime();
		}
		// Choice: a click on the words keeps that card open and a click anywhere else (no hit) closes it; the mouse moving
		// off the words, onto another card or none, closes it too, so coming back needs a new click.
		if (clickPending)
		{
			clickPending = false;
			clickedCard = hitCard;
		}
		// Choice: the hold. When the card is lost to the chat moving under a mouse that has not moved at all since the
		// frame before (a new line arrives and the rows shift up), the card shown stays the held one while the hand rests:
		// within HOLD_JITTER px of where it rested, no wheel turn. A move off the words is the player's doing and drops the
		// card, even by a pixel, so the hold begins only on an unmoved mouse; the chat shifting is not. The walk's own
		// result (hitCard, hitSince and the row) is left as it is, so the walk keeps its bookkeeping and the hover-reveal
		// keeps following the line under the pointer; the card shown is a local.
		HoverCard shown = hitCard;
		long shownSince = hitSince;
		final boolean still = mouseX == lastX && mouseY == lastY;
		final boolean near = Math.abs(mouseX - heldX) <= HOLD_JITTER && Math.abs(mouseY - heldY) <= HOLD_JITTER;
		if (held != null && shown != held && !wheelTurned && (holding ? near : still))
		{
			shown = held;
			shownSince = heldSince;
			holding = true;
		}
		else
		{
			holding = false;
			if (shown != null)
			{
				held = shown;
				heldSince = shownSince;
				heldX = mouseX;
				heldY = mouseY;
			}
			else
			{
				held = null;
			}
		}
		wheelTurned = false;
		lastX = mouseX;
		lastY = mouseY;
		// The clicked card stays clicked while it is the card shown, held or hit.
		if (clickedCard != shown)
		{
			clickedCard = null;
		}
		hovered = shown;
		// Choice: the hover-reveal, before the gate: the mouse anywhere on a line that holds cards underlines the words of
		// every quiet card on it, whatever the show mode and the delay say about the card. A marked card's words are
		// already coloured in the chat, so they carry no underline of their own while the colouring is on.
		if (rowLine != null)
		{
			for (final HoverCard card : rowMatch.cards)
			{
				if (!card.marked() || !config.colourHoverables())
				{
					underline(g, rowLine, card, rowMatch.offset, metrics, config.hoverColour());
				}
			}
		}
		final boolean clicked = shown != null && shown == clickedCard;
		if (shown == null
			|| !CardGate.shows(config.cardTrigger(), keyHeld, clicked, shownSince, System.nanoTime(), config.cardDelayMs()))
		{
			return null;
		}

		final BufferedImage picture = pictureOf(shown);
		final int fontSize = config.cardFontSize();
		final Graphics2D measure = scratch.createGraphics();
		measure.setRenderingHints(g.getRenderingHints());
		if (config.cardPlace() == ClanHubConfig.CardPlace.CHAT)
		{
			// Choice: the chatbox's whole frame sets the wide card's width and place, the scroll area's bounds when the
			// frame is not shown.
			final Rectangle chat = chatFrame(areaBounds);
			final String description = descriptionOf(shown);
			final Color accent = config.hoverColour();
			final Dimension size = CardPainter.paintWide(measure, shown, picture, fontSize, chat.width, 0, 0, description,
				accent);
			measure.dispose();
			final Rectangle at = CardPainter.placeAboveChat(chat, size);
			CardPainter.paintWide(g, shown, picture, fontSize, chat.width, at.x, at.y, description, accent);
			return null;
		}
		final String description = descriptionOf(shown);
		final ClanHubConfig.CardWidth width = config.cardWidth();
		final Dimension size = CardPainter.paintSplit(measure, shown, picture, fontSize, 0, 0, description, width.cap,
			width.lines);
		measure.dispose();
		final Rectangle at = CardPainter.placeCentred(new Point(mouseX, mouseY), size,
			new Dimension(client.getCanvasWidth(), client.getCanvasHeight()));
		CardPainter.paintSplit(g, shown, picture, fontSize, at.x, at.y, description, width.cap, width.lines);
		return null;
	}

	/** The bounds of the chatbox's frame, or {@code fallback} when the frame is not there or not shown. */
	private Rectangle chatFrame(Rectangle fallback)
	{
		final Widget frame = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		if (frame != null && !frame.isHidden())
		{
			final Rectangle bounds = frame.getBounds();
			if (bounds != null)
			{
				return bounds;
			}
		}
		return fallback;
	}

	/**
	 * The text of the card's description, which the painter lays out in the card: the card's own notes when it has any,
	 * else the first paragraph of its wiki page, else "". One note is the text alone, a paragraph; two or more are joined
	 * with a line break, which the painter sets as a numbered list, one note to an item. "" for the paragraph when the
	 * "Wiki descriptions" setting is off, when no summaries were handed over, and when it has not come yet, in which case
	 * it is asked for again, which does nothing while the first call is still waiting.
	 */
	private String descriptionOf(HoverCard card)
	{
		final List<String> notes = card.notes();
		if (notes.size() == 1)
		{
			return notes.get(0);
		}
		if (notes.size() > 1)
		{
			return String.join("\n", notes);
		}
		final WikiSummaries asked = summaries;
		if (asked == null || !config.wikiDescriptions())
		{
			return "";
		}
		final String paragraph = asked.summaryOf(card.wikiTitle());
		if (paragraph == null)
		{
			asked.fetch(card.wikiTitle());
			return "";
		}
		return paragraph;
	}

	/**
	 * The card's picture: for a card that wants the wiki's own, while "Wiki pictures" is on, the one kept for its page, and
	 * when none is kept the page is asked for again, which does nothing while the first call is waiting or the page is
	 * known to have none; then the item's sprite, else the game's own sprite, else none.
	 */
	private BufferedImage pictureOf(HoverCard card)
	{
		final WikiPictures asked = pictures;
		if (card.wikiPicture() && asked != null && config.wikiPictures())
		{
			final BufferedImage wiki = asked.pictureOf(card.wikiTitle());
			if (wiki != null)
			{
				return wiki;
			}
			asked.fetch(card.wikiTitle());
		}
		if (card.itemId() >= 0)
		{
			return itemManager.getImage(card.itemId(), card.quantity(), card.quantity() > 1);
		}
		if (card.spriteId() >= 0)
		{
			return spriteManager.getSprite(card.spriteId(), 0);
		}
		return null;
	}

	/**
	 * Lets go of the last hit, and of a click not yet read or a card opened by one: a click that lands while the mouse is
	 * outside the chat's scroll area, or the cards are off, counts as a click elsewhere and must not open a card later.
	 * It lets go of the held card too, and of where the mouse was, so the first frame back is never a still one.
	 */
	private void forget()
	{
		clearHit();
		clickPending = false;
		clickedCard = null;
		held = null;
		holding = false;
		lastX = NOWHERE;
		lastY = NOWHERE;
	}

	/** Lets go of the last hit alone: the walk starts from here, and a click's card is kept while the mouse stays on it. */
	private void clearHit()
	{
		hitLine = null;
		hitMatch = null;
		hitCard = null;
		rowLine = null;
		rowMatch = null;
		hovered = null;
	}

	/**
	 * Whether the last hit's line is still under the mouse, still says what it said, and the mouse is still on the hit
	 * card's own words: a move of a pixel or two off them, under the walk threshold, drops the hit.
	 */
	private boolean stillHit(int mouseX, int mouseY, FontMetrics metrics)
	{
		final Rectangle bounds = hitLine.getBounds();
		if (hitLine.isHidden() || bounds == null || !bounds.contains(mouseX, mouseY))
		{
			return false;
		}
		final String plain = plain(hitLine);
		if (!holds(plain, hitCard))
		{
			return false;
		}
		final int offset = plain.indexOf(hitCard.plainLine());
		return ChatRows.of(plain, metrics::stringWidth, bounds).pick(hitMatch.cards, offset, mouseX, mouseY) == hitCard;
	}

	/**
	 * Whether the last row's line is still under the mouse and still says the stored line it said: a line that scrolled
	 * away, or was replaced by a new message, drops the row.
	 */
	private boolean stillRow(int mouseX, int mouseY)
	{
		final Rectangle bounds = rowLine.getBounds();
		if (rowLine.isHidden() || bounds == null || !bounds.contains(mouseX, mouseY))
		{
			return false;
		}
		final String plain = plain(rowLine);
		final CardStore.Match now = plain == null ? null : store.find(plain);
		return now != null && now.cards == rowMatch.cards && now.offset == rowMatch.offset;
	}

	/** Whether a widget's plain text holds the card's line. */
	private static boolean holds(String text, HoverCard card)
	{
		return text != null && text.contains(card.plainLine());
	}

	/**
	 * Walks the chatbox's line widgets, the dynamic children of the scroll area {@code render} found the mouse inside, and
	 * then the static ones, for the first that is shown, has the mouse inside its bounds and says a line the plugin has
	 * cards for (the row), and for the first of those whose card's words hold the mouse (the hit). With the mouse inside
	 * the area, a line scrolled out of it cannot hold the mouse, and a line half scrolled hits only in the part that shows.
	 */
	private void walk(Widget area, int mouseX, int mouseY, FontMetrics metrics)
	{
		clearHit();
		if (!find(area.getDynamicChildren(), mouseX, mouseY, metrics))
		{
			find(area.getStaticChildren(), mouseX, mouseY, metrics);
		}
	}

	/**
	 * Looks through one group of widgets; true when the hit is set. A widget is a hit only when the mouse is on the words
	 * of one of its cards, so the hit's line, match and card are always set together or not at all. The row is the first
	 * widget the mouse is on that says a line the plugin has cards for, set even when the mouse is not on any card's words
	 * in it; a hit makes its own widget the row. A widget the store has nothing for is read once as a line RuneLite
	 * rewrote after it arrived ({@link #readLate}), and a card found that way is stored, so it is a stored line from then on.
	 */
	private boolean find(Widget[] widgets, int mouseX, int mouseY, FontMetrics metrics)
	{
		if (widgets == null)
		{
			return false;
		}
		for (final Widget widget : widgets)
		{
			if (widget == null || widget.isHidden())
			{
				continue;
			}
			final Rectangle bounds = widget.getBounds();
			if (bounds == null || !bounds.contains(mouseX, mouseY))
			{
				continue;
			}
			final String plain = plain(widget);
			CardStore.Match match = plain == null ? null : store.find(plain);
			if (match == null && plain != null)
			{
				match = readLate(plain);
			}
			if (match == null)
			{
				continue;
			}
			if (rowLine == null)
			{
				rowLine = widget;
				rowMatch = match;
			}
			final HoverCard card = ChatRows.of(plain, metrics::stringWidth, bounds).pick(match.cards, match.offset, mouseX,
				mouseY);
			if (card != null)
			{
				rowLine = widget;
				rowMatch = match;
				hitLine = widget;
				hitMatch = match;
				hitCard = card;
				return true;
			}
		}
		return false;
	}

	/**
	 * The match for a drawn text the store did not know, read as an answer of RuneLite's Chat Commands plugin
	 * ({@link ChatCommandReader}): the card it gives is stored under the text and the store's own match is returned, at
	 * offset 0; null when the text is no such answer.
	 *
	 * <p>Choice: the text is stored whole, as drawn, so {@code stillRow} and {@code stillHit} find the line as they find
	 * any other, and a text that is no answer is read again on the next walk, which is a pattern match and no more.
	 */
	private CardStore.Match readLate(String plain)
	{
		final List<HoverCard> cards = ChatCommandReader.read(plain, tables);
		if (cards.isEmpty())
		{
			return null;
		}
		store.put(plain, cards);
		return store.find(plain);
	}

	/**
	 * A line widget's text the way the card store keys it, {@link ChatText#plain}: without its tags and with a
	 * non-breaking space written as a plain one (the game draws a player's name with the first, the stored line may have
	 * the second); null when the widget has no text.
	 */
	private static String plain(Widget widget)
	{
		final String text = widget.getText();
		return text == null ? null : ChatText.plain(text);
	}

	/**
	 * The always-underline: for each shown line of the scroll area, the dynamic children and then the static ones, that is
	 * in the visible part of the area and says a line the plugin has cards for, a 1 px underline under the words of each
	 * of its quiet cards, in the always-underline colour.
	 *
	 * <p>Choice: a line whose underline row is outside the area is a line scrolled out of the chat (its rectangle sits over
	 * the game world) and gets none.
	 */
	private void underlineQuiet(Graphics2D g, Widget area, Rectangle areaBounds, FontMetrics metrics)
	{
		final Color colour = config.alwaysUnderlineColour();
		underlineQuiet(g, area.getDynamicChildren(), areaBounds, colour, metrics);
		underlineQuiet(g, area.getStaticChildren(), areaBounds, colour, metrics);
	}

	/** One group of the scroll area's children, for the always-underline. */
	private void underlineQuiet(Graphics2D g, Widget[] widgets, Rectangle areaBounds, Color colour, FontMetrics metrics)
	{
		if (widgets == null)
		{
			return;
		}
		for (final Widget widget : widgets)
		{
			if (widget == null || widget.isHidden())
			{
				continue;
			}
			final Rectangle bounds = widget.getBounds();
			if (bounds == null || !areaBounds.intersects(bounds)
				|| bounds.y + bounds.height >= areaBounds.y + areaBounds.height)
			{
				continue;
			}
			final String plain = plain(widget);
			final CardStore.Match match = plain == null ? null : store.find(plain);
			if (match == null)
			{
				continue;
			}
			for (final HoverCard card : match.cards)
			{
				if (!card.marked())
				{
					underline(g, widget, card, match.offset, metrics, colour);
				}
			}
		}
	}

	/**
	 * A 1 px underline, 1 px under the text box of each row the card's words fall on, from where the words start to where
	 * they end on that row: the widget's left edge plus the width of the plain text before them on their row in the
	 * game's chat font, the stored line's place in the text ({@code offset}) added to the card's own. A line of one row
	 * gets one, under its box; a wrapped line ({@link ChatRows}) gets a piece on each row the words touch. It is drawn in
	 * {@code colour}.
	 *
	 * <p>Choice: when the rows come to one but the widget is taller than a row, the game broke the text where this wrap
	 * did not (an {@code <img>} rank icon's width, say), the rows cannot be told, and the line gets the card and no
	 * underline.
	 */
	private void underline(Graphics2D g, Widget line, HoverCard card, int offset, FontMetrics metrics, Color colour)
	{
		final Rectangle bounds = line.getBounds();
		if (bounds == null)
		{
			return;
		}
		final String text = plain(line);
		if (text == null || offset + card.end() > text.length())
		{
			return;
		}
		final ChatRows rows = ChatRows.of(text, metrics::stringWidth, bounds);
		if (rows.rows() == 1 && bounds.height > ONE_ROW)
		{
			return;
		}
		g.setColor(colour);
		for (final int[] piece : rows.spans(offset + card.start(), offset + card.end()))
		{
			g.fillRect(piece[0], piece[2], piece[1] - piece[0], 1);
		}
	}
}
