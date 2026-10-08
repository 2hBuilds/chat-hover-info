package com.clanhub;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

/**
 * The names of every item in the game, lowercase, with each name's item id: the item cards' way to an id the price list
 * lacks (a new item, a pet). The game has tens of thousands of items and reading them all at once would stall a frame,
 * so the plugin builds the index a slice at a time, {@link #buildSome} on each game tick, until every id has been read.
 * Every real item is kept, tradeable or not: not a noted copy, not a placeholder, not "null", not a name under five
 * characters. It is read and built on the client thread, the caller's, so it is not synchronized.
 */
final class ItemNameIndex
{
	/** The shortest name kept, so "bow" never matches and "bones" does. */
	private static final int MIN_LENGTH = 5;

	/** The item id by lowercase name; the first id seen for a name stays. */
	private final Map<String, Integer> ids = new HashMap<>();
	/** The next item id to read. */
	int next;
	/** The client's item count as {@link #buildSome} last read it; -1 before the first call. */
	private int total = -1;

	/**
	 * Reads the item ids from {@link #next} up to {@code count} of them, never past the client's item count, and keeps
	 * the items that pass: not a noted item, not a placeholder, a name that is not "null" and has five characters or
	 * more. The first id seen for a lowercase name stays.
	 *
	 * <p>Choice: the item count is read on every call, so a call after the index is complete reads nothing; the index is
	 * not complete before its first call, whatever the item count. An item that cannot be traded or alched stays in:
	 * a pet and the other untradeables have no price, but a card needs their ids, for their pictures and their names.
	 */
	void buildSome(Client client, ItemManager itemManager, int count)
	{
		total = client.getItemCount();
		final int stop = (int) Math.min((long) next + count, total);
		for (; next < stop; next++)
		{
			final ItemComposition composition = itemManager.getItemComposition(next);
			if (composition != null && keeps(composition))
			{
				add(next, composition.getName());
			}
		}
	}

	/** Stores a name with its id, unless the lowercase name is already there, in which case the first one stays. */
	void add(int id, String name)
	{
		final String lower = name.toLowerCase(Locale.ROOT);
		if (idOf(lower) == null)
		{
			ids.put(lower, id);
		}
	}

	/** Whether every item id up to the client's item count has been read. */
	boolean complete()
	{
		return total >= 0 && next >= total;
	}

	/** How many names the index holds. */
	int size()
	{
		return ids.size();
	}

	/** The item id for a lowercase name, or null when the index does not know it. */
	Integer idOf(String lowercaseName)
	{
		return ids.get(lowercaseName);
	}

	/** Whether an item is one the index keeps. */
	private static boolean keeps(ItemComposition composition)
	{
		final String name = composition.getName();
		return composition.getNote() == -1
			&& composition.getPlaceholderTemplateId() == -1
			&& name != null
			&& name.length() >= MIN_LENGTH
			&& !"null".equalsIgnoreCase(name);
	}
}
