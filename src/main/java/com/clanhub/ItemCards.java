package com.clanhub;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

/**
 * The one place an item card is made: it turns an item's name, with where the name sits in a chat line, into an
 * {@link ItemCard} with the item's id, its market price and its high alchemy value from the item manager, and the item's
 * drop sources from the {@link DropRates} table when it has any. The id comes from the item manager's price list, and,
 * when that does not know the name (a pet and the other untradeables have no price), from the {@link ItemNameIndex}. Every
 * item manager lookup is the item manager's, so a call belongs on the client thread, the caller's.
 */
class ItemCards
{
	private final ItemManager itemManager;
	/** The drop rate table, or null for none. */
	private final DropRates dropRates;
	/** The names of every real item, or null for none; asked when the price list does not know a name. */
	private final ItemNameIndex index;

	/** Cards without drop rates and without the names index: no table, no index. */
	ItemCards(ItemManager itemManager)
	{
		this(itemManager, null, null);
	}

	/** Cards with the drop rates of {@code dropRates}, which may still be loading, and no names index. */
	ItemCards(ItemManager itemManager, DropRates dropRates)
	{
		this(itemManager, dropRates, null);
	}

	/** Cards with the drop rates of {@code dropRates} and the names of {@code index}, either of which may still be loading. */
	ItemCards(ItemManager itemManager, DropRates dropRates, ItemNameIndex index)
	{
		this.itemManager = itemManager;
		this.dropRates = dropRates;
		this.index = index;
	}

	/**
	 * The id of the item called {@code name}: the first search result whose name equals {@code name} ignoring case (a
	 * result that only starts with it, such as "Dragon claws (or)", does not count), else the id the names index holds for
	 * the lowercase name, else -1.
	 *
	 * <p>Choice: a null name has no id.
	 */
	int idOf(String name)
	{
		if (name == null)
		{
			return -1;
		}
		for (final ItemPrice result : itemManager.search(name))
		{
			if (name.equalsIgnoreCase(result.getName()))
			{
				return result.getId();
			}
		}
		final Integer indexed = index == null ? null : index.idOf(name.toLowerCase(Locale.ROOT));
		return indexed == null ? -1 : indexed;
	}

	/**
	 * The card for the item called {@code name}, or null when no item has that name ({@link #idOf} gives -1). The card's
	 * price is the item manager's, which is 0 for an item that has none, and its drops are the sources {@link
	 * DropRates#lookup} gives for the name, of which the card shows four and counts the rest, or none when there is no
	 * table.
	 *
	 * <p>Choice: a null name gives no card, as an unknown one does.
	 */
	HoverCard forName(String name, int quantity, String plainLine, int start, int end)
	{
		final int id = idOf(name);
		if (id < 0)
		{
			return null;
		}
		final long price = itemManager.getItemPrice(id);
		final int alch = itemManager.getItemComposition(id).getHaPrice();
		return new ItemCard(id, name, quantity, price, alch, plainLine, start, end, sources(name));
	}

	/**
	 * The rate of the first source the table gives for the name, as the table writes it ("1/4000"), or an empty text when
	 * the table has not loaded, does not hold the name, or has nothing for it. It reads the sources the way {@link
	 * #forName} does, so the name's case counts the same; a pet's card asks it for the pet's own rate.
	 */
	String rateOf(String name)
	{
		final List<DropRates.DropSource> found = sources(name);
		return found.isEmpty() ? "" : found.get(0).rate;
	}

	/**
	 * The sources of the item, the table's list, or an empty one.
	 *
	 * <p>Choice: a table that has not loaded is not asked, so a card made in the first second after start-up is the card of
	 * lot 1.
	 */
	private List<DropRates.DropSource> sources(String name)
	{
		if (dropRates == null || !dropRates.loaded())
		{
			return Collections.emptyList();
		}
		return dropRates.lookup(name);
	}
}
