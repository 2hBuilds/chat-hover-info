package com.clanhub;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.runelite.api.ChatMessageType;

/**
 * The reader of the broadcast lines of the clan, guest clan, group ironman and friends chat channels: a drop, a stacked
 * drop, a clue item or a new collection log item. {@link BroadcastLine} finds the item's name in the line and
 * {@link ItemCards} makes the card, so a line gives one card, or none when it is not a broadcast or names no item the
 * item manager knows.
 */
final class BroadcastItemReader implements LineReader
{
	private final ItemCards itemCards;

	BroadcastItemReader(ItemCards itemCards)
	{
		this.itemCards = itemCards;
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
		final HoverCard card = itemCards.forName(mention.name, mention.quantity, mention.text, mention.start,
			mention.end);
		return card == null ? Collections.<HoverCard>emptyList() : Collections.singletonList(card);
	}
}
