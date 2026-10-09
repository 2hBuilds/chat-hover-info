package com.clanhub;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.ChatMessageType;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link BroadcastKindReader}: the quest, task, pet, level, XP, diary and personal best broadcasts and the player's own
 * kill count each give their card, raid loot gives the item card, and every other line, a combat level or total level
 * line among them, gives nothing.
 */
public class BroadcastKindReaderTest
{
	private static final String QUEST_LINE = "Klar has completed a quest: Dragon Slayer II";

	/** The tables of the fixture, read the way the plugin reads its own. */
	private static CardTables fixtureTables() throws Exception
	{
		try (Reader in = new InputStreamReader(BroadcastKindReaderTest.class.getResourceAsStream(
			"/com/clanhub/cards-fixture.json"), StandardCharsets.UTF_8))
		{
			return CardTables.parse(in, new Gson());
		}
	}

	/** A reader with the fixture's tables over the given item cards. */
	private static BroadcastKindReader reader(ItemCards itemCards) throws Exception
	{
		return new BroadcastKindReader(fixtureTables(), itemCards);
	}

	/** The row as the card shows it: each pair as its label and value, the pairs set apart by commas. */
	private static String rowOf(HoverCard card)
	{
		assertEquals("one row", 1, card.rows().size());
		final List<String> pairs = new ArrayList<>();
		for (final HoverCard.Pair pair : card.rows().get(0))
		{
			pairs.add((pair.label + " " + pair.value).trim());
		}
		return String.join(", ", pairs);
	}

	/** The one card a line of the type gives. */
	private static HoverCard theCard(BroadcastKindReader reader, ChatMessageType type, String line)
	{
		final List<HoverCard> cards = reader.read(type, line);
		assertEquals("one card from: " + line, 1, cards.size());
		return cards.get(0);
	}

