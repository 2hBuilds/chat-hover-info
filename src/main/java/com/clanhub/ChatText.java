package com.clanhub;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

/**
 * The chat text as the plugin reads it and as it changes it. Pure: strings in, strings out, no RuneLite state.
 * {@link #plain} is the one text every reader's offsets index and the card store keys on: the message with the game's
 * string variables filled in ({@link #expand}), without its tags, without the {@code CA_ID:<n>|} a combat achievement
 * line carries, which the client does not draw, with every non-breaking space written as a plain one, and trimmed of the whitespace around the text (before the prefix, after it
 * and at the end). {@link #colourSpans} is the only change the plugin makes to a line:
 * colour tags put around words, nothing removed or rewritten; {@link #spansOf} gives it the words to colour, those of
 * the cards that are marked.
 */
final class ChatText
{
	/**
	 * What comes before a line's first drawn character: any whitespace, an optional "CA_ID:279|" (the id a combat
	 * achievement line starts with) and any whitespace after it.
	 */
	private static final Pattern LEAD = Pattern.compile("^\\s*(?:CA_ID:\\d+\\|)?\\s*");
	/** A tag, the way {@link Text#removeTags} sees one: a "<", anything but a ">", and the ">". */
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	/** The game's chat icon tag as a whole tag: {@code <img=N>}, group 1 the icon's number. */
	private static final Pattern IMAGE = Pattern.compile("<img=(\\d{1,9})>");
	/**
	 * One of the game's string variables, as a whole tag: {@code <str_NAME=VALUE>} defines one (group 1 the name, group 3
	 * the value, up to the first "&gt;") and {@code <str_NAME>} refers to one (group 2 is null).
	 */
	private static final Pattern VARIABLE = Pattern.compile("<str_([A-Za-z0-9_]+)(=([^>]*))?>");
	/**
	 * The close of a span when no colour stands open before it: "<col" + {@link ChatColorType#NORMAL} + ">", the marker
	 * RuneLite's chat manager replaces with the line's own colour (the line has to be stored with {@code
	 * MessageNode.setRuneLiteFormatMessage} for that), where {@code </col>} would read as the widget's base colour.
	 */
	private static final String NORMAL = "<col" + ChatColorType.NORMAL + ">";

	private ChatText()
	{
	}

	/**
	 * The message with the game's string variables filled in ({@link #expand}), without its tags, without a leading
	 * {@code CA_ID:<digits>|} and the whitespace around it, with every non-breaking space (U+00A0) written as a plain
	 * space, and without trailing whitespace; an empty text for null.
	 *
	 * <p>Choice: the prefix is taken off only when it stands first once the tags are gone (whitespace before it
	 * allowed), which is where the game puts it. The text is trimmed because the game's task line can carry a space after
	 * the prefix (or whitespace at an end) that the drawn widget text does not, and the card store finds a line by its
	 * text: the stored line and the drawn line have to be the same characters. The game writes a player's name with a non-breaking space (U+00A0, as in a name like "self ctrl") in one place
	 * and a plain space in another, and the card store finds a line by its text, so both are made the one plain space;
	 * each is one character, so the length and every offset stay true.
	 */
	static String plain(String message)
	{
		if (message == null)
		{
			return "";
		}
		final String text = Text.removeTags(expand(message)).replace('\u00a0', ' ');
		final Matcher lead = LEAD.matcher(text);
		final int from = lead.find() ? lead.end() : 0;
		int to = text.length();
		while (to > from && Character.isWhitespace(text.charAt(to - 1)))
		{
			to--;
		}
		return text.substring(from, to);
	}

