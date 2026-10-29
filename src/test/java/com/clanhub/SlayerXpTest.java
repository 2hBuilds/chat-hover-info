package com.clanhub;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Lot 14C, the frozen tests of its plan: the slayer task card says the Slayer XP a kill gives and the XP left on the task,
 * and the bundled table carries the XP and the three slayer uniques that are always named (Dust battlestaff, Mist battlestaff,
 * Lava battlestaff).
 */
public class SlayerXpTest
{
	private static final String FIXTURE = "/com/clanhub/slayer-xp-fixture.json";
	private static final String BUNDLED = "/com/clanhub/slayer.json";
	/** One boss the way the boss table writes it. */
	private static final String BOSSES = "{\"bosses\": [{\"name\": \"Cerberus\", \"wiki\": \"Cerberus\", \"combat\": \"320\","
		+ " \"hitpoints\": \"600\", \"maxHit\": \"23 (Melee)\", \"quest\": \"\"}]}";

	private static CardTables tables(String resource, boolean withBosses) throws Exception
	{
		final CardTables tables = CardTables.parse(new StringReader("{}"), new Gson());
		if (withBosses)
		{
			tables.readBosses(new StringReader(BOSSES), new Gson());
		}
		try (Reader in = new InputStreamReader(SlayerXpTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8))
		{
			return tables.readSlayer(in, new Gson());
		}
	}

	/**
	 * The one card's words as each pair's label and value, in order, as the card said them before the user's layout pick: the
	 * progress, the facts, the XP left, the drops.
	 */
	private static List<String> pairsOf(String text, CardTables tables)
	{
		// Re-aimed by the lead, 2026-10-28: the user's layout pick. The progress and the XP left are the bar's words, the
		// drops the rows after the first; the helper puts them back in the order the card once listed them (the progress
		// first, then the facts, the XP left and the drops), so every assertion stands.
		final List<HoverCard> cards = SlayerTaskReader.read(text, tables);
		assertEquals("one card from: " + text, 1, cards.size());
		final HoverCard card = cards.get(0);
		final List<String> pairs = new ArrayList<>();
		pairs.add("Progress " + card.progress().text);
		for (final HoverCard.Pair pair : card.rows().get(0))
		{
			pairs.add(pair.label + " " + pair.value);
		}
		if (!card.progress().xpLeft.isEmpty())
		{
			pairs.add("XP left " + card.progress().xpLeft);
		}
		for (final HoverCard.Pair pair : CardPainter.dropsOf(card))
		{
			pairs.add(pair.label + " " + pair.value);
		}
		return pairs;
	}

	@Test
	public void theXpEachAndTheXpLeftFollowTheCombatLevel() throws Exception
	{
		final CardTables tables = tables(FIXTURE, true);
		// Re-aimed by the lead, 2026-10-28: the user dropped "each" ("Slayer XP 116", "Slayer XP 116-130").
		assertEquals("10 of 155 killed leaves 145 at 116 each",
			Arrays.asList("Progress 10/155", "Combat 122-136", "Slayer XP 116", "XP left 16,820",
				"Smouldering stone 1/32768"), pairsOf("Slayer Task: Hellhounds: 10/155 killed", tables));
		assertEquals("the place comes after the progress, the XP after the Slayer level and the combat level",
			Arrays.asList("Progress 3/120", "Place Catacombs of Kourend", "Slayer 85", "Combat 124", "Slayer XP 116-130",
				"Abyssal whip 1/512"),
			pairsOf("Slayer Task: Abyssal demons (Catacombs of Kourend): 3/120 killed", tables));
	}

	@Test
	public void theAmountLeftIsMultipliedAsItStands() throws Exception
	{
		final CardTables tables = tables(FIXTURE, true);
		// Re-aimed by the lead, 2026-10-28: the user dropped "each".
		assertEquals(Arrays.asList("Progress 145 left", "Combat 122-136", "Slayer XP 116", "XP left 16,820",
			"Smouldering stone 1/32768"), pairsOf("Slayer Task: Hellhounds: 145 left", tables));
		assertTrue("a thousands comma reads in the amounts",
			pairsOf("Slayer Task: Hellhounds: 1,203/1,500 killed", tables).contains("XP left 34,452"));
		assertTrue("and in an amount left", pairsOf("Slayer Task: Hellhounds: 1,203 left", tables).contains("XP left 139,548"));
		assertTrue("a finished task has none left",
			pairsOf("Slayer Task: Hellhounds: 155/155 killed", tables).contains("XP left 0"));
		assertTrue("and more kills than the task asked for clamp at none",
			pairsOf("Slayer Task: Hellhounds: 160/155 killed", tables).contains("XP left 0"));
	}

