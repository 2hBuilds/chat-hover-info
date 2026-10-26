package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.runelite.client.ui.ColorScheme;

/**
 * What the sidebar paints with: the colours of its ground, its titles, its words and its rows, and the look of its three
 * boxes (the top card, the how-to's bar and a recent card's row). Pure: a theme in, a skin out, and the painting of a
 * box on a {@link Graphics2D} handed in; {@link ClanHubPanel} draws the sidebar's content in these colours.
 *
 * <p>{@link #PLAIN} is the sidebar as it always was (RuneLite's own greys, flat boxes), which Classic gives. Stone, Void
 * and Wiki dress the boxes as their cards are dressed (Stone's frame, Void's gradient and stars, Wiki's beige and thin
 * brown edge); each of the eight space themes and the twelve scene themes paints a calm version of its sky (the
 * ground's gradient, a soft corner glow, the edge) into the top card and the rows, and its top colour into the bar. The words carry no shadow and no glow
 * (they are Swing labels), so the colours are the whole of a skin's words.
 */
final class SidebarSkin
{
	/** How strong the calm sky boxes' corner glow is at its brightest. */
	private static final int CALM_GLOW_ALPHA = 60;

	/** The three boxes the sidebar paints: the top card, the how-to's title bar and a recent card's row. */
	enum Box
	{
		CARD,
		BAR,
		ROW
	}

	/** How a skin paints a box of {@code width} by {@code height} px at the graphics' origin. */
	private interface Painter
	{
		void paint(Graphics2D g, int width, int height, Box kind);
	}

	/** The Stone row's and bar's own colours, from the sidebar mock-up. */
	private static final Color STONE_ROW_LINE = new Color(110, 98, 80);
	private static final Color STONE_BAR = new Color(45, 39, 31);
	private static final Color STONE_BAR_EDGE = new Color(30, 26, 20);
	private static final Color STONE_BAR_LINE = new Color(100, 90, 72);

	/** The Void bar's gradient, top and bottom, and its edge. */
	private static final Color VOID_BAR_TOP = new Color(72, 34, 112);
	private static final Color VOID_BAR_BOTTOM = new Color(46, 20, 74);
	private static final Color VOID_BAR_EDGE = new Color(176, 118, 246);

	/** The Wiki boxes: the card, the row and the bar, and the edge of each. */
	private static final Color WIKI_CARD = new Color(234, 226, 210);
	private static final Color WIKI_CARD_EDGE = new Color(150, 135, 105);
	private static final Color WIKI_ROW = new Color(244, 238, 226);
	private static final Color WIKI_ROW_EDGE = new Color(180, 165, 135);
	private static final Color WIKI_BAR = new Color(194, 172, 136);

	/** What lies over a box under the mouse: a light veil on a dark skin, a dark one on Wiki's light ground. */
	private static final Color LIGHT_VEIL = new Color(255, 255, 255, 26);
	private static final Color DARK_VEIL = new Color(0, 0, 0, 24);

	/** The sidebar as it always was: RuneLite's own greys, flat boxes, and a lighter grey under the mouse. */
	static final SidebarSkin PLAIN = new SidebarSkin(ColorScheme.DARKER_GRAY_COLOR, Color.WHITE, Color.WHITE,
		ColorScheme.LIGHT_GRAY_COLOR, ClanHubPanel.HOW_TO_HEADING_COLOUR, null, ClanHubPanel.ROW_TIME_COLOUR,
		ColorScheme.DARK_GRAY_HOVER_COLOR, (g, width, height, kind) ->
		{
			g.setColor(kind == Box.BAR ? ClanHubPanel.HOW_TO_BAR_COLOUR : ColorScheme.DARK_GRAY_COLOR);
			g.fillRect(0, 0, width, height);
		});

	/** The twelve scene themes, Forest to Lavender; the other themes after Wiki are the space themes. */
	private static final Set<CardTheme> SCENES = EnumSet.range(CardTheme.FOREST, CardTheme.LAVENDER);

