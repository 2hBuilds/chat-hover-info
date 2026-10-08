package com.clanhub;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** {@link DemoLines}: the item samples are broadcasts the reader reads, and they lead the list of every sample. */
public class DemoLinesTest
{
	@Test
	public void everyItemSampleIsABroadcastTheReaderReads()
	{
		final List<String> expected = Arrays.asList("Dragon claws", "Black knife", "Ranger boots", "Abyssal whip");
		assertEquals(expected.size(), DemoLines.ITEMS.size());
		for (int i = 0; i < DemoLines.ITEMS.size(); i++)
		{
			final DemoLines.Line line = DemoLines.ITEMS.get(i);
			final Optional<BroadcastLine.ItemMention> found = BroadcastLine.parse(line.type, line.text);
			assertTrue("a mention in: " + line.text, found.isPresent());
			assertEquals(expected.get(i), found.get().name);
			assertEquals("a broadcast has no speaker", "", line.name);
		}

		final List<DemoLines.Line> all = DemoLines.lines();
		assertTrue("lines() holds at least the four", all.size() >= DemoLines.ITEMS.size());
		assertEquals("lines() starts with the four", DemoLines.ITEMS, all.subList(0, DemoLines.ITEMS.size()));
	}

	@Test
	public void theOwnSamplesFollowTheItems()
	{
		// Re-aimed by the lead, 2026-10-08 (lot 8e): player chat is never read, so the demo has no typed line.
		final List<DemoLines.Line> all = DemoLines.lines();
		final int items = DemoLines.ITEMS.size();
		assertEquals(2, DemoLines.OWN.size());
		assertEquals("the own lines follow the items", DemoLines.OWN, all.subList(items, items + 2));
		assertEquals("15 lines in all", 15, all.size());

		final ItemCards itemCards = Mockito.mock(ItemCards.class);
		when(itemCards.forName(anyString(), anyInt(), anyString(), anyInt(), anyInt())).thenAnswer(call ->
			new ItemCard(0, call.getArgument(0), call.getArgument(1), 0L, 0, call.getArgument(2),
				call.getArgument(3), call.getArgument(4)));
		final OwnLineReader reader = new OwnLineReader(itemCards);
		for (final DemoLines.Line line : DemoLines.OWN)
		{
			assertEquals(ChatMessageType.GAMEMESSAGE, line.type);
			assertEquals("an own game line has no speaker", "", line.name);
			assertEquals("one card from: " + line.text, 1, reader.read(line.type, line.text).size());
		}
	}

	@Test
	public void theKindSamplesEndTheList() throws Exception
	{
		assertEquals(9, DemoLines.KINDS.size());
		for (final DemoLines.Line line : DemoLines.KINDS)
		{
			assertEquals(ChatMessageType.CLAN_MESSAGE, line.type);
			assertEquals("a broadcast has no speaker", "", line.name);
		}
		final List<DemoLines.Line> all = DemoLines.lines();
		assertEquals("lines() ends with the kinds", DemoLines.KINDS, all.subList(all.size() - 9, all.size()));

		final CardTables tables;
		try (Reader in = new InputStreamReader(DemoLinesTest.class.getResourceAsStream("/com/clanhub/cards-fixture.json"),
			StandardCharsets.UTF_8))
		{
			tables = CardTables.parse(in, new Gson());
		}
		final BroadcastKindReader reader = new BroadcastKindReader(tables, Mockito.mock(ItemCards.class));
		for (final DemoLines.Line line : DemoLines.KINDS)
		{
			assertTrue("a card from: " + line.text, reader.read(line.type, line.text).size() >= 1);
		}
	}
}
