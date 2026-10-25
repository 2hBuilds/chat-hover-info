package com.clanhub;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** {@link ChatRows}: where the game breaks a wrapped line, the row a word is on, and a card's words row by row. */
public class ChatRowsTest
{
	/** Ten pixels a character. */
	private static final ToIntFunction<String> TEN = s -> s.length() * 10;

	/** A card whose words stand at {@code start} to {@code end} of its line. */
	private static ItemCard spanning(int start, int end)
	{
		return new ItemCard(1, "Item", 1, 1L, 1, "a line", start, end);
	}

	@Test
	public void breaksAtSpacesLikeTheGame()
	{
		final ChatRows two = ChatRows.of("aaaa bbbb cccc dddd", TEN, new Rectangle(0, 400, 100, 28));
		assertEquals(2, two.rows());
		assertEquals(0, two.start(0));
		assertEquals(9, two.end(0));
		assertEquals(10, two.start(1));
		assertEquals(19, two.end(1));
		assertEquals(0, two.rowOf(0));
		assertEquals("the break space is on the row before", 0, two.rowOf(9));
		assertEquals(1, two.rowOf(10));
		assertEquals(1, two.rowOf(18));
		assertEquals("an index past the text is on the last row", 1, two.rowOf(19));
		assertEquals(50, two.x(5));
		assertEquals(0, two.x(10));
		assertEquals(50, two.x(15));
		assertEquals(0, two.rowAt(405));
		assertEquals(1, two.rowAt(420));
		assertEquals("above the widget is the first row", 0, two.rowAt(399));
		assertEquals("below it is the last", 1, two.rowAt(500));
		assertEquals(414, two.underlineY(0));
		assertEquals(428, two.underlineY(1));

		final ChatRows one = ChatRows.of("aaaa", TEN, new Rectangle(0, 400, 100, 14));
		assertEquals(1, one.rows());
		assertEquals(414, one.underlineY(0));
		assertEquals(0, one.rowAt(407));
		assertEquals(40, one.x(4));

		final ChatRows empty = ChatRows.of("", TEN, new Rectangle(0, 400, 100, 14));
		assertEquals(1, empty.rows());
		assertEquals(0, empty.start(0));
		assertEquals(0, empty.end(0));

		final ChatRows wide = ChatRows.of("aaaaaaaaaaaa bb", TEN, new Rectangle(0, 400, 100, 28));
		assertEquals("a word wider than the row stands alone, uncut", 2, wide.rows());
		assertEquals(12, wide.end(0));
		assertEquals(13, wide.start(1));
		assertEquals(15, wide.end(1));
	}

	@Test
	public void picksTheCardOnTheMousesRowAndSpansEachRow()
	{
		final ChatRows two = ChatRows.of("aaaa bbbb cccc dddd", TEN, new Rectangle(0, 400, 100, 28));
		final HoverCard a = spanning(5, 9);
		final HoverCard b = spanning(15, 19);
		final List<HoverCard> both = Arrays.asList(a, b);
		assertSame(a, two.pick(both, 0, 60, 405));
		assertSame(b, two.pick(both, 0, 60, 420));
		assertNull("right of the words on the first row", two.pick(both, 0, 95, 405));
		assertNull("left of the words on the second row", two.pick(both, 0, 20, 420));
		assertNull("left of the words on the first row", two.pick(both, 0, 0, 405));

		final List<HoverCard> across = Collections.<HoverCard>singletonList(spanning(5, 14));
		final HoverCard c = across.get(0);
		assertSame("the first row's piece", c, two.pick(across, 0, 60, 405));
		assertSame("the second row's piece", c, two.pick(across, 0, 20, 420));
		assertNull("under the first row's piece, on the second row", two.pick(across, 0, 60, 420));

		assertEquals(1, two.spans(5, 9).size());
		assertArrayEquals(new int[] {50, 90, 414}, two.spans(5, 9).get(0));
		final List<int[]> broken = two.spans(5, 14);
		assertEquals(2, broken.size());
		assertArrayEquals(new int[] {50, 90, 414}, broken.get(0));
		assertArrayEquals(new int[] {0, 40, 428}, broken.get(1));
		assertTrue("a break space alone has no piece", two.spans(9, 10).isEmpty());

		final ChatRows three = ChatRows.of("xx aaaa bbbb cccc dddd", TEN, new Rectangle(0, 400, 100, 42));
		assertEquals(3, three.rows());
		assertSame(a, three.pick(both, 3, 20, 420));
		assertSame(b, three.pick(both, 3, 20, 435));
		assertNull(three.pick(both, 3, 20, 405));
		final List<int[]> third = three.spans(8, 12);
		assertEquals(1, third.size());
		assertArrayEquals(new int[] {0, 40, 428}, third.get(0));
	}

	@Test
	public void theFirstRowStartsAfterTheIndent()
	{
		// Six pixels a character; the widget's left edge is 100 and it wraps at 120 px.
		final ToIntFunction<String> six = t -> t.length() * 6;
		final Rectangle bounds = new Rectangle(100, 400, 120, 28);

		assertEquals("the indent moves the first row", 100 + 14 + 18, ChatRows.of("ab cd", six, bounds, 14).x(3));
		assertEquals("no indent is the old reading", 100 + 18, ChatRows.of("ab cd", six, bounds, 0).x(3));
		assertEquals("the three-argument form means no indent", 100 + 18, ChatRows.of("ab cd", six, bounds).x(3));

		final String eighteen = "aaaa bbbb cccc ddd";
		assertEquals("18 characters are 108 px, one row at indent 0", 1, ChatRows.of(eighteen, six, bounds, 0).rows());
		final ChatRows indented = ChatRows.of(eighteen, six, bounds, 14);
		assertEquals("and two rows at indent 14, the first row having less room", 2, indented.rows());
		assertEquals(14, indented.end(0));
		assertEquals(15, indented.start(1));
		assertEquals("an index on the second row has no indent", 100 + 18, indented.x(15 + 3));
		assertEquals("the first row's end is the indent further right", 100 + 14 + 84, indented.x(14));
	}
}
