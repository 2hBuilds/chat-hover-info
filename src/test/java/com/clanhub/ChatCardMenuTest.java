package com.clanhub;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link CardMenu}: the two entries a card's line adds to the menu, what they do when clicked, and none without a card. */
public class ChatCardMenuTest
{
	private static final String LINE = "Klar received a drop: Dragon claws (121,453,000 coins).";

	private static ItemCard claws()
	{
		return new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, LINE, 24, 36);
	}

	/** An entry whose setters each return the entry itself, the way the client's do. */
	private static MenuEntry entry()
	{
		return Mockito.mock(MenuEntry.class, Answers.RETURNS_SELF);
	}

	@SuppressWarnings("unchecked")
	private static Consumer<MenuEntry> clickOf(MenuEntry entry)
	{
		final ArgumentCaptor<Consumer<MenuEntry>> click = ArgumentCaptor.forClass(Consumer.class);
		verify(entry).onClick(click.capture());
		return click.getValue();
	}

	@Test
	public void twoEntriesWhenAnItemIsHoveredNoneOtherwise()
	{
		final MenuEntry wiki = entry();
		final MenuEntry copy = entry();
		final Menu menu = Mockito.mock(Menu.class);
		// Re-aimed by the lead, 2026-10-07: the client draws the LAST created entry at the top, so Copy is made first
		// and Open wiki last (the plan had said the reverse).
		when(menu.createMenuEntry(-1)).thenReturn(copy, wiki);
		final List<String> browsed = new ArrayList<>();
		final List<String> copied = new ArrayList<>();

		CardMenu.addEntries(menu, claws(), browsed::add, copied::add);

		verify(wiki).setOption("Open wiki");
		verify(copy).setOption("Copy");
		final ArgumentCaptor<String> wikiTarget = ArgumentCaptor.forClass(String.class);
		final ArgumentCaptor<String> copyTarget = ArgumentCaptor.forClass(String.class);
		verify(wiki).setTarget(wikiTarget.capture());
		verify(copy).setTarget(copyTarget.capture());
		assertTrue(wikiTarget.getValue(), wikiTarget.getValue().contains("Dragon claws"));
		assertTrue(copyTarget.getValue(), copyTarget.getValue().contains("Dragon claws"));
		verify(wiki).setType(MenuAction.RUNELITE);
		verify(copy).setType(MenuAction.RUNELITE);

		assertTrue("nothing runs before a click", browsed.isEmpty() && copied.isEmpty());
		clickOf(wiki).accept(wiki);
		assertEquals(List.of("https://oldschool.runescape.wiki/w/Special:Lookup?type=item&id=13652&name=Dragon%20claws"),
			browsed);
		assertTrue("Open wiki copies nothing", copied.isEmpty());
		clickOf(copy).accept(copy);
		assertEquals(List.of(LINE), copied);
		assertEquals("Copy browses nothing more", 1, browsed.size());

		final Menu untouched = Mockito.mock(Menu.class);
		CardMenu.addEntries(untouched, null, browsed::add, copied::add);
		verify(untouched, never()).createMenuEntry(anyInt());
	}
}
