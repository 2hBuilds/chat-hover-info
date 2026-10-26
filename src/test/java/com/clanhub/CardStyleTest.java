package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** {@link CardStyle}: what each card theme paints with. */
public class CardStyleTest
{
	@Test
	public void classicIsTodaysCard()
	{
		for (final CardStyle style : new CardStyle[] {CardStyle.of(CardTheme.CLASSIC), CardStyle.of(null)})
		{
			assertEquals("the brown ground", CardPainter.GROUND, style.ground);
			assertEquals("a white title", Color.WHITE, style.title);
			assertEquals("grey labels", CardPainter.LABEL, style.label);
			assertEquals("white values", Color.WHITE, style.value);
			assertEquals("a white description", Color.WHITE, style.text);
			assertEquals("the grey hint", CardPainter.HINT, style.hint);
			assertEquals("the tan rule", CardPainter.LINE, style.rule);
			assertNull("no light rule", style.ruleLight);
			assertFalse("no shadow", style.shadow);
			assertNull("no glow", style.titleGlow);
			assertEquals("the inset is 9", 9, style.inset);
			assertEquals("the wide card's rates are grey", CardPainter.LABEL, style.wideRate);
		}
		assertSame("a null theme is Classic", CardStyle.of(CardTheme.CLASSIC), CardStyle.of(null));
	}

	@Test
	public void stoneIsTheGamesFrame()
	{
		final CardStyle style = CardStyle.of(CardTheme.STONE);

		assertEquals(new Color(62, 54, 43), style.ground);
		assertEquals("the game's orange title", new Color(255, 152, 31), style.title);
		assertEquals(new Color(205, 188, 150), style.label);
		assertEquals("yellow values", new Color(255, 255, 0), style.value);
		assertEquals(Color.WHITE, style.text);
		assertEquals(new Color(175, 160, 128), style.hint);
		assertEquals(new Color(36, 31, 24), style.rule);
		assertEquals("a light rule under the rule", new Color(100, 90, 72), style.ruleLight);
		assertTrue("every string has a shadow", style.shadow);
		assertNull("no glow", style.titleGlow);
		assertEquals("the inset is 12", 12, style.inset);
		assertEquals("the wide card's rates are yellow", new Color(255, 255, 0), style.wideRate);
	}

	@Test
	public void voidIsPurpleWithStars()
	{
		final CardStyle style = CardStyle.of(CardTheme.VOID);

		assertEquals("a light violet title", new Color(228, 194, 255), style.title);
		assertEquals(new Color(162, 142, 202), style.label);
		assertEquals("white values", Color.WHITE, style.value);
		assertEquals(new Color(236, 226, 250), style.text);
		assertEquals(new Color(142, 122, 182), style.hint);
		assertEquals(new Color(100, 62, 152), style.rule);
		assertNull("no light rule", style.ruleLight);
		assertFalse("no shadow", style.shadow);
		assertEquals("a violet halo behind the title", new Color(190, 110, 255, 70), style.titleGlow);
		assertEquals("the inset is 10", 10, style.inset);
		assertEquals("the wide card's rates are white", Color.WHITE, style.wideRate);
	}

	/** Lot 10Q: the Wiki theme is the infobox: beige, the wiki's browns, an inset of 6, and the only style laid out by {@link WikiCard}. */
	@Test
	public void wikiIsTheInfobox()
	{
		final CardStyle style = CardStyle.of(CardTheme.WIKI);

		assertEquals("the beige ground", new Color(234, 226, 210), style.ground);
		assertEquals("a dark brown title", new Color(38, 28, 16), style.title);
		assertEquals(new Color(50, 40, 25), style.label);
		assertEquals(new Color(30, 25, 15), style.value);
		assertEquals(new Color(30, 25, 15), style.text);
		assertEquals(new Color(120, 105, 80), style.hint);
		assertEquals(new Color(180, 165, 135), style.rule);
		assertNull("no light rule", style.ruleLight);
		assertFalse("no shadow", style.shadow);
		assertNull("no glow", style.titleGlow);
		assertEquals("the inset is 6", 6, style.inset);
		assertEquals("the wide card's rates are dark", new Color(30, 25, 15), style.wideRate);
		assertTrue("it is the infobox", style.infobox);
		assertFalse("Classic is not", CardStyle.of(CardTheme.CLASSIC).infobox);
		assertFalse("Stone is not", CardStyle.of(CardTheme.STONE).infobox);
		assertFalse("Void is not", CardStyle.of(CardTheme.VOID).infobox);
	}

