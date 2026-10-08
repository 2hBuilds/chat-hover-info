package com.clanhub;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** {@link CardTables}: the table's shape read from a small fixture, an unloaded table, and the real bundled table. */
public class CardTablesTest
{
	private static final String FIXTURE = "/com/clanhub/cards-fixture.json";
	private static final String BUNDLED = "/com/clanhub/cards.json";
	private static final String BOSSES_FIXTURE = "/com/clanhub/bosses-fixture.json";
	private static final String BOSSES_BUNDLED = "/com/clanhub/bosses.json";

	/** A classpath resource opened as the plugin opens its table: read as UTF-8. */
	private static Reader open(String resource)
	{
		return new InputStreamReader(CardTablesTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8);
	}

	@Test
	public void parsesTheFixtureAndLooksUpIgnoringCaseAndTrailingDots() throws Exception
	{
		final CardTables tables;
		try (Reader in = open(FIXTURE))
		{
			tables = CardTables.parse(in, new Gson());
		}

		final CardTables.QuestInfo quest = tables.quest("dragon slayer ii.");
		assertNotNull(quest);
		assertEquals("Grandmaster", quest.difficulty);
		assertEquals("Very Long", quest.length);
		assertEquals(5, quest.points);
		assertEquals("Dragon_Slayer_II", quest.wiki);
		assertNotNull("a no-break space reads as a space and the spaces around are trimmed",
			tables.quest(" Dragon Slayer II ..."));

		final CardTables.TaskInfo task = tables.achievement("Perfect Zulrah");
		assertNotNull(task);
		assertEquals("Master", task.tier);
		assertEquals(5, task.points);
		assertEquals("Zulrah", task.monster);
		assertEquals("Perfection", task.type);
		assertTrue(task.description.startsWith("Kill Zulrah"));
		assertEquals("Perfect_Zulrah", task.wiki);

		final CardTables.PetInfo pet = tables.pet("pet snakeling");
		assertNotNull(pet);
		assertEquals("Pet Snakeling", pet.name);
		assertEquals("Zulrah", pet.source);
		assertEquals("Pet_Snakeling", pet.wiki);

		final CardTables.DiaryInfo diary = tables.diary("Ardougne");
		assertNotNull(diary);
		assertEquals("Ardougne_Diary", diary.wiki);

		assertNull(tables.quest("Nonsense"));
		assertTrue(tables.loaded());

		final CardTables none = new CardTables();
		assertFalse(none.loaded());
		assertNull(none.quest("Dragon Slayer II"));
		assertNull(none.achievement("Perfect Zulrah"));
		assertNull(none.pet("Pet Snakeling"));
		assertNull(none.diary("Ardougne"));
	}

