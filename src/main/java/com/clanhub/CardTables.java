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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The quests, combat achievement tasks, pets, achievement diaries and skill unlocks of the Old School RuneScape Wiki,
 * from the table the plugin bundles ({@code /com/clanhub/cards.json}, built from the wiki and credited in its own
 * header), the bosses' combat level, hitpoints, max hit and quest from the second table it bundles
 * ({@code /com/clanhub/bosses.json}, credited the same way) and the slayer tasks' Slayer level, combat level and notable
 * drops from the third ({@code /com/clanhub/slayer.json}): for a name, what the wiki says about it, and for a skill's
 * level, what the wiki lists as new at it. The tables are read once, off the client thread, a second or two after
 * start-up; until then, and for ever if the file is missing or unreadable, every lookup answers null and the cards of
 * these broadcasts are the plain ones. Parsing is pure and is the only part that needs the tables' shape.
 *
 * <p>Choice: the load task runs once; a plugin restart runs it again.
 *
 * <p>Choice: the boss table is a file of its own, and the load treats it apart from the cards: a boss file that is
 * missing or unreadable leaves {@link #boss} answering null and the rest of the table loaded, and when the card table
 * did not load nothing of the bosses is read. The slayer table is treated the same way, apart from both.
 */
final class CardTables
{
	private static final Logger log = LoggerFactory.getLogger(CardTables.class);

	/** Where the bundled table lives on the classpath. */
	private static final String RESOURCE = "/com/clanhub/cards.json";

	/** Where the bundled boss table lives on the classpath. */
	private static final String BOSSES = "/com/clanhub/bosses.json";

	/** Where the bundled slayer table lives on the classpath. */
	private static final String SLAYER = "/com/clanhub/slayer.json";

	/** What the wiki says of a quest. */
	static final class QuestInfo
	{
		/** The difficulty, such as "Grandmaster". */
		final String difficulty;
		/** The length, such as "Very Long". */
		final String length;
		/** The quest points it gives. */
		final int points;
		/** The wiki page, such as "Dragon_Slayer_II". */
		final String wiki;

		QuestInfo(String difficulty, String length, int points, String wiki)
		{
			this.difficulty = difficulty;
			this.length = length;
			this.points = points;
			this.wiki = wiki;
		}
	}

	/** What the wiki says of a combat achievement task. */
	static final class TaskInfo
	{
		/** The tier, such as "Master". */
		final String tier;
		/** The points it gives. */
		final int points;
		/** The monster it is about, such as "Zulrah". */
		final String monster;
		/** The kind of task, such as "Perfection". */
		final String type;
		/** The task's description, as the wiki writes it. */
		final String description;
		/** The wiki page, such as "Perfect_Zulrah". */
		final String wiki;

		TaskInfo(String tier, int points, String monster, String type, String description, String wiki)
		{
			this.tier = tier;
			this.points = points;
			this.monster = monster;
			this.type = type;
			this.description = description;
			this.wiki = wiki;
		}
	}

	/** What the wiki says of a pet. */
	static final class PetInfo
	{
		/** The pet's name as the wiki spells it, such as "Pet Snakeling". */
		final String name;
		/** What it comes from, such as "Zulrah". */
		final String source;
		/** The wiki page, such as "Pet_Snakeling". */
		final String wiki;
		/** The id of the pet's item, whose sprite is the card's picture; 0 when the table has none. */
		final int itemId;

		PetInfo(String name, String source, String wiki, int itemId)
		{
			this.name = name;
			this.source = source;
			this.wiki = wiki;
			this.itemId = itemId;
		}
	}

	/** What the wiki says of an achievement diary's area. */
	static final class DiaryInfo
	{
		/** The area as the wiki spells it, such as "Lumbridge & Draynor". */
		final String name;
		/** The wiki page, such as "Ardougne_Diary". */
		final String wiki;

		DiaryInfo(String name, String wiki)
		{
			this.name = name;
			this.wiki = wiki;
		}
	}

	/** What the wiki says of a boss, or of a raid, with the quest it goes with. */
	static final class BossInfo
	{
		/** The boss's name as the pets' table and the game spell it, such as "Duke Sucellus". */
		final String name;
		/** The wiki page, such as "Duke_Sucellus". */
		final String wiki;
		/** The combat level as the wiki writes it, such as "758"; empty when the wiki has none. */
		final String combat;
		/** The hitpoints as the wiki writes them, such as "485"; empty when the wiki has none. */
		final String hitpoints;
		/** The max hit as the wiki writes it, such as "56 (Melee)"; empty when the wiki has none. */
		final String maxHit;
		/** The quest the boss belongs to, such as "Desert Treasure II - The Fallen Empire"; empty when it has none. */
		final String quest;

		BossInfo(String name, String wiki, String combat, String hitpoints, String maxHit, String quest)
		{
			this.name = name;
			this.wiki = wiki;
			this.combat = combat;
			this.hitpoints = hitpoints;
			this.maxHit = maxHit;
			this.quest = quest;
		}
	}

	/** What the wiki says of a slayer task, as RuneLite's Slayer plugin names it in the answer to {@code !task}. */
	static final class SlayerInfo
	{
		/** One notable drop of the task's monster. */
		static final class Drop
		{
			/** The item's name, such as "Abyssal whip". */
			final String item;
			/** The rate as "1/N", such as "1/512"; empty when the table has none. */
			final String rate;

			Drop(String item, String rate)
			{
				this.item = item;
				this.rate = rate;
			}
		}

		/** The task's name as RuneLite spells it, such as "Abyssal demons". */
		final String name;
		/** The wiki page of the monster the task is named for, such as "Abyssal_demon". */
		final String wiki;
		/** The Slayer level the task asks for; 1 when it asks for none, 0 when the table does not say. */
		final int slayer;
		/** The monster's combat level or range as the table writes it, such as "122-136"; empty when it has none. */
		final String combat;
		/** The Slayer XP one kill gives as the table writes it, such as "116", "62.5" or "116-130"; empty when it has none. */
		final String xp;
		/** The task's notable drops, at most three in the bundled table, the rarest first; may be empty. */
		final List<Drop> drops;

		SlayerInfo(String name, String wiki, int slayer, String combat, String xp, List<Drop> drops)
		{
			this.name = name;
			this.wiki = wiki;
			this.slayer = slayer;
			this.combat = combat;
			this.xp = xp;
			this.drops = Collections.unmodifiableList(new ArrayList<>(drops));
		}
	}

	/** A table read whole: each kind by its name's key. */
	private static final class Table
	{
		private final Map<String, QuestInfo> quests = new HashMap<>();
		private final Map<String, TaskInfo> tasks = new HashMap<>();
		private final Map<String, PetInfo> pets = new HashMap<>();
		private final Map<String, DiaryInfo> diaries = new HashMap<>();
		private final Map<String, BossInfo> bosses = new HashMap<>();
		private final Map<String, SlayerInfo> slayers = new HashMap<>();
		/** Each skill's key, then each level with unlocks, to what the wiki lists as new at it. */
		private final Map<String, Map<Integer, List<String>>> unlocks = new HashMap<>();
	}

	/** The table once loaded; null until the load has published it. */
	private volatile Table table;

	/** A table that has not loaded: every lookup answers null and {@link #loaded()} is false. */
	CardTables()
	{
	}

	private CardTables(Table table)
	{
		this.table = table;
	}

	/**
	 * Reads the table's shape: {@code {"quests": [...], "achievements": [...], "pets": [...], "diaries": [...],
	 * "unlocks": {...}}}, each entry of an array an object with a {@code name} and the members of its info class, and
	 * {@code unlocks} an object of the skills, each an object of its levels (a decimal text) to an array of texts, what
	 * the wiki lists as new at that level. A missing array or object gives none of that kind, and the table still counts
	 * as loaded.
	 *
	 * <p>Choice: an entry with no {@code name} is skipped, a missing text member reads as an empty text and a missing
	 * number as 0, and a name met twice keeps its first entry; in {@code unlocks} a skill that is not an object, a level
	 * that is not a whole number or whose value is not an array, and an element of that array that is not a text, are
	 * each skipped; nothing in the file's shape that is merely odd stops the rest of the table being read.
	 *
	 * @throws com.google.gson.JsonParseException when the text is not JSON
	 */
	static CardTables parse(Reader json, Gson gson)
	{
		final Table read = new Table();
		final JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root != null)
		{
			for (final JsonObject entry : objectsOf(root, "quests"))
			{
				read.quests.putIfAbsent(key(text(entry, "name")), new QuestInfo(text(entry, "difficulty"),
					text(entry, "length"), number(entry, "points"), text(entry, "wiki")));
			}
			for (final JsonObject entry : objectsOf(root, "achievements"))
			{
				read.tasks.putIfAbsent(key(text(entry, "name")), new TaskInfo(text(entry, "tier"),
					number(entry, "points"), text(entry, "monster"), text(entry, "type"), text(entry, "description"),
					text(entry, "wiki")));
			}
			for (final JsonObject entry : objectsOf(root, "pets"))
			{
				read.pets.putIfAbsent(key(text(entry, "name")),
					new PetInfo(text(entry, "name"), text(entry, "source"), text(entry, "wiki"), number(entry, "item")));
			}
			for (final JsonObject entry : objectsOf(root, "diaries"))
			{
				read.diaries.putIfAbsent(key(text(entry, "name")),
					new DiaryInfo(text(entry, "name"), text(entry, "wiki")));
			}
			readUnlocks(root, read.unlocks);
		}
		return new CardTables(read);
	}

	/**
	 * Reads the boss table's shape, {@code {"bosses": [...]}}, each entry an object with a {@code name} and the members of
	 * {@link BossInfo}, into this table, and returns this table. A missing array gives no bosses.
	 *
	 * <p>Choice: an entry with no {@code name} is skipped, a missing text member reads as an empty text, a member the
	 * class has no field for (the bundled file still carries each boss's {@code petItem}) is ignored, and a name met twice
	 * keeps its first entry, as {@link #parse} reads the other kinds.
	 *
	 * @throws IllegalStateException when this table has not loaded: the bosses are added to a table {@link #parse} made
	 * @throws com.google.gson.JsonParseException when the text is not JSON
	 */
	CardTables readBosses(Reader json, Gson gson)
	{
		if (table == null)
		{
			throw new IllegalStateException("the boss table is read into a table that has loaded");
		}
		final JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root != null)
		{
			for (final JsonObject entry : objectsOf(root, "bosses"))
			{
				table.bosses.putIfAbsent(key(text(entry, "name")), new BossInfo(text(entry, "name"), text(entry, "wiki"),
					text(entry, "combat"), text(entry, "hitpoints"), text(entry, "maxHit"), text(entry, "quest")));
			}
		}
		return this;
	}

	/**
	 * Reads the slayer table's shape, {@code {"tasks": [...]}}, each entry an object with a {@code name}, a {@code wiki}
	 * page, a {@code slayer} level, a {@code combat} text, an {@code xp} text and a {@code drops} array of objects with an
	 * {@code item} and a {@code rate}, into this table, and returns this table. A missing array gives no tasks.
	 *
	 * <p>Choice: as {@link #readBosses} reads its kind: an entry with no {@code name} is skipped, a missing text reads as an
	 * empty text and a missing number as 0, and a name met twice keeps its first entry; a drop that is no object or has no
	 * item is skipped and one with no rate reads as an empty rate.
	 *
	 * @throws IllegalStateException when this table has not loaded: the tasks are added to a table {@link #parse} made
	 * @throws com.google.gson.JsonParseException when the text is not JSON
	 */
	CardTables readSlayer(Reader json, Gson gson)
	{
		if (table == null)
		{
			throw new IllegalStateException("the slayer table is read into a table that has loaded");
		}
		final JsonObject root = gson.fromJson(json, JsonObject.class);
		if (root != null)
		{
			for (final JsonObject entry : objectsOf(root, "tasks"))
			{
				table.slayers.putIfAbsent(key(text(entry, "name")), new SlayerInfo(text(entry, "name"), text(entry, "wiki"),
					number(entry, "slayer"), text(entry, "combat"), text(entry, "xp"), dropsOf(entry)));
			}
		}
		return this;
	}

	/** The drops of a slayer task's entry: each object of its {@code drops} array that names an item, in the file's order. */
	private static List<SlayerInfo.Drop> dropsOf(JsonObject entry)
	{
		final List<SlayerInfo.Drop> drops = new ArrayList<>();
		final JsonElement array = entry.get("drops");
		if (array == null || !array.isJsonArray())
		{
			return drops;
		}
		for (final JsonElement element : array.getAsJsonArray())
		{
			if (element.isJsonObject() && !text(element.getAsJsonObject(), "item").isEmpty())
			{
				drops.add(new SlayerInfo.Drop(text(element.getAsJsonObject(), "item"), text(element.getAsJsonObject(), "rate")));
			}
		}
		return drops;
	}

	/**
	 * Reads the {@code unlocks} member of the root into {@code into}: the skill's key, then each level that has at least
	 * one text to its list of them, in the file's order. A skill met twice keeps its first entry.
	 */
	private static void readUnlocks(JsonObject root, Map<String, Map<Integer, List<String>>> into)
	{
		final JsonElement skills = root.get("unlocks");
		if (skills == null || !skills.isJsonObject())
		{
			return;
		}
		for (final Map.Entry<String, JsonElement> skill : skills.getAsJsonObject().entrySet())
		{
			if (!skill.getValue().isJsonObject() || key(skill.getKey()).isEmpty())
			{
				continue;
			}
			final Map<Integer, List<String>> levels = new HashMap<>();
			for (final Map.Entry<String, JsonElement> level : skill.getValue().getAsJsonObject().entrySet())
			{
				final Integer number = wholeNumber(level.getKey());
				if (number == null || !level.getValue().isJsonArray())
				{
					continue;
				}
				final List<String> unlocks = new ArrayList<>();
				for (final JsonElement unlock : level.getValue().getAsJsonArray())
				{
					if (unlock.isJsonPrimitive() && unlock.getAsJsonPrimitive().isString())
					{
						unlocks.add(unlock.getAsString());
					}
				}
				levels.putIfAbsent(number, Collections.unmodifiableList(unlocks));
			}
			into.putIfAbsent(key(skill.getKey()), levels);
		}
	}

	/** The whole number a text spells in decimal, or null when it spells none. */
	private static Integer wholeNumber(String text)
	{
		try
		{
			return Integer.valueOf(text.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/** The objects of a member's array that have a name, in the file's order; none when the member is not an array. */
	private static List<JsonObject> objectsOf(JsonObject root, String member)
	{
		final List<JsonObject> objects = new ArrayList<>();
		final JsonElement array = root.get(member);
		if (array == null || !array.isJsonArray())
		{
			return objects;
		}
		for (final JsonElement entry : array.getAsJsonArray())
		{
			if (entry.isJsonObject() && !text(entry.getAsJsonObject(), "name").isEmpty())
			{
				objects.add(entry.getAsJsonObject());
			}
		}
		return objects;
	}

	/** The text of a member, or an empty text when it is missing or is not a string. */
	private static String text(JsonObject object, String member)
	{
		final JsonElement value = object.get(member);
		return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
	}

	/** The whole number of a member, or 0 when it is missing or is not a number. */
	private static int number(JsonObject object, String member)
	{
		final JsonElement value = object.get(member);
		return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : 0;
	}

	/**
	 * The key a name is kept and looked up by: a no-break space read as a space, the surrounding spaces and the trailing
	 * dots taken off ("Another Slice of H.A.M." and "Another Slice of H.A.M" are one name), in lower case.
	 */
	private static String key(String name)
	{
		final String spaced = name.replace((char) 160, ' ').trim();
		int end = spaced.length();
		while (end > 0 && (spaced.charAt(end - 1) == '.' || spaced.charAt(end - 1) == ' '))
		{
			end--;
		}
		return spaced.substring(0, end).toLowerCase(Locale.ROOT);
	}

	/**
	 * The quest called {@code name}, ignoring case, trailing dots and surrounding spaces, or null when the table does
	 * not know it or has not loaded.
	 *
	 * <p>Choice: the lookups ask {@link #loaded()} first; the table is published once and never taken back, so what
	 * follows may read it.
	 */
	QuestInfo quest(String name)
	{
		return loaded() && name != null ? table.quests.get(key(name)) : null;
	}

	/** The combat achievement task called {@code name}, matched as {@link #quest} matches; null when unknown. */
	TaskInfo achievement(String name)
	{
		return loaded() && name != null ? table.tasks.get(key(name)) : null;
	}

	/** The pet called {@code name}, matched as {@link #quest} matches; null when unknown. */
	PetInfo pet(String name)
	{
		return loaded() && name != null ? table.pets.get(key(name)) : null;
	}

	/**
	 * The boss, monster or raid called {@code name}, matched as {@link #quest} matches; null when the boss table does not
	 * know it or has not loaded.
	 */
	BossInfo boss(String name)
	{
		return loaded() && name != null ? table.bosses.get(key(name)) : null;
	}

	/**
	 * The slayer task called {@code task}, as RuneLite's Slayer plugin writes it in its answer to {@code !task}, matched as
	 * {@link #quest} matches; null when the slayer table does not know it or has not loaded.
	 */
	SlayerInfo slayer(String task)
	{
		return loaded() && task != null ? table.slayers.get(key(task)) : null;
	}

	/** The achievement diary of the area called {@code area}, matched as {@link #quest} matches; null when unknown. */
	DiaryInfo diary(String area)
	{
		return loaded() && area != null ? table.diaries.get(key(area)) : null;
	}

	/**
	 * What the wiki lists as new at {@code level} of the skill called {@code skill}, ignoring case, in the table's order
	 * and at most a handful: an empty list when the table has the skill but nothing at that level, and null when the
	 * table has not loaded or has no object for the skill (so the caller can tell "nothing new" from "do not know").
	 */
	List<String> unlocks(String skill, int level)
	{
		if (!loaded() || skill == null)
		{
			return null;
		}
		final Map<Integer, List<String>> levels = table.unlocks.get(key(skill));
		if (levels == null)
		{
			return null;
		}
		final List<String> found = levels.get(level);
		return found == null ? Collections.<String>emptyList() : found;
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

	/** The load task: three classpath reads, three parses, one publish. */
	private void load(Gson gson)
	{
		try (InputStream in = CardTables.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("The card table {} is not in the plugin; the cards show what the chat line says and no more", RESOURCE);
				return;
			}
			final CardTables read = parse(new InputStreamReader(in, StandardCharsets.UTF_8), gson);
			readBossTable(read, gson);
			readSlayerTable(read, gson);
			table = read.table;
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("The card table {} could not be read ({}); the cards show what the chat line says and no more", RESOURCE,
				e.toString());
		}
	}

	/**
	 * Reads the bundled boss table into {@code into}, which has not been published. A missing resource or a table that
	 * does not parse logs one warning naming the cause and leaves the bosses empty; nothing is thrown out of it.
	 */
	private static void readBossTable(CardTables into, Gson gson)
	{
		try (InputStream in = CardTables.class.getResourceAsStream(BOSSES))
		{
			if (in == null)
			{
				log.warn("The boss table {} is not in the plugin; the boss cards show no facts", BOSSES);
				return;
			}
			into.readBosses(new InputStreamReader(in, StandardCharsets.UTF_8), gson);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("The boss table {} could not be read ({}); the boss cards show no facts", BOSSES, e.toString());
		}
	}

	/**
	 * Reads the bundled slayer table into {@code into}, which has not been published. A missing resource or a table that
	 * does not parse logs one warning naming the cause and leaves the tasks empty; nothing is thrown out of it.
	 */
	private static void readSlayerTable(CardTables into, Gson gson)
	{
		try (InputStream in = CardTables.class.getResourceAsStream(SLAYER))
		{
			if (in == null)
			{
				log.warn("The slayer table {} is not in the plugin; the slayer task cards are not given", SLAYER);
				return;
			}
			into.readSlayer(new InputStreamReader(in, StandardCharsets.UTF_8), gson);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("The slayer table {} could not be read ({}); the slayer task cards are not given", SLAYER, e.toString());
		}
	}
}
