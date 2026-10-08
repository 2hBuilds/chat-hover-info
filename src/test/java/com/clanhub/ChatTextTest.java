package com.clanhub;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** {@link ChatText}: the plain text a card's offsets index, and the colour tags put around its words. */
public class ChatTextTest
{
	private static final Color BLUE = new Color(59, 130, 246);

	/** The spans as text, so a list of them can be compared: "[22, 34]". */
	private static List<String> shown(List<int[]> spans)
	{
		final List<String> shown = new ArrayList<>();
		for (final int[] span : spans)
		{
			shown.add(Arrays.toString(span));
		}
		return shown;
	}

	@Test
	public void plainStripsTagsAndTheAchievementPrefix()
	{
		assertEquals("Mossy Rock has completed a Master combat task: Perfect Zulrah (5 points).",
			ChatText.plain("<col=0000a0>CA_ID:279|Mossy Rock has completed a Master combat task: Perfect Zulrah "
				+ "(5 points).</col>"));
		assertEquals("Klar: gz", ChatText.plain("Klar: gz"));
		assertEquals("", ChatText.plain(null));
	}

	@Test
	public void plainTreatsTheTwoSpacesAsOne()
	{
		assertEquals("self ctrl received a drop: 154 x Dragon knife (618,310 coins).",
			ChatText.plain("self\u00a0ctrl received a drop: 154 x Dragon knife (618,310 coins)."));
		assertEquals("Klar: gz", ChatText.plain("Klar: gz"));
	}

	@Test
	public void plainTrimsAroundThePrefix()
	{
		final String task = "macemasterxx has completed an easy combat task: A Demon's Best Friend.";
		assertEquals(task, ChatText.plain("CA_ID:1234| " + task));
		assertEquals("Klar has completed a quest: Dragon Slayer II",
			ChatText.plain("  CA_ID:7|Klar has completed a quest: Dragon Slayer II  "));
		assertEquals("Klar: gz", ChatText.plain(" Klar: gz "));
		assertEquals("the drawn widget text of the probe is the same line", task,
			ChatText.plain("<col=000000>macemasterxx has completed an easy combat task: <col=3b82f6>A Demon's Best Friend"
				+ "<col=000000>.</col>"));
	}

	@Test
	public void coloursSpansAfterATrimmedLead()
	{
		assertEquals("CA_ID:1234| macemasterxx has completed an easy combat task: <col=3b82f6>A Demon's Best Friend<colNORMAL>.",
			ChatText.colourSpans("CA_ID:1234| macemasterxx has completed an easy combat task: A Demon's Best Friend.",
				Collections.singletonList(new int[] {48, 69}), BLUE));
		assertEquals("  Klar: selling <col=3b82f6>tbow<colNORMAL>",
			ChatText.colourSpans("  Klar: selling tbow", Collections.singletonList(new int[] {14, 18}), BLUE));
	}

	@Test
	public void coloursSpansInsideTheRawText()
	{
		// Re-aimed by the lead, 2026-10-08 (fix 1): a span closes with the colour that was open before it.
		assertEquals("<col=0000a0>Klar received a drop: <col=3b82f6>Dragon claws<col=0000a0> (1 coins).</col>",
			ChatText.colourSpans("<col=0000a0>Klar received a drop: Dragon claws (1 coins).</col>",
				Collections.singletonList(new int[] {22, 34}), BLUE));

		// Re-aimed by the lead, 2026-10-08 (lot 8): with no colour open, the close is RuneLite's <colNORMAL> marker, never </col>.
		assertEquals("selling <col=3b82f6>dragon claws<colNORMAL> and <col=3b82f6>tbow<colNORMAL>, 120m",
			ChatText.colourSpans("selling dragon claws and tbow, 120m",
				Arrays.asList(new int[] {8, 20}, new int[] {25, 29}), BLUE));

		// Re-aimed by the lead, 2026-10-08 (lot 8): with no colour open, the close is RuneLite's <colNORMAL> marker, never </col>.
		assertEquals("CA_ID:1|Klar has completed a quest: <col=3b82f6>Dragon Slayer II<colNORMAL>",
			ChatText.colourSpans("CA_ID:1|Klar has completed a quest: Dragon Slayer II",
				Collections.singletonList(new int[] {28, 44}), BLUE));

		assertEquals("no spans, the raw text back", "<col=0000a0>Klar: gz</col>",
			ChatText.colourSpans("<col=0000a0>Klar: gz</col>", Collections.<int[]>emptyList(), BLUE));
	}

	@Test
	public void spansOfTakesOnlyTheMarkedCards()
	{
		final String clawsLine = "Klar received a drop: Dragon claws and a Twisted bow.";
		final HoverCard claws = new ItemCard(13652, "Dragon claws", 1, 121_453_000L, 123_000, clawsLine, 22, 34);
		final HoverCard bow = new ItemCard(20997, "Twisted bow", 1, 1_000_000_000L, 1, clawsLine, 41, 52);
		final String levelLine = "Klar has reached Defence level 70.";
		final HoverCard level = new InfoCard("Defence level 70", Collections.singletonList(
			new HoverCard.Pair("Skill", "Defence", "")), Collections.<String>emptyList(), "Defence", levelLine, 16, 33, 199,
			-1, false);

		assertEquals("the quiet card is left out", Collections.singletonList("[22, 34]"),
			shown(ChatText.spansOf(Arrays.asList(level, claws))));
		assertEquals("two marked cards, in order", Arrays.asList("[22, 34]", "[41, 52]"),
			shown(ChatText.spansOf(Arrays.asList(claws, bow))));
		assertTrue("only quiet cards", ChatText.spansOf(Collections.singletonList(level)).isEmpty());
		assertTrue("no cards", ChatText.spansOf(Collections.<HoverCard>emptyList()).isEmpty());
	}

	// Re-aimed by the lead, 2026-10-08 (lot 8): with no colour open, the close is RuneLite's <colNORMAL> marker, never </col>.
	@Test
	public void closesASpanWithTheColourInForce()
	{
		assertEquals("a colour open before the span is the span's close",
			"<col=0000a0>Klar received a drop: <col=3b82f6>Dragon claws<col=0000a0> (1 coins).</col>",
			ChatText.colourSpans("<col=0000a0>Klar received a drop: Dragon claws (1 coins).</col>",
				Collections.singletonList(new int[] {22, 34}), BLUE));

		assertEquals("a colour closed before the span leaves the marker close",
			"<col=ff0000>Klar</col>: selling <col=3b82f6>tbow<colNORMAL>",
			ChatText.colourSpans("<col=ff0000>Klar</col>: selling tbow",
				Collections.singletonList(new int[] {14, 18}), BLUE));

		assertEquals("no colour at all gives the marker close", "selling <col=3b82f6>tbow<colNORMAL>",
			ChatText.colourSpans("selling tbow", Collections.singletonList(new int[] {8, 12}), BLUE));
	}
}
