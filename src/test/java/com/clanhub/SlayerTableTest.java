package com.clanhub;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;

/**
 * {@link CardTables}' slayer table (lot 14B): its shape read from a small fixture, an unloaded table, odd files, the
 * background load, and the real bundled {@code slayer.json}.
 */
public class SlayerTableTest
{
	private static final String FIXTURE = "/com/clanhub/slayer-fixture.json";
	private static final String BUNDLED = "/com/clanhub/slayer.json";

	/** A classpath resource opened as the plugin opens its table: read as UTF-8. */
	private static Reader open(String resource)
	{
		return new InputStreamReader(SlayerTableTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8);
	}

	/** A table that has loaded and holds nothing. */
	private static CardTables empty()
	{
		return CardTables.parse(new StringReader("{}"), new Gson());
	}

	@Test
	public void parsesTheFixtureAndLooksUpIgnoringCase() throws Exception
	{
		final CardTables tables;
		try (Reader in = open(FIXTURE))
		{
			tables = empty().readSlayer(in, new Gson());
		}

		final CardTables.SlayerInfo demons = tables.slayer("abyssal DEMONS");
		assertNotNull(demons);
		assertEquals("Abyssal demons", demons.name);
		assertEquals("Abyssal_demon", demons.wiki);
		assertEquals(85, demons.slayer);
		assertEquals("124", demons.combat);
		assertEquals(3, demons.drops.size());
		assertEquals("Abyssal dagger", demons.drops.get(0).item);
		assertEquals("1/32000", demons.drops.get(0).rate);
		assertEquals("Abyssal whip", demons.drops.get(2).item);

		final CardTables.SlayerInfo hounds = tables.slayer(" Hellhounds ");
		assertNotNull("the spaces around a name are trimmed", hounds);
		assertEquals("122-136", hounds.combat);
		assertEquals(1, hounds.slayer);

		final CardTables.SlayerInfo bare = tables.slayer("Banshees");
		assertNotNull(bare);
		assertEquals("a missing number reads as 0", 0, bare.slayer);
		assertEquals("a missing text reads as empty", "", bare.combat);
		assertTrue("and a missing array as none", bare.drops.isEmpty());

		assertNull("a task the table lacks is null", tables.slayer("Banana men"));
		assertNull("a null name is null", tables.slayer(null));
		assertTrue(tables.loaded());
		assertNull("a table with only cards has no tasks", empty().slayer("Hellhounds"));

		final CardTables none = new CardTables();
		assertFalse(none.loaded());
		assertNull("a table that has not loaded is null", none.slayer("Hellhounds"));
	}

	@Test
	public void anOddFileStopsNothingThatIsReadable() throws Exception
	{
		final String odd = "{\"tasks\": [{\"name\": \"Aggy\"}, {\"wiki\": \"Nameless\"}, \"junk\", {\"name\": \"Odd\","
			+ " \"slayer\": \"x\", \"drops\": [{\"item\": \"\", \"rate\": \"1/2\"}, {\"item\": \"Rune\", \"rate\": \"1/2\"},"
			+ " \"junk\", {\"item\": \"Bare\"}]}, {\"name\": \"Aggy\", \"slayer\": 9}]}";
		final CardTables tables = empty().readSlayer(new StringReader(odd), new Gson());
		assertEquals("a name met twice keeps its first entry", 0, tables.slayer("Aggy").slayer);
		assertNull("an entry with no name is skipped", tables.slayer(""));
		final CardTables.SlayerInfo oddity = tables.slayer("Odd");
		assertEquals("a text where a number goes reads as 0", 0, oddity.slayer);
		assertEquals("a drop with no item or that is no object is skipped, one with no rate reads as empty", 2,
			oddity.drops.size());
		assertEquals("Rune", oddity.drops.get(0).item);
		assertEquals("", oddity.drops.get(1).rate);

		final CardTables noArray = empty().readSlayer(new StringReader("{\"tasks\": 7}"), new Gson());
		assertNull("a member that is no array gives no tasks", noArray.slayer("Aggy"));
		assertNull("an empty file gives none and does not throw",
			empty().readSlayer(new StringReader(""), new Gson()).slayer("Aggy"));
	}