	@Test
	public void theUnlocksAndThePetItemRead() throws Exception
	{
		final CardTables tables;
		try (Reader in = open(FIXTURE))
		{
			tables = CardTables.parse(in, new Gson());
		}
		final List<String> defence70 = Arrays.asList("Wear Barrows armour", "Wear Bandos armour",
			"Wear Armadyl armour", "Wield the dragonfire shield");
		assertEquals(defence70, tables.unlocks("Defence", 70));
		assertEquals("ignoring case", defence70, tables.unlocks("defence", 70));
		assertEquals(Collections.singletonList("Wear bronze armour"), tables.unlocks("Defence", 1));
		assertEquals("a level the skill has nothing at is empty, not null", Collections.<String>emptyList(),
			tables.unlocks("Defence", 72));
		assertNull("a skill the table lacks is null", tables.unlocks("Crafting", 88));
		assertNull("a table that has not loaded is null", new CardTables().unlocks("Defence", 70));
		assertEquals(12921, tables.pet("Pet snakeling").itemId);

		final CardTables bare;
		try (Reader in = new StringReader(
			"{\"pets\": [{\"name\": \"Aggy\", \"source\": \"Mad Angel\", \"wiki\": \"Aggy\"}]}"))
		{
			bare = CardTables.parse(in, new Gson());
		}
		assertEquals("no item reads as 0", 0, bare.pet("Aggy").itemId);
		assertNull("no unlocks member gives null", bare.unlocks("Defence", 1));
		assertTrue(bare.loaded());
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8U): the wiki's picture replaces the pet.
	@Test
	public void theBossTableGivesStatsAndQuest() throws Exception
	{
		final CardTables tables;
		try (Reader cards = open(FIXTURE); Reader bosses = open(BOSSES_FIXTURE))
		{
			tables = CardTables.parse(cards, new Gson()).readBosses(bosses, new Gson());
		}

		final CardTables.BossInfo duke = tables.boss("Duke Sucellus");
		assertNotNull(duke);
		assertEquals("Duke Sucellus", duke.name);
		assertEquals("450", duke.combat);
		assertEquals("440", duke.hitpoints);
		assertEquals("24", duke.maxHit);
		assertEquals("Desert Treasure II - The Fallen Empire", duke.quest);
		assertEquals("Duke_Sucellus", duke.wiki);

		final CardTables.BossInfo zulrah = tables.boss("zulrah");
		assertNotNull("ignoring case", zulrah);
		assertEquals("a boss with no quest has an empty one", "", zulrah.quest);

		final CardTables.BossInfo raid = tables.boss("Chambers of Xeric");
		assertNotNull(raid);
		assertEquals("", raid.combat);
		assertEquals("", raid.hitpoints);
		assertEquals("", raid.maxHit);
		assertNull("a name the table lacks is null", tables.boss("Nobody"));
		assertNull("a null name is null", tables.boss(null));
		assertNotNull("the other tables are still there", tables.quest("Dragon Slayer II"));

		final CardTables none = new CardTables();
		assertNull(none.boss("Duke Sucellus"));
		assertNull(none.boss("Zulrah"));
		assertNull(none.boss("Nobody"));

		final CardTables bare;
		try (Reader cards = new StringReader("{}");
			Reader bosses = new StringReader("{\"bosses\": [{\"name\": \"Aggy\"}, {\"wiki\": \"Nameless\"}]}"))
		{
			bare = CardTables.parse(cards, new Gson()).readBosses(bosses, new Gson());
		}
		assertEquals("a missing text reads as empty", "", bare.boss("Aggy").quest);
		assertNull("an entry with no name is skipped", bare.boss(""));
	}

	@Test
	public void theBundledBossTableIsSane() throws Exception
	{
		Assume.assumeNotNull(CardTables.class.getResource(BOSSES_BUNDLED));
		final CardTables tables;
		final JsonArray entries;
		try (Reader cards = open(FIXTURE); Reader bosses = open(BOSSES_BUNDLED))
		{
			tables = CardTables.parse(cards, new Gson()).readBosses(bosses, new Gson());
		}
		try (Reader bosses = open(BOSSES_BUNDLED))
		{
			entries = new Gson().fromJson(bosses, JsonObject.class).getAsJsonArray("bosses");
		}
		assertTrue("at least 50 bosses: " + entries.size(), entries.size() >= 50);
		int withQuestOrPet = 0;
		for (final JsonElement element : entries)
		{
			final JsonObject entry = element.getAsJsonObject();
			assertFalse("every entry has a name: " + entry, entry.get("name").getAsString().isEmpty());
			assertFalse("and a wiki page: " + entry, entry.get("wiki").getAsString().isEmpty());
			if (!entry.get("quest").getAsString().isEmpty() || entry.get("petItem").getAsInt() > 0)
			{
				withQuestOrPet++;
			}
		}
		assertTrue("at least half have a quest or a pet: " + withQuestOrPet + " of " + entries.size(),
			withQuestOrPet * 2 >= entries.size());
		for (final String name : new String[]{"Zulrah", "Vorkath", "Duke Sucellus"})
		{
			final CardTables.BossInfo boss = tables.boss(name);
			assertNotNull(name + " is in the table", boss);
			assertFalse(name + " has a combat level", boss.combat.isEmpty());
		}
	}

	@Test
	public void theBundledTableParsesWhenItIsThere() throws Exception
	{
		Assume.assumeNotNull(CardTables.class.getResource(BUNDLED));
		final CardTables tables;
		try (Reader in = open(BUNDLED))
		{
			tables = CardTables.parse(in, new Gson());
		}
		assertNotNull(tables.quest("Dragon Slayer II"));
		assertNotNull(tables.achievement("Perfect Zulrah"));
		assertNotNull(tables.pet("Pet Snakeling"));
		assertNotNull(tables.diary("Wilderness"));
	}
}
