package com.clanhub;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The first paragraph of a wiki page, for the cards to show under their rows: the Old School RuneScape Wiki is asked for
 * a page's introduction when a chat line with a card arrives, the answer is kept, and the card reads it from here when it
 * is hovered. What is sent is the page's title ("Goblin champion scroll") and nothing else, to
 * {@code oldschool.runescape.wiki}, through RuneLite's own HTTP client, with a User-Agent that names this plugin; nothing
 * about the player, the line or the world goes with it. The call is asked for in the background ({@code enqueue}, never
 * {@code execute}), so the client thread never waits on the wiki. A title is asked for once: a page that failed, or has
 * no text, is remembered as empty and not asked again. When the wiki says there is no page of that title, its own search
 * is asked once for the name (the name only, the first hit wanted) and the first hit's page is read instead, the
 * paragraph kept under the name the card asked by ("Jubbly Jive Shark" shows the paragraph of "Jubbly Jive Barracuda
 * Trial"); the hit's title goes only into the query of that second page call, never into a host or a path. The asking is
 * capped (10 titles at a time, 60 calls a minute, a search counting as a call), and the answers kept are the 500 asked
 * for last.
 *
 * <p>Choice: a failed call is never retried in the same run; one warning is logged the first time a call fails, and the
 * later ones go to the debug log, so a wiki that is down does not fill the log.
 */
final class WikiSummaries
{
	private static final Logger log = LoggerFactory.getLogger(WikiSummaries.class);

	/** The wiki's host; the only one this class talks to, and the one {@link WikiPictures} reads its pictures from. */
	static final String HOST = "oldschool.runescape.wiki";
	/** The User-Agent every call carries, naming the plugin and where its source is; {@link WikiPictures} sends it too. */
	static final String USER_AGENT = "2h Chat Hover Info (RuneLite plugin; github.com/2hBuilds/chat-hover-info)";
	/** How many answers are kept; the oldest go first. */
	private static final int MAX_CACHED = 500;
	/** How many titles may be waiting for an answer at once. */
	private static final int MAX_IN_FLIGHT = 10;
	/** How many calls may be started in one minute. */
	private static final int MAX_PER_MINUTE = 60;
	/** A minute, in {@link System#nanoTime} terms. */
	private static final long MINUTE_NANOS = 60_000_000_000L;

	private final Call.Factory calls;
	private final Gson gson;
	/** Guards everything below: the answers arrive on the HTTP client's thread and are read on the client thread. */
	private final Object lock = new Object();
	/** The paragraphs by title, "" for a page that failed or has none; at most {@link #MAX_CACHED}, the oldest out. */
	private final Map<String, String> cache = new LinkedHashMap<String, String>()
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> eldest)
		{
			return size() > MAX_CACHED;
		}
	};
	/** The titles whose chain of calls (page, search, nearest page) has not ended yet. */
	private final Set<String> inFlight = new HashSet<>();
	/** When the current minute began, in {@link System#nanoTime} terms, and how many calls it has started. */
	private long windowStart = System.nanoTime();
	private int startedInWindow;
	/** Whether a failed call has been logged as a warning yet. */
	private boolean warned;

	/**
	 * @param calls where calls are made: RuneLite's own HTTP client (an {@code okhttp3.Call.Factory}), or a stand-in in a test
	 * @param gson RuneLite's Gson, which reads the answer
	 */
	WikiSummaries(Call.Factory calls, Gson gson)
	{
		this.calls = calls;
		this.gson = gson;
	}

	/**
	 * The request for a page's introduction: the wiki's {@code api.php} asked for the page's plain-text extract up to its
	 * first section, redirects followed, as JSON; the title is percent-encoded (a space as %20). It carries this plugin's
	 * User-Agent and nothing about the player.
	 */
	static Request requestFor(String title)
	{
		final HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host(HOST)
			.addPathSegments("api.php")
			.addQueryParameter("action", "query")
			.addQueryParameter("prop", "extracts")
			.addQueryParameter("exintro", "1")
			.addQueryParameter("explaintext", "1")
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.addQueryParameter("titles", title)
			.build();
		return new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
	}

	/**
	 * The request for the wiki's own search of a name: {@code api.php} asked for the single best hit, as JSON; the name is
	 * percent-encoded (a space as %20). It carries this plugin's User-Agent and nothing about the player.
	 */
	static Request requestForSearch(String title)
	{
		final HttpUrl url = new HttpUrl.Builder()
			.scheme("https")
			.host(HOST)
			.addPathSegments("api.php")
			.addQueryParameter("action", "query")
			.addQueryParameter("list", "search")
			.addQueryParameter("srsearch", title)
			.addQueryParameter("srlimit", "1")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.build();
		return new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
	}

	/**
	 * The title of the first hit the wiki's search answer holds, or null when there is none. The answer is
	 * {@code {"query": {"search": [{"title": "...", ...}]}}}; no hit is {@code "search": []}. An answer with no search
	 * list, a hit with no title or a blank one, an empty answer, or text that is not JSON gives null.
	 */
	static String firstSearchTitle(String json, Gson gson)
	{
		final JsonObject query = objectOf(parse(json, gson), "query");
		final JsonElement hits = query == null ? null : query.get("search");
		if (hits == null || !hits.isJsonArray() || hits.getAsJsonArray().size() == 0
			|| !hits.getAsJsonArray().get(0).isJsonObject())
		{
			return null;
		}
		final JsonElement title = hits.getAsJsonArray().get(0).getAsJsonObject().get("title");
		if (title == null || !title.isJsonPrimitive() || title.getAsString().trim().isEmpty())
		{
			return null;
		}
		return title.getAsString().trim();
	}

	/**
	 * Whether the wiki's page answer says the page does not exist: its first page carries {@code "missing": true}. A page
	 * that exists but has no text, an empty answer, or text that is not JSON is not missing.
	 */
	static boolean isMissing(String json, Gson gson)
	{
		final JsonObject page = firstPage(json, gson);
		final JsonElement missing = page == null ? null : page.get("missing");
		return missing != null && missing.isJsonPrimitive() && missing.getAsJsonPrimitive().isBoolean()
			&& missing.getAsBoolean();
	}

	/**
	 * The first paragraph of the page the wiki's answer holds: the first page's {@code extract} up to its first line
	 * break, trimmed. The answer is {@code {"query": {"pages": [{"title": "...", "extract": "..."}]}}}; a page that does
	 * not exist has no extract. Anything else gives an empty text: a missing page, an answer with no pages or no extract,
	 * an empty answer, or text that is not JSON.
	 */
	static String firstParagraph(String json, Gson gson)
	{
		final JsonObject page = firstPage(json, gson);
		final JsonElement extract = page == null ? null : page.get("extract");
		if (extract == null || !extract.isJsonPrimitive())
		{
			return "";
		}
		final String text = extract.getAsString().trim();
		final int lineBreak = text.indexOf('\n');
		return (lineBreak < 0 ? text : text.substring(0, lineBreak)).trim();
	}

	/** The first page of a page answer, or null when the answer has none or is not JSON; {@link WikiPictures} reads it too. */
	static JsonObject firstPage(String json, Gson gson)
	{
		final JsonObject query = objectOf(parse(json, gson), "query");
		final JsonElement pages = query == null ? null : query.get("pages");
		if (pages == null || !pages.isJsonArray())
		{
			return null;
		}
		final JsonArray list = pages.getAsJsonArray();
		if (list.size() == 0 || !list.get(0).isJsonObject())
		{
			return null;
		}
		return list.get(0).getAsJsonObject();
	}

	/** The answer as an object, or null when it is empty, is not JSON, or is not an object. */
	private static JsonObject parse(String json, Gson gson)
	{
		try
		{
			return gson.fromJson(json, JsonObject.class);
		}
		catch (JsonParseException e)
		{
			return null;
		}
	}

	/** A member of an object that is itself an object, or null when it is missing or is something else. */
	private static JsonObject objectOf(JsonObject parent, String member)
	{
		if (parent == null)
		{
			return null;
		}
		final JsonElement child = parent.get(member);
		return child != null && child.isJsonObject() ? child.getAsJsonObject() : null;
	}

	/**
	 * The paragraph kept for a title: the text, an empty text for a page that failed or has none, or null when the title
	 * was never asked for or its answer has not come yet.
	 */
	String summaryOf(String title)
	{
		synchronized (lock)
		{
			return cache.get(title);
		}
	}

	/**
	 * Asks the wiki for a title's first paragraph in the background. Nothing happens when the title is blank, already kept,
	 * already waiting for its answer, when 10 titles are already waiting, or when 60 calls have been started in the last
	 * minute (the title is then asked for again by the next hover). The answer is kept when it arrives: the paragraph on a
	 * 200 answer, an empty text on anything else. A page the wiki says is missing leads to one search call and, on a hit
	 * that is another title, one more page call; that paragraph is kept under the title asked for here. Each of those
	 * calls counts in the minute; when the minute is full before one of them, the title is let go unkept and the next
	 * hover asks again.
	 */
	void fetch(String title)
	{
		if (title == null || title.trim().isEmpty())
		{
			return;
		}
		synchronized (lock)
		{
			if (cache.containsKey(title) || inFlight.contains(title) || inFlight.size() >= MAX_IN_FLIGHT
				|| !startCall())
			{
				return;
			}
			inFlight.add(title);
		}
		ask(title, requestFor(title), Step.PAGE);
	}

	/**
	 * Counts one call started in the minute, or says there is no room: false when 60 have started in the last minute.
	 * The caller holds {@link #lock}.
	 */
	private boolean startCall()
	{
		final long now = System.nanoTime();
		if (now - windowStart >= MINUTE_NANOS)
		{
			windowStart = now;
			startedInWindow = 0;
		}
		if (startedInWindow >= MAX_PER_MINUTE)
		{
			return false;
		}
		startedInWindow++;
		return true;
	}

	/** Makes one call of a title's chain in the background; its answer goes to {@link #answered}. */
	private void ask(String title, Request request, Step step)
	{
		try
		{
			calls.newCall(request).enqueue(new Callback()
			{
				@Override
				public void onFailure(Call call, IOException e)
				{
					failed(title, e.toString());
				}

				@Override
				public void onResponse(Call call, Response response)
				{
					answered(title, step, response);
				}
			});
		}
		catch (RuntimeException e)
		{
			failed(title, e.toString());
		}
	}

	/**
	 * Reads a 200 answer for the step it was asked in and keeps an empty text for any other answer; the answer is always
	 * closed. A page answer that says the page is missing goes on to the search; a search answer with a hit that is
	 * another title goes on to that page; any other answer ends the chain.
	 */
	private void answered(String title, Step step, Response response)
	{
		try (Response answer = response)
		{
			final ResponseBody body = answer.body();
			if (answer.code() != 200 || body == null)
			{
				failed(title, "HTTP " + answer.code());
				return;
			}
			final String json = body.string();
			if (step == Step.PAGE && isMissing(json, gson))
			{
				next(title, requestForSearch(title), Step.SEARCH);
			}
			else if (step == Step.SEARCH)
			{
				final String nearest = firstSearchTitle(json, gson);
				if (nearest == null || nearest.equals(title))
				{
					finish(title, "");
				}
				else
				{
					next(title, requestFor(nearest), Step.NEAREST);
				}
			}
			else
			{
				finish(title, firstParagraph(json, gson));
			}
		}
		catch (IOException | RuntimeException e)
		{
			failed(title, e.toString());
		}
	}

	/** Goes on to the next call of a title's chain, or lets the title go unkept when the minute has no room for it. */
	private void next(String title, Request request, Step step)
	{
		synchronized (lock)
		{
			if (!startCall())
			{
				inFlight.remove(title);
				return;
			}
		}
		ask(title, request, step);
	}

	/** Keeps an empty text for the title and says why: one warning the first time, debug lines after. */
	private void failed(String title, String why)
	{
		final boolean first;
		synchronized (lock)
		{
			first = !warned;
			warned = true;
		}
		if (first)
		{
			log.warn("The wiki's description for a card could not be read ({}); cards show without it", why);
		}
		else
		{
			log.debug("No wiki description for '{}': {}", title, why);
		}
		finish(title, "");
	}

	/** Keeps the paragraph for the title and takes it off the waiting list. */
	private void finish(String title, String paragraph)
	{
		synchronized (lock)
		{
			inFlight.remove(title);
			cache.put(title, paragraph);
		}
	}

	/** Which call of a title's chain an answer belongs to. */
	private enum Step
	{
		/** The title's own page. */
		PAGE,
		/** The wiki's search for the title, after its page was missing. */
		SEARCH,
		/** The page the search ranked first. */
		NEAREST
	}
}
