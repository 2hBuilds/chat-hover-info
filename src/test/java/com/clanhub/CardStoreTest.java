package com.clanhub;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/** {@link CardStore}: the line a widget's text holds and where it starts, and the last 200 lines. */
public class CardStoreTest
{
	private static final String CLAWS_LINE = "Klar received a drop: Dragon claws (121,453,000 coins).";

	private static ItemCard claws()
	{
		return new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, CLAWS_LINE, 24, 36);
	}

	@Test
	public void findsTheLineTheWidgetTextHoldsAndItsOffset()
	{
		final CardStore store = new CardStore();
		final ItemCard card = claws();
		store.put(CLAWS_LINE, Collections.<HoverCard>singletonList(card));

		final CardStore.Match prefixed = store.find("[2h Clan] " + CLAWS_LINE);
		assertNotNull("the prefixed widget text holds the line", prefixed);
		assertEquals(1, prefixed.cards.size());
		assertSame(card, prefixed.cards.get(0));
		assertEquals("the line starts after the 10-char prefix", 10, prefixed.offset);

		assertNull("a line nobody stored", store.find("Klar: gz!!!"));

		final CardStore.Match bare = store.find(CLAWS_LINE);
		assertNotNull(bare);
		assertEquals(0, bare.offset);

		store.clear();
		assertNull("cleared", store.find(CLAWS_LINE));
	}

	@Test
	public void keepsTheLastTwoHundredLines()
	{
		final CardStore store = new CardStore();
		final HoverCard card = claws();
		for (int i = 0; i < 201; i++)
		{
			store.put(line(i), Collections.singletonList(card));
		}

		assertNull("the first line is gone", store.find(line(0)));
		assertNotNull("the second is kept", store.find(line(1)));
		assertNotNull("the 201st is kept", store.find(line(200)));

		store.put("an empty list", Collections.<HoverCard>emptyList());
		assertNull("an empty list stores nothing", store.find("an empty list"));
		assertNotNull("and pushes nothing out", store.find(line(1)));
	}

	/** A line of its own: the number is padded to three digits, so no line is inside another. */
	private static String line(int i)
	{
		return "Klar received a drop: item " + String.format("%03d", i) + ".";
	}
}
