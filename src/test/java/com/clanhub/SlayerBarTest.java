package com.clanhub;

import com.clanhub.ClanHubConfig.CardTheme;
import com.clanhub.ClanHubConfig.CardWidth;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Lot 14C, the frozen tests of the user's layout pick for the slayer task card: the progress under the title is a bar
 * (a fact "Progress" on the Wiki theme), the drops are the card's own grey "Drops:" line, and a task shows its rarest
 * five. The pixel proof that no other card changed is the local-only {@code CardPaintUnchangedTest}.
 */
public class SlayerBarTest
{
	private static final String XP_FIXTURE = "/com/clanhub/slayer-xp-fixture.json";
	private static final String BUNDLED = "/com/clanhub/slayer.json";
	private static final int FONT_SIZE = 16;

	private static CardTables tables(String slayerResource) throws Exception
	{
		final CardTables tables = CardTables.parse(new StringReader("{}"), new Gson());
		try (Reader bosses = new InputStreamReader(SlayerBarTest.class.getResourceAsStream("/com/clanhub/bosses.json"),
			StandardCharsets.UTF_8))
		{
			tables.readBosses(bosses, new Gson());
		}
		try (Reader in = new InputStreamReader(SlayerBarTest.class.getResourceAsStream(slayerResource), StandardCharsets.UTF_8))
		{
			return tables.readSlayer(in, new Gson());
		}
	}

	private static HoverCard card(String line, CardTables tables)
	{
		final List<HoverCard> cards = SlayerTaskReader.read(line, tables);
		assertEquals("one card from: " + line, 1, cards.size());
		return cards.get(0);
	}

