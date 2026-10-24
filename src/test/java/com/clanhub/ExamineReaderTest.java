package com.clanhub;

import com.clanhub.ClanHubConfig.ExamineLine;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.game.ItemManager;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link ExamineReader}: the click on Examine names the thing, and the examine line that follows within two game ticks
 * gets its card, once.
 */
public class ExamineReaderTest
{
	private static final String WHIP_LINE = "A weapon from the abyss.";
	private static final String VORKATH_LINE = "Azure and ancient.";

	/** The game tick the reader is told, which a test moves. */
	private final int[] now = {100};
	/** Whether the "Examine cards" setting is on, which a test flips. */
	private final boolean[] on = {true};
	/** How an examine line reads, which a test sets. */
	private final ExamineLine[] style = {ExamineLine.NAME_FIRST};

	/** An {@link ItemManager} that knows the abyssal whip (and its noted form), and an id the game has no name for. */
	private static ItemCards itemCards()
	{
		final ItemManager itemManager = Mockito.mock(ItemManager.class);
		when(itemManager.canonicalize(4151)).thenReturn(4151);
		when(itemManager.canonicalize(4152)).thenReturn(4151);
		when(itemManager.canonicalize(77)).thenReturn(77);
		final ItemComposition whip = Mockito.mock(ItemComposition.class);
		when(whip.getName()).thenReturn("Abyssal whip");
		when(whip.getHaPrice()).thenReturn(72_000);
		when(itemManager.getItemComposition(4151)).thenReturn(whip);
		when(itemManager.getItemPrice(4151)).thenReturn(1_500_000L);
		final ItemComposition nothing = Mockito.mock(ItemComposition.class);
		when(nothing.getName()).thenReturn("null");
		when(itemManager.getItemComposition(77)).thenReturn(nothing);
		return new ItemCards(itemManager);
	}

	private ExamineReader reader()
	{
		return new ExamineReader(itemCards(), new CardTables(), () -> on[0], () -> now[0], () -> style[0]);
	}

	/** A menu click, answering what the reader asks of one. */
	private static MenuOptionClicked click(String option, String target, MenuAction action, int id, int itemId, NPC npc)
	{
		final MenuOptionClicked event = Mockito.mock(MenuOptionClicked.class);
		when(event.getMenuOption()).thenReturn(option);
		when(event.getMenuTarget()).thenReturn(target);
		when(event.getMenuAction()).thenReturn(action);
		when(event.getId()).thenReturn(id);
		when(event.getItemId()).thenReturn(itemId);
		final MenuEntry entry = Mockito.mock(MenuEntry.class);
		when(entry.getNpc()).thenReturn(npc);
		when(event.getMenuEntry()).thenReturn(entry);
		return event;
	}

	@Test
	public void anItemClickThenItsExamineLineGivesTheItemsCard()
	{
		final ExamineReader reader = reader();

		reader.itemExamined(4151);
		final List<HoverCard> cards = reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE);

