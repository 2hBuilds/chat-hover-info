package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The sidebar panel: one message card (a bold line and a grey line, centred) whose top row ends with the support
 * pair, and the settings menu the settings icon opens (STANDARD A4 and A5): the header, the six switches, the five
 * editable rows (when a card opens, its delay, where it opens, how wide it grows, its text size), then the OK row. The colour and the key are set in
 * RuneLite's own settings panel, not here. The plugin replaces the card with its own views; the pair, the menu and the
 * type sizes stay.
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
	static final String CARD_DELAY_TEXT = "Card delay (ms)";
	static final String CARD_PLACE_TEXT = "Card place";
	static final String CARD_WIDTH_TEXT = "Card width";
	static final String CARD_SIZE_TEXT = "Card text size";

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

	private final Runnable resetToDefault;
	private final PanelSettings settings;
	private final SupportLinks support;
	private final JCheckBoxMenuItem showCheck;
	private final JCheckBoxMenuItem colourCheck;
	private final JCheckBoxMenuItem wikiCheck;
	private final JCheckBoxMenuItem picturesCheck;
	private final JCheckBoxMenuItem bossCheck;
	private final JCheckBoxMenuItem underlineCheck;
	private final JComboBox<CardTrigger> triggerCombo;
	private final JComboBox<CardPlace> placeCombo;
	private final JComboBox<CardWidth> widthCombo;
	private final JSpinner delaySpinner;
	private final JSpinner sizeSpinner;
	private final JPopupMenu menu;
	private long closedAtNanos = System.nanoTime() - 2 * REOPEN_GUARD_NANOS;
	/** True while the menu's controls are being set from the settings, so that setting them writes nothing back. */
	private boolean syncing;

	/**
	 * @param resetToDefault what "Reset to default" does: puts the plugin's settings back to their defaults
	 * @param settings the settings the menu's controls show, read each time the menu opens and after "Reset to default",
	 *        and written when a control changes
	 */
	ClanHubPanel(Runnable resetToDefault, PanelSettings settings)
	{
		super();
		this.resetToDefault = resetToDefault;
		this.settings = settings;
		this.support = new SupportLinks("2h Chat Hover Info", Version.CURRENT, GITHUB_URL, LinkBrowser::browse, this::closeMenu);
		this.showCheck = check(SHOW_CARDS_TEXT, settings::setShowCards);
		this.colourCheck = check(COLOUR_TEXT, settings::setColourHoverables);
		this.wikiCheck = check(WIKI_TEXT, settings::setWikiDescriptions);
		this.picturesCheck = check(PICTURES_TEXT, settings::setWikiPictures);
		this.bossCheck = check(BOSS_TEXT, settings::setBossCards);
		this.underlineCheck = check(ALWAYS_UNDERLINE_TEXT, settings::setAlwaysUnderlineLevels);
		this.triggerCombo = triggerCombo();
		this.placeCombo = placeCombo();
		this.widthCombo = widthCombo();
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
		menu.show(anchor, 0, anchor.getHeight());
	}

	/** Takes the menu down: OK, a link press, or the reset. */
	private void closeMenu()
	{
		menu.setVisible(false);
	}

	/**
	 * The settings menu, in STANDARD A5's order: the header, a rule, the six switches, a rule, the five editable rows
	 * (when a card opens, the delay, where it opens, how wide it grows, the text size), a rule, then the OK row.
	 */
	private JPopupMenu buildMenu()
	{
		final JPopupMenu popup = new JPopupMenu();
		popup.setBorder(new EmptyBorder(5, 5, 5, 5));
		popup.add(support.header());
		popup.addSeparator();
		popup.add(showCheck);
		popup.add(colourCheck);
		popup.add(wikiCheck);
		popup.add(picturesCheck);
		popup.add(bossCheck);
		popup.add(underlineCheck);
		popup.addSeparator();
		popup.add(row(CARD_TRIGGER_TEXT, triggerCombo));
		popup.add(row(CARD_DELAY_TEXT, delaySpinner));
		popup.add(row(CARD_PLACE_TEXT, placeCombo));
		popup.add(row(CARD_WIDTH_TEXT, widthCombo));
		popup.add(row(CARD_SIZE_TEXT, sizeSpinner));
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

	/** A switch: a check in the menu's 12 px type, white on the menu's own ground, writing its state when clicked. */
	private static JCheckBoxMenuItem check(String text, Consumer<Boolean> write)
	{
		final JCheckBoxMenuItem check = new JCheckBoxMenuItem(text);
		check.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		check.setForeground(Color.WHITE);
		check.addActionListener(e -> write.accept(check.isSelected()));
		return check;
	}

	/** The "Show a card" dropdown, in the menu's 12 px type and no wider than its longest choice. */
	private JComboBox<CardTrigger> triggerCombo()
	{
		final JComboBox<CardTrigger> combo = new JComboBox<>(CardTrigger.values());
		combo.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		combo.setMaximumSize(combo.getPreferredSize());
		combo.addActionListener(e ->
		{
			if (!syncing && combo.getSelectedItem() != null)
			{
				settings.setCardTrigger((CardTrigger) combo.getSelectedItem());
			}
		});
		return combo;
	}

	/** The "Card place" dropdown, in the menu's 12 px type and no wider than its longest choice. */
	private JComboBox<CardPlace> placeCombo()
	{
		final JComboBox<CardPlace> combo = new JComboBox<>(CardPlace.values());
		combo.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		combo.setMaximumSize(combo.getPreferredSize());
		combo.addActionListener(e ->
		{
			if (!syncing && combo.getSelectedItem() != null)
			{
				settings.setCardPlace((CardPlace) combo.getSelectedItem());
			}
		});
		return combo;
	}

	/** The "Card width" dropdown, in the menu's 12 px type and no wider than its longest choice. */
	private JComboBox<CardWidth> widthCombo()
	{
		final JComboBox<CardWidth> combo = new JComboBox<>(CardWidth.values());
		combo.setFont(FontManager.getDefaultFont().deriveFont((float) SupportLinks.MENU_FONT_SIZE));
		combo.setMaximumSize(combo.getPreferredSize());
		combo.addActionListener(e ->
		{
			if (!syncing && combo.getSelectedItem() != null)
			{
				settings.setCardWidth((CardWidth) combo.getSelectedItem());
			}
		});
		return combo;
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
	 * Sets the checks, the dropdown and the spinners from the settings, without writing anything back.
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
			triggerCombo.setSelectedItem(settings.cardTrigger());
			placeCombo.setSelectedItem(settings.cardPlace());
			widthCombo.setSelectedItem(settings.cardWidth());
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