	/** The card painted as the overlay paints the split card, in the theme, with no picture. */
	private static BufferedImage paint(HoverCard card, CardTheme theme)
	{
		final CardStyle style = CardStyle.of(theme);
		final CardWidth width = CardWidth.MEDIUM;
		final String description = "A paragraph of plain text, long enough to take two lines at the width of the card, so the rule is painted.";
		final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D measure = scratch.createGraphics();
		final Dimension size;
		try
		{
			size = CardPainter.paintSplit(measure, card, null, FONT_SIZE, 0, 0, description, width.cap, width.lines, style);
		}
		finally
		{
			measure.dispose();
		}
		final BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			CardPainter.paintSplit(g, card, null, FONT_SIZE, 0, 0, description, width.cap, width.lines, style);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/**
	 * The bar a painted card has, as {track length, filled length} in px, or null for none: a row that has a run of at least
	 * 60 pixels, each in the style's track colour (its rule colour) or its fill colour (its value colour), narrower than the
	 * card's content (the frame and the rule across the card are runs of the track colour as wide as that), the fill first.
	 */
	private static int[] barOf(BufferedImage image, CardStyle style)
	{
		final int track = style.rule.getRGB() & 0xFFFFFF;
		final int fill = style.value.getRGB() & 0xFFFFFF;
		final int content = image.getWidth() - 2 * style.inset;
		for (int y = 0; y < image.getHeight(); y++)
		{
			int x = 0;
			while (x < image.getWidth())
			{
				int end = x;
				while (end < image.getWidth() && isBar(image.getRGB(end, y), track, fill))
				{
					end++;
				}
				if (end - x >= 60 && end - x < content)
				{
					int filled = 0;
					while (x + filled < end && (image.getRGB(x + filled, y) & 0xFFFFFF) == fill)
					{
						filled++;
					}
					return new int[] {end - x, filled};
				}
				x = Math.max(end, x + 1);
			}
		}
		return null;
	}

	private static boolean isBar(int argb, int track, int fill)
	{
		final int rgb = argb & 0xFFFFFF;
		return (argb >>> 24) == 0xFF && (rgb == track || rgb == fill);
	}

	private static List<String> labels(HoverCard card)
	{
		final List<String> labels = new ArrayList<>();
		for (final HoverCard.Pair pair : card.rows().get(0))
		{
			labels.add(pair.label);
		}
		return labels;
	}

	@Test
	public void theBarIsFilledToTheKillsOfTheTotal() throws Exception
	{
		final HoverCard card = card("Slayer Task: Hellhounds: 10/155 killed", tables(XP_FIXTURE));
		assertNotNull(card.progress());
		assertEquals("10/155", card.progress().text);
		assertEquals(10.0 / 155.0, card.progress().fraction, 1e-9);
		assertEquals("16,820", card.progress().xpLeft);
		final CardStyle classic = CardStyle.of(CardTheme.CLASSIC);
		final int[] bar = barOf(paint(card, CardTheme.CLASSIC), classic);
		assertNotNull("the card has a bar", bar);
		assertEquals("the bar fills 10/155 of its track, within 1 px: " + bar[1] + " of " + bar[0], 10.0 / 155.0 * bar[0], bar[1], 1.0);
		assertTrue("and the fill is more than a mark", bar[1] >= 2);
	}

	@Test
	public void aFullAndAnOverfullTaskFillTheBar() throws Exception
	{
		final CardTables tables = tables(XP_FIXTURE);
		assertEquals(1.0, card("Slayer Task: Hellhounds: 155/155 killed", tables).progress().fraction, 1e-9);
		assertEquals("the fill never passes the track", 1.0, card("Slayer Task: Hellhounds: 160/155 killed", tables).progress().fraction, 1e-9);
		assertEquals(0.0, card("Slayer Task: Hellhounds: 0/155 killed", tables).progress().fraction, 1e-9);
		final int[] bar = barOf(paint(card("Slayer Task: Hellhounds: 155/155 killed", tables), CardTheme.CLASSIC), CardStyle.of(CardTheme.CLASSIC));
		assertNotNull(bar);
		assertEquals("a finished task's bar is filled to its end", bar[0], bar[1]);
	}

	@Test
	public void anAmountLeftHasNoBarAndShowsTheAmountAndTheXpLeftAsText() throws Exception
	{
		final HoverCard card = card("Slayer Task: Hellhounds: 145 left", tables(XP_FIXTURE));
		assertNotNull(card.progress());
		assertEquals("145 left", card.progress().text);
		assertTrue("there is no total to fill a bar to", card.progress().fraction < 0);
		assertEquals("16,820", card.progress().xpLeft);
		assertNull("no bar is painted", barOf(paint(card, CardTheme.CLASSIC), CardStyle.of(CardTheme.CLASSIC)));
		assertFalse("and no Progress fact stands in for it", labels(card).contains("Progress"));
	}

	@Test
	public void noXpLeftWhenTheXpIsARangeOrNone() throws Exception
	{
		final CardTables tables = tables(XP_FIXTURE);
		assertEquals("", card("Slayer Task: Abyssal demons: 10/155 killed", tables).progress().xpLeft);
		assertEquals("", card("Slayer Task: Banshees: 5/9 killed", tables).progress().xpLeft);
		assertEquals("", card("Slayer Task: Cows: 1/3 killed", tables).progress().xpLeft);
		assertNotNull("the bar is there all the same", barOf(paint(card("Slayer Task: Banshees: 5/9 killed", tables), CardTheme.CLASSIC),
			CardStyle.of(CardTheme.CLASSIC)));
	}

	@Test
	public void noDropNameIsAmongTheFacts() throws Exception
	{
		final HoverCard card = card("Slayer Task: Hellhounds: 10/155 killed", tables(XP_FIXTURE));
		assertEquals("the facts are the combat level and the XP", Arrays.asList("Combat", "Slayer XP"), labels(card));
		assertFalse(labels(card).contains("Smouldering stone"));
		assertFalse("Progress is the bar, not a fact", labels(card).contains("Progress"));
		assertFalse("and so is the XP left", labels(card).contains("XP left"));
	}

	@Test
	public void theDropsAreTheCardsOwnDropsLine() throws Exception
	{
		final HoverCard card = card("Slayer Task: Hellhounds: 10/155 killed", tables(XP_FIXTURE));
		assertEquals("Drops: Smouldering stone 1/32768", dropsLine(card));
		final HoverCard two = card("Slayer Task: Hellhounds: 10/155 killed", tables("/com/clanhub/slayer-fixture.json"));
		assertEquals("Drops: Smouldering stone 1/16384, Magic shortbow scroll 1/326.5", dropsLine(two));
		assertEquals("a task with no drops has no line", "", dropsLine(card("Slayer Task: Cows: 1/3 killed", tables(XP_FIXTURE))));
	}

	/** The line the painter sets from the card's drops: "Drops:" then each source and its rate, a comma between. */
	private static String dropsLine(HoverCard card)
	{
		final List<HoverCard.Pair> drops = CardPainter.dropsOf(card);
		if (drops.isEmpty())
		{
			return "";
		}
		final StringBuilder line = new StringBuilder("Drops:");
		for (int i = 0; i < drops.size(); i++)
		{
			line.append(i == 0 ? " " : ", ").append(drops.get(i).label).append(' ').append(drops.get(i).value);
		}
		return line.toString();
	}

	@Test
	public void theWikiThemeShowsTheProgressAsARowAndTheXpLeftAsAnother() throws Exception
	{
		final HoverCard card = card("Slayer Task: Hellhounds: 10/155 killed", tables(XP_FIXTURE));
		final List<String> rows = new ArrayList<>();
		for (final HoverCard.Pair pair : WikiCard.factsOf(card))
		{
			rows.add(pair.label + " " + pair.value);
		}
		assertEquals(Arrays.asList("Progress 10/155", "XP left 16,820", "Combat 122-136", "Slayer XP 116"), rows);
		final List<String> left = new ArrayList<>();
		for (final HoverCard.Pair pair : WikiCard.factsOf(card("Slayer Task: Hellhounds: 145 left", tables(XP_FIXTURE))))
		{
			left.add(pair.label + " " + pair.value);
		}
		assertEquals("Progress 145 left", left.get(0));
		assertEquals("XP left 16,820", left.get(1));
	}

	@Test
	public void everyOtherCardHasNoProgress() throws Exception
	{
		assertNull(new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, "Dragon claws", 0, 12).progress());
		assertNull(new InfoCard("Dragon Slayer II", Arrays.asList(new HoverCard.Pair("Quest completed", "", "")),
			new ArrayList<String>(), "Dragon_Slayer_II", "Klar has completed a quest: Dragon Slayer II", 0, 4,
			SkillSprites.QUEST).progress());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// the bundled table: the rarest five, without the junk
	// ---------------------------------------------------------------------------------------------------------------

	private static List<String> bundledDrops(CardTables tables, String task)
	{
		final List<String> drops = new ArrayList<>();
		for (final CardTables.SlayerInfo.Drop drop : tables.slayer(task).drops)
		{
			drops.add(drop.item + " " + drop.rate);
		}
		return drops;
	}

	@Test
	public void theBundledDropsAreTheRarestFive() throws Exception
	{
		Assume.assumeNotNull(CardTables.class.getResource(BUNDLED));
		final CardTables tables = tables(BUNDLED);

		final HoverCard cerberus = card("Slayer Task: Cerberus: 12/80 killed", tables);
		assertEquals("Cerberus carries five drops", 5, CardPainter.dropsOf(cerberus).size());

		final List<String> dust = bundledDrops(tables, "Dust devils");
		assertTrue(dust.toString(), dust.contains("Dragon chainbody 1/32768") && dust.contains("Dust battlestaff 1/4000"));
		final List<String> vetion = bundledDrops(tables, "Vet'ion");
		assertTrue(vetion.toString(), vetion.contains("Skeleton champion scroll 1/5000") && vetion.contains("Voidwaker blade 1/360"));
		for (final String drop : bundledDrops(tables, "The Abyssal Sire"))
		{
			assertFalse("no talisman on the Sire: " + drop, drop.toLowerCase().contains("talisman"));
		}
		assertEquals("Hellhounds' rarest is the Smouldering stone", "Smouldering stone 1/32768", bundledDrops(tables, "Hellhounds").get(0));

		final Pattern junk = Pattern.compile("talisman|^clue |\\bseeds?\\b|^grimy |\\bhalf of\\b.*\\bkey\\b|^(tooth|loop) half|drop table|^nothing",
			Pattern.CASE_INSENSITIVE);
		for (final String task : new String[] {"Dust devils", "Vet'ion", "The Abyssal Sire", "Cerberus", "Hellhounds", "Gargoyles", "Nechryael",
			"Black demons", "Abyssal demons", "Hydras", "Zulrah", "Vorkath", "Icefiends"})
		{
			final CardTables.SlayerInfo info = tables.slayer(task);
			assertTrue(task + " shows at most five: " + info.drops.size(), info.drops.size() <= 5);
			for (final CardTables.SlayerInfo.Drop drop : info.drops)
			{
				assertFalse(task + " has junk: " + drop.item, junk.matcher(drop.item).find());
			}
		}
	}

	@Test
	public void noStackOfCoinsIsADrop() throws Exception
	{
		// The user, 2026-10-28: coins are skipped, as the junk list is. Aquanites and Icefiends once showed them.
		Assume.assumeNotNull(CardTables.class.getResource(BUNDLED));
		final CardTables tables = tables(BUNDLED);
		for (final String task : new String[] {"Aquanites", "Icefiends"})
		{
			assertFalse(task + " has no Coins: " + bundledDrops(tables, task), bundledDrops(tables, task).toString().contains("Coins"));
			assertFalse(task + " has a drops line all the same", bundledDrops(tables, task).isEmpty());
		}
		final JsonArray entries;
		try (Reader in = new InputStreamReader(SlayerBarTest.class.getResourceAsStream(BUNDLED), StandardCharsets.UTF_8))
		{
			entries = new Gson().fromJson(in, JsonObject.class).getAsJsonArray("tasks");
		}
		for (final JsonElement entry : entries)
		{
			for (final JsonElement drop : entry.getAsJsonObject().getAsJsonArray("drops"))
			{
				assertFalse(entry.getAsJsonObject().get("name").getAsString() + " names coins",
					drop.getAsJsonObject().get("item").getAsString().toLowerCase().startsWith("coins"));
			}
		}
	}

	@Test
	public void theBundledDropsRunFromTheRarestToTheCommonest() throws Exception
	{
		Assume.assumeNotNull(CardTables.class.getResource(BUNDLED));
		final CardTables tables = tables(BUNDLED);
		int tasksWithFive = 0;
		for (final String task : new String[] {"Aberrant spectres", "Abyssal demons", "Cerberus", "Dust devils", "Gargoyles", "Hellhounds",
			"Hydras", "Nechryael", "The Abyssal Sire", "Vet'ion", "Waterfiends", "Zulrah"})
		{
			final List<CardTables.SlayerInfo.Drop> drops = tables.slayer(task).drops;
			double chance = 0;
			for (final CardTables.SlayerInfo.Drop drop : drops)
			{
				final double now = 1.0 / Double.parseDouble(drop.rate.substring(2));
				assertTrue(task + " is rarest first at " + drop.item + " " + drop.rate, now >= chance - 1e-12);
				chance = now;
			}
			tasksWithFive += drops.size() == 5 ? 1 : 0;
		}
		assertTrue("most of them have a full line: " + tasksWithFive, tasksWithFive >= 8);
	}
}
