package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import com.google.gson.Gson;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Lot 15A: the catalogue of picture themes, its order, its pictures' cache, and the fall back to Classic. */
public class PictureThemesTest
{
	private static final String JSON = "[{\"id\":\"b\",\"name\":\"Beta\",\"group\":\"Mystic\",\"tier\":\"dark\",\"heights\":[150,200,250],"
		+ "\"edge\":\"#112233\",\"ink\":\"light\",\"outline\":1,\"panel\":0.5,\"focalTitle\":true,"
		+ "\"roles\":{\"title\":\"#FFEE00\",\"label\":\"#CCCCCC\",\"value\":\"#FFFFFF\",\"text\":\"#EEEEEE\",\"hint\":\"#AAAAAA\","
		+ "\"rule\":\"#777777\"},\"sidebar\":[\"#203050\",\"#101828\"]},"
		+ "{\"id\":\"a\",\"name\":\"Alpha\",\"group\":\"Calm\",\"tier\":\"in-between\",\"heights\":[200]},"
		+ "{\"id\":\"c\",\"name\":\"Gamma\",\"group\":\"Calm\",\"tier\":\"bright\",\"heights\":[200]},"
		+ "{\"id\":\"d\",\"name\":\"Delta\",\"group\":\"Calm\",\"tier\":\"dark\",\"heights\":[200]},"
		+ "{\"id\":\"e\",\"name\":\"Echo\",\"group\":\"Calm\",\"tier\":\"bright\",\"heights\":[200]},"
		+ "{\"id\":\"x\",\"name\":\"No heights\",\"group\":\"Calm\",\"tier\":\"bright\"},"
		+ "{\"id\":\"a\",\"name\":\"Alpha again\",\"group\":\"Calm\",\"tier\":\"bright\",\"heights\":[200]},"
		+ "{\"name\":\"No id\",\"group\":\"Calm\",\"heights\":[200]}]";

	@After
	public void putTheCatalogueBack()
	{
		PictureThemes.install(PictureFixtures.catalogue(null));
	}

	private static PictureThemes parsed()
	{
		return PictureThemes.parse(new StringReader(JSON), new Gson(), PictureFixtures.loader(Color.GRAY, new HashMap<String, Integer>(),
			new ArrayList<String>()));
	}

	@Test
	public void theCatalogueIsReadAndWhatIsNotAThemeIsSkipped()
	{
		final PictureThemes themes = parsed();
		assertNotNull(themes.byId("a"));
		assertEquals("the first of a repeated id wins", "Alpha", themes.byId("a").name);
		assertNull("no heights, no theme", themes.byId("x"));
		assertNull("no id, no theme", themes.byId(null));
		assertNull(themes.byId("nope"));
		final PictureTheme beta = themes.byId("b");
		assertEquals(new Color(0x11, 0x22, 0x33), beta.edgeColour());
		assertEquals(new Color(0xFF, 0xEE, 0x00), beta.role("title", Color.RED));
		assertEquals("a role the theme lacks takes the fallback", Color.RED, themes.byId("a").role("title", Color.RED));
		assertTrue(beta.focalTitle);
		assertEquals(0.5, beta.panel, 0);
		assertEquals(new Color(0x10, 0x18, 0x28), beta.sidebarColour(1, Color.RED));
	}

	@Test
	public void groupsComeInTheMenusOrderAndAGroupsThemesBrightFirst()
	{
		final PictureThemes themes = parsed();
		assertEquals("Calm before Mystic", Arrays.asList("Calm", "Mystic"), themes.groups());
		final List<String> names = new ArrayList<>();
		for (final PictureTheme theme : themes.inGroup("Calm"))
		{
			names.add(theme.name);
		}
		assertEquals("bright (by name), in-between, dark", Arrays.asList("Echo", "Gamma", "Alpha", "Delta"), names);
		assertEquals("the eighteen groups, in order", "Calm", PictureThemes.GROUPS.get(0));
		assertEquals(18, PictureThemes.GROUPS.size());
		assertEquals("Blossoms", PictureThemes.GROUPS.get(17));
	}

