package com.clanhub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.gameval.ItemID;

/**
 * The reader of the broadcast lines of the clan, guest clan, group ironman and friends chat channels that are about
 * something other than an item the {@link BroadcastItemReader} knows: a raid's special loot (an item card, through
 * {@link ItemCards}), a quest, a combat achievement task, a pet, a level, a total level, an XP milestone, a diary tier
 * and a personal best; and, from the player's own game lines, a boss kill count. The sentences are the research's
 * ({@code docs/data/broadcasts.md}, section 3) and are matched as they stand once the tags and a combat achievement
 * line's {@code CA_ID:<n>|} are taken off ({@link ChatText#plain}), the text the offsets index; the first kind that
 * matches wins. What the wiki says of a quest, task, pet or diary comes from the {@link CardTables}; for a name the table
 * does not know, or before it has loaded, the card is the plain one the line alone gives. On a level, total level or XP
 * line the phrase is the card ("Smithing level 99", "total level of 2225", "200,000,000 XP in Cooking"): one card, over
 * the phrase, and a quiet one, whose words the chat does not colour; a level line of a skill the table knows gives the
 * card of what is new at that level, else the plain level card. A pet's picture is its item, whose id
 * the table gives, else {@link ItemCards#idOf} finds by the name; a personal best's is the clue watch; a kill count's is
 * the combat icon. The chat text is never rewritten.
 */
final class BroadcastKindReader implements LineReader
{
	/** "Player received special loot from a raid: Twisted bow." */
	private static final Pattern RAID = Pattern.compile(
		"^(?<player>.+?) received special loot from a raid: (?<item>.+?)\\.?$");
	/** "Klar has completed a quest: Dragon Slayer II" */
	private static final Pattern QUEST = Pattern.compile("^(?<player>.+?) has completed a quest: (?<quest>.+)$");
	/** "CA_ID:279|Klar has completed a Master combat task: Perfect Zulrah (5 points)." */
	private static final Pattern CA_TASK = Pattern.compile(
		"^(?<player>.+?) has completed an? (?<tier>Easy|Medium|Hard|Elite|Master|Grandmaster) combat task: (?<task>.+?)"
			+ "(?: \\((?<points>\\d+) points?\\))?\\.?$", Pattern.CASE_INSENSITIVE);
	/** "Clipper has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount." */
	private static final Pattern PET_FOLLOWED = Pattern.compile(
		"^(?<player>.+?) has a funny feeling like (?:he|she|they)(?:'s|'re| would have) (?:being|been) followed: "
			+ "(?<pet>.+?)(?: at (?<count>[\\d,]+) (?<unit>.+?))?\\.?$");
	/** "Dash Inc feels something weird sneaking into his backpack: Heron at 8,426,283 XP." */
	private static final Pattern PET_BACKPACK = Pattern.compile(
		"^(?<player>.+?) feels something weird sneaking into (?:his|her|their) backpack: "
			+ "(?<pet>.+?)(?: at (?<count>[\\d,]+) (?<unit>.+?))?\\.?$");
	/** "ClanMate feels like she's acquired something special: Abyssal orphan" */
	private static final Pattern PET_SPECIAL = Pattern.compile(
		"^(?<player>.+?) feels like (?:he|she|they)(?:'s|'ve| has| have) acquired something special: (?<pet>.+?)\\.?$");
	/** "I Vision I has reached a total level of 2225." */
	private static final Pattern TOTAL = Pattern.compile(
		"^(?<player>.+?) has reached (?:a|the highest possible) total level of (?<level>[\\d,]+)[.!]?$");
	/** "Th3TRiPPyOn3 has reached Defence level 70."; the skill may be "combat". */
	private static final Pattern LEVEL = Pattern.compile(
		"^(?<player>.+?) has reached (?:the highest possible )?(?<skill>[A-Za-z]+) level(?: of)? (?<level>\\d+)[.!]?$");
	/** "Marsel has reached 200,000,000 XP in Cooking." */
	private static final Pattern XP = Pattern.compile(
		"^(?<player>.+?) has reached (?<xp>[\\d,]+) XP in (?<skill>[A-Za-z]+)[.!]?$");
	/** "Player has completed the Elite Ardougne diary." */
	private static final Pattern DIARY = Pattern.compile(
		"^(?<player>.+?) has completed the (?<tier>Easy|Medium|Hard|Elite) (?<area>.+?) diary\\.?$");
	/** "Player has achieved a new Vorkath personal best: 2:28" */
	private static final Pattern PB = Pattern.compile(
		"^(?<player>.+?) has achieved a new (?<activity>.+?) personal best: "
			+ "(?<time>\\d+(?::\\d+)+(?:\\.\\d+)?)\\.?$");
	/** The player's own "Your King Black Dragon kill count is: 420." */
	private static final Pattern OWN_KC = Pattern.compile(
		"^Your (?<boss>.+?) (?<kind>kill|chest|completion|harvest|success|opened|lap|Total Ticket) ?count is: ?"
			+ "(?<count>[\\d,]+)\\.?$", Pattern.CASE_INSENSITIVE);