		assertEquals("one card", 1, cards.size());
		final HoverCard card = cards.get(0);
		assertEquals("Abyssal whip", card.title());
		assertEquals(WHIP_LINE, card.plainLine());
		assertEquals("the whole text is the hover words, from its start", 0, card.start());
		assertEquals("to its end", WHIP_LINE.length(), card.end());
		assertTrue("marked", card.marked());
		assertEquals("the item's price", 1_500_000L, ((ItemCard) card).price);
		assertEquals("the item's high alchemy value", 72_000, ((ItemCard) card).alch);
		assertTrue("one click, one card: the same line again gives nothing",
			reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());
	}

	@Test
	public void aLineWithoutAFreshClickGivesNothing()
	{
		final ExamineReader reader = reader();
		assertTrue("no click: nothing", reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());

		reader.itemExamined(4151);
		now[0] = 103;
		assertTrue("a click at 100 read at 103: nothing", reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());

		now[0] = 100;
		reader.itemExamined(4151);
		now[0] = 102;
		assertEquals("a click at 100 read at 102: the card", 1, reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).size());

		now[0] = 100;
		reader.itemExamined(4151);
		now[0] = 99;
		assertTrue("a click from the future: nothing", reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());

		now[0] = 100;
		reader.itemExamined(4151);
		assertTrue("a game message after the click: nothing",
			reader.read(ChatMessageType.GAMEMESSAGE, WHIP_LINE).isEmpty());
		assertTrue("a monster's examine line after an item click: nothing",
			reader.read(ChatMessageType.NPC_EXAMINE, VORKATH_LINE).isEmpty());
		assertEquals("and the item's own line after them still gives the card", 1,
			reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).size());
	}

	@Test
	public void aNotedItemGivesTheItemItself()
	{
		final ExamineReader reader = reader();

		reader.itemExamined(4152);
		final List<HoverCard> cards = reader.read(ChatMessageType.ITEM_EXAMINE, "Swap this note at any bank for the equivalent item.");
		assertEquals(1, cards.size());
		assertEquals("the note gives the item itself", "Abyssal whip", cards.get(0).title());
		assertEquals(4151, ((ItemCard) cards.get(0)).id);
		assertEquals("and its picture is the item's", 4151, cards.get(0).itemId());

		reader.itemExamined(77);
		assertTrue("an id the game has no name for: nothing", reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());
	}

	@Test
	public void aMonsterClickGivesItsCard()
	{
		final ExamineReader reader = reader();

		reader.npcExamined("Vorkath");
		final List<HoverCard> cards = reader.read(ChatMessageType.NPC_EXAMINE, VORKATH_LINE);

		assertEquals("one card", 1, cards.size());
		final HoverCard card = cards.get(0);
		assertEquals("Vorkath", card.title());
		assertTrue("marked", card.marked());
		assertTrue("with the wiki's picture", card.wikiPicture());
		assertEquals(VORKATH_LINE, card.plainLine());
		assertEquals(0, card.start());
		assertEquals(VORKATH_LINE.length(), card.end());
		assertTrue("a table with no such boss gives no facts", card.rows().get(0).isEmpty());
		assertEquals("Vorkath", card.wikiTitle());
		assertTrue("one click, one card", reader.read(ChatMessageType.NPC_EXAMINE, VORKATH_LINE).isEmpty());
	}

	@Test
	public void aBossTheTableKnowsGivesItsFacts() throws Exception
	{
		final CardTables tables;
		try (Reader cards = new InputStreamReader(ExamineReaderTest.class.getResourceAsStream(
			"/com/clanhub/cards-fixture.json"), StandardCharsets.UTF_8);
			Reader bosses = new InputStreamReader(ExamineReaderTest.class.getResourceAsStream(
				"/com/clanhub/bosses-fixture.json"), StandardCharsets.UTF_8))
		{
			tables = CardTables.parse(cards, new Gson()).readBosses(bosses, new Gson());
		}
		final ExamineReader reader = new ExamineReader(itemCards(), tables, () -> true, () -> now[0],
			() -> style[0]);

		reader.npcExamined("Duke Sucellus");
		final HoverCard card = reader.read(ChatMessageType.NPC_EXAMINE, "A vampyric Duke.").get(0);

		final List<String> facts = new ArrayList<>();
		for (final HoverCard.Pair pair : card.rows().get(0))
		{
			facts.add(pair.label + " " + pair.value);
		}
		assertEquals(Arrays.asList("Combat 450", "Hitpoints 440", "Max hit 24",
			"Quest Desert Treasure II - The Fallen Empire"), facts);
	}

	@Test
	public void theSwitchOffGivesNothing()
	{
		final ExamineReader reader = reader();

		on[0] = false;
		reader.itemExamined(4151);
		assertTrue("off: an item click and its line give nothing",
			reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());

		on[0] = true;
		reader.itemExamined(4151);
		assertEquals("on again, a new click and its line give the card", 1,
			reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).size());
	}

	/** Lot 10B: the name goes in front of the text, and the card's words are the name alone. */
	@Test
	public void nameFirstPutsTheNameInFrontAndHoversIt()
	{
		final ExamineReader reader = reader();
		style[0] = ExamineLine.NAME_FIRST;
		final String labelled = "Abyssal whip: " + WHIP_LINE;

		reader.itemExamined(4151);
		assertEquals("the name, a colon and the text", labelled, reader.label(ChatMessageType.ITEM_EXAMINE, WHIP_LINE));
		assertEquals("labelling forgets nothing: asked again, the same", labelled,
			reader.label(ChatMessageType.ITEM_EXAMINE, WHIP_LINE));

		final List<HoverCard> cards = reader.read(ChatMessageType.ITEM_EXAMINE, labelled);
		assertEquals("one card", 1, cards.size());
		final HoverCard card = cards.get(0);
		assertEquals(labelled, card.plainLine());
		assertEquals("the hover words start at the name", 0, card.start());
		assertEquals("and end with it", 12, card.end());
		assertEquals("Abyssal whip", card.title());
		assertTrue("one click, one card: the same line again gives nothing",
			reader.read(ChatMessageType.ITEM_EXAMINE, labelled).isEmpty());
	}

	/** Lot 10B: with the style at 'Description only' the line stays as it came and all of it is the hover words. */
	@Test
	public void descriptionOnlyLeavesTheLine()
	{
		final ExamineReader reader = reader();
		style[0] = ExamineLine.DESCRIPTION;

		reader.itemExamined(4151);
		assertEquals("the line is unchanged", WHIP_LINE, reader.label(ChatMessageType.ITEM_EXAMINE, WHIP_LINE));

		final List<HoverCard> cards = reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE);
		assertEquals("one card", 1, cards.size());
		assertEquals("over the whole text, from its start", 0, cards.get(0).start());
		assertEquals("to its end", WHIP_LINE.length(), cards.get(0).end());
	}

	/** Lot 10B: a line that answers no fresh click of its kind is never given a name. */
	@Test
	public void noFreshClickNeverChangesTheLine()
	{
		final ExamineReader reader = reader();
		style[0] = ExamineLine.NAME_FIRST;
		assertEquals("no click: unchanged", WHIP_LINE, reader.label(ChatMessageType.ITEM_EXAMINE, WHIP_LINE));

		reader.itemExamined(4151);
		now[0] = 103;
		assertEquals("a click at 100 labelled at 103: unchanged", WHIP_LINE,
			reader.label(ChatMessageType.ITEM_EXAMINE, WHIP_LINE));

		now[0] = 100;
		on[0] = false;
		reader.itemExamined(4151);
		assertEquals("'Examine cards' off: unchanged", WHIP_LINE, reader.label(ChatMessageType.ITEM_EXAMINE, WHIP_LINE));

		on[0] = true;
		assertEquals("a game message after the item click: unchanged", WHIP_LINE,
			reader.label(ChatMessageType.GAMEMESSAGE, WHIP_LINE));
		assertEquals("a monster's line after the item click: unchanged", VORKATH_LINE,
			reader.label(ChatMessageType.NPC_EXAMINE, VORKATH_LINE));

		reader.itemExamined(77);
		assertEquals("an item whose name is 'null': unchanged", WHIP_LINE,
			reader.label(ChatMessageType.ITEM_EXAMINE, WHIP_LINE));

		reader.itemExamined(4151);
		assertNull("a null message is null", reader.label(ChatMessageType.ITEM_EXAMINE, null));
	}

	/** Lot 10B: a monster's line gets the monster's name in front too. */
	@Test
	public void aMonsterNameFirst()
	{
		final ExamineReader reader = reader();
		style[0] = ExamineLine.NAME_FIRST;
		final String labelled = "Vorkath: " + VORKATH_LINE;

		reader.npcExamined("Vorkath");
		assertEquals(labelled, reader.label(ChatMessageType.NPC_EXAMINE, VORKATH_LINE));

		final List<HoverCard> cards = reader.read(ChatMessageType.NPC_EXAMINE, labelled);
		assertEquals("one card", 1, cards.size());
		final HoverCard card = cards.get(0);
		assertEquals("Vorkath", card.title());
		assertEquals(labelled, card.plainLine());
		assertEquals("the hover words start at the name", 0, card.start());
		assertEquals("and end with it", 7, card.end());
		assertTrue("with the wiki's picture", card.wikiPicture());
	}

	@Test
	public void theClickIsReadFromTheMenu()
	{
		// An examine of an item on the ground.
		ExamineReader reader = reader();
		ExamineReader.clicked(reader, click("Examine", "<col=ff9040>Abyssal whip", MenuAction.EXAMINE_ITEM_GROUND, 4151, -1, null));
		List<HoverCard> cards = reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE);
		assertEquals("a ground item remembers its id", 1, cards.size());
		assertEquals("Abyssal whip", cards.get(0).title());

		// Another option on the same item is not an examine.
		reader = reader();
		ExamineReader.clicked(reader, click("Use", "<col=ff9040>Abyssal whip", MenuAction.CC_OP, 0, 4151, null));
		assertTrue("'Use' remembers nothing", reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());

		// An examine of an item in the inventory, the bank or the worn items arrives as a widget action with the item's id.
		reader = reader();
		ExamineReader.clicked(reader, click("Examine", "<col=ff9040>Abyssal whip", MenuAction.CC_OP, 0, 4151, null));
		cards = reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE);
		assertEquals("a widget item remembers the item's id", 1, cards.size());
		assertEquals("Abyssal whip", cards.get(0).title());

		// An examine of a monster the menu entry has no NPC for: the name comes from the target.
		reader = reader();
		ExamineReader.clicked(reader, click("Examine", "<col=ffff00>Vorkath<col=ff00>  (level-732)", MenuAction.EXAMINE_NPC,
			0, -1, null));
		cards = reader.read(ChatMessageType.NPC_EXAMINE, VORKATH_LINE);
		assertEquals("a monster remembers its name", 1, cards.size());
		assertEquals("without its tags and its level", "Vorkath", cards.get(0).title());

		// A non-breaking space in a name reads as a space.
		reader = reader();
		ExamineReader.clicked(reader, click("Examine", "<col=ffff00>Giant\u00a0spider<col=ff00>  (level-2)",
			MenuAction.EXAMINE_NPC, 0, -1, null));
		assertEquals("Giant spider", reader.read(ChatMessageType.NPC_EXAMINE, "Incredibly large.").get(0).title());

		// The menu entry's NPC is asked first.
		reader = reader();
		final NPC npc = Mockito.mock(NPC.class);
		when(npc.getName()).thenReturn("Zulrah");
		ExamineReader.clicked(reader, click("Examine", "<col=ffff00>Snakeling", MenuAction.EXAMINE_NPC, 0, -1, npc));
		assertEquals("Zulrah", reader.read(ChatMessageType.NPC_EXAMINE, "A serpent.").get(0).title());

		// An examine of an object forgets what was remembered before it.
		reader = reader();
		ExamineReader.clicked(reader, click("Examine", "<col=ff9040>Abyssal whip", MenuAction.EXAMINE_ITEM_GROUND, 4151, -1, null));
		ExamineReader.clicked(reader, click("Examine", "<col=ffff>Bank booth", MenuAction.EXAMINE_OBJECT, 10355, -1, null));
		assertTrue("an object's examine forgets the item",
			reader.read(ChatMessageType.ITEM_EXAMINE, WHIP_LINE).isEmpty());
	}
}
