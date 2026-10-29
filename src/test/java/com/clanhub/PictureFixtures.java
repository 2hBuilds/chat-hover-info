package com.clanhub;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Small picture themes for the tests: a theme is made by hand (its fields are plain), and its pictures are drawn on demand
 * by a loader, so no real art is needed. A loader counts its calls, and a path in its {@code missing} set throws.
 */
final class PictureFixtures
{
	/** The width the fixture pictures are drawn at: the Medium card's. */
	static final int WIDTH = 409;

	private PictureFixtures()
	{
	}

	/** A theme with the given treatment, heights 150, 200 and 250, a red edge and light-ink roles. */
	static PictureTheme theme(String id, String group, String tier, boolean dark, double outline, double panel, boolean focal)
	{
		final PictureTheme theme = new PictureTheme();
		theme.id = id;
		theme.name = id.substring(0, 1).toUpperCase() + id.substring(1);
		theme.group = group;
		theme.tier = tier;
		theme.heights = new int[] {150, 200, 250};
		theme.edge = "#FF0000";
		theme.ink = dark ? "dark" : "light";
		theme.outline = outline;
		theme.panel = panel;
		theme.focalTitle = focal;
		theme.roles = new HashMap<>();
		theme.roles.put("title", dark ? "#202020" : "#FFE890");
		theme.roles.put("label", dark ? "#303030" : "#C0C8D8");
		theme.roles.put("value", dark ? "#282828" : "#FFFFFF");
		theme.roles.put("text", dark ? "#181818" : "#F8F8F8");
		theme.roles.put("hint", dark ? "#404040" : "#B0B8C8");
		theme.roles.put("rule", "#808080");
		theme.sidebar = new String[] {"#203050", "#101828"};
		return theme;
	}

	/** A catalogue of {@code themes} whose pictures are {@code fill} everywhere, drawn by {@code loader}. */
	static PictureThemes catalogue(Function<String, BufferedImage> loader, PictureTheme... themes)
	{
		return new PictureThemes(new ArrayList<>(Arrays.asList(themes)), loader);
	}

	/** A loader that draws a plain {@code fill} picture of the height in the path's name, and counts its calls in {@code calls}. */
	static Function<String, BufferedImage> loader(Color fill, Map<String, Integer> calls, List<String> missing)
	{
		return path ->
		{
			calls.merge(path, 1, Integer::sum);
			if (missing.contains(path))
			{
				throw new IllegalArgumentException("no such picture: " + path);
			}
			final int height = Integer.parseInt(path.substring(path.lastIndexOf('-') + 1, path.lastIndexOf('.')));
			return plain(fill, WIDTH, height);
		};
	}

	/** A plain picture of one colour. */
	static BufferedImage plain(Color fill, int width, int height)
	{
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			g.setColor(fill);
			g.fillRect(0, 0, width, height);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/** The path of a theme's picture at a height, as {@link PictureThemes} asks for it. */
	static String path(String id, int height)
	{
		return "/com/clanhub/themes/" + id + "-" + height + ".jpg";
	}
}
