package com.clanhub;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The picture themes: the catalogue the bundled {@code themes.json} lists, and the pictures that go with it. The pictures
 * are finished card backgrounds, kept as resources, loaded when a card first wears one, decoded a handful at a time and
 * stretched to the card's exact size; the card's own words are laid over them by {@link PictureArt}.
 *
 * <p>Choice: one catalogue is installed for the whole plugin ({@link #install}, from {@code startUp}), so the painter, the
 * overlay and the menu all read the same list without each being handed it; the caches live in the catalogue, so a new one
 * starts with none. A picture that is missing or does not decode makes the whole theme unusable, once, with one warning,
 * and the card wears Classic instead; nothing is thrown.
 */
final class PictureThemes
{
	private static final Logger log = LoggerFactory.getLogger(PictureThemes.class);

	/** The groups, in the order the menu lists them. */
	static final List<String> GROUPS = Arrays.asList("Calm", "Tranquility", "Cosy", "Desolate", "Elegance", "Primal",
		"Mystic", "Wonder", "Epic", "Triumph", "Holy", "Dread", "Fury", "Infernal", "Seasons", "Garden", "Mountains",
		"Blossoms");
	/** The tiers, in the order a group lists its themes. */
	private static final List<String> TIERS = Arrays.asList("bright", "in-between", "dark");
	private static final String DIR = "/com/clanhub/themes/";
	/** How many decoded pictures and how many finished card backgrounds are kept. */
	private static final int KEEP_PICTURES = 4;
	private static final int KEEP_GROUNDS = 8;

	private static volatile PictureThemes current = new PictureThemes(new ArrayList<PictureTheme>(), null);

	/** The player's Blur behind text setting as a share, 0 (none) to 1 (the full band); 0 unless set. */
	private static volatile double frost = 0;

	private final Map<String, PictureTheme> byId = new HashMap<>();
	private final List<PictureTheme> all;
	private final Function<String, BufferedImage> loader;
	private final Map<String, Boolean> usable = new HashMap<>();
	private final Map<String, BufferedImage> pictures = new LinkedHashMap<String, BufferedImage>()
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest)
		{
			return size() > KEEP_PICTURES;
		}
	};
	private final Map<String, BufferedImage> grounds = new LinkedHashMap<String, BufferedImage>()
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest)
		{
			return size() > KEEP_GROUNDS;
		}
	};

	/**
	 * @param all the themes
	 * @param loader reads the picture at a resource path, or throws; null reads the plugin's own resources
	 */
	PictureThemes(List<PictureTheme> all, Function<String, BufferedImage> loader)
	{
		this.all = all;
		this.loader = loader == null ? path -> ImageUtil.loadImageResource(PictureThemes.class, path) : loader;
		for (final PictureTheme theme : all)
		{
			byId.put(theme.id, theme);
		}
	}

	/** The catalogue in use: empty until {@link #install} is called. */
	static PictureThemes current()
	{
		return current;
	}

	/** Makes {@code themes} the catalogue in use. */
	static void install(PictureThemes themes)
	{
		current = themes;
	}

	/** Sets the Blur behind text setting, in percent; backgrounds already made are keyed by it, so the next card shows it. */
	static void frostOpacity(int percent)
	{
		frost = Math.max(0, Math.min(100, percent)) / 100.0;
	}

	/** Reads the bundled catalogue; a missing or unreadable one logs a warning and gives an empty one. */
	static PictureThemes bundled(Gson gson)
	{
		try (InputStream in = PictureThemes.class.getResourceAsStream(DIR + "themes.json"))
		{
			if (in != null)
			{
				return parse(new InputStreamReader(in, StandardCharsets.UTF_8), gson, null);
			}
			log.warn("The picture themes are not in the plugin; the card themes are the original ones");
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("The picture themes could not be read ({}); the card themes are the original ones", e.toString());
		}
		return new PictureThemes(new ArrayList<PictureTheme>(), null);
	}

	/**
	 * Reads a catalogue: a JSON array of themes. An entry with no id, name, group or heights is skipped, and an id met twice
	 * keeps its first entry.
	 *
	 * @throws com.google.gson.JsonParseException when the text is not JSON
	 */
	static PictureThemes parse(Reader json, Gson gson, Function<String, BufferedImage> loader)
	{
		final List<PictureTheme> kept = new ArrayList<>();
		final Map<String, Boolean> seen = new HashMap<>();
		final PictureTheme[] read = gson.fromJson(json, PictureTheme[].class);
		for (final PictureTheme theme : read == null ? new PictureTheme[0] : read)
		{
			if (theme != null && theme.id != null && theme.name != null && theme.group != null && theme.heights != null
				&& theme.heights.length > 0 && seen.put(theme.id, true) == null)
			{
				kept.add(theme);
			}
		}
		return new PictureThemes(kept, loader);
	}

	/** The theme with this id, or null when there is none (or no id). */
	PictureTheme byId(String id)
	{
		return id == null ? null : byId.get(id);
	}

	/** The groups that have a theme, in the menu's order; a group the list does not know of comes after, in the order met. */
	List<String> groups()
	{
		final List<String> groups = new ArrayList<>();
		for (final String known : GROUPS)
		{
			if (inGroup(known).size() > 0)
			{
				groups.add(known);
			}
		}
		for (final PictureTheme theme : all)
		{
			if (!groups.contains(theme.group))
			{
				groups.add(theme.group);
			}
		}
		return groups;
	}

	/** The themes of a group: the bright ones first, then the in-between, then the dark, each by name. */
	List<PictureTheme> inGroup(String group)
	{
		final List<PictureTheme> themes = new ArrayList<>();
		for (final PictureTheme theme : all)
		{
			if (group.equals(theme.group))
			{
				themes.add(theme);
			}
		}
		Collections.sort(themes, (a, b) ->
		{
			final int tier = Integer.compare(TIERS.indexOf(a.tier), TIERS.indexOf(b.tier));
			return tier != 0 ? tier : a.name.compareToIgnoreCase(b.name);
		});
		return themes;
	}

	/**
	 * Whether the theme can be worn: every one of its pictures loads. Worked out once per theme; a theme that fails logs one
	 * warning and is never tried again.
	 */
	synchronized boolean usable(PictureTheme theme)
	{
		Boolean known = usable.get(theme.id);
		if (known == null)
		{
			known = true;
			for (final int height : theme.heights)
			{
				if (picture(theme, height) == null)
				{
					known = false;
					log.warn("The picture theme {} has no usable picture at {} px; its cards wear Classic", theme.id, height);
					break;
				}
			}
			usable.put(theme.id, known);
		}
		return known;
	}

	/**
	 * The card background of {@code theme} for a card {@code width} by {@code height} px with these words: the nearest
	 * picture stretched to the card, the theme's treatment under the words, the crisp edge ring. Kept for the last few
	 * cards, so a card costs the treatment once. Null when the picture cannot be had.
	 *
	 * @param lines the boxes of the strings, in px of the card
	 * @param title the title's box, or null
	 */
	synchronized BufferedImage ground(PictureTheme theme, int width, int height, List<int[]> lines, int[] title)
	{
		final StringBuilder key = new StringBuilder(theme.id).append('|').append(width).append('x').append(height);
		for (final int[] box : lines)
		{
			key.append('|').append(Arrays.toString(box));
		}
		final double share = frost;
		key.append('|').append(title == null ? "" : Arrays.toString(title)).append('|').append(share);
		BufferedImage done = grounds.get(key.toString());
		if (done == null)
		{
			final BufferedImage art = picture(theme, theme.nearestHeight(height));
			if (art == null)
			{
				return null;
			}
			done = PictureArt.stretch(art, width, height);
			PictureArt.treat(done, theme, lines, title, share);
			PictureArt.ring(done, theme.edgeColour());
			grounds.put(key.toString(), done);
		}
		return done;
	}

	/** The decoded picture at one of the theme's heights, kept for a while; null when it cannot be read. */
	private BufferedImage picture(PictureTheme theme, int height)
	{
		final String path = DIR + theme.id + "-" + height + ".jpg";
		BufferedImage art = pictures.get(path);
		if (art == null)
		{
			try
			{
				art = loader.apply(path);
			}
			catch (RuntimeException e)
			{
				art = null;
			}
			if (art != null)
			{
				pictures.put(path, art);
			}
		}
		return art;
	}
}
