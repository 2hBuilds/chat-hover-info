package com.clanhub;

import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link WikiSummaries}: the wiki's answer read, the request made, the answers kept and asked for once. */
public class WikiSummariesTest
{
	private static final String SENTENCES = "The goblin champion scroll is an extremely rare drop from goblins. It contains "
		+ "a challenge from the Goblin Champion. In order to be eligible to receive the scroll, players must have at least "
		+ "32 quest points and be on a members world.";

	/** The wiki's answer for the goblin champion scroll: the extract holds the paragraph, a line break, and more. */
	private static final String ANSWER = "{\"batchcomplete\": true, \"query\": {\"pages\": [{\"pageid\": 1, \"ns\": 0, "
		+ "\"title\": \"Goblin champion scroll\", \"extract\": \"" + SENTENCES + "\\nBy bringing this scroll to Larxus ...\"}]}}";

	private final Gson gson = new Gson();

	/** A 200 answer holding {@code json}. */
	private static Response ok(String json)
	{
		return new Response.Builder()
			.request(WikiSummaries.requestFor("Goblin champion scroll"))
			.protocol(Protocol.HTTP_1_1)
			.code(200)
			.message("OK")
			.body(ResponseBody.create(MediaType.parse("application/json"), json))
			.build();
	}

	@Test
	public void theFirstParagraphOfAnExtract()
	{
		assertEquals(SENTENCES, WikiSummaries.firstParagraph(ANSWER, gson));
		assertEquals("a page that does not exist", "", WikiSummaries.firstParagraph(
			"{\"query\": {\"pages\": [{\"ns\": 0, \"title\": \"Nonexistent\", \"missing\": true}]}}", gson));
		assertEquals("an empty answer", "", WikiSummaries.firstParagraph("{}", gson));
		assertEquals("a page with no extract", "", WikiSummaries.firstParagraph(
			"{\"query\": {\"pages\": [{\"pageid\": 1, \"ns\": 0, \"title\": \"Goblin champion scroll\"}]}}", gson));
		assertEquals("text that is not JSON", "", WikiSummaries.firstParagraph("<html>no</html>", gson));
		assertEquals("nothing at all", "", WikiSummaries.firstParagraph("", gson));
	}

	@Test
	public void theRequestNamesThePluginAndThePage()
	{
		final Request request = WikiSummaries.requestFor("Goblin champion scroll");
		assertEquals("https://oldschool.runescape.wiki/api.php?action=query&prop=extracts&exintro=1&explaintext=1"
			+ "&redirects=1&format=json&formatversion=2&titles=Goblin%20champion%20scroll", request.url().toString());
		assertTrue("the User-Agent names the plugin: " + request.header("User-Agent"),
			request.header("User-Agent").contains("2h Chat Hover Info"));
		// Re-aimed by the lead, 2026-10-08 (lot 8M): the name is 2h Chat Hover Info and the command ::hoverinfo.
		assertTrue("the User-Agent names the repository: " + request.header("User-Agent"),
			request.header("User-Agent").contains("chat-hover-info"));
		assertTrue("a question mark is encoded", WikiSummaries.requestFor("Why Cook?").url().toString()
			.endsWith("titles=Why%20Cook%3F"));
	}