	/** Lot 10R: the eight space looks carry the table's colours, a shadow, an inset of 10, and Cosmic edge alone the gradient. */
	// Re-aimed by the lead, 2026-10-26 (lot 10T): twelve scene themes after Cosmic edge; most of them have a gradient too.
	@Test
	public void theSpaceThemes()
	{
		space(CardTheme.NEBULA, rgb(255, 215, 255), rgb(230, 90, 255), rgb(180, 166, 220), rgb(255, 255, 255),
			rgb(240, 232, 252), rgb(156, 140, 196), rgb(126, 84, 196));
		space(CardTheme.AURORA, rgb(195, 255, 235), rgb(60, 255, 190), rgb(136, 190, 196), rgb(150, 255, 226),
			rgb(228, 246, 246), rgb(112, 154, 164), rgb(44, 126, 116));
		space(CardTheme.GALAXY, rgb(242, 238, 255), rgb(150, 130, 255), rgb(168, 162, 212), rgb(214, 200, 255),
			rgb(236, 234, 250), rgb(142, 136, 182), rgb(92, 82, 164));
		space(CardTheme.PLANET, rgb(228, 242, 255), rgb(90, 170, 255), rgb(152, 172, 208), rgb(134, 216, 255),
			rgb(234, 242, 252), rgb(128, 144, 178), rgb(44, 74, 136));
		space(CardTheme.ECLIPSE, rgb(255, 208, 152), rgb(255, 120, 40), rgb(194, 154, 134), rgb(255, 184, 104),
			rgb(246, 234, 222), rgb(164, 124, 104), rgb(116, 52, 32));
		space(CardTheme.CONSTELLATION, rgb(255, 236, 186), rgb(255, 210, 120), rgb(162, 172, 212), rgb(255, 236, 186),
			rgb(236, 238, 250), rgb(132, 142, 182), rgb(62, 74, 134));
		space(CardTheme.BLACK_HOLE, rgb(255, 178, 128), rgb(255, 90, 60), rgb(196, 156, 150), rgb(255, 204, 156),
			rgb(246, 236, 232), rgb(162, 124, 120), rgb(110, 46, 60));

		final CardStyle cosmic = CardStyle.of(CardTheme.COSMIC);
		assertEquals("a white title, in the gradient", Color.WHITE, cosmic.title);
		assertNull("no halo", cosmic.titleGlow);
		assertEquals(rgb(172, 162, 212), cosmic.label);
		assertEquals(rgb(120, 232, 255), cosmic.value);
		assertEquals(rgb(236, 232, 250), cosmic.text);
		assertEquals(rgb(142, 132, 182), cosmic.hint);
		assertEquals("the wide card's rates are the value's colour", cosmic.value, cosmic.wideRate);
		assertTrue("every string has a shadow", cosmic.shadow);
		assertEquals("the inset is 10", 10, cosmic.inset);
		assertFalse("it is not the infobox", cosmic.infobox);
		assertNull("no light rule", cosmic.ruleLight);
		assertNotNull("the title and the rules run through three colours", cosmic.titleGradient);
		assertEquals(3, cosmic.titleGradient.length);
		assertEquals("pink", rgb(255, 70, 200), cosmic.titleGradient[0]);
		assertEquals("violet", rgb(150, 80, 255), cosmic.titleGradient[1]);
		assertEquals("cyan", rgb(60, 220, 255), cosmic.titleGradient[2]);
		for (final CardTheme theme : CardTheme.values())
		{
			assertEquals(theme + " has its own style", theme, themeOf(CardStyle.of(theme)));
			if (theme.ordinal() <= CardTheme.COSMIC.ordinal())
			{
				assertEquals(theme + (theme == CardTheme.COSMIC ? " has" : " has no") + " the gradient",
					theme == CardTheme.COSMIC, CardStyle.of(theme).titleGradient != null);
			}
		}
	}

