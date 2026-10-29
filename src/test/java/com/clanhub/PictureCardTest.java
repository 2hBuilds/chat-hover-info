package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import com.clanhub.ClanHubConfig.SidebarTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Lot 15A: a card in a picture theme: as wide as the Medium card, its ground the stretched picture with a crisp edge ring,
 * its words in the theme's colours over it; and the sidebar's skin for a picture theme.
 */
public class PictureCardTest
{
	private static final Color FILL = new Color(30, 90, 60);
	private static final String PARAGRAPH = "A short paragraph of plain text for the card, long enough to take two lines at the card's width, "
		+ "so that the rule under the facts and the hint under the paragraph are painted too.";

	private PictureTheme theme;

	@Before
	public void installACatalogue()
	{
		theme = PictureFixtures.theme("card", "Calm", "bright", false, 1, 0.5, true);
		PictureThemes.install(PictureFixtures.catalogue(
			PictureFixtures.loader(FILL, new HashMap<String, Integer>(), new ArrayList<String>()), theme));
	}

	@After
	public void putTheCatalogueBack()
	{
		PictureThemes.install(PictureFixtures.catalogue(null));
	}

	private static BufferedImage split(HoverCard card, String description, CardStyle style)
	{
		final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D measure = scratch.createGraphics();
		final Dimension size;
		try
		{
			size = CardPainter.paintSplit(measure, card, null, 16, 0, 0, description, 409, 5, style);
		}
		finally
		{
			measure.dispose();
		}
		final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			CardPainter.paintSplit(g, card, null, 16, 0, 0, description, 409, 5, style);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	@Test
	public void aPictureCardIsAsWideAsTheMediumCardWhateverItHolds()
	{
		final CardStyle style = CardStyle.of(CardTheme.CLASSIC, "card");
		assertSame(theme, style.picture);
		final HoverCard card = ThemePreviews.sampleCard();
		assertEquals("a short card is still the Medium width", 409, split(card, "", style).getWidth());
		assertEquals("a long one too", 409, split(card, PARAGRAPH, style).getWidth());
		final HoverCard small = new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, "Klar received a drop: Dragon claws.", 22, 34);
		assertEquals("a card with little to say is still the Medium width", 409, split(small, "", style).getWidth());
		assertTrue("while in Classic it is narrower", split(small, "", CardStyle.CLASSIC).getWidth() < 409);
	}

