package com.clanhub;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WikiPictures}: the requests made, the thumbnail address read and checked, the picture decoded and fitted to its
 * box, and the answers kept and asked for once.
 */
public class WikiPicturesTest
{
	/** The wiki's page answer for Duke Sucellus: a PNG thumbnail on the wiki's own host. */
	private static final String DUKE = "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":1,\"ns\":0,\"title\":"
		+ "\"Duke Sucellus\",\"thumbnail\":{\"source\":\"https://oldschool.runescape.wiki/images/thumb/Duke_Sucellus.png/"
		+ "128px-Duke_Sucellus.png?abcde\",\"width\":128,\"height\":142},\"pageimage\":\"Duke_Sucellus.png\"}]}}";

	/** The wiki's page answer for a page with no thumbnail. */
	private static final String NO_THUMBNAIL = "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"pageid\":2,\"ns\":0,"
		+ "\"title\":\"Zulrah\"}]}}";

	private final Gson gson = new Gson();

	/** The bytes of the fixture picture: 128 x 40, transparent but for a red rectangle. */
	private static byte[] fixtureBytes() throws IOException
	{
		try (InputStream in = WikiPicturesTest.class.getResourceAsStream("/com/clanhub/picture-fixture.png"))
		{
			assertNotNull("the fixture is on the test classpath", in);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] chunk = new byte[4096];
			for (int read = in.read(chunk); read >= 0; read = in.read(chunk))
			{
				out.write(chunk, 0, read);
			}
			return out.toByteArray();
		}
	}

	/** A 200 answer holding {@code json}. */
	private static Response ok(String json)
	{
		return answer(200, "application/json", json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	/** An answer with {@code code} and {@code body} of the given type. */
	private static Response answer(int code, String type, byte[] body)
	{
		return new Response.Builder()
			.request(WikiPictures.requestFor("Duke Sucellus"))
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message(code == 200 ? "OK" : "No")
			.body(ResponseBody.create(MediaType.parse(type), body))
			.build();
	}

	/** A 200 answer holding the fixture picture. */
	private static Response picture() throws IOException
	{
		return answer(200, "image/png", fixtureBytes());
	}

	@Test
	public void theRequestNamesThePluginAndThePage()
	{
		final Request request = WikiPictures.requestFor("Duke Sucellus");
		assertEquals("GET", request.method());
		assertEquals("https", request.url().scheme());
		assertEquals("oldschool.runescape.wiki", request.url().host());
		assertEquals("/api.php", request.url().encodedPath());
		assertEquals("query", request.url().queryParameter("action"));
		assertEquals("pageimages", request.url().queryParameter("prop"));
		assertEquals("thumbnail", request.url().queryParameter("piprop"));
		assertEquals("128", request.url().queryParameter("pithumbsize"));
		assertEquals("1", request.url().queryParameter("redirects"));
		assertEquals("json", request.url().queryParameter("format"));
		assertEquals("2", request.url().queryParameter("formatversion"));
		assertEquals("Duke Sucellus", request.url().queryParameter("titles"));
		assertEquals("nothing else is sent", 8, request.url().querySize());
		assertEquals(WikiSummaries.USER_AGENT, request.header("User-Agent"));
		assertTrue("the User-Agent names the plugin: " + request.header("User-Agent"),
			request.header("User-Agent").contains("2h Chat Hover Info"));

		final HttpUrl address = HttpUrl.parse("https://oldschool.runescape.wiki/images/thumb/a.png/128px-a.png?1");
		final Request picture = WikiPictures.requestForPicture(address);
		assertEquals("GET", picture.method());
		assertEquals(address, picture.url());
		assertEquals(WikiSummaries.USER_AGENT, picture.header("User-Agent"));
	}

	@Test
	public void theThumbnailAddressIsReadAndChecked()
	{
		final HttpUrl duke = WikiPictures.thumbnailUrl(DUKE, gson);
		assertNotNull(duke);
		assertEquals("https://oldschool.runescape.wiki/images/thumb/Duke_Sucellus.png/128px-Duke_Sucellus.png?abcde",
			duke.toString());

		assertNull("no thumbnail", WikiPictures.thumbnailUrl(NO_THUMBNAIL, gson));
		assertNull("another host", WikiPictures.thumbnailUrl(page("https://example.com/a.png"), gson));
		assertNull("not https", WikiPictures.thumbnailUrl(page("http://oldschool.runescape.wiki/a.png"), gson));
		assertNull("a WebP file", WikiPictures.thumbnailUrl(
			page("https://oldschool.runescape.wiki/images/thumb/Mad_Angel.png/128px-Mad_Angel.png.webp"), gson));
		assertNull("a page that is missing", WikiPictures.thumbnailUrl(
			"{\"query\":{\"pages\":[{\"title\":\"X\",\"missing\":true}]}}", gson));
		assertNull("an empty answer", WikiPictures.thumbnailUrl("{}", gson));
		assertNull("nothing at all", WikiPictures.thumbnailUrl("", gson));
		assertNull("text that is not JSON", WikiPictures.thumbnailUrl("not json", gson));

		assertNotNull("a JPEG", WikiPictures.thumbnailUrl(page("https://oldschool.runescape.wiki/a/b.JPG"), gson));
		assertNotNull("a GIF", WikiPictures.thumbnailUrl(page("https://oldschool.runescape.wiki/a/b.gif"), gson));
		assertNotNull("a JPEG, the long way", WikiPictures.thumbnailUrl(page("https://oldschool.runescape.wiki/b.jpeg"), gson));
	}

	/** The wiki's page answer whose thumbnail is at {@code source}. */
	private static String page(String source)
	{
		return "{\"query\":{\"pages\":[{\"title\":\"X\",\"thumbnail\":{\"source\":\"" + source + "\",\"width\":128,"
			+ "\"height\":40}}]}}";
	}

	@Test
	public void aPictureIsDecodedAndFittedToTheBox() throws Exception
	{
		final BufferedImage decoded = WikiPictures.decode(fixtureBytes());
		assertNotNull("the fixture decodes", decoded);
		assertEquals(128, decoded.getWidth());
		assertEquals(40, decoded.getHeight());
		assertEquals("a transparent pixel keeps its alpha", 0, decoded.getRGB(0, 0) >>> 24);
		assertEquals("an opaque red one is red", 0xFFFF0000, decoded.getRGB(64, 20));

		final BufferedImage fitted = WikiPictures.fit(decoded);
		assertEquals(64, fitted.getWidth());
		assertTrue("19 to 21 tall: " + fitted.getHeight(), fitted.getHeight() >= 19 && fitted.getHeight() <= 21);

		final BufferedImage tall = WikiPictures.fit(new BufferedImage(128, 381, BufferedImage.TYPE_INT_ARGB));
		assertEquals(64, tall.getHeight());
		assertTrue("20 to 22 wide: " + tall.getWidth(), tall.getWidth() >= 20 && tall.getWidth() <= 22);

		final BufferedImage small = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
		assertSame("a small picture is not scaled up", small, WikiPictures.fit(small));
		final BufferedImage exact = WikiPictures.fit(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
		assertEquals(64, exact.getWidth());
		assertEquals(64, exact.getHeight());

		assertNull("bytes that are no picture", WikiPictures.decode(new byte[] {1, 2, 3}));
		assertNull("a WebP file", WikiPictures.decode("RIFF....WEBPVP8 ".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
		assertNull("no bytes", WikiPictures.decode(new byte[0]));
		assertNull("nothing", WikiPictures.decode(null));
	}

	@Test
	public void aFetchAsksTwiceKeepsThePictureAndNeverRepeats() throws Exception
	{
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiPictures pictures = new WikiPictures(factory, gson);

		pictures.fetch("Duke Sucellus");
		final ArgumentCaptor<Request> requests = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(1)).newCall(requests.capture());
		assertEquals(WikiPictures.requestFor("Duke Sucellus").url(), requests.getValue().url());
		assertNull("not answered yet", pictures.pictureOf("Duke Sucellus"));
		assertNull("never asked for", pictures.pictureOf("Zulrah"));
		pictures.fetch("Duke Sucellus");
		verify(factory, times(1)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> page = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(1)).enqueue(page.capture());
		page.getValue().onResponse(call, ok(DUKE));
		final ArgumentCaptor<Request> second = ArgumentCaptor.forClass(Request.class);
		verify(factory, times(2)).newCall(second.capture());
		assertEquals("the second call is the thumbnail's own address", WikiPictures.thumbnailUrl(DUKE, gson),
			second.getValue().url());
		assertNull("not kept while the chain runs", pictures.pictureOf("Duke Sucellus"));
		pictures.fetch("Duke Sucellus");
		verify(factory, times(2)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> thumb = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(2)).enqueue(thumb.capture());
		thumb.getValue().onResponse(call, picture());
		final BufferedImage kept = pictures.pictureOf("Duke Sucellus");
		assertNotNull("the picture is kept", kept);
		assertEquals(64, kept.getWidth());
		assertTrue("19 to 21 tall: " + kept.getHeight(), kept.getHeight() >= 19 && kept.getHeight() <= 21);
		pictures.fetch("Duke Sucellus");
		verify(factory, times(2)).newCall(any(Request.class));

		// A page with no thumbnail ends the chain at one call, with none.
		pictures.fetch("Zulrah");
		verify(factory, times(3)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> bare = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(3)).enqueue(bare.capture());
		bare.getValue().onResponse(call, ok(NO_THUMBNAIL));
		verify(factory, times(3)).newCall(any(Request.class));
		assertNull(pictures.pictureOf("Zulrah"));
		pictures.fetch("Zulrah");
		verify(factory, times(3)).newCall(any(Request.class));

		// A picture call that answers 404 gives none, and is not asked again.
		pictures.fetch("Vorkath");
		final ArgumentCaptor<Callback> vorkathPage = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(4)).enqueue(vorkathPage.capture());
		vorkathPage.getValue().onResponse(call, ok(DUKE));
		verify(factory, times(5)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> vorkathPicture = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(5)).enqueue(vorkathPicture.capture());
		vorkathPicture.getValue().onResponse(call, answer(404, "text/html", "gone".getBytes()));
		assertNull(pictures.pictureOf("Vorkath"));
		pictures.fetch("Vorkath");
		verify(factory, times(5)).newCall(any(Request.class));

		// A picture body that is no picture gives none, and is not asked again.
		pictures.fetch("Nex");
		final ArgumentCaptor<Callback> nexPage = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(6)).enqueue(nexPage.capture());
		nexPage.getValue().onResponse(call, ok(DUKE));
		verify(factory, times(7)).newCall(any(Request.class));
		final ArgumentCaptor<Callback> nexPicture = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(7)).enqueue(nexPicture.capture());
		nexPicture.getValue().onResponse(call, answer(200, "image/png", new byte[] {1, 2, 3}));
		assertNull(pictures.pictureOf("Nex"));
		pictures.fetch("Nex");
		verify(factory, times(7)).newCall(any(Request.class));

		// A thumbnail that is a WebP file is never fetched: one call only.
		pictures.fetch("Mad Angel");
		final ArgumentCaptor<Callback> angel = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(8)).enqueue(angel.capture());
		angel.getValue().onResponse(call, ok(
			page("https://oldschool.runescape.wiki/images/thumb/Mad_Angel.png/128px-Mad_Angel.png.webp")));
		verify(factory, times(8)).newCall(any(Request.class));
		assertNull(pictures.pictureOf("Mad Angel"));
		pictures.fetch("Mad Angel");
		verify(factory, times(8)).newCall(any(Request.class));
	}

	@Test
	public void theInFlightCapHolds() throws Exception
	{
		final Call.Factory factory = mock(Call.Factory.class);
		final Call call = mock(Call.class);
		when(factory.newCall(any(Request.class))).thenReturn(call);
		final WikiPictures pictures = new WikiPictures(factory, gson);

		for (int title = 0; title < 10; title++)
		{
			pictures.fetch("Boss " + title);
		}
		verify(factory, times(10)).newCall(any(Request.class));
		pictures.fetch("Boss 10");
		verify(factory, times(10)).newCall(any(Request.class));

		final ArgumentCaptor<Callback> callbacks = ArgumentCaptor.forClass(Callback.class);
		verify(call, times(10)).enqueue(callbacks.capture());
		callbacks.getAllValues().get(0).onResponse(call, ok(NO_THUMBNAIL));
		pictures.fetch("Boss 10");
		verify(factory, times(11)).newCall(any(Request.class));
	}
}
