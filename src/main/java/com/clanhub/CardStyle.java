package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * What a card theme paints with: the colours of the ground, the title, the labels, the values, the description, the hint
 * and the rules, whether every string gets a black shadow, whether the title gets a halo, how far in the content starts,
 * and the card's ground and frame. Pure: a theme in, a style out, and the painting of the ground and the edge on a
 * {@link Graphics2D} handed in; {@link CardPainter} draws the card's content in these colours.
 *
 * <p>Classic is the plain brown card the plugin first shipped, and it paints that card pixel for pixel. Stone is the
 * game's own stone frame: a speckled brown ground, a black edge, a light line, a band, a dark line and a rivet in each
 * corner, the title in the game's orange, the values in yellow and a black shadow under every string. Void is a purple
 * card whose ground runs dark downwards with stars over it, a light purple edge, and a halo behind the title. Wiki is the
 * wiki's own infobox: a beige ground with a thin brown edge, the title in a bar across the top and the facts and the drops
 * in tables; its layout is {@link WikiCard}'s, which {@link CardPainter} hands the card to when {@link #infobox} is set.
 *
 * <p>The eight space looks (Nebula, Aurora, Galaxy, Planet rise, Eclipse, Constellation, Black hole and Cosmic edge) are
 * dark cards laid out as Void is, with a black shadow under every string; their ground, a picture of space, is
 * {@link SpaceSky}'s. Cosmic edge alone of those has a {@link #titleGradient}: its title and its rules run pink, violet,
 * cyan across the card.
 *
 * <p>The twelve scene looks (Forest, Ocean, Autumn, Sakura, Steampunk, Cyberpunk, Synthwave, Volcano, Runic, Spooky,
 * Garden and Lavender) are laid out the same way, with a black shadow under every string and a halo behind the title
 * where the theme has one; their ground, a picture of a scene, is {@link SceneSky}'s. All but Cyberpunk have the title
 * in a gradient across the card (the rules keep their own colour); some give the title an {@link #titleOutline} or a
 * {@link #titleSplit}, and some the card room under the hint ({@link #bottomExtra}) for the picture there.
 */
final class CardStyle
{
	/** The side of the square tile the speckle and the stars are laid with, in px. */
	private static final int TILE = 64;

	/** The Stone frame, from the outside in. */
	private static final Color STONE_EDGE = Color.BLACK;
	private static final Color STONE_LINE = new Color(110, 98, 80);
	private static final Color STONE_BAND = new Color(84, 75, 61);
	private static final Color STONE_DARK = new Color(30, 26, 20);
	private static final Color STONE_RIVET = new Color(150, 132, 104);
	/** The side of a rivet, and how far in from the card's edge it stands, in px. */
	private static final int RIVET = 4;
	private static final int RIVET_IN = 2;

	/** The Void ground's top and bottom, and its two frame lines. */
	private static final Color VOID_TOP = new Color(56, 24, 88);
	private static final Color VOID_BOTTOM = new Color(18, 8, 38);
	private static final Color VOID_EDGE = new Color(176, 118, 246);
	private static final Color VOID_LINE = new Color(120, 80, 180);

	/** The Wiki card's edge, one pixel round the beige ground. */
	private static final Color WIKI_EDGE = new Color(150, 135, 105);

	/** The Classic card, as it always was. */
	static final CardStyle CLASSIC = new CardStyle(CardTheme.CLASSIC, CardPainter.GROUND, Color.WHITE, CardPainter.LABEL,
		Color.WHITE, Color.WHITE, CardPainter.HINT, CardPainter.LINE, null, false, CardPainter.INSET, CardPainter.LABEL,
		null, null, false, null, false);

	/** The game's own stone frame. */
	static final CardStyle STONE = new CardStyle(CardTheme.STONE, new Color(62, 54, 43), new Color(255, 152, 31),
		new Color(205, 188, 150), new Color(255, 255, 0), Color.WHITE, new Color(175, 160, 128), new Color(36, 31, 24),
		new Color(100, 90, 72), true, 12, new Color(255, 255, 0), null, stoneTile(), false, null, false);

	/** A purple card with stars. */
	static final CardStyle VOID = new CardStyle(CardTheme.VOID, VOID_TOP, new Color(228, 194, 255),
		new Color(162, 142, 202), Color.WHITE, new Color(236, 226, 250), new Color(142, 122, 182), new Color(100, 62, 152),
		null, false, 10, Color.WHITE, new Color(190, 110, 255, 70), starTile(), false, null, false);

	/** The wiki's infobox: beige, the title in a bar, the facts and the drops as tables. */
	static final CardStyle WIKI = new CardStyle(CardTheme.WIKI, new Color(234, 226, 210), new Color(38, 28, 16),
		new Color(50, 40, 25), new Color(30, 25, 15), new Color(30, 25, 15), new Color(120, 105, 80),
		new Color(180, 165, 135), null, false, 6, new Color(30, 25, 15), null, null, true, null, false);

	/** The eight space looks; {@link SpaceSky} paints each one's ground. */
	static final CardStyle NEBULA = spaceLook(CardTheme.NEBULA, new Color(255, 215, 255), new Color(230, 90, 255),
		new Color(180, 166, 220), new Color(255, 255, 255), new Color(240, 232, 252), new Color(156, 140, 196),
		new Color(126, 84, 196));
	static final CardStyle AURORA = spaceLook(CardTheme.AURORA, new Color(195, 255, 235), new Color(60, 255, 190),
		new Color(136, 190, 196), new Color(150, 255, 226), new Color(228, 246, 246), new Color(112, 154, 164),
		new Color(44, 126, 116));
	static final CardStyle GALAXY = spaceLook(CardTheme.GALAXY, new Color(242, 238, 255), new Color(150, 130, 255),
		new Color(168, 162, 212), new Color(214, 200, 255), new Color(236, 234, 250), new Color(142, 136, 182),
		new Color(92, 82, 164));
	static final CardStyle PLANET = spaceLook(CardTheme.PLANET, new Color(228, 242, 255), new Color(90, 170, 255),
		new Color(152, 172, 208), new Color(134, 216, 255), new Color(234, 242, 252), new Color(128, 144, 178),
		new Color(44, 74, 136));
	static final CardStyle ECLIPSE = spaceLook(CardTheme.ECLIPSE, new Color(255, 208, 152), new Color(255, 120, 40),
		new Color(194, 154, 134), new Color(255, 184, 104), new Color(246, 234, 222), new Color(164, 124, 104),
		new Color(116, 52, 32));
	static final CardStyle CONSTELLATION = spaceLook(CardTheme.CONSTELLATION, new Color(255, 236, 186),
		new Color(255, 210, 120), new Color(162, 172, 212), new Color(255, 236, 186), new Color(236, 238, 250),
		new Color(132, 142, 182), new Color(62, 74, 134));
	static final CardStyle BLACK_HOLE = spaceLook(CardTheme.BLACK_HOLE, new Color(255, 178, 128), new Color(255, 90, 60),
		new Color(196, 156, 150), new Color(255, 204, 156), new Color(246, 236, 232), new Color(162, 124, 120),
		new Color(110, 46, 60));
	/** Cosmic edge: no halo, and the title and the rules in the gradient (the rule colour is the gradient's middle). */
	static final CardStyle COSMIC = new CardStyle(CardTheme.COSMIC, SpaceSky.ground(CardTheme.COSMIC), Color.WHITE,
		new Color(172, 162, 212), new Color(120, 232, 255), new Color(236, 232, 250), new Color(142, 132, 182),
		SpaceSky.COSMIC_VIOLET, null, true, SpaceSky.INSET, new Color(120, 232, 255), null, null, false,
		new Color[] {SpaceSky.COSMIC_PINK, SpaceSky.COSMIC_VIOLET, SpaceSky.COSMIC_CYAN}, true);

	/** The twelve scene looks; {@link SceneSky} paints each one's ground. */
	static final CardStyle FOREST = sceneLook(CardTheme.FOREST, rgb(220, 245, 160), rgb(170, 240, 90), rgb(170, 196, 150),
		rgb(255, 230, 140), rgb(236, 244, 228), rgb(150, 176, 132), rgb(90, 140, 70),
		new Color[] {rgb(255, 250, 190), rgb(170, 230, 110)}, null, null, 14, 0);
	static final CardStyle OCEAN = sceneLook(CardTheme.OCEAN, rgb(220, 248, 255), rgb(90, 210, 255), rgb(150, 196, 220),
		rgb(255, 230, 160), rgb(234, 246, 252), rgb(130, 170, 196), rgb(80, 150, 200),
		new Color[] {rgb(255, 255, 255), rgb(150, 230, 255)}, null, null, 14, 0);
	static final CardStyle AUTUMN = sceneLook(CardTheme.AUTUMN, rgb(255, 210, 120), rgb(255, 130, 40), rgb(220, 176, 136),
		rgb(255, 226, 150), rgb(252, 238, 222), rgb(196, 146, 110), rgb(170, 90, 40),
		new Color[] {rgb(255, 236, 150), rgb(255, 150, 60)}, null, null, 14, 0);
	static final CardStyle SAKURA = sceneLook(CardTheme.SAKURA, rgb(255, 222, 238), rgb(255, 130, 190), rgb(220, 180, 202),
		rgb(255, 232, 242), rgb(252, 240, 246), rgb(190, 146, 170), rgb(170, 100, 140),
		new Color[] {rgb(255, 255, 255), rgb(255, 180, 214)}, null, null, 14, 0);
	static final CardStyle STEAMPUNK = sceneLook(CardTheme.STEAMPUNK, rgb(240, 200, 120), null, rgb(206, 170, 126),
		rgb(255, 214, 140), rgb(246, 232, 210), rgb(180, 146, 104), rgb(170, 120, 56),
		new Color[] {rgb(255, 236, 160), rgb(220, 160, 70), rgb(255, 220, 140)}, rgb(40, 24, 8), null, 16, 2);
	static final CardStyle CYBERPUNK = sceneLook(CardTheme.CYBERPUNK, rgb(255, 255, 255), rgb(255, 40, 200),
		rgb(176, 156, 206), rgb(0, 236, 255), rgb(238, 232, 252), rgb(156, 126, 186), rgb(255, 50, 200), null, null,
		new Color[] {rgb(0, 240, 255), rgb(255, 40, 200)}, 14, 0);
	static final CardStyle SYNTHWAVE = sceneLook(CardTheme.SYNTHWAVE, rgb(255, 230, 140), rgb(255, 90, 180),
		rgb(220, 176, 230), rgb(255, 232, 150), rgb(252, 238, 250), rgb(206, 156, 216), rgb(255, 110, 220),
		new Color[] {rgb(255, 250, 200), rgb(255, 210, 110), rgb(255, 120, 200), rgb(255, 170, 240)}, rgb(60, 10, 70), null,
		14, 0);
	static final CardStyle VOLCANO = sceneLook(CardTheme.VOLCANO, rgb(255, 196, 90), rgb(255, 100, 20), rgb(214, 156, 126),
		rgb(255, 214, 120), rgb(252, 234, 222), rgb(184, 124, 100), rgb(170, 60, 20),
		new Color[] {rgb(255, 250, 170), rgb(255, 180, 60), rgb(255, 90, 30)}, rgb(50, 14, 6), null, 14, 0);
	static final CardStyle RUNIC = sceneLook(CardTheme.RUNIC, rgb(180, 238, 255), rgb(60, 210, 255), rgb(166, 180, 194),
		rgb(170, 236, 255), rgb(234, 240, 246), rgb(136, 156, 170), rgb(80, 120, 140),
		new Color[] {rgb(240, 250, 255), rgb(140, 220, 255)}, rgb(10, 30, 40), null, 15, 14);
	static final CardStyle SPOOKY = sceneLook(CardTheme.SPOOKY, rgb(255, 160, 50), rgb(255, 110, 20), rgb(186, 166, 202),
		rgb(196, 255, 120), rgb(242, 234, 248), rgb(156, 136, 176), rgb(120, 70, 130),
		new Color[] {rgb(255, 210, 90), rgb(255, 110, 20)}, rgb(30, 8, 30), null, 14, 0);
	static final CardStyle GARDEN = sceneLook(CardTheme.GARDEN, rgb(255, 226, 232), rgb(255, 160, 190), rgb(196, 214, 176),
		rgb(255, 236, 150), rgb(244, 248, 236), rgb(170, 190, 150), rgb(120, 170, 100),
		new Color[] {rgb(255, 210, 225), rgb(255, 240, 160)}, rgb(30, 50, 26), null, 14, 26);
	static final CardStyle LAVENDER = sceneLook(CardTheme.LAVENDER, rgb(236, 214, 255), rgb(190, 130, 255),
		rgb(206, 186, 226), rgb(255, 226, 190), rgb(248, 240, 252), rgb(176, 156, 200), rgb(150, 110, 190),
		new Color[] {rgb(255, 240, 255), rgb(210, 170, 255)}, rgb(40, 20, 60), null, 14, 32);

	/** Every theme's style, by theme. */
	private static final Map<CardTheme, CardStyle> STYLES = new EnumMap<>(CardTheme.class);

	static
	{
		for (final CardStyle style : new CardStyle[] {CLASSIC, STONE, VOID, WIKI, NEBULA, AURORA, GALAXY, PLANET, ECLIPSE,
			CONSTELLATION, BLACK_HOLE, COSMIC, FOREST, OCEAN, AUTUMN, SAKURA, STEAMPUNK, CYBERPUNK, SYNTHWAVE, VOLCANO, RUNIC,
			SPOOKY, GARDEN, LAVENDER})
		{
			STYLES.put(style.theme, style);
		}
	}

	private final CardTheme theme;
	/** The card's ground: its solid colour (for Void, the colour at the top of its gradient). */
	final Color ground;
	/** The title. */
	final Color title;
	/** The labels, the notes after a value, and the drops. */
	final Color label;
	/** The values after a label, and the rates of the split card's drops. */
	final Color value;
	/** The description. */
	final Color text;
	/** The hint. */
	final Color hint;
	/** The rules across the card. */
	final Color rule;
	/** A second rule drawn the pixel under each rule, or null for none. */
	final Color ruleLight;
	/** Whether every string has a black copy of itself 1 px to the right and down, drawn first. */
	final boolean shadow;
	/**
	 * Whether every string has a black outline instead: copies 1 px left, right, up, down and down-right, drawn first. The
	 * twenty space and scene themes have it, their pictures lying under the words.
	 *
	 * <p>Choice: the outline, with the description in white, is what kept the words readable over the brightest of the
	 * pictures (Synthwave's sun and grid, Ocean's light, Autumn's sun, Lavender's horizon) in the renders the user
	 * compared, 2026-10-26; a darker veil or faded art cost the pictures more.
	 */
	final boolean outline;
	/** The gap from the card's edge to its content, in px. */
	final int inset;
	/** The rates of the wide card's drops. */
	final Color wideRate;
	/** The halo drawn behind the title alone, or null for none. */
	final Color titleGlow;
	/**
	 * Whether the card is laid out as the wiki's infobox by {@link WikiCard} (the title in a bar, the facts and the drops
	 * in tables) instead of by {@link CardPainter}'s own layout; only Wiki is.
	 */
	final boolean infobox;
	/**
	 * The colours the title is painted in, left to right across the card, or null for the plain {@link #title}; Cosmic edge
	 * and all but one of the scene looks have them, and Cosmic edge's rules are painted in them too ({@link #gradientRules}).
	 */
	final Color[] titleGradient;
	/**
	 * The colour the title is drawn in first, at the 1 px offsets left, right, up, down and down-right of where it stands,
	 * so that it has an outline, or null for none.
	 */
	final Color titleOutline;
	/**
	 * Two colours the title is drawn in first, the first 2 px to the left of where it stands and the second 2 px to the
	 * right, so that it splits in two, or null for none.
	 */
	final Color[] titleSplit;
	/** The room added under the hint, in px at 16 px (scaled with the text size), for a picture there; 0 for none. */
	final int bottomExtra;
	/** Whether the rules are painted in the {@link #titleGradient} too, and not in {@link #rule}; only Cosmic edge's are. */
	final boolean gradientRules;
	/** The picture theme this style wears, whose picture is the card's ground and whose treatment lies under the words, or null. */
	final PictureTheme picture;
	/** The tile laid over the ground, or null for none. */
	private final BufferedImage tile;
	/** Whether the ground is a {@link SpaceSky}. */
	private final boolean space;
	/** Whether the ground is a {@link SceneSky}. */
	private final boolean scene;

	/** A style with no outline, no split and no room under the hint, whose rules take the gradient when it has one. */
	private CardStyle(CardTheme theme, Color ground, Color title, Color label, Color value, Color text, Color hint,
		Color rule, Color ruleLight, boolean shadow, int inset, Color wideRate, Color titleGlow, BufferedImage tile,
		boolean infobox, Color[] titleGradient, boolean space)
	{
		this(theme, ground, title, label, value, text, hint, rule, ruleLight, shadow, inset, wideRate, titleGlow, tile,
			infobox, titleGradient, space, false, null, null, 0, titleGradient != null, null);
	}

	private CardStyle(CardTheme theme, Color ground, Color title, Color label, Color value, Color text, Color hint,
		Color rule, Color ruleLight, boolean shadow, int inset, Color wideRate, Color titleGlow, BufferedImage tile,
		boolean infobox, Color[] titleGradient, boolean space, boolean scene, Color titleOutline, Color[] titleSplit,
		int bottomExtra, boolean gradientRules, PictureTheme picture)
	{
		this.theme = theme;
		this.ground = ground;
		this.title = title;
		this.label = label;
		this.value = value;
		this.text = text;
		this.hint = hint;
		this.rule = rule;
		this.ruleLight = ruleLight;
		this.shadow = shadow;
		this.inset = inset;
		this.wideRate = wideRate;
		this.titleGlow = titleGlow;
		this.tile = tile;
		this.infobox = infobox;
		this.titleGradient = titleGradient;
		this.space = space;
		this.scene = scene;
		this.outline = space || scene;
		this.titleOutline = titleOutline;
		this.titleSplit = titleSplit;
		this.bottomExtra = bottomExtra;
		this.gradientRules = gradientRules;
		this.picture = picture;
	}

	/** A space look: the shadow on, the inset 10, no light rule and no gradient, the wide card's rates the values' colour. */
	private static CardStyle spaceLook(CardTheme theme, Color title, Color glow, Color label, Color value, Color text,
		Color hint, Color rule)
	{
		return new CardStyle(theme, SpaceSky.ground(theme), title, label, value, text, hint, rule, null, true,
			SpaceSky.INSET, value, new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 70), null, false, null, true);
	}

	/**
	 * A scene look: the shadow on, not the infobox, the wide card's rates the values' colour, the halo the glow colour at
	 * alpha 70 (none when {@code glow} is null) and the rules in their own colour; the title gradient, the outline, the
	 * split, the inset and the room under the hint are the theme's own.
	 */
	private static CardStyle sceneLook(CardTheme theme, Color title, Color glow, Color label, Color value, Color text,
		Color hint, Color rule, Color[] gradient, Color outline, Color[] split, int inset, int bottomExtra)
	{
		return new CardStyle(theme, SceneSky.groundTop(theme), title, label, value, text, hint, rule, null, true, inset,
			value, glow == null ? null : new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 70), null, false, gradient,
			false, true, outline, split, bottomExtra, false, null);
	}

	private static Color rgb(int red, int green, int blue)
	{
		return new Color(red, green, blue);
	}

	/** The style of {@code theme}; a null theme gives Classic. */
	static CardStyle of(CardTheme theme)
	{
		final CardStyle style = theme == null ? null : STYLES.get(theme);
		return style == null ? CLASSIC : style;
	}

	/**
	 * The style the cards wear: the picture theme {@code pictureId} names, when the catalogue has it and its pictures load,
	 * Classic when it has it and they do not, and the original {@code theme} when {@code pictureId} is empty or names no
	 * theme.
	 */
	static CardStyle of(CardTheme theme, String pictureId)
	{
		final PictureThemes themes = PictureThemes.current();
		final PictureTheme found = themes.byId(pictureId);
		if (found == null)
		{
			return of(theme);
		}
		return themes.usable(found) ? ofPicture(found) : CLASSIC;
	}

	/**
	 * The style of a picture theme: the words in its role colours, the inset 10, the wide card's rates in the value colour, and
	 * no outline or shadow of the originals' kind (the theme's own outline is drawn by {@link PictureArt}).
	 */
	private static CardStyle ofPicture(PictureTheme t)
	{
		final Color value = t.role("value", Color.WHITE);
		return new CardStyle(null, t.sidebarColour(0, Color.BLACK), t.role("title", Color.WHITE), t.role("label", Color.LIGHT_GRAY),
			value, t.role("text", Color.WHITE), t.role("hint", Color.GRAY), t.role("rule", Color.GRAY), null, false, 10, value, null,
			null, false, null, false, false, null, null, 0, false, t);
	}

	/**
	 * Paints the card's ground and its edge, its top-left corner at ({@code x}, {@code y}), {@code width} by
	 * {@code height} px: Classic a solid brown ground with a black edge and a lighter line inside it; Stone its speckled
	 * ground and the four lines and the rivets; Void its gradient, its stars and its two purple lines; Wiki its beige
	 * ground with a one pixel brown edge; a space look its {@link SpaceSky} and a scene look its {@link SceneSky}, the
	 * whole card's ground in one image.
	 */
	void frame(Graphics2D g, int x, int y, int width, int height)
	{
		if (picture != null)
		{
			return;
		}
		if (space)
		{
			g.drawImage(SpaceSky.of(theme, width, height), x, y, null);
		}
		else if (scene)
		{
			g.drawImage(SceneSky.of(theme, width, height), x, y, null);
		}
		else if (theme == CardTheme.STONE)
		{
			g.setColor(ground);
			g.fillRect(x, y, width, height);
			lay(g, x, y, width, height);
			ring(g, STONE_EDGE, x, y, width, height, 0, 1);
			ring(g, STONE_LINE, x, y, width, height, 1, 1);
			ring(g, STONE_BAND, x, y, width, height, 2, 2);
			ring(g, STONE_DARK, x, y, width, height, 4, 1);
			g.setColor(STONE_RIVET);
			final int far = RIVET_IN + RIVET;
			g.fillRect(x + RIVET_IN, y + RIVET_IN, RIVET, RIVET);
			g.fillRect(x + width - far, y + RIVET_IN, RIVET, RIVET);
			g.fillRect(x + RIVET_IN, y + height - far, RIVET, RIVET);
			g.fillRect(x + width - far, y + height - far, RIVET, RIVET);
		}
		else if (theme == CardTheme.VOID)
		{
			final Paint before = g.getPaint();
			g.setPaint(new GradientPaint(x, y, VOID_TOP, x, y + height, VOID_BOTTOM));
			g.fillRect(x, y, width, height);
			g.setPaint(before);
			lay(g, x, y, width, height);
			ring(g, VOID_EDGE, x, y, width, height, 0, 1);
			ring(g, VOID_LINE, x, y, width, height, 1, 1);
		}
		else if (theme == CardTheme.WIKI)
		{
			g.setColor(ground);
			g.fillRect(x, y, width, height);
			g.setColor(WIKI_EDGE);
			g.drawRect(x, y, width - 1, height - 1);
		}
		else
		{
			g.setColor(ground);
			g.fillRect(x, y, width, height);
			g.setColor(Color.BLACK);
			g.drawRect(x, y, width - 1, height - 1);
			g.setColor(CardPainter.LINE);
			g.drawRect(x + 1, y + 1, width - 3, height - 3);
		}
	}

	/**
	 * Lays the tile over the rectangle, the tile's corner on the rectangle's. The tile is laid over what is already there,
	 * so the caller fills the ground first; only Stone and Void have a tile, the sidebar's Stone rows use Stone's alone.
	 */
	void lay(Graphics2D g, int x, int y, int width, int height)
	{
		final Paint before = g.getPaint();
		g.setPaint(new TexturePaint(tile, new Rectangle(x, y, TILE, TILE)));
		g.fillRect(x, y, width, height);
		g.setPaint(before);
	}

	/**
	 * Draws the outline of a rectangle {@code thickness} px thick, {@code in} px inside the edge of the {@code width} by
	 * {@code height} rectangle at ({@code x}, {@code y}).
	 */
	private static void ring(Graphics2D g, Color colour, int x, int y, int width, int height, int in, int thickness)
	{
		final int ringWidth = width - 2 * in;
		final int ringHeight = height - 2 * in;
		g.setColor(colour);
		g.fillRect(x + in, y + in, ringWidth, thickness);
		g.fillRect(x + in, y + in + ringHeight - thickness, ringWidth, thickness);
		g.fillRect(x + in, y + in + thickness, thickness, ringHeight - 2 * thickness);
		g.fillRect(x + in + ringWidth - thickness, y + in + thickness, thickness, ringHeight - 2 * thickness);
	}

	/** The Stone speckle: about one pixel in seven, light or dark, over the rest, which is see-through. */
	private static BufferedImage stoneTile()
	{
		final BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
		final Random random = new Random(3);
		final int light = new Color(84, 74, 60, 120).getRGB();
		final int dark = new Color(42, 36, 28, 140).getRGB();
		for (int y = 0; y < TILE; y++)
		{
			for (int x = 0; x < TILE; x++)
			{
				if (random.nextInt(7) == 0)
				{
					image.setRGB(x, y, random.nextBoolean() ? light : dark);
				}
			}
		}
		return image;
	}

	/** The Void stars: about one pixel in a hundred, white at an alpha from 60 to 200, over the rest, which is see-through. */
	private static BufferedImage starTile()
	{
		final BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
		final Random random = new Random(5);
		for (int y = 0; y < TILE; y++)
		{
			for (int x = 0; x < TILE; x++)
			{
				if (random.nextInt(100) == 0)
				{
					image.setRGB(x, y, new Color(255, 255, 255, 60 + random.nextInt(141)).getRGB());
				}
			}
		}
		return image;
	}
}
