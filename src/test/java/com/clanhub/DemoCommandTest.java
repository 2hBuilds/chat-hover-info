package com.clanhub;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.CommandExecuted;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** {@link DemoCommand}: the sample lines are posted in developer mode for {@code ::hoverinfo demo} and nowhere else. */
public class DemoCommandTest
{
	private static CommandExecuted command(String name, String... arguments)
	{
		return new CommandExecuted(name, arguments);
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8M): the name is 2h Chat Hover Info and the command ::hoverinfo.
	@Test
	public void postsTheSampleLinesOnlyInDeveloperMode()
	{
		final Client off = Mockito.mock(Client.class);
		assertEquals("a Hub build does nothing", 0, DemoCommand.run(off, false, command("hoverinfo", "demo")));
		verifyNoInteractions(off);

		final Client on = Mockito.mock(Client.class);
		assertEquals(DemoLines.lines().size(), DemoCommand.run(on, true, command("hoverinfo", "demo")));
		assertTrue("the item samples are posted", DemoLines.lines().size() >= DemoLines.ITEMS.size());
		for (final DemoLines.Line line : DemoLines.lines())
		{
			// A clan or friends line is posted from "2h Chat Hover Info"; a public chat or game line has no channel name.
			final boolean typedOrGame = line.type == ChatMessageType.PUBLICCHAT || line.type == ChatMessageType.GAMEMESSAGE;
			verify(on).addChatMessage(line.type, line.name, line.text, typedOrGame ? "" : "2h Chat Hover Info", true);
		}

		final Client bare = Mockito.mock(Client.class);
		assertEquals(0, DemoCommand.run(bare, true, command("hoverinfo")));
		verifyNoInteractions(bare);

		final Client other = Mockito.mock(Client.class);
		assertEquals(0, DemoCommand.run(other, true, command("hoverinfo", "show")));
		verifyNoInteractions(other);
	}

	/** Lot 8Y: {@code ::hoverinfo trace} is read in developer mode only, in any case, and posts no sample line. */
	@Test
	public void theTraceWordIsReadOnlyInDeveloperMode()
	{
		assertTrue(DemoCommand.isTrace(true, command("hoverinfo", "trace")));
		assertTrue("any case", DemoCommand.isTrace(true, command("HoverInfo", "TRACE")));
		assertFalse("a Hub build", DemoCommand.isTrace(false, command("hoverinfo", "trace")));
		assertFalse("the demo word", DemoCommand.isTrace(true, command("hoverinfo", "demo")));
		assertFalse("no arguments", DemoCommand.isTrace(true, command("hoverinfo")));
		assertFalse("another command", DemoCommand.isTrace(true, command("other", "trace")));

		final Client client = Mockito.mock(Client.class);
		assertEquals("the trace word posts no sample line", 0, DemoCommand.run(client, true, command("hoverinfo", "trace")));
		verifyNoInteractions(client);
	}
}
