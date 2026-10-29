package com.clanhub;

import com.clanhub.TipCycle.Tip;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Lot 14A: the order the empty recent cards' tips come in. A shuffled bag, so every tip shows once before any shows again,
 * the same tip never twice in a row, and a tip whose feature is switched off never comes.
 */
public class TipCycleTest
{
	/** How many seeds each test tries: the order is random, so one lucky seed proves nothing. */
	private static final int SEEDS = 20;

	/** The first five draws, on a fresh cycle with everything on, are the five tips, each once. */
	@Test
	public void everyTipBeforeARepeat()
	{
		for (int seed = 0; seed < SEEDS; seed++)
		{
			final TipCycle cycle = new TipCycle(new Random(seed));
			final Set<Tip> seen = EnumSet.noneOf(Tip.class);
			for (int i = 0; i < Tip.values().length - 1; i++)
			{
				assertTrue("seed " + seed + ": a repeat before every tip showed", seen.add(cycle.next(true, true, false)));
			}
			assertEquals("seed " + seed + ": five different tips (blur only with a picture theme)",
				EnumSet.complementOf(EnumSet.of(Tip.BLUR)), seen);
			assertEquals("there are six tips (the sixth, blur, only with a picture theme)", 6, Tip.values().length);
		}
	}

	/** A long run: no tip is ever followed by itself, at the end of a bag and the start of the next included. */
	@Test
	public void neverTheSameTwiceInARow()
	{
		for (int seed = 0; seed < SEEDS; seed++)
		{
			final TipCycle cycle = new TipCycle(new Random(seed));
			Tip last = cycle.next(true, true, false);
			for (int i = 1; i < 1_000; i++)
			{
				final Tip tip = cycle.next(true, true, false);
				assertNotEquals("seed " + seed + ", draw " + i + ": the same tip twice", last, tip);
				last = tip;
			}
		}
	}

	/** Examine cards off drops the examine tip; Boss cards off drops the boss tip; both off leave the other three. */
	@Test
	public void switchedOffTipsAreSkipped()
	{
		for (int seed = 0; seed < SEEDS; seed++)
		{
			final List<Tip> noExamine = draws(new TipCycle(new Random(seed)), false, true, 200);
			assertFalse("seed " + seed + ": examine tip with examine off", noExamine.contains(Tip.EXAMINE));
			assertTrue("seed " + seed + ": the boss tip still comes", noExamine.contains(Tip.BOSS));

			final List<Tip> noBoss = draws(new TipCycle(new Random(seed)), true, false, 200);
			assertFalse("seed " + seed + ": boss tip with boss cards off", noBoss.contains(Tip.BOSS));
			assertTrue("seed " + seed + ": the examine tip still comes", noBoss.contains(Tip.EXAMINE));

			final List<Tip> neither = draws(new TipCycle(new Random(seed)), false, false, 200);
			assertEquals("seed " + seed + ": only the other three", EnumSet.of(Tip.THEME, Tip.WIKI, Tip.NAME),
				EnumSet.copyOf(neither));
		}
	}

	/** A switch turned off in the middle of a bag takes its tip out of the draws from then on, and never repeats one in a row. */
	@Test
	public void aSwitchTurnedOffMidwayIsSkippedAtOnce()
	{
		for (int seed = 0; seed < SEEDS; seed++)
		{
			final TipCycle cycle = new TipCycle(new Random(seed));
			cycle.next(true, true, false);
			Tip last = null;
			for (int i = 0; i < 200; i++)
			{
				final Tip tip = cycle.next(false, false, false);
				assertNotEquals("seed " + seed + ": examine tip after the switch went off", Tip.EXAMINE, tip);
				assertNotEquals("seed " + seed + ": boss tip after the switch went off", Tip.BOSS, tip);
				if (last != null)
				{
					assertNotEquals("seed " + seed + ": the same tip twice", last, tip);
				}
				last = tip;
			}
		}
	}

	private static List<Tip> draws(TipCycle cycle, boolean examineOn, boolean bossOn, int count)
	{
		final List<Tip> tips = new ArrayList<>();
		for (int i = 0; i < count; i++)
		{
			tips.add(cycle.next(examineOn, bossOn, false));
		}
		return tips;
	}

	/** 1.1.0: the blur tip comes only while a picture theme is on, and then within one round of the tips. */
	@Test
	public void theBlurTipShowsOnlyWithAPictureTheme()
	{
		final TipCycle off = new TipCycle(new Random(7));
		for (int i = 0; i < 60; i++)
		{
			assertTrue("never with an Original theme", off.next(true, true, false) != Tip.BLUR);
		}
		final TipCycle on = new TipCycle(new Random(7));
		boolean seen = false;
		for (int i = 0; i < Tip.values().length; i++)
		{
			seen |= on.next(true, true, true) == Tip.BLUR;
		}
		assertTrue("with a picture theme, once in a round", seen);
	}
}