	@Test
	public void theSlayerTableIsReadIntoATableThatHasLoaded()
	{
		try
		{
			new CardTables().readSlayer(new StringReader("{\"tasks\": []}"), new Gson());
			fail("a table that has not loaded cannot take the slayer table");
		}
		catch (IllegalStateException expected)
		{
			// as the boss table's reading is
		}
	}

	@Test
	public void theBackgroundLoadReadsTheBundledSlayerTable()
	{
		Assume.assumeNotNull(CardTables.class.getResource(BUNDLED));
		final ScheduledExecutorService now = Mockito.mock(ScheduledExecutorService.class);
		Mockito.doAnswer(call ->
		{
			((Runnable) call.getArgument(0)).run();
			return null;
		}).when(now).execute(any(Runnable.class));

		final CardTables tables = new CardTables();
		tables.loadInBackground(now, new Gson());
		assertTrue(tables.loaded());
		assertNotNull("the slayer tasks load with the other tables", tables.slayer("Hellhounds"));
		assertNotNull("and so do the bosses", tables.boss("Zulrah"));
		assertNotNull("and the cards", tables.quest("Dragon Slayer II"));
	}

	@Test
	public void theBundledSlayerTableIsSane() throws Exception
	{
		Assume.assumeNotNull(CardTables.class.getResource(BUNDLED));
		final CardTables tables;
		final JsonArray entries;
		try (Reader in = open(BUNDLED))
		{
			tables = empty().readSlayer(in, new Gson());
		}
		try (Reader in = open(BUNDLED))
		{
			entries = new Gson().fromJson(in, JsonObject.class).getAsJsonArray("tasks");
		}
		assertTrue("at least 80 tasks: " + entries.size(), entries.size() >= 80);
		for (final JsonElement element : entries)
		{
			final JsonObject entry = element.getAsJsonObject();
			final String name = entry.get("name").getAsString();
			assertFalse("every entry has a name: " + entry, name.isEmpty());
			assertFalse(name + " has a wiki page", entry.get("wiki").getAsString().isEmpty());
			assertTrue(name + " has a Slayer level: " + entry, entry.get("slayer").getAsInt() >= 1);
			assertTrue(name + " says its combat level in a text: " + entry, entry.get("combat").getAsJsonPrimitive().isString());
			// Re-aimed by the lead, 2026-10-28: the user's layout pick: the rarest five.
			assertTrue(name + " keeps at most five drops: " + entry, entry.getAsJsonArray("drops").size() <= 5);
			final CardTables.SlayerInfo info = tables.slayer(name);
			assertNotNull(name + " is found by its name", info);
			assertEquals(name, info.name);
			for (final CardTables.SlayerInfo.Drop drop : info.drops)
			{
				assertFalse(name + " names its drops: " + entry, drop.item.isEmpty());
				assertTrue(name + " gives a rate as 1/N: " + drop.rate, drop.rate.matches("1/\\d+(\\.\\d)?"));
			}
		}
		assertNotNull("Hellhounds is there", tables.slayer("Hellhounds"));
		assertEquals("Hellhound", tables.slayer("Hellhounds").wiki);
		assertEquals(85, tables.slayer("Abyssal demons").slayer);
		assertEquals(91, tables.slayer("Cerberus").slayer);
		for (final String name : new String[] {"Abyssal demons", "Cerberus", "Gargoyles", "Nechryael", "Zulrah", "Kurask"})
		{
			assertNotNull(name + " is there", tables.slayer(name));
			assertFalse(name + " has a combat level", tables.slayer(name).combat.isEmpty());
		}
	}
}
