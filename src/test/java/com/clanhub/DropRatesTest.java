package com.clanhub;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link DropRates}: the table's shape read from a small fixture, an empty table, and the real bundled table. */
public class DropRatesTest
{
	private static final String FIXTURE = "/com/clanhub/drops-fixture.json";
	private static final String BUNDLED = "/com/clanhub/drops.json";

	/** A classpath resource opened as the plugin opens its table: read as UTF-8. */
	private static Reader open(String resource)
	{
		return new InputStreamReader(DropRatesTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8);
	}

	@Test
	public void parsesTheFixtureAndAnswersByName() throws Exception
	{
		final DropRates rates;
		try (Reader in = open(FIXTURE))
		{
			rates = DropRates.parse(in, new Gson());
		}

		final List<DropRates.DropSource> whip = rates.lookup("Abyssal whip");
		assertEquals(2, whip.size());
		assertEquals("Abyssal demon", whip.get(0).from);
		assertEquals("1/512", whip.get(0).rate);
		assertEquals("Abyssal_demon", whip.get(0).wiki);
		assertEquals("Greater abyssal demon", whip.get(1).from);

		final List<DropRates.DropSource> lower = rates.lookup("abyssal whip");
		assertEquals(2, lower.size());
		assertEquals("Abyssal demon", lower.get(0).from);
		assertEquals("1/512", lower.get(0).rate);
		assertEquals("Abyssal_demon", lower.get(0).wiki);

		assertTrue(rates.lookup("Nonsense").isEmpty());
		assertTrue(rates.loaded());
	}

	@Test
	public void anEmptyTableAnswersNothingWithoutFailing()
	{
		final DropRates none = new DropRates();
		assertFalse(none.loaded());
		assertTrue(none.lookup("Dragon claws").isEmpty());

		final DropRates empty = DropRates.parse(new StringReader("{\"items\": {}}"), new Gson());
		assertTrue(empty.loaded());
		assertTrue(empty.lookup("Dragon claws").isEmpty());
	}

	/**
	 * Choice: the items are counted from the file's own list of names (read with a Gson of the test's own), each name
	 * asked of the parsed table, so the count is of items the table answers for and no member exists for the test alone.
	 */
	@Test
	public void theBundledTableParsesWhenItIsThere() throws Exception
	{
		Assume.assumeNotNull(DropRates.class.getResource(BUNDLED));
		final Gson gson = new Gson();
		final DropRates rates;
		try (Reader in = open(BUNDLED))
		{
			rates = DropRates.parse(in, gson);
		}
		final JsonObject root;
		try (Reader in = open(BUNDLED))
		{
			root = gson.fromJson(in, JsonObject.class);
		}

		int items = 0;
		for (final Map.Entry<String, JsonElement> item : root.getAsJsonObject("items").entrySet())
		{
			final List<DropRates.DropSource> sources = rates.lookup(item.getKey());
			if (!sources.isEmpty())
			{
				items++;
			}
			for (final DropRates.DropSource source : sources)
			{
				assertFalse("a source without a name for " + item.getKey(), source.from.isEmpty());
				assertFalse("a source without a rate for " + item.getKey(), source.rate.isEmpty());
			}
		}
		assertTrue("at least 1,000 items, found " + items, items >= 1000);
		assertFalse("the whip has a source", rates.lookup("Abyssal whip").isEmpty());
	}
}