	@Test
	public void aRangeOfXpGivesNoXpLeft() throws Exception
	{
		final List<String> pairs = pairsOf("Slayer Task: Abyssal demons: 10/155 killed", tables(FIXTURE, true));
		assertTrue(pairs.toString(), pairs.contains("Slayer XP 116-130"));
		for (final String pair : pairs)
		{
			assertFalse("no XP left from a range: " + pair, pair.startsWith("XP left"));
		}
		for (final String pair : pairsOf("Slayer Task: Abyssal demons: 145 left", tables(FIXTURE, true)))
		{
			assertFalse("nor from an amount left: " + pair, pair.startsWith("XP left"));
		}
	}

	@Test
	public void noXpGivesNeitherFact() throws Exception
	{
		final CardTables tables = tables(FIXTURE, true);
		assertEquals("a task whose XP is empty", Arrays.asList("Progress 5/9", "Slayer 15", "Combat 23",
			"Mystic gloves (dark) 1/512"), pairsOf("Slayer Task: Banshees: 5/9 killed", tables));
		assertEquals("a task with no xp member at all", Arrays.asList("Progress 1/3", "Combat 2"),
			pairsOf("Slayer Task: Cows: 1/3 killed", tables));
	}

	@Test
	public void aHalfXpKeepsItsDecimal() throws Exception
	{
		final CardTables tables = tables(FIXTURE, true);
		// Re-aimed by the lead, 2026-10-28: the user dropped "each".
		assertEquals(Arrays.asList("Progress 3/10", "Slayer 7", "Combat 6", "Slayer XP 62.5", "XP left 437.5"),
			pairsOf("Slayer Task: Cave bugs: 3/10 killed", tables));
		assertTrue(pairsOf("Slayer Task: Cave bugs: 4 left", tables).contains("XP left 250"));
	}

	@Test
	public void aBossTasksXpFollowsTheBossFactsAndComesBeforeTheDrops() throws Exception
	{
		// Re-aimed by the lead, 2026-10-28: the user dropped "each".
		assertEquals(Arrays.asList("Progress 2/5", "Slayer 91", "Combat 320", "Hitpoints 600", "Max hit 23 (Melee)",
			"Slayer XP 690", "XP left 2,070", "Hellpuppy 1/3000"),
			pairsOf("Slayer Task: Cerberus: 2/5 killed", tables(FIXTURE, true)));
	}

	@Test
	public void theBundledTableCarriesTheXpAndTheAlwaysNamedUniques() throws Exception
	{
		Assume.assumeNotNull(CardTables.class.getResource(BUNDLED));
		final CardTables tables = tables(BUNDLED, false);

		final CardTables.SlayerInfo hounds = tables.slayer("Hellhounds");
		assertNotNull(hounds);
		assertEquals("the wiki gives a Hellhound 116 Slayer XP", "116", hounds.xp);
		// Re-aimed by the lead, 2026-10-28: the user dropped "each"; and the user's layout pick: the table keeps the rarest five.
		final List<String> words = pairsOf("Slayer Task: Hellhounds: 10/155 killed", tables);
		assertEquals(Arrays.asList("Progress 10/155", "Combat 122-136", "Slayer XP 116", "XP left 16,820",
			"Smouldering stone 1/32768"), words.subList(0, 5));
		assertEquals("and four more, the rarest first", 9, words.size());

		assertTrue("Dust battlestaff is on Dust devils", dropsOf(tables, "Dust devils").contains("Dust battlestaff 1/4000"));
		assertTrue("Mist battlestaff is on Waterfiends", dropsOf(tables, "Waterfiends").contains("Mist battlestaff 1/3000"));
		assertTrue("Lava battlestaff is on Infernal mages",
			dropsOf(tables, "Infernal mages").contains("Lava battlestaff 1/1000"));
		for (final String task : new String[] {"Dust devils", "Waterfiends", "Infernal mages"})
		{
			// Re-aimed by the lead, 2026-10-28: the user's layout pick: the rarest five, no always-named list.
			assertTrue(task + " keeps at most five drops", tables.slayer(task).drops.size() <= 5);
		}
		for (final String task : new String[] {"Abyssal demons", "Gargoyles", "Nechryael", "Dust devils", "Cave kraken",
			"Smoke devils", "Hydras", "Cerberus"})
		{
			assertTrue(task + " has an XP: " + tables.slayer(task).xp, tables.slayer(task).xp.matches("\\d+(\\.\\d)?(-\\d+(\\.\\d)?)?"));
		}
	}

	private static List<String> dropsOf(CardTables tables, String task)
	{
		final List<String> drops = new ArrayList<>();
		for (final CardTables.SlayerInfo.Drop drop : tables.slayer(task).drops)
		{
			drops.add(drop.item + " " + drop.rate);
		}
		return drops;
	}
}