	/**
	 * The width in pixels the game draws before the first character of {@link #plain}{@code (raw)}: what {@link #plain}
	 * drops from the front of the line. Each {@code <img=N>} tag before that character adds {@code iconWidth(N)}, the
	 * width of chat icon N; every other tag adds nothing; each space or non-breaking space the lead drops adds the
	 * {@code textWidth} of one space; the hidden {@code CA_ID:<digits>|} prefix, which the client does not draw, adds
	 * nothing. A null text, or one with nothing before its first character, gives 0.
	 *
	 * <p>Choice: the walk is over the expanded text ({@link #expand}), the text {@link #plain} reads, so a string
	 * variable's definition tag at the front adds nothing and the two agree on which character is the first. A tag
	 * after the first character adds nothing here, an icon in the middle of the line being inside the words' own
	 * widths.
	 */
	static int leadWidth(String raw, IntUnaryOperator iconWidth, ToIntFunction<String> textWidth)
	{
		if (raw == null)
		{
			return 0;
		}
		final String expanded = expand(raw);
		final String tagless = Text.removeTags(expanded).replace('\u00a0', ' ');
		final Matcher lead = LEAD.matcher(tagless);
		final int skipped = lead.find() ? lead.end() : 0;
		int width = 0;
		int consumed = 0;
		int from = 0;
		final Matcher tags = TAG.matcher(expanded);
		while (tags.find())
		{
			consumed += tags.start() - from;
			from = tags.end();
			if (consumed > skipped)
			{
				break;
			}
			final Matcher icon = IMAGE.matcher(tags.group());
			if (icon.matches())
			{
				width += iconWidth.applyAsInt(Integer.parseInt(icon.group(1)));
			}
		}
		int spaces = 0;
		for (int i = 0; i < skipped; i++)
		{
			if (tagless.charAt(i) == ' ')
			{
				spaces++;
			}
		}
		return spaces == 0 ? width : width + spaces * textWidth.applyAsInt(" ");
	}

	/**
	 * The raw message with the game's string variables resolved. Some broadcasts carry a name as a variable: a definition
	 * tag {@code <str_NAME=VALUE>} at the front of the line and a reference tag {@code <str_NAME>} where the name is
	 * drawn, as in {@code <str_quest_name_0=King's Ransom>Klar has completed a quest: <str_quest_name_0>}, which the
	 * client reads as "Klar has completed a quest: King's Ransom". Here each definition is removed and remembered (a
	 * later one of the same NAME replaces the earlier) and each reference becomes the value remembered for its NAME, or
	 * nothing when it was never defined. NAME is letters, digits and underscores; VALUE is everything up to the first
	 * "&gt;", so it may hold spaces, an apostrophe or an "=". Every other tag and every other character stays as it is,
	 * and a null message gives an empty text.
	 */
	static String expand(String raw)
	{
		if (raw == null)
		{
			return "";
		}
		final Map<String, String> values = new HashMap<>();
		final StringBuilder out = new StringBuilder(raw.length());
		final Matcher tags = TAG.matcher(raw);
		int from = 0;
		while (tags.find())
		{
			out.append(raw, from, tags.start());
			from = tags.end();
			final Matcher variable = VARIABLE.matcher(tags.group());
			if (!variable.matches())
			{
				out.append(tags.group());
			}
			else if (variable.group(2) != null)
			{
				values.put(variable.group(1), variable.group(3));
			}
			else
			{
				out.append(values.getOrDefault(variable.group(1), ""));
			}
		}
		return out.append(raw, from, raw.length()).toString();
	}

	/**
	 * The {@code [start, end)} spans of the cards whose {@link HoverCard#marked} is true, in the cards' order: the words
	 * the chat colours. A quiet card has no span here; no marked card gives an empty list.
	 */
	static List<int[]> spansOf(List<HoverCard> cards)
	{
		final List<int[]> spans = new ArrayList<>();
		for (final HoverCard card : cards)
		{
			if (card.marked())
			{
				spans.add(new int[] {card.start(), card.end()});
			}
		}
		return spans;
	}