	/**
	 * Lot 10T: the twelve scene looks carry the table's colours, title gradient, outline, split, inset and room under the
	 * hint, a shadow, the values' colour for the wide card's rates, and never the infobox; every older look has no outline,
	 * no split and no room under the hint.
	 */
	@Test
	public void theTwelveSceneThemes()
	{
		scene(CardTheme.FOREST, rgb(220, 245, 160), rgb(170, 240, 90), rgb(170, 196, 150), rgb(255, 230, 140),
			rgb(236, 244, 228), rgb(150, 176, 132), rgb(90, 140, 70), new Color[] {rgb(255, 250, 190), rgb(170, 230, 110)},
			null, null, 14, 0);
		scene(CardTheme.OCEAN, rgb(220, 248, 255), rgb(90, 210, 255), rgb(150, 196, 220), rgb(255, 230, 160),
			rgb(234, 246, 252), rgb(130, 170, 196), rgb(80, 150, 200), new Color[] {rgb(255, 255, 255), rgb(150, 230, 255)},
			null, null, 14, 0);
		scene(CardTheme.AUTUMN, rgb(255, 210, 120), rgb(255, 130, 40), rgb(220, 176, 136), rgb(255, 226, 150),
			rgb(252, 238, 222), rgb(196, 146, 110), rgb(170, 90, 40), new Color[] {rgb(255, 236, 150), rgb(255, 150, 60)},
			null, null, 14, 0);
		scene(CardTheme.SAKURA, rgb(255, 222, 238), rgb(255, 130, 190), rgb(220, 180, 202), rgb(255, 232, 242),
			rgb(252, 240, 246), rgb(190, 146, 170), rgb(170, 100, 140), new Color[] {rgb(255, 255, 255), rgb(255, 180, 214)},
			null, null, 14, 0);
		scene(CardTheme.STEAMPUNK, rgb(240, 200, 120), null, rgb(206, 170, 126), rgb(255, 214, 140), rgb(246, 232, 210),
			rgb(180, 146, 104), rgb(170, 120, 56),
			new Color[] {rgb(255, 236, 160), rgb(220, 160, 70), rgb(255, 220, 140)}, rgb(40, 24, 8), null, 16, 2);
		scene(CardTheme.CYBERPUNK, rgb(255, 255, 255), rgb(255, 40, 200), rgb(176, 156, 206), rgb(0, 236, 255),
			rgb(238, 232, 252), rgb(156, 126, 186), rgb(255, 50, 200), null, null,
			new Color[] {rgb(0, 240, 255), rgb(255, 40, 200)}, 14, 0);
		scene(CardTheme.SYNTHWAVE, rgb(255, 230, 140), rgb(255, 90, 180), rgb(220, 176, 230), rgb(255, 232, 150),
			rgb(252, 238, 250), rgb(206, 156, 216), rgb(255, 110, 220),
			new Color[] {rgb(255, 250, 200), rgb(255, 210, 110), rgb(255, 120, 200), rgb(255, 170, 240)}, rgb(60, 10, 70),
			null, 14, 0);
		scene(CardTheme.VOLCANO, rgb(255, 196, 90), rgb(255, 100, 20), rgb(214, 156, 126), rgb(255, 214, 120),
			rgb(252, 234, 222), rgb(184, 124, 100), rgb(170, 60, 20),
			new Color[] {rgb(255, 250, 170), rgb(255, 180, 60), rgb(255, 90, 30)}, rgb(50, 14, 6), null, 14, 0);
		scene(CardTheme.RUNIC, rgb(180, 238, 255), rgb(60, 210, 255), rgb(166, 180, 194), rgb(170, 236, 255),
			rgb(234, 240, 246), rgb(136, 156, 170), rgb(80, 120, 140), new Color[] {rgb(240, 250, 255), rgb(140, 220, 255)},
			rgb(10, 30, 40), null, 15, 14);
		scene(CardTheme.SPOOKY, rgb(255, 160, 50), rgb(255, 110, 20), rgb(186, 166, 202), rgb(196, 255, 120),
			rgb(242, 234, 248), rgb(156, 136, 176), rgb(120, 70, 130), new Color[] {rgb(255, 210, 90), rgb(255, 110, 20)},
			rgb(30, 8, 30), null, 14, 0);
		scene(CardTheme.GARDEN, rgb(255, 226, 232), rgb(255, 160, 190), rgb(196, 214, 176), rgb(255, 236, 150),
			rgb(244, 248, 236), rgb(170, 190, 150), rgb(120, 170, 100), new Color[] {rgb(255, 210, 225), rgb(255, 240, 160)},
			rgb(30, 50, 26), null, 14, 26);
		scene(CardTheme.LAVENDER, rgb(236, 214, 255), rgb(190, 130, 255), rgb(206, 186, 226), rgb(255, 226, 190),
			rgb(248, 240, 252), rgb(176, 156, 200), rgb(150, 110, 190), new Color[] {rgb(255, 240, 255), rgb(210, 170, 255)},
			rgb(40, 20, 60), null, 14, 32);

		for (final CardTheme theme : CardTheme.values())
		{
			final CardStyle style = CardStyle.of(theme);
			assertEquals(theme + " has its own style", theme, themeOf(style));
			if (theme.ordinal() < CardTheme.FOREST.ordinal())
			{
				assertNull(theme + " has no outline", style.titleOutline);
				assertNull(theme + " has no split", style.titleSplit);
				assertEquals(theme + " has no room under the hint", 0, style.bottomExtra);
			}
		}
	}

