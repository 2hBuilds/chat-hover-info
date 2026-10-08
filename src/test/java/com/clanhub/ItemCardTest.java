package com.clanhub;

import com.google.gson.Gson;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/** {@link ItemCard}: the card's texts in stack sizes, the stack's total and each, the dash, and the wiki address. */
public class ItemCardTest
{
	private static final String CLAWS_LINE = "Klar received a drop: Dragon claws (121,453,000 coins).";

	private static ItemCard claws()
	{
		return new ItemCard(ItemID.DRAGON_CLAWS, "Dragon claws", 1, 121_453_000L, 123_000, CLAWS_LINE, 24, 36);
	}

	/** The one row's two pairs. */
	private static List<HoverCard.Pair> theRow(ItemCard card)
	{
		final List<List<HoverCard.Pair>> rows = card.rows();
		assertEquals("one row", 1, rows.size());
		assertEquals("two pairs", 2, rows.get(0).size());
		return rows.get(0);
	}

	@Test
	public void wikiTitleIsTheNameOrThePage()
	{
		assertEquals("Dragon claws", claws().wikiTitle());
	}

	@Test
	public void theRowsReadPriceAndAlchInStackSizes()
	{
		final ItemCard card = claws();
		final List<HoverCard.Pair> row = theRow(card);

		assertEquals("Dragon claws", card.title());
		assertEquals("Price", row.get(0).label);
		assertEquals("121M", row.get(0).value);
		assertEquals("", row.get(0).note);
		assertEquals("High alch", row.get(1).label);
		assertEquals("123K", row.get(1).value);
		assertEquals("Right-click: Open wiki | Copy", card.hint());
		assertEquals("https://oldschool.runescape.wiki/w/Special:Lookup?type=item&id=13652&name=Dragon%20claws",
			card.wikiUrl());
		assertEquals(CLAWS_LINE, card.plainLine());
	}

	@Test
	public void aStackShowsTheTotalAndTheEachAndZeroIsADash()
	{
		final ItemCard stack = new ItemCard(869, "Black knife", 10, 460L, 110,
			"Iron Pete received a drop: 10 x Black knife (460 coins).", 38, 49);
		final List<HoverCard.Pair> row = theRow(stack);

		assertEquals("Black knife x10", stack.title());
		assertEquals("Price", row.get(0).label);
		assertEquals("4,600 (460 each)", row.get(0).value + row.get(0).note);
		assertEquals("the each is the note, so it can be drawn in the label grey", " (460 each)", row.get(0).note);
		assertEquals("High alch", row.get(1).label);
		assertEquals("1,100", row.get(1).value);

		final ItemCard nothing = new ItemCard(1, "Cabbage", 1, 0L, 0, "x", 0, 1);
		final List<HoverCard.Pair> dashes = theRow(nothing);
		assertEquals("-", dashes.get(0).value);
		assertEquals("-", dashes.get(1).value);
	}

	@Test
	public void aDropSourceAddsTheDropRow()
	{
		final ItemCard plain = claws();
		assertEquals("no drop, one row", 1, plain.rows().size());

		final ItemCard card = new ItemCard(ItemID.DRAGON_CLAWS, "Dragon claws", 1, 121_453_000L, 123_000, CLAWS_LINE, 24,
			36, Collections.singletonList(new DropRates.DropSource("Chambers of Xeric", "1/171.6", "Chambers_of_Xeric")));
		final List<List<HoverCard.Pair>> rows = card.rows();
		// Re-aimed by the lead, 2026-10-08 (lot 8k): the table's order stands, four rows, then "and N more".
		assertEquals("one source gives one drop row, with no more row: the price row and the drop row", 2, rows.size());
		assertEquals("the price row is as before", 2, rows.get(0).size());
		assertEquals("Price", rows.get(0).get(0).label);
		assertEquals(1, rows.get(1).size());
		// Re-aimed by the lead, 2026-10-08 (lot 8c): a drop row is the source and its rate, so a painter can right-align the rate.
		assertEquals("Chambers of Xeric", rows.get(1).get(0).label);
		assertEquals("1/171.6", rows.get(1).get(0).value);
		assertEquals("", rows.get(1).get(0).note);
	}

