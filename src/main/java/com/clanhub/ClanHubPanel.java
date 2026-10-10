package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultFormatter;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The sidebar panel: one message card (a bold line and a grey line, centred) whose top row ends with the support
 * pair, and the settings menu the settings icon opens (STANDARD A4 and A5): the header, the six switches, the three
 * choice submenus, the card key box and the two number boxes (when a card opens, the key that opens it, its delay,
 * where it opens, how wide it grows, its text size), the two colour rows (the hover colour and the underline colour,
 * each a swatch that opens RuneLite's colour picker), then the OK row. The plugin replaces the card with its own views;
 * the pair, the menu and the type sizes stay.
 */
public class ClanHubPanel extends PluginPanel
{
	/** The plugin's GitHub page, what the menu header's fourth mark opens. */
	static final String GITHUB_URL = "https://github.com/2hBuilds/chat-hover-info";

	/** The menu's rows. */
	static final String RESET_TEXT = "Reset to default";
	static final String OK_TEXT = "OK";
	static final String SHOW_CARDS_TEXT = "Show hover cards";
	static final String COLOUR_TEXT = "Colour hoverable words";
	static final String WIKI_TEXT = "Wiki descriptions";
	static final String PICTURES_TEXT = "Wiki pictures";
	static final String BOSS_TEXT = "Boss cards";
	static final String ALWAYS_UNDERLINE_TEXT = "Underline level-ups";
	static final String CARD_TRIGGER_TEXT = "Show a card";
	static final String CARD_KEY_TEXT = "Card key";
	static final String CARD_KEY_TIP = "Press, then the key to hold";
	static final String CARD_DELAY_TEXT = "Card delay (ms)";
	static final String CARD_PLACE_TEXT = "Card place";
	static final String CARD_WIDTH_TEXT = "Card width";
	static final String CARD_SIZE_TEXT = "Card text size";
	static final String HOVER_COLOUR_TEXT = "Hover colour";
	static final String UNDERLINE_COLOUR_TEXT = "Underline colour";

	/** The card text size's smallest, default and largest, in px: the setting's own range. */
	static final int MIN_CARD_SIZE = 8;
	static final int DEFAULT_CARD_SIZE = 16;
	static final int MAX_CARD_SIZE = 24;
	/** The card delay's largest, in ms (its smallest is 0), and the spinner's step: the setting's own range. */
	static final int MAX_CARD_DELAY = 1000;
	static final int CARD_DELAY_STEP = 100;

	/** The card's padding, in px. */
	private static final int PADDING = 8;
	/** The card's message. */
	private static final String HEADLINE = "Nothing here yet";
	private static final String HELPER = "This card is the plugin's frame";
	/** The settings icon's size, in px. */
	private static final int ICON_SIZE = 12;
	/** A press on the settings icon this soon after the menu closed is the press that closed it, not a new open. */
	private static final long REOPEN_GUARD_NANOS = 250_000_000L;
	/** A colour swatch's side, in px. */
	private static final int SWATCH_SIZE = 16;
	/** What the card key box says while it waits for the key. */
	private static final String KEY_PROMPT = "Press a key";

	private final Runnable resetToDefault;
	private final PanelSettings settings;
	private final ColourPicker picker;
	private final SupportLinks support;
	private final JCheckBoxMenuItem showCheck;
	private final JCheckBoxMenuItem colourCheck;
	private final JCheckBoxMenuItem wikiCheck;
	private final JCheckBoxMenuItem picturesCheck;
	private final JCheckBoxMenuItem bossCheck;
	private final JCheckBoxMenuItem underlineCheck;
	private final JButton keyBox;
	private final JMenu triggerMenu;
	private final JMenu placeMenu;
	private final JMenu widthMenu;
	private final JSpinner delaySpinner;
	private final JSpinner sizeSpinner;
	private final JPopupMenu menu;
	/** What sets each choice submenu from its setting: selects the current choice's item and retitles the submenu. */
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
	 * @param resetToDefault what "Reset to default" does: puts the plugin's settings back to their defaults
	 * @param settings the settings the menu's controls show, read each time the menu opens and after "Reset to default",
	 *        and written when a control changes
	 * @param picker opens the colour picker a colour swatch asks for
	 */
	ClanHubPanel(Runnable resetToDefault, PanelSettings settings, ColourPicker picker)
	{
		super();
		this.resetToDefault = resetToDefault;
		this.settings = settings;
		this.picker = picker;
		this.support = new SupportLinks("2h Chat Hover Info", Version.CURRENT, GITHUB_URL, LinkBrowser::browse, this::closeMenu);
		this.showCheck = check(SHOW_CARDS_TEXT, settings::setShowCards);
		this.colourCheck = check(COLOUR_TEXT, settings::setColourHoverables);
		this.wikiCheck = check(WIKI_TEXT, settings::setWikiDescriptions);
		this.picturesCheck = check(PICTURES_TEXT, settings::setWikiPictures);
		this.bossCheck = check(BOSS_TEXT, settings::setBossCards);
		this.underlineCheck = check(ALWAYS_UNDERLINE_TEXT, settings::setAlwaysUnderlineLevels);
		this.keyBox = buildKeyBox();
		this.triggerMenu = choices(CARD_TRIGGER_TEXT, CardTrigger.values(), settings::cardTrigger, trigger ->
		{
			settings.setCardTrigger(trigger);
			keyBoxEnabled(trigger == CardTrigger.HOLD_KEY);
		});
		this.placeMenu = choices(CARD_PLACE_TEXT, CardPlace.values(), settings::cardPlace, settings::setCardPlace);
		this.widthMenu = choices(CARD_WIDTH_TEXT, CardWidth.values(), settings::cardWidth, settings::setCardWidth);
		this.delaySpinner = spinner(0, 0, MAX_CARD_DELAY, CARD_DELAY_STEP, 4, settings::setCardDelayMs);
		this.sizeSpinner = spinner(DEFAULT_CARD_SIZE, MIN_CARD_SIZE, MAX_CARD_SIZE, 1, 2, settings::setCardFontSize);
		this.menu = buildMenu();

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
		add(messageCard(), BorderLayout.NORTH);
	}

	/** The message card: the top row with the support pair at its right end, then the two centred lines. */
	private JPanel messageCard()
	{
		final JPanel card = new JPanel(new BorderLayout());
		card.setBackground(ColorScheme.DARK_GRAY_COLOR);
		card.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		final JPanel topRow = new JPanel(new BorderLayout());
		topRow.setOpaque(false);
		topRow.add(support.supportPair(settingsIcon()), BorderLayout.EAST);
		card.add(topRow, BorderLayout.NORTH);

		final JPanel lines = new JPanel();
		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		lines.setOpaque(false);
		lines.setBorder(new EmptyBorder(PADDING, 0, PADDING, 0));
		lines.add(line(HEADLINE, FontManager.getDefaultBoldFont().deriveFont(14f), Color.WHITE));
		lines.add(Box.createVerticalStrut(4));
		lines.add(line(HELPER, FontManager.getDefaultFont().deriveFont(12f), ColorScheme.LIGHT_GRAY_COLOR));
		card.add(lines, BorderLayout.CENTER);
		return card;
	}

	/** One centred line of the card. */
	private static JLabel line(String text, Font font, Color colour)
	{
		final JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setFont(font);
		label.setForeground(colour);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		return label;
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
	 * Shows the menu again under the settings icon it was last opened under, once a colour picker has closed. The reopen
	 * guard is not asked: it is for the icon press that closed the menu, and a picker closing seconds later is not that
	 * press. Nothing shows if the icon is gone from the screen or the menu is already up.
	 */
	private void reopenMenu()
	{
		final JLabel anchor = menuAnchor;
		if (anchor == null || !anchor.isShowing() || menu.isVisible())
		{
			return;
		}
		menu.show(anchor, 0, anchor.getHeight());
	}

	/** Takes the menu down: OK, a link press, or the reset. */
	private void closeMenu()
	{
		menu.setVisible(false);
	}

	/**
	 * The settings menu, in STANDARD A5's order: the header, a rule, the six switches, a rule, the three choice submenus
	 * and the card key and the two number rows (when a card opens, the key that opens it, the delay, where it opens, how
	 * wide it grows, the text size) and the two colour rows (the hover colour, the underline colour), a rule, then the OK
	 * row. The header is focusable, so the menu's window takes the keyboard when the menu opens and gives it to the
	 * header, which shows no caret; no box is focusable at rest, and a press moves the keyboard to the box pressed.
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
		popup.add(colourCheck);
		popup.add(wikiCheck);
		popup.add(picturesCheck);
		popup.add(bossCheck);
		popup.add(underlineCheck);
		popup.addSeparator();
		popup.add(triggerMenu);
		popup.add(row(CARD_KEY_TEXT, keyBox));
		popup.add(row(CARD_DELAY_TEXT, delaySpinner));
		popup.add(placeMenu);
		popup.add(widthMenu);
		popup.add(row(CARD_SIZE_TEXT, sizeSpinner));
		popup.add(colourRow(HOVER_COLOUR_TEXT, settings::hoverColour, settings::setHoverColour, HOVER_COLOUR_TEXT));
		popup.add(colourRow(UNDERLINE_COLOUR_TEXT, settings::alwaysUnderlineColour, settings::setAlwaysUnderlineColour,
			UNDERLINE_COLOUR_TEXT));
		popup.addSeparator();
		popup.add(okRow());
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
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
				closedAtNanos = System.nanoTime();
			}
		});
		return popup;
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

	/**
	 * A choice row: a submenu titled with the caption and the current choice, holding one radio item per choice in the
	 * order of {@code values}. Picking one writes it, retitles the submenu and closes the submenu alone; the main menu
	 * stays open, as it does for the switches. {@link #syncControls} sets the radio and the title from {@code read}.
	 *
	 * <p>Choice: submenus and not drop-downs, because a drop-down's list is a popup of its own, outside the menu's
	 * selection path, and Swing's menu machinery reads a click on it as a click outside and takes the whole menu down
	 * before the list can show. The submenu is as wide as its longest title, so that retitling never clips it. Each
	 * radio item is an anonymous subclass taking the release over through {@link #takeRelease}, as the switches are.
	 *
	 * @param caption the row's name, shown before the colon
	 * @param values the choices, shown in this order with their {@code toString} words
	 * @param read the setting's current choice
	 * @param write what picking a choice does: writes the setting
	 */
	private <T> JMenu choices(String caption, T[] values, Supplier<T> read, Consumer<T> write)
	{
		final Font font = FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE);
		final JMenu submenu = new JMenu();
		submenu.setFont(font);
		submenu.setForeground(Color.WHITE);
		final ButtonGroup group = new ButtonGroup();
		final List<JRadioButtonMenuItem> items = new ArrayList<>();
		for (final T value : values)
		{
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(value))
			{
				@Override
				protected void processMouseEvent(MouseEvent e)
				{
					if (!takeRelease(this, e, false))
					{
						super.processMouseEvent(e);
					}
				}
			};
			item.setFont(font);
			item.setForeground(Color.WHITE);
			item.addActionListener(e ->
			{
				if (!syncing)
				{
					write.accept(value);
					submenu.setText(title(caption, value));
					// Only the submenu goes: the path ends at the main menu, which stays up.
					MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu});
				}
			});
			group.add(item);
			submenu.add(item);
			items.add(item);
		}
		int width = 0;
		int height = 0;
		for (final T value : values)
		{
			submenu.setText(title(caption, value));
			final Dimension size = submenu.getPreferredSize();
			width = Math.max(width, size.width);
			height = Math.max(height, size.height);
		}
		submenu.setPreferredSize(new Dimension(width, height));
		submenu.setText(title(caption, values[0]));
		final List<T> choices = Arrays.asList(values);
		choiceSyncs.add(() ->
		{
			final T current = read.get();
			final int index = choices.indexOf(current);
			if (index < 0)
			{
				group.clearSelection();
				return;
			}
			items.get(index).setSelected(true);
			submenu.setText(title(caption, current));
		});
		return submenu;
	}

	/** A choice submenu's title: the caption, a colon, and the current choice's words. */
	private static String title(String caption, Object choice)
	{
		return caption + ": " + choice;
	}

	/**
	 * The card key box: a small button in the menu's 12 px type showing the key's words. A press arms it, and the next key
	 * pressed is the card key: Escape keeps the old one, Backspace or Delete clears it, any other key (a modifier alone
	 * included) is taken. The box is greyed unless "Show a card" is "While holding a key"; {@link #syncControls} sets it
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

	/** A row of the menu: the caption at its left in grey, the control at its right; see-through. */
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
		row.add(Box.createHorizontalGlue());
		row.add(control);
		return row;
	}

	/**
	 * A colour row: the caption at its left, a swatch of the setting's colour at its right. A press on the swatch takes
	 * the menu down, opens the colour picker on that colour, writes each colour the picker settles on (the swatch
	 * follows), and brings the menu back under the settings icon when the picker closes.
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
			closeMenu();
			picker.pick(this, pickerTitle, read.get(), colour ->
			{
				write.accept(colour);
				swatch.setBackground(colour);
			}, colour -> reopenMenu());
		});
		swatchSyncs.add(() -> swatch.setBackground(read.get()));
		return row(caption, swatch);
	}

	/**
	 * Sets the checks, the choice submenus, the card key box, the spinners and the colour swatches from the settings,
	 * without writing anything back.
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
			bossCheck.setSelected(settings.bossCards());
			underlineCheck.setSelected(settings.alwaysUnderlineLevels());
			choiceSyncs.forEach(Runnable::run);
			swatchSyncs.forEach(Runnable::run);
			showKey(settings.cardKey());
			keyBoxEnabled(settings.cardTrigger() == CardTrigger.HOLD_KEY);
			delaySpinner.setValue(Math.max(0, Math.min(MAX_CARD_DELAY, settings.cardDelayMs())));
			sizeSpinner.setValue(Math.max(MIN_CARD_SIZE, Math.min(MAX_CARD_SIZE, settings.cardFontSize())));
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
