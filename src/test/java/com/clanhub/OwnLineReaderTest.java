package com.clanhub;

import java.util.List;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** {@link OwnLineReader}: the player's valuable drop, untradeable drop and collection log lines each give their item's card. */
public class OwnLineReaderTest
{
	/** An {@link ItemCards} that answers every name with a card titled by it, at the offsets it was given. */
	private static ItemCards echoing()
	{
		final ItemCards itemCards = Mockito.mock(ItemCards.class);
		when(itemCards.forName(anyString(), anyInt(), anyString(), anyInt(), anyInt())).thenAnswer(call ->
			new ItemCard(0, call.getArgument(0), call.getArgument(1), 0L, 0, call.getArgument(2),
				call.getArgument(3), call.getArgument(4)));
		return itemCards;
	}

	@Test
	public void readsValuableUntradeableAndCollectionLogLines()
	{
		final OwnLineReader reader = new OwnLineReader(echoing());

		final List<HoverCard> valuable = reader.read(ChatMessageType.GAMEMESSAGE,
			"Valuable drop: Dragon claws (121,453,000 coins)");
		assertEquals(1, valuable.size());
		assertEquals("Dragon claws", valuable.get(0).title());
		assertEquals(1, valuable.get(0).quantity());
		assertEquals(15, valuable.get(0).start());
		assertEquals(27, valuable.get(0).end());

		final List<HoverCard> stack = reader.read(ChatMessageType.GAMEMESSAGE,
			"Valuable drop: 10 x Black knife (4,600 coins)");
		assertEquals(1, stack.size());
		assertEquals(10, stack.get(0).quantity());
		assertEquals("Black knife x10", stack.get(0).title());

		final List<HoverCard> pet = reader.read(ChatMessageType.GAMEMESSAGE, "Untradeable drop: Pet snakeling");
		assertEquals(1, pet.size());
		assertEquals("Pet snakeling", pet.get(0).title());
		final List<HoverCard> valued = reader.read(ChatMessageType.GAMEMESSAGE,
			"Untradeable drop: Pet snakeling (1 coin)");
		assertEquals("Pet snakeling", valued.get(0).title());

		final List<HoverCard> log = reader.read(ChatMessageType.GAMEMESSAGE,
			"New item added to your collection log: Abyssal whip");
		assertEquals(1, log.size());
		assertEquals("Abyssal whip", log.get(0).title());
		assertEquals(39, log.get(0).start());
		assertEquals(51, log.get(0).end());

		assertTrue(reader.read(ChatMessageType.GAMEMESSAGE, "You have been poisoned.").isEmpty());
		assertTrue("only own game lines are read",
			reader.read(ChatMessageType.CLAN_MESSAGE, "Valuable drop: Dragon claws (121,453,000 coins)").isEmpty());
	}
}