	@Test
	public void fourDropRowsInTheTablesOrderThenMore()
	{
		final DropRates.DropSource unsired = new DropRates.DropSource("Unsired", "1/10.7", "Unsired");
		final DropRates.DropSource greater = new DropRates.DropSource("Greater abyssal demon", "1/170.7",
			"Greater_abyssal_demon");
		final DropRates.DropSource demon = new DropRates.DropSource("Abyssal demon", "1/512", "Abyssal_demon");
		final DropRates.DropSource fourth = new DropRates.DropSource("Fourth source", "1/1000", "Fourth_source");
		final DropRates.DropSource fifth = new DropRates.DropSource("Fifth source", "1/2000", "Fifth_source");
		final DropRates.DropSource sixth = new DropRates.DropSource("Sixth source", "1/3000", "Sixth_source");
		final String line = "Klar received a drop: Abyssal whip (1,500,000 coins).";

		final ItemCard whip = new ItemCard(4151, "Abyssal whip", 1, 1_500_000L, 72_000, line, 22, 34,
			Arrays.asList(unsired, greater, demon));
		final List<List<HoverCard.Pair>> rows = whip.rows();
		assertEquals("the price row and three drop rows", 4, rows.size());
		// Re-aimed by the lead, 2026-10-08 (lot 8k): the table's order stands, four rows, then "and N more".
		assertEquals("Unsired", rows.get(1).get(0).label);
		assertEquals("1/10.7", rows.get(1).get(0).value);
		assertEquals("Greater abyssal demon", rows.get(2).get(0).label);
		assertEquals("1/170.7", rows.get(2).get(0).value);
		assertEquals("Abyssal demon", rows.get(3).get(0).label);
		assertEquals("1/512", rows.get(3).get(0).value);

		final ItemCard four = new ItemCard(4151, "Abyssal whip", 1, 1_500_000L, 72_000, line, 22, 34,
			Arrays.asList(unsired, greater, demon, fourth));
		final List<List<HoverCard.Pair>> fourRows = four.rows();
		assertEquals("four sources: the price row and four drop rows, no more row", 5, fourRows.size());
		assertEquals("the fourth source is the last row", "Fourth source", fourRows.get(4).get(0).label);

		final ItemCard six = new ItemCard(4151, "Abyssal whip", 1, 1_500_000L, 72_000, line, 22, 34,
			Arrays.asList(unsired, greater, demon, fourth, fifth, sixth));
		final List<List<HoverCard.Pair>> sixRows = six.rows();
		assertEquals("six sources: the price row, four drop rows and the more row", 6, sixRows.size());
		assertEquals("the fourth row is the table's fourth", "Fourth source", sixRows.get(4).get(0).label);
		assertEquals("the more row is one pair", 1, sixRows.get(5).size());
		assertEquals("and 2 more", sixRows.get(5).get(0).label);
		assertEquals("the more row has no rate", "", sixRows.get(5).get(0).value);
		assertEquals("", sixRows.get(5).get(0).note);

		final ItemManager itemManager = Mockito.mock(ItemManager.class);
		final ItemPrice found = new ItemPrice();
		found.setId(4151);
		found.setName("Abyssal whip");
		when(itemManager.search("Abyssal whip")).thenReturn(Collections.singletonList(found));
		when(itemManager.getItemPrice(4151)).thenReturn(1_500_000L);
		final ItemComposition composition = Mockito.mock(ItemComposition.class);
		when(composition.getHaPrice()).thenReturn(72_000);
		when(itemManager.getItemComposition(4151)).thenReturn(composition);
		final DropRates rates = DropRates.parse(new StringReader("{\"items\": {\"Abyssal whip\": ["
			+ "{\"from\": \"Unsired\", \"rate\": \"1/10.7\", \"wiki\": \"Unsired\"},"
			+ "{\"from\": \"Greater abyssal demon\", \"rate\": \"1/170.7\", \"wiki\": \"Greater_abyssal_demon\"},"
			+ "{\"from\": \"Abyssal demon\", \"rate\": \"1/512\", \"wiki\": \"Abyssal_demon\"},"
			+ "{\"from\": \"Fourth source\", \"rate\": \"1/1000\", \"wiki\": \"Fourth_source\"},"
			+ "{\"from\": \"Fifth source\", \"rate\": \"1/2000\", \"wiki\": \"Fifth_source\"},"
			+ "{\"from\": \"Sixth source\", \"rate\": \"1/3000\", \"wiki\": \"Sixth_source\"}]}}"), new Gson());
		assertEquals("six sources in the table, five drop rows on the card (four and the more row)", 6,
			new ItemCards(itemManager, rates).forName("Abyssal whip", 1, line, 22, 34).rows().size());
	}
}
