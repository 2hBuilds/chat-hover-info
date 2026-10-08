package com.clanhub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.ChatMessageType;

/**
 * The sample lines the developer-mode command {@code ::hoverinfo demo} posts into the player's own chatbox, so the cards
 * can be hovered at once without waiting for a clan mate's drop. Plain data: each lot adds a list of its own here and
 * one line in {@link #lines()}.
 */
final class DemoLines
{
	/** One line to post: the chat type, the speaking player's name and the text. */
	static final class Line
	{
		/** The chat type the line is posted as. */
		final ChatMessageType type;
		/** The speaking player for a typed chat line; empty for a broadcast. */
		final String name;
		/** The line's text. */
		final String text;

		Line(ChatMessageType type, String name, String text)
		{
			this.type = type;
			this.name = name;
			this.text = text;
		}
	}

	/** The four item broadcasts: a drop, a stacked drop, a clue item and a collection log item. */
	static final List<Line> ITEMS = Collections.unmodifiableList(Arrays.asList(
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Klar received a drop: Dragon claws (121,453,000 coins)."),
		new Line(ChatMessageType.FRIENDSCHATNOTIFICATION, "",
			"Iron Pete received a drop: 10 x Black knife (460 coins)."),
		new Line(ChatMessageType.CLAN_GUEST_MESSAGE, "", "Klar received a clue item: Ranger boots (25,296,193 coins)."),
		new Line(ChatMessageType.CLAN_GIM_MESSAGE, "",
			"Mossy Rock received a new collection log item: Abyssal whip (1/484).")));

	/** Two of the player's own game lines: a valuable drop and a new collection log item. */
	static final List<Line> OWN = Collections.unmodifiableList(Arrays.asList(
		new Line(ChatMessageType.GAMEMESSAGE, "", "Valuable drop: Dragon claws (121,453,000 coins)"),
		new Line(ChatMessageType.GAMEMESSAGE, "", "New item added to your collection log: Abyssal whip")));

	/** Nine clan broadcasts that are not about an item: a quest, a combat task, a pet, three levels, XP, a diary and a best. */
	static final List<Line> KINDS = Collections.unmodifiableList(Arrays.asList(
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Klar has completed a quest: Dragon Slayer II"),
		new Line(ChatMessageType.CLAN_MESSAGE, "",
			"CA_ID:279|Mossy Rock has completed a Master combat task: Perfect Zulrah (5 points)."),
		new Line(ChatMessageType.CLAN_MESSAGE, "",
			"Iron Pete has a funny feeling like he's being followed: Pet snakeling at 2,678 killcount."),
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Klar has reached Defence level 70."),
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Mossy Rock has reached combat level 104."),
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Iron Pete has reached a total level of 2225."),
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Klar has reached 200,000,000 XP in Cooking."),
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Mossy Rock has completed the Elite Ardougne diary."),
		new Line(ChatMessageType.CLAN_MESSAGE, "", "Iron Pete has achieved a new Vorkath personal best: 2:28")));

	private DemoLines()
	{
	}

	/** Every sample line, section by section, in the order they are posted. */
	static List<Line> lines()
	{
		final List<Line> all = new ArrayList<>(ITEMS);
		all.addAll(OWN);
		all.addAll(KINDS);
		return all;
	}
}
