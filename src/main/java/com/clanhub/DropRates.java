package com.clanhub;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The drop rates of the Old School RuneScape Wiki, from the table the plugin bundles
 * ({@code /com/clanhub/drops.json}, built from the wiki's drop data and credited in its own header): for an item's name,
 * the sources it drops from, the most likely first. The table is read once, off the client thread, a second or two
 * after start-up; until then, and for ever if the file is missing or unreadable, every lookup answers an empty list and
 * the item card simply shows no Drop row. Parsing is pure and is the only part that needs the table's shape.
 *
 * <p>Choice: the load task runs once; a plugin restart runs it again.
 */
final class DropRates
{
	private static final Logger log = LoggerFactory.getLogger(DropRates.class);

	/** Where the bundled table lives on the classpath. */
	private static final String RESOURCE = "/com/clanhub/drops.json";

	/** One place an item drops from, with how often. */
	static final class DropSource
	{
		/** The source's name, such as "Abyssal demon" or "Chambers of Xeric". */
		final String from;
		/** The rate as the wiki writes it, such as "1/512", "1/171.6" or "Rare". */
		final String rate;
		/** The wiki page of the source, such as "Abyssal_demon". */
		final String wiki;

		DropSource(String from, String rate, String wiki)
		{
			this.from = from;
			this.rate = rate;
			this.wiki = wiki;
		}
	}

	/** A table read whole: the sources by the item's exact name, and by its name ignoring case. */
	private static final class Table
	{
		private final Map<String, List<DropSource>> exact = new HashMap<>();
		private final Map<String, List<DropSource>> ignoringCase = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	}

	/** The table once loaded; null until the load has published it. */
	private volatile Table table;

	/** A table that has not loaded: every lookup answers nothing and {@link #loaded()} is false. */
	DropRates()
	{
	}

	private DropRates(Table table)
	{
		this.table = table;
	}

	/**
	 * Reads the table's shape: {@code {"items": {"<item name>": [{"from": "...", "rate": "...", "wiki": "..."}]}}}.
	 * A missing or empty {@code items} gives an empty table that counts as loaded.
	 *
	 * <p>Choice: a source with no {@code from} or no {@code rate} is skipped, an item left with no source is left out,
	 * and a missing {@code wiki} reads as an empty text; nothing in the file's shape that is merely odd stops the rest
	 * of the table being read.
	 *
	 * @throws com.google.gson.JsonParseException when the text is not JSON
	 */
	static DropRates parse(Reader json, Gson gson)
	{
		final Table read = new Table();
		final JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root != null && root.has("items") && root.get("items").isJsonObject())
		{
			for (final Map.Entry<String, JsonElement> item : root.getAsJsonObject("items").entrySet())
			{
				final List<DropSource> sources = sourcesOf(item.getValue());
				if (!sources.isEmpty())
				{
					final List<DropSource> kept = Collections.unmodifiableList(sources);
					read.exact.put(item.getKey(), kept);
					read.ignoringCase.putIfAbsent(item.getKey(), kept);
				}
			}
		}
		return new DropRates(read);
	}

	/** The sources one item's array holds, the file's order kept. */
	private static List<DropSource> sourcesOf(JsonElement array)
	{
		final List<DropSource> sources = new ArrayList<>();
		if (!array.isJsonArray())
		{
			return sources;
		}
		for (final JsonElement entry : array.getAsJsonArray())
		{
			if (!entry.isJsonObject())
			{
				continue;
			}
			final JsonObject source = entry.getAsJsonObject();
			final String from = text(source, "from");
			final String rate = text(source, "rate");
			if (!from.isEmpty() && !rate.isEmpty())
			{
				sources.add(new DropSource(from, rate, text(source, "wiki")));
			}
		}
		return sources;
	}

	/** The text of a member, or an empty text when it is missing or is not a string. */
	private static String text(JsonObject object, String member)
	{
		final JsonElement value = object.get(member);
		return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
	}

	/**
	 * The sources the item drops from, the most likely first: those of the exact name, else of the name ignoring case,
	 * else an empty list. Also an empty list while the table has not loaded.
	 */
	List<DropSource> lookup(String itemName)
	{
		final Table loaded = table;
		if (loaded == null || itemName == null)
		{
			return Collections.emptyList();
		}
		final List<DropSource> exact = loaded.exact.get(itemName);
		if (exact != null)
		{
			return exact;
		}
		final List<DropSource> similar = loaded.ignoringCase.get(itemName);
		return similar != null ? similar : Collections.<DropSource>emptyList();
	}

	/** Whether the table has been read and published. */
	boolean loaded()
	{
		return table != null;
	}

	/**
	 * Reads the bundled table on the executor and publishes it. A missing resource or a table that does not parse leaves
	 * this empty and logs one warning naming the cause; nothing is thrown out of the task.
	 *
	 * <p>Choice: after a failed load {@link #loaded()} stays false, since nothing was published.
	 */
	void loadInBackground(ScheduledExecutorService executor, Gson gson)
	{
		executor.execute(() -> load(gson));
	}

	/** The load task: one classpath read, one parse, one publish. */
	private void load(Gson gson)
	{
		try (InputStream in = DropRates.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("The drop rate table {} is not in the plugin; the cards show no drop rates", RESOURCE);
				return;
			}
			table = parse(new InputStreamReader(in, StandardCharsets.UTF_8), gson).table;
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("The drop rate table {} could not be read ({}); the cards show no drop rates", RESOURCE, e.toString());
		}
	}
}
