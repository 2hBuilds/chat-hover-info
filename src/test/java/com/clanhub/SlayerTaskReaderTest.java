package com.clanhub;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link SlayerTaskReader}: the answer of RuneLite's Slayer plugin to a clan mate's {@code !task}, "Slayer Task: <task>[
 * (<place>)]: <killed>/<total> killed" or "... <n> left", is read from the text as drawn into one quiet card, and nothing
 * else is. Lot 14B; the frozen tests of its plan.
 */
public class SlayerTaskReaderTest
{
	private static final String FIXTURE = "/com/clanhub/slayer-fixture.json";
	/** One boss the way the boss table writes it; its combat level is not the slayer fixture's, so a card shows whose it is. */
	private static final String BOSSES = "{\"bosses\": [{\"name\": \"Cerberus\", \"wiki\": \"Cerberus\", \"combat\": \"320\","
		+ " \"hitpoints\": \"600\", \"maxHit\": \"23 (Melee)\", \"quest\": \"\"},"
		+ " {\"name\": \"Abyssal Sire\", \"wiki\": \"Abyssal_Sire\", \"combat\": \"350\", \"hitpoints\": \"400\","
		+ " \"maxHit\": \"\", \"quest\": \"\"}]}";

	/** The slayer fixture's table, with the one-boss table when {@code withBosses}. */
	private static CardTables tables(boolean withBosses) throws Exception
	{
		final CardTables tables = CardTables.parse(new StringReader("{}"), new Gson());
		if (withBosses)
		{
			tables.readBosses(new StringReader(BOSSES), new Gson());
		}
		try (Reader in = new InputStreamReader(SlayerTaskReaderTest.class.getResourceAsStream(FIXTURE), StandardCharsets.UTF_8))
		{
			return tables.readSlayer(in, new Gson());
		}
	}

	/** The one card a text gives. */
	private static HoverCard theCard(String text, CardTables tables)
	{
		final List<HoverCard> cards = SlayerTaskReader.read(text, tables);
		assertEquals("one card from: " + text, 1, cards.size());
		return cards.get(0);
	}

	/**
	 * The card's words as each pair's label and value, in order, as the card said them before the user's layout pick: the
	 * progress (now the bar's words), the facts (the card's one row), the drops (now the drops line, the rows after the first).
	 */
	private static List<String> pairsOf(HoverCard card)
	{
		// Re-aimed by the lead, 2026-10-28: the user's layout pick. The progress is the bar's words and the drops are the
		// rows after the first; the helper puts them back in the order the card once listed them, so every assertion stands.
		final List<String> pairs = new ArrayList<>();
		pairs.add("Progress " + card.progress().text);
		for (final HoverCard.Pair pair : card.rows().get(0))
		{
			pairs.add(pair.label + " " + pair.value);
		}
		for (final HoverCard.Pair pair : CardPainter.dropsOf(card))
		{
			pairs.add(pair.label + " " + pair.value);
		}
		return pairs;
	}

	@Test
	public void readsTheUsersRealHellhoundsLine() throws Exception
	{
		// The line from the user's screenshot as the overlay hands it over: ChatText.plain turns the name's no-break space
		// into a plain one.
		final String text = ChatText.plain("xB0RNx\u00A0HABS: Slayer Task: Hellhounds: 10/155 killed");
		final HoverCard card = theCard(text, tables(true));
		assertEquals("Hellhounds", card.title());
		assertEquals("Hellhounds", card.plainLine().substring(card.start(), card.end()));
		assertEquals("xB0RNx HABS: Slayer Task: Hellhounds: 10/155 killed", card.plainLine());
		assertEquals("Progress 10/155", pairsOf(card).get(0));
		assertFalse("quiet: there is no event for the rewrite, so the chat cannot colour the words", card.marked());
		assertTrue("it asks for the wiki's picture", card.wikiPicture());
		assertEquals("and the Slayer icon stands behind it", SkillSprites.spriteOf("Slayer"), card.spriteId());
		assertEquals(-1, card.itemId());
		assertEquals("Hellhound", card.wikiTitle());
		assertEquals("https://oldschool.runescape.wiki/w/Hellhound", card.wikiUrl());
		assertTrue("the card has no paragraph of its own: the wiki's first one is asked for", card.notes().isEmpty());
	}

	@Test
	public void theFactsAreTheOnesTheTableKnowsInTheirOrder() throws Exception
	{
		assertEquals("a task that needs no Slayer level does not say so",
			Arrays.asList("Progress 10/155", "Combat 122-136", "Smouldering stone 1/16384",
				"Magic shortbow scroll 1/326.5"),
			pairsOf(theCard("Slayer Task: Hellhounds: 10/155 killed", tables(true))));
		assertEquals("a Slayer level, a combat level and the drops, the place after the progress",
			Arrays.asList("Progress 3/120", "Place Catacombs of Kourend", "Slayer 85", "Combat 124",
				"Abyssal dagger 1/32000", "Abyssal head 1/6000", "Abyssal whip 1/512"),
			pairsOf(theCard("Slayer Task: Abyssal demons (Catacombs of Kourend): 3/120 killed", tables(true))));
		assertEquals("a task the table knows by its name alone has only the progress",
			Arrays.asList("Progress 5/9"), pairsOf(theCard("Slayer Task: Banshees: 5/9 killed", tables(true))));
		assertEquals("an apostrophe in the task's name reads",
			Arrays.asList("Progress 1/3", "Combat 580"), pairsOf(theCard("Slayer Task: Kree'arra: 1/3 killed", tables(true))));
	}

