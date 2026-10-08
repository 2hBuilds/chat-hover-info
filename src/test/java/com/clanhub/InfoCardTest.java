package com.clanhub;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link InfoCard}: the wiki page names, and what the card says it is. */
public class InfoCardTest
{
	@Test
	public void wikiTitleIsTheNameOrThePage()
	{
		final HoverCard.Pair points = new HoverCard.Pair("Points", "5", "");
		final InfoCard slayer = new InfoCard("Dragon Slayer II", Collections.singletonList(points),
			Collections.<String>emptyList(), "Dragon_Slayer_II", "Klar has completed a quest: Dragon Slayer II", 28, 44,
			SkillSprites.QUEST);
		assertEquals("Dragon Slayer II", slayer.wikiTitle());
		final InfoCard cook = new InfoCard("Why Cook?", Collections.singletonList(points),
			Collections.<String>emptyList(), "Why_Cook%3F", "Klar has completed a diary: Why Cook?", 28, 37,
			SkillSprites.QUEST);
		assertEquals("Why Cook?", cook.wikiTitle());
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8L): the card no longer wraps; the painter does.
	@Test
	public void buildsPagesAndSaysWhatItIs()
	{
		assertEquals("King_Black_Dragon", InfoCard.pageFor("King Black Dragon"));
		assertEquals("Lumbridge_%26_Draynor_Diary", InfoCard.pageFor("Lumbridge & Draynor Diary"));
		assertEquals("Vet%27ion_Jr.", InfoCard.pageFor("Vet'ion Jr."));

		final InfoCard card = new InfoCard("Dragon Slayer II", Collections.singletonList(
			new HoverCard.Pair("Points", "5", "")), Collections.<String>emptyList(), "Dragon_Slayer_II",
			"Klar has completed a quest: Dragon Slayer II", 28, 44, SkillSprites.QUEST);
		assertEquals("https://oldschool.runescape.wiki/w/Dragon_Slayer_II", card.wikiUrl());
		assertEquals(-1, card.itemId());
		assertEquals(1, card.quantity());
		final ItemCard item = new ItemCard(13652, "Dragon claws", 1, 0L, 0, "x", 0, 1);
		assertEquals(item.hint(), card.hint());

		// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture is a card's say, false for the three older constructors.
		assertFalse("the shortest constructor has no wiki picture", card.wikiPicture());
		final InfoCard withItem = new InfoCard("Pet", Collections.singletonList(new HoverCard.Pair("Rate", "1/4000", "")),
			Collections.<String>emptyList(), "Pet_snakeling", "x", 0, 1, -1, 12921);
		assertFalse("the item constructor has none", withItem.wikiPicture());
		final InfoCard quiet = new InfoCard("Defence 70", Collections.singletonList(new HoverCard.Pair("New unlocks", "4", "")),
			Collections.<String>emptyList(), "Defence", "x", 0, 1, 199, -1, false);
		assertFalse("the quiet constructor has none", quiet.wikiPicture());
		final InfoCard boss = new InfoCard("Duke Sucellus", Collections.<HoverCard.Pair>emptyList(),
			Collections.<String>emptyList(), "Duke_Sucellus", "x", 0, 1, 168, -1, false, true);
		assertTrue("the fullest constructor says it", boss.wikiPicture());
		assertFalse("and still says quiet", boss.marked());
	}
}
