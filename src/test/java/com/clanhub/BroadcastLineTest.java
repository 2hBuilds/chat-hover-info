package com.clanhub;

import java.util.Optional;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** {@link BroadcastLine}: the four sentence shapes, the tags stripped first, and the lines and channels it leaves alone. */
public class BroadcastLineTest
{
	private static BroadcastLine.ItemMention read(ChatMessageType type, String message)
	{
		final Optional<BroadcastLine.ItemMention> found = BroadcastLine.parse(type, message);
		assertTrue("a mention in: " + message, found.isPresent());
		return found.get();
	}

	/** The name's place in the plain text, as the offsets say. */
	private static String named(BroadcastLine.ItemMention mention)
	{
		return mention.text.substring(mention.start, mention.end);
	}

	@Test
	public void aDropLineGivesTheItemAndQuantityOne()
	{
		final BroadcastLine.ItemMention mention = read(ChatMessageType.CLAN_MESSAGE,
			"Klar received a drop: Dragon claws (121,453,000 coins).");

		assertEquals("Dragon claws", mention.name);
		assertEquals(1, mention.quantity);
		assertEquals("Dragon claws", named(mention));
		assertEquals("Klar received a drop: Dragon claws (121,453,000 coins).", mention.text);
	}

	@Test
	public void aStackedDropGivesTheQuantity()
	{
		final BroadcastLine.ItemMention mention = read(ChatMessageType.FRIENDSCHATNOTIFICATION,
			"Iron Pete received a drop: 10 x Black knife (460 coins).");

		assertEquals("Black knife", mention.name);
		assertEquals(10, mention.quantity);
		assertEquals("the offsets skip the quantity", "Black knife", named(mention));
	}

	@Test
	public void aStackOfThousandsWithCommasGivesTheQuantity()
	{
		// The real line from the user's client log, 2026-10-28; it gave no card while the quantity had to be plain digits.
		final BroadcastLine.ItemMention mention = read(ChatMessageType.CLAN_MESSAGE,
			"Jake\u00A0PVM received a drop: 2,359 x Demon tear (577,955 coins).");

		assertEquals("Demon tear", mention.name);
		assertEquals(2359, mention.quantity);
		assertEquals("the offsets skip the quantity", "Demon tear", named(mention));
		assertEquals(null, mention.source);
	}

	@Test
	public void aStackOfMillionsWithCommasKeepsItsSource()
	{
		final BroadcastLine.ItemMention mention = read(ChatMessageType.CLAN_MESSAGE,
			"Klar received a drop: 1,234,567 x Coins (1,234,567 coins) from Doom of Mokhaiotl.");

		assertEquals("Coins", mention.name);
		assertEquals(1234567, mention.quantity);
		assertEquals("Doom of Mokhaiotl", mention.source);
	}

	@Test
	public void aStackTooLargeForAnIntGivesNothing()
	{
		assertFalse(BroadcastLine.parse(ChatMessageType.CLAN_MESSAGE,
			"Klar received a drop: 2,147,483,648 x Coins (2,147,483,648 coins).").isPresent());
		assertFalse(BroadcastLine.parse(ChatMessageType.CLAN_MESSAGE,
			"Klar received a drop: 2147483648 x Coins (2,147,483,648 coins).").isPresent());
	}

	@Test
	public void clueAndCollectionLogLinesAreRead()
	{
		final BroadcastLine.ItemMention clue = read(ChatMessageType.CLAN_GUEST_MESSAGE,
			"Klar received a clue item: Ranger boots (25,296,193 coins).");
		assertEquals("Ranger boots", clue.name);
		assertEquals(1, clue.quantity);
		assertEquals("Ranger boots", named(clue));

		final BroadcastLine.ItemMention log = read(ChatMessageType.CLAN_GIM_MESSAGE,
			"Mossy Rock received a new collection log item: Abyssal whip (1/484).");
		assertEquals("Abyssal whip", log.name);
		assertEquals(1, log.quantity);
		assertEquals("Abyssal whip", named(log));
	}

	@Test
	public void tagsAndIconsAreStrippedFirst()
	{
		final BroadcastLine.ItemMention mention = read(ChatMessageType.CLAN_MESSAGE,
			"<col=0000a0><img=22>Klar received a drop: Dragon claws (1 coins).</col>");

		assertEquals("Dragon claws", mention.name);
		assertTrue("the plain text starts with the player: " + mention.text, mention.text.startsWith("Klar received"));
		assertEquals("Dragon claws", named(mention));
	}

	@Test
	public void aDropWithTheSourceTailStillGivesTheItem()
	{
		final BroadcastLine.ItemMention mention = read(ChatMessageType.CLAN_MESSAGE,
			"Blade Fiery received a drop: Hallowfell (2,320,896 coins) from Mad Angel.");

		assertEquals("Hallowfell", mention.name);
		assertEquals(1, mention.quantity);
		assertEquals("Hallowfell", named(mention));
	}

	@Test
	public void otherLinesAndChannelsGiveNothing()
	{
		assertFalse(BroadcastLine.parse(ChatMessageType.CLAN_MESSAGE, "Klar: gz!!!").isPresent());
		final String drop = "Klar received a drop: Dragon claws (121,453,000 coins).";
		assertFalse(BroadcastLine.parse(ChatMessageType.PUBLICCHAT, drop).isPresent());
		assertFalse(BroadcastLine.parse(ChatMessageType.CLAN_CHAT, drop).isPresent());
		assertFalse(BroadcastLine.parse(ChatMessageType.GAMEMESSAGE, drop).isPresent());
	}
}