	/** 1.1.0: another frost opacity gives another background for the same card, and the first again gives it back. */
	@Test
	public void anotherFrostOpacityGivesAnotherBackground()
	{
		final PictureTheme theme = PictureFixtures.theme("frost", "Calm", "bright", false, 1, 0.5, false);
		final PictureThemes themes = PictureFixtures.catalogue(PictureFixtures.loader(Color.GRAY, new HashMap<String, Integer>(),
			new ArrayList<String>()), theme);
		final List<int[]> lines = new ArrayList<>();
		lines.add(new int[] {20, 20, 100, 16});
		try
		{
			PictureThemes.frostOpacity(40);
			final BufferedImage on = themes.ground(theme, 409, 212, lines, null);
			PictureThemes.frostOpacity(0);
			final BufferedImage off = themes.ground(theme, 409, 212, lines, null);
			assertNotSame("off is its own background", on, off);
			PictureThemes.frostOpacity(40);
			assertSame("on again is the first, kept", on, themes.ground(theme, 409, 212, lines, null));
		}
		finally
		{
			PictureThemes.frostOpacity(40);
		}
	}

	@Test
	public void thePicturesAreDecodedOnceAndTheCardBackgroundIsKept()
	{
		final Map<String, Integer> calls = new HashMap<>();
		final PictureTheme theme = PictureFixtures.theme("kept", "Calm", "bright", false, 1, 0.5, false);
		final PictureThemes themes = PictureFixtures.catalogue(PictureFixtures.loader(Color.GRAY, calls, new ArrayList<String>()), theme);
		final List<int[]> lines = new ArrayList<>();
		lines.add(new int[] {20, 20, 100, 16});
		final BufferedImage first = themes.ground(theme, 409, 212, lines, null);
		final BufferedImage second = themes.ground(theme, 409, 212, lines, null);
		assertSame("the same card costs the treatment once", first, second);
		assertEquals(409, first.getWidth());
		assertEquals("stretched to the card's exact height", 212, first.getHeight());
		assertEquals("the nearest picture, 200, was read once", Integer.valueOf(1), calls.get(PictureFixtures.path("kept", 200)));
		themes.ground(theme, 409, 214, lines, null);
		assertEquals("another height of the same card still reads no file twice", Integer.valueOf(1),
			calls.get(PictureFixtures.path("kept", 200)));
		assertNull("only the nearest was read", calls.get(PictureFixtures.path("kept", 150)));
	}

	@Test
	public void aMissingPictureMakesTheThemeUnusableOnceAndTheCardsClassic()
	{
		final Map<String, Integer> calls = new HashMap<>();
		final PictureTheme broken = PictureFixtures.theme("broken", "Calm", "bright", false, 1, 0.5, false);
		final PictureTheme fine = PictureFixtures.theme("fine", "Calm", "bright", false, 1, 0.5, false);
		final PictureThemes themes = PictureFixtures.catalogue(
			PictureFixtures.loader(Color.GRAY, calls, Collections.singletonList(PictureFixtures.path("broken", 250))), broken, fine);
		PictureThemes.install(themes);

		assertTrue(themes.usable(fine));
		assertTrue(!themes.usable(broken));
		final int callsAfterFirst = calls.get(PictureFixtures.path("broken", 250));
		assertTrue(!themes.usable(broken));
		assertTrue(!themes.usable(broken));
		assertEquals("the failed picture is never asked for again", callsAfterFirst,
			(int) calls.get(PictureFixtures.path("broken", 250)));

		assertSame("a broken picture theme wears Classic", CardStyle.CLASSIC, CardStyle.of(CardTheme.STONE, "broken"));
		assertSame("a theme the catalogue lacks leaves the original theme", CardStyle.STONE, CardStyle.of(CardTheme.STONE, "gone"));
		assertSame("an empty id leaves the original theme", CardStyle.STONE, CardStyle.of(CardTheme.STONE, ""));
		assertSame("a null id too", CardStyle.STONE, CardStyle.of(CardTheme.STONE, null));
		final CardStyle picture = CardStyle.of(CardTheme.STONE, "fine");
		assertSame("a good picture theme is its own style", fine, picture.picture);
	}

	@Test
	public void noCatalogueMeansNoPictureThemes()
	{
		PictureThemes.install(PictureFixtures.catalogue(null));
		assertTrue(PictureThemes.current().groups().isEmpty());
		assertSame(CardStyle.VOID, CardStyle.of(CardTheme.VOID, "anything"));
	}
}