	/** An {@code @colour@} macro the game puts in a name. */
	private static final Pattern MACRO = Pattern.compile("@[^@\\s]+@");

	/** The combat achievement tiers in order; a tier's index is added to the first sword's sprite. */
	private static final List<String> TIERS = Arrays.asList("Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster");

	private final CardTables tables;
	private final ItemCards itemCards;

	BroadcastKindReader(CardTables tables, ItemCards itemCards)
	{
		this.tables = tables;
		this.itemCards = itemCards;
	}

	/**
	 * The card for the thing a line names, or none. The four broadcast channels' lines are tried against the kinds in
	 * the order raid loot, quest, combat task, the three pet shapes, total level, level, XP, diary, personal best, the
	 * first match giving the one card; a {@code GAMEMESSAGE} line is tried against the kill count only; any other type, or a line that is none of these,
	 * gives an empty list.
	 */
	@Override
	public List<HoverCard> read(ChatMessageType type, String message)
	{
		if (message == null)
		{
			return Collections.emptyList();
		}
		if (type == ChatMessageType.GAMEMESSAGE)
		{
			final String plain = ChatText.plain(message);
			final Matcher kills = OWN_KC.matcher(plain);
			return kills.matches() ? one(killCount(kills, plain)) : Collections.<HoverCard>emptyList();
		}
		if (!isBroadcast(type))
		{
			return Collections.emptyList();
		}
		final String plain = ChatText.plain(message);
		Matcher found = RAID.matcher(plain);
		if (found.matches())
		{
			return one(itemCards.forName(found.group("item"), 1, plain, found.start("item"), found.end("item")));
		}
		found = QUEST.matcher(plain);
		if (found.matches())
		{
			return one(quest(found, plain));
		}
		found = CA_TASK.matcher(plain);
		if (found.matches())
		{
			return one(task(found, plain));
		}
		found = PET_FOLLOWED.matcher(plain);
		if (found.matches())
		{
			return one(pet(found, plain, true));
		}
		found = PET_BACKPACK.matcher(plain);
		if (found.matches())
		{
			return one(pet(found, plain, true));
		}
		found = PET_SPECIAL.matcher(plain);
		if (found.matches())
		{
			return one(pet(found, plain, false));
		}
		found = TOTAL.matcher(plain);
		if (found.matches())
		{
			return one(totalLevel(found, plain));
		}
		found = LEVEL.matcher(plain);
		if (found.matches())
		{
			return one(level(found, plain));
		}
		found = XP.matcher(plain);
		if (found.matches())
		{
			return one(experience(found, plain));
		}
		found = DIARY.matcher(plain);
		if (found.matches())
		{
			return one(diary(found, plain));
		}
		found = PB.matcher(plain);
		if (found.matches())
		{
			return one(personalBest(found, plain));
		}
		return Collections.emptyList();
	}