	/** The theme whose style {@code style} is, found by asking each theme for its own. */
	private static CardTheme themeOf(CardStyle style)
	{
		for (final CardTheme theme : CardTheme.values())
		{
			if (CardStyle.of(theme) == style)
			{
				return theme;
			}
		}
		return null;
	}

	/** One space style against its row of the table: the halo is the glow colour at alpha 70. */
	private static void space(CardTheme theme, Color title, Color glow, Color label, Color value, Color text, Color hint,
		Color rule)
	{
		final CardStyle style = CardStyle.of(theme);
		assertEquals(theme + " title", title, style.title);
		assertEquals(theme + " halo", new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 70), style.titleGlow);
		assertEquals(theme + " label", label, style.label);
		assertEquals(theme + " value", value, style.value);
		assertEquals(theme + " text", text, style.text);
		assertEquals(theme + " hint", hint, style.hint);
		assertEquals(theme + " rule", rule, style.rule);
		assertEquals(theme + " wide rates", value, style.wideRate);
		assertNull(theme + " no light rule", style.ruleLight);
		assertTrue(theme + " has a shadow", style.shadow);
		assertEquals(theme + " inset", 10, style.inset);
		assertFalse(theme + " is not the infobox", style.infobox);
		assertNull(theme + " has no gradient", style.titleGradient);
	}

	/** One scene style against its row of the table: the halo is the glow colour at alpha 70, or none when {@code glow} is null. */
	private static void scene(CardTheme theme, Color title, Color glow, Color label, Color value, Color text, Color hint,
		Color rule, Color[] gradient, Color outline, Color[] split, int inset, int bottomExtra)
	{
		final CardStyle style = CardStyle.of(theme);
		assertEquals(theme + " title", title, style.title);
		assertEquals(theme + " halo", glow == null ? null : new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 70),
			style.titleGlow);
		assertEquals(theme + " label", label, style.label);
		assertEquals(theme + " value", value, style.value);
		assertEquals(theme + " text", text, style.text);
		assertEquals(theme + " hint", hint, style.hint);
		assertEquals(theme + " rule", rule, style.rule);
		assertEquals(theme + " wide rates", value, style.wideRate);
		assertNull(theme + " no light rule", style.ruleLight);
		assertTrue(theme + " has a shadow", style.shadow);
		assertEquals(theme + " inset", inset, style.inset);
		assertEquals(theme + " bottom extra", bottomExtra, style.bottomExtra);
		assertFalse(theme + " is not the infobox", style.infobox);
		assertArrayEquals(theme + " gradient", gradient, style.titleGradient);
		assertEquals(theme + " outline", outline, style.titleOutline);
		assertArrayEquals(theme + " split", split, style.titleSplit);
	}

	private static Color rgb(int red, int green, int blue)
	{
		return new Color(red, green, blue);
	}
}
