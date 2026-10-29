package com.clanhub;

import java.awt.Color;
import java.util.Map;

/**
 * One picture theme, as {@code themes.json} lists it: a finished card background (three pictures, one per height) and the
 * numbers that say how the card's words sit on it. Read by Gson, so its fields are plain and a missing one stays null or 0;
 * every reader here copes with that.
 */
final class PictureTheme
{
	/** The theme's file name stem, its name, its group (the mood), and its tier: "bright", "in-between" or "dark". */
	String id;
	String name;
	String group;
	String tier;
	/** The heights the pictures were drawn at, in px, at the Medium width. */
	int[] heights;
	/** The one pixel ring round the card, as "#RRGGBB". */
	String edge;
	/** "light" or "dark": the ink of the words. */
	String ink;
	/** How strong the outline round the words is, 0 to 1. */
	double outline;
	/** How strong the frosted panel behind each line is, 0 to 1. */
	double panel;
	/** Whether the picture's light is held down under the title. */
	boolean focalTitle;
	/** The colours of the words, by role: title, label, value, text, hint and rule, as "#RRGGBB". */
	Map<String, String> roles;
	/** The sidebar's two colours, top and bottom, as "#RRGGBB". */
	String[] sidebar;

	/** The words' ink is dark: dark words over a pale halo, instead of light words over black. */
	boolean darkInk()
	{
		return "dark".equals(ink);
	}

	/** The colour of a role of the words, or {@code fallback} when the theme has none. */
	Color role(String role, Color fallback)
	{
		return colour(roles == null ? null : roles.get(role), fallback);
	}

	/** The sidebar's colour at {@code index} (0 top, 1 bottom), or {@code fallback}. */
	Color sidebarColour(int index, Color fallback)
	{
		return colour(sidebar != null && sidebar.length > index ? sidebar[index] : null, fallback);
	}

	/** The ring colour of the card's edge. */
	Color edgeColour()
	{
		return colour(edge, Color.BLACK);
	}

	/** The picture height nearest {@code height}, or -1 when the theme lists none. */
	int nearestHeight(int height)
	{
		int best = -1;
		for (final int candidate : heights == null ? new int[0] : heights)
		{
			if (best < 0 || Math.abs(candidate - height) < Math.abs(best - height))
			{
				best = candidate;
			}
		}
		return best;
	}

	private static Color colour(String hex, Color fallback)
	{
		try
		{
			return hex == null ? fallback : Color.decode(hex);
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}