	/** Every theme's skin, by theme. */
	private static final Map<CardTheme, SidebarSkin> SKINS = new EnumMap<>(CardTheme.class);

	static
	{
		SKINS.put(CardTheme.STONE, new SidebarSkin(new Color(36, 31, 25), new Color(255, 152, 31), Color.WHITE,
			new Color(205, 188, 150), new Color(175, 160, 128), null, new Color(175, 160, 128), LIGHT_VEIL,
			SidebarSkin::stone));
		SKINS.put(CardTheme.VOID, new SidebarSkin(new Color(12, 6, 26), new Color(228, 194, 255), new Color(236, 226, 250),
			new Color(162, 142, 202), new Color(142, 122, 182), null, new Color(142, 122, 182), LIGHT_VEIL,
			SidebarSkin::voidLook));
		SKINS.put(CardTheme.WIKI, new SidebarSkin(new Color(222, 213, 193), new Color(38, 28, 16), new Color(30, 25, 15),
			new Color(110, 95, 70), new Color(120, 105, 80), new Color(42, 92, 172), new Color(120, 105, 80), DARK_VEIL,
			SidebarSkin::wiki));
		for (final CardTheme theme : CardTheme.values())
		{
			if (theme != CardTheme.CLASSIC && !SKINS.containsKey(theme))
			{
				final CardStyle style = CardStyle.of(theme);
				final boolean scene = SCENES.contains(theme);
				final Color page = scene ? SceneSky.groundBottom(theme) : SpaceSky.groundBottom(theme);
				SKINS.put(theme, new SidebarSkin(page, style.title, style.text, style.label, style.hint, null, style.hint,
					LIGHT_VEIL, (g, width, height, kind) -> sky(g, theme, scene, width, height, kind)));
			}
		}
	}

	/** The panel's own ground, behind the boxes, the list and the scroll pane. */
	final Color page;
	/** The words of the how-to bar and of the recent cards' header. */
	final Color title;
	/** The action line, the how-to's labels, and a row's title unless the skin sets {@link #rowTitle}. */
	final Color text;
	/** The how-to's second line and rest-of-row words, and the rows' kind, the count and the clear link. */
	final Color grey;
	/** The how-to's small heading. */
	final Color heading;
	/** A row's title: the skin's own colour for it, or {@link #text} when the skin sets none. */
	final Color rowTitle;
	/** A row's time. */
	final Color time;
	/** What is laid over a bar or a row while the mouse is on it. */
	final Color hot;
	private final Painter painter;

	private SidebarSkin(Color page, Color title, Color text, Color grey, Color heading, Color rowTitle, Color time,
		Color hot, Painter painter)
	{
		this.page = page;
		this.title = title;
		this.text = text;
		this.grey = grey;
		this.heading = heading;
		this.rowTitle = rowTitle == null ? text : rowTitle;
		this.time = time;
		this.hot = hot;
		this.painter = painter;
	}

	/** The skin of {@code theme}; Classic, and a null theme, give {@link #PLAIN}. */
	static SidebarSkin of(CardTheme theme)
	{
		final SidebarSkin skin = theme == null ? null : SKINS.get(theme);
		return skin == null ? PLAIN : skin;
	}

	/**
	 * Paints a box of {@code width} by {@code height} px with its top-left corner at the graphics' origin: the top card
	 * and a row as the card theme paints its card (a row of Stone without the band and the rivets), the bar in a shade of
	 * its own. The box's whole area is painted.
	 */
	void paintBox(Graphics2D g, int width, int height, Box kind)
	{
		painter.paint(g, width, height, kind);
	}