	/** The quest card: difficulty, length and points from the table, or "Quest completed" alone for a quest it lacks. */
	private HoverCard quest(Matcher found, String plain)
	{
		final String name = clean(found.group("quest"));
		final CardTables.QuestInfo info = tables.quest(name);
		final List<HoverCard.Pair> row = info == null
			? Collections.singletonList(pair("Quest completed", ""))
			: Arrays.asList(pair("Difficulty", info.difficulty), pair("Length", info.length),
				pair("Points", String.valueOf(info.points)));
		return new InfoCard(name, row, Collections.<String>emptyList(), page(info == null ? null : info.wiki, name),
			plain, found.start("quest"), found.end("quest"), SkillSprites.QUEST);
	}

	/**
	 * The combat achievement task card: tier, points and boss from the table with the description under them, whole (the
	 * painter wraps it to the card), or only the tier for a task it lacks; the picture is the tier's sword.
	 */
	private HoverCard task(Matcher found, String plain)
	{
		final String name = clean(found.group("task"));
		final String tier = canonicalTier(found.group("tier"));
		final CardTables.TaskInfo info = tables.achievement(name);
		final List<HoverCard.Pair> row;
		final List<String> notes;
		if (info == null)
		{
			row = Collections.singletonList(pair("Tier", tier));
			notes = Collections.emptyList();
		}
		else
		{
			row = new ArrayList<>(Arrays.asList(pair("Tier", info.tier), pair("Points", String.valueOf(info.points))));
			if (!info.monster.isEmpty())
			{
				row.add(pair("Boss", info.monster));
			}
			final String description = info.description.trim();
			notes = description.isEmpty() ? Collections.<String>emptyList() : Collections.singletonList(description);
		}
		return new InfoCard(name, row, notes, page(info == null ? null : info.wiki, name), plain, found.start("task"),
			found.end("task"), SkillSprites.CA_TIER_SWORDS + TIERS.indexOf(tier));
	}

	/**
	 * The pet card: the pet's name as the table spells it, where it comes from, its "Rate" when the drop table holds one
	 * ({@link ItemCards#rateOf}), and (when {@code counted} and the line has one) "Dropped at", the member's own count
	 * when the pet dropped, as the line gives it with its unit word; for a pet the table lacks, the name from the line and
	 * "Pet" alone. The picture is the pet's own item: the id the table gives when it has one, else the id {@link
	 * ItemCards#idOf} gives for the name as the table spells it, else the line's; -1 (no picture) when no item has that
	 * name.
	 *
	 * <p>Choice: the table is asked first, so the picture shows from the first second after login, before the names
	 * index {@code idOf} reads has been built.
	 */
	private HoverCard pet(Matcher found, String plain, boolean counted)
	{
		final String name = clean(found.group("pet"));
		final CardTables.PetInfo info = tables.pet(name);
		final List<HoverCard.Pair> row = new ArrayList<>();
		if (info == null)
		{
			row.add(pair("Pet", ""));
		}
		else
		{
			row.add(pair("Pet from", info.source));
			final String rate = itemCards.rateOf(info.name);
			if (rate != null && !rate.isEmpty())
			{
				row.add(pair("Rate", rate));
			}
			if (counted && found.group("count") != null)
			{
				row.add(pair("Dropped at", found.group("count") + " " + found.group("unit")));
			}
		}
		final String spelled = info == null ? name : info.name;
		final int itemId = info != null && info.itemId > 0 ? info.itemId : itemCards.idOf(spelled);
		return new InfoCard(spelled, row, Collections.<String>emptyList(), page(info == null ? null : info.wiki, name),
			plain, found.start("pet"), found.end("pet"), -1, itemId);
	}

