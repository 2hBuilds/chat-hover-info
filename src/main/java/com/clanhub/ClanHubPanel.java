package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTheme;
import com.clanhub.ClanHubConfig.CardTime;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.clanhub.ClanHubConfig.ExamineLine;
import com.clanhub.ClanHubConfig.SidebarTheme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultFormatter;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The sidebar panel. At the top, fixed, a card whose top row ends with the support pair and under it a "How to use" box
 * whose title is a bar across the card: a press anywhere on the bar folds the box to the bar, and it stays folded (the
 * panel's own memory, a setting no player sees). Under the bar the action line in two parts (white, then grey), a
 * small grey heading ("WHAT IT WORKS ON") and an icon list of what gets a card, one row per kind (the game's icon, a
 * bold label, a grey rest). Under it the recent cards: a header ("Recent cards", how many are kept this session,
 * "Clear") and a scrolling list of rows, one per chat line that gave a card, newest first, the whole session up to
 * {@link CardHistory#CAP}. A row shows the card's picture, its title, when the line came (how long ago, or the clock time, as set) and the line itself;
 * under the mouse it shows the card beside the sidebar, as in game (what a {@link CardViewer} does), and a right-click on
 * it offers "Open wiki" and "Copy", as the game's own menu does.
 *
 * <p>The sidebar wears a {@link SidebarSkin}: the one the "Sidebar theme" choice names (the card theme's own, RuneLite's
 * plain one, or a theme of its own). The three boxes (the top card, the how-to's bar and a row) paint themselves with it
 * and the labels take its colours; the menu keeps RuneLite's own look. {@link #settingsChanged} works the skin out again.
 *
 * <p>The settings menu the settings icon opens (STANDARD A4 and A5), about ten rows: the header, the "Hover cards"
 * switch, the two theme submenus ("Card theme" and "Sidebar theme", each showing its current theme in grey at the right),
 * four groups that open beside the menu, then the OK row. "Card" holds the width, the place and the time as small
 * choices and the text size; "When it shows" holds when a card opens, the key and the delay; "What gets a card" holds the
 * four switches of what a card carries and the examine line's choice; "Colours" holds the colour switch, the underline
 * switch and the three colour rows (each a swatch that opens RuneLite's colour picker). A pick or a click in a group
 * keeps the group and the menu open. While a theme in the card theme's list or the sidebar theme's list is armed, a
 * picture of it shows beside the list (a sample card, or a small sidebar), and goes when no theme is armed or the menu
 * closes.
 *
 * <p>Everything here runs on the event thread. A chat line arrives on the client thread, so the history's change is
 * handed to the event thread before the list is brought up to date.
 *
 * <p>Choice: the recent cards are a {@link JList}, not a column of panels, because a list paints only the rows on screen
 * and the whole session may be thousands of them: a new card is one insert at the top of the list's model, and a burst
 * of lines is one sync, so five thousand rows cost a lookup of their height each, not a rebuild. A panel per row would be
 * rebuilt, every row, for every card that came.
 */
public class ClanHubPanel extends PluginPanel
{
	/** The plugin's GitHub page, what the menu header's fourth mark opens. */
	static final String GITHUB_URL = "https://github.com/2hBuilds/chat-hover-info";

	/** The menu's rows. */
	static final String RESET_TEXT = "Reset to default";
	static final String OK_TEXT = "OK";
	static final String SHOW_CARDS_TEXT = "Hover cards";
	static final String COLOUR_TEXT = "Colour names";
	static final String WIKI_TEXT = "Wiki text";
	static final String PICTURES_TEXT = "Wiki pictures";
	static final String FROST_TEXT = "Blur behind text";
	static final String FROST_TIP = "Softens the picture behind the words so they're easier to read. 0 is off, 100 is the most.";
	/** The hover text while an original theme is on: the row is greyed, since only picture themes have a picture to soften. */
	static final String FROST_OFF_TIP = "Blur isn't available with Original themes. Pick a picture theme to use it.";
	/** The frost box's smallest, default and largest, in percent, and its step: the setting's own range. */
	static final int MIN_FROST = 0;
	static final int DEFAULT_FROST = 0;
	static final int MAX_FROST = 100;
	/** The blur slider's width, px (a slider with its number beside it; 100, the user's pick B: long enough to reach the number). */
	static final int FROST_SLIDER_WIDTH = 100;
	static final String BOSS_TEXT = "Boss cards";
	static final String ALWAYS_UNDERLINE_TEXT = "Underline level-ups";
	static final String SIDEBAR_THEME_TEXT = "Sidebar theme";
	static final String EXAMINE_TEXT = "Examine cards";
	static final String CARD_KEY_TEXT = "Key";
	static final String CARD_KEY_TIP = "Press, then the key to hold";
	static final String CARD_DELAY_TEXT = "Delay (ms)";
	static final String CARD_THEME_TEXT = "Card theme";
	/** The submenu of the theme lists that holds the original themes; the picture themes' groups follow it. */
	static final String ORIGINAL_TEXT = "Original";
	/** What the Card width and Card place rows say while a picture theme sets them. */
	static final String WIDTH_LOCK_TEXT = "Medium (by the theme)";
	static final String PLACE_LOCK_TEXT = "At the cursor (by the theme)";
	static final String CARD_SIZE_TEXT = "Text size";
	static final String HOVER_COLOUR_TEXT = "Hover colour";
	static final String UNDERLINE_COLOUR_TEXT = "Underline colour";
	/** The name colour row's caption in the menu; the picker it opens keeps the setting's name, {@link #NAME_COLOUR_TEXT}. */
	static final String SIDEBAR_NAMES_TEXT = "Sidebar names";
	static final String NAME_COLOUR_TEXT = "Name colour";
	/** The four groups the menu opens beside it. */
	static final String CARD_GROUP_TEXT = "Card";
	static final String WHEN_GROUP_TEXT = "When it shows";
	static final String WHAT_GROUP_TEXT = "What gets a card";
	static final String COLOURS_GROUP_TEXT = "Colours";
	/** The small headings inside the groups. */
	static final String WIDTH_HEADING = "WIDTH";
	static final String PLACE_HEADING = "PLACE";
	static final String TIME_HEADING = "TIME";
	static final String SHOW_HEADING = "SHOW";
	static final String EXAMINE_LINE_HEADING = "EXAMINE LINE";

	/** The card text size's smallest, default and largest, in px: the setting's own range. */
	static final int MIN_CARD_SIZE = 8;
	static final int DEFAULT_CARD_SIZE = 16;
	static final int MAX_CARD_SIZE = 24;
	/** The card delay's largest, in ms (its smallest is 0), and the spinner's step: the setting's own range. */
	static final int MAX_CARD_DELAY = 1000;
	static final int CARD_DELAY_STEP = 100;

	/** The top card's header, the history's header and its clear link, and what a right-click on a row offers. */
	static final String HOW_TO_TEXT = "How to use";
	static final String RECENT_TEXT = "Recent cards";
	static final String CLEAR_TEXT = "Clear";
	static final String OPEN_WIKI_TEXT = "Open wiki";
	static final String COPY_TEXT = "Copy";
	/** The words of the how-to's action line that stand in the hover colour. */
	static final String COLOURED_WORDS = "coloured name";
	/** The small heading over the how-to's rows. */
	static final String HOW_TO_HEADING = "WHAT IT WORKS ON";
	/** The how-to's action line, in white, under its bar and over the wiki line. */
	static final String HOW_TO_ACTION = "Hover the " + COLOURED_WORDS + " for its card.";
	/** The how-to's action line while "Colour hoverable words" is off, when no name is coloured, in no colour. */
	static final String HOW_TO_ACTION_PLAIN = "Hover a name in a broadcast for its card.";
	/** The how-to's second line, in grey, under the action line and over the heading. */
	static final String HOW_TO_WIKI = "Right-click it for the wiki.";
	/**
	 * The how-to's last line, in grey under its rows, its first word in bold: where the themes are. The recent cards start
	 * their own tip with the same bold word while there are none ({@link #showNextTip}).
	 */
	static final String HOW_TO_TIP_WORD = "Tip:";
	static final String HOW_TO_TIP = " change theme in settings.";
	/**
	 * The rest of each tip the empty list shows, after the bold {@link #HOW_TO_TIP_WORD}, with its leading space (the user's
	 * words, 2026-10-28; {@link TipCycle} says which one comes next): where the themes are, what an examine does, what a
	 * right-click on a row offers, what a name in chat does, what a boss's name does. The name tip has a second wording
	 * for while the names are not coloured, as the how-to's action line does ({@link #HOW_TO_ACTION_PLAIN}). Each is set in
	 * {@link #EMPTY_TIP_WIDTH} px and takes two lines at most.
	 */
	static final String TIP_THEME = " Pick a theme in the settings menu.";
	static final String TIP_EXAMINE = " Examine an item or NPC for its card.";
	static final String TIP_WIKI = " Right-click a saved card for the wiki.";
	static final String TIP_NAME = " Hover a coloured name in chat for its card.";
	static final String TIP_NAME_PLAIN = " Hover a name in a broadcast for its card.";
	static final String TIP_BOSS = " Hover a boss in chat for its card.";
	/** 1.1.0, the user's sixth tip: shown only while a picture theme is on. */
	static final String TIP_BLUR = " Use Blur behind text if words are hard to read.";
	/** The name the how-to's title bar carries, which the panel's tests find it by. */
	static final String HOW_TO_BAR_NAME = "how-to-bar";

	/** One kind of card the how-to lists: its label, shown in bold white, and the rest of its words, shown in grey. */
	static final class HowToRow
	{
		final String label;
		final String rest;

		HowToRow(String label, String rest)
		{
			this.label = label;
			this.rest = rest;
		}
	}

	/**
	 * The how-to's rows, one per kind of card, in the order they show: the label and the rest run on as one text, the rest
	 * starting with its own space or comma. The icons are in {@link #askForHowToIcon}.
	 */
	static final List<HowToRow> HOW_TO_ROWS = List.of(
		new HowToRow("Drops & pets", " in clan, friends or your chat"),
		new HowToRow("Quests, tasks, diaries", ""),
		new HowToRow("Level-ups", " and what they unlock"),
		new HowToRow("Bosses", ", kill counts, PBs"),
		new HowToRow("Examine", " any item or monster"));
	/** The name a how-to row carries and the name of the row's icon box, which the panel's tests find them by. */
	static final String HOW_TO_ROW_NAME = "how-to-row";
	static final String HOW_TO_ICON_NAME = "how-to-icon";
	/** The name the history's list carries, which the panel's tests find it by. */
	static final String HISTORY_LIST_NAME = "recent-cards-list";
	/** The count's words after the number: how many cards the session has given. */
	static final String COUNT_SUFFIX = " this session";

	/** The card's padding, in px. */
	private static final int PADDING = 8;
	/**
	 * A history row's height with a one-line title and with a two-line title (one more line), and the gap under each row
	 * (part of its list cell), in px. A row's text is laid out in lines of {@code ROW_LINE} px: the title's one or two,
	 * then the name and the kind.
	 */
	private static final int ROW_HEIGHT = 44;
	private static final int ROW_LINE = 16;
	private static final int ROW_HEIGHT_TWO_LINES = ROW_HEIGHT + ROW_LINE;
	private static final int ROW_GAP = 2;
	/** A row's picture box, in px: the picture is scaled to fit it and never drawn larger than it is. */
	private static final int PICTURE_WIDTH = 36;
	private static final int PICTURE_HEIGHT = 32;
	/** The bar at a hovered row's left edge, in px. */
	private static final int BAR_WIDTH = 2;
	/**
	 * What a row keeps clear at its left (the bar and its room), at its right, and between the picture and the text, in
	 * px, and so how much of the list's width the row's text does not have.
	 */
	private static final int ROW_LEFT_INSET = BAR_WIDTH + 4;
	private static final int ROW_RIGHT_INSET = 6;
	private static final int ROW_PICTURE_GAP = 6;
	private static final int ROW_TEXT_INSET = ROW_LEFT_INSET + PICTURE_WIDTH + ROW_PICTURE_GAP + ROW_RIGHT_INSET;
	/**
	 * A row's words: the title in bold white, the time small and light grey in the top right corner, the name in the name
	 * colour and the kind in grey under the title, and the room between the name and the kind or the time and the title,
	 * in px. The colours are the plain sidebar's ({@link SidebarSkin#PLAIN} reads the time's); a themed sidebar sets its own.
	 */
	private static final Color ROW_WHAT_COLOUR = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color ROW_TIME_COLOUR = new Color(130, 130, 130);
	private static final float ROW_TIME_SIZE = 10f;
	private static final int ROW_WORDS_GAP = 6;
	/** The history list's wheel step, in px. */
	private static final int SCROLL_STEP = 16;
	/** How often the "ago" words are refreshed while the panel is showing, in ms. */
	private static final int AGO_REFRESH_MS = 30_000;
	/** A how-to row's icon box, a square of this side in px, and the gap between it and the row's text. */
	private static final int HOW_TO_ICON_SIZE = 20;
	private static final int HOW_TO_ICON_GAP = 6;
	/** The how-to's text size, in px, for the rows and the action line. */
	private static final float HOW_TO_FONT_SIZE = 12f;
	/** The names of the two cards the recent cards' place shows: the empty list's tip, or the list. */
	private static final String TIP_CARD = "tip";
	private static final String LIST_CARD = "list";
	/**
	 * The how-to's title bar: its height, the room at each end before the title and the arrow, in px, and its ground at rest
	 * in the plain sidebar ({@link SidebarSkin#PLAIN} paints it; the bar's own background keeps it for the component to report).
	 */
	private static final int HOW_TO_BAR_HEIGHT = 30;
	private static final int HOW_TO_BAR_INSET = 10;
	static final Color HOW_TO_BAR_COLOUR = new Color(50, 50, 50);
	/** The room between the support row and the bar, and between the bar and the action line, in px. */
	private static final int HOW_TO_BAR_ABOVE = PADDING;
	private static final int HOW_TO_BAR_BELOW = 10;
	/** The item the first row's icon shows, the Casket (a small treasure chest, the user's pick), and its stack. */
	private static final int CASKET_ITEM_ID = 405;
	private static final int CASKET_STACK = 1;
	/** The how-to heading's text size, in px, and its grey in the plain sidebar. */
	private static final float HOW_TO_HEADING_SIZE = 10f;
	static final Color HOW_TO_HEADING_COLOUR = new Color(125, 125, 125);
	/** The gap between two how-to rows, above the heading and between the heading and the first row, in px. */
	private static final int HOW_TO_ROW_GAP = 4;
	private static final int HOW_TO_HEADING_ABOVE = 8;
	private static final int HOW_TO_HEADING_BELOW = 4;
	/** The room above a row's text so that its first line sits level with the middle of the icon box, in px. */
	private static final int HOW_TO_TEXT_DROP = 2;
	/**
	 * How wide the how-to's full-width text (the action line) is set, in px: the width RuneLite gives a panel that is not
	 * wrapped (its panel width and the scroll bar's) less the panel's and the card's padding and a few px to spare, so that
	 * a line never runs past the card's right edge. A row's text is this less its icon box and the gap.
	 */
	private static final int HOW_TO_WIDTH = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH - 4 * PADDING - 4;
	private static final int HOW_TO_ROW_TEXT_WIDTH = HOW_TO_WIDTH - HOW_TO_ICON_SIZE - HOW_TO_ICON_GAP;
	/**
	 * How wide the empty list's tip is set, in px: the list's width (the same panel width less the panel's padding alone,
	 * no card around it) less a few px to spare. The shortest tip's words ({@link #TIP_THEME}) are 212 px wide in the
	 * client's font: more than the how-to's {@link #HOW_TO_WIDTH}, which would break them in two, and less than this, which
	 * keeps them on one line; a longer tip breaks in two.
	 */
	private static final int EMPTY_TIP_WIDTH = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH - 2 * PADDING - 4;
	/** A row's border at rest and under the mouse; the bar takes the room the resting border leaves empty. */
	private static final Border ROW_BORDER = new EmptyBorder(0, ROW_LEFT_INSET, 0, ROW_RIGHT_INSET);
	private static final Border ROW_HOT_BORDER = new CompoundBorder(
		new MatteBorder(0, BAR_WIDTH, 0, 0, ColorScheme.BRAND_ORANGE), new EmptyBorder(0, ROW_LEFT_INSET - BAR_WIDTH, 0,
			ROW_RIGHT_INSET));
	/** The settings icon's size, in px. */
	private static final int ICON_SIZE = 12;
	/** A press on the settings icon this soon after the menu closed is the press that closed it, not a new open. */
	private static final long REOPEN_GUARD_NANOS = 250_000_000L;
	/** A colour swatch's side, in px. */
	private static final int SWATCH_SIZE = 16;
	/** The room above a group's heading, and between two small buttons of a row, in px. */
	private static final int GROUP_HEADING_ABOVE = 6;
	private static final int SEGMENT_GAP = 2;
	/** The least room between a row's caption and its control, in px, so that a narrow group never joins them. */
	private static final int ROW_CAPTION_GAP = 10;
	/** What the card key box says while it waits for the key. */
	private static final String KEY_PROMPT = "Press a key";

	private final Runnable resetToDefault;
	private final PanelSettings settings;
	private final ColourPicker picker;
	private final CardHistory history;
	private final CardViewer viewer;
	private final IconSource icons;
	private final Consumer<String> browse;
	private final Consumer<String> copy;
	private final SupportLinks support;
	private final JCheckBoxMenuItem showCheck;
	private final JCheckBoxMenuItem colourCheck;
	private final JCheckBoxMenuItem wikiCheck;
	private final JCheckBoxMenuItem picturesCheck;
	private final JSlider frostSlider;
	/** The blur slider's number, beside it, live while it is dragged. */
	private final JLabel frostValue;
	/** The Blur behind text row of the menu, under Card theme: greyed, with its own hover text, while an original theme is on. */
	private JPanel frostRow;
	private final JCheckBoxMenuItem bossCheck;
	private final JCheckBoxMenuItem underlineCheck;
	private final JCheckBoxMenuItem examineCheck;
	private final JButton keyBox;
	private final ValueMenu themeMenu;
	private final ValueMenu sidebarThemeMenu;
	private final JSpinner delaySpinner;
	private final JSpinner sizeSpinner;
	private final JPopupMenu menu;
	/** The "Colours" group, which the menu comes back with open when a colour picker closes; set when the menu is built. */
	private JMenu coloursGroup;
	/** The Card width and Card place rows, and the greyed words that stand in for them while a picture theme sets them. */
	private JPanel widthRow;
	private JLabel widthLock;
	private List<JRadioButtonMenuItem> placeItems;
	private JLabel placeLock;
	/** What sets each choice (a theme row, a group's radio items or small buttons) from its setting. */
	private final List<Runnable> choiceSyncs = new ArrayList<>();
	/** What sets each colour swatch from its setting. */
	private final List<Runnable> swatchSyncs = new ArrayList<>();
	/** The settings icon the menu was last opened under, which a colour picker's closing shows it under again. */
	private JLabel menuAnchor;
	private long closedAtNanos = System.nanoTime() - 2 * REOPEN_GUARD_NANOS;
	/** True while the menu's controls are being set from the settings, so that setting them writes nothing back. */
	private boolean syncing;
	/** The key the card key box shows when it is not waiting: the setting's, or the one just taken. */
	private Keybind shownKey = Keybind.NOT_SET;
	/** True while the card key box waits for the key to take. */
	private boolean keyArmed;
	/**
	 * The item of a theme list (the card theme's or the sidebar theme's) that is armed and whose picture is up, or null; and
	 * whether a picture has been handed to the viewer and not yet taken down.
	 */
	private JMenuItem previewItem;
	private boolean previewUp;
	/** The whip's picture, the sample card's, once the icon source has given it; asked for once, at the first sample card. */
	private volatile BufferedImage whip;
	private boolean whipAsked;
	/** The skin the sidebar is painted with now: the one the sidebar theme names; see {@link #applySkin}. */
	private SidebarSkin skin;
	/** The skin whose colours the labels and the grounds wear now, or null before the first dressing. */
	private SidebarSkin dressed;
	/** The how-to's action line, which {@link #refreshHowTo} sets, and the box of that line, the wiki line, the heading and the rows. */
	private final JLabel howToAction = new JLabel();
	private final JPanel howToLines = new JPanel();
	/** The how-to bar's title, the second line, the small heading, the text of each row (in the rows' order) and the examine row's icon box. */
	private final JLabel howToTitle = new JLabel();
	private final JLabel howToWiki = new JLabel();
	private final JLabel howToHeading = new JLabel();
	private final JLabel howToTip = new JLabel();
	private final List<JLabel> howToRowTexts = new ArrayList<>();
	private JLabel examineIconBox;
	/** The how-to bar's fold arrow. */
	private final JLabel foldArrow = new JLabel();
	/** Whether the how-to is folded to its bar, as last shown. */
	private boolean howToFolded;
	/** Whether the mouse is on the how-to bar. */
	private boolean barHot;
	/** The recent cards' header: its title, its count, "N this session", and its clear link. */
	private final JLabel recentTitle = new JLabel();
	private final JLabel count = label("", 11f, ColorScheme.LIGHT_GRAY_COLOR);
	private final JLabel clearLabel = new JLabel();
	/**
	 * The line the recent cards show while there are none, in the skin's grey: "Tip:" in bold and one of the tips
	 * ({@link #TIP_THEME} and the others), in the middle of the list's place, across and down (the user's pick, option B of
	 * docs/empty-tip-options-2.png), shown instead of the list while it is empty ({@link #syncList}). It changes to the next
	 * tip each time the sidebar is opened while the list is empty, and when the list is cleared ({@link #showNextTip}).
	 */
	private final JLabel emptyTip = new JLabel();
	/** Which tip the empty list shows next; a new order for each run of the client. */
	private final TipCycle tips = new TipCycle(new Random());
	/** The scroll pane of the recent cards, made with the history's view. */
	private JScrollPane historyScroll;
	/** The recent cards' place under their header: the card of {@link #emptyTip} or the card of {@link #historyScroll}. */
	private JPanel historyBody;
	/** The history's entries as the list shows them, newest first; changed only by {@link #syncList}. */
	private final HistoryModel model = new HistoryModel();
	/** The list of recent cards, which the history's scroll pane shows; a change of its width has every row measured again. */
	private final JList<CardHistory.Entry> historyList = new HistoryList(model, model::measureAgain);
	/**
	 * The picture of each entry in the model, by identity: the entry is a key from the moment it enters the model until it
	 * leaves it, its value null until the picture comes.
	 */
	private final Map<CardHistory.Entry, ImageIcon> pictures = new IdentityHashMap<>();
	/**
	 * How many lines (one or two) the title of each entry in the model takes, by identity, worked out once per entry for
	 * the list width in {@link #measuredWidth} and the Card time setting in {@link #measuredTime}: the entry is a key from
	 * the first time its row is drawn or measured until it leaves the model, and every key goes when the list's width or
	 * the setting changes.
	 */
	private final Map<CardHistory.Entry, Integer> titleLines = new IdentityHashMap<>();
	/** The list width, in px, that the counts in {@link #titleLines} were worked out for. */
	private int measuredWidth = -1;
	/** The Card time setting that the counts in {@link #titleLines} were worked out for, or null before the first count. */
	private CardTime measuredTime;
	/**
	 * The Card time setting {@link #settingsChanged} last saw, or null before its first call. It is not
	 * {@link #measuredTime}: a draw between the setting's change and the call counts the drawn rows again on its own and
	 * moves that one, which would hide the change from the call and keep the list's old heights.
	 */
	private CardTime notifiedTime;
	/** The entry the mouse is on, so that its cell draws hot and its card is shown, or null. */
	private CardHistory.Entry hovered;
	/** True while a {@link #syncList} is queued for the event thread, so that a burst of lines asks for one. */
	private final AtomicBoolean syncQueued = new AtomicBoolean();
	/** Repaints the list, so that the "ago" words are read anew, while the panel is showing. */
	private final Timer agoTimer = new Timer(AGO_REFRESH_MS, e -> refreshAgo());

	/**
	 * The panel: the top card, the recent cards under it, and the settings menu.
	 *
	 * <p>Choice: the panel is not wrapped ({@code super(false)}), so that it lays out its own scrolling: the top card
	 * stays put while only the history scrolls, which RuneLite's wrapping, one scroll pane around the whole panel, would
	 * not allow.
	 *
	 * @param resetToDefault what "Reset to default" does: puts the plugin's settings back to their defaults
	 * @param settings the settings the menu's controls show, read each time the menu opens and after "Reset to default",
	 *        and written when a control changes; also whether the how-to is folded
	 * @param picker opens the colour picker a colour swatch asks for
	 * @param history the recent cards the list shows, listened to for every change
	 * @param viewer what shows a hovered row's card beside the sidebar and takes it down
	 * @param icons where a row gets its picture
	 * @param browse what "Open wiki" does with the card's wiki address
	 * @param copy what "Copy" does with the card's line
	 */
	ClanHubPanel(Runnable resetToDefault, PanelSettings settings, ColourPicker picker, CardHistory history,
		CardViewer viewer, IconSource icons, Consumer<String> browse, Consumer<String> copy)
	{
		super(false);
		this.resetToDefault = resetToDefault;
		this.settings = settings;
		this.picker = picker;
		this.history = history;
		this.viewer = viewer;
		this.icons = icons;
		this.browse = browse;
		this.copy = copy;
		this.skin = skinNow();
		this.support = new SupportLinks("2h Chat Hover Info", Version.CURRENT, GITHUB_URL, LinkBrowser::browse, this::closeMenu);
		this.showCheck = check(SHOW_CARDS_TEXT, settings::setShowCards);
		this.colourCheck = check(COLOUR_TEXT, on ->
		{
			settings.setColourHoverables(on);
			refreshHowTo();
		});
		this.wikiCheck = check(WIKI_TEXT, settings::setWikiDescriptions);
		this.picturesCheck = check(PICTURES_TEXT, settings::setWikiPictures);
		this.frostSlider = new JSlider(MIN_FROST, MAX_FROST, DEFAULT_FROST);
		this.frostValue = new JLabel(String.valueOf(DEFAULT_FROST));
		frostSlider.setOpaque(false);
		frostSlider.setFocusable(false);
		frostSlider.setPreferredSize(new Dimension(FROST_SLIDER_WIDTH, frostSlider.getPreferredSize().height));
		frostSlider.setMaximumSize(frostSlider.getPreferredSize());
		frostValue.setFont(menuFont());
		frostValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		// The number is as wide as "100", so the slider does not move as it changes, and ends at the row's right edge,
		// under the arrows of the rows around it (the box layout sizes a label to its maximum, so that is set too).
		final Dimension numberBox = new Dimension(frostValue.getFontMetrics(frostValue.getFont()).stringWidth("100") + 2,
			frostValue.getPreferredSize().height);
		frostValue.setPreferredSize(numberBox);
		frostValue.setMaximumSize(numberBox);
		frostValue.setHorizontalAlignment(SwingConstants.RIGHT);
		frostSlider.addChangeListener(e ->
		{
			frostValue.setText(String.valueOf(frostSlider.getValue()));
			// Saved when the drag ends (once, not on every step of it); a key press or a click saves at once.
			if (!syncing && !frostSlider.getValueIsAdjusting())
			{
				settings.setFrostOpacity(frostSlider.getValue());
			}
		});
		this.bossCheck = check(BOSS_TEXT, settings::setBossCards);
		this.underlineCheck = check(ALWAYS_UNDERLINE_TEXT, settings::setAlwaysUnderlineLevels);
		this.examineCheck = check(EXAMINE_TEXT, settings::setExamineCards);
		this.keyBox = buildKeyBox();
		this.themeMenu = themeChoices(CARD_THEME_TEXT, cardPicks(), () ->
			PictureThemes.current().byId(settings.pictureTheme()) != null ? settings.pictureTheme() : settings.cardTheme(),
			key ->
			{
				if (key instanceof CardTheme)
				{
					settings.setPictureTheme("");
					settings.setCardTheme((CardTheme) key);
				}
				else
				{
					settings.setPictureTheme((String) key);
				}
				settingsChanged();
			}, key -> ThemePreviews.card(key instanceof CardTheme ? CardStyle.of((CardTheme) key)
				: CardStyle.of(CardTheme.CLASSIC, (String) key), whipPicture()));
		this.sidebarThemeMenu = themeChoices(SIDEBAR_THEME_TEXT, sidebarPicks(), () ->
			PictureThemes.current().byId(settings.sidebarPicture()) != null ? settings.sidebarPicture() : settings.sidebarLook(),
			key ->
			{
				if (key instanceof SidebarTheme)
				{
					settings.setSidebarPicture("");
					settings.setSidebarLook((SidebarTheme) key);
				}
				else
				{
					settings.setSidebarPicture((String) key);
				}
				settingsChanged();
			}, key -> ThemePreviews.sidebar(key instanceof SidebarTheme
				? SidebarSkin.pick((SidebarTheme) key, settings.cardTheme(), settings.pictureTheme(), "")
				: SidebarSkin.ofPicture(PictureThemes.current().byId((String) key))));
		this.delaySpinner = spinner(0, 0, MAX_CARD_DELAY, CARD_DELAY_STEP, 4, settings::setCardDelayMs);
		this.sizeSpinner = spinner(DEFAULT_CARD_SIZE, MIN_CARD_SIZE, MAX_CARD_SIZE, 1, 2, settings::setCardFontSize);
		this.menu = buildMenu();

		setLayout(new BorderLayout(0, PADDING));
		setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
		add(topCard(), BorderLayout.NORTH);
		add(historyView(), BorderLayout.CENTER);
		applyFold(settings.howToFolded());
		applySkin();
		history.listen(this::queueSync);
		syncList();
	}

	/**
	 * A label with its font, size and colour set explicitly, never left to the look and feel: the menu's 12 px plain face
	 * is {@code size} 12, the headers' bold one is made by {@link #boldLabel}.
	 */
	private static JLabel label(String text, float size, Color colour)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getDefaultFont().deriveFont(size));
		label.setForeground(colour);
		return label;
	}

	/** A label in the bold face at {@code size} px. */
	private static JLabel boldLabel(String text, float size, Color colour)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getDefaultBoldFont().deriveFont(size));
		label.setForeground(colour);
		return label;
	}

	/**
	 * The top card, fixed: the top row with the support pair at its right end, then the "How to use" box, whose bar runs
	 * the card's whole inner width.
	 */
	private JPanel topCard()
	{
		final JPanel card = new JPanel(new BorderLayout())
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				paintSkin(g, this, SidebarSkin.Box.CARD, false);
			}
		};
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		final JPanel topRow = new JPanel(new BorderLayout());
		topRow.setOpaque(false);
		topRow.add(support.supportPair(settingsIcon()), BorderLayout.EAST);
		card.add(topRow, BorderLayout.NORTH);
		card.add(howTo(), BorderLayout.CENTER);
		return card;
	}

	/**
	 * The "How to use" box: a title bar across the card, "How to use" at its left and the fold arrow at its right, over
	 * the action line in two parts (white, then grey), the small heading "WHAT IT WORKS ON", the icon rows and the grey
	 * tip ("Tip:" in bold) that the themes are in the settings. A press
	 * anywhere on the bar folds the lines away or brings them back and writes which; the arrow points down while they show
	 * and right while they are folded. The bar's ground is a shade lighter than the card's, and lighter again under the
	 * mouse; the skin paints it ({@link #paintSkin}), and the bar keeps the plain look's two grounds as its background,
	 * which is what it reports.
	 *
	 * <p>Choice: the bar's title and arrow take no mouse listener of their own, so a press anywhere on the bar, and the
	 * mouse entering or leaving it, reaches the bar's one listener.
	 */
	private JPanel howTo()
	{
		final JPanel box = new JPanel(new BorderLayout());
		box.setOpaque(false);
		box.setBorder(new EmptyBorder(HOW_TO_BAR_ABOVE, 0, 0, 0));

		final JPanel bar = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getPreferredSize()
			{
				return new Dimension(super.getPreferredSize().width, HOW_TO_BAR_HEIGHT);
			}

			@Override
			protected void paintComponent(Graphics g)
			{
				paintSkin(g, this, SidebarSkin.Box.BAR, barHot);
			}
		};
		bar.setName(HOW_TO_BAR_NAME);
		bar.setBackground(HOW_TO_BAR_COLOUR);
		bar.setBorder(new EmptyBorder(0, HOW_TO_BAR_INSET, 0, HOW_TO_BAR_INSET));
		bar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		howToTitle.setText(HOW_TO_TEXT);
		howToTitle.setFont(FontManager.getDefaultBoldFont().deriveFont(13f));
		bar.add(howToTitle, BorderLayout.WEST);
		bar.add(foldArrow, BorderLayout.EAST);
		bar.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				barHot = true;
				bar.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
				bar.repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				barHot = false;
				bar.setBackground(HOW_TO_BAR_COLOUR);
				bar.repaint();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					final boolean folded = !howToFolded;
					applyFold(folded);
					settings.setHowToFolded(folded);
				}
			}
		});
		box.add(bar, BorderLayout.NORTH);

		howToLines.setLayout(new BoxLayout(howToLines, BoxLayout.Y_AXIS));
		howToLines.setOpaque(false);
		howToLines.add(Box.createVerticalStrut(HOW_TO_BAR_BELOW));
		howToAction.setFont(FontManager.getDefaultFont().deriveFont(HOW_TO_FONT_SIZE));
		howToLines.add(actionLine(howToAction));
		howToWiki.setFont(FontManager.getDefaultFont().deriveFont(HOW_TO_FONT_SIZE));
		howToWiki.setText(howToHtml(List.of(new Run(HOW_TO_WIKI, "", "", howToWiki.getFontMetrics(howToWiki.getFont()))),
			HOW_TO_WIDTH));
		howToLines.add(actionLine(howToWiki));
		howToLines.add(Box.createVerticalStrut(HOW_TO_HEADING_ABOVE));
		howToHeading.setText(HOW_TO_HEADING);
		howToHeading.setFont(FontManager.getDefaultBoldFont().deriveFont(HOW_TO_HEADING_SIZE));
		howToHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
		howToLines.add(howToHeading);
		howToLines.add(Box.createVerticalStrut(HOW_TO_HEADING_BELOW));
		for (int i = 0; i < HOW_TO_ROWS.size(); i++)
		{
			howToLines.add(howToRow(i));
			howToLines.add(Box.createVerticalStrut(HOW_TO_ROW_GAP));
		}
		writeTip(howToTip, HOW_TO_TIP, HOW_TO_WIDTH, false);
		howToLines.add(actionLine(howToTip));
		refreshHowTo();
		box.add(howToLines, BorderLayout.CENTER);
		return box;
	}

	/**
	 * One of the how-to's lines (the two action lines, the tip) in a box of its own, so that it fills the card's width and
	 * sits at its left.
	 */
	private static JPanel actionLine(JLabel line)
	{
		line.setVerticalAlignment(SwingConstants.TOP);
		final JPanel box = new JPanel(new BorderLayout());
		box.setOpaque(false);
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.add(line, BorderLayout.CENTER);
		return box;
	}

	/**
	 * Gives {@code line} a theme tip: the how-to's text size, {@link #HOW_TO_TIP_WORD} in bold and {@code rest} after it,
	 * set like the how-to's other lines ({@link #howToHtml}, no wider than {@code width} px, so on one line where it fits).
	 * The how-to's last line ({@link #HOW_TO_TIP}, in {@link #HOW_TO_WIDTH}) and the empty list's (one of the
	 * {@code TIP_} words, in {@link #EMPTY_TIP_WIDTH}) are both made here, so that they look the same. The colour is the
	 * label's own, which {@link #applySkin} sets.
	 *
	 * @param rest the words after "Tip:", with their leading space
	 * @param width the widest the line may be, in px
	 * @param centred whether each line of a tip that breaks in two is centred (the empty list's), or stays at the left
	 */
	private static void writeTip(JLabel line, String rest, int width, boolean centred)
	{
		line.setFont(FontManager.getDefaultFont().deriveFont(HOW_TO_FONT_SIZE));
		final Font tipWord = FontManager.getDefaultBoldFont().deriveFont(HOW_TO_FONT_SIZE);
		line.setText(howToHtml(List.of(new Run(HOW_TO_TIP_WORD, "<b>", "</b>", line.getFontMetrics(tipWord)),
			new Run(rest, "", "", line.getFontMetrics(line.getFont()))), width, centred));
	}

	/**
	 * Shows the next tip in the empty list's place, from the cycle: the tips whose feature is off (the settings are read
	 * now) are left out, and the name tip reads as it does for the names as they are coloured or not. Event thread only.
	 */
	private void showNextTip()
	{
		final TipCycle.Tip tip = tips.next(settings.examineCards(), settings.bossCards(),
			CardStyle.of(settings.cardTheme(), settings.pictureTheme()).picture != null);
		writeTip(emptyTip, tipRest(tip, settings.colourHoverables()), EMPTY_TIP_WIDTH, true);
	}

	/** The words of {@code tip} after "Tip:", for names that are coloured or not. */
	private static String tipRest(TipCycle.Tip tip, boolean colourNames)
	{
		switch (tip)
		{
			case EXAMINE:
				return TIP_EXAMINE;
			case WIKI:
				return TIP_WIKI;
			case NAME:
				return colourNames ? TIP_NAME : TIP_NAME_PLAIN;
			case BOSS:
				return TIP_BOSS;
			case BLUR:
				return TIP_BLUR;
			default:
				return TIP_THEME;
		}
	}

	/**
	 * One row of the how-to, number {@code index} of {@link #HOW_TO_ROWS}: an icon box of {@link #HOW_TO_ICON_SIZE} px at
	 * the left, empty until its picture comes, and beside it the row's label in bold white and its rest in grey, wrapped to
	 * the room the icon leaves. The picture is asked for here, once; the words are set by {@link #styleHowToRow}.
	 */
	private JPanel howToRow(int index)
	{
		final JPanel panel = new JPanel(new BorderLayout(HOW_TO_ICON_GAP, 0));
		panel.setName(HOW_TO_ROW_NAME);
		panel.setOpaque(false);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		final JLabel icon = new JLabel();
		icon.setName(HOW_TO_ICON_NAME);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.TOP);
		icon.setPreferredSize(new Dimension(HOW_TO_ICON_SIZE, HOW_TO_ICON_SIZE));
		panel.add(icon, BorderLayout.WEST);
		askForHowToIcon(index, icon);

		final JLabel text = label("", HOW_TO_FONT_SIZE, ColorScheme.LIGHT_GRAY_COLOR);
		text.setVerticalAlignment(SwingConstants.TOP);
		text.setBorder(new EmptyBorder(HOW_TO_TEXT_DROP, 0, 0, 0));
		howToRowTexts.add(text);
		panel.add(text, BorderLayout.CENTER);
		return panel;
	}

	/**
	 * Sets the words of how-to row number {@code index} in the skin's colours: the label in bold in the skin's text colour
	 * and the rest in its grey, wrapped to the room the icon leaves. Again, as the skin changes.
	 *
	 * <p>Choice: the label is a bold run of the one HTML text, so that the rest wraps beside it as running text, and the
	 * words are measured for the wrap in the client's bold face; RuneLite registers that face with the graphics
	 * environment, which is what lets Swing's HTML draw {@code <b>} in it. The rest is in the label's own foreground.
	 */
	private void styleHowToRow(int index)
	{
		final HowToRow row = HOW_TO_ROWS.get(index);
		final JLabel text = howToRowTexts.get(index);
		text.setForeground(skin.grey);
		final Font bold = FontManager.getDefaultBoldFont().deriveFont(HOW_TO_FONT_SIZE);
		final List<Run> runs = new ArrayList<>();
		runs.add(new Run(row.label, "<b><font color='" + hex(skin.text) + "'>", "</font></b>", text.getFontMetrics(bold)));
		if (!row.rest.isEmpty())
		{
			runs.add(new Run(row.rest, "", "", text.getFontMetrics(text.getFont())));
		}
		text.setText(howToHtml(runs, HOW_TO_ROW_TEXT_WIDTH));
	}

	/** {@code colour} as the {@code #rrggbb} of an HTML colour. */
	private static String hex(Color colour)
	{
		return String.format("#%02x%02x%02x", colour.getRed(), colour.getGreen(), colour.getBlue());
	}

	/**
	 * Asks the icon source for the picture of how-to row {@code index} and puts it in {@code box} when it comes: the
	 * Casket (at a stack of 1) for the drops, the quest sprite, the total level sprite for the level-ups, the combat sprite for the
	 * bosses, and for the examine row a magnifying glass drawn in code. The row's order is {@link #HOW_TO_ROWS}'s.
	 */
	private void askForHowToIcon(int index, JLabel box)
	{
		final Consumer<BufferedImage> picture = pictureIn(box, HOW_TO_ICON_SIZE, HOW_TO_ICON_SIZE);
		switch (index)
		{
			case 0:
				icons.item(CASKET_ITEM_ID, CASKET_STACK, picture);
				break;
			case 1:
				icons.sprite(SkillSprites.QUEST, picture);
				break;
			case 2:
				icons.sprite(SkillSprites.spriteOf("Total"), picture);
				break;
			case 3:
				icons.sprite(SkillSprites.spriteOf("Combat"), picture);
				break;
			default:
				examineIconBox = box;
				box.setIcon(ExamineGlyph.icon(HOW_TO_ICON_SIZE, skin.grey));
				break;
		}
	}

	/**
	 * Sets the how-to's action line, in white: {@link #HOW_TO_ACTION} wrapped to {@link #HOW_TO_WIDTH} px and written as
	 * HTML, the {@link #COLOURED_WORDS} in the hover colour as the settings have it now. While "Colour hoverable words" is
	 * off no name is coloured, so the line is {@link #HOW_TO_ACTION_PLAIN}, in no colour.
	 *
	 * <p>Choice: the line is wrapped here, by the width of its words in the label's own font, and joined with line breaks,
	 * and no width is written into the HTML: Swing's HTML reads a CSS width in px as 1.3 times what it says (it was
	 * measured), which would let a line run past the card's right edge.
	 */
	private void refreshHowTo()
	{
		final FontMetrics metrics = howToAction.getFontMetrics(howToAction.getFont());
		final List<Run> runs = new ArrayList<>();
		if (settings.colourHoverables())
		{
			final Color hover = settings.hoverColour();
			final String coloured = String.format("#%02x%02x%02x", hover.getRed(), hover.getGreen(), hover.getBlue());
			final int at = HOW_TO_ACTION.indexOf(COLOURED_WORDS);
			final int end = at + COLOURED_WORDS.length();
			runs.add(new Run(HOW_TO_ACTION.substring(0, at), "", "", metrics));
			runs.add(new Run(COLOURED_WORDS, "<font color='" + coloured + "'>", "</font>", metrics));
			runs.add(new Run(HOW_TO_ACTION.substring(end), "", "", metrics));
		}
		else
		{
			runs.add(new Run(HOW_TO_ACTION_PLAIN, "", "", metrics));
		}
		howToAction.setText(howToHtml(runs, HOW_TO_WIDTH));
	}

	/**
	 * A stretch of a how-to text in one look: its words, the HTML that opens and closes it (both empty for the label's own
	 * look), and the metrics of the font the words are drawn in, which the wrapping measures them with.
	 */
	private static final class Run
	{
		final String text;
		final String open;
		final String close;
		final FontMetrics metrics;

		Run(String text, String open, String close, FontMetrics metrics)
		{
			this.text = text;
			this.open = open;
			this.close = close;
			this.metrics = metrics;
		}
	}

	/**
	 * The runs as one HTML text: the words broken into lines no wider than {@code maxWidth} px, each word measured in its
	 * own run's font (a word wider than that has a line to itself), joined with line breaks, every run's tags opened and
	 * closed again on each line it reaches, so that a run split across two lines keeps its look on both. The space a line
	 * breaks at is dropped.
	 */
	private static String howToHtml(List<Run> runs, int maxWidth)
	{
		return howToHtml(runs, maxWidth, false);
	}

	/**
	 * {@link #howToHtml(List, int)}, with every line centred across the text's width when {@code centred} (the lines of a
	 * label whose text breaks in two otherwise stay at the left of the block the label centres).
	 */
	private static String howToHtml(List<Run> runs, int maxWidth, boolean centred)
	{
		final StringBuilder all = new StringBuilder();
		for (final Run run : runs)
		{
			all.append(run.text);
		}
		final String text = all.toString();
		final StringBuilder html = new StringBuilder(centred ? "<html><div style='text-align:center'>" : "<html>");
		int from = 0;
		while (from < text.length())
		{
			// [from, to) is the words that fit on this line; the first word is taken whatever its width.
			int to = from;
			int next = from;
			while (next < text.length())
			{
				final int space = text.indexOf(' ', next);
				final int wordEnd = space < 0 ? text.length() : space;
				if (to > from && widthOf(runs, from, wordEnd) > maxWidth)
				{
					break;
				}
				to = wordEnd;
				next = space < 0 ? text.length() : space + 1;
			}
			appendLine(html, runs, from, to);
			if (to < text.length())
			{
				html.append("<br>");
			}
			from = to + 1;
		}
		return html.append(centred ? "</div></html>" : "</html>").toString();
	}

	/** How wide the characters {@code from} to {@code to} of the runs' text are, each run's part in its own font. */
	private static int widthOf(List<Run> runs, int from, int to)
	{
		int width = 0;
		int at = 0;
		for (final Run run : runs)
		{
			final int runFrom = Math.max(from, at);
			final int runTo = Math.min(to, at + run.text.length());
			if (runFrom < runTo)
			{
				width += run.metrics.stringWidth(run.text.substring(runFrom - at, runTo - at));
			}
			at += run.text.length();
		}
		return width;
	}

	/** Adds the characters {@code from} to {@code to} of the runs' text to {@code html}, each run's part in its own tags. */
	private static void appendLine(StringBuilder html, List<Run> runs, int from, int to)
	{
		int at = 0;
		for (final Run run : runs)
		{
			final int runFrom = Math.max(from, at);
			final int runTo = Math.min(to, at + run.text.length());
			if (runFrom < runTo)
			{
				html.append(run.open).append(run.text, runFrom - at, runTo - at).append(run.close);
			}
			at += run.text.length();
		}
	}

	/** Shows the how-to folded to its bar or open, writing nothing: the lines, the arrow and the card's height follow. */
	private void applyFold(boolean folded)
	{
		howToFolded = folded;
		howToLines.setVisible(!folded);
		foldArrow.setIcon(new FoldArrow(!folded, skin.grey));
		revalidate();
		repaint();
	}

	/**
	 * The recent cards: a header row ("Recent cards", the count, "Clear" at the right) over the scroll pane of the list,
	 * which has no horizontal bar, a vertical one as needed and {@link #SCROLL_STEP} px wheel steps. While the list is
	 * empty, its place shows a tip instead, in the middle ({@link #emptyTip}).
	 *
	 * <p>The list: every cell {@code ROW_HEIGHT + ROW_GAP} px high, or {@code ROW_HEIGHT_TWO_LINES + ROW_GAP} when its
	 * title takes two lines, drawn by the one {@link RowRenderer}, with no selection painted (the keyboard never reaches
	 * it). The mouse is read on the list itself, since a list has no component per row: where it moves, which cell it is
	 * on and so which card the viewer shows; a popup trigger offers the row's menu.
	 *
	 * <p>Choice: the list's cells have a fixed width of 0, and the list is made as wide as the scroll pane's view, so that
	 * the list never asks the renderer for the width of every row (which a list does to size itself when its cells' width
	 * is not fixed, and which would read every row of a session); a cell's width is then the list's, which is what cuts a
	 * long line with "..." and keeps the horizontal bar away. Their height is not fixed, as a long title takes a second
	 * line, so the list asks the renderer for each row's height; that is a lookup of the line count the renderer worked out
	 * once for the entry (see {@link RowRenderer}).
	 */
	private JPanel historyView()
	{
		final JPanel view = new JPanel(new BorderLayout());
		view.setOpaque(false);

		final JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
		header.setOpaque(false);
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		recentTitle.setText(RECENT_TEXT);
		recentTitle.setFont(FontManager.getDefaultBoldFont().deriveFont(13f));
		header.add(recentTitle);
		header.add(Box.createHorizontalStrut(8));
		header.add(count);
		header.add(Box.createHorizontalGlue());
		header.add(clearLink());
		view.add(header, BorderLayout.NORTH);

		historyList.setName(HISTORY_LIST_NAME);
		historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		historyList.setFocusable(false);
		historyList.setFixedCellWidth(0);
		historyList.setCellRenderer(new RowRenderer());
		final MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				hoverAt(e.getPoint());
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				hoverAt(e.getPoint());
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hoverAt(null);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				offerMenu(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				offerMenu(e);
			}
		};
		historyList.addMouseListener(mouse);
		historyList.addMouseMotionListener(mouse);
		historyScroll = new JScrollPane(historyList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		historyScroll.setBorder(null);
		historyScroll.getVerticalScrollBar().setUnitIncrement(SCROLL_STEP);
		showNextTip();
		emptyTip.setHorizontalAlignment(SwingConstants.CENTER);
		emptyTip.setVerticalAlignment(SwingConstants.CENTER);
		historyBody = new JPanel(new CardLayout());
		historyBody.setOpaque(false);
		historyBody.add(emptyTip, TIP_CARD);
		historyBody.add(historyScroll, LIST_CARD);
		view.add(historyBody, BorderLayout.CENTER);
		return view;
	}

	/** The history header's "Clear": grey at rest, in the skin's text colour under the mouse; a left press empties the history. */
	private JLabel clearLink()
	{
		final JLabel clear = clearLabel;
		clear.setText(CLEAR_TEXT);
		clear.setFont(FontManager.getDefaultFont().deriveFont(12f));
		clear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		clear.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				clear.setForeground(skin.text);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				clear.setForeground(skin.grey);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger())
				{
					history.clear();
				}
			}
		});
		return clear;
	}

	/** Asks for one {@link #syncList} on the event thread, unless one is already queued; may be called on any thread. */
	private void queueSync()
	{
		if (syncQueued.compareAndSet(false, true))
		{
			SwingUtilities.invokeLater(this::syncList);
		}
	}

	/**
	 * Brings the list up to date with the history, from one read of it. The entries in front of the model's first one (by
	 * identity) are the new ones: they are inserted at the top in one step, in order, and each new entry's picture is asked
	 * for, once. The model is then cut to the history's size from the bottom, and the cut entries leave the pictures and
	 * the line counts. A
	 * history that is empty, or that no longer holds the model's first entry (it was cleared and filled again), empties the
	 * model first. The count is set, and the tip shows in the list's place only while the model is empty; a list that
	 * has just become empty (cleared) shows the next tip. If the entry the mouse was on is gone,
	 * the card shown for it comes down: the row is gone, and no exit would ever tell the viewer. Runs on the event thread.
	 *
	 * <p>Choice: the queued flag is let go before the history is read, so that a line that comes while this runs asks for a
	 * sync of its own and is never lost.
	 */
	private void syncList()
	{
		syncQueued.set(false);
		final boolean wasEmpty = model.isEmpty();
		final List<CardHistory.Entry> entries = history.entries();
		int fresh = entries.size();
		if (!model.isEmpty())
		{
			final int top = identityIndex(entries, model.get(0));
			if (top < 0)
			{
				pictures.clear();
				titleLines.clear();
				model.clear();
			}
			else
			{
				fresh = top;
			}
		}
		if (fresh > 0)
		{
			final List<CardHistory.Entry> added = new ArrayList<>(entries.subList(0, fresh));
			for (final CardHistory.Entry entry : added)
			{
				pictures.put(entry, null);
			}
			model.addAll(0, added);
			for (final CardHistory.Entry entry : added)
			{
				askForPicture(entry);
			}
		}
		final int size = entries.size();
		if (model.size() > size)
		{
			for (int i = size; i < model.size(); i++)
			{
				pictures.remove(model.get(i));
				titleLines.remove(model.get(i));
			}
			model.removeRange(size, model.size() - 1);
		}
		count.setText(history.size() + COUNT_SUFFIX);
		if (model.isEmpty() && !wasEmpty)
		{
			showNextTip();
		}
		((CardLayout) historyBody.getLayout()).show(historyBody, model.isEmpty() ? TIP_CARD : LIST_CARD);
		if (hovered != null && !pictures.containsKey(hovered))
		{
			hovered = null;
			viewer.hide();
		}
	}

	/** Where {@code entry} is in {@code entries}, by identity, or -1. */
	private static int identityIndex(List<CardHistory.Entry> entries, CardHistory.Entry entry)
	{
		for (int i = 0; i < entries.size(); i++)
		{
			if (entries.get(i) == entry)
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Asks the icon source for {@code entry}'s picture, which may come on any thread, more than once; each time it comes
	 * it is fitted to the picture box and kept, on the event thread, and the entry's cell is drawn again, unless the entry
	 * has left the model in the meantime.
	 */
	private void askForPicture(CardHistory.Entry entry)
	{
		icons.iconFor(entry.card(), image -> onEventThread(() ->
		{
			if (!pictures.containsKey(entry))
			{
				return;
			}
			final ImageIcon icon = fitted(image, PICTURE_WIDTH, PICTURE_HEIGHT);
			if (icon != null)
			{
				pictures.put(entry, icon);
				repaintCell(entry);
			}
		}));
	}

	/** Draws the cell of {@code entry} again, if it is in the model; a null entry is ignored. */
	private void repaintCell(CardHistory.Entry entry)
	{
		if (entry == null)
		{
			return;
		}
		final int index = model.indexOf(entry);
		final Rectangle bounds = index < 0 ? null : historyList.getCellBounds(index, index);
		if (bounds != null)
		{
			historyList.repaint(bounds);
		}
	}

	/** Draws the whole list again, so that every "ago" is read from the clock now. */
	private void refreshAgo()
	{
		historyList.repaint();
	}

	/** The index of the cell whose bounds hold {@code point}, or -1: the list answers the cell nearest a point, so it is checked. */
	private int cellAt(Point point)
	{
		final int index = historyList.locationToIndex(point);
		final Rectangle bounds = index < 0 ? null : historyList.getCellBounds(index, index);
		return bounds != null && bounds.contains(point) ? index : -1;
	}

	/**
	 * Makes the entry under {@code point} (a point of the list, or null once the mouse has left it) the hovered one: its
	 * cell draws hot and the viewer is asked for its card, level with the cell; off every cell, or off the list, the cell
	 * is let go and the viewer is asked to take the card down. A mouse that stays on the same cell asks for nothing.
	 */
	private void hoverAt(Point point)
	{
		final int index = point == null ? -1 : cellAt(point);
		final CardHistory.Entry now = index < 0 ? null : model.get(index);
		if (now == hovered)
		{
			return;
		}
		final CardHistory.Entry before = hovered;
		hovered = now;
		repaintCell(before);
		repaintCell(now);
		if (now == null)
		{
			viewer.hide();
		}
		else if (historyList.isShowing())
		{
			final Rectangle cell = historyList.getCellBounds(index, index);
			final Point origin = historyList.getLocationOnScreen();
			cell.translate(origin.x, origin.y);
			viewer.show(now.card(), cell);
		}
	}

	/**
	 * A popup-trigger press or release on a row opens a menu under the mouse with "Open wiki" (the card's wiki address goes
	 * to {@code browse}) and "Copy" (the line goes to {@code copy}), the two entries the game's own menu gets on the same
	 * line.
	 */
	private void offerMenu(MouseEvent e)
	{
		if (!e.isPopupTrigger())
		{
			return;
		}
		final int index = cellAt(e.getPoint());
		if (index < 0)
		{
			return;
		}
		final HoverCard card = model.get(index).card();
		final JPopupMenu popup = new JPopupMenu();
		popup.add(menuItem(OPEN_WIKI_TEXT, () -> browse.accept(card.wikiUrl())));
		popup.add(menuItem(COPY_TEXT, () -> copy.accept(card.plainLine())));
		popup.show(historyList, e.getX(), e.getY());
	}

	/** One entry of a row's menu, in the menu's 12 px type. */
	private static JMenuItem menuItem(String text, Runnable action)
	{
		final JMenuItem item = new JMenuItem(text);
		item.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		item.setForeground(Color.WHITE);
		item.addActionListener(e -> action.run());
		return item;
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

	/**
	 * What takes a picture, on any thread, into {@code box}: the picture is fitted to {@code boxWidth} by {@code boxHeight}
	 * px and set as the box's icon on the event thread, each time it is given (a picture that loads gives itself twice).
	 */
	private static Consumer<BufferedImage> pictureIn(JLabel box, int boxWidth, int boxHeight)
	{
		return image -> onEventThread(() ->
		{
			final ImageIcon icon = fitted(image, boxWidth, boxHeight);
			if (icon != null)
			{
				box.setIcon(icon);
			}
		});
	}

	/**
	 * The picture drawn into a copy that fits a box of {@code boxWidth} by {@code boxHeight} px, in its own proportions and
	 * never larger than it came: a copy, because an item's picture is filled in after it is handed out and the copy is taken
	 * when the picture is given. Null when the picture has no size.
	 */
	private static ImageIcon fitted(BufferedImage image, int boxWidth, int boxHeight)
	{
		final int width = image.getWidth();
		final int height = image.getHeight();
		if (width <= 0 || height <= 0)
		{
			return null;
		}
		final double scale = Math.min(1.0, Math.min((double) boxWidth / width, (double) boxHeight / height));
		final int fitWidth = Math.max(1, (int) Math.round(width * scale));
		final int fitHeight = Math.max(1, (int) Math.round(height * scale));
		final BufferedImage copy = new BufferedImage(fitWidth, fitHeight, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = copy.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.drawImage(image, 0, 0, fitWidth, fitHeight, null);
		}
		finally
		{
			g.dispose();
		}
		return new ImageIcon(copy);
	}

	/**
	 * RuneLite calls this, on the event thread, each time the sidebar shows this panel (its icon pressed): while the list
	 * of recent cards is empty the next tip shows. With cards in the list nothing changes.
	 */
	@Override
	public void onActivate()
	{
		super.onActivate();
		if (model.isEmpty())
		{
			showNextTip();
		}
	}

	/** The "ago" words are refreshed from now on, once now and then every {@link #AGO_REFRESH_MS} ms, while the panel shows. */
	@Override
	public void addNotify()
	{
		super.addNotify();
		refreshAgo();
		agoTimer.start();
	}

	/** The "ago" refresh stops, and a card shown for a hovered row comes down, as the panel leaves the screen. */
	@Override
	public void removeNotify()
	{
		agoTimer.stop();
		if (hovered != null)
		{
			hovered = null;
			viewer.hide();
		}
		super.removeNotify();
	}

	/**
	 * The history's list model: a {@link DefaultListModel} that can tell its list that every row may have changed, which
	 * is how the list is made to measure its rows again.
	 */
	private static final class HistoryModel extends DefaultListModel<CardHistory.Entry>
	{
		/** Tells the list that every row's content may have changed, so that it measures each row again; a model with no rows tells nobody. */
		void measureAgain()
		{
			if (!isEmpty())
			{
				fireContentsChanged(this, 0, getSize() - 1);
			}
		}
	}

	/**
	 * The history's list: as wide as the scroll pane's view, so that a row never asks for more width than the sidebar has,
	 * which is what cuts a long line with "..." and keeps the horizontal bar away. Its cells' width is fixed (at 0, see
	 * {@link #historyView}), so its own preferred width is 0 and the view is always the wider. A change of its width is
	 * told to {@code widthChanged} as the new width is set, as a title that took two lines at one width may take one at
	 * another and a list measures its rows on its model's word alone.
	 */
	private static final class HistoryList extends JList<CardHistory.Entry>
	{
		private final Runnable widthChanged;

		HistoryList(HistoryModel model, Runnable widthChanged)
		{
			super(model);
			this.widthChanged = widthChanged;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public void setBounds(int x, int y, int width, int height)
		{
			final boolean widthChanges = width != getWidth();
			super.setBounds(x, y, width, height);
			if (widthChanges)
			{
				widthChanged.run();
			}
		}
	}

	/**
	 * Draws an entry of the history as a row: the picture box at the left (36 x 32 px, the entry's picture in it) and the
	 * text beside it. The card's title is in bold white with the time, small and grey, in the row's top right corner on
	 * the title's first line (how long ago, or the clock time, as set); a title that does not fit beside the widest time
	 * the setting can read goes on to a second line (never a third: what is left after two lines is cut with "..."), so
	 * that a time that grows never cuts a title. Under the title come who the line is about in the name colour as the
	 * settings have it at the moment of the draw, and what kind of card it gave in grey, whole ({@link RowWords}); only if
	 * the two do not fit is the kind cut with "...", and then the name. The hovered entry's row takes the hover ground and
	 * an orange bar at its left edge. Under the row, inside the cell, the gap to the next row in the list's own ground.
	 *
	 * <p>Choice: one component is built, once, and set from the entry at every draw, which is how a list draws only the
	 * rows on screen at the cost of those rows; the time is worked out here, at draw time, so that a timer's repaint is
	 * all it takes to refresh it. No selection is drawn.
	 *
	 * <p>Choice: a row has only two heights, {@code ROW_HEIGHT} with a one-line title and {@code ROW_HEIGHT_TWO_LINES}
	 * with a two-line one (each with the gap), and the cell's preferred height is the one the line count says. A list
	 * with no fixed cell height asks the renderer for the height of every row of the session whenever its model or its
	 * width changes, so the wrap, which measures text, is done once per entry for the list's width and the Card time
	 * setting and kept in {@code titleLines}; every later ask is a lookup. The title is measured beside the widest time
	 * the setting can read ({@link CardHistory#widestTimes}, in the time's font), not beside the time as it reads at the
	 * first measure, which grows from "now" to "59m" and on; so the count holds however the time's words grow. The count
	 * is dropped when the list's width or the setting changes, and not when the time's words do, so the height always
	 * matches the lines drawn and no title is cut by its time.
	 */
	private final class RowRenderer implements ListCellRenderer<CardHistory.Entry>
	{
		private final JPanel cell;
		/** Whether the row drawn now is the hovered one; the row is painted by the skin, which lays its hover ground over it. */
		private boolean rowHot;
		private final JPanel row = new JPanel(new BorderLayout(ROW_PICTURE_GAP, 0))
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				paintSkin(g, this, SidebarSkin.Box.ROW, rowHot);
			}
		};
		private final JLabel picture = new JLabel();
		private final JLabel titleFirst = boldLabel("", 12f, Color.WHITE);
		private final JLabel titleRest = boldLabel("", 12f, Color.WHITE);
		private final JLabel time = label("", ROW_TIME_SIZE, ROW_TIME_COLOUR);
		private final JLabel who = label("", 12f, settings.nameColour());
		private final JLabel what = label("", 12f, ROW_WHAT_COLOUR);
		/** The height the cell asks for, in px: set from the entry's line count at every draw. */
		private int cellHeight = ROW_HEIGHT + ROW_GAP;
		/** The width, in px, of the widest time the setting in {@link #measuredTime} can read in the time's font. */
		private int widestTime;

		RowRenderer()
		{
			cell = new JPanel(new BorderLayout())
			{
				@Override
				public Dimension getPreferredSize()
				{
					return new Dimension(0, cellHeight);
				}
			};
			cell.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			cell.setBorder(new EmptyBorder(0, 0, ROW_GAP, 0));

			picture.setHorizontalAlignment(SwingConstants.CENTER);
			picture.setPreferredSize(new Dimension(PICTURE_WIDTH, PICTURE_HEIGHT));
			final JPanel pictureBox = new JPanel(new GridBagLayout());
			pictureBox.setOpaque(false);
			pictureBox.add(picture);
			row.add(pictureBox, BorderLayout.WEST);

			final JPanel words = new JPanel(new RowWordsLayout(who, what));
			words.setOpaque(false);
			words.add(who);
			words.add(what);
			final JPanel text = new JPanel(new RowTextLayout(titleFirst, titleRest, time, words));
			text.setOpaque(false);
			text.add(titleFirst);
			text.add(titleRest);
			text.add(time);
			text.add(words);
			row.add(text, BorderLayout.CENTER);
			cell.add(row, BorderLayout.CENTER);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends CardHistory.Entry> list, CardHistory.Entry entry,
			int index, boolean isSelected, boolean cellHasFocus)
		{
			final HoverCard card = entry.card();
			final boolean hot = entry == hovered;
			rowHot = hot;
			cell.setBackground(skin.page);
			row.setBackground(hot ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARK_GRAY_COLOR);
			row.setBorder(hot ? ROW_HOT_BORDER : ROW_BORDER);
			picture.setIcon(pictures.get(entry));
			titleFirst.setForeground(skin.rowTitle);
			titleRest.setForeground(skin.rowTitle);
			time.setForeground(skin.time);
			what.setForeground(skin.grey);
			final CardTime style = settings.cardTime();
			time.setText(CardHistory.timeOf(entry.at(), style, ZoneId.systemDefault(), System.currentTimeMillis()));
			who.setForeground(settings.nameColour());
			who.setText(RowWords.who(entry.line()));
			what.setText(RowWords.what(entry.line()));

			final String title = card.title();
			final int room = Math.max(0, list.getWidth() - ROW_TEXT_INSET);
			if (list.getWidth() != measuredWidth || style != measuredTime)
			{
				titleLines.clear();
				measuredWidth = list.getWidth();
				measuredTime = style;
				widestTime = widestTimeIn(style);
			}
			String[] parts = null;
			Integer lines = titleLines.get(entry);
			if (lines == null)
			{
				parts = wrap(title, room);
				lines = parts[1].isEmpty() ? 1 : 2;
				titleLines.put(entry, lines);
			}
			if (lines == 2)
			{
				parts = parts == null ? wrap(title, room) : parts;
				titleFirst.setText(parts[0]);
				titleRest.setText(parts[1]);
			}
			else
			{
				titleFirst.setText(title);
				titleRest.setText("");
			}
			cellHeight = (lines == 2 ? ROW_HEIGHT_TWO_LINES : ROW_HEIGHT) + ROW_GAP;
			return cell;
		}

		/**
		 * The width, in px, of the widest time the Card time setting {@code style} can read, in the time label's own font:
		 * the widest of {@link CardHistory#widestTimes}, built with the digit of '0' to '9' that font draws widest.
		 */
		private int widestTimeIn(CardTime style)
		{
			final FontMetrics metrics = time.getFontMetrics(time.getFont());
			char digit = '0';
			for (char c = '1'; c <= '9'; c++)
			{
				if (metrics.charWidth(c) > metrics.charWidth(digit))
				{
					digit = c;
				}
			}
			int widest = 0;
			for (final String form : CardHistory.widestTimes(style, digit))
			{
				widest = Math.max(widest, metrics.stringWidth(form));
			}
			return widest;
		}

		/**
		 * The title as the first line, which has the text's width less the widest time the setting can read
		 * ({@link #widestTime}, not the time as it reads now: that grows) and the room before it, and the rest (empty when
		 * the title fits the first line) for the second. The line breaks at a space; a first word too wide for the line
		 * keeps it alone, and its label cuts it with "...".
		 *
		 * @param title the card's title
		 * @param room the width the row's text has, in px
		 */
		private String[] wrap(String title, int room)
		{
			final FontMetrics metrics = titleFirst.getFontMetrics(titleFirst.getFont());
			final int first = Math.max(0, room - widestTime - ROW_WORDS_GAP);
			if (metrics.stringWidth(title) <= first)
			{
				return new String[] {title, ""};
			}
			int cut = -1;
			int space = title.indexOf(' ');
			while (space > 0 && metrics.stringWidth(title.substring(0, space)) <= first)
			{
				cut = space;
				space = title.indexOf(' ', space + 1);
			}
			if (cut < 0)
			{
				cut = title.indexOf(' ');
			}
			return cut <= 0 ? new String[] {title, ""} : new String[] {title.substring(0, cut), title.substring(cut + 1).trim()};
		}
	}

	/**
	 * Lays out a row's text in lines of {@link ClanHubPanel#ROW_LINE} px, the block centred in the row: the title's first
	 * line at the left and the time at the right of it on the same baseline, the rest of the title under it when there is
	 * one, and last the line of the name and the kind. The first line has the width the time leaves; the others have the
	 * whole width, and a label given less than its text needs cuts it with "...".
	 */
	private static final class RowTextLayout implements LayoutManager
	{
		private final JLabel first;
		private final JLabel rest;
		private final JLabel time;
		private final JPanel words;

		RowTextLayout(JLabel first, JLabel rest, JLabel time, JPanel words)
		{
			this.first = first;
			this.rest = rest;
			this.time = time;
			this.words = words;
		}

		@Override
		public void addLayoutComponent(String name, Component comp)
		{
		}

		@Override
		public void removeLayoutComponent(Component comp)
		{
		}

		@Override
		public Dimension preferredLayoutSize(Container parent)
		{
			final Insets in = parent.getInsets();
			final int lines = rest.getText().isEmpty() ? 2 : 3;
			return new Dimension(in.left + in.right, in.top + in.bottom + lines * ROW_LINE);
		}

		@Override
		public Dimension minimumLayoutSize(Container parent)
		{
			final Insets in = parent.getInsets();
			return new Dimension(in.left + in.right, in.top + in.bottom);
		}

		@Override
		public void layoutContainer(Container parent)
		{
			final Insets in = parent.getInsets();
			final int width = Math.max(0, parent.getWidth() - in.left - in.right);
			final boolean second = !rest.getText().isEmpty();
			final int lines = second ? 3 : 2;
			final int top = in.top + Math.max(0, (parent.getHeight() - in.top - in.bottom - lines * ROW_LINE) / 2);

			final Dimension f = first.getPreferredSize();
			final Dimension t = time.getPreferredSize();
			final int timeWidth = Math.min(t.width, width);
			final int firstWidth = Math.max(0, width - timeWidth - ROW_WORDS_GAP);
			final int firstY = top + Math.max(0, (ROW_LINE - f.height) / 2);
			final int timeY = firstY + Math.max(0, baselineOf(first, f) - baselineOf(time, t));
			first.setBounds(in.left, firstY, firstWidth, f.height);
			time.setBounds(in.left + width - timeWidth, timeY, timeWidth, t.height);

			int y = top + ROW_LINE;
			if (second)
			{
				final Dimension r = rest.getPreferredSize();
				rest.setBounds(in.left, y + Math.max(0, (ROW_LINE - r.height) / 2), width, r.height);
				y += ROW_LINE;
			}
			else
			{
				rest.setBounds(0, 0, 0, 0);
			}
			words.setBounds(in.left, y, width, ROW_LINE);
		}
	}

	/**
	 * Lays out a row's last line: the name and the kind on one baseline, at the left, with {@link ClanHubPanel#ROW_WORDS_GAP}
	 * between them. When the two do not fit the width, the kind is cut first (it keeps the room that is left, and none at
	 * all when that is less than the width of "..."), then the name; a label given less than its own width cuts its text
	 * with "...".
	 */
	private static final class RowWordsLayout implements LayoutManager
	{
		private final JLabel who;
		private final JLabel what;

		RowWordsLayout(JLabel who, JLabel what)
		{
			this.who = who;
			this.what = what;
		}

		@Override
		public void addLayoutComponent(String name, Component comp)
		{
		}

		@Override
		public void removeLayoutComponent(Component comp)
		{
		}

		@Override
		public Dimension preferredLayoutSize(Container parent)
		{
			final Insets in = parent.getInsets();
			final Dimension w = who.getPreferredSize();
			final Dimension k = what.getPreferredSize();
			final boolean hasWhat = !what.getText().isEmpty();
			final int width = w.width + (hasWhat ? ROW_WORDS_GAP + k.width : 0);
			return new Dimension(in.left + in.right + width, in.top + in.bottom + Math.max(w.height, k.height));
		}

		@Override
		public Dimension minimumLayoutSize(Container parent)
		{
			final Insets in = parent.getInsets();
			return new Dimension(in.left + in.right, in.top + in.bottom);
		}

		@Override
		public void layoutContainer(Container parent)
		{
			final Insets in = parent.getInsets();
			final int width = Math.max(0, parent.getWidth() - in.left - in.right);
			final Dimension w = who.getPreferredSize();
			final Dimension k = what.getPreferredSize();

			final boolean hasWhat = !what.getText().isEmpty();
			final int whoWidth = Math.min(w.width, width);
			int whatWidth = 0;
			if (hasWhat && w.width + ROW_WORDS_GAP <= width)
			{
				whatWidth = Math.min(k.width, width - w.width - ROW_WORDS_GAP);
				final int ellipsis = what.getFontMetrics(what.getFont()).stringWidth("...");
				if (whatWidth < k.width && whatWidth < ellipsis)
				{
					whatWidth = 0;
				}
			}

			// One baseline: each label sits so that its text's baseline is the lower of the two, then the block is centred.
			final int whoBase = baselineOf(who, w);
			final int whatBase = baselineOf(what, k);
			final int base = Math.max(whoBase, whatBase);
			final int whoY = base - whoBase;
			final int whatY = base - whatBase;
			final int blockHeight = Math.max(whoY + w.height, whatY + k.height);
			final int top = in.top + Math.max(0, (parent.getHeight() - in.top - in.bottom - blockHeight) / 2);

			final int whoX = in.left;
			final int whatX = whoX + whoWidth + (whatWidth > 0 ? ROW_WORDS_GAP : 0);
			who.setBounds(whoX, top + whoY, whoWidth, w.height);
			what.setBounds(whatX, top + whatY, whatWidth, k.height);
		}
	}

	/** Where the text's baseline is in a label of size {@code size}, from its top; 0 when it has none. */
	private static int baselineOf(JLabel label, Dimension size)
	{
		return Math.max(0, label.getBaseline(Math.max(0, size.width), Math.max(0, size.height)));
	}

	/** The how-to bar's fold arrow: a small solid triangle in the skin's grey (the captions' grey in the plain sidebar), pointing down or right. */
	private static final class FoldArrow implements Icon
	{
		private static final int SIZE = 10;
		private final boolean down;
		private final Color colour;

		FoldArrow(boolean down, Color colour)
		{
			this.down = down;
			this.colour = colour;
		}

		@Override
		public int getIconWidth()
		{
			return SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return SIZE;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			final Graphics2D g2 = (Graphics2D) g.create();
			try
			{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(colour);
				final Polygon triangle = down
					? new Polygon(new int[] {x, x + SIZE, x + SIZE / 2}, new int[] {y + 2, y + 2, y + SIZE - 2}, 3)
					: new Polygon(new int[] {x + 2, x + 2, x + SIZE - 2}, new int[] {y, y + SIZE, y + SIZE / 2}, 3);
				g2.fillPolygon(triangle);
			}
			finally
			{
				g2.dispose();
			}
		}
	}

	/**
	 * One small button of a choice row in a group: the chosen one in the brand orange with dark words, the others dark grey
	 * with white words and a grey edge. It paints itself whatever look and feel is on (as a colour swatch does), takes no
	 * keyboard, and a click on it chooses it.
	 */
	private static final class SegmentButton extends JToggleButton
	{
		private static final Color OFF_COLOUR = new Color(55, 55, 55);
		private static final Color EDGE_COLOUR = new Color(80, 80, 80);
		private static final Color ON_TEXT_COLOUR = new Color(20, 20, 20);
		private static final int PAD_X = 8;
		private static final int PAD_Y = 3;

		SegmentButton(String words, Font font)
		{
			super(words);
			setFont(font);
			setFocusable(false);
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
		}

		/** The words and a little room round them. */
		@Override
		public Dimension getPreferredSize()
		{
			final FontMetrics metrics = getFontMetrics(getFont());
			return new Dimension(metrics.stringWidth(getText()) + 2 * PAD_X, metrics.getHeight() + 2 * PAD_Y);
		}

		/** The whole button: its ground, its edge, its words. */
		@Override
		protected void paintComponent(Graphics g)
		{
			final boolean on = isSelected();
			g.setColor(on ? ColorScheme.BRAND_ORANGE : OFF_COLOUR);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(on ? ColorScheme.BRAND_ORANGE : EDGE_COLOUR);
			g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
			g.setFont(getFont());
			g.setColor(on ? ON_TEXT_COLOUR : Color.WHITE);
			final FontMetrics metrics = g.getFontMetrics();
			g.drawString(getText(), (getWidth() - metrics.stringWidth(getText())) / 2,
				(getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
		}
	}

	/** The settings icon: grey at rest, orange under the mouse; a press opens the menu under it. */
	private JLabel settingsIcon()
	{
		final JLabel[] self = new JLabel[1];
		self[0] = SupportLinks.namedIconButton(GearGlyph.icon(ICON_SIZE, ColorScheme.LIGHT_GRAY_COLOR),
			GearGlyph.icon(ICON_SIZE, ColorScheme.BRAND_ORANGE), SupportLinks.SETTINGS_TIP, () -> openMenu(self[0]));
		self[0].setBorder(SupportLinks.iconBorder());
		return self[0];
	}

	/** Opens the menu under {@code anchor}; the press that closed it a moment ago opens nothing, so the icon toggles. */
	private void openMenu(JLabel anchor)
	{
		if (!anchor.isShowing() || System.nanoTime() - closedAtNanos < REOPEN_GUARD_NANOS)
		{
			return;
		}
		menuAnchor = anchor;
		menu.show(anchor, 0, anchor.getHeight());
	}

	/**
	 * Shows the menu again under the settings icon it was last opened under, with its "Colours" group open (the colour
	 * rows are in it), once a colour picker has closed. The reopen guard is not asked: it is for the icon press that
	 * closed the menu, and a picker closing seconds later is not that press. Nothing shows if the icon is gone from the
	 * screen or the menu is already up.
	 */
	private void reopenMenu()
	{
		final JLabel anchor = menuAnchor;
		if (anchor == null || !anchor.isShowing() || menu.isVisible())
		{
			return;
		}
		menu.show(anchor, 0, anchor.getHeight());
		if (coloursGroup != null)
		{
			MenuSelectionManager.defaultManager().setSelectedPath(
				new MenuElement[] {menu, coloursGroup, coloursGroup.getPopupMenu()});
		}
	}

	/**
	 * Draws the sidebar again from the settings as they stand: its skin (the one the sidebar theme names), the how-to's action line (its words follow the colouring switch and the hover
	 * colour) and the list of recent cards (the rows follow the name colour and the time setting; a time setting changed
	 * since the last call has every row measured again, since the titles are measured beside the widest time the setting
	 * can read, even when the list drew in between). The plugin calls it when a setting of the plugin's group changes,
	 * whether the sidebar's menu wrote it or RuneLite's own settings page did, so that a colour shows in the rows while a
	 * colour picker is still open and a theme shows at once. Event thread only.
	 *
	 * <p>Choice: the change is judged against {@link #notifiedTime}, which only this writes, and not against the counts'
	 * {@link #measuredTime}. A profile switch posts the setting's change from RuneLite's own thread, so this call comes
	 * later, through the event queue; a draw in that gap (the "ago" timer's, a picture's, a hover's) counts the drawn rows
	 * under the new setting and moves {@link #measuredTime}, and judged against that the change would be missed, the list
	 * keeping the old setting's heights until the next card.
	 */
	void settingsChanged()
	{
		applyLocks();
		applySkin();
		final CardTime time = settings.cardTime();
		if (time != notifiedTime)
		{
			notifiedTime = time;
			model.measureAgain();
		}
		historyList.repaint();
	}

	/** The skin the sidebar is painted with by the settings as they stand. */
	private SidebarSkin skinNow()
	{
		return SidebarSkin.pick(settings.sidebarLook(), settings.cardTheme(), settings.pictureTheme(), settings.sidebarPicture());
	}

	/**
	 * Shows the Card width and Card place rows as the greyed words "Medium (by the theme)" and "At the cursor (by the
	 * theme)" while a picture theme is the card theme (its cards are Medium and open at the cursor whatever the settings say),
	 * and as the rows again with an original theme; the player's own choices are never written.
	 */
	private void applyLocks()
	{
		final boolean locked = CardStyle.of(settings.cardTheme(), settings.pictureTheme()).picture != null;
		widthRow.setVisible(!locked);
		widthLock.setVisible(locked);
		placeItems.forEach(item -> item.setVisible(!locked));
		placeLock.setVisible(locked);
		if (frostRow != null)
		{
			frostSlider.setEnabled(locked);
			frostValue.setEnabled(locked);
			final String tip = locked ? FROST_TIP : FROST_OFF_TIP;
			frostRow.setToolTipText(tip);
			for (final Component part : frostRow.getComponents())
			{
				if (part instanceof JComponent)
				{
					((JComponent) part).setEnabled(locked);
					((JComponent) part).setToolTipText(tip);
				}
			}
			frostSlider.setToolTipText(tip);
			frostValue.setToolTipText(tip);
		}
	}

	/**
	 * Works the skin out again and puts its colours on everything that has one: the panel's ground and the list's, the
	 * how-to's bar, lines, heading and rows (their HTML is built again), the recent cards' header, the empty list's tip
	 * and the examine icon, all of it only when the skin is not the one the components already wear. The boxes themselves
	 * are painted by the skin as they draw ({@link #paintSkin}), so they are only asked to draw again.
	 */
	private void applySkin()
	{
		skin = skinNow();
		if (skin != dressed)
		{
			dressed = skin;
			setBackground(skin.page);
			historyList.setBackground(skin.page);
			historyScroll.setBackground(skin.page);
			historyScroll.getViewport().setBackground(skin.page);
			howToTitle.setForeground(skin.title);
			howToAction.setForeground(skin.text);
			howToWiki.setForeground(skin.grey);
			howToTip.setForeground(skin.grey);
			emptyTip.setForeground(skin.grey);
			howToHeading.setForeground(skin.heading);
			for (int i = 0; i < howToRowTexts.size(); i++)
			{
				styleHowToRow(i);
			}
			if (examineIconBox != null)
			{
				examineIconBox.setIcon(ExamineGlyph.icon(HOW_TO_ICON_SIZE, skin.grey));
			}
			foldArrow.setIcon(new FoldArrow(!howToFolded, skin.grey));
			recentTitle.setForeground(skin.title);
			count.setForeground(skin.grey);
			clearLabel.setForeground(skin.grey);
			revalidate();
		}
		refreshHowTo();
		repaint();
	}

	/**
	 * Paints one of the sidebar's three boxes into {@code box}: the skin's look of {@code kind} at the box's size and, while
	 * the mouse is on it ({@code hot}), the skin's hover ground over that.
	 */
	private void paintSkin(Graphics g, JComponent box, SidebarSkin.Box kind, boolean hot)
	{
		final Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			skin.paintBox(g2, box.getWidth(), box.getHeight(), kind);
			if (hot)
			{
				g2.setColor(skin.hot);
				g2.fillRect(0, 0, box.getWidth(), box.getHeight());
			}
		}
		finally
		{
			g2.dispose();
		}
	}

	/** Takes the menu down: OK, a link press, or the reset. */
	private void closeMenu()
	{
		menu.setVisible(false);
	}

	/**
	 * The settings menu, in STANDARD A5's order and kept to about ten rows: the header, a rule, the "Hover cards" switch, a
	 * rule, the two theme submenus (each shows its current theme at the right), a rule, the four groups ("Card", "When it
	 * shows", "What gets a card", "Colours", each opening beside the menu), a rule, then the OK row. The header is
	 * focusable, so the menu's window takes the keyboard when the menu opens and gives it to the header, which shows no
	 * caret; no box is focusable at rest, and a press moves the keyboard to the box pressed.
	 *
	 * <p>Choice: no row of a group closes anything when the mouse enters it, as the OK row does ({@link #closesSubmenu}):
	 * a group's rows are panels in the group's own popup, and cutting the selected path back to the menu there would close
	 * the group the mouse is in.
	 */
	private JPopupMenu buildMenu()
	{
		final JPopupMenu popup = new JPopupMenu();
		popup.setBorder(new EmptyBorder(5, 5, 5, 5));
		final JPanel header = support.header();
		header.setFocusable(true);
		popup.add(header);
		popup.addSeparator();
		popup.add(showCheck);
		popup.addSeparator();
		popup.add(themeMenu);
		frostRow = blurRow();
		popup.add(closesSubmenu(frostRow));
		popup.add(sidebarThemeMenu);
		popup.addSeparator();
		popup.add(cardGroup());
		popup.add(whenGroup());
		popup.add(whatGroup());
		coloursGroup = coloursGroup();
		popup.add(coloursGroup);
		popup.addSeparator();
		popup.add(closesSubmenu(okRow()));
		popup.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				syncControls();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
				hidePreview();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
				hidePreview();
			}
		});
		return popup;
	}

	/** The menu's 12 px face, which every menu item, caption and small button wears. */
	private static Font menuFont()
	{
		return FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE);
	}

	/** A group of the menu: a plain submenu with no value, in the menu's face, that opens its popup beside the menu. */
	private static JMenu group(String name)
	{
		final JMenu group = new JMenu(name);
		group.setFont(menuFont());
		group.setForeground(Color.WHITE);
		return group;
	}

	/**
	 * A small heading inside a group, in the face, size and grey of the how-to's "WHAT IT WORKS ON" heading in the plain
	 * sidebar, with a little room above. It is a label, not a menu item.
	 *
	 * @param takesKeyboard whether the heading is focusable: the first part of a group holding a typing box is, so that the
	 *        group's window takes the keyboard when it opens and a press on a box can move it there (as the header does for
	 *        the menu); every other heading is not
	 */
	private static JPanel heading(String words, boolean takesKeyboard)
	{
		final JPanel box = new JPanel(new BorderLayout());
		box.setOpaque(false);
		box.setBorder(new EmptyBorder(GROUP_HEADING_ABOVE, 6, 2, 6));
		final JLabel label = new JLabel(words);
		label.setFont(FontManager.getDefaultBoldFont().deriveFont(HOW_TO_HEADING_SIZE));
		label.setForeground(HOW_TO_HEADING_COLOUR);
		box.add(label, BorderLayout.WEST);
		box.setFocusable(takesKeyboard);
		return box;
	}

	/**
	 * The "Card" group: the card's width, place and time as small choices, and its text size. The heading at the top is
	 * focusable, for the text size's box.
	 */
	private JMenu cardGroup()
	{
		final JMenu group = group(CARD_GROUP_TEXT);
		group.add(heading(WIDTH_HEADING, true));
		widthRow = segments(CardWidth.values(), settings::cardWidth, settings::setCardWidth);
		widthLock = lockLabel(WIDTH_LOCK_TEXT);
		group.add(widthRow);
		group.add(widthLock);
		group.add(heading(PLACE_HEADING, false));
		placeItems = radios(CardPlace.values(), settings::cardPlace, settings::setCardPlace);
		placeLock = lockLabel(PLACE_LOCK_TEXT);
		for (final JRadioButtonMenuItem item : placeItems)
		{
			group.add(item);
		}
		group.add(placeLock);
		group.add(heading(TIME_HEADING, false));
		group.add(segments(CardTime.values(), settings::cardTime, time ->
		{
			settings.setCardTime(time);
			historyList.repaint();
		}));
		group.add(row(CARD_SIZE_TEXT, sizeSpinner));
		return group;
	}

	/**
	 * The Blur behind text row: its caption and its box, right under Card theme, where a picture theme is chosen (the
	 * user's pick: the setting sits beside the choice it belongs to). {@link #applyLocks} greys it with an original theme.
	 */
	private JPanel blurRow()
	{
		final JPanel control = new JPanel();
		control.setOpaque(false);
		control.setLayout(new BoxLayout(control, BoxLayout.X_AXIS));
		control.add(frostSlider);
		control.add(Box.createHorizontalStrut(4));
		control.add(frostValue);
		control.setMaximumSize(control.getPreferredSize());
		final JPanel blur = row(FROST_TEXT, control);
		// One pixel less at the right than the other rows: the digits' own side room, so the number ends where the arrows do.
		blur.setBorder(new EmptyBorder(2, 6, 2, 5));
		// A row of the menu itself, as Card theme is: its caption in the items' white (greyed by applyLocks when disabled).
		blur.getComponent(0).setForeground(Color.WHITE);
		return blur;
	}

	/**
	 * The "When it shows" group: when a card opens, the key it waits for (greyed unless it waits for a key) and its delay.
	 * The heading at the top is focusable, for the key box and the delay box.
	 */
	private JMenu whenGroup()
	{
		final JMenu group = group(WHEN_GROUP_TEXT);
		group.add(heading(SHOW_HEADING, true));
		for (final JRadioButtonMenuItem item : radios(CardTrigger.values(), settings::cardTrigger, trigger ->
		{
			settings.setCardTrigger(trigger);
			keyBoxEnabled(trigger == CardTrigger.HOLD_KEY);
		}))
		{
			group.add(item);
		}
		group.add(row(CARD_KEY_TEXT, keyBox));
		group.add(row(CARD_DELAY_TEXT, delaySpinner));
		return group;
	}

	/** The "What gets a card" group: the four switches of what a card carries and how an examine line reads. */
	private JMenu whatGroup()
	{
		final JMenu group = group(WHAT_GROUP_TEXT);
		group.add(bossCheck);
		group.add(examineCheck);
		group.add(wikiCheck);
		group.add(picturesCheck);
		group.add(heading(EXAMINE_LINE_HEADING, false));
		for (final JRadioButtonMenuItem item : radios(ExamineLine.values(), settings::examineLine, settings::setExamineLine))
		{
			group.add(item);
		}
		return group;
	}

	/** The "Colours" group: the colouring switch, the hover colour, the underline switch and its colour, the sidebar's names. */
	private JMenu coloursGroup()
	{
		final JMenu group = group(COLOURS_GROUP_TEXT);
		group.add(colourCheck);
		group.add(colourRow(HOVER_COLOUR_TEXT, settings::hoverColour, settings::setHoverColour, HOVER_COLOUR_TEXT));
		group.add(underlineCheck);
		group.add(colourRow(UNDERLINE_COLOUR_TEXT, settings::alwaysUnderlineColour, settings::setAlwaysUnderlineColour,
			UNDERLINE_COLOUR_TEXT));
		group.add(colourRow(SIDEBAR_NAMES_TEXT, settings::nameColour, colour ->
		{
			settings.setNameColour(colour);
			historyList.repaint();
		}, NAME_COLOUR_TEXT));
		return group;
	}

	/**
	 * Makes {@code row}, and every part in it, take down an open theme submenu or group when the mouse enters it, and gives
	 * the row back. The menu is left up: the selected path is cut back to the menu alone, as a pick from a submenu does.
	 *
	 * <p>Choice: Swing closes a submenu only when another menu item is armed, and this row is a plain panel, not a menu
	 * item, so a submenu opened by a pass over a row above it stayed up over it. The listener goes on every
	 * part, down to the buttons, because the entry is told to the part the mouse comes onto, which is seldom the row
	 * itself.
	 */
	private JPanel closesSubmenu(JPanel row)
	{
		listenForEntry(row, new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (MenuSelectionManager.defaultManager().getSelectedPath().length > 1)
				{
					MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu});
				}
			}
		});
		return row;
	}

	/** Adds {@code listener} to {@code part} and to every part inside it. */
	private static void listenForEntry(Component part, MouseAdapter listener)
	{
		part.addMouseListener(listener);
		if (part instanceof Container)
		{
			for (final Component child : ((Container) part).getComponents())
			{
				listenForEntry(child, listener);
			}
		}
	}

	/**
	 * A switch: a check in the menu's 12 px type, white on the menu's own ground, writing its state when clicked.
	 *
	 * <p>Choice: the release is taken over because Swing's menu item UI would clear the menu's selection path on it and
	 * close the menu; the user wants to flip several switches in one visit. The keyboard's activation of a focused item
	 * still closes the menu, as Swing does; the menu is a mouse menu.
	 */
	private static JCheckBoxMenuItem check(String text, Consumer<Boolean> write)
	{
		final JCheckBoxMenuItem check = new JCheckBoxMenuItem(text)
		{
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (!takeRelease(this, e, true))
				{
					super.processMouseEvent(e);
				}
			}
		};
		check.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		check.setForeground(Color.WHITE);
		check.addActionListener(e -> write.accept(check.isSelected()));
		return check;
	}

	/**
	 * Takes over a left release inside {@code item}: clicks it, consumes the event, and tells the caller not to pass it
	 * to Swing's menu item UI. Both the switches and the choice items use it, so that a click never takes the menu down.
	 *
	 * @param rearm whether the item is armed again after the click, to keep its hover highlight while the mouse stays on
	 *        it; a choice item is not, since its submenu closes with the click
	 * @return whether the event was a left release inside the item and has been taken
	 */
	private static boolean takeRelease(JMenuItem item, MouseEvent e, boolean rearm)
	{
		if (e.getID() != MouseEvent.MOUSE_RELEASED || !SwingUtilities.isLeftMouseButton(e) || !item.contains(e.getPoint()))
		{
			return false;
		}
		item.doClick(0);
		if (rearm)
		{
			// The click leaves the item un-armed; re-arming keeps its hover highlight while the mouse stays on it.
			item.getModel().setArmed(true);
		}
		e.consume();
		return true;
	}

	/** One theme of a nested list: its words, the submenu it is in (null for the list itself) and what it stands for. */
	private static final class Pick
	{
		final String group;
		final String text;
		final Object key;

		Pick(String group, String text, Object key)
		{
			this.group = group;
			this.text = text;
			this.key = key;
		}
	}

	/** The Card theme list: the original themes, then each group's picture themes. A key is a {@link CardTheme} or a picture id. */
	private static List<Pick> cardPicks()
	{
		final List<Pick> picks = new ArrayList<>();
		for (final CardTheme theme : CardTheme.values())
		{
			picks.add(new Pick(ORIGINAL_TEXT, theme.toString(), theme));
		}
		addPictures(picks);
		return picks;
	}

	/** The Sidebar theme list: "Same as cards" and "RuneLite", then the originals, then each group's picture themes. */
	private static List<Pick> sidebarPicks()
	{
		final List<Pick> picks = new ArrayList<>();
		for (final SidebarTheme look : SidebarTheme.values())
		{
			picks.add(new Pick(look.card() == null ? null : ORIGINAL_TEXT, look.toString(), look));
		}
		addPictures(picks);
		return picks;
	}

	/** Adds the catalogue's themes to {@code picks}, each group in the menu's order, a group's themes bright first. */
	private static void addPictures(List<Pick> picks)
	{
		final PictureThemes themes = PictureThemes.current();
		for (final String group : themes.groups())
		{
			for (final PictureTheme theme : themes.inGroup(group))
			{
				picks.add(new Pick(group, theme.name, theme.id));
			}
		}
	}

	/**
	 * A theme row: a submenu showing its caption at the left and the current theme in grey at the right, before the arrow
	 * (a {@link ValueMenu}), holding the picks: those with no group as items of the list itself, the others as items of a
	 * submenu of their group's name, which the list holds in the order the groups first appear (a rule before the first
	 * group after the plain items and before the first picture group). Each item is a radio item, one button group over the
	 * whole row, and may show a picture of its theme while it is armed (under the mouse, or picked with the keys). Picking
	 * one writes it, shows it at the row's right and closes the submenus alone; the main menu stays open, as it does for the
	 * switches. {@link #syncControls} sets the radio and the value from {@code read}.
	 *
	 * <p>Choice: submenus and not drop-downs, because a drop-down's list is a popup of its own, outside the menu's
	 * selection path, and Swing's menu machinery reads a click on it as a click outside and takes the whole menu down
	 * before the list can show. The row is as wide as its widest theme asks, so that a pick never changes its width. Each
	 * radio item is an anonymous subclass taking the release over through {@link #takeRelease}, as the switches are.
	 *
	 * @param caption the row's name
	 * @param picks the themes, in the order they are listed
	 * @param read the key of the setting's current theme
	 * @param write what picking a theme does: writes the setting
	 * @param preview what makes the picture of a theme from its key; the picture shows beside the item, level with it, and
	 *        goes when no item of the list is armed or the menu closes
	 */
	private ValueMenu themeChoices(String caption, List<Pick> picks, Supplier<Object> read, Consumer<Object> write,
		Function<Object, BufferedImage> preview)
	{
		final String[] words = new String[picks.size()];
		for (int at = 0; at < words.length; at++)
		{
			words[at] = picks.get(at).text;
		}
		final ValueMenu submenu = new ValueMenu(caption, words);
		submenu.setFont(menuFont());
		submenu.setForeground(Color.WHITE);
		final ButtonGroup radios = new ButtonGroup();
		final List<JRadioButtonMenuItem> items = new ArrayList<>();
		final Map<String, JMenu> groups = new LinkedHashMap<>();
		String before = "";
		for (final Pick pick : picks)
		{
			final JRadioButtonMenuItem item = radioItem(pick.text, false);
			item.addActionListener(e ->
			{
				if (!syncing)
				{
					write.accept(pick.key);
					submenu.setValue(pick.text);
					// Only the submenus go: the path ends at the main menu, which stays up.
					MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu});
				}
			});
			item.getModel().addChangeListener(e -> armChanged(item, () -> preview.apply(pick.key)));
			radios.add(item);
			items.add(item);
			if (pick.group == null)
			{
				submenu.add(item);
			}
			else
			{
				JMenu inGroup = groups.get(pick.group);
				if (inGroup == null)
				{
					if (submenu.getMenuComponentCount() > 0 && (before == null
						|| ORIGINAL_TEXT.equals(before) && !ORIGINAL_TEXT.equals(pick.group)))
					{
						submenu.addSeparator();
					}
					inGroup = group(pick.group);
					groups.put(pick.group, inGroup);
					submenu.add(inGroup);
				}
				inGroup.add(item);
			}
			before = pick.group;
		}
		choiceSyncs.add(() ->
		{
			final Object current = read.get();
			for (int at = 0; at < picks.size(); at++)
			{
				if (picks.get(at).key.equals(current))
				{
					items.get(at).setSelected(true);
					submenu.setValue(picks.get(at).text);
					return;
				}
			}
			radios.clearSelection();
		});
		return submenu;
	}

	/** The greyed words that stand in for a row while a picture theme sets it; hidden until then. */
	private static JLabel lockLabel(String words)
	{
		final JLabel label = new JLabel(words);
		label.setFont(menuFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(3, 10, 3, 6));
		// The menu lays its rows out on one axis: a plain label's left alignment set it from the middle and cut it, so it
		// takes the rows' own centre alignment and the whole width, its words still at its left.
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
		label.setVisible(false);
		return label;
	}

	/**
	 * A radio item in the menu's face that takes the left release over through {@link #takeRelease}, so that a click never
	 * takes the menu, or the popup it is in, down.
	 *
	 * @param rearm whether the item is armed again after the click, to keep its hover highlight while the mouse stays on it:
	 *        an item in a group is, since its group stays up; an item of a theme list is not, since its list closes
	 */
	private static JRadioButtonMenuItem radioItem(Object value, boolean rearm)
	{
		final JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(value))
		{
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				if (!takeRelease(this, e, rearm))
				{
					super.processMouseEvent(e);
				}
			}
		};
		item.setFont(menuFont());
		item.setForeground(Color.WHITE);
		return item;
	}

	/**
	 * A choice as radio items for a group: one item per value in the order of {@code values}, in one button group, for the
	 * caller to add to the group in the order it wants. A pick writes the setting, and both the group and the menu stay
	 * open: the item takes the release over, and nothing cuts the selected path. {@link #syncControls} selects the item
	 * of {@code read}'s choice.
	 *
	 * @param values the choices, shown with their {@code toString} words
	 * @param read the setting's current choice
	 * @param write what picking a choice does: writes the setting
	 */
	private <T> List<JRadioButtonMenuItem> radios(T[] values, Supplier<T> read, Consumer<T> write)
	{
		final ButtonGroup group = new ButtonGroup();
		final List<JRadioButtonMenuItem> items = new ArrayList<>();
		for (final T value : values)
		{
			final JRadioButtonMenuItem item = radioItem(value, true);
			item.addActionListener(e ->
			{
				if (!syncing)
				{
					write.accept(value);
				}
			});
			group.add(item);
			items.add(item);
		}
		final List<T> choices = Arrays.asList(values);
		choiceSyncs.add(() ->
		{
			final int index = choices.indexOf(read.get());
			if (index < 0)
			{
				group.clearSelection();
				return;
			}
			items.get(index).setSelected(true);
		});
		return items;
	}

	/**
	 * A choice as a row of small buttons side by side, one per value in the order of {@code values}, filling the row, the
	 * chosen one lit in the brand orange. A click writes the setting at once and keeps the group and the menu open.
	 * {@link #syncControls} lights the button of {@code read}'s choice.
	 *
	 * @param values the choices, shown with their {@code toString} words
	 * @param read the setting's current choice
	 * @param write what a click does: writes the setting
	 */
	private <T> JPanel segments(T[] values, Supplier<T> read, Consumer<T> write)
	{
		final JPanel row = new JPanel(new GridLayout(1, 0, SEGMENT_GAP, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final ButtonGroup group = new ButtonGroup();
		final List<SegmentButton> buttons = new ArrayList<>();
		for (final T value : values)
		{
			final SegmentButton button = new SegmentButton(String.valueOf(value), menuFont());
			button.addActionListener(e ->
			{
				if (!syncing)
				{
					write.accept(value);
				}
			});
			group.add(button);
			buttons.add(button);
			row.add(button);
		}
		final List<T> choices = Arrays.asList(values);
		choiceSyncs.add(() ->
		{
			final int index = choices.indexOf(read.get());
			if (index < 0)
			{
				group.clearSelection();
				return;
			}
			buttons.get(index).setSelected(true);
		});
		return row;
	}

	/**
	 * A theme item's arming changed: its picture is handed to the viewer when it is armed and not the item already shown,
	 * and when it is no longer armed a check for no armed item is queued.
	 *
	 * <p>Choice: the check is queued and not made at once, because moving from one item to the next un-arms the first
	 * before it arms the second, and an immediate hide would take the picture down for the instant between; the check runs
	 * after the event, when the second item has taken over. The picture is made only when the item arms.
	 *
	 * @param item the radio item whose model changed
	 * @param picture makes the picture of the item's choice
	 */
	private void armChanged(JMenuItem item, Supplier<BufferedImage> picture)
	{
		if (item.getModel().isArmed())
		{
			if (previewItem != item && item.isShowing())
			{
				previewItem = item;
				previewUp = true;
				viewer.showPicture(picture.get(), new Rectangle(item.getLocationOnScreen(), item.getSize()));
			}
		}
		else if (previewItem == item)
		{
			previewItem = null;
			SwingUtilities.invokeLater(this::hidePreviewIfNoneArmed);
		}
	}

	/** Takes the picture down when it is up and no theme item is armed. */
	private void hidePreviewIfNoneArmed()
	{
		if (previewItem == null && previewUp)
		{
			previewUp = false;
			viewer.hide();
		}
	}

	/** Takes the picture down when it is up, whatever is armed: the menu has closed. */
	private void hidePreview()
	{
		previewItem = null;
		if (previewUp)
		{
			previewUp = false;
			viewer.hide();
		}
	}

	/**
	 * The whip's picture for the sample card: asked for from the icon source the first time, once, and kept when it comes
	 * (it may come later, more than once, on any thread); null until then, which leaves the card's picture box empty.
	 */
	private BufferedImage whipPicture()
	{
		if (!whipAsked)
		{
			whipAsked = true;
			icons.item(ThemePreviews.WHIP_ITEM_ID, 1, image -> whip = image);
		}
		return whip;
	}

	/**
	 * The card key box: a small button in the menu's 12 px type showing the key's words. A press arms it, and the next key
	 * pressed is the card key: Escape keeps the old one, Backspace or Delete clears it, any other key (a modifier alone
	 * included) is taken. The box is greyed unless the card opens "While holding a key"; {@link #syncControls} sets it
	 * from the setting.
	 */
	private JButton buildKeyBox()
	{
		final JButton box = new JButton();
		box.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		box.setFocusPainted(false);
		box.setToolTipText(CARD_KEY_TIP);
		typesOnPress(box);
		fitKeyBox(box, shownKey.toString());
		box.addActionListener(e -> armKey());
		box.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (keyArmed)
				{
					takeKey(e);
				}
			}
		});
		box.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (keyArmed)
				{
					cancelKey();
				}
			}
		});
		return box;
	}

	/** Puts {@code words} on the key box, as wide as the wider of them and "Press a key", so the box never jumps. */
	private static void fitKeyBox(JButton box, String words)
	{
		box.setText(words);
		box.setPreferredSize(null);
		box.setMinimumSize(null);
		box.setMaximumSize(null);
		final Dimension natural = box.getPreferredSize();
		final FontMetrics metrics = box.getFontMetrics(box.getFont());
		final int extra = Math.max(0, metrics.stringWidth(KEY_PROMPT) - metrics.stringWidth(words));
		final Dimension size = new Dimension(natural.width + extra, natural.height);
		box.setPreferredSize(size);
		box.setMinimumSize(size);
		box.setMaximumSize(size);
		box.revalidate();
	}

	/** A press on the key box: it says "Press a key" and takes the keyboard. */
	private void armKey()
	{
		if (!keyArmed)
		{
			keyArmed = true;
			fitKeyBox(keyBox, KEY_PROMPT);
		}
		takeKeyboard(keyBox);
	}

	/** The key press an armed key box has been waiting for, consumed so that nothing else reads it. */
	private void takeKey(KeyEvent e)
	{
		e.consume();
		final int code = e.getKeyCode();
		if (code == KeyEvent.VK_ESCAPE)
		{
			cancelKey();
		}
		else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE)
		{
			setKey(Keybind.NOT_SET);
		}
		else
		{
			setKey(new Keybind(e));
		}
	}

	/** Writes the card key and shows it; the key box stops waiting and lets the keyboard go. */
	private void setKey(Keybind key)
	{
		settings.setCardKey(key);
		showKey(key);
	}

	/** The key box stops waiting, writes nothing, and shows the key it showed before. */
	private void cancelKey()
	{
		showKey(shownKey);
	}

	/** Shows {@code key} on the key box, not waiting and not holding the keyboard. */
	private void showKey(Keybind key)
	{
		keyArmed = false;
		shownKey = key;
		fitKeyBox(keyBox, key.toString());
		keyBox.setFocusable(false);
	}

	/** Enables the key box, or greys it (and stops it waiting, if it was) when the card does not open on a key. */
	private void keyBoxEnabled(boolean on)
	{
		if (!on && keyArmed)
		{
			cancelKey();
		}
		keyBox.setEnabled(on);
	}

	/**
	 * Keeps a typing part of the menu out of the keyboard's reach until it is pressed: it is not focusable at rest, a
	 * left press on it makes it focusable and asks for the keyboard, and losing the keyboard makes it unfocusable again.
	 *
	 * <p>Choice: RuneLite's menu is a heavyweight window, and a window hands the keyboard to its first focusable part
	 * when it opens, which was the delay box's text field, with its caret blinking and the game's keys going nowhere.
	 * With no box focusable at rest, none holds a caret when the menu opens. The window takes the keyboard when it is
	 * shown only if it holds a focusable part then, which the header is, so the window has the keyboard and a press can
	 * move it to a box. The window may not be the focused one yet when a part is pressed, so {@code requestFocus} is
	 * asked, not {@code requestFocusInWindow}, which would fail.
	 */
	private static void typesOnPress(JComponent part)
	{
		part.setFocusable(false);
		part.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					takeKeyboard(part);
				}
			}
		});
		part.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				part.setFocusable(false);
			}
		});
	}

	/** Makes the typing part focusable and asks for the keyboard for it. */
	private static void takeKeyboard(JComponent part)
	{
		part.setFocusable(true);
		part.requestFocus();
	}

	/**
	 * A number spinner from {@code min} to {@code max} in steps of {@code step}, in the menu's 12 px type and
	 * {@code columns} characters wide, writing its value when it changes.
	 */
	private JSpinner spinner(int value, int min, int max, int step, int columns, IntConsumer write)
	{
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
		final Font font = FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE);
		spinner.setFont(font);
		final JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
		editor.getTextField().setFont(font);
		editor.getTextField().setColumns(columns);
		if (editor.getTextField().getFormatter() instanceof DefaultFormatter)
		{
			// A typed number applies as it is typed; a number out of range, or not a number, is never committed.
			((DefaultFormatter) editor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		}
		typesOnPress(editor.getTextField());
		spinner.setMaximumSize(spinner.getPreferredSize());
		spinner.addChangeListener(e ->
		{
			if (!syncing)
			{
				write.accept(((Number) spinner.getValue()).intValue());
			}
		});
		return spinner;
	}

	/** A row of the menu: the caption at its left in grey, the control at its right, some room between; see-through. */
	private static JPanel row(String captionText, JComponent control)
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final JLabel caption = new JLabel(captionText);
		caption.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		caption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(caption);
		row.add(Box.createHorizontalStrut(ROW_CAPTION_GAP));
		row.add(Box.createHorizontalGlue());
		row.add(control);
		return row;
	}

	/**
	 * A colour row: the caption at its left, a swatch of the setting's colour at its right. A press on the swatch takes
	 * the menu down, opens the colour picker on that colour beside the swatch (the picker is told the swatch's centre on
	 * the screen), writes each colour the picker settles on (the swatch follows), and brings the menu back under the
	 * settings icon, with the "Colours" group open, when the picker closes.
	 *
	 * <p>Choice: the menu goes while the picker is open because the picker is a window of its own; a menu left up under
	 * a dialog is a menu nobody can use, and Swing would take it down on the first click in the dialog anyway.
	 * {@link #syncControls} sets the swatch from {@code read}, which also happens when the menu comes back.
	 *
	 * @param caption the row's name
	 * @param read the setting's current colour
	 * @param write what a new colour does: writes the setting
	 * @param pickerTitle the picker window's title
	 */
	private JPanel colourRow(String caption, Supplier<Color> read, Consumer<Color> write, String pickerTitle)
	{
		final JButton swatch = new JButton()
		{
			/** Fills the whole button with its background, whatever look and feel is on. */
			@Override
			protected void paintComponent(Graphics g)
			{
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		swatch.setFocusable(false);
		swatch.setContentAreaFilled(false);
		swatch.setOpaque(true);
		swatch.setBorder(new LineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
		final Dimension size = new Dimension(SWATCH_SIZE, SWATCH_SIZE);
		swatch.setPreferredSize(size);
		swatch.setMinimumSize(size);
		swatch.setMaximumSize(size);
		swatch.setToolTipText(caption);
		swatch.setBackground(read.get());
		swatch.addActionListener(e ->
		{
			// Read before the menu goes: the swatch is on the screen only while the menu is up.
			final Point at = new Point(swatch.getWidth() / 2, swatch.getHeight() / 2);
			SwingUtilities.convertPointToScreen(at, swatch);
			closeMenu();
			picker.pick(this, pickerTitle, read.get(), colour ->
			{
				write.accept(colour);
				swatch.setBackground(colour);
			}, colour -> reopenMenu(), at);
		});
		swatchSyncs.add(() -> swatch.setBackground(read.get()));
		return row(caption, swatch);
	}

	/**
	 * Sets the checks, the theme rows and the groups' choices, the card key box, the spinners and the colour swatches from
	 * the settings, without writing anything back; the how-to's coloured words follow the hover colour and its fold
	 * follows the setting, which "Reset to default" has just put back.
	 *
	 * <p>Choice: a stored size outside 8 to 24 shows as the nearest end of the range, and a stored delay outside 0 to 1000
	 * likewise.
	 */
	private void syncControls()
	{
		syncing = true;
		try
		{
			showCheck.setSelected(settings.showCards());
			colourCheck.setSelected(settings.colourHoverables());
			wikiCheck.setSelected(settings.wikiDescriptions());
			picturesCheck.setSelected(settings.wikiPictures());
			frostSlider.setValue(Math.max(MIN_FROST, Math.min(MAX_FROST, settings.frostOpacity())));
			frostValue.setText(String.valueOf(frostSlider.getValue()));
			bossCheck.setSelected(settings.bossCards());
			underlineCheck.setSelected(settings.alwaysUnderlineLevels());
			examineCheck.setSelected(settings.examineCards());
			choiceSyncs.forEach(Runnable::run);
			applyLocks();
			swatchSyncs.forEach(Runnable::run);
			showKey(settings.cardKey());
			keyBoxEnabled(settings.cardTrigger() == CardTrigger.HOLD_KEY);
			delaySpinner.setValue(Math.max(0, Math.min(MAX_CARD_DELAY, settings.cardDelayMs())));
			sizeSpinner.setValue(Math.max(MIN_CARD_SIZE, Math.min(MAX_CARD_SIZE, settings.cardFontSize())));
			refreshHowTo();
			applyFold(settings.howToFolded());
		}
		finally
		{
			syncing = false;
		}
	}

	/** The menu's last row: "Reset to default" at its left, "OK" at its right. */
	private JPanel okRow()
	{
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(2, 6, 2, 6));
		final JButton reset = button(RESET_TEXT);
		reset.addActionListener(e ->
		{
			resetToDefault.run();
			syncControls();
		});
		final JButton ok = button(OK_TEXT);
		ok.addActionListener(e -> closeMenu());
		row.add(reset);
		row.add(Box.createHorizontalGlue());
		row.add(ok);
		return row;
	}

	/** A small button in the menu's 12 px type, no wider than its word. */
	private static JButton button(String text)
	{
		final JButton button = new JButton(text);
		button.setFont(FontManager.getDefaultFont().deriveFont(12f));
		button.setFocusable(false);
		button.setMaximumSize(button.getPreferredSize());
		return button;
	}
}