	/**
	 * The raw message with {@code <col=rrggbb>} put before each span and a close tag after it, the spans being
	 * {@code [start, end)} offsets into {@code plain(rawMessage)}. The walk skips every tag and the lead
	 * of {@link #plain} (whitespace, a {@code CA_ID:<digits>|} and the whitespace after it) without counting them, and puts
	 * the open tag just before the span's first character and the close tag just after its last, so a tag the line already
	 * has stays where it was. Spans are applied from the last to
	 * the first, so the offsets of the earlier ones stay true. With no spans, the raw message comes back as it was.
	 *
	 * <p>The offsets index the expanded text ({@link #expand}), and the tags go into the raw message as the game wrote
	 * it, its string variables unexpanded: a span over a variable's words wraps the reference tag whole, opening just
	 * before {@code <str_NAME>} and closing just after its {@code >}, so the game fills the name in between the colours.
	 *
	 * <p>Choice: a span that is empty, runs outside the text, or starts inside the span before it is left out, so the
	 * tags never cross; the spans are taken in the order of their starts, whatever order they come in. A span is closed
	 * with the colour that was open before it, not with {@code </col>}: in the game a {@code </col>} sets the text after
	 * it to the line widget's base colour, which for a line a player typed is black, and a line the game sent in its own
	 * colour would lose it after the first span. That colour is the last {@code <col=...>} tag before the span when it
	 * stands after the last {@code </col>}, else RuneLite's {@code <colNORMAL>} marker, the way the Chat Notifications
	 * plugin closes a highlighted word; the chat manager fills the marker with the line's own colour when it builds the
	 * line (see {@link #NORMAL}).
	 */
	static String colourSpans(String rawMessage, List<int[]> spans, Color colour)
	{
		if (rawMessage == null || spans == null || spans.isEmpty())
		{
			return rawMessage;
		}
		final String tagless = Text.removeTags(expand(rawMessage));
		final int[][] rawAt = rawOffsets(rawMessage, tagless.length());
		final int[] opens = rawAt[0];
		final int[] lasts = rawAt[1];
		final Matcher lead = LEAD.matcher(tagless.replace(' ', ' '));
		final int skipped = lead.find() ? lead.end() : 0;
		final int plainLength = tagless.length() - skipped;

		final List<int[]> ordered = new ArrayList<>(spans);
		Collections.sort(ordered, (a, b) -> Integer.compare(a[0], b[0]));
		final List<int[]> kept = new ArrayList<>();
		int lastEnd = 0;
		int lastRaw = 0;
		for (final int[] span : ordered)
		{
			// A span that would open inside the tag the span before it closed after (two spans over one variable) is left out too.
			if (span[0] >= lastEnd && span[1] > span[0] && span[1] <= plainLength && opens[skipped + span[0]] >= lastRaw)
			{
				kept.add(span);
				lastEnd = span[1];
				lastRaw = lasts[skipped + span[1] - 1] + 1;
			}
		}

		final String open = "<col=" + ColorUtil.colorToHexCode(colour).toLowerCase(Locale.ROOT) + ">";
		final StringBuilder out = new StringBuilder(rawMessage);
		for (int i = kept.size() - 1; i >= 0; i--)
		{
			final int[] span = kept.get(i);
			final int first = opens[skipped + span[0]];
			out.insert(lasts[skipped + span[1] - 1] + 1, colourBefore(rawMessage, first));
			out.insert(first, open);
		}
		return out.toString();
	}

	/**
	 * What closes a span that starts at raw index {@code at}: the last {@code <col=...>} tag before it, whole, when that
	 * tag stands after the last {@code </col>} (the colour still open there), else the {@link #NORMAL} marker, never
	 * {@code </col>}.
	 */
	private static String colourBefore(String raw, int at)
	{
		final String before = raw.substring(0, at);
		final int opened = before.lastIndexOf("<col=");
		if (opened > before.lastIndexOf("</col>"))
		{
			final int closed = before.indexOf('>', opened);
			if (closed >= 0)
			{
				return before.substring(opened, closed + 1);
			}
		}
		return NORMAL;
	}

	/**
	 * Where each character of the expanded, tag-free text stands in the raw message, as two arrays of
	 * {@code taglessLength} entries: entry {@code k} of the first is the raw index a span that starts at the {@code k}th
	 * character opens at, entry {@code k} of the second the raw index of the last raw character of the {@code k}th
	 * (a span that ends there closes just after it). A character that is not inside a tag stands at its own index in both.
	 * A definition tag adds no character; a reference tag adds the characters of its value ({@link #expand}), all of
	 * them opening at the tag's first character and ending at its {@code >}, so a span that starts or ends inside the
	 * value takes the tag whole. Any other tag adds nothing.
	 */
	private static int[][] rawOffsets(String raw, int taglessLength)
	{
		final int[] opens = new int[taglessLength];
		final int[] lasts = new int[taglessLength];
		final Map<String, String> values = new HashMap<>();
		final Matcher tags = TAG.matcher(raw);
		int next = 0;
		int i = 0;
		while (tags.find())
		{
			for (; i < tags.start() && next < taglessLength; i++)
			{
				opens[next] = i;
				lasts[next++] = i;
			}
			i = tags.end();
			final Matcher variable = VARIABLE.matcher(tags.group());
			if (!variable.matches())
			{
				continue;
			}
			if (variable.group(2) != null)
			{
				values.put(variable.group(1), variable.group(3));
				continue;
			}
			final int length = values.getOrDefault(variable.group(1), "").length();
			for (int k = 0; k < length && next < taglessLength; k++)
			{
				opens[next] = tags.start();
				lasts[next++] = tags.end() - 1;
			}
		}
		for (; i < raw.length() && next < taglessLength; i++)
		{
			opens[next] = i;
			lasts[next++] = i;
		}
		return new int[][] {opens, lasts};
	}
}