	/**
	 * The total level card, "Total level 2225" with the skills "All". Its words are the phrase "total level of 2225", and
	 * it is quiet.
	 *
	 * <p>Choice: the phrase is the card, from the start of "total level" to the end of the digits; the chat does not
	 * colour it, the hover shows it.
	 */
	private HoverCard totalLevel(Matcher found, String plain)
	{
		final int start = plain.lastIndexOf("total level", found.start("level"));
		return new InfoCard("Total level " + found.group("level"), Collections.singletonList(pair("Skills", "All")),
			Collections.<String>emptyList(), "Total_level", plain, start, found.end("level"),
			SkillSprites.spriteOf("Total"), -1, false);
	}

	/**
	 * The level card, over the phrase "Defence level 70": for a skill the table knows, {@link #unlocks} ("Defence 70",
	 * what is new at that level); for any other skill, or before the table has loaded, the plain card "Defence level 70"
	 * with the skill's row and page. The combat level, which the pattern also reads, is told by its skill word and always
	 * gets the plain card, with the combat level page. The card is quiet.
	 */
	private HoverCard level(Matcher found, String plain)
	{
		final String skill = capitalised(found.group("skill"));
		final boolean combat = "Combat".equals(skill);
		final List<String> unlocks = combat ? null : tables.unlocks(skill, levelOf(found.group("level")));
		if (unlocks != null)
		{
			return unlocks(skill, found, plain, unlocks);
		}
		return new InfoCard(skill + " level " + found.group("level"), Collections.singletonList(pair("Skill", skill)),
			Collections.<String>emptyList(), combat ? "Combat_level" : InfoCard.pageFor(skill), plain,
			found.start("skill"), found.end("level"), SkillSprites.spriteOf(skill), -1, false);
	}

	/**
	 * The card of what is new at a level, "Defence 70": one row, "New" with "4 unlocks", "1 unlock" or "Nothing new", its
	 * unlocks under it as sentences, and the skill's {@code Level_up_table} page. Its words are the phrase "Defence level
	 * 70", and it is quiet.
	 *
	 * <p>Choice: every unlock is kept whole, in the table's order, and a full stop is added to one that does not end in
	 * {@code .}, {@code !} or {@code ?}, so the notes joined into the card's paragraph read as sentences; the painter
	 * wraps them under the card's header, three lines at most, and cuts with "..." where they run on.
	 */
	private HoverCard unlocks(String skill, Matcher found, String plain, List<String> unlocks)
	{
		final int count = unlocks.size();
		final List<String> notes = new ArrayList<>();
		for (final String unlock : unlocks)
		{
			final String sentence = sentence(unlock);
			if (!sentence.isEmpty())
			{
				notes.add(sentence);
			}
		}
		final String summary = count == 0 ? "Nothing new" : count + (count == 1 ? " unlock" : " unlocks");
		return new InfoCard(skill + " " + found.group("level"),
			Collections.singletonList(pair("New", summary)), notes, skill + "/Level_up_table", plain,
			found.start("skill"), found.end("level"), SkillSprites.spriteOf(skill), -1, false);
	}

	/** The unlock as a sentence: trimmed, with a full stop added when it does not end in a full stop, "!" or "?". */
	private static String sentence(String unlock)
	{
		final String text = unlock.trim();
		if (text.isEmpty())
		{
			return text;
		}
		final char last = text.charAt(text.length() - 1);
		return last == '.' || last == '!' || last == '?' ? text : text + ".";
	}

