package com.clanhub;

import com.google.gson.Gson;
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
