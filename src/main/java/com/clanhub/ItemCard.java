package com.clanhub;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.util.QuantityFormatter;

/**
 * What the card says about one item named in a chat line: the item, how many, its prices, and the line it came from
 * with the place of the name in it. Pure model: it holds numbers and words and makes the card's texts and the wiki
 * address; it draws nothing and asks the client nothing. It is the {@link HoverCard} of an item: no notes, and the
 * item's own sprite for its picture.
 */
final class ItemCard implements HoverCard
{
	/** The card's last line. */
	private static final String HINT = "Right-click: Open wiki | Copy";
	/** The wiki's item lookup, to which the id and the name are added. */
	private static final String WIKI_LOOKUP = "https://oldschool.runescape.wiki/w/Special:Lookup?type=item&id=";
	/** What a price or an alch value of nothing reads as. */
	private static final String NONE = "-";
	/** How many drop sources a card shows at most; the rest are counted in the "and N more" row. */
	private static final int MAX_DROPS = 4;

	/** The item's id. */
	final int id;
	/** The item's name. */
	final String name;
	/** How many of the item the line names. */
	final int quantity;
	/** The market price of one, in coins; 0 when the item has none. */
	final long price;
	/** The high alchemy value of one, in coins. */
	final int alch;
	/** The chat line without its tags. */
	final String line;
	/** Where the name starts in {@link #line}. */
	final int start;
	/** Just past the name's last character in {@link #line}. */
	final int end;
	/** The drop sources the card shows, the table's first four in its order; empty when the table has none. */
	private final List<DropRates.DropSource> drops;
	/** How many sources the table holds beyond the ones shown. */
	private final int more;

	ItemCard(int id, String name, int quantity, long price, int alch, String line, int start, int end)
	{
		this(id, name, quantity, price, alch, line, start, end, Collections.<DropRates.DropSource>emptyList());
	}

	/**
	 * The card with the item's drop sources in the table's order (the monsters first, the best rate first), or an empty
	 * list for none.
	 *
	 * <p>Choice: the card shows the table's first four sources in the table's order, none reversed, so the best monster
	 * leads; when the table holds more, {@link #rows} ends in one more row, "and N more" with N the count beyond four,
	 * a row with no rate, so either painter draws it as a grey line of its own.
	 */
	ItemCard(int id, String name, int quantity, long price, int alch, String line, int start, int end,
		List<DropRates.DropSource> drops)
	{
		this.drops = Collections.unmodifiableList(new ArrayList<>(drops.subList(0, Math.min(MAX_DROPS, drops.size()))));
		this.more = drops.size() - this.drops.size();
		this.id = id;
		this.name = name;
		this.quantity = quantity;
		this.price = price;
		this.alch = alch;
		this.line = line;
		this.start = start;
		this.end = end;
	}

	/** The card's title: the name, with "x10" after it for a stack. */
	@Override
	public String title()
	{
		return quantity > 1 ? name + " x" + quantity : name;
	}

	/**
	 * The card's rows of label and value pairs: the price and the high alch value, and under them one row for each drop
	 * source (at most four), each a single pair with the source's name as its label and the rate as its value
	 * ("Abyssal demon", "1/512"), so a painter can set the name at the left and the rate at the right; when the table
	 * holds more sources, a last row of one pair with "and 2 more" as its label and no value. For a stack the
	 * price is the total with the price of one after it, and the alch value is the total; a price of nothing reads "-".
	 */
	@Override
	public List<List<Pair>> rows()
	{
		final String each = quantity > 1 && price > 0 ? " (" + stackSize(price) + " each)" : "";
		final Pair priced = new Pair("Price", price > 0 ? stackSize(price * quantity) : NONE, each);
		final Pair alched = new Pair("High alch", alch > 0 ? stackSize((long) alch * quantity) : NONE, "");
		final List<List<Pair>> rows = new ArrayList<>();
		rows.add(Arrays.asList(priced, alched));
		for (final DropRates.DropSource source : drops)
		{
			rows.add(Collections.singletonList(new Pair(source.from, source.rate, "")));
		}
		if (more > 0)
		{
			rows.add(Collections.singletonList(new Pair("and " + more + " more", "", "")));
		}
		return rows;
	}

	/** An item has no description lines. */
	@Override
	public List<String> notes()
	{
		return Collections.emptyList();
	}

	/** The card's last line: what a right-click does. */
	@Override
	public String hint()
	{
		return HINT;
	}

	/**
	 * The wiki's lookup page for the item, by id and name.
	 *
	 * <p>Choice: the name is percent-encoded in full, a space as %20 (not +), so a name with an apostrophe or a bracket
	 * reaches the wiki whole.
	 */
	@Override
	public String wikiUrl()
	{
		return WIKI_LOOKUP + id + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/** The wiki page's title for the card's paragraph: the item's name, which the wiki's redirects resolve. */
	@Override
	public String wikiTitle()
	{
		return name;
	}

	/** The chat line as plain text, what "Copy" puts on the clipboard. */
	@Override
	public String plainLine()
	{
		return line;
	}

	/** Where the item's name starts in the line. */
	@Override
	public int start()
	{
		return start;
	}

	/** Just past the item's name in the line. */
	@Override
	public int end()
	{
		return end;
	}

	/** The item's id: its sprite is the card's picture. */
	@Override
	public int itemId()
	{
		return id;
	}

	/** How many of the item the line names. */
	@Override
	public int quantity()
	{
		return quantity;
	}

	/** An item's picture is its item sprite, never a game sprite. */
	@Override
	public int spriteId()
	{
		return -1;
	}

	/** An item's name is coloured in the chat. */
	@Override
	public boolean marked()
	{
		return true;
	}

	/** A number the way the game writes a stack: 121M, 25.2M, 123K, 4,600. */
	private static String stackSize(long value)
	{
		return QuantityFormatter.quantityToStackSize(value);
	}
}
