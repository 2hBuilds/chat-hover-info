package com.clanhub;

import java.util.Collections;
import java.util.List;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link BroadcastItemReader}: a drop line gives its one card, other chat gives none and asks for nothing. */
public class BroadcastItemReaderTest
{
	private static final String LINE = "Klar received a drop: Dragon claws (121,453,000 coins).";

	@Test
	public void aDropLineGivesOneCardAndChatterNone()
	{
		final int start = LINE.indexOf("Dragon claws");
		final int end = start + "Dragon claws".length();
		final HoverCard card = new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, LINE, start, end);
		final ItemCards itemCards = Mockito.mock(ItemCards.class);
		when(itemCards.forName("Dragon claws", 1, LINE, start, end)).thenReturn(card);
		final BroadcastItemReader reader = new BroadcastItemReader(itemCards);

		assertEquals(Collections.singletonList(card), reader.read(ChatMessageType.CLAN_MESSAGE, LINE));

		final List<HoverCard> chatter = reader.read(ChatMessageType.CLAN_MESSAGE, "Klar: gz!!!");
		assertTrue("chatter holds no card", chatter.isEmpty());
		verify(itemCards, times(1)).forName(anyString(), anyInt(), anyString(), anyInt(), anyInt());
	}
}
