package com.clanhub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.runelite.api.ChatMessageType;

/**
 * The reader of the broadcast lines of the clan, guest clan, group ironman and friends chat channels: a drop, a stacked
 * drop, a clue item or a new collection log item. {@link BroadcastLine} finds the item's name in the line and
 * {@link ItemCards} makes the card, so a line gives the item's card, or none when it is not a broadcast or names no item
 * the item manager knows. A drop line that says where the drop came from ("from Duke Sucellus") gives a second card, the
 * boss's, while the "Boss cards" setting is on: a quiet card with the wiki's own picture of the boss (the combat icon
 * until it comes), its combat level, hitpoints, max hit and quest from the {@link CardTables}, and the wiki's first
 * paragraph.
 *
 * <p>Choice: the boss card is given even when the item gives none (an unknown name), because the line still says where
 * the drop came from; the list is the item card, then the boss card, the order their words stand in the line.
 *
 * <p>Choice: a boss the table lacks, or a table that has not loaded, gives the card as before: no facts, and the wiki's
 * picture, which every name is asked for, the combat icon behind it.
 */
final class BroadcastItemReader implements LineReader
{
	private final ItemCards itemCards;
	private final BooleanSupplier bossCards;
	private final CardTables tables;

	/**
	 * @param itemCards makes the item card
	 * @param bossCards whether the "Boss cards" setting is on, asked for each line
	 * @param tables the bosses' facts, which may not have loaded yet
	 */
	BroadcastItemReader(ItemCards itemCards, BooleanSupplier bossCards, CardTables tables)
	{
		this.itemCards = itemCards;
		this.bossCards = bossCards;
		this.tables = tables;
	}

	@Override
	public List<HoverCard> read(ChatMessageType type, String message)
	{
		final Optional<BroadcastLine.ItemMention> found = BroadcastLine.parse(type, message);
		if (!found.isPresent())
		{
			return Collections.emptyList();
		}
		final BroadcastLine.ItemMention mention = found.get();
		final List<HoverCard> cards = new ArrayList<>(2);
		final HoverCard card = itemCards.forName(mention.name, mention.quantity, mention.text, mention.start,
			mention.end);
		if (card != null)
		{
			cards.add(card);
		}
		if (mention.source != null && bossCards.getAsBoolean())
		{
			cards.add(bossCard(mention.source, mention.text, mention.sourceStart, mention.sourceEnd, tables));
		}
		return cards;
	}

	/**
	 * The quiet card for the boss, monster, raid or chest a drop line names: its name for the title, the boss's facts for
	 * its row ({@link BossCards#facts}, none for a boss {@code tables} lacks), no notes, its wiki page, the wiki's own
	 * picture of that page and the combat icon behind it until the picture comes. Quiet, so its words keep the line's own
	 * colour and show themselves only under the mouse; the item stays the line's one blue word. The {@link OwnLineReader} makes the player's own drop
	 * lines' boss card the same way.
	 *
	 * @param tables the bosses' table, which may not have loaded yet
	 */
	static HoverCard bossCard(String source, String line, int start, int end, CardTables tables)
	{
		final CardTables.BossInfo info = tables.boss(source);
		return new InfoCard(source, BossCards.facts(info), Collections.<String>emptyList(), BossCards.page(info, source),
			line, start, end, SkillSprites.spriteOf("Combat"), -1, false, true);
	}
}