	/** Stone: the card is the card's frame, a row is its ground and speckle with the black edge and the light line alone. */
	private static void stone(Graphics2D g, int width, int height, Box kind)
	{
		if (kind == Box.CARD)
		{
			CardStyle.STONE.frame(g, 0, 0, width, height);
		}
		else if (kind == Box.ROW)
		{
			g.setColor(CardStyle.STONE.ground);
			g.fillRect(0, 0, width, height);
			CardStyle.STONE.lay(g, 0, 0, width, height);
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, width - 1, height - 1);
			g.setColor(STONE_ROW_LINE);
			g.drawRect(1, 1, width - 3, height - 3);
		}
		else
		{
			g.setColor(STONE_BAR);
			g.fillRect(0, 0, width, height);
			g.setColor(STONE_BAR_EDGE);
			g.drawRect(0, 0, width - 1, height - 1);
			g.setColor(STONE_BAR_LINE);
			g.fillRect(0, height - 1, width, 1);
		}
	}

	/** Void: the card and a row are the card's frame, the bar a lighter purple gradient with a light purple edge. */
	private static void voidLook(Graphics2D g, int width, int height, Box kind)
	{
		if (kind == Box.BAR)
		{
			g.setPaint(new GradientPaint(0, 0, VOID_BAR_TOP, 0, height, VOID_BAR_BOTTOM));
			g.fillRect(0, 0, width, height);
			g.setColor(VOID_BAR_EDGE);
			g.drawRect(0, 0, width - 1, height - 1);
		}
		else
		{
			CardStyle.VOID.frame(g, 0, 0, width, height);
		}
	}

	/** Wiki: beige boxes with a thin brown edge, a row lighter than the card, the bar darker. */
	private static void wiki(Graphics2D g, int width, int height, Box kind)
	{
		final Color fill = kind == Box.BAR ? WIKI_BAR : kind == Box.ROW ? WIKI_ROW : WIKI_CARD;
		final Color edge = kind == Box.ROW ? WIKI_ROW_EDGE : WIKI_CARD_EDGE;
		g.setColor(fill);
		g.fillRect(0, 0, width, height);
		g.setColor(edge);
		g.drawRect(0, 0, width - 1, height - 1);
	}

	/**
	 * A space or scene theme: the card and a row are a calm version of its sky, its ground's gradient with a soft glow of
	 * its title's glow colour in the top right corner and its edge colour round it; the bar is its ground's top colour
	 * with the edge round it.
	 *
	 * <p>Choice: the sky's own picture (a planet, a fence, gears, a black hole) is left out of the sidebar's boxes. It was
	 * drawn into them at first, and in a row 40 px tall it sat behind the title and the name and made them hard to read
	 * (the user's look at the renders, 2026-10-26); the card at the chat keeps it.
	 */
	private static void sky(Graphics2D g, CardTheme theme, boolean scene, int width, int height, Box kind)
	{
		final Color top = scene ? SceneSky.groundTop(theme) : SpaceSky.ground(theme);
		final Color edge = scene ? SceneSky.edge(theme) : SpaceSky.edge(theme);
		if (kind == Box.BAR)
		{
			g.setColor(top);
			g.fillRect(0, 0, width, height);
			g.setColor(edge);
			g.drawRect(0, 0, width - 1, height - 1);
			return;
		}
		final Color bottom = scene ? SceneSky.groundBottom(theme) : SpaceSky.groundBottom(theme);
		final Paint before = g.getPaint();
		g.setPaint(new GradientPaint(0, 0, top, 0, Math.max(1, height), bottom));
		g.fillRect(0, 0, width, height);
		final Color glow = CardStyle.of(theme).titleGlow;
		if (glow != null && width > 2 && height > 2)
		{
			final Color from = new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), CALM_GLOW_ALPHA);
			final Color to = new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0);
			g.setPaint(new RadialGradientPaint(new Point2D.Float(width, 0), Math.max(width, height) * 0.6f,
				new float[] {0f, 1f}, new Color[] {from, to}));
			g.fillRect(0, 0, width, height);
		}
		g.setPaint(before);
		g.setColor(edge);
		g.drawRect(0, 0, width - 1, height - 1);
	}
}
