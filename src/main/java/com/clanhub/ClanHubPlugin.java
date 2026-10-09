package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 2h Chat Hover Info: the plugin's frame and its first part. It adds the sidebar button and its panel at start-up and takes
 * them away at shut-down. In between it asks its line readers about every chat message (the first reads the clan,
 * guest clan, group ironman and friends chat broadcasts for the item each names), keeps the cards they give, and lets
 * the overlay show a card at the cursor when the mouse is on its line; a right-click on the line adds "Open wiki" and
 * "Copy" to the game's menu. While "Wiki descriptions" is on, each card's page name is sent to the wiki as its line
 * arrives, for the page's first paragraph, which the card shows (see {@link WikiSummaries}); while "Wiki pictures" is on,
 * the page name of each boss, monster, raid or chest card is sent too, for the page's own picture (see
 * {@link WikiPictures}). With the show mode "On click" a left click on the words opens the card; the click is read
 * through RuneLite's mouse manager and passed on to the game untouched. In developer mode the command {@code ::hoverinfo demo} posts sample lines to hover.
 */
@PluginDescriptor(
	name = "2h Chat Hover Info",
	internalName = "chat-hover-info",
	description = "Hover a broadcast in your chat, a drop, a quest, a combat task, a pet or a level, for a card with the price, the drop rates, the unlocks, the boss's picture and the wiki's first paragraph. (v" + Version.CURRENT + ")",
	tags = {"chat", "hover", "info", "tooltip", "broadcast", "wiki", "drop", "price", "item", "quest", "achievement", "combat", "task", "pet", "level", "clan", "friends"}
)
public class ClanHubPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(ClanHubPlugin.class);

	/** The sidebar button's place. */
	private static final int NAV_PRIORITY = 7;

	/** How many item ids the names index reads on one game tick. */
	private static final int ITEMS_PER_TICK = 1000;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	/** RuneLite's colour picker maker, which the sidebar menu's two colour swatches open the picker through. */
	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatCardOverlay overlay;

	@Inject
	private ClanHubConfig config;

	/** RuneLite's key manager, which the card key's listener is registered with. */
	@Inject
	private KeyManager keyManager;

	/** RuneLite's mouse manager, which the click listener of the "On click" show mode is registered with. */
	@Inject
	private MouseManager mouseManager;

	/** RuneLite's Gson, for reading the bundled drop rate table. */
	@Inject
	private Gson gson;

	/** RuneLite's executor, which the drop rate table loads on. */
	@Inject
	private ScheduledExecutorService executor;

	/** RuneLite's HTTP client, which the wiki's first paragraphs are asked for through. */
	@Inject
	private OkHttpClient okHttpClient;

	/** Whether the client was started in developer mode; the {@code ::hoverinfo demo} command works only then. */
	@Inject
	@Named("developerMode")
	private boolean developerMode;

	/**
	 * Whether {@code ::hoverinfo trace} has switched the developer trace on: while it is, each clan, friends or game chat
	 * event writes INFO lines to the client log. It starts ON in developer mode and off everywhere else.
	 *
	 * <p>Choice: volatile, because the command is read on one thread and the chat events on another.
	 */
	private volatile boolean tracing;

	/** The cards by plain chat line; it is safe to use from any thread. */
	private final CardStore store = new CardStore();

	/**
	 * The readers every chat message is asked about, built in {@link #startUp}.
	 *
	 * <p>Choice: volatile, because start-up writes it on a thread other than the client thread that reads it.
	 */
	private volatile List<LineReader> readers = Collections.emptyList();

	/** The drop rates of the wiki, loading in the background after start-up; the item cards read from it. */
	private DropRates dropRates;

	/**
	 * The quests, tasks, pets, diaries and bosses of the wiki, loading in the background after start-up; the kind cards
	 * and the boss cards read from it.
	 */
	private CardTables tables;

	/**
	 * The first paragraphs of the wiki's pages, asked for as chat lines with cards arrive; the cards read from it.
	 *
	 * <p>Choice: made new in {@link #startUp} and volatile, because start-up writes it on a thread other than the client
	 * thread that reads it; turning the plugin off and on starts with none kept.
	 */
	private volatile WikiSummaries summaries;

	/**
	 * The wiki's own pictures of the boss, monster, raid and chest pages, asked for as chat lines with such cards arrive; the
	 * cards read from it.
	 *
	 * <p>Choice: made new in {@link #startUp} and volatile, like {@link #summaries}; turning the plugin off and on starts
	 * with none kept.
	 */
	private volatile WikiPictures pictures;

	/**
	 * The names of every item, built a slice per game tick after login: the item cards' way to an id the price list
	 * lacks (a new item, a pet).
	 *
	 * <p>Choice: made new in {@link #startUp} and volatile, because start-up writes it on a thread other than the client
	 * thread that builds and reads it; turning the plugin off and on builds it again.
	 */
	private volatile ItemNameIndex index = new ItemNameIndex();

	private NavigationButton navButton;

	/**
	 * The listener of the card key, registered in {@link #startUp} and taken away in {@link #shutDown}. It takes the key
	 * from the game only over a card in the "While holding a key" mode, so a letter set as the card key still types.
	 */
	private CardKeyListener cardKeyListener;

	/** The listener of left clicks, registered in {@link #startUp} and taken away in {@link #shutDown}. */
	private MouseAdapter clickListener;

	/** The listener of mouse wheel turns, registered in {@link #startUp} and taken away in {@link #shutDown}. */
	private MouseWheelListener wheelListener;

	@Provides
	ClanHubConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanHubConfig.class);
	}

	@Override
	protected void startUp()
	{
		// In developer mode the trace is on from the start, so a restart between lots never loses the next quest line;
		// ::hoverinfo trace still toggles it. A Hub build has no developer mode, so it starts off and stays off.
		tracing = developerMode;
		dropRates = new DropRates();
		dropRates.loadInBackground(executor, gson);
		tables = new CardTables();
		tables.loadInBackground(executor, gson);
		index = new ItemNameIndex();
		final ItemCards itemCards = new ItemCards(itemManager, dropRates, index);
		readers = Arrays.asList(
			new BroadcastItemReader(itemCards, config::bossCards, tables),
			new OwnLineReader(itemCards, config::bossCards, tables),
			new BroadcastKindReader(tables, itemCards));
		summaries = new WikiSummaries(okHttpClient, gson);
		overlay.summaries(summaries);
		pictures = new WikiPictures(okHttpClient, gson);
		overlay.pictures(pictures);
		overlay.tables(tables);
		overlay.attach(store);
		overlayManager.add(overlay);
		cardKeyListener = new CardKeyListener(config::cardKey, config::cardTrigger, config::showCards, overlay::hovered,
			overlay::keyHeld);
		keyManager.registerKeyListener(cardKeyListener);
		clickListener = new MouseAdapter()
		{
			/** Tells the overlay of a left click when the show mode is "On click"; the click is passed on untouched. */
			@Override
			public MouseEvent mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && config.cardTrigger() == CardTrigger.CLICK)
				{
					overlay.clicked();
				}
				return e;
			}
		};
		mouseManager.registerMouseListener(clickListener);
		wheelListener = new MouseWheelListener()
		{
			/** Tells the overlay the wheel turned, which ends a held card; the event is passed on untouched. */
			@Override
			public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e)
			{
				overlay.wheeled();
				return e;
			}
		};
		mouseManager.registerMouseWheelListener(wheelListener);
		final ClanHubPanel panel = new ClanHubPanel(this::resetSettings, new PanelSettings()
		{
			@Override
			public boolean showCards()
			{
				return config.showCards();
			}

			@Override
			public void setShowCards(boolean show)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "showCards", show);
			}

			@Override
			public boolean colourHoverables()
			{
				return config.colourHoverables();
			}

			@Override
			public void setColourHoverables(boolean colour)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "colourHoverables", colour);
			}

			@Override
			public boolean wikiDescriptions()
			{
				return config.wikiDescriptions();
			}

			@Override
			public void setWikiDescriptions(boolean on)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "wikiDescriptions", on);
			}

			@Override
			public boolean wikiPictures()
			{
				return config.wikiPictures();
			}

			@Override
			public void setWikiPictures(boolean on)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "wikiPictures", on);
			}

			@Override
			public boolean bossCards()
			{
				return config.bossCards();
			}

			@Override
			public void setBossCards(boolean on)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "bossCards", on);
			}

			@Override
			public boolean alwaysUnderlineLevels()
			{
				return config.alwaysUnderlineLevels();
			}

			@Override
			public void setAlwaysUnderlineLevels(boolean on)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "alwaysUnderlineLevels", on);
			}

			@Override
			public Color hoverColour()
			{
				return config.hoverColour();
			}

			@Override
			public void setHoverColour(Color colour)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "hoverColour", colour);
			}

			@Override
			public Color alwaysUnderlineColour()
			{
				return config.alwaysUnderlineColour();
			}

			@Override
			public void setAlwaysUnderlineColour(Color colour)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "alwaysUnderlineColour", colour);
			}

			@Override
			public CardTrigger cardTrigger()
			{
				return config.cardTrigger();
			}

			@Override
			public void setCardTrigger(CardTrigger trigger)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardTrigger", trigger);
			}

			@Override
			public CardPlace cardPlace()
			{
				return config.cardPlace();
			}

			@Override
			public void setCardPlace(CardPlace place)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardPlace", place);
			}

			@Override
			public CardWidth cardWidth()
			{
				return config.cardWidth();
			}

			@Override
			public void setCardWidth(CardWidth width)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardWidth", width);
			}

			@Override
			public int cardDelayMs()
			{
				return config.cardDelayMs();
			}

			@Override
			public void setCardDelayMs(int delayMs)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardDelayMs", delayMs);
			}

			@Override
			public int cardFontSize()
			{
				return config.cardFontSize();
			}

			@Override
			public void setCardFontSize(int size)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardFontSize", size);
			}
		}, (parent, title, current, onChange, onClose) ->
		{
			final RuneliteColorPicker colourPicker = colorPickerManager.create(parent, current, title, true);
			colourPicker.setOnColorChange(onChange);
			colourPicker.setOnClose(onClose);
			colourPicker.setVisible(true);
		});
		navButton = NavigationButton.builder()
			.tooltip("2h Chat Hover Info")
			.icon(NavIcon.create())
			.priority(NAV_PRIORITY)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		log.info("2h Chat Hover Info {} started on RuneLite {}", Version.CURRENT, RuneLiteProperties.getVersion());
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(cardKeyListener);
		mouseManager.unregisterMouseListener(clickListener);
		mouseManager.unregisterMouseWheelListener(wheelListener);
		overlayManager.remove(overlay);
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		store.clear();
	}

	/**
	 * A chat line: every reader is asked for the cards the line holds, and what they give is kept under the line's plain
	 * text (the text without its tags and a combat achievement's id). A line no reader finds a card in is not kept. When
	 * "Colour hoverable words" is on and the line holds marked cards, the message's text gets colour tags around each of
	 * their words, and nothing else changes in it (a line whose cards are all quiet is not touched); the store's key is the
	 * plain text, which the tags do not change. In developer mode, {@code ::hoverinfo trace} toggles an INFO line per chat
	 * event in the client log, for reading why a line gave no card.
	 *
	 * <p>Choice: a message that is null is skipped.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final String message = event.getMessage();
		if (message == null)
		{
			return;
		}
		final boolean traced = tracing && isTraced(event.getType());
		if (traced)
		{
			log.info("hoverinfo trace: type={} name={} sender={} raw={} plain={}", event.getType(),
				quoted(event.getName()), quoted(event.getSender()), quoted(message), quoted(ChatText.plain(message)));
		}
		final List<HoverCard> cards = new ArrayList<>();
		for (final LineReader reader : readers)
		{
			cards.addAll(reader.read(event.getType(), message));
		}
		if (traced)
		{
			final StringBuilder titles = new StringBuilder();
			for (final HoverCard card : cards)
			{
				titles.append(titles.length() == 0 ? "" : ", ").append(card.title());
			}
			log.info("hoverinfo trace: {} card(s): {}", cards.size(), cards.isEmpty() ? "-" : titles);
		}
		store.put(ChatText.plain(message), cards);
		if (cards.isEmpty())
		{
			return;
		}
		log.debug("{} card(s) from a {} line", cards.size(), event.getType());
		prefetch(cards);
		if (config.colourHoverables())
		{
			// Only the marked cards' words are coloured; a line of quiet cards alone is left as the game wrote it.
			final List<int[]> spans = ChatText.spansOf(cards);
			if (spans.isEmpty())
			{
				return;
			}
			final String coloured = ChatText.colourSpans(message, spans, config.hoverColour());
			event.getMessageNode().setValue(coloured);
			// The closes are RuneLite's "<colNORMAL>" marker; the chat manager fills it in from this format message.
			event.getMessageNode().setRuneLiteFormatMessage(coloured);
		}
	}

	/**
	 * Whether the trace writes a line for a chat event of this type: every type the client has. It was the clan, friends
	 * and game types until a real clan quest broadcast arrived under none of them (2026-10-09, 'Small Za has completed a
	 * quest: Making History' came and went untraced), which is the very thing the trace is for finding.
	 */
	private static boolean isTraced(ChatMessageType type)
	{
		return type != null;
	}

	/** The text in single quotes for the trace's log line, or {@code null} for a text that is null. */
	private static String quoted(String text)
	{
		return text == null ? "null" : "'" + text + "'";
	}

	/**
	 * Asks the wiki, in the background, for what each card shows of its page, so it is there by the time the line is
	 * hovered: only while "Show hover cards" is on, and then the first paragraph of each card's page while "Wiki
	 * descriptions" is on (only for a card with no description of its own) and the page's own picture of each boss,
	 * monster, raid or chest card while "Wiki pictures" is on. Only the page's title is sent.
	 */
	private void prefetch(List<HoverCard> cards)
	{
		if (!config.showCards())
		{
			return;
		}
		final WikiSummaries asked = summaries;
		if (asked != null && config.wikiDescriptions())
		{
			for (final HoverCard card : cards)
			{
				if (card.notes().isEmpty())
				{
					asked.fetch(card.wikiTitle());
				}
			}
		}
		final WikiPictures pictured = pictures;
		if (pictured != null && config.wikiPictures())
		{
			for (final HoverCard card : cards)
			{
				if (card.wikiPicture())
				{
					pictured.fetch(card.wikiTitle());
				}
			}
		}
	}

	/**
	 * A game tick: while logged in and the item names index is not complete, it reads the next 1,000 item ids (about 30
	 * ticks in all), so the client never hitches; one debug line says how many names it holds when it completes.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		final ItemNameIndex names = index;
		if (names.complete() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		names.buildSome(client, itemManager, ITEMS_PER_TICK);
		if (names.complete())
		{
			log.debug("Item names index complete: {} names", names.size());
		}
	}

	/**
	 * A command typed in the chat, in developer mode only: {@code ::hoverinfo demo} posts the sample lines, and
	 * {@code ::hoverinfo trace} toggles an INFO line per chat event in the client log, for reading why a line gave no
	 * card (one game message says whether the trace is now on or off).
	 */
	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		final int posted = DemoCommand.run(client, developerMode, event);
		if (posted != 0)
		{
			log.debug("Posted {} demo lines", posted);
		}
		if (DemoCommand.isTrace(developerMode, event))
		{
			tracing = !tracing;
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				tracing ? "2h Chat Hover Info: trace on, see client.log" : "2h Chat Hover Info: trace off", null);
		}
	}

	/** A right-click: when the mouse is on a line that holds a card, the menu gains "Open wiki" and "Copy". */
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		CardMenu.addEntries(client.getMenu(), overlay.hovered(), LinkBrowser::browse, ClanHubPlugin::copyToClipboard);
	}

	/** Puts the text on the system clipboard. */
	private static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	/** "Reset to default": the plugin's fourteen settings go back to their defaults. */
	private void resetSettings()
	{
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "showCards");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardFontSize");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "colourHoverables");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "hoverColour");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardTrigger");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardKey");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardDelayMs");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "wikiDescriptions");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "wikiPictures");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "bossCards");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardPlace");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardWidth");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "alwaysUnderlineLevels");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "alwaysUnderlineColour");
	}
}
