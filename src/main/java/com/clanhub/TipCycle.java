package com.clanhub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The order the sidebar's empty recent cards show their tips in. A bag of the tips that are on, shuffled: each
 * {@link #next} takes one out, and when the bag is empty it is filled and shuffled again, so that every tip shows once
 * before any shows a second time. The tip taken first from a new bag is never the one shown last, so no tip is ever
 * shown twice in a row. A tip whose feature is switched off is left out of the bag, and one that went off after the bag
 * was filled is passed over when its turn comes.
 *
 * <p>Choice: the cycle hands out which tip, not its words. The words depend on settings of their own (the fourth tip
 * says "coloured" only while the names are coloured), and the panel, which reads the settings, writes them.
 *
 * <p>Choice: the cycle is asked about the two switches on every draw rather than told once, so a player who turns
 * "Examine cards" off sees no examine tip from the very next draw, with no listener to keep in step.
 */
final class TipCycle
{
	/** The tips, in the order the panel's words are written: the theme, the examine cards, the wiki, a name, a boss. */
	enum Tip
	{
		THEME,
		EXAMINE,
		WIKI,
		NAME,
		BOSS,
		/** 1.1.0: Blur behind text; shown only while a picture theme is on (an Original theme has no picture to blur). */
		BLUR
	}

	private final Random random;
	/** The tips still to come from this bag; the next one is the last of the list. */
	private final List<Tip> bag = new ArrayList<>();
	/** The tip shown last, or null before the first. */
	private Tip last;

	/** @param random where the shuffles come from; the panel gives it a new one */
	TipCycle(Random random)
	{
		this.random = random;
	}

	/**
	 * The tip to show now.
	 *
	 * @param examineOn whether "Examine cards" is on; if it is off the examine tip is not given
	 * @param pictureOn whether a picture theme is on (the blur tip shows only then)
	 * @param bossOn whether "Boss cards" is on; if it is off the boss tip is not given
	 */
	Tip next(boolean examineOn, boolean bossOn, boolean pictureOn)
	{
		Tip tip;
		do
		{
			if (bag.isEmpty())
			{
				refill(examineOn, bossOn, pictureOn);
			}
			tip = bag.remove(bag.size() - 1);
		}
		while (!isOn(tip, examineOn, bossOn, pictureOn));
		last = tip;
		return tip;
	}

	/** Fills the bag with the tips that are on and shuffles it, with a tip other than {@link #last} on top of it. */
	private void refill(boolean examineOn, boolean bossOn, boolean pictureOn)
	{
		for (final Tip tip : Tip.values())
		{
			if (isOn(tip, examineOn, bossOn, pictureOn))
			{
				bag.add(tip);
			}
		}
		Collections.shuffle(bag, random);
		final int top = bag.size() - 1;
		if (top > 0 && bag.get(top) == last)
		{
			Collections.swap(bag, top, random.nextInt(top));
		}
	}

	/** Whether the feature {@code tip} speaks of is on. */
	private static boolean isOn(Tip tip, boolean examineOn, boolean bossOn, boolean pictureOn)
	{
		switch (tip)
		{
			case EXAMINE:
				return examineOn;
			case BOSS:
				return bossOn;
			case BLUR:
				return pictureOn;
			default:
				return true;
		}
	}
}
