package com.clanhub;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The reader of the answer RuneLite's own Slayer plugin writes into a line after it has arrived. A member types
 * {@code !task}, and the Slayer plugin of everyone who runs it replaces the typed line with the answer through
 * {@code MessageNode.setRuneLiteFormatMessage}: {@code Slayer Task: <task>[ (<place>)]: <killed>/<total> killed}, or {@code
 * Slayer Task: <task>[ (<place>)]: <n> left} when the first amount is unknown, the task as the plugin's {@code Task} enum
 * names it ("Hellhounds", "Abyssal demons", "Cerberus") and the place only when the task has one. Like the Chat Commands
 * answers ({@link ChatCommandReader}) the rewrite lands after every {@code ChatMessage} subscriber has run, so it is read
 * where it can be seen, on the drawn widget, by the overlay when the mouse reaches the line. Pure: the widget's plain text
 * in, the one card out; no RuneLite state.
 *
 * <p>Choice: the whole drawn text is the key. The text has to be the answer from its label (or its sender and a colon,
 * when the chatbox puts them in the same widget) to its last word, so a line that goes on after "killed" or "left", a typed
 * "my slayer task: hellhounds lol" and a task the bundled table does not know match nothing. The card is quiet, as the
 * kill count's is: there is no event for the rewrite, so the chat cannot colour the words, and the hover shows them. Its
 * words are the task's name.
 *
 * <p>Choice: the card is laid out as the user picked: the progress is a bar under the title ({@link HoverCard.Progress}:
 * "10/155" and "16,820 XP left" beside it, or, for a line that gives no total ("145 left"), no bar and the same words as
 * text); the facts are the ones the table knows, each only when known, in this order: the place when the line names one,
 * the Slayer level when it is above 1 (a task that asks for none says nothing), the combat level and the Slayer XP a kill
 * gives ("116"); and the drops are the card's own grey "Drops:" line, the way an item card shows its sources, the table's
 * rarest five with their rates. A task the boss table also knows (Cerberus, Zulrah, ... , found by the task's name, without
 * a leading "The ", or by its wiki page) takes the boss's combat level, hitpoints, max hit and quest in place of the slayer
 * table's combat level, and its XP follows them.
 *
 * <p>Choice: the XP left is told only when the table's XP is one figure: the kills left times it, the kills left being the
 * total less the killed ("10/155 killed" leaves 145) or the amount the line says is left, never below none. A range of XP
 * ("116-130", when the task's monsters give different amounts) and no XP at all tell no XP left, and no XP tells no XP at
 * all. The card's picture is the wiki's own of the task's page, the Slayer icon in the box until it comes; its paragraph is
 * the wiki's first, asked for as for any card without notes.
 */
final class SlayerTaskReader
{
	/** The XP left, with its thousands commas and no more than one decimal (a half XP is real). */
	private static final DecimalFormat XP_LEFT = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.ROOT));
	/** A whole number as the game writes it or with thousands commas. */
	private static final String NUMBER = "(?:\\d{1,3}(?:,\\d{3})+|\\d+)";
	/**
	 * "Slayer Task: Hellhounds: 10/155 killed", "Slayer Task: Abyssal demons (Catacombs of Kourend): 3/120 killed" and
	 * "Slayer Task: Hellhounds: 145 left", after the sender and a colon if any.
	 */
	private static final Pattern TASK = Pattern.compile(
		"(?:.*: )?Slayer Task: (?<task>[^:()]+?)(?: \\((?<place>[^()]+)\\))?: "
			+ "(?:(?<done>" + NUMBER + ")/(?<total>" + NUMBER + ") killed|(?<left>" + NUMBER + ") left)");

	private SlayerTaskReader()
	{
	}

	/**
	 * The one card a widget's plain text ({@link ChatText#plain}) answers, or an empty list. The card's words are the task's
	 * name; its facts are described in the class comment. A null text, a task that is only spaces, a task the table lacks
	 * and a table that has not loaded give none.
	 */
	static List<HoverCard> read(String plain, CardTables tables)
	{
		if (plain == null)
		{
			return Collections.emptyList();
		}
		final Matcher found = TASK.matcher(plain);
		if (!found.matches() || found.group("task").trim().isEmpty())
		{
			return Collections.emptyList();
		}
		final String task = found.group("task").trim();
		final CardTables.SlayerInfo info = tables.slayer(task);
		if (info == null)
		{
			return Collections.emptyList();
		}
		final int start = found.start("task") + found.group("task").indexOf(task);
		final String page = info.wiki.isEmpty() ? InfoCard.pageFor(task) : info.wiki;
		return Collections.<HoverCard>singletonList(new InfoCard(task, facts(found, info, bossOf(task, info, tables)),
			drops(info), progress(found, info), Collections.<String>emptyList(), page, plain, start, start + task.length(),
			SkillSprites.spriteOf("Slayer"), -1, false, true));
	}

	/** The progress the card paints as a bar: the words, how far it is filled and the XP left; see the class comment. */
	private static HoverCard.Progress progress(Matcher found, CardTables.SlayerInfo info)
	{
		final String xpLeft = oneFigure(info.xp) == null ? ""
			: XP_LEFT.format(oneFigure(info.xp).multiply(BigDecimal.valueOf(killsLeft(found))));
		if (found.group("left") != null)
		{
			return new HoverCard.Progress(found.group("left") + " left", -1, xpLeft);
		}
		final double total = Double.parseDouble(found.group("total").replace(",", ""));
		final double done = Double.parseDouble(found.group("done").replace(",", ""));
		return new HoverCard.Progress(found.group("done") + "/" + found.group("total"),
			total > 0 ? Math.min(1.0, done / total) : -1, xpLeft);
	}

	/** The card's drops line: each drop of the table as a source (its name) and its rate. */
	private static List<HoverCard.Pair> drops(CardTables.SlayerInfo info)
	{
		final List<HoverCard.Pair> drops = new ArrayList<>();
		for (final CardTables.SlayerInfo.Drop drop : info.drops)
		{
			drops.add(new HoverCard.Pair(drop.item, drop.rate, ""));
		}
		return drops;
	}

	/** The card's facts, in order; see the class comment. */
	private static List<HoverCard.Pair> facts(Matcher found, CardTables.SlayerInfo info, CardTables.BossInfo boss)
	{
		final List<HoverCard.Pair> facts = new ArrayList<>();
		if (found.group("place") != null && !found.group("place").trim().isEmpty())
		{
			facts.add(new HoverCard.Pair("Place", found.group("place").trim(), ""));
		}
		if (info.slayer > 1)
		{
			facts.add(new HoverCard.Pair("Slayer", String.valueOf(info.slayer), ""));
		}
		if (boss != null)
		{
			facts.addAll(BossCards.facts(boss));
		}
		else if (!info.combat.isEmpty())
		{
			facts.add(new HoverCard.Pair("Combat", info.combat, ""));
		}
		if (!info.xp.isEmpty())
		{
			facts.add(new HoverCard.Pair("Slayer XP", info.xp, ""));
		}
		return facts;
	}

	/** The kills the line leaves: the amount it says is left, else the total less the killed, never below none. */
	private static long killsLeft(Matcher found)
	{
		if (found.group("left") != null)
		{
			return Long.parseLong(found.group("left").replace(",", ""));
		}
		final long total = Long.parseLong(found.group("total").replace(",", ""));
		final long done = Long.parseLong(found.group("done").replace(",", ""));
		return Math.max(0L, total - done);
	}

	/** The XP as a number when the table's text is one figure ("116", "62.5"), null for a range ("116-130") or anything else. */
	private static BigDecimal oneFigure(String xp)
	{
		return xp.matches("\\d+(\\.\\d+)?") ? new BigDecimal(xp) : null;
	}

	/**
	 * The boss table's entry for a boss task, or null: the task's name, else the name without a leading "The " ("The Abyssal
	 * Sire"), else the name of the task's wiki page ("Cave_Kraken" read as "Cave Kraken").
	 */
	private static CardTables.BossInfo bossOf(String task, CardTables.SlayerInfo info, CardTables tables)
	{
		CardTables.BossInfo boss = tables.boss(task);
		if (boss == null && task.regionMatches(true, 0, "The ", 0, 4))
		{
			boss = tables.boss(task.substring(4));
		}
		return boss != null ? boss : tables.boss(info.wiki.replace('_', ' '));
	}
}
