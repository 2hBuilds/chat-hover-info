package com.clanhub;

import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/** {@link ItemNameIndex}: built in slices, it keeps every real item's name, tradeable or not, and skips notes, placeholders and junk. */
public class ItemNameIndexTest
{
	/** An item the way the cache gives one: not a note, not a placeholder, the given name and worth. */
	private static ItemComposition item(String name, boolean tradeable, int alch, int note)
	{
		final ItemComposition composition = Mockito.mock(ItemComposition.class);
		when(composition.getName()).thenReturn(name);
		when(composition.isTradeable()).thenReturn(tradeable);
		when(composition.getHaPrice()).thenReturn(alch);
		when(composition.getNote()).thenReturn(note);
		when(composition.getPlaceholderTemplateId()).thenReturn(-1);
		return composition;
	}

	@Test
	public void buildsInChunksSkippingNotesPlaceholdersAndJunk()
	{
		final Client client = Mockito.mock(Client.class);
		when(client.getItemCount()).thenReturn(6);
		final ItemManager items = Mockito.mock(ItemManager.class);
		final ItemComposition[] compositions = {
			item("Dragon claws", true, 0, -1),
			item("Dragon claws", true, 0, 0),
			item("null", false, 0, -1),
			item("Man", false, 0, -1),
			item("Abyssal whip", true, 0, -1),
			item("Bones", false, 1, -1)};
		for (int id = 0; id < compositions.length; id++)
		{
			when(items.getItemComposition(id)).thenReturn(compositions[id]);
		}
		final ItemNameIndex index = new ItemNameIndex();
		assertFalse("not complete before it reads anything", index.complete());

		index.buildSome(client, items, 4);
		assertFalse(index.complete());
		assertEquals(4, index.next);
		assertEquals(Integer.valueOf(0), index.idOf("dragon claws"));
		assertNull(index.idOf("abyssal whip"));

		index.buildSome(client, items, 4);
		assertTrue(index.complete());
		assertEquals(Integer.valueOf(4), index.idOf("abyssal whip"));
		assertEquals(Integer.valueOf(5), index.idOf("bones"));
		assertNull(index.idOf("man"));
		assertNull(index.idOf("null"));
		assertEquals(3, index.size());
	}

	@Test
	public void keepsUntradeableNamesOfFiveCharactersOrMore()
	{
		final Client client = Mockito.mock(Client.class);
		when(client.getItemCount()).thenReturn(3);
		final ItemManager items = Mockito.mock(ItemManager.class);
		final ItemComposition[] compositions = {
			item("Pet snakeling", false, 0, -1),
			item("Man", false, 0, -1),
			item("null", false, 0, -1)};
		for (int id = 0; id < compositions.length; id++)
		{
			when(items.getItemComposition(id)).thenReturn(compositions[id]);
		}
		final ItemNameIndex index = new ItemNameIndex();

		index.buildSome(client, items, 10);

		assertTrue(index.complete());
		assertEquals(Integer.valueOf(0), index.idOf("pet snakeling"));
		assertNull(index.idOf("man"));
		assertNull(index.idOf("null"));
		assertEquals(1, index.size());
	}
}