	@Test
	public void readsThePlaceAndTheTaskAlone() throws Exception
	{
		final String text = "Slayer Task: Abyssal demons (Catacombs of Kourend): 3/120 killed";
		final HoverCard card = theCard(text, tables(true));
		assertEquals("Abyssal demons", card.title());
		assertEquals("Abyssal demons", card.plainLine().substring(card.start(), card.end()));
		assertEquals(Arrays.asList("Progress 3/120", "Place Catacombs of Kourend", "Slayer 85", "Combat 124"),
			pairsOf(card).subList(0, 4));
		assertEquals("Abyssal demon", card.wikiTitle());

		final String sent = "Klar: " + text;
		final HoverCard fromKlar = theCard(sent, tables(true));
		assertEquals("Abyssal demons", fromKlar.plainLine().substring(fromKlar.start(), fromKlar.end()));
		assertEquals("the words stand after the sender, the colon and the label", sent.indexOf("Abyssal"), fromKlar.start());
	}

	@Test
	public void readsTheAmountLeftAndThousandsCommas() throws Exception
	{
		final HoverCard left = theCard("Slayer Task: Hellhounds: 145 left", tables(true));
		assertEquals("Hellhounds", left.title());
		assertEquals("Progress 145 left", pairsOf(left).get(0));

		final HoverCard leftAt = theCard("Slayer Task: Abyssal demons (Catacombs of Kourend): 1,203 left", tables(true));
		assertEquals("Progress 1,203 left", pairsOf(leftAt).get(0));
		assertEquals("Place Catacombs of Kourend", pairsOf(leftAt).get(1));

		assertEquals("Progress 1,203/1,500", pairsOf(theCard("Slayer Task: Hellhounds: 1,203/1,500 killed", tables(true))).get(0));
	}

	@Test
	public void readsNoOtherLine() throws Exception
	{
		final String[] none = {
			"Slayer Task: Banana men: 1/2 killed",
			"Slayer Task: Hellhounds",
			"Slayer Task: Hellhounds:",
			"my slayer task: hellhounds lol",
			"Klar: my slayer task: hellhounds lol",
			"Slayer Task: Hellhounds: 10/155 killed lol",
			"zambzy: Slayer Task: Hellhounds: 10/155 killed!",
			"Slayer task: Hellhounds: 10/155 killed",
			"Slayer Task: Hellhounds: x/155 killed",
			"Slayer Task: Hellhounds: 10/ killed",
			"Slayer Task: Hellhounds: 10 killed",
			"Slayer Task: : 10/155 killed",
			"Slayer Task: Hellhounds (): 10/155 killed",
			"!task",
			"Klar: gz",
			"",
		};
		final CardTables tables = tables(true);
		for (final String text : none)
		{
			assertTrue("nothing from: " + text, SlayerTaskReader.read(text, tables).isEmpty());
		}
		assertTrue("nothing from null", SlayerTaskReader.read(null, tables).isEmpty());
		assertTrue("nothing from a table that has not loaded",
			SlayerTaskReader.read("Slayer Task: Hellhounds: 10/155 killed", new CardTables()).isEmpty());
		assertTrue("nothing from a table that has the cards but no slayer tasks",
			SlayerTaskReader.read("Slayer Task: Hellhounds: 10/155 killed",
				CardTables.parse(new StringReader("{}"), new Gson())).isEmpty());
	}

	@Test
	public void aBossTaskCarriesTheBossTablesFacts() throws Exception
	{
		final HoverCard cerberus = theCard("Slayer Task: Cerberus: 2/5 killed", tables(true));
		assertEquals("Cerberus", cerberus.title());
		assertEquals("the boss table's figures replace the slayer table's combat level",
			Arrays.asList("Progress 2/5", "Slayer 91", "Combat 320", "Hitpoints 600", "Max hit 23 (Melee)",
				"Hellpuppy 1/3000"), pairsOf(cerberus));
		assertEquals("Cerberus", cerberus.wikiTitle());

		final HoverCard sire = theCard("Slayer Task: The Abyssal Sire: 7/80 killed", tables(true));
		assertEquals("The Abyssal Sire", sire.title());
		assertEquals("a boss the table spells without its The is found all the same",
			Arrays.asList("Progress 7/80", "Slayer 85", "Combat 350", "Hitpoints 400"), pairsOf(sire));

		assertEquals("without the boss table the slayer table's combat level stands",
			Arrays.asList("Progress 2/5", "Slayer 91", "Combat 318", "Hellpuppy 1/3000"),
			pairsOf(theCard("Slayer Task: Cerberus: 2/5 killed", tables(false))));
	}

	@Test
	public void theDemoLineGivesItsCardWhenDrawn() throws Exception
	{
		assertEquals("the demo has one slayer line", 1, DemoLines.SLAYER.size());
		final DemoLines.Line line = DemoLines.SLAYER.get(0);
		assertEquals(ChatMessageType.CLAN_CHAT, line.type);
		assertEquals("Klar", line.name);
		assertEquals("Slayer Task: Hellhounds: 10/155 killed", line.text);
		assertTrue("it is posted with the other samples", DemoLines.lines().contains(line));

		// The chatbox draws a clan chat line as its sender, a colon and the text.
		final String drawn = line.name + ": " + line.text;
		final HoverCard card = theCard(ChatText.plain(drawn), tables(true));
		assertEquals("Hellhounds", card.title());
		assertEquals("Hellhounds", card.plainLine().substring(card.start(), card.end()));
		assertEquals("the same card the table and the reader above give for the real line", "Progress 10/155",
			pairsOf(card).get(0));
	}
}