	/** The level a group of digits spells, or 0 when it is too large to be one (the table then has nothing for it). */
	private static int levelOf(String digits)
	{
		try
		{
			return Integer.parseInt(digits);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	/** The XP milestone card, "200,000,000 XP in Cooking". Its words are that whole phrase, and it is quiet. */
	private HoverCard experience(Matcher found, String plain)
	{
		final String skill = capitalised(found.group("skill"));
		return new InfoCard(found.group("xp") + " XP in " + skill, Collections.singletonList(pair("Skill", skill)),
			Collections.<String>emptyList(), InfoCard.pageFor(skill), plain, found.start("xp"), found.end("skill"),
			SkillSprites.spriteOf(skill), -1, false);
	}

	/** The diary card, "Ardougne diary, Elite", with the area as the table spells it when it knows it. */
	private HoverCard diary(Matcher found, String plain)
	{
		final String area = clean(found.group("area"));
		final CardTables.DiaryInfo info = tables.diary(area);
		final String tier = found.group("tier");
		return new InfoCard((info == null ? area : info.name) + " diary, " + tier,
			Collections.singletonList(pair("Tier", tier)), Collections.<String>emptyList(),
			page(info == null ? null : info.wiki, area + " Diary"), plain, found.start("area"), found.end("area"),
			SkillSprites.DIARY);
	}

	/** The personal best card, "Vorkath personal best" with the time; the picture is the clue watch's item. */
	private HoverCard personalBest(Matcher found, String plain)
	{
		final String activity = clean(found.group("activity"));
		return new InfoCard(activity + " personal best", Collections.singletonList(pair("Time", found.group("time"))),
			Collections.<String>emptyList(), InfoCard.pageFor(activity), plain, found.start("activity"),
			found.end("activity"), -1, ItemID.TRAIL_WATCH);
	}

	/**
	 * The kill count card, "King Black Dragon kill count" with the count; the picture is the game's combat icon.
	 *
	 * <p>Choice: the pair's label is "Kills" for a kill count and "Count" for the other kinds (chest, completion and the
	 * rest), and the title carries the kind's word in lower case.
	 */
	private HoverCard killCount(Matcher found, String plain)
	{
		final String boss = clean(found.group("boss"));
		final String kind = found.group("kind").toLowerCase(Locale.ROOT);
		return new InfoCard(boss + " " + kind + " count",
			Collections.singletonList(pair("kill".equals(kind) ? "Kills" : "Count", found.group("count"))),
			Collections.<String>emptyList(), InfoCard.pageFor(boss), plain, found.start("boss"), found.end("boss"),
			SkillSprites.spriteOf("Combat"));
	}

	/** A pair of a label and its value with no note. */
	private static HoverCard.Pair pair(String label, String value)
	{
		return new HoverCard.Pair(label, value, "");
	}

	/** The wiki page of a thing: the table's page when it has one, else the page built from the name. */
	private static String page(String tablePage, String name)
	{
		return tablePage == null || tablePage.isEmpty() ? InfoCard.pageFor(name) : tablePage;
	}

	/** The one card in a list, or an empty list for none. */
	private static List<HoverCard> one(HoverCard card)
	{
		return card == null ? Collections.<HoverCard>emptyList() : Collections.singletonList(card);
	}

	/**
	 * A name taken from a group, cleaned: a no-break space read as a space, {@code @...@} macros removed, the trailing
	 * dots and the spaces around it taken off.
	 */
	private static String clean(String name)
	{
		final String text = MACRO.matcher(name.replace((char) 160, ' ')).replaceAll("").trim();
		int end = text.length();
		while (end > 0 && (text.charAt(end - 1) == '.' || text.charAt(end - 1) == ' '))
		{
			end--;
		}
		return text.substring(0, end);
	}

	/** A skill's word with its first letter in capitals and the rest in lower case ("combat" is "Combat"). */
	private static String capitalised(String word)
	{
		return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.ROOT);
	}

	/** The tier's word as {@link #TIERS} spells it, whatever case the line wrote it in. */
	private static String canonicalTier(String word)
	{
		for (final String tier : TIERS)
		{
			if (tier.equalsIgnoreCase(word))
			{
				return tier;
			}
		}
		return word;
	}

	/** The four channels whose broadcast lines carry these cards. */
	private static boolean isBroadcast(ChatMessageType type)
	{
		return type == ChatMessageType.CLAN_MESSAGE
			|| type == ChatMessageType.CLAN_GUEST_MESSAGE
			|| type == ChatMessageType.CLAN_GIM_MESSAGE
			|| type == ChatMessageType.FRIENDSCHATNOTIFICATION;
	}
}
