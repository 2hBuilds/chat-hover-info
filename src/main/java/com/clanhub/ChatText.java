package com.clanhub;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

/**
 * The chat text as the plugin reads it and as it changes it. Pure: strings in, strings out, no RuneLite state.
 * {@link #plain} is the one text every reader's offsets index and the card store keys on: the message without its tags,
 * without the {@code CA_ID:<n>|} a combat achievement line carries, which the client does not draw, with every
 * non-breaking space written as a plain one, and trimmed of the whitespace around the text (before the prefix, after it
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
	 * The message without its tags, without a leading {@code CA_ID:<digits>|} and the whitespace around it, with every
	 * non-breaking space (U+00A0) written as a plain space, and without trailing whitespace; an empty text for null.
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
		final String text = Text.removeTags(message).replace('\u00a0', ' ');
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
		final String tagless = Text.removeTags(rawMessage);
		final int[] rawAt = rawOffsets(rawMessage, tagless.length());
		final Matcher lead = LEAD.matcher(tagless.replace(' ', ' '));
		final int skipped = lead.find() ? lead.end() : 0;
		final int plainLength = tagless.length() - skipped;

		final List<int[]> ordered = new ArrayList<>(spans);
		Collections.sort(ordered, (a, b) -> Integer.compare(a[0], b[0]));
		final List<int[]> kept = new ArrayList<>();
		int lastEnd = 0;
		for (final int[] span : ordered)
		{
			if (span[0] >= lastEnd && span[1] > span[0] && span[1] <= plainLength)
			{
				kept.add(span);
				lastEnd = span[1];
			}
		}

		final String open = "<col=" + ColorUtil.colorToHexCode(colour).toLowerCase(Locale.ROOT) + ">";
		final StringBuilder out = new StringBuilder(rawMessage);
		for (int i = kept.size() - 1; i >= 0; i--)
		{
			final int[] span = kept.get(i);
			final int first = rawAt[skipped + span[0]];
			out.insert(rawAt[skipped + span[1] - 1] + 1, colourBefore(rawMessage, first));
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
	 * Where each tag-free character stands in the raw message: entry {@code k} is the raw index of the {@code k}th
	 * character that is not inside a tag; {@code taglessLength} entries.
	 */
	private static int[] rawOffsets(String raw, int taglessLength)
	{
		final int[] at = new int[taglessLength];
		final Matcher tags = TAG.matcher(raw);
		int next = 0;
		int i = 0;
		while (tags.find())
		{
			for (; i < tags.start(); i++)
			{
				at[next++] = i;
			}
			i = tags.end();
		}
		for (; i < raw.length(); i++)
		{
			at[next++] = i;
		}
		return at;
	}
}
