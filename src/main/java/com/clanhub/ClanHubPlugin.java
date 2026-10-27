package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTheme;
import com.clanhub.ClanHubConfig.CardTime;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.clanhub.ClanHubConfig.ExamineLine;
import com.clanhub.ClanHubConfig.SidebarTheme;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.input.MouseWheelListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AsyncBufferedImage;
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
 * through RuneLite's mouse manager and passed on to the game untouched. The sidebar lists the last 50 lines that gave a
 * card (see {@link CardHistory} and {@link ClanHubPanel}), kept for the session only; hovering a row shows its card in a
 * small window beside the sidebar. In developer mode the command {@code ::hoverinfo demo} posts sample lines to hover.
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

	/** The gap between a sidebar row's card window and the sidebar's left edge, in px. */
	private static final int CARD_GAP = 8;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	/** RuneLite's colour picker maker, which the sidebar menu's three colour swatches open the picker through. */
	@Inject
	private ColorPickerManager colorPickerManager;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	/** RuneLite's sprite manager, which the sidebar rows' pictures of a sprite card are asked from. */
	@Inject
	private SpriteManager spriteManager;

	/** RuneLite's client thread, which a hovered sidebar row's card is painted on. */
	@Inject
	private ClientThread clientThread;

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

	/**
	 * The reader of the player's own examine lines, built in {@link #startUp}; the menu clicks are told to it, which is how
	 * its lines know what was examined.
	 *
	 * <p>Choice: volatile, like {@link #readers}: start-up writes it on a thread other than the client thread that reads it.
	 */
	private volatile ExamineReader examine;

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
	 * The sidebar's panel, kept so that a change of the plugin's settings can tell it to repaint ({@link
	 * #onConfigChanged}); null while the plugin is off.
	 *
	 * <p>Choice: volatile, because start-up writes it on a thread other than the one that reads it.
	 */
	private volatile ClanHubPanel panel;

	/**
	 * The recent cards the sidebar lists: the last 50 chat lines that gave a card, made in {@link #startUp} and cleared in
	 * {@link #shutDown}, kept for the session only.
	 *
	 * <p>Choice: volatile, because start-up writes it on a thread other than the client thread that adds to it.
	 */
	private volatile CardHistory history;

	/** The window that shows a hovered sidebar row's card beside the sidebar, made on first use; the event thread only. */
	private JWindow cardWindow;

	/** What {@link #cardWindow} draws, the card's picture at device pixels; the event thread only. */
	private SharpCardWindow.Surface cardLabel;

	/**
	 * The window the in-game card shows in while the game is stretched or scaled, made in {@link #startUp} and taken away
	 * in {@link #shutDown}.
	 */
	private SharpCardWindow sharpWindow;

	/**
	 * Counts every ask of the sidebar's card viewer, a show or a hide, so that a card painted on the client thread is put
	 * up only if nothing was asked since it was.
	 */
	private final AtomicInteger cardAsks = new AtomicInteger();

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
		examine = new ExamineReader(itemCards, tables, config::examineCards, client::getTickCount,
			config::examineLine);
		readers = Arrays.asList(
			new BroadcastItemReader(itemCards, config::bossCards, tables),
			new OwnLineReader(itemCards, config::bossCards, tables),
			new BroadcastKindReader(tables, itemCards),
			examine);
		summaries = new WikiSummaries(okHttpClient, gson);
		overlay.summaries(summaries);
		pictures = new WikiPictures(okHttpClient, gson);
		overlay.pictures(pictures);
		overlay.tables(tables);
		overlay.attach(store);
		sharpWindow = new SharpCardWindow(client::getCanvas);
		overlay.sharpCards(sharpWindow);
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
		history = new CardHistory();
		final ClanHubPanel sidebar = new ClanHubPanel(this::resetSettings, new PanelSettings()
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
			public boolean examineCards()
			{
				return config.examineCards();
			}

			@Override
			public void setExamineCards(boolean on)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "examineCards", on);
			}

			@Override
			public ExamineLine examineLine()
			{
				return config.examineLine();
			}

			@Override
			public void setExamineLine(ExamineLine line)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "examineLine", line);
			}

			@Override
			public CardTime cardTime()
			{
				return config.cardTime();
			}

			@Override
			public void setCardTime(CardTime time)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardTime", time);
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
			public Color nameColour()
			{
				return config.nameColour();
			}

			@Override
			public void setNameColour(Color colour)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "nameColour", colour);
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
			public Keybind cardKey()
			{
				return config.cardKey();
			}

			@Override
			public void setCardKey(Keybind key)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardKey", key);
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
			public SidebarTheme sidebarLook()
			{
				return config.sidebarLook();
			}

			@Override
			public void setSidebarLook(SidebarTheme look)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "sidebarLook", look);
			}

			@Override
			public CardTheme cardTheme()
			{
				return config.cardTheme();
			}

			@Override
			public void setCardTheme(CardTheme theme)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "cardTheme", theme);
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

			@Override
			public boolean howToFolded()
			{
				return config.howToFolded();
			}

			@Override
			public void setHowToFolded(boolean folded)
			{
				configManager.setConfiguration(ClanHubConfig.GROUP, "howToFolded", folded);
			}
		}, (parent, title, current, onChange, onClose, at) ->
		{
			final RuneliteColorPicker colourPicker = colorPickerManager.create(parent, current, title, true);
			colourPicker.setOnColorChange(onChange);
			colourPicker.setOnClose(onClose);
			colourPicker.setLocation(ScreenPlace.leftOf(at, colourPicker.getSize(),
				screenOf(new Rectangle(at.x, at.y, 1, 1))));
			colourPicker.setVisible(true);
		}, history, cardViewer(), iconSource(), LinkBrowser::browse, ClanHubPlugin::copyToClipboard);
		panel = sidebar;
		navButton = NavigationButton.builder()
			.tooltip("2h Chat Hover Info")
			.icon(NavIcon.create())
			.priority(NAV_PRIORITY)
			.panel(sidebar)
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
		overlay.sharpCards(null);
		if (sharpWindow != null)
		{
			sharpWindow.dispose();
			sharpWindow = null;
		}
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		panel = null;
		store.clear();
		final CardHistory recent = history;
		if (recent != null)
		{
			recent.clear();
		}
		cardAsks.incrementAndGet();
		onEventThread(() ->
		{
			if (cardWindow != null)
			{
				cardWindow.dispose();
				cardWindow = null;
				cardLabel = null;
			}
		});
	}

	/**
	 * A chat line: every reader is asked for the cards the line holds, and what they give is kept under the line's plain
	 * text (the text without its tags and a combat achievement's id), and the line with its first card is added to the
	 * sidebar's recent cards. A line no reader finds a card in is not kept. When
	 * "Colour hoverable words" is on and the line holds marked cards, the message's text gets colour tags around each of
	 * their words, and nothing else changes in it (a line whose cards are all quiet is not touched); the store's key is the
	 * plain text, which the tags do not change. A line that answers an Examine click and is to carry the thing's name gets
	 * it first, "Law rune: Used for teleport spells.", written into the message before the readers read it (see {@link
	 * ExamineReader#label}), whatever the colouring setting. In developer mode, {@code ::hoverinfo trace} toggles an INFO line per chat
	 * event in the client log, for reading why a line gave no card.
	 *
	 * <p>Choice: a message that is null is skipped.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final String raw = event.getMessage();
		if (raw == null)
		{
			return;
		}
		final boolean traced = tracing && isTraced(event.getType());
		if (traced)
		{
			log.info("hoverinfo trace: type={} name={} sender={} raw={} plain={}", event.getType(),
				quoted(event.getName()), quoted(event.getSender()), quoted(raw), quoted(ChatText.plain(raw)));
		}
		// An examine line may gain the examined thing's name in front; the line read from here on is that one.
		final ExamineReader labeller = examine;
		final String message = labeller == null ? raw : labeller.label(event.getType(), raw);
		if (!message.equals(raw))
		{
			event.getMessageNode().setValue(message);
			event.getMessageNode().setRuneLiteFormatMessage(message);
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
		final String plain = ChatText.plain(message);
		store.put(plain, cards);
		if (cards.isEmpty())
		{
			return;
		}
		final CardHistory recent = history;
		if (recent != null)
		{
			// The local player's name lets the history count the player's own game line and the clan's broadcast of it as
			// one event; the event comes on the client thread, where the local player may be read.
			final Player me = client.getLocalPlayer();
			final String self = me == null ? null : me.getName();
			final long now = System.currentTimeMillis();
			if (self == null)
			{
				recent.add(plain, cards, now);
			}
			else
			{
				recent.add(plain, cards, now, self);
			}
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

	/**
	 * A change in the plugin's settings, made in the sidebar's menu or on RuneLite's own settings page (whose colour
	 * picker is live too): the sidebar repaints at once, on the event thread, so that the recent cards show the new colour
	 * and the how-to the new words. A change in another plugin's group is not ours and is ignored.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ClanHubConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		final ClanHubPanel sidebar = panel;
		if (sidebar != null)
		{
			onEventThread(sidebar::settingsChanged);
		}
	}

	/** A right-click: when the mouse is on a line that holds a card, the menu gains "Open wiki" and "Copy". */
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		CardMenu.addEntries(client.getMenu(), overlay.hovered(), LinkBrowser::browse, ClanHubPlugin::copyToClipboard);
	}

	/** A menu click: an Examine click is told to the examine reader, which is how an examine line gets its card. */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		final ExamineReader reader = examine;
		if (reader != null)
		{
			ExamineReader.clicked(reader, event);
		}
	}

	/** Puts the text on the system clipboard. */
	private static void copyToClipboard(String text)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	/**
	 * What shows a hovered sidebar row's card: the card is painted on the client thread ({@link
	 * ChatCardOverlay#cardImage}), and put in a small window beside the sidebar on the event thread, unless a hide or a
	 * newer show was asked in between ({@link #cardAsks}).
	 */
	private CardViewer cardViewer()
	{
		return new CardViewer()
		{
			@Override
			public void show(HoverCard card, Rectangle rowOnScreen)
			{
				final int ask = cardAsks.incrementAndGet();
				clientThread.invokeLater(() ->
				{
					final BufferedImage image = overlay.cardImage(card);
					if (image == null)
					{
						return;
					}
					SwingUtilities.invokeLater(() ->
					{
						if (cardAsks.get() == ask)
						{
							showCardWindow(image, rowOnScreen, false);
						}
					});
				});
			}

			@Override
			public void showPicture(BufferedImage picture, Rectangle besideOnScreen)
			{
				cardAsks.incrementAndGet();
				onEventThread(() -> showCardWindow(picture, besideOnScreen, true));
			}

			@Override
			public void hide()
			{
				cardAsks.incrementAndGet();
				onEventThread(() ->
				{
					if (cardWindow != null)
					{
						cardWindow.setVisible(false);
					}
				});
			}
		};
	}

	/**
	 * Puts {@code image} in the card window and shows it, its right edge {@link #CARD_GAP} px left of the row's left edge
	 * and its middle level with the row's, kept on the row's screen; with {@code eitherSide} it goes right of the row
	 * instead when there is room there, which a menu's item asks for ({@link #besideItem}). The window is made on first
	 * use, owned by the window the sidebar is in when it has one, with no decorations and never taking the keyboard from
	 * the game. Runs on the event thread.
	 */
	private void showCardWindow(BufferedImage image, Rectangle row, boolean eitherSide)
	{
		if (cardWindow == null)
		{
			final PluginPanel sidebar = navButton == null ? null : navButton.getPanel();
			final Window owner = sidebar == null ? null : SwingUtilities.getWindowAncestor(sidebar);
			cardWindow = owner == null ? new JWindow() : new JWindow(owner);
			cardWindow.setFocusableWindowState(false);
			cardLabel = new SharpCardWindow.Surface();
			cardWindow.getContentPane().add(cardLabel);
		}
		// Choice: drawn at device pixels, a whole number of them to a card pixel, so it is sharp on a screen scaled above 100%.
		final double deviceScale = SharpCardWindow.deviceScaleOf(cardWindow);
		final int scale = SharpCardWindow.scaleFor(1, deviceScale);
		final Dimension size = SharpCardWindow.sizeOf(new Dimension(image.getWidth(), image.getHeight()), scale, deviceScale);
		cardLabel.set(image, scale);
		cardWindow.setSize(size);
		cardWindow.setLocation(eitherSide ? besideItem(row, size, screenOf(row)) : besideRow(row, size, screenOf(row)));
		cardWindow.setVisible(true);
	}

	/**
	 * Where a picture of {@code card} size goes for the menu item {@code item}: {@link #CARD_GAP} px right of the item's
	 * right edge when it fits on {@code screen} there, else {@link #CARD_GAP} px left of its left edge, its middle level
	 * with the item's and the whole of it held inside {@code screen} (at its left and top edges when the picture is bigger
	 * than the room).
	 */
	private static Point besideItem(Rectangle item, Dimension card, Rectangle screen)
	{
		final int right = item.x + item.width + CARD_GAP;
		if (right + card.width <= screen.x + screen.width)
		{
			final int y = item.y + item.height / 2 - card.height / 2;
			return new Point(right, Math.max(screen.y, Math.min(y, screen.y + screen.height - card.height)));
		}
		return besideRow(item, card, screen);
	}

	/**
	 * Where a card window of {@code card} size goes for the row {@code row}: its right edge {@link #CARD_GAP} px left of the
	 * row's left edge, its middle level with the row's, and held inside {@code screen} (at its left and top edges when the
	 * card is bigger than the room).
	 */
	private static Point besideRow(Rectangle row, Dimension card, Rectangle screen)
	{
		final int x = row.x - CARD_GAP - card.width;
		final int y = row.y + row.height / 2 - card.height / 2;
		return new Point(
			Math.max(screen.x, Math.min(x, screen.x + screen.width - card.width)),
			Math.max(screen.y, Math.min(y, screen.y + screen.height - card.height)));
	}

	/** The usable area (the screen less its task bar and the like) of the screen the middle of {@code row} is on. */
	private static Rectangle screenOf(Rectangle row)
	{
		final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration found = environment.getDefaultScreenDevice().getDefaultConfiguration();
		for (final GraphicsDevice device : environment.getScreenDevices())
		{
			final GraphicsConfiguration candidate = device.getDefaultConfiguration();
			if (candidate.getBounds().contains(row.x + row.width / 2, row.y + row.height / 2))
			{
				found = candidate;
				break;
			}
		}
		final Rectangle bounds = found.getBounds();
		final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(found);
		return new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
			bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
	}

	/**
	 * Where the sidebar gets its pictures: a row's card through {@link #iconFor}, and the how-to's rows' sprites and items
	 * straight from the game's sprite and item managers, an item's picture handed out at once and again when it has
	 * loaded. Each may be called on any thread; the panel moves a picture to the event thread itself.
	 */
	private IconSource iconSource()
	{
		return new IconSource()
		{
			@Override
			public void iconFor(HoverCard card, Consumer<BufferedImage> onIcon)
			{
				ClanHubPlugin.this.iconFor(card, onIcon);
			}

			@Override
			public void sprite(int spriteId, Consumer<BufferedImage> onIcon)
			{
				spriteManager.getSpriteAsync(spriteId, 0, onIcon);
			}

			@Override
			public void item(int itemId, int quantity, Consumer<BufferedImage> onIcon)
			{
				final AsyncBufferedImage image = itemManager.getImage(itemId, quantity, quantity > 1);
				onIcon.accept(image);
				image.onLoaded(() -> onIcon.accept(image));
			}
		};
	}

	/**
	 * Gives {@code onIcon} the picture of a sidebar row's card: the wiki's own picture when the card wants it, "Wiki
	 * pictures" is on and one is kept; else the card's item, now and again when it has loaded; else the card's sprite, when
	 * it comes; else nothing. May be called on any thread; the panel moves the picture to the event thread itself.
	 */
	private void iconFor(HoverCard card, Consumer<BufferedImage> onIcon)
	{
		final WikiPictures pictured = pictures;
		if (pictured != null && card.wikiPicture() && config.wikiPictures())
		{
			final BufferedImage wiki = pictured.pictureOf(card.wikiTitle());
			if (wiki != null)
			{
				onIcon.accept(wiki);
				return;
			}
		}
		if (card.itemId() >= 0)
		{
			final AsyncBufferedImage image = itemManager.getImage(card.itemId(), card.quantity(), card.quantity() > 1);
			onIcon.accept(image);
			image.onLoaded(() -> onIcon.accept(image));
		}
		else if (card.spriteId() >= 0)
		{
			spriteManager.getSpriteAsync(card.spriteId(), 0, onIcon);
		}
	}

	/** Runs {@code body} now when this is the event thread, else on it later. */
	private static void onEventThread(Runnable body)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			body.run();
		}
		else
		{
			SwingUtilities.invokeLater(body);
		}
	}

	/** "Reset to default": the plugin's twenty settings go back to their defaults, and the sidebar's how-to opens. */
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
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "examineCards");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "examineLine");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardPlace");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardWidth");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardTheme");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "sidebarLook");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "cardTime");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "nameColour");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "alwaysUnderlineLevels");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "alwaysUnderlineColour");
		configManager.unsetConfiguration(ClanHubConfig.GROUP, "howToFolded");
	}
}