	@Test
	public void questAndCombatTaskLines() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));

		final HoverCard quest = theCard(reader, ChatMessageType.CLAN_MESSAGE, QUEST_LINE);
		assertEquals("Dragon Slayer II", quest.title());
		assertEquals("Difficulty Grandmaster, Length Very Long, Points 5", rowOf(quest));
		assertEquals(899, quest.spriteId());
		assertTrue(quest.wikiUrl().endsWith("/w/Dragon_Slayer_II"));
		assertEquals(28, quest.start());
		assertEquals(44, quest.end());
		assertEquals(QUEST_LINE, quest.plainLine());
		assertTrue(quest.notes().isEmpty());
		assertTrue("a quest name is coloured in the chat", quest.marked());

		final HoverCard task = theCard(reader, ChatMessageType.CLAN_MESSAGE,
			"CA_ID:279|Klar has completed a Master combat task: Perfect Zulrah (5 points).");
		assertEquals("Perfect Zulrah", task.title());
		assertEquals("Tier Master, Points 5, Boss Zulrah", rowOf(task));
		// Re-aimed by the lead, 2026-10-08 (lot 8L): the description is one whole note; the painter wraps it.
		assertEquals(Collections.singletonList("Kill Zulrah whilst taking no damage from the following: Snakelings, "
			+ "Venom Clouds, Zulrah's Green or Crimson phase."), task.notes());
		assertEquals(3397, task.spriteId());
		assertEquals("Perfect Zulrah", task.plainLine().substring(task.start(), task.end()));
		assertTrue(task.wikiUrl().endsWith("/w/Perfect_Zulrah"));
		assertTrue("a task name is coloured in the chat", task.marked());

		final HoverCard plain = theCard(reader, ChatMessageType.CLAN_MESSAGE,
			"Klar has completed a quest: Made Up Quest");
		assertEquals("Made Up Quest", plain.title());
		assertEquals("Quest completed", rowOf(plain));
		assertTrue(plain.wikiUrl().endsWith("/w/Made_Up_Quest"));

		final HoverCard unknownTask = theCard(reader, ChatMessageType.CLAN_GIM_MESSAGE,
			"CA_ID:5|Klar has completed an Easy combat task: Made Up Task.");
		assertEquals("Made Up Task", unknownTask.title());
		assertEquals("Tier Easy", rowOf(unknownTask));
		assertEquals(3393, unknownTask.spriteId());
		assertTrue(unknownTask.notes().isEmpty());

		final BroadcastKindReader unloaded = new BroadcastKindReader(new CardTables(), Mockito.mock(ItemCards.class));
		final HoverCard early = theCard(unloaded, ChatMessageType.FRIENDSCHATNOTIFICATION, QUEST_LINE);
		assertEquals("Quest completed", rowOf(early));
		assertTrue(early.wikiUrl().endsWith("/w/Dragon_Slayer_II"));
	}

	@Test
	public void aQuestLineWithAStringVariableGivesTheQuestCard() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));
		final String plain = "Small Za has completed a quest: King's Ransom";
		final String raw = "<img=2> <str_quest_name_0=King's Ransom>Small Za has completed a quest: "
			+ "<str_quest_name_0>";

		final HoverCard quest = theCard(reader, ChatMessageType.CLAN_MESSAGE, raw);
		assertEquals(plain, quest.plainLine());
		assertEquals("King's Ransom", quest.plainLine().substring(quest.start(), quest.end()));
		assertTrue("a quest name is coloured in the chat", quest.marked());

		final HoverCard literal = theCard(reader, ChatMessageType.CLAN_MESSAGE, plain);
		assertEquals("King's Ransom", quest.title());
		assertEquals("the card of the quest written out", literal.title(), quest.title());
		assertEquals(rowOf(literal), rowOf(quest));
		assertEquals(literal.spriteId(), quest.spriteId());
		assertEquals(literal.wikiUrl(), quest.wikiUrl());
	}

	@Test
	public void petLevelTotalXpDiaryAndPersonalBestLines() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));
		final ChatMessageType clan = ChatMessageType.CLAN_MESSAGE;

		final String petLine = "Clipper has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount.";
		final HoverCard pet = theCard(reader, clan, petLine);
		assertEquals("Pet Snakeling", pet.title());
		// Re-aimed by the lead, 2026-10-08 (lot 8N): the row says what it is.
		assertEquals("Pet from Zulrah, Dropped at 2,678 killcount", rowOf(pet));
		assertEquals(-1, pet.spriteId());
		assertEquals("Pet snakeling", petLine.substring(pet.start(), pet.end()));
		assertTrue(pet.wikiUrl().endsWith("/w/Pet_Snakeling"));
		assertTrue("a pet name is coloured in the chat", pet.marked());

		final HoverCard backpack = theCard(reader, clan,
			"Dash Inc feels something weird sneaking into his backpack: Pet snakeling at 8,426,283 XP.");
		// Re-aimed by the lead, 2026-10-08 (lot 8N): the row says what it is.
		assertEquals("Pet from Zulrah, Dropped at 8,426,283 XP", rowOf(backpack));
		final HoverCard special = theCard(reader, clan,
			"ClanMate feels like she's acquired something special: Pet snakeling");
		assertEquals("Pet from Zulrah", rowOf(special));
		final HoverCard unknownPet = theCard(reader, clan,
			"ClanMate feels like she's acquired something special: Abyssal orphan");
		assertEquals("Abyssal orphan", unknownPet.title());
		assertEquals("Pet", rowOf(unknownPet));
		assertTrue(unknownPet.wikiUrl().endsWith("/w/Abyssal_orphan"));

		// Re-aimed by the lead, 2026-10-08 (lot 8g): the number is the card; the skill's word is never one.
		// Re-aimed by the lead, 2026-10-08 (lot 8j): the phrase is the card, and it is quiet in the chat.
		// (With the fixture's Defence unlocks the line gives the one unlocks card, which
		// aLevelLineGivesTheUnlocksOverThePhrase pins; the plain Defence card is read here before the tables load, every
		// assertion on it kept and its span the phrase.)
		final BroadcastKindReader early = new BroadcastKindReader(new CardTables(), Mockito.mock(ItemCards.class));
		final String defenceLine = "Th3TRiPPyOn3 has reached Defence level 70.";
		final HoverCard defence = theCard(early, clan, defenceLine);
		assertEquals("Defence level 70", defence.title());
		assertEquals("Skill Defence", rowOf(defence));
		assertEquals(199, defence.spriteId());
		assertTrue(defence.wikiUrl().endsWith("/w/Defence"));
		assertEquals("Defence level 70", defenceLine.substring(defence.start(), defence.end()));
		assertFalse("a level phrase is quiet", defence.marked());

		// Re-aimed by the lead, 2026-10-08 (lot 8V): combat level and total level lines give no card.
		assertTrue(reader.read(clan, "MechaPanzer has reached combat level 104.").isEmpty());
		assertTrue(reader.read(clan, "I Vision I has reached a total level of 2225.").isEmpty());

		final HoverCard xp = theCard(reader, clan, "Marsel has reached 200,000,000 XP in Cooking.");
		assertEquals("200,000,000 XP in Cooking", xp.title());
		assertEquals("Skill Cooking", rowOf(xp));
		assertEquals(212, xp.spriteId());
		assertTrue(xp.wikiUrl().endsWith("/w/Cooking"));
		// Re-aimed by the lead, 2026-10-08 (lot 8g): the number is the card; the skill's word is never one.
		// Re-aimed by the lead, 2026-10-08 (lot 8j): the phrase is the card, and it is quiet in the chat.
		assertEquals("200,000,000 XP in Cooking", xp.plainLine().substring(xp.start(), xp.end()));
		assertFalse("an XP phrase is quiet", xp.marked());

		final String diaryLine = "Player has completed the Elite Ardougne diary.";
		final HoverCard diary = theCard(reader, clan, diaryLine);
		assertEquals("Ardougne diary, Elite", diary.title());
		assertEquals("Tier Elite", rowOf(diary));
		assertEquals(1298, diary.spriteId());
		assertTrue(diary.wikiUrl().endsWith("/w/Ardougne_Diary"));
		assertEquals("Ardougne", diaryLine.substring(diary.start(), diary.end()));
		assertTrue("a diary area is coloured in the chat", diary.marked());
		final HoverCard unknownDiary = theCard(reader, clan, "Player has completed the Hard Lumbridge & Draynor diary.");
		assertEquals("Lumbridge & Draynor diary, Hard", unknownDiary.title());
		assertTrue(unknownDiary.wikiUrl().endsWith("/w/Lumbridge_%26_Draynor_Diary"));

		final HoverCard best = theCard(reader, clan, "Player has achieved a new Vorkath personal best: 2:28");
		assertEquals("Vorkath personal best", best.title());
		assertEquals("Time 2:28", rowOf(best));
		assertEquals(-1, best.spriteId());
		assertTrue(best.wikiUrl().endsWith("/w/Vorkath"));
		assertTrue("a personal best's activity is coloured in the chat", best.marked());
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8g): the number is the card; the skill's word is never one.
	// Re-aimed by the lead, 2026-10-08 (lot 8j): the phrase is the card, and it is quiet in the chat.
	@Test
	public void aLevelLineGivesTheUnlocksOverThePhrase() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));
		final ChatMessageType clan = ChatMessageType.CLAN_MESSAGE;

		final HoverCard unlocks = theCard(reader, clan, "Klar has reached Defence level 70.");
		assertEquals("Defence 70", unlocks.title());
		assertEquals("Defence level 70", unlocks.plainLine().substring(unlocks.start(), unlocks.end()));
		assertFalse("quiet", unlocks.marked());
		assertEquals("New unlocks 4", rowOf(unlocks));
		// Re-aimed by the lead, 2026-10-08 (lot 8L): the unlocks come whole, as sentences; the painter wraps them.
		// Re-aimed by the lead, 2026-10-08 (lot 8S): the unlocks are a list, no full stop is added.
		assertEquals(Arrays.asList("Wear Barrows armour", "Wear Bandos armour", "Wear Armadyl armour",
			"Wield the dragonfire shield"), unlocks.notes());
		assertTrue(unlocks.wikiUrl().endsWith("/w/Defence/Level_up_table"));
		assertEquals(199, unlocks.spriteId());

		final HoverCard nothing = theCard(reader, clan, "Klar has reached Defence level 72.");
		assertEquals("New unlocks None", rowOf(nothing));
		assertTrue(nothing.notes().isEmpty());
		assertEquals("Defence level 72", nothing.plainLine().substring(nothing.start(), nothing.end()));
		assertFalse("quiet", nothing.marked());

		final HoverCard crafting = theCard(reader, clan, "Klar has reached Crafting level 88.");
		assertEquals("the fixture has no Crafting", "Crafting level 88", crafting.title());
		assertEquals("Crafting level 88", crafting.plainLine().substring(crafting.start(), crafting.end()));
		assertEquals(207, crafting.spriteId());
		assertFalse("quiet", crafting.marked());

		// Re-aimed by the lead, 2026-10-08 (lot 8V): combat level and total level lines give no card.
		assertTrue(reader.read(clan, "Mossy Rock has reached combat level 104.").isEmpty());

		final BroadcastKindReader unloaded = new BroadcastKindReader(new CardTables(), Mockito.mock(ItemCards.class));
		final HoverCard plain = theCard(unloaded, clan, "Klar has reached Defence level 70.");
		assertEquals("before the tables load", "Defence level 70", plain.title());
		assertEquals("Defence level 70", plain.plainLine().substring(plain.start(), plain.end()));
		assertFalse("quiet", plain.marked());
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8j): the phrase is the card, and it is quiet in the chat.
	// Re-aimed by the lead, 2026-10-08 (lot 8V): combat level and total level lines give no card.
	@Test
	public void thePhraseIsTheCardAndIsQuiet() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));
		final String[][] linesAndPhrases = {
			{"Klar has reached Defence level 70.", "Defence level 70"},
			{"Marsel has reached 200,000,000 XP in Cooking.", "200,000,000 XP in Cooking"}};
		for (final String[] lineAndPhrase : linesAndPhrases)
		{
			final HoverCard card = theCard(reader, ChatMessageType.CLAN_MESSAGE, lineAndPhrase[0]);
			assertEquals("the phrase: " + lineAndPhrase[0], lineAndPhrase[1],
				card.plainLine().substring(card.start(), card.end()));
			assertFalse("quiet: " + lineAndPhrase[0], card.marked());
		}
	}

	@Test
	public void combatAndTotalLevelLinesGiveNoCard() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));
		final ChatMessageType clan = ChatMessageType.CLAN_MESSAGE;
		for (final String line : new String[] {
			"Mossy Rock has reached combat level 104.",
			"Mossy Rock has reached the highest possible combat level of 126.",
			"Iron Pete has reached a total level of 2225.",
			"Iron Pete has reached the highest possible total level of 2376."})
		{
			assertTrue("no card from: " + line, reader.read(clan, line).isEmpty());
		}

		final BroadcastKindReader unloaded = new BroadcastKindReader(new CardTables(), Mockito.mock(ItemCards.class));
		assertEquals("Defence level 70", theCard(unloaded, clan, "Klar has reached Defence level 70.").title());
		assertEquals("200,000,000 XP in Cooking",
			theCard(unloaded, clan, "Marsel has reached 200,000,000 XP in Cooking.").title());
	}

	@Test
	public void thePetPictureComesFromTheTableFirst() throws Exception
	{
		final String line = "Iron Pete has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount.";
		final ItemCards noIds = Mockito.mock(ItemCards.class);
		when(noIds.idOf(anyString())).thenReturn(-1);
		assertEquals("from the table, with the names index knowing nothing", 12921,
			theCard(reader(noIds), ChatMessageType.CLAN_MESSAGE, line).itemId());

		final ItemCards indexed = Mockito.mock(ItemCards.class);
		when(indexed.idOf("Pet snakeling")).thenReturn(12921);
		final BroadcastKindReader unloaded = new BroadcastKindReader(new CardTables(), indexed);
		assertEquals("before the tables load, from the names index", 12921,
			theCard(unloaded, ChatMessageType.CLAN_MESSAGE, line).itemId());
	}

	@Test
	public void thePetCardCarriesTheTablesRate() throws Exception
	{
		final String line = "Iron Pete has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount.";
		final DropRates rates;
		try (Reader in = new InputStreamReader(BroadcastKindReaderTest.class.getResourceAsStream(
			"/com/clanhub/drops-fixture.json"), StandardCharsets.UTF_8))
		{
			rates = DropRates.parse(in, new Gson());
		}
		final BroadcastKindReader withRates = reader(new ItemCards(Mockito.mock(ItemManager.class), rates));
		assertEquals("Pet from Zulrah, Rate 1/4000, Dropped at 2,678 killcount",
			rowOf(theCard(withRates, ChatMessageType.CLAN_MESSAGE, line)));

		assertEquals("Pet from Zulrah, Dropped at 2,678 killcount",
			rowOf(theCard(reader(Mockito.mock(ItemCards.class)), ChatMessageType.CLAN_MESSAGE, line)));
		assertEquals("a table that has not loaded adds no Rate row", "Pet from Zulrah, Dropped at 2,678 killcount",
			rowOf(theCard(reader(new ItemCards(Mockito.mock(ItemManager.class), new DropRates())),
				ChatMessageType.CLAN_MESSAGE, line)));
	}

	@Test
	public void theAchievementLineKeysWithoutItsPrefix() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));
		final String plain = "Mossy Rock has completed a Master combat task: Perfect Zulrah (5 points).";

		final HoverCard task = theCard(reader, ChatMessageType.CLAN_MESSAGE, "CA_ID:279|" + plain);
		assertTrue("the plain line starts with the player: " + task.plainLine(), task.plainLine().startsWith("Mossy Rock"));
		assertEquals(plain, task.plainLine());
		assertEquals("Perfect Zulrah", task.plainLine().substring(task.start(), task.end()));

		final CardStore store = new CardStore();
		store.put(task.plainLine(), Collections.singletonList(task));
		final CardStore.Match match = store.find(plain);
		assertTrue("the drawn line, which has no prefix, finds the card", match != null);
		assertEquals(0, match.offset);
		assertEquals(Collections.singletonList(task), match.cards);
	}

	@Test
	public void aRealTaskLineWithASpaceAfterThePrefixKeysLikeTheDrawnOne() throws Exception
	{
		final BroadcastKindReader reader = reader(Mockito.mock(ItemCards.class));
		final String drawn = "macemasterxx has completed an easy combat task: A Demon's Best Friend.";

		final HoverCard task = theCard(reader, ChatMessageType.CLAN_MESSAGE, "CA_ID:1234| " + drawn);
		assertEquals(drawn, task.plainLine());
		assertEquals("A Demon's Best Friend", task.plainLine().substring(task.start(), task.end()));

		final CardStore store = new CardStore();
		store.put(task.plainLine(), Collections.singletonList(task));
		final CardStore.Match match = store.find(drawn);
		assertTrue("the drawn line finds the card", match != null);
		assertEquals(0, match.offset);
		assertEquals(Collections.singletonList(task), match.cards);
	}

	@Test
	public void thePetCardCarriesItsItemAndTheOthersTheirPictures() throws Exception
	{
		final ItemCards itemCards = Mockito.mock(ItemCards.class);
		when(itemCards.idOf("Pet Snakeling")).thenReturn(12921);
		final BroadcastKindReader reader = reader(itemCards);

		final HoverCard pet = theCard(reader, ChatMessageType.CLAN_MESSAGE,
			"Iron Pete has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount.");
		assertEquals(12921, pet.itemId());
		assertEquals(-1, pet.spriteId());
		// Re-aimed by the lead, 2026-10-08 (lot 8U): the pet card keeps its item and asks for no wiki picture.
		assertFalse("the pet card shows its item, not the wiki's picture", pet.wikiPicture());

		final HoverCard best = theCard(reader, ChatMessageType.CLAN_MESSAGE,
			"Iron Pete has achieved a new Vorkath personal best: 2:28");
		assertEquals(ItemID.TRAIL_WATCH, best.itemId());
		assertEquals(-1, best.spriteId());

		final HoverCard kills = theCard(reader, ChatMessageType.GAMEMESSAGE, "Your King Black Dragon kill count is: 420.");
		assertEquals(168, kills.spriteId());
	}

	@Test
	public void ownKillCountRaidLootAndNothingElse() throws Exception
	{
		final ItemCards itemCards = Mockito.mock(ItemCards.class);
		final String raidLine = "Player received special loot from a raid: Twisted bow.";
		final int start = raidLine.indexOf("Twisted bow");
		final int end = start + "Twisted bow".length();
		final HoverCard bow = new ItemCard(20997, "Twisted bow", 1, 1_000_000_000L, 1, raidLine, start, end);
		when(itemCards.forName("Twisted bow", 1, raidLine, start, end)).thenReturn(bow);
		final BroadcastKindReader reader = reader(itemCards);

		final HoverCard kills = theCard(reader, ChatMessageType.GAMEMESSAGE, "Your King Black Dragon kill count is: 420.");
		assertEquals("King Black Dragon kill count", kills.title());
		assertEquals("Kills 420", rowOf(kills));
		// Re-aimed by the lead, 2026-10-08 (lot 5): a kill count card shows the game's combat icon, sprite 168.
		assertEquals(168, kills.spriteId());
		assertTrue(kills.wikiUrl().endsWith("/w/King_Black_Dragon"));
		assertEquals("King Black Dragon", kills.plainLine().substring(kills.start(), kills.end()));

		assertEquals(Collections.singletonList(bow), reader.read(ChatMessageType.CLAN_MESSAGE, raidLine));

		assertTrue(reader.read(ChatMessageType.CLAN_MESSAGE, "Klar: gz!!!").isEmpty());
		assertTrue(reader.read(ChatMessageType.CLAN_MESSAGE, "Klar has joined.").isEmpty());
		assertTrue(reader.read(ChatMessageType.PUBLICCHAT, QUEST_LINE).isEmpty());
		assertTrue(reader.read(ChatMessageType.CLAN_MESSAGE,
			"Klar received a drop: Dragon claws (121,453,000 coins).").isEmpty());
		assertTrue(reader.read(ChatMessageType.GAMEMESSAGE, QUEST_LINE).isEmpty());
		verify(itemCards, times(1)).forName(anyString(), anyInt(), anyString(), anyInt(), anyInt());
	}

	/** The fixture's tables with the bosses fixture read into them, as the plugin's own are. */
	private static CardTables fixtureTablesWithBosses() throws Exception
	{
		try (Reader bosses = new InputStreamReader(BroadcastKindReaderTest.class.getResourceAsStream(
			"/com/clanhub/bosses-fixture.json"), StandardCharsets.UTF_8))
		{
			return fixtureTables().readBosses(bosses, new Gson());
		}
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture replaces the pet.
	@Test
	public void theKillCountAndPersonalBestCardsCarryTheBoss() throws Exception
	{
		final BroadcastKindReader reader = new BroadcastKindReader(fixtureTablesWithBosses(), Mockito.mock(ItemCards.class));

		final HoverCard zulrah = theCard(reader, ChatMessageType.GAMEMESSAGE, "Your Zulrah kill count is: 343.");
		assertEquals("Zulrah kill count", zulrah.title());
		assertEquals("the count first, then the stats, and no Quest row for a boss with no quest",
			"Kills 343, Combat 725, Hitpoints 500, Max hit 41", rowOf(zulrah));
		assertEquals("no pet: the wiki's picture replaces it", -1, zulrah.itemId());
		assertTrue("the kill count card asks for the wiki's picture", zulrah.wikiPicture());
		assertEquals("with the combat sprite behind it", 168, zulrah.spriteId());
		assertEquals("Zulrah", zulrah.plainLine().substring(zulrah.start(), zulrah.end()));

		final HoverCard vorkath = theCard(reader, ChatMessageType.CLAN_MESSAGE,
			"Klar has achieved a new Vorkath personal best: 2:28");
		assertEquals("Vorkath personal best", vorkath.title());
		assertEquals("the time first, then the four facts", "Time 2:28, Combat 732, Hitpoints 750, Max hit 32, "
			+ "Quest Dragon Slayer II", rowOf(vorkath));
		assertEquals("the clue watch stands behind the wiki's picture", ItemID.TRAIL_WATCH, vorkath.itemId());
		assertTrue("the personal best card asks for the wiki's picture", vorkath.wikiPicture());
		assertEquals("Vorkath", vorkath.plainLine().substring(vorkath.start(), vorkath.end()));

		final HoverCard brutus = theCard(reader, ChatMessageType.CLAN_MESSAGE,
			"Klar has achieved a new Brutus personal best: 0:58");
		assertEquals("Time 0:58, Combat 70, Hitpoints 100, Max hit 10, Quest The Ides of Milk", rowOf(brutus));
		assertEquals("the clue watch behind the picture", ItemID.TRAIL_WATCH, brutus.itemId());
		assertTrue("and the wiki's picture asked for", brutus.wikiPicture());

		final HoverCard raid = theCard(reader, ChatMessageType.GAMEMESSAGE,
			"Your Chambers of Xeric chest count is: 12.");
		assertEquals("a raid with no stats has the count alone", "Count 12", rowOf(raid));
		assertEquals("a raid card has no pet either", -1, raid.itemId());
		assertTrue("and asks for the wiki's picture", raid.wikiPicture());

		final HoverCard unknownKills = theCard(reader, ChatMessageType.GAMEMESSAGE,
			"Your King Black Dragon kill count is: 420.");
		assertEquals("a boss the table lacks gives the card as today", "Kills 420", rowOf(unknownKills));
		assertEquals(-1, unknownKills.itemId());
		assertEquals(168, unknownKills.spriteId());
		assertTrue("a boss the table lacks still asks for the wiki's picture", unknownKills.wikiPicture());
		final HoverCard unknownBest = theCard(reader, ChatMessageType.CLAN_MESSAGE,
			"Iron Pete has achieved a new Inferno personal best: 1:02:15");
		assertEquals("Time 1:02:15", rowOf(unknownBest));
		assertEquals(ItemID.TRAIL_WATCH, unknownBest.itemId());
		assertTrue("and so does an unknown personal best", unknownBest.wikiPicture());
	}
}