	@Test
	public void aFetchIsRememberedAndNotRepeated() throws Exception
	{
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiSummaries summaries = new WikiSummaries(factory, gson);

		summaries.fetch("Goblin champion scroll");
		verify(factory, times(1)).newCall(any(Request.class));
		assertNull("not answered yet", summaries.summaryOf("Goblin champion scroll"));
		assertNull("never asked for", summaries.summaryOf("Dragon claws"));

		summaries.fetch("Goblin champion scroll");
		verify(factory, times(1)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> callback = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(callback.capture());
		callback.getValue().onResponse(call, ok(ANSWER));
		assertEquals(SENTENCES, summaries.summaryOf("Goblin champion scroll"));

		summaries.fetch("Goblin champion scroll");
		verify(factory, times(1)).newCall(any(Request.class));

		summaries.fetch("Nonexistent");
		verify(factory, times(2)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> second = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(second.capture());
		second.getValue().onFailure(call, new IOException("no route to the wiki"));
		assertEquals("a failed page is kept as empty", "", summaries.summaryOf("Nonexistent"));
		summaries.fetch("Nonexistent");
		verify(factory, times(2)).newCall(any(Request.class));
	}

	/** The wiki's answer for a title that has no page. */
	private static String missingPage(String title)
	{
		return "{\"batchcomplete\": true, \"query\": {\"pages\": [{\"ns\": 0, \"title\": \"" + title + "\", \"missing\": true}]}}";
	}

	/** The wiki's search answer with one hit. */
	private static final String SEARCH_HIT = "{\"batchcomplete\": true, \"query\": {\"searchinfo\": {\"totalhits\": 12}, "
		+ "\"search\": [{\"ns\": 0, \"title\": \"Jubbly Jive Barracuda Trial\", \"pageid\": 123}]}}";

	/** The wiki's search answer with no hit. */
	private static final String SEARCH_NONE = "{\"batchcomplete\": true, \"query\": {\"searchinfo\": {\"totalhits\": 0}, "
		+ "\"search\": []}}";

	/** The wiki's extract answer for the trial's page. */
	private static final String TRIAL_ANSWER = "{\"batchcomplete\": true, \"query\": {\"pages\": [{\"pageid\": 123, "
		+ "\"ns\": 0, \"title\": \"Jubbly Jive Barracuda Trial\", \"extract\": \"The Jubbly Jive Barracuda Trial is a "
		+ "sailing trial.\\nIt is run at ...\"}]}}";

	@Test
	public void theSearchRequestAndItsFirstTitle()
	{
		final Request request = WikiSummaries.requestForSearch("Jubbly Jive Shark");
		assertEquals("https://oldschool.runescape.wiki/api.php?action=query&list=search&srsearch=Jubbly%20Jive%20Shark"
			+ "&srlimit=1&format=json&formatversion=2", request.url().toString());
		assertTrue("the User-Agent names the plugin: " + request.header("User-Agent"),
			request.header("User-Agent").contains("2h Chat Hover Info"));
		// Re-aimed by the lead, 2026-10-08 (lot 8M): the name is 2h Chat Hover Info and the command ::hoverinfo.
		assertTrue("the User-Agent names the repository: " + request.header("User-Agent"),
			request.header("User-Agent").contains("chat-hover-info"));
		assertEquals("Jubbly Jive Barracuda Trial", WikiSummaries.firstSearchTitle(SEARCH_HIT, gson));
		assertNull("no hit", WikiSummaries.firstSearchTitle("{\"query\": {\"search\": []}}", gson));
		assertNull("an empty answer", WikiSummaries.firstSearchTitle("{}", gson));
		assertNull("text that is not JSON", WikiSummaries.firstSearchTitle("<html>no</html>", gson));
		assertNull("nothing at all", WikiSummaries.firstSearchTitle("", gson));
	}

	@Test
	public void aMissingPageFallsBackToTheSearchsFirstHit() throws Exception
	{
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiSummaries summaries = new WikiSummaries(factory, gson);

		summaries.fetch("Jubbly Jive Shark");
		final ArgumentCaptor<Request> requests = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(1)).newCall(requests.capture());
		assertEquals(WikiSummaries.requestFor("Jubbly Jive Shark").url(), requests.getValue().url());
		final ArgumentCaptor<Callback> callbacks = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(callbacks.capture());
		callbacks.getValue().onResponse(call, ok(missingPage("Jubbly Jive Shark")));
		assertNull("not kept while the chain runs", summaries.summaryOf("Jubbly Jive Shark"));

		final ArgumentCaptor<Request> second = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(2)).newCall(second.capture());
		assertEquals(WikiSummaries.requestForSearch("Jubbly Jive Shark").url(), second.getValue().url());
		final ArgumentCaptor<Callback> secondCallbacks = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(secondCallbacks.capture());
		summaries.fetch("Jubbly Jive Shark");
		verify(factory, times(2)).newCall(any(Request.class));
		secondCallbacks.getValue().onResponse(call, ok(SEARCH_HIT));
		assertNull("not kept while the chain runs", summaries.summaryOf("Jubbly Jive Shark"));

		final ArgumentCaptor<Request> third = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(3)).newCall(third.capture());
		assertEquals(WikiSummaries.requestFor("Jubbly Jive Barracuda Trial").url(), third.getValue().url());
		final ArgumentCaptor<Callback> thirdCallbacks = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(3)).enqueue(thirdCallbacks.capture());
		thirdCallbacks.getValue().onResponse(call, ok(TRIAL_ANSWER));
		assertEquals("The Jubbly Jive Barracuda Trial is a sailing trial.", summaries.summaryOf("Jubbly Jive Shark"));
		assertNull("kept under the asked name only", summaries.summaryOf("Jubbly Jive Barracuda Trial"));

		verify(factory, times(3)).newCall(any(Request.class));
		summaries.fetch("Jubbly Jive Shark");
		verify(factory, times(3)).newCall(any(Request.class));
	}

	@Test
	public void aSearchWithNoHitGivesNoParagraph() throws Exception
	{
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiSummaries summaries = new WikiSummaries(factory, gson);

		summaries.fetch("Nonsense title");
		final ArgumentCaptor<Callback> first = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(first.capture());
		first.getValue().onResponse(call, ok(missingPage("Nonsense title")));
		verify(factory, times(2)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> search = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(search.capture());
		search.getValue().onResponse(call, ok(SEARCH_NONE));
		assertEquals("", summaries.summaryOf("Nonsense title"));
		verify(factory, times(2)).newCall(any(Request.class));
		summaries.fetch("Nonsense title");
		verify(factory, times(2)).newCall(any(Request.class));

		summaries.fetch("Another nonsense");
		final ArgumentCaptor<Callback> again = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(3)).enqueue(again.capture());
		again.getValue().onResponse(call, ok(missingPage("Another nonsense")));
		final ArgumentCaptor<Callback> failing = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(4)).enqueue(failing.capture());
		failing.getValue().onFailure(call, new IOException("no route to the wiki"));
		assertEquals("a failed search", "", summaries.summaryOf("Another nonsense"));
		verify(factory, times(4)).newCall(any(Request.class));
		summaries.fetch("Another nonsense");
		verify(factory, times(4)).newCall(any(Request.class));
	}

	/** The wiki's search answer with one hit of the given title. */
	private static String searchHit(String title)
	{
		return "{\"batchcomplete\": true, \"query\": {\"searchinfo\": {\"totalhits\": 3}, \"search\": [{\"ns\": 0, "
			+ "\"title\": \"" + title + "\", \"pageid\": 9}]}}";
	}

	@Test
	public void theNearestPageMustShareAWord() throws Exception
	{
		assertTrue(WikiSummaries.nearEnough("Tempor Tantrum Marlin", "The Tempor Tantrum"));
		assertTrue(WikiSummaries.nearEnough("Jubbly Jive Shark", "Jubbly Jive Barracuda Trial"));
		assertTrue(WikiSummaries.nearEnough("Brutus", "Brutus (Ides of Milk)"));
		assertFalse(WikiSummaries.nearEnough("Pandemonium", "Chambers of Xeric"));
		assertFalse(WikiSummaries.nearEnough("The", "Of"));
		assertTrue("case does not matter", WikiSummaries.nearEnough("Zulrah", "zulrah"));
		assertFalse("no words of 3 letters", WikiSummaries.nearEnough("A B", "C"));

		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiSummaries summaries = new WikiSummaries(factory, gson);

		summaries.fetch("Pandemonium");
		final ArgumentCaptor<Callback> first = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(first.capture());
		first.getValue().onResponse(call, ok(missingPage("Pandemonium")));
		final ArgumentCaptor<Request> second = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(2)).newCall(second.capture());
		assertEquals(WikiSummaries.requestForSearch("Pandemonium").url(), second.getValue().url());
		final ArgumentCaptor<Callback> search = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(search.capture());
		search.getValue().onResponse(call, ok(searchHit("Chambers of Xeric")));

		verify(factory, times(2)).newCall(any(Request.class));
		assertEquals("asked and empty, not null", "", summaries.summaryOf("Pandemonium"));
		summaries.fetch("Pandemonium");
		verify(factory, times(2)).newCall(any(Request.class));
	}
}