	@Test
	public void theGroundIsThePictureWithACrispEdgeRingAndTheWordsAreInTheThemesColours()
	{
		final BufferedImage image = split(ThemePreviews.sampleCard(), PARAGRAPH, CardStyle.of(CardTheme.CLASSIC, "card"));
		final int w = image.getWidth();
		final int h = image.getHeight();
		assertEquals("the ring's corners", 0xFFFF0000, image.getRGB(0, 0));
		assertEquals(0xFFFF0000, image.getRGB(w - 1, h - 1));
		assertEquals(0xFFFF0000, image.getRGB(w / 2, 0));
		assertEquals(0xFFFF0000, image.getRGB(0, h / 2));
		assertEquals("one pixel in, the picture itself", FILL.getRGB(), image.getRGB(1, h / 2));
		assertEquals("far from every word the picture is untouched", FILL.getRGB(), image.getRGB(3, h - 3));
		boolean title = false;
		boolean text = false;
		boolean outline = false;
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				final int p = image.getRGB(x, y) & 0xFFFFFF;
				title |= p == 0xFFE890;
				text |= p == 0xF8F8F8;
				outline |= p == 0x000000;
			}
		}
		assertTrue("the title is in the theme's title colour", title);
		assertTrue("the paragraph is in its text colour", text);
		assertTrue("and the words have the black outline of light ink", outline);
	}

	/** The picture of a dark-ink theme is pale, and the sample card's words must be the theme's dark roles over a pale halo. */
	@Test
	public void aDarkInkThemePaintsItsTitleAndParagraphInItsDarkRolesWithNoBlackOutline()
	{
		final PictureTheme dark = PictureFixtures.theme("inky", "Calm", "bright", true, 0.5, 0.2, false);
		PictureThemes.install(PictureFixtures.catalogue(PictureFixtures.loader(new Color(250, 230, 235),
			new HashMap<String, Integer>(), new ArrayList<String>()), dark));
		final BufferedImage image = ThemePreviews.card(CardStyle.of(CardTheme.CLASSIC, "inky"), null);
		boolean title = false;
		boolean text = false;
		boolean white = false;
		for (int y = 3; y < image.getHeight() - 3; y++)
		{
			for (int x = 3; x < image.getWidth() - 3; x++)
			{
				final int p = image.getRGB(x, y) & 0xFFFFFF;
				title |= p == 0x202020;
				text |= p == 0x181818;
				white |= p == 0xFFFFFF || p == 0xFFE890 || p == 0xF8F8F8;
				assertNotEquals("no black outline pixel at " + x + "," + y, 0x000000, p);
			}
		}
		assertTrue("the title is in the theme's dark title colour", title);
		assertTrue("the paragraph is in its dark text colour", text);
		assertTrue("none of the light roles or white is drawn", !white);
	}

	@Test
	public void thePanelDarkensThePictureUnderTheWords()
	{
		final BufferedImage image = split(ThemePreviews.sampleCard(), PARAGRAPH, CardStyle.of(CardTheme.CLASSIC, "card"));
		boolean darker = false;
		for (int y = 10; y < image.getHeight() - 10 && !darker; y++)
		{
			for (int x = 10; x < image.getWidth() - 10; x++)
			{
				final int p = image.getRGB(x, y);
				if ((p & 0xFFFFFF) != 0 && (p >> 8 & 255) < FILL.getGreen() - 20 && (p >> 8 & 255) > 20 && (p >> 16 & 255) < 40)
				{
					darker = true;
					break;
				}
			}
		}
		assertTrue("some pixel under the lines is the picture pulled darker", darker);
	}

	@Test
	public void theWideCardPaintsOnAPictureToo()
	{
		final BufferedImage image = new BufferedImage(520, 300, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			CardPainter.paintWide(g, ThemePreviews.sampleCard(), null, 16, 520, 0, 0, PARAGRAPH, Color.BLUE,
				CardStyle.of(CardTheme.CLASSIC, "card"));
		}
		finally
		{
			g.dispose();
		}
		assertEquals(0xFFFF0000, image.getRGB(0, 0));
		assertEquals(0xFFFF0000, image.getRGB(519, 5));
	}

	@Test
	public void aPictureThemesSidebarIsACalmBoxOfItsTwoColoursNeverThePicture()
	{
		final SidebarSkin skin = SidebarSkin.ofPicture(theme);
		assertEquals("the page is the bottom colour", new Color(0x10, 0x18, 0x28), skin.page);
		assertEquals(theme.role("title", Color.WHITE), skin.title);
		assertEquals(theme.role("text", Color.WHITE), skin.text);
		final BufferedImage box = new BufferedImage(100, 40, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = box.createGraphics();
		try
		{
			skin.paintBox(g, 100, 40, SidebarSkin.Box.ROW);
		}
		finally
		{
			g.dispose();
		}
		assertEquals("the edge round the box", 0xFFFF0000, box.getRGB(0, 20));
		final int top = box.getRGB(10, 1);
		final int bottom = box.getRGB(10, 38);
		assertNotEquals("a gradient from the top colour to the bottom", top, bottom);
		assertTrue("every pixel is opaque", (box.getRGB(50, 20) >>> 24) == 255);
	}

	@Test
	public void theSkinFollowsThePictureThemeOrTheOriginalLook()
	{
		final SidebarSkin picture = SidebarSkin.pick(SidebarTheme.SAME, CardTheme.STONE, "card", "");
		assertEquals("Same as cards with a picture card theme wears its sidebar", theme.role("title", Color.WHITE), picture.title);
		assertSame("Same as cards with an original theme wears that theme's", SidebarSkin.of(CardTheme.STONE),
			SidebarSkin.pick(SidebarTheme.SAME, CardTheme.STONE, "", ""));
		assertSame("an unknown picture id is ignored", SidebarSkin.of(CardTheme.STONE),
			SidebarSkin.pick(SidebarTheme.SAME, CardTheme.STONE, "gone", "gone"));
		assertSame("RuneLite is the plain skin", SidebarSkin.PLAIN, SidebarSkin.pick(SidebarTheme.RUNELITE, CardTheme.STONE, "card", ""));
		assertSame("an original look of its own", SidebarSkin.of(CardTheme.VOID),
			SidebarSkin.pick(SidebarTheme.VOID, CardTheme.STONE, "card", ""));
		final SidebarSkin own = SidebarSkin.pick(SidebarTheme.RUNELITE, CardTheme.STONE, "", "card");
		assertEquals("a picture sidebar theme beats the look under it", theme.role("title", Color.WHITE), own.title);
	}
}
