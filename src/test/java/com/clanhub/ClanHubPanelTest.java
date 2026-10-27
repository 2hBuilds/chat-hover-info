package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTheme;
import com.clanhub.ClanHubConfig.CardTime;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.clanhub.ClanHubConfig.ExamineLine;
import com.clanhub.ClanHubConfig.SidebarTheme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * {@link ClanHubPanel}'s settings menu, opened the way a player opens it: the panel in a laid-out frame, a press on the
 * settings icon, and the menu read back from the screen's component tree.
 */
public class ClanHubPanelTest
{
	/** The how-to's theme tip as a player reads it: its last line. */
	private static final String THEME_TIP = "Tip: change theme in settings.";
	/** The empty list's tip as a player reads it (the user's words). */
	private static final String EMPTY_LIST_TIP = "Tip: Pick a theme in the settings menu.";

	/** What the panel wrote: the values handed to the settings' setters. */
	private final List<Boolean> shown = new ArrayList<>();
	private final List<Boolean> coloured = new ArrayList<>();
	private final List<Boolean> wiki = new ArrayList<>();
	private final List<Boolean> pictures = new ArrayList<>();
	private final List<Boolean> bosses = new ArrayList<>();
	private final List<Boolean> examines = new ArrayList<>();
	private final List<Boolean> underlines = new ArrayList<>();
	private final List<SidebarTheme> sidebarLooks = new ArrayList<>();
	private final List<CardTrigger> triggers = new ArrayList<>();
	private final List<Keybind> keys = new ArrayList<>();
	private final List<CardPlace> places = new ArrayList<>();
	private final List<CardWidth> widths = new ArrayList<>();
	private final List<CardTheme> themes = new ArrayList<>();
	private final List<ExamineLine> examineLines = new ArrayList<>();
	private final List<CardTime> cardTimes = new ArrayList<>();
	private final List<Integer> delays = new ArrayList<>();
	private final List<Integer> sizes = new ArrayList<>();
	private final List<Color> hoverColours = new ArrayList<>();
	private final List<Color> underlineColours = new ArrayList<>();
	private final List<Color> nameColours = new ArrayList<>();

	/** What the settings stub answers for "Name colour": the write of the colour sets it too. */
	private Color nameColour = new Color(220, 138, 0);

	/** The key and the show mode the settings stub answers: a test may set them before the menu opens. */
	private Keybind cardKey = Keybind.ALT;
	private CardTrigger trigger = CardTrigger.HOVER;

	/** What the settings stub answers for "Card time": the write of the choice sets it too. */
	private CardTime cardTime = CardTime.AGO;

	/** What the settings stub answers for "Card theme": the write of the choice sets it too. */
	private CardTheme cardTheme = CardTheme.CLASSIC;

	/** What the settings stub answers for "Sidebar theme": the write of the choice sets it too. */
	private SidebarTheme sidebarLook = SidebarTheme.SAME;

	/** What the settings stub answers for "Colour hoverable words": the write of the colour check sets it too. */
	private boolean colourOn = true;

	/** What the settings stub answers for the how-to's fold, and what the panel wrote for it. */
	private boolean howToFolded = false;
	private final List<Boolean> folds = new ArrayList<>();

	/** The recent cards the panel under test lists. */
	private final CardHistory history = new CardHistory();

	/** What the viewer stub was asked to show, and how many times to hide. */
	private final List<Object[]> viewerShows = new ArrayList<>();
	private int viewerHides;
	/** The pictures the viewer was asked to show beside a theme list: the picture and the rectangle of each ask. */
	private final List<Object[]> viewerPictures = new ArrayList<>();

	/** A viewer that shows nothing: it records what it was asked. */
	private final CardViewer viewer = new CardViewer()
	{
		@Override
		public void show(HoverCard card, Rectangle rowOnScreen)
		{
			viewerShows.add(new Object[] {card, rowOnScreen});
		}

		@Override
		public void showPicture(BufferedImage picture, Rectangle besideOnScreen)
		{
			viewerPictures.add(new Object[] {picture, besideOnScreen});
		}

		@Override
		public void hide()
		{
			viewerHides++;
		}
	};

	/** An icon source that gives nothing. */
	private final IconSource icons = (card, onIcon) -> { };

	/** What "Open wiki" and "Copy" handed over. */
	private final List<String> browsed = new ArrayList<>();
	private final List<String> copied = new ArrayList<>();

	/** What the picker stub was asked: the parent, the title, the starting colour and the point of each call. */
	private final List<Object[]> pickerCalls = new ArrayList<>();
	private Consumer<Color> pickerOnChange;
	private Consumer<Color> pickerOnClose;

	/** A colour picker that opens nothing: it records the call and keeps the callbacks for the test to call. */
	private final ColourPicker picker = (parent, title, current, onChange, onClose, at) ->
	{
		pickerCalls.add(new Object[] {parent, title, current, at});
		pickerOnChange = onChange;
		pickerOnClose = onClose;
	};

	/** The settings of a player who has never opened them, recording every write. */
	private final PanelSettings settings = new PanelSettings()
	{
		@Override
		public boolean showCards()
		{
			return true;
		}

		@Override
		public void setShowCards(boolean show)
		{
			shown.add(show);
		}

		@Override
		public boolean colourHoverables()
		{
			return colourOn;
		}

		@Override
		public void setColourHoverables(boolean colour)
		{
			colourOn = colour;
			coloured.add(colour);
		}

		@Override
		public boolean wikiDescriptions()
		{
			return true;
		}

		@Override
		public void setWikiDescriptions(boolean on)
		{
			wiki.add(on);
		}

		@Override
		public boolean wikiPictures()
		{
			return true;
		}

		@Override
		public void setWikiPictures(boolean on)
		{
			pictures.add(on);
		}

		@Override
		public boolean bossCards()
		{
			return true;
		}

		@Override
		public void setBossCards(boolean on)
		{
			bosses.add(on);
		}

		@Override
		public boolean examineCards()
		{
			return true;
		}

		@Override
		public void setExamineCards(boolean on)
		{
			examines.add(on);
		}

		@Override
		public ExamineLine examineLine()
		{
			return ExamineLine.NAME_FIRST;
		}

		@Override
		public void setExamineLine(ExamineLine line)
		{
			examineLines.add(line);
		}

		@Override
		public SidebarTheme sidebarLook()
		{
			return sidebarLook;
		}

		@Override
		public void setSidebarLook(SidebarTheme look)
		{
			sidebarLook = look;
			sidebarLooks.add(look);
		}

		@Override
		public CardTime cardTime()
		{
			return cardTime;
		}

		@Override
		public void setCardTime(CardTime time)
		{
			cardTime = time;
			cardTimes.add(time);
		}

		@Override
		public boolean alwaysUnderlineLevels()
		{
			return false;
		}

		@Override
		public void setAlwaysUnderlineLevels(boolean on)
		{
			underlines.add(on);
		}

		@Override
		public Color hoverColour()
		{
			return new Color(59, 130, 246);
		}

		@Override
		public void setHoverColour(Color colour)
		{
			hoverColours.add(colour);
		}

		@Override
		public Color nameColour()
		{
			return nameColour;
		}

		@Override
		public void setNameColour(Color colour)
		{
			nameColour = colour;
			nameColours.add(colour);
		}

		@Override
		public Color alwaysUnderlineColour()
		{
			return new Color(150, 150, 150);
		}

		@Override
		public void setAlwaysUnderlineColour(Color colour)
		{
			underlineColours.add(colour);
		}

		@Override
		public CardTrigger cardTrigger()
		{
			return trigger;
		}

		@Override
		public void setCardTrigger(CardTrigger trigger)
		{
			triggers.add(trigger);
		}

		@Override
		public Keybind cardKey()
		{
			return cardKey;
		}

		@Override
		public void setCardKey(Keybind key)
		{
			keys.add(key);
		}

		@Override
		public CardPlace cardPlace()
		{
			return CardPlace.CURSOR;
		}

		@Override
		public void setCardPlace(CardPlace place)
		{
			places.add(place);
		}

		@Override
		public CardWidth cardWidth()
		{
			return CardWidth.WIDE;
		}

		@Override
		public void setCardWidth(CardWidth width)
		{
			widths.add(width);
		}

		@Override
		public CardTheme cardTheme()
		{
			return cardTheme;
		}

		@Override
		public void setCardTheme(CardTheme theme)
		{
			cardTheme = theme;
			themes.add(theme);
		}

		@Override
		public int cardDelayMs()
		{
			return 0;
		}

		@Override
		public void setCardDelayMs(int delayMs)
		{
			delays.add(delayMs);
		}

		@Override
		public int cardFontSize()
		{
			return 16;
		}

		@Override
		public void setCardFontSize(int size)
		{
			sizes.add(size);
		}

		@Override
		public boolean howToFolded()
		{
			return howToFolded;
		}

		@Override
		public void setHowToFolded(boolean folded)
		{
			folds.add(folded);
		}
	};

	/** The panel under test, built with the fixture's settings, history, viewer, icons and recorders. */
	private ClanHubPanel newPanel()
	{
		return newPanel(icons);
	}

	/** The panel under test, as {@link #newPanel()} builds it, with {@code source} for its pictures. */
	private ClanHubPanel newPanel(IconSource source)
	{
		return new ClanHubPanel(() -> { }, settings, picker, history, viewer, source, browsed::add, copied::add);
	}

	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theMenuHoldsTheSwitchesTheRowsAndTheOkRow() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);

				final Component[] parts = menu.getComponents();
				assertEquals("header, rule, the Hover cards check, rule, the two theme rows, rule, four groups, rule, OK row", 13,
					parts.length);
				assertTrue("the header is the support header's one panel", parts[0] instanceof JPanel);
				assertEquals("the header's title, version and links", 3, ((JPanel) parts[0]).getComponentCount());
				assertTrue("a rule", parts[1] instanceof JPopupMenu.Separator);
				assertTrue("the Hover cards check", parts[2] instanceof JCheckBoxMenuItem);
				assertTrue("a rule", parts[3] instanceof JPopupMenu.Separator);
				assertTrue("the card theme row", parts[4] instanceof ValueMenu);
				assertTrue("the sidebar theme row", parts[5] instanceof ValueMenu);
				assertTrue("a rule", parts[6] instanceof JPopupMenu.Separator);
				for (int i = 7; i <= 10; i++)
				{
					assertTrue("a group", parts[i] instanceof JMenu && !(parts[i] instanceof ValueMenu));
				}
				assertEquals("the four groups in order", List.of("Card", "When it shows", "What gets a card", "Colours"),
					Arrays.asList(((JMenu) parts[7]).getText(), ((JMenu) parts[8]).getText(), ((JMenu) parts[9]).getText(),
						((JMenu) parts[10]).getText()));
				assertTrue("a rule", parts[11] instanceof JPopupMenu.Separator);
				assertTrue("the OK row", parts[12] instanceof JPanel);

				final JCheckBoxMenuItem showCheck = (JCheckBoxMenuItem) parts[2];
				assertEquals("Hover cards", showCheck.getText());
				assertTrue("ticked, as the setting is", showCheck.isSelected());

				final ValueMenu themeMenu = (ValueMenu) parts[4];
				assertEquals("Card theme", themeMenu.getText());
				assertEquals("Classic", themeMenu.value());
				assertRadios(themeMenu, CardTheme.values(), CardTheme.CLASSIC);

				final ValueMenu sidebarMenu = (ValueMenu) parts[5];
				assertEquals("Sidebar theme", sidebarMenu.getText());
				assertEquals("Same as cards", sidebarMenu.value());
				assertEquals("one radio item per look and a rule after the second", SidebarTheme.values().length + 1,
					sidebarMenu.getMenuComponentCount());
				assertTrue("the first look is Same as cards, selected", radio(sidebarMenu, 0).isSelected()
					&& "Same as cards".equals(radio(sidebarMenu, 0).getText()));
				assertEquals("RuneLite", radio(sidebarMenu, 1).getText());
				assertTrue("a rule after RuneLite", sidebarMenu.getMenuComponent(2) instanceof JPopupMenu.Separator);
				for (int i = 0; i < CardTheme.values().length; i++)
				{
					assertEquals("then the card themes in order", CardTheme.values()[i].toString(),
						radio(sidebarMenu, 3 + i).getText());
					assertFalse("none of them selected", radio(sidebarMenu, 3 + i).isSelected());
				}

				final JPopupMenu card = ((JMenu) parts[7]).getPopupMenu();
				final JPopupMenu when = ((JMenu) parts[8]).getPopupMenu();
				final JPopupMenu what = ((JMenu) parts[9]).getPopupMenu();
				final JPopupMenu colours = ((JMenu) parts[10]).getPopupMenu();

				final JCheckBoxMenuItem colourCheck = checkSaying(colours, "Colour names");
				assertTrue("ticked, as the setting is", colourCheck.isSelected());
				final JCheckBoxMenuItem wikiCheck = checkSaying(what, "Wiki text");
				assertTrue("ticked, as the setting is", wikiCheck.isSelected());
				final JCheckBoxMenuItem picturesCheck = checkSaying(what, "Wiki pictures");
				assertTrue("ticked, as the setting is", picturesCheck.isSelected());
				final JCheckBoxMenuItem bossCheck = checkSaying(what, "Boss cards");
				assertTrue("ticked, as the setting is", bossCheck.isSelected());
				final JCheckBoxMenuItem underlineCheck = checkSaying(colours, "Underline level-ups");
				assertFalse("not ticked, as the setting is", underlineCheck.isSelected());
				final JCheckBoxMenuItem examineCheck = checkSaying(what, "Examine cards");
				assertTrue("ticked, as the setting is", examineCheck.isSelected());

				assertRadiosIn(when, CardTrigger.values(), CardTrigger.HOVER);

				final JPanel keyRow = rowSaying(when, "Key");
				assertTrue("the key row ends in a button", lastOf(keyRow) instanceof JButton);
				assertEquals("Alt", ((JButton) lastOf(keyRow)).getText());
				assertFalse("greyed unless the show mode is 'While holding a key'", lastOf(keyRow).isEnabled());

				final JPanel delayRow = rowSaying(when, "Delay (ms)");
				final JSpinner delaySpinner = (JSpinner) lastOf(delayRow);
				assertEquals(0, delaySpinner.getValue());
				assertTrue("no wider than four digits", ((JSpinner.DefaultEditor) delaySpinner.getEditor())
					.getTextField().getColumns() <= 4);

				assertRadiosIn(card, CardPlace.values(), CardPlace.CURSOR);
				assertTogglesIn(card, 0, CardWidth.values(), CardWidth.WIDE);
				assertTogglesIn(card, 1, CardTime.values(), CardTime.AGO);

				assertRadiosIn(what, ExamineLine.values(), ExamineLine.NAME_FIRST);

				final JPanel sizeRow = rowSaying(card, "Text size");
				assertFalse("see-through", sizeRow.isOpaque());
				final JSpinner sizeSpinner = (JSpinner) lastOf(sizeRow);
				assertEquals(16, sizeSpinner.getValue());
				assertTrue("no wider than four digits", ((JSpinner.DefaultEditor) sizeSpinner.getEditor())
					.getTextField().getColumns() <= 4);

				final JPanel hoverRow = rowSaying(colours, "Hover colour");
				assertTrue("the hover colour row ends in a swatch", lastOf(hoverRow) instanceof JButton);
				assertEquals(new Color(59, 130, 246), lastOf(hoverRow).getBackground());
				final JPanel underlineRow = rowSaying(colours, "Underline colour");
				assertTrue("the underline colour row ends in a swatch", lastOf(underlineRow) instanceof JButton);
				assertEquals(new Color(150, 150, 150), lastOf(underlineRow).getBackground());
				final JPanel nameRow = rowSaying(colours, "Sidebar names");
				assertTrue("the name colour row ends in a swatch", lastOf(nameRow) instanceof JButton);
				assertEquals(new Color(220, 138, 0), lastOf(nameRow).getBackground());

				final List<String> okRow = new ArrayList<>();
				for (final Component part : ((JPanel) parts[12]).getComponents())
				{
					if (part instanceof JButton)
					{
						okRow.add(((JButton) part).getText());
					}
				}
				assertEquals("the OK row is last: Reset to default, then OK", List.of("Reset to default", "OK"), okRow);

				assertTrue("opening the menu writes nothing back", shown.isEmpty() && coloured.isEmpty() && wiki.isEmpty()
					&& pictures.isEmpty() && bosses.isEmpty() && examines.isEmpty() && underlines.isEmpty() && triggers.isEmpty() && keys.isEmpty() && places.isEmpty() && widths.isEmpty() && themes.isEmpty() && examineLines.isEmpty() && cardTimes.isEmpty() && delays.isEmpty()
					&& sidebarLooks.isEmpty() && sizes.isEmpty() && hoverColours.isEmpty() && underlineColours.isEmpty() && nameColours.isEmpty());
				colourCheck.doClick(0);
				assertEquals("un-ticking the colour check says false", List.of(false), coloured);
				assertTrue("and writes nothing to the wiki check", wiki.isEmpty());
				wikiCheck.doClick(0);
				assertEquals("un-ticking the wiki check says false, once", List.of(false), wiki);
				assertEquals("and the colour check was not touched again", List.of(false), coloured);
				assertTrue("and nothing to the pictures check", pictures.isEmpty());
				picturesCheck.doClick(0);
				assertEquals("un-ticking the pictures check says false, once", List.of(false), pictures);
				assertEquals("and the wiki check was not touched again", List.of(false), wiki);
				assertEquals("nor the colour check", List.of(false), coloured);
				assertTrue("and nothing to the boss check", bosses.isEmpty());
				bossCheck.doClick(0);
				assertEquals("un-ticking the boss check says false, once", List.of(false), bosses);
				assertEquals("and the wiki check was not touched again", List.of(false), wiki);
				assertTrue("nor the first check", shown.isEmpty());
				assertTrue("nor the fourth", underlines.isEmpty());
				underlineCheck.doClick(0);
				assertEquals("ticking the fourth check says true, once", List.of(true), underlines);
				assertEquals("and the wiki check was not touched again", List.of(false), wiki);
				assertTrue("and nothing to the examine check", examines.isEmpty());
				examineCheck.doClick(0);
				assertEquals("un-ticking the examine check says false, once", List.of(false), examines);
				assertEquals("and the underline check was not touched again", List.of(true), underlines);
				assertEquals("nor the boss check", List.of(false), bosses);
				radio(sidebarMenu, 1).doClick(0);
				assertEquals("choosing 'RuneLite' says so, once", List.of(SidebarTheme.RUNELITE), sidebarLooks);
				assertEquals("and the row says so, its text unchanged", "RuneLite", sidebarMenu.value());
				assertEquals("Sidebar theme", sidebarMenu.getText());
				assertEquals("and the examine check was not touched again", List.of(false), examines);
				radiosIn(when).get(1).doClick(0);
				assertEquals("choosing 'While holding a key' says so", List.of(CardTrigger.HOLD_KEY), triggers);
				assertTrue("and the key box is enabled", lastOf(keyRow).isEnabled());
				radiosIn(card).get(1).doClick(0);
				assertEquals("choosing 'Above the chat' says so, once", List.of(CardPlace.CHAT), places);
				assertEquals("and the trigger was not touched again", List.of(CardTrigger.HOLD_KEY), triggers);
				togglesIn(card).get(1).doClick(0);
				assertEquals("choosing 'Medium' says so, once", List.of(CardWidth.MEDIUM), widths);
				assertEquals("and the place was not touched again", List.of(CardPlace.CHAT), places);
				radiosIn(what).get(1).doClick(0);
				assertEquals("choosing 'Description only' says so, once", List.of(ExamineLine.DESCRIPTION), examineLines);
				assertEquals("and the width was not touched again", List.of(CardWidth.MEDIUM), widths);
				togglesIn(card).get(4).doClick(0);
				assertEquals("choosing '14:32' says so, once", List.of(CardTime.CLOCK_24), cardTimes);
				assertEquals("and the examine line was not touched again", List.of(ExamineLine.DESCRIPTION), examineLines);
				delaySpinner.setValue(300);
				assertEquals("setting the delay spinner to 300 says 300", List.of(300), delays);
				sizeSpinner.setValue(20);
				assertEquals("setting the size spinner to 20 says 20", List.of(20), sizes);
				assertTrue("the first check was not touched", shown.isEmpty());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/** Lot 9A: a click on a switch flips it and writes it once, and the menu stays up until OK takes it down. */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void aSwitchClickKeepsTheMenuOpen() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final JPopupMenu colours = open(menu, groupOf(menu, "Colours"));
				final JCheckBoxMenuItem colourCheck = checkSaying(colours, "Colour names");
				final boolean before = colourCheck.isSelected();
				assertTrue("ticked, as the setting is", before);

				click(colourCheck);
				assertTrue("the menu is still up after the click", menu.isVisible());
				assertTrue("and the group", colours.isVisible());
				assertEquals("the check flipped", !before, colourCheck.isSelected());
				assertEquals("one write, un-ticked", List.of(false), coloured);

				click(colourCheck);
				assertTrue("the menu is still up after the second click", menu.isVisible());
				assertTrue("and the group", colours.isVisible());
				assertEquals("the check flipped back", before, colourCheck.isSelected());
				assertEquals("a second write, ticked again", List.of(false, true), coloured);

				JButton ok = null;
				for (final Component part : ((JPanel) menu.getComponents()[12]).getComponents())
				{
					if (part instanceof JButton && "OK".equals(((JButton) part).getText()))
					{
						ok = (JButton) part;
					}
				}
				assertNotNull("the OK button is in the OK row", ok);
				ok.doClick(0);
				assertFalse("OK takes the menu down", menu.isVisible());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 9B: a click on a choice writes it and the menu stays up until OK takes it down. Lot 11A: a choice is in a group
	 * now, and the group stays up with the menu: nothing closes when it is picked.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void aChoiceClickKeepsTheMenuOpenAndClosesTheSubmenu() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final JPopupMenu when = open(menu, groupOf(menu, "When it shows"));

				final JRadioButtonMenuItem onClick = radioSaying(when, "On click");
				assertFalse("not the selected one yet", onClick.isSelected());
				click(onClick);

				assertTrue("the menu is still up after the choice", menu.isVisible());
				assertTrue("the group is still up too", when.isVisible());
				assertTrue("the chosen item is the selected one", onClick.isSelected());
				assertEquals("one write, 'On click'", List.of(CardTrigger.CLICK), triggers);

				JButton ok = null;
				for (final Component part : ((JPanel) menu.getComponents()[12]).getComponents())
				{
					if (part instanceof JButton && "OK".equals(((JButton) part).getText()))
					{
						ok = (JButton) part;
					}
				}
				assertNotNull("the OK button is in the OK row", ok);
				ok.doClick(0);
				assertFalse("OK takes the menu down", menu.isVisible());
				assertFalse("and the group with it", when.isVisible());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10P: the "Card theme" submenu holds the themes with Classic selected at first; a click on Stone writes it once,
	 * selects it, retitles the submenu, and the menu stays up.
	 */
	// Re-aimed by the lead, 2026-10-25 (lot 10Q): a fourth theme, Wiki.
	// Re-aimed by the lead, 2026-10-26 (lot 10R): eight space themes after Wiki.
	// Re-aimed by the lead, 2026-10-26 (lot 10S): an eighth check, "Sidebar follows the theme".
	// Re-aimed by the lead, 2026-10-26 (lot 10T): twelve scene themes after Cosmic edge.
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theThemeSubmenuWritesTheTheme() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final ValueMenu themeMenu = valueMenuSaying(menu, "Card theme");
				assertEquals("its value is the theme", "Classic", themeMenu.value());
				assertEquals("one radio item per theme", 24, themeMenu.getMenuComponentCount());
				assertEquals("Classic", radio(themeMenu, 0).getText());
				assertEquals("Stone", radio(themeMenu, 1).getText());
				assertEquals("Void", radio(themeMenu, 2).getText());
				assertEquals("Wiki", radio(themeMenu, 3).getText());
				assertEquals("Nebula", radio(themeMenu, 4).getText());
				assertEquals("Aurora", radio(themeMenu, 5).getText());
				assertEquals("Galaxy", radio(themeMenu, 6).getText());
				assertEquals("Planet rise", radio(themeMenu, 7).getText());
				assertEquals("Eclipse", radio(themeMenu, 8).getText());
				assertEquals("Constellation", radio(themeMenu, 9).getText());
				assertEquals("Black hole", radio(themeMenu, 10).getText());
				assertEquals("Cosmic edge", radio(themeMenu, 11).getText());
				assertEquals("Forest", radio(themeMenu, 12).getText());
				assertEquals("Ocean", radio(themeMenu, 13).getText());
				assertEquals("Autumn", radio(themeMenu, 14).getText());
				assertEquals("Sakura", radio(themeMenu, 15).getText());
				assertEquals("Steampunk", radio(themeMenu, 16).getText());
				assertEquals("Cyberpunk", radio(themeMenu, 17).getText());
				assertEquals("Synthwave", radio(themeMenu, 18).getText());
				assertEquals("Volcano", radio(themeMenu, 19).getText());
				assertEquals("Runic", radio(themeMenu, 20).getText());
				assertEquals("Spooky", radio(themeMenu, 21).getText());
				assertEquals("Garden", radio(themeMenu, 22).getText());
				assertEquals("Lavender", radio(themeMenu, 23).getText());
				assertRadios(themeMenu, CardTheme.values(), CardTheme.CLASSIC);
				assertTrue("opening the menu writes no theme", themes.isEmpty());

				MenuSelectionManager.defaultManager().setSelectedPath(
					new MenuElement[] {menu, themeMenu, themeMenu.getPopupMenu()});
				assertTrue("the submenu is up", themeMenu.getPopupMenu().isVisible());
				final JRadioButtonMenuItem stone = radio(themeMenu, 1);
				click(stone);

				assertEquals("one write, Stone", List.of(CardTheme.STONE), themes);
				assertEquals("the stub is at Stone now", CardTheme.STONE, cardTheme);
				assertTrue("Stone is the selected item", stone.isSelected());
				assertFalse("Classic is not", radio(themeMenu, 0).isSelected());
				assertEquals("the row shows the choice at its right", "Stone", themeMenu.value());
				assertEquals("and its text is still the caption", "Card theme", themeMenu.getText());
				assertTrue("the menu is still up after the choice", menu.isVisible());
				assertFalse("the submenu is down", themeMenu.getPopupMenu().isVisible());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 9D: a press on a colour swatch takes the menu down and asks the picker for that colour; each colour the picker
	 * settles on is written and shown on the swatch; the picker closing brings the menu back.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void aSwatchOpensThePickerAndTheMenuComesBack() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final JPopupMenu colours = open(menu, groupOf(menu, "Colours"));
				final JButton hoverSwatch = (JButton) lastOf(rowSaying(colours, "Hover colour"));
				assertEquals(new Color(59, 130, 246), hoverSwatch.getBackground());
				hoverSwatch.doClick(0);
				assertFalse("the menu goes while the picker is open", menu.isVisible());
				assertEquals("the picker was asked once", 1, pickerCalls.size());
				assertEquals("Hover colour", pickerCalls.get(0)[1]);
				assertEquals(new Color(59, 130, 246), pickerCalls.get(0)[2]);
				pickerOnChange.accept(new Color(1, 2, 3));
				assertEquals("the new colour is written", List.of(new Color(1, 2, 3)), hoverColours);
				assertEquals("and shown on the swatch", new Color(1, 2, 3), hoverSwatch.getBackground());
				assertTrue("and the underline colour was not touched", underlineColours.isEmpty());
				pickerOnClose.accept(new Color(1, 2, 3));
				assertTrue("the menu comes back when the picker closes", menu.isVisible());

				final JButton underlineSwatch = (JButton) lastOf(rowSaying(colours, "Underline colour"));
				assertEquals(new Color(150, 150, 150), underlineSwatch.getBackground());
				underlineSwatch.doClick(0);
				assertFalse("the menu goes again", menu.isVisible());
				assertEquals("the picker was asked a second time", 2, pickerCalls.size());
				assertEquals("Underline colour", pickerCalls.get(1)[1]);
				assertEquals(new Color(150, 150, 150), pickerCalls.get(1)[2]);
				pickerOnChange.accept(new Color(4, 5, 6));
				assertEquals("the new colour is written", List.of(new Color(4, 5, 6)), underlineColours);
				assertEquals("and shown on the swatch", new Color(4, 5, 6), underlineSwatch.getBackground());
				assertEquals("and the hover colour was not written again", List.of(new Color(1, 2, 3)), hoverColours);
				pickerOnClose.accept(new Color(4, 5, 6));
				assertTrue("the menu comes back again", menu.isVisible());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10M: a submenu left open closes when the mouse enters a plain row of the menu; the menu itself stays up, and
	 * with no submenu open entering a row changes nothing. Lot 11A: the OK row is the one plain row of the menu now (the
	 * others are in groups), and the rows inside a group close nothing, as the group they are in would go with it.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void aPlainRowClosesAnOpenSubmenu() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final ValueMenu themeMenu = valueMenuSaying(menu, "Card theme");
				final JMenu whenGroup = groupOf(menu, "When it shows");
				final JMenu coloursGroup = groupOf(menu, "Colours");
				final JPanel nameRow = rowSaying(coloursGroup.getPopupMenu(), "Sidebar names");
				final JSpinner delaySpinner = (JSpinner) lastOf(rowSaying(whenGroup.getPopupMenu(), "Delay (ms)"));
				final JPanel okRow = (JPanel) menu.getComponents()[12];

				MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu});
				enter(okRow);
				assertPathIsThe(menu);
				assertTrue("with no submenu open nothing happens to the menu", menu.isVisible());

				final MenuElement[] open = {menu, themeMenu, themeMenu.getPopupMenu()};
				MenuSelectionManager.defaultManager().setSelectedPath(open);
				assertTrue("the theme submenu is up", themeMenu.getPopupMenu().isVisible());
				enter(okRow);
				assertPathIsThe(menu);
				assertFalse("the OK row took the submenu down", themeMenu.getPopupMenu().isVisible());
				assertTrue("the menu is still up", menu.isVisible());

				final MenuElement[] openWhen = {menu, whenGroup, whenGroup.getPopupMenu()};
				MenuSelectionManager.defaultManager().setSelectedPath(openWhen);
				assertTrue("the When it shows group is up", whenGroup.getPopupMenu().isVisible());
				enter(delaySpinner);
				enter(((JSpinner.DefaultEditor) delaySpinner.getEditor()).getTextField());
				assertEquals("a row inside the group leaves the path as it was", 3,
					MenuSelectionManager.defaultManager().getSelectedPath().length);
				assertTrue("and the group up", whenGroup.getPopupMenu().isVisible());
				enter(okRow);
				assertPathIsThe(menu);
				assertFalse("the OK row took the group down", whenGroup.getPopupMenu().isVisible());
				assertTrue("the menu is still up after all of it", menu.isVisible());

				MenuSelectionManager.defaultManager().setSelectedPath(
					new MenuElement[] {menu, coloursGroup, coloursGroup.getPopupMenu()});
				assertTrue("the Colours group is up", coloursGroup.getPopupMenu().isVisible());
				enter(nameRow);
				assertTrue("a colour row inside the group does not take it down", coloursGroup.getPopupMenu().isVisible());
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10M: a press on a colour swatch hands the picker the swatch's centre on the screen, as it was before the menu
	 * came down.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theSwatchTellsThePickerWhereItIs() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final JPopupMenu colours = open(menu, groupOf(menu, "Colours"));
				final JButton hoverSwatch = (JButton) lastOf(rowSaying(colours, "Hover colour"));
				final Rectangle onScreen = new Rectangle(hoverSwatch.getLocationOnScreen(), hoverSwatch.getSize());
				hoverSwatch.doClick(0);
				assertFalse("the menu goes while the picker is open", menu.isVisible());
				assertEquals("the picker was asked once", 1, pickerCalls.size());
				final Point at = (Point) pickerCalls.get(0)[3];
				assertNotNull("the picker was told a point", at);
				assertTrue("the point " + at + " is inside the swatch " + onScreen, onScreen.contains(at));
				pickerOnClose.accept(new Color(59, 130, 246));
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10L: a row's name is drawn in the name colour; the Name colour swatch opens the picker, and each colour the
	 * picker settles on is written and shows in the rows at once, with the picker still open.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theNameColourPaintsLive() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String line = "GMucker has completed a medium combat task: Smite Fight.";
			history.add(line, Collections.<HoverCard>singletonList(card("Smite Fight", "Smite_Fight", line)),
				System.currentTimeMillis());
			flush();
			onEdt(() ->
			{
				frame[0].validate();
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("one entry", 1, list.getModel().getSize());
				// Re-aimed by the lead, 2026-10-25 (lot 10M): the 2h orange, the user's word.
				assertEquals("the name is drawn in the 2h orange", new Color(220, 138, 0),
					labelSaying(cellOf(list, 0), "GMucker").getForeground());

				press(settingsIcon(panel[0]));
				final JPopupMenu menu = popupMenu(frame[0]);
				assertNotNull("the menu is on screen", menu);
				final JPopupMenu colours = open(menu, groupOf(menu, "Colours"));
				final JButton nameSwatch = (JButton) lastOf(rowSaying(colours, "Sidebar names"));
				assertEquals(new Color(220, 138, 0), nameSwatch.getBackground());
				nameSwatch.doClick(0);
				assertFalse("the menu goes while the picker is open", menu.isVisible());
				assertEquals("the picker was asked once", 1, pickerCalls.size());
				assertEquals("Name colour", pickerCalls.get(0)[1]);
				assertEquals(new Color(220, 138, 0), pickerCalls.get(0)[2]);

				pickerOnChange.accept(new Color(1, 2, 3));
				assertEquals("the new colour is written, once", List.of(new Color(1, 2, 3)), nameColours);
				assertEquals("and the swatch shows it", new Color(1, 2, 3), nameSwatch.getBackground());
				assertEquals("and the row shows it with the picker still open", new Color(1, 2, 3),
					labelSaying(cellOf(list, 0), "GMucker").getForeground());
				assertFalse("the menu stays down until the picker closes", menu.isVisible());
				assertTrue("the hover colour was not touched", hoverColours.isEmpty());
				assertTrue("nor the underline colour", underlineColours.isEmpty());

				pickerOnClose.accept(new Color(1, 2, 3));
				assertTrue("the menu comes back when the picker closes", menu.isVisible());
				menu.setVisible(false);
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 10L: when the plugin's settings change from somewhere else (RuneLite's own settings page), the sidebar draws its
	 * rows in the new name colour and its how-to in the new words once it is told.
	 */
	@Test
	public void aSettingsChangeRepaintsTheSidebar() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String line = "GMucker has completed a medium combat task: Smite Fight.";
			history.add(line, Collections.<HoverCard>singletonList(card("Smite Fight", "Smite_Fight", line)),
				System.currentTimeMillis());
			flush();
			onEdt(() ->
			{
				frame[0].validate();
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("one entry", 1, list.getModel().getSize());
				assertEquals("the how-to starts coloured", "Hover the coloured name for its card.", actionLine(panel[0]));

				nameColour = new Color(9, 9, 9);
				panel[0].settingsChanged();
				assertEquals("the name is drawn in the new colour", new Color(9, 9, 9),
					labelSaying(cellOf(list, 0), "GMucker").getForeground());

				colourOn = false;
				panel[0].settingsChanged();
				assertEquals("the how-to reads the plain line", "Hover a name in a broadcast for its card.",
					actionLine(panel[0]));
				assertFalse("in no colour", rawActionLine(panel[0]).contains("<font"));
				assertTrue("and nothing was written back", nameColours.isEmpty() && coloured.isEmpty());
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 9F: the card key box waits for a press, takes the next key (Escape keeps the old one, Backspace clears it, a
	 * modifier alone is a key), and holds the keyboard only while it waits.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theKeyBoxTakesTheNextKey() throws Exception
	{
		trigger = CardTrigger.HOLD_KEY;
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final JPopupMenu when = open(menu, groupOf(menu, "When it shows"));
				final JButton box = (JButton) lastOf(rowSaying(when, "Key"));
				assertEquals("Alt", box.getText());
				assertTrue("enabled in the key mode", box.isEnabled());
				assertFalse("not focusable at rest", box.isFocusable());

				box.doClick(0);
				assertEquals("Press a key", box.getText());
				assertTrue("focusable while it waits", box.isFocusable());

				final KeyEvent g = keyPress(box, KeyEvent.VK_G, 0);
				for (final KeyListener listener : box.getKeyListeners())
				{
					listener.keyPressed(g);
				}
				assertEquals("G is written", List.of(new Keybind(KeyEvent.VK_G, 0)), keys);
				assertEquals("and shown", "G", box.getText());
				assertFalse("the keyboard is let go", box.isFocusable());
				assertTrue("the key press is consumed", g.isConsumed());
				assertTrue("the menu stays up", menu.isVisible());

				box.doClick(0);
				assertEquals("Press a key", box.getText());
				final KeyEvent escape = keyPress(box, KeyEvent.VK_ESCAPE, 0);
				for (final KeyListener listener : box.getKeyListeners())
				{
					listener.keyPressed(escape);
				}
				assertEquals("Escape writes nothing", 1, keys.size());
				assertEquals("and the old key is back", "G", box.getText());
				assertFalse("the keyboard is let go", box.isFocusable());
				assertTrue("the menu stays up", menu.isVisible());

				box.doClick(0);
				final KeyEvent backspace = keyPress(box, KeyEvent.VK_BACK_SPACE, 0);
				for (final KeyListener listener : box.getKeyListeners())
				{
					listener.keyPressed(backspace);
				}
				assertEquals("Backspace clears the key", List.of(new Keybind(KeyEvent.VK_G, 0), Keybind.NOT_SET), keys);
				assertEquals("Not set", box.getText());

				box.doClick(0);
				final KeyEvent control = keyPress(box, KeyEvent.VK_CONTROL, InputEvent.CTRL_DOWN_MASK);
				for (final KeyListener listener : box.getKeyListeners())
				{
					listener.keyPressed(control);
				}
				assertEquals("Ctrl alone is a key", 3, keys.size());
				assertEquals(Keybind.CTRL, keys.get(2));
				assertEquals("Ctrl", box.getText());

				box.doClick(0);
				assertEquals("Press a key", box.getText());
				for (final FocusListener listener : box.getFocusListeners())
				{
					listener.focusLost(new FocusEvent(box, FocusEvent.FOCUS_LOST));
				}
				assertEquals("losing the keyboard writes nothing", 3, keys.size());
				assertEquals("and the old key is back", "Ctrl", box.getText());
				assertFalse("and the box is unfocusable again", box.isFocusable());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 9F: no number box takes the keyboard when the menu opens (a blinking caret in the delay box was the fault); a
	 * left press gives it the keyboard, losing the keyboard takes it away again, and a typed number applies as it is typed.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theNumberBoxesTakeTheKeyboardOnlyWhenPressedAndWriteAsYouType() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final JSpinner delaySpinner = (JSpinner) lastOf(rowSaying(open(menu, groupOf(menu, "When it shows")), "Delay (ms)"));
				final JSpinner sizeSpinner = (JSpinner) lastOf(rowSaying(open(menu, groupOf(menu, "Card")), "Text size"));
				final JFormattedTextField delayField = ((JSpinner.DefaultEditor) delaySpinner.getEditor()).getTextField();
				final JFormattedTextField sizeField = ((JSpinner.DefaultEditor) sizeSpinner.getEditor()).getTextField();
				assertFalse("the delay box is not focusable at rest", delayField.isFocusable());
				assertFalse("the size box is not focusable at rest", sizeField.isFocusable());

				delayField.dispatchEvent(new MouseEvent(delayField, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
					InputEvent.BUTTON1_DOWN_MASK, 1, 1, 1, false, MouseEvent.BUTTON1));
				assertTrue("a left press makes it focusable", delayField.isFocusable());
				assertFalse("and not the other box", sizeField.isFocusable());
				for (final FocusListener listener : delayField.getFocusListeners())
				{
					listener.focusLost(new FocusEvent(delayField, FocusEvent.FOCUS_LOST));
				}
				assertFalse("losing the keyboard makes it unfocusable again", delayField.isFocusable());

				delayField.setText("300");
				assertEquals("a typed number applies without Enter", List.of(300), delays);
				delayField.setText("5000");
				assertEquals("a number out of range is not written", List.of(300), delays);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 9G: the menu's header is the one focusable part at rest, so the menu's window takes the keyboard when the menu
	 * opens and the boxes can take it from the window on a press; the boxes stay unfocusable at rest.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theHeaderHoldsTheKeyboardWhenTheMenuOpens() throws Exception
	{
		trigger = CardTrigger.HOLD_KEY;
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = new JFrame();
			frame.setFocusableWindowState(false);
			frame.add(panel);
			frame.setBounds(100, 100, 500, 600);
			frame.setVisible(true);
			try
			{
				frame.validate();
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				assertTrue("the menu is up", menu.isVisible());

				final Component header = menu.getComponents()[0];
				assertTrue("the header is a panel", header instanceof JPanel);
				assertTrue("the header is focusable", header.isFocusable());
				assertEquals("the part the window gives the keyboard to is the header", header,
					new LayoutFocusTraversalPolicy().getFirstComponent(SwingUtilities.getWindowAncestor(menu)));

				final JPopupMenu when = groupOf(menu, "When it shows").getPopupMenu();
				final JSpinner delaySpinner = (JSpinner) lastOf(rowSaying(when, "Delay (ms)"));
				final JSpinner sizeSpinner = (JSpinner) lastOf(rowSaying(groupOf(menu, "Card").getPopupMenu(), "Text size"));
				assertFalse("the delay box is not focusable at rest",
					((JSpinner.DefaultEditor) delaySpinner.getEditor()).getTextField().isFocusable());
				assertFalse("the size box is not focusable at rest",
					((JSpinner.DefaultEditor) sizeSpinner.getEditor()).getTextField().isFocusable());
				final JButton box = (JButton) lastOf(rowSaying(when, "Key"));
				assertFalse("the key box is not focusable at rest", box.isFocusable());

				box.doClick(0);
				assertEquals("Press a key", box.getText());
				assertTrue("the key box is focusable while it waits", box.isFocusable());
				final KeyEvent g = keyPress(box, KeyEvent.VK_G, 0);
				for (final KeyListener listener : box.getKeyListeners())
				{
					listener.keyPressed(g);
				}
				assertEquals("G is written", List.of(new Keybind(KeyEvent.VK_G, 0)), keys);
				assertFalse("the key box is unfocusable again", box.isFocusable());
				assertTrue("and the header is still focusable", header.isFocusable());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10C: the sidebar holds the how-to, the recent cards' header and its count and clear link, and no placeholder.
	 * Lot 10E: the how-to is five rows, one per kind of card, and the action line.
	 */
	@Test
	public void theSidebarHoldsTheHowToAndTheHistory() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				final List<String> texts = new ArrayList<>();
				final List<String> raw = new ArrayList<>();
				for (final JLabel label : labelsIn(panel))
				{
					if (label.getText() == null)
					{
						continue;
					}
					raw.add(label.getText());
					texts.add(plain(label.getText()));
				}
				assertTrue("the how-to's header", texts.contains("How to use"));
				// Re-aimed by the lead, 2026-10-25 (lot 10E): the how-to is the icon list of what gets a card.
				for (final ClanHubPanel.HowToRow row : ClanHubPanel.HOW_TO_ROWS)
				{
					assertTrue("a row says " + row.label + row.rest, texts.contains((row.label + row.rest).trim()));
				}
				assertEquals("five kinds of card", 5, ClanHubPanel.HOW_TO_ROWS.size());
				// Re-aimed by the lead, 2026-10-25 (lot 10G): the header bar, the action line in two parts.
				assertTrue("the action line, whole", texts.contains("Hover the coloured name for its card."));
				assertTrue("the wiki line", texts.contains("Right-click it for the wiki."));
				// Re-aimed by the lead, 2026-10-25 (lot 10F): the heading "WHAT IT WORKS ON", under the action line.
				assertTrue("the heading over the rows", texts.contains("WHAT IT WORKS ON"));
				final int actionY = labelSaying(panel, "Hover the coloured name for its card.").getLocationOnScreen().y;
				final int wikiY = labelSaying(panel, "Right-click it for the wiki.").getLocationOnScreen().y;
				final int headingY = labelSaying(panel, "WHAT IT WORKS ON").getLocationOnScreen().y;
				final int firstRowY = rowsOfTheHowTo(panel).get(0).getLocationOnScreen().y;
				assertTrue("the wiki line is under the action line", actionY < wikiY);
				assertTrue("the wiki line is above the heading", wikiY < headingY);
				assertTrue("the action line is above the heading", actionY < headingY);
				assertTrue("and the heading is above the first row", headingY < firstRowY);
				assertTrue("the history's header", texts.contains("Recent cards"));
				// Re-aimed by the lead, 2026-10-25 (lot 10F): the count is the session's, no cap in it.
				assertTrue("the count, with nothing kept", texts.contains("0 this session"));
				assertTrue("the clear link", texts.contains("Clear"));
				for (final String text : texts)
				{
					assertFalse("no placeholder: " + text, text.contains("Nothing here yet"));
				}
				boolean blue = false;
				for (final String text : raw)
				{
					blue |= text.contains("<font color='#3b82f6'>coloured");
				}
				assertTrue("'coloured name' stands in the hover colour", blue);
				assertEquals("a row per kind, no more", 5, rowsOfTheHowTo(panel).size());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10D: with "Colour hoverable words" off the how-to's action line is the plain one, with no colour; ticking the switch
	 * in the menu brings the coloured line at once, and un-ticking it the plain one again.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void theFirstLineFollowsTheColourSwitch() throws Exception
	{
		// Re-aimed by the lead, 2026-10-25 (lot 10E): the how-to is the icon list of what gets a card.
		colourOn = false;
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				// Re-aimed by the lead, 2026-10-25 (lot 10G): the header bar, the action line in two parts.
				assertEquals("plain while the switch is off", "Hover a name in a broadcast for its card.", actionLine(panel));
				assertFalse("and in no colour", rawActionLine(panel).contains("<font"));
				assertTrue("the wiki line is there", textsIn(panel).contains("Right-click it for the wiki."));

				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				final JCheckBoxMenuItem colourCheck = checkSaying(open(menu, groupOf(menu, "Colours")), "Colour names");
				assertFalse("not ticked, as the setting is", colourCheck.isSelected());

				click(colourCheck);
				assertEquals("ticking writes true", List.of(true), coloured);
				assertEquals("the coloured line at once", "Hover the coloured name for its card.", actionLine(panel));
				assertTrue("with the words in the hover colour", rawActionLine(panel).contains("<font color='#3b82f6'>coloured"));
				assertTrue("the wiki line unchanged", textsIn(panel).contains("Right-click it for the wiki."));

				click(colourCheck);
				assertEquals("un-ticking writes false", List.of(true, false), coloured);
				assertEquals("the plain line again", "Hover a name in a broadcast for its card.", actionLine(panel));
				assertFalse("in no colour", rawActionLine(panel).contains("<font"));
				assertTrue("and the wiki line still", textsIn(panel).contains("Right-click it for the wiki."));
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/** Lot 10D: a row whose line starts with its card's title and ": " shows the rest alone, and no label reads the whole line. */
	@Test
	public void anExamineRowShowsTheDescriptionAlone() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String line = "Law rune: Used for teleport spells.";
			history.add(line, Collections.<HoverCard>singletonList(card("Law rune", "Law_rune", line)), 1000L);
			flush();
			onEdt(() ->
			{
				frame[0].validate();
				// Re-aimed by the lead, 2026-10-25 (lot 10F): the row is a cell of the history's list, read through its renderer.
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("one entry", 1, list.getModel().getSize());
				final List<String> texts = textsIn(cellOf(list, 0));
				// Re-aimed by the lead, 2026-10-25 (lot 10J): an examine row says "You examined", the user's row style 4.
				assertTrue("the title", texts.contains("Law rune"));
				assertTrue("who is you", texts.contains("You"));
				assertTrue("what is examined", texts.contains("examined"));
				assertFalse("no label reads the description", texts.contains("Used for teleport spells."));
				assertFalse("no label reads the whole line", texts.contains(line));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/** Lot 10J: a row's second line says who, what and when (the user's row style 4), and no label reads the whole line. */
	@Test
	public void aRowSaysWhoWhatAndWhen() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String line = "GMucker has completed a medium combat task: Smite Fight.";
			history.add(line, Collections.<HoverCard>singletonList(card("Smite Fight", "Smite_Fight", line)),
				System.currentTimeMillis());
			flush();
			onEdt(() ->
			{
				frame[0].validate();
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("one entry", 1, list.getModel().getSize());
				final List<String> texts = textsIn(cellOf(list, 0));
				assertTrue("the title", texts.contains("Smite Fight"));
				assertTrue("who", texts.contains("GMucker"));
				assertTrue("what", texts.contains("medium task"));
				// Re-aimed by the lead, 2026-10-25 (lot 10N): the row's short time.
				assertTrue("when", texts.contains("now"));
				assertFalse("no label reads the whole line", texts.contains(line));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/** Lot 10K: a row's time reads as set: how long ago, the 24-hour clock or the 12-hour clock of the player's own zone. */
	@Test
	public void aRowShowsTheTimeAsSet() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String line = "GMucker has completed a medium combat task: Smite Fight.";
			final long at = System.currentTimeMillis();
			history.add(line, Collections.<HoverCard>singletonList(card("Smite Fight", "Smite_Fight", line)), at);
			flush();
			final String clock24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
				.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()));
			final String clock12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
				.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()));
			onEdt(() ->
			{
				frame[0].validate();
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("one entry", 1, list.getModel().getSize());
				// Re-aimed by the lead, 2026-10-25 (lot 10N): the row's short time.
				assertTrue("at 'ago' the row reads 'now'", textsIn(cellOf(list, 0)).contains("now"));

				cardTime = CardTime.CLOCK_24;
				assertTrue("at the 24-hour clock the row reads the moment as HH:mm", textsIn(cellOf(list, 0)).contains(clock24));
				assertFalse("and no longer 'now'", textsIn(cellOf(list, 0)).contains("now"));

				cardTime = CardTime.CLOCK_12;
				assertTrue("at the 12-hour clock the row reads the moment as h:mm a", textsIn(cellOf(list, 0)).contains(clock12));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 10N: in a list as wide as RuneLite gives the sidebar a long title takes a second line and its row is taller, a
	 * short one keeps one; the time sits in the row's top corner, above the name, and the kind is said in full.
	 */
	@Test
	public void aLongTitleTakesTwoLinesAndTheFullKind() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String dartLine = "LOVE MINERS received a new collection log item: Atlatl dart (1/50).";
			final String taskLine = "GMucker has completed a medium combat task: Deranged Archaeologist Champion.";
			final long now = System.currentTimeMillis();
			history.add(dartLine, Collections.<HoverCard>singletonList(card("Atlatl dart", "Atlatl_dart", dartLine)), now);
			history.add(taskLine, Collections.<HoverCard>singletonList(card("Deranged Archaeologist Champion",
				"Deranged_Archaeologist_Champion", taskLine)), now);
			flush();
			onEdt(() ->
			{
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("two entries", 2, list.getModel().getSize());
				listWidthTo(frame[0], list, 226);

				final Rectangle long1 = list.getCellBounds(0, 0);
				final Rectangle short1 = list.getCellBounds(1, 1);
				assertEquals("a two-line title: 60 px and the gap", 62, long1.height);
				assertEquals("a one-line title: 44 px and the gap", 46, short1.height);
				assertTrue("the first cell is taller than the second", long1.height > short1.height);

				final Container longCell = cellOf(list, 0);
				layOut(longCell, long1);
				final List<String> longTexts = textsIn(longCell);
				assertTrue("the title's words are in its labels", String.join(" ", longTexts).contains("Deranged"));
				assertTrue("both of them", String.join(" ", longTexts).contains("Champion"));
				assertFalse("no label says '...'", String.join(" ", longTexts).contains("..."));
				assertWhole(longCell);
				assertTrue("the kind", longTexts.contains("medium task"));
				assertTrue("the time is above the name", yIn(longCell, labelSaying(longCell, "now"))
					< yIn(longCell, labelSaying(longCell, "GMucker")));

				final Container shortCell = cellOf(list, 1);
				layOut(shortCell, short1);
				assertTrue("the full kind", textsIn(shortCell).contains("collection log"));
				assertWhole(shortCell);
				assertTrue("the time is above the name", yIn(shortCell, labelSaying(shortCell, "now"))
					< yIn(shortCell, labelSaying(shortCell, "LOVE MINERS")));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/** Lot 10N: a row's lines are counted again when the list's width changes: wide enough, a long title takes one line. */
	@Test
	public void aWiderListMeasuresTheRowsAgain() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String taskLine = "GMucker has completed a medium combat task: Deranged Archaeologist Champion.";
			history.add(taskLine, Collections.<HoverCard>singletonList(card("Deranged Archaeologist Champion",
				"Deranged_Archaeologist_Champion", taskLine)), System.currentTimeMillis());
			flush();
			onEdt(() ->
			{
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				listWidthTo(frame[0], list, 226);
				assertEquals("two lines at 226 px", 62, list.getCellBounds(0, 0).height);

				listWidthTo(frame[0], list, 500);
				assertEquals("one line at 500 px, the counts measured again", 46, list.getCellBounds(0, 0).height);

				listWidthTo(frame[0], list, 226);
				assertEquals("and two again when it narrows", 62, list.getCellBounds(0, 0).height);
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 12A: a row's height does not depend on its time. At the sidebar's width the same title added 1 minute, 59 minutes
	 * and 23 hours old (the time reading "1m", "59m" and "23h") makes three rows of one height, each laid out whole with no
	 * label reading "...", for every title of a sweep that widens a few px at a time, so that one of them fits beside "1m"
	 * and not beside "59m".
	 */
	@Test
	public void aRowsHeightDoesNotDependOnItsTime() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final List<String> titles = sweepTitles();
			final String[] reads = {"1m", "59m", "23h"};
			final long now = System.currentTimeMillis();
			// A little past the minute, the 59th minute and the 23rd hour, so that the words hold while the test runs.
			final long[] ages = {90_000L, 59 * 60_000L + 30_000L, 23 * 3_600_000L + 1_800_000L};
			for (final String title : titles)
			{
				final String line = "GMucker has completed a medium combat task: " + title + ".";
				for (final long age : ages)
				{
					history.add(line, Collections.<HoverCard>singletonList(card(title, "Abyssal_whip", line)), now - age);
				}
			}
			flush();
			onEdt(() ->
			{
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("three rows a title", titles.size() * 3, list.getModel().getSize());
				listWidthTo(frame[0], list, 226);

				final Map<String, List<Integer>> heights = new LinkedHashMap<>();
				final Map<String, List<String>> times = new LinkedHashMap<>();
				for (int i = 0; i < list.getModel().getSize(); i++)
				{
					final String title = list.getModel().getElementAt(i).card().title();
					final Rectangle bounds = list.getCellBounds(i, i);
					final Container cell = cellOf(list, i);
					layOut(cell, bounds);
					final List<String> texts = textsIn(cell);
					assertFalse("no label of the row '" + title + "' says '...'", String.join(" ", texts).contains("..."));
					assertWhole(cell);
					String time = "?";
					for (final String read : reads)
					{
						if (texts.contains(read))
						{
							time = read;
						}
					}
					heights.computeIfAbsent(title, t -> new ArrayList<>()).add(bounds.height);
					times.computeIfAbsent(title, t -> new ArrayList<>()).add(time);
				}

				for (final String title : titles)
				{
					assertEquals("'" + title + "' was read at the three times", new HashSet<>(Arrays.asList(reads)),
						new HashSet<>(times.get(title)));
					assertEquals("'" + title + "' gave the heights " + heights.get(title) + " at the times " + times.get(title)
						+ ": one height", 1, new HashSet<>(heights.get(title)).size());
				}
				assertEquals("the first title takes one line", 46, heights.get(titles.get(0)).get(0).intValue());
				assertEquals("the last takes two beside any time", 62,
					heights.get(titles.get(titles.size() - 1)).get(0).intValue());
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 12A: a change of the Card time setting has the rows measured again. The same sweep of titles is measured under
	 * "4m ago"; once the clock is the 12-hour one and the sidebar is told, every row is laid out whole with no label
	 * reading "..." and is as high as a fresh sidebar measures it under that clock; set back to "4m ago", every row has its
	 * first height again.
	 */
	@Test
	public void theTimeSettingMeasuresTheRowsAgain() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[2];
		final JFrame[] frame = new JFrame[2];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final List<String> titles = sweepTitles();
			// Four and a half minutes old, so that the row reads "4m" while the test runs.
			final long at = System.currentTimeMillis() - 270_000L;
			for (final String title : titles)
			{
				final String line = "GMucker has completed a medium combat task: " + title + ".";
				history.add(line, Collections.<HoverCard>singletonList(card(title, "Abyssal_whip", line)), at);
			}
			flush();
			onEdt(() ->
			{
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("a row a title", titles.size(), list.getModel().getSize());
				listWidthTo(frame[0], list, 226);
				final int[] agoHeights = new int[titles.size()];
				int agoTall = 0;
				for (int i = 0; i < agoHeights.length; i++)
				{
					agoHeights[i] = list.getCellBounds(i, i).height;
					agoTall += agoHeights[i] > 46 ? 1 : 0;
				}
				assertTrue("under '4m ago' the sweep has titles of one line and of two", agoTall > 0 && agoTall < agoHeights.length);

				cardTime = CardTime.CLOCK_12;
				panel[0].settingsChanged();
				panel[1] = newPanel();
				frame[1] = frameOf(panel[1]);
				final JList<CardHistory.Entry> fresh = historyListIn(panel[1]);
				listWidthTo(frame[1], fresh, 226);
				int clockTall = 0;
				for (int i = 0; i < agoHeights.length; i++)
				{
					final String title = list.getModel().getElementAt(i).card().title();
					final Rectangle bounds = list.getCellBounds(i, i);
					final Container cell = cellOf(list, i);
					layOut(cell, bounds);
					assertFalse("no label of the row '" + title + "' says '...'", String.join(" ", textsIn(cell)).contains("..."));
					assertWhole(cell);
					assertEquals("the fresh sidebar lists the same row", title, fresh.getModel().getElementAt(i).card().title());
					assertEquals("'" + title + "' is as high as a fresh sidebar measures it under the 12-hour clock",
						fresh.getCellBounds(i, i).height, bounds.height);
					clockTall += bounds.height > 46 ? 1 : 0;
				}
				assertTrue("the wider time gives more titles a second line", clockTall > agoTall);

				cardTime = CardTime.AGO;
				panel[0].settingsChanged();
				for (int i = 0; i < agoHeights.length; i++)
				{
					assertEquals("back at '4m ago', row " + i + " has its first height", agoHeights[i],
						list.getCellBounds(i, i).height);
				}
			});
		}
		finally
		{
			onEdt(() ->
			{
				for (final JFrame each : frame)
				{
					if (each != null)
					{
						each.dispose();
					}
				}
			});
		}
	}

	/**
	 * Lot 12A, the lead's case after the review: a profile switch changes the Card time setting from RuneLite's own
	 * thread, so the list can draw before the sidebar is told. With every row drawn under the 12-hour clock before
	 * {@code settingsChanged} runs, the call still has the list measure the rows again: each row is as high as its cell
	 * asks.
	 */
	@Test
	public void aDrawBeforeTheSidebarIsToldStillMeasuresTheRowsAgain() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final List<String> titles = sweepTitles();
			// Four and a half minutes old, so that the row reads "4m" while the test runs.
			final long at = System.currentTimeMillis() - 270_000L;
			for (final String title : titles)
			{
				final String line = "GMucker has completed a medium combat task: " + title + ".";
				history.add(line, Collections.<HoverCard>singletonList(card(title, "Abyssal_whip", line)), at);
			}
			flush();
			onEdt(() ->
			{
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				listWidthTo(frame[0], list, 226);
				panel[0].settingsChanged();
				final int[] agoHeights = new int[titles.size()];
				for (int i = 0; i < agoHeights.length; i++)
				{
					agoHeights[i] = list.getCellBounds(i, i).height;
				}

				cardTime = CardTime.CLOCK_12;
				// The draw in the gap: every row's cell asked for under the new setting, as a repaint asks for its rows.
				for (int i = 0; i < agoHeights.length; i++)
				{
					cellOf(list, i);
				}
				panel[0].settingsChanged();
				int moved = 0;
				for (int i = 0; i < agoHeights.length; i++)
				{
					final int asked = cellOf(list, i).getPreferredSize().height;
					assertEquals("row " + i + " is as high as its cell asks under the 12-hour clock", asked,
						list.getCellBounds(i, i).height);
					moved += asked != agoHeights[i] ? 1 : 0;
				}
				assertTrue("the 12-hour clock gives some titles a second line", moved > 0);
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/** Lot 10C: a press on the how-to's header folds the lines and writes it; a second press opens them; a stored fold is kept. */
	@Test
	public void theHowToFoldsAndRemembers() throws Exception
	{
		// Re-aimed by the lead, 2026-10-25 (lot 10E): the how-to is the icon list of what gets a card.
		// Re-aimed by the lead, 2026-10-25 (lot 10G): the press is made on the title bar, and the lines are the new ones.
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				final JLabel line = labelSaying(panel, "Hover the coloured name for its card.");
				final JLabel wiki = labelSaying(panel, "Right-click it for the wiki.");
				assertTrue("the lines show at first", line.isShowing());
				assertTrue("both of them", wiki.isShowing());
				final Component bar = howToBarIn(panel);
				assertSame("a press on the title lands on the bar", bar, pressTarget(labelSaying(panel, "How to use")));

				leftPress(bar);
				assertFalse("a press folds the lines away", line.isShowing());
				assertFalse("the wiki line too", wiki.isShowing());
				assertTrue("the bar stays", bar.isShowing());
				assertEquals("and writes true, once", List.of(true), folds);

				leftPress(bar);
				assertTrue("a second press brings them back", line.isShowing());
				assertTrue("the wiki line too", wiki.isShowing());
				assertEquals("and writes false", List.of(true, false), folds);
			}
			finally
			{
				frame.dispose();
			}
		});
		howToFolded = true;
		folds.clear();
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				assertFalse("a panel built with the fold stored starts folded",
					labelSaying(panel, "Hover the coloured name for its card.").isShowing());
				assertFalse("the wiki line too", labelSaying(panel, "Right-click it for the wiki.").isShowing());
				assertTrue("the bar still shows", labelSaying(panel, "How to use").isShowing());
				assertTrue("and building it wrote nothing", folds.isEmpty());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10G: the how-to's title bar runs the top card's whole inner width and has a ground of its own, a shade off the
	 * card's; a press at its left end folds the how-to (writing true once) and a press at its right end opens it again
	 * (writing false once); the mouse entering it changes its ground and the mouse leaving it brings the ground back.
	 */
	@Test
	public void theBarFoldsOnAPressAnywhereOnIt() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				final JPanel bar = howToBarIn(panel);
				final Container card = (Container) panel.getComponent(0);
				assertTrue("the bar is inside the top card", SwingUtilities.isDescendingFrom(bar, card));
				final int inner = card.getWidth() - card.getInsets().left - card.getInsets().right;
				assertTrue("the card has a width", inner > 0);
				assertEquals("the bar is as wide as the card's inner width", inner, bar.getWidth());
				assertFalse("its ground is not the card's", bar.getBackground().equals(card.getBackground()));

				final Color atRest = bar.getBackground();
				bar.dispatchEvent(new MouseEvent(bar, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 3, 3, 0, false));
				final Color hot = bar.getBackground();
				assertFalse("the mouse on it changes its ground", hot.equals(atRest));
				bar.dispatchEvent(new MouseEvent(bar, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1, -1, 0, false));
				assertEquals("the mouse off it brings the ground back", atRest, bar.getBackground());

				leftPressAt(bar, 3, 3);
				assertEquals("a press at its left end folds, writing true once", List.of(true), folds);
				assertFalse("the lines are away", labelSaying(panel, "Right-click it for the wiki.").isShowing());
				leftPressAt(bar, bar.getWidth() - 3, bar.getHeight() / 2);
				assertEquals("a press at its right end opens, writing false once", List.of(true, false), folds);
				assertTrue("the lines are back", labelSaying(panel, "Right-click it for the wiki.").isShowing());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10E: building the panel asks the icon source once for each picture the how-to's rows need (the Collection log, the
	 * quest, total level and combat sprites), and a picture handed to the quest's callback, from any thread, ends up as the
	 * icon of the quest row, scaled down to the 20 px box and never up; the examine row's glass is drawn in code.
	 */
	@Test
	public void theHowToRowsAskForTheirIcons() throws Exception
	{
		final List<String> asks = new ArrayList<>();
		final List<Consumer<BufferedImage>> questCallbacks = new ArrayList<>();
		final IconSource recording = new IconSource()
		{
			@Override
			public void iconFor(HoverCard card, Consumer<BufferedImage> onIcon)
			{
			}

			@Override
			public void sprite(int spriteId, Consumer<BufferedImage> onIcon)
			{
				asks.add("sprite " + spriteId);
				if (spriteId == SkillSprites.QUEST)
				{
					questCallbacks.add(onIcon);
				}
			}

			@Override
			public void item(int itemId, int quantity, Consumer<BufferedImage> onIcon)
			{
				asks.add("item " + itemId + " x" + quantity);
			}
		};
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel(recording);
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			assertEquals("four pictures asked for", 4, asks.size());
			// Re-aimed by the lead, 2026-10-25 (lot 10F): the first row's icon is the Collection log at 1, not Coins.
			// Re-aimed by the lead, 2026-10-25: the Casket in its place, the user's pick.
			assertEquals("the Casket at 1, once", 1, Collections.frequency(asks, "item 405 x1"));
			assertEquals("and no Coins", 0, Collections.frequency(asks, "item 995 x10000"));
			assertEquals("the quest sprite, once", 1, Collections.frequency(asks, "sprite " + SkillSprites.QUEST));
			assertEquals("the total level sprite, once", 1,
				Collections.frequency(asks, "sprite " + SkillSprites.spriteOf("Total")));
			assertEquals("the combat sprite, once", 1,
				Collections.frequency(asks, "sprite " + SkillSprites.spriteOf("Combat")));
			assertEquals("one callback for the quest", 1, questCallbacks.size());

			onEdt(() ->
			{
				final List<JPanel> howToRows = rowsOfTheHowTo(panel[0]);
				assertEquals("five rows", 5, howToRows.size());
				final JPanel questRow = (JPanel) labelSaying(panel[0], "Quests, tasks, diaries").getParent();
				assertSame("the quest row is the second", howToRows.get(1), questRow);
				assertNull("its icon box is empty until the picture comes", iconBoxOf(questRow).getIcon());
				assertNull("so are the coins'", iconBoxOf(howToRows.get(0)).getIcon());
				assertNotNull("the examine glass is drawn at once", iconBoxOf(howToRows.get(4)).getIcon());
				assertEquals(20, iconBoxOf(howToRows.get(4)).getIcon().getIconWidth());
			});

			// A picture comes from the client thread, not the event thread, and is bigger than the box.
			questCallbacks.get(0).accept(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB));
			flush();
			onEdt(() ->
			{
				final JPanel questRow = (JPanel) labelSaying(panel[0], "Quests, tasks, diaries").getParent();
				final Icon icon = iconBoxOf(questRow).getIcon();
				assertNotNull("the picture is the quest row's icon", icon);
				assertEquals("scaled down to the box", 20, icon.getIconWidth());
				assertEquals(20, icon.getIconHeight());
				assertNull("no other row took it", iconBoxOf(rowsOfTheHowTo(panel[0]).get(0)).getIcon());
			});

			// A smaller picture is never scaled up.
			questCallbacks.get(0).accept(new BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB));
			flush();
			onEdt(() ->
			{
				final JPanel questRow = (JPanel) labelSaying(panel[0], "Quests, tasks, diaries").getParent();
				assertEquals("kept as it is", 10, iconBoxOf(questRow).getIcon().getIconWidth());
				assertEquals(8, iconBoxOf(questRow).getIcon().getIconHeight());
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/** Lot 10C: a row per line, newest first, each with its title and its line; the count follows; Clear empties it all. */
	@Test
	public void theRowsFollowTheHistory() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String whipLine = "Klar received a drop: Abyssal whip (1,500,000 coins).";
			final String herbLine = "Klar has reached Herblore level 78.";
			history.add(whipLine, Collections.<HoverCard>singletonList(card("Abyssal whip", "Abyssal_whip", whipLine)), 1000L);
			history.add(herbLine, Collections.<HoverCard>singletonList(card("Herblore 78", "Herblore", herbLine)), 2000L);
			flush();
			onEdt(() ->
			{
				frame[0].validate();
				// Re-aimed by the lead, 2026-10-25 (lot 10F): the rows are the cells of the history's list, read through its renderer.
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("two entries in the model", 2, list.getModel().getSize());
				assertEquals("the newest first", "Herblore 78", list.getModel().getElementAt(0).card().title());
				final List<String> first = textsIn(cellOf(list, 0));
				// Re-aimed by the lead, 2026-10-25 (lot 10J): a row's second line says who, what and when, not the whole line.
				assertTrue("the newest first, by its title", first.contains("Herblore 78"));
				assertTrue("and its who", first.contains("Klar"));
				assertFalse("not the whole line", first.contains(herbLine));
				final List<String> second = textsIn(cellOf(list, 1));
				assertTrue("the older second, by its title", second.contains("Abyssal whip"));
				assertTrue("and its who", second.contains("Klar"));
				assertFalse("not the whole line", second.contains(whipLine));
				// Re-aimed by the lead, 2026-10-25 (lot 10F): the count is the session's, no cap in it.
				assertTrue("the count follows", textsIn(panel[0]).contains("2 this session"));
			});

			onEdt(() -> leftPress(labelSaying(panel[0], "Clear")));
			flush();
			onEdt(() ->
			{
				assertEquals("Clear empties the history", 0, history.size());
				assertEquals("and the list's model", 0, historyListIn(panel[0]).getModel().getSize());
				assertTrue("and the count", textsIn(panel[0]).contains("0 this session"));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/** Lot 10C: the mouse entering a row asks the viewer for its card, once; leaving it asks for the card to come down, once. */
	@Test
	public void hoveringARowShowsItsCard() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String whipLine = "Klar received a drop: Abyssal whip (1,500,000 coins).";
			final String herbLine = "Klar has reached Herblore level 78.";
			final HoverCard herb = card("Herblore 78", "Herblore", herbLine);
			final HoverCard whip = card("Abyssal whip", "Abyssal_whip", whipLine);
			history.add(whipLine, Collections.<HoverCard>singletonList(whip), 1000L);
			history.add(herbLine, Collections.<HoverCard>singletonList(herb), 2000L);
			flush();
			onEdt(() ->
			{
				frame[0].validate();
				// Re-aimed by the lead, 2026-10-25 (lot 10F): the mouse is read on the list, cell by cell.
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("nothing was asked before the mouse came", 0, viewerShows.size() + viewerHides);

				final Point inFirst = middleOf(list, 0);
				mouse(list, MouseEvent.MOUSE_MOVED, inFirst);
				assertEquals("the viewer was asked once to show", 1, viewerShows.size());
				assertSame("for the first cell's card", herb, viewerShows.get(0)[0]);
				final Rectangle cell = list.getCellBounds(0, 0);
				cell.translate(list.getLocationOnScreen().x, list.getLocationOnScreen().y);
				assertEquals("level with the cell on the screen", cell, viewerShows.get(0)[1]);
				assertEquals("and asked to hide none", 0, viewerHides);
				assertEquals("the hovered cell draws hot", ColorScheme.DARK_GRAY_HOVER_COLOR, rowOf(list, 0).getBackground());
				assertEquals("and the other does not", ColorScheme.DARK_GRAY_COLOR, rowOf(list, 1).getBackground());

				mouse(list, MouseEvent.MOUSE_MOVED, new Point(inFirst.x + 1, inFirst.y));
				assertEquals("a move inside the same cell asks for nothing", 1, viewerShows.size());

				mouse(list, MouseEvent.MOUSE_MOVED, middleOf(list, 1));
				assertEquals("a move to the second cell shows its card", 2, viewerShows.size());
				assertSame("the older line's", whip, viewerShows.get(1)[0]);
				assertEquals("the second cell draws hot", ColorScheme.DARK_GRAY_HOVER_COLOR, rowOf(list, 1).getBackground());
				assertEquals("and the first does not", ColorScheme.DARK_GRAY_COLOR, rowOf(list, 0).getBackground());

				mouse(list, MouseEvent.MOUSE_EXITED, middleOf(list, 1));
				assertEquals("leaving asks to hide, once", 1, viewerHides);
				assertEquals("and shows nothing more", 2, viewerShows.size());
				assertEquals("and no cell draws hot", ColorScheme.DARK_GRAY_COLOR, rowOf(list, 1).getBackground());
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/** Lot 10C: a right-click on a row offers "Open wiki" and "Copy", which hand over the card's address and the line. */
	@Test
	public void aRightClickOnARowOffersWikiAndCopy() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String herbLine = "Klar has reached Herblore level 78.";
			final HoverCard herb = card("Herblore 78", "Herblore", herbLine);
			history.add(herbLine, Collections.<HoverCard>singletonList(herb), 2000L);
			flush();
			onEdt(() ->
			{
				frame[0].validate();
				// Re-aimed by the lead, 2026-10-25 (lot 10F): the press lands on the first cell of the history's list.
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				final Point inFirst = middleOf(list, 0);
				list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
					MouseEvent.BUTTON3_DOWN_MASK, inFirst.x, inFirst.y, 1, true, MouseEvent.BUTTON3));
				final JPopupMenu menu = popupMenu(frame[0]);
				assertNotNull("the row's menu is on screen", menu);
				final List<JMenuItem> items = new ArrayList<>();
				for (final Component part : menu.getComponents())
				{
					if (part instanceof JMenuItem)
					{
						items.add((JMenuItem) part);
					}
				}
				assertEquals("two entries", 2, items.size());
				assertEquals("Open wiki", items.get(0).getText());
				assertEquals("Copy", items.get(1).getText());

				items.get(0).doClick(0);
				assertEquals("'Open wiki' hands over the card's address", List.of(herb.wikiUrl()), browsed);
				assertTrue("and copies nothing", copied.isEmpty());
				items.get(1).doClick(0);
				assertEquals("'Copy' hands over the card's line", List.of(herb.plainLine()), copied);
				assertEquals("and opens nothing more", 1, browsed.size());
				menu.setVisible(false);
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 10F: a card is one insert at the top of the list's model, the entries below it the same objects in the same order;
	 * a burst of lines is one sync; the whole session is kept, to the history's cap of five thousand, and the count says how
	 * many; a history cleared and filled again before a sync leaves only the new entries.
	 */
	@Test
	public void theListGrowsAtTheTopAndKeepsTheWholeSession() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			for (int i = 1; i <= 3; i++)
			{
				final String line = "line " + i;
				history.add(line, Collections.<HoverCard>singletonList(card("Card " + i, "Page", line)), i);
			}
			flush();
			final List<CardHistory.Entry> three = history.entries();
			onEdt(() ->
			{
				final List<CardHistory.Entry> model = modelOf(historyListIn(panel[0]));
				assertEquals("three entries", 3, model.size());
				assertEquals("the newest first", "line 3", model.get(0).line());
				assertEquals("the oldest last", "line 1", model.get(2).line());
				for (int i = 0; i < 3; i++)
				{
					assertSame("the model holds the history's own entry " + i, three.get(i), model.get(i));
				}
			});

			history.add("line 4", Collections.<HoverCard>singletonList(card("Card 4", "Page", "line 4")), 4);
			flush();
			onEdt(() ->
			{
				final List<CardHistory.Entry> model = modelOf(historyListIn(panel[0]));
				assertEquals("four entries", 4, model.size());
				assertEquals("the new one is at the top", "line 4", model.get(0).line());
				for (int i = 0; i < 3; i++)
				{
					assertSame("the earlier ones are the same objects, in the same order, below it", three.get(i),
						model.get(i + 1));
				}
			});

			// Re-aimed by the lead, 2026-10-25 (lot 10F): the whole session, up to 5,000, the user's word.
			for (int i = 5; i <= 5005; i++)
			{
				final String line = "line " + i;
				history.add(line, Collections.<HoverCard>singletonList(card("Card " + i, "Page", line)), i);
			}
			flush();
			final List<CardHistory.Entry> kept = history.entries();
			onEdt(() ->
			{
				final List<CardHistory.Entry> model = modelOf(historyListIn(panel[0]));
				assertEquals("five thousand in the model", 5000, model.size());
				assertEquals("the first is the newest", "line 5005", model.get(0).line());
				assertEquals("the last is the 5000th newest", "line 6", model.get(4999).line());
				assertSame("the first is the history's own", kept.get(0), model.get(0));
				assertSame("and so is the last", kept.get(4999), model.get(4999));
				assertTrue("the count says so", textsIn(panel[0]).contains("5000 this session"));
			});

			history.clear();
			history.add("fresh", Collections.<HoverCard>singletonList(card("Fresh", "Page", "fresh")), 9);
			flush();
			onEdt(() ->
			{
				final List<CardHistory.Entry> model = modelOf(historyListIn(panel[0]));
				assertEquals("a history cleared and filled again before a sync leaves the new entry only", 1, model.size());
				assertEquals("fresh", model.get(0).line());
				assertTrue("and the count", textsIn(panel[0]).contains("1 this session"));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 10S: a sidebar at Stone with "Same as cards" is dressed in Stone: the ground, the bar's title, a row's title and
	 * kind, and the top card's frame (a black pixel in its corner).
	 */
	@Test
	public void aStoneSidebarIsDressedInStone() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final String line = "GMucker has completed a medium combat task: Smite Fight.";
			history.add(line, Collections.<HoverCard>singletonList(card("Smite Fight", "Smite_Fight", line)),
				System.currentTimeMillis());
			flush();
			onEdt(() ->
			{
				assertEquals("Classic's ground at first", new Color(30, 30, 30), panel[0].getBackground());
				cardTheme = CardTheme.STONE;
				// Re-aimed by the lead, 2026-10-26 (lot 10W): a Sidebar theme choice in place of the switch.
				sidebarLook = SidebarTheme.SAME;
				panel[0].settingsChanged();
				frame[0].validate();

				assertEquals("the panel's ground is Stone's", new Color(36, 31, 25), panel[0].getBackground());
				assertEquals("the bar's title is Stone's orange", new Color(255, 152, 31),
					labelSaying(panel[0], "How to use").getForeground());
				assertEquals("the action line is white", Color.WHITE,
					labelSaying(panel[0], "Hover the coloured name for its card.").getForeground());
				assertEquals("the heading is Stone's", new Color(175, 160, 128),
					labelSaying(panel[0], "WHAT IT WORKS ON").getForeground());
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				assertEquals("one entry", 1, list.getModel().getSize());
				assertEquals("the list's ground is Stone's", new Color(36, 31, 25), list.getBackground());
				final Container cell = cellOf(list, 0);
				assertEquals("a row's title is white", Color.WHITE, labelSaying(cell, "Smite Fight").getForeground());
				assertEquals("its kind is Stone's grey", new Color(205, 188, 150),
					labelSaying(cell, "medium task").getForeground());
				assertEquals("its name stays in the name colour", new Color(220, 138, 0),
					labelSaying(cell, "GMucker").getForeground());

				final Container top = (Container) panel[0].getComponent(0);
				assertTrue("the top card has a size", top.getWidth() > 0 && top.getHeight() > 0);
				final BufferedImage painted = new BufferedImage(top.getWidth(), top.getHeight(), BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g = painted.createGraphics();
				try
				{
					top.paint(g);
				}
				finally
				{
					g.dispose();
				}
				assertEquals("the top card is framed in Stone: black in its corner", Color.BLACK, new Color(painted.getRGB(0, 0)));
				assertEquals("and a rivet is in its corner", new Color(150, 132, 104), new Color(painted.getRGB(3, 3)));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 10S: with "RuneLite" chosen a sidebar at Stone looks as a sidebar at Classic does: the same ground, the same
	 * "How to use", the same row colours, and the plain flat top card.
	 */
	@Test
	public void theSwitchOffKeepsTodaysSidebar() throws Exception
	{
		final String line = "GMucker has completed a medium combat task: Smite Fight.";
		history.add(line, Collections.<HoverCard>singletonList(card("Smite Fight", "Smite_Fight", line)),
			System.currentTimeMillis());
		flush();
		final ClanHubPanel[] panel = new ClanHubPanel[2];
		final JFrame[] frame = new JFrame[2];
		onEdt(() ->
		{
			cardTheme = CardTheme.STONE;
			// Re-aimed by the lead, 2026-10-26 (lot 10W): a Sidebar theme choice in place of the switch.
			sidebarLook = SidebarTheme.RUNELITE;
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
			cardTheme = CardTheme.CLASSIC;
			sidebarLook = SidebarTheme.SAME;
			panel[1] = newPanel();
			frame[1] = frameOf(panel[1]);
		});
		try
		{
			onEdt(() ->
			{
				assertEquals("the same ground", panel[1].getBackground(), panel[0].getBackground());
				assertEquals("today's ground", new Color(30, 30, 30), panel[0].getBackground());
				assertEquals("the same 'How to use'", labelSaying(panel[1], "How to use").getForeground(),
					labelSaying(panel[0], "How to use").getForeground());
				assertEquals("white", Color.WHITE, labelSaying(panel[0], "How to use").getForeground());
				final Container off = cellOf(historyListIn(panel[0]), 0);
				final Container classic = cellOf(historyListIn(panel[1]), 0);
				for (final String words : new String[] {"Smite Fight", "medium task", "GMucker", "now"})
				{
					assertEquals("the same colour for '" + words + "'", labelSaying(classic, words).getForeground(),
						labelSaying(off, words).getForeground());
				}
				assertEquals("a row's kind is today's grey", ColorScheme.LIGHT_GRAY_COLOR,
					labelSaying(off, "medium task").getForeground());
				assertEquals("a row's time is today's", new Color(130, 130, 130), labelSaying(off, "now").getForeground());

				final Container top = (Container) panel[0].getComponent(0);
				final BufferedImage painted = new BufferedImage(top.getWidth(), top.getHeight(), BufferedImage.TYPE_INT_ARGB);
				final Graphics2D g = painted.createGraphics();
				try
				{
					top.paint(g);
				}
				finally
				{
					g.dispose();
				}
				assertEquals("the top card is flat, as it always was", ColorScheme.DARK_GRAY_COLOR, new Color(painted.getRGB(0, 0)));
			});
		}
		finally
		{
			onEdt(() ->
			{
				frame[0].dispose();
				frame[1].dispose();
			});
		}
	}

	/**
	 * Lot 10S: a theme set while the sidebar shows is worn at once (a change told by {@code settingsChanged}), and the
	 * menu's Sidebar theme submenu, at "RuneLite", takes the sidebar back to the plain look and writes the choice.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void aThemeChangeDressesTheSidebarAtOnce() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				assertEquals("Classic's ground", new Color(30, 30, 30), panel.getBackground());
				assertEquals("Classic's 'How to use'", Color.WHITE, labelSaying(panel, "How to use").getForeground());

				cardTheme = CardTheme.VOID;
				panel.settingsChanged();
				assertEquals("Void's ground", new Color(12, 6, 26), panel.getBackground());
				assertEquals("Void's 'How to use'", new Color(228, 194, 255), labelSaying(panel, "How to use").getForeground());

				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				final ValueMenu sidebarMenu = valueMenuSaying(menu, "Sidebar theme");
				assertEquals("Same as cards", sidebarMenu.value());
				assertTrue("Same as cards is the selected one", radio(sidebarMenu, 0).isSelected());
				assertEquals("RuneLite", radio(sidebarMenu, 1).getText());
				radio(sidebarMenu, 1).doClick(0);
				assertEquals("the choice is written, RuneLite", List.of(SidebarTheme.RUNELITE), sidebarLooks);
				assertEquals("and the stub holds it", SidebarTheme.RUNELITE, sidebarLook);
				assertEquals("the ground is Classic's again", new Color(30, 30, 30), panel.getBackground());
				assertEquals("and 'How to use' is white", Color.WHITE, labelSaying(panel, "How to use").getForeground());

				radio(sidebarMenu, 0).doClick(0);
				assertEquals("Same as cards again", List.of(SidebarTheme.RUNELITE, SidebarTheme.SAME), sidebarLooks);
				assertEquals("Void's ground is back", new Color(12, 6, 26), panel.getBackground());
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10W: the sidebar can wear a theme of its own: with the cards at Classic and the sidebar's look at Garden, the
	 * panel's ground is Garden's page and the cards' theme is untouched.
	 */
	@Test
	public void aSidebarThemeOfItsOwn() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				assertEquals("Classic's ground at first", new Color(30, 30, 30), panel.getBackground());
				cardTheme = CardTheme.CLASSIC;
				sidebarLook = SidebarTheme.GARDEN;
				panel.settingsChanged();
				assertEquals("Garden's page", new Color(22, 40, 24), panel.getBackground());
				assertEquals("the cards are still Classic", CardTheme.CLASSIC, cardTheme);
				assertTrue("and no card theme was written", themes.isEmpty());
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 10W: while an item of the Card theme or the Sidebar theme submenu is armed a picture of it is handed to the
	 * viewer, level with the item on the screen: a sample card in the theme, or a small sidebar in the look; arming
	 * another gives another; closing the menu takes the picture down.
	 */
	// Re-aimed for lot 11A, 2026-10-26: the menu in groups.
	@Test
	public void hoveringAThemeShowsItsPreview() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				final ValueMenu themeMenu = valueMenuSaying(menu, "Card theme");
				final ValueMenu sidebarMenu = valueMenuSaying(menu, "Sidebar theme");
				assertEquals("Classic", themeMenu.value());
				assertEquals("Same as cards", sidebarMenu.value());
				assertTrue("no picture before an item is armed", viewerPictures.isEmpty());

				final JRadioButtonMenuItem synthwave = radio(themeMenu, CardTheme.SYNTHWAVE.ordinal());
				assertEquals("Synthwave", synthwave.getText());
				arm(menu, themeMenu, synthwave);
				assertEquals("arming Synthwave shows one picture", 1, viewerPictures.size());
				final BufferedImage first = (BufferedImage) viewerPictures.get(0)[0];
				assertEquals("a Synthwave card: its edge in the corner", new Color(255, 60, 180), new Color(first.getRGB(0, 0)));
				assertEquals("level with the item on the screen", new Rectangle(synthwave.getLocationOnScreen(), synthwave.getSize()),
					viewerPictures.get(0)[1]);

				final JRadioButtonMenuItem forest = radio(themeMenu, CardTheme.FOREST.ordinal());
				assertEquals("Forest", forest.getText());
				arm(menu, themeMenu, forest);
				assertEquals("arming Forest shows a second", 2, viewerPictures.size());
				final BufferedImage second = (BufferedImage) viewerPictures.get(1)[0];
				assertEquals("a Forest card: its edge in the corner", new Color(150, 110, 60), new Color(second.getRGB(0, 0)));
				assertEquals("level with Forest's item", new Rectangle(forest.getLocationOnScreen(), forest.getSize()),
					viewerPictures.get(1)[1]);

				final JRadioButtonMenuItem garden = radio(sidebarMenu, 3 + CardTheme.GARDEN.ordinal());
				assertEquals("Garden", garden.getText());
				arm(menu, sidebarMenu, garden);
				assertEquals("arming Garden in the sidebar list shows a third", 3, viewerPictures.size());
				final BufferedImage third = (BufferedImage) viewerPictures.get(2)[0];
				assertEquals("a Garden sidebar: its page", new Color(22, 40, 24), new Color(third.getRGB(2, 2)));

				final int hidesBefore = viewerHides;
				menu.setVisible(false);
				assertEquals("closing the menu takes the picture down", hidesBefore + 1, viewerHides);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * The how-to ends with the theme tip, under the examine row: one grey line, "Tip:" in bold, no icon, on one line;
	 * the five rows stay five, and the only other line that speaks of themes is the empty list's tip.
	 */
	// Re-aimed for lot 12B, 2026-10-27: the empty list shows the tip too (the user's word).
	@Test
	public void theHowToEndsWithTheThemeTip() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		onEdt(() ->
		{
			cardTheme = CardTheme.STONE;
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			onEdt(() ->
			{
				final JLabel text = howToTipIn(panel[0]);
				assertTrue("the tip shows", text.isShowing());
				assertNull("no icon", text.getIcon());
				final Component tip = text.getParent();
				final List<JPanel> rows = rowsOfTheHowTo(panel[0]);
				assertEquals("the rows stay five", 5, rows.size());
				final JPanel examine = rows.get(4);
				assertSame("in the rows' box", examine.getParent(), tip.getParent());
				final List<Component> lines = Arrays.asList(tip.getParent().getComponents());
				assertTrue("under the examine row", lines.indexOf(tip) > lines.indexOf(examine));
				assertEquals("as wide as the wiki line's box", labelSaying(panel[0], ClanHubPanel.HOW_TO_WIKI).getParent()
					.getWidth(), tip.getWidth());
				assertEquals("in Stone's grey", labelSaying(panel[0], ClanHubPanel.HOW_TO_WIKI).getForeground(),
					text.getForeground());
				assertTrue("'Tip:' in bold, in the line's own grey", text.getText().contains("<b>Tip:</b>"));
				assertFalse("on one line", text.getText().contains("<br>"));
				final List<JLabel> others = new ArrayList<>();
				for (final JLabel label : labelsIn(panel[0]))
				{
					if (label != text && plain(label.getText()).contains("theme"))
					{
						others.add(label);
					}
				}
				assertEquals("one other line speaks of themes", 1, others.size());
				assertSame("and it is the empty list's tip", emptyListTipIn(panel[0]), others.get(0));
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 12B: while the recent cards are empty their place shows a tip instead, under the header, in the middle: "Tip:
	 * Pick a theme in the settings menu.", "Tip:" in bold as in the how-to's, on one line that fits the sidebar,
	 * in the how-to tip's grey, which a new theme changes with it; the first card takes it away and Clear brings it back.
	 */
	// The words changed by the user during the build, 2026-10-27: "Tip:" before ad38f45's "Pick a theme in the settings menu."
	@Test
	public void theEmptyListShowsTheTip() throws Exception
	{
		final ClanHubPanel[] panel = new ClanHubPanel[1];
		final JFrame[] frame = new JFrame[1];
		final JLabel[] tip = new JLabel[1];
		onEdt(() ->
		{
			cardTheme = CardTheme.STONE;
			panel[0] = newPanel();
			frame[0] = frameOf(panel[0]);
		});
		try
		{
			final Color[] stoneGrey = new Color[1];
			onEdt(() ->
			{
				tip[0] = emptyListTipIn(panel[0]);
				final JLabel howToTip = howToTipIn(panel[0]);
				final JList<CardHistory.Entry> list = historyListIn(panel[0]);
				final JLabel header = labelSaying(panel[0], ClanHubPanel.RECENT_TEXT);
				assertTrue("the tip shows while the list is empty", tip[0].isShowing());
				// Re-aimed by the lead, 2026-10-27: the user picked option B of docs/empty-tip-options-2.png, the middle of the list.
				final JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
				assertEquals("centred across", SwingConstants.CENTER, tip[0].getHorizontalAlignment());
				assertEquals("and down", SwingConstants.CENTER, tip[0].getVerticalAlignment());
				assertEquals("in the list's place", scroll.getBounds(), tip[0].getBounds());
				assertTrue("which is tall", tip[0].getHeight() > 100);
				assertFalse("the empty list is not shown", scroll.isShowing());
				assertTrue("below the recent cards' header", yIn(panel[0], header) + header.getHeight() <= yIn(panel[0], tip[0]));
				assertTrue("'Tip:' in bold", tip[0].getText().contains("<b>Tip:</b>"));
				assertFalse("on one line", tip[0].getText().contains("<br>"));
				// The user's words are 212 px, wider than the 209 px of ad38f45's shorter line: they are held to the list's room.
				assertTrue("in the 222 px the list leaves it", tip[0].getPreferredSize().width <= 222);
				assertEquals("in the how-to tip's grey", howToTip.getForeground(), tip[0].getForeground());
				assertEquals("which is Stone's", SidebarSkin.of(CardTheme.STONE).grey, tip[0].getForeground());
				assertTrue("the how-to keeps its own tip", howToTip.isShowing());
				stoneGrey[0] = tip[0].getForeground();
			});
			final String line = "GMucker has completed a medium combat task: Smite Fight.";
			history.add(line, Collections.<HoverCard>singletonList(card("Smite Fight", "Smite_Fight", line)),
				System.currentTimeMillis());
			flush();
			onEdt(() ->
			{
				assertFalse("the first card takes it away", tip[0].isVisible());
				assertTrue("and the list shows", historyListIn(panel[0]).isShowing());
				assertTrue("the how-to keeps its own tip", howToTipIn(panel[0]).isShowing());
			});
			history.clear();
			flush();
			onEdt(() -> assertTrue("Clear brings it back", tip[0].isShowing()));
			onEdt(() ->
			{
				final Color nebulaGrey = SidebarSkin.of(CardTheme.NEBULA).grey;
				cardTheme = CardTheme.NEBULA;
				panel[0].settingsChanged();
				assertEquals("a new theme: still the how-to tip's grey", howToTipIn(panel[0]).getForeground(),
					tip[0].getForeground());
				assertEquals("which is Nebula's", nebulaGrey, tip[0].getForeground());
				if (!nebulaGrey.equals(stoneGrey[0]))
				{
					assertNotEquals("and no longer Stone's", stoneGrey[0], tip[0].getForeground());
				}
			});
		}
		finally
		{
			onEdt(() -> frame[0].dispose());
		}
	}

	/**
	 * Lot 11A: the Card group holds the width, the place and the time, then the text size; a click on a small button or a
	 * pick of an item writes it, and the group and the menu stay up.
	 */
	@Test
	public void theCardGroupHoldsWidthPlaceTimeAndSize() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				final JPopupMenu card = open(menu, groupOf(menu, "Card"));

				assertEquals("the group's parts in order", List.of("WIDTH", "Wide|Medium|Narrow", "PLACE", "Above the cursor",
					"Above the chat", "TIME", "4m ago|14:32|2:32 PM", "Text size"), wordsIn(card));
				assertTogglesIn(card, 0, CardWidth.values(), CardWidth.WIDE);
				assertRadiosIn(card, CardPlace.values(), CardPlace.CURSOR);
				assertTogglesIn(card, 1, CardTime.values(), CardTime.AGO);
				assertTrue("the text size row ends in a spinner", lastOf(rowSaying(card, "Text size")) instanceof JSpinner);

				final JToggleButton narrow = toggleSaying(card, "Narrow");
				narrow.doClick(0);
				assertEquals("clicking 'Narrow' writes it", List.of(CardWidth.NARROW), widths);
				assertTrue("and selects it", narrow.isSelected());
				assertFalse("and not Wide", toggleSaying(card, "Wide").isSelected());

				final JRadioButtonMenuItem chat = radioSaying(card, "Above the chat");
				click(chat);
				assertEquals("picking 'Above the chat' writes it", List.of(CardPlace.CHAT), places);
				assertTrue("and selects it", chat.isSelected());

				final JToggleButton clock = toggleSaying(card, "14:32");
				clock.doClick(0);
				assertEquals("clicking '14:32' writes it", List.of(CardTime.CLOCK_24), cardTimes);
				assertTrue("and selects it", clock.isSelected());

				assertTrue("the menu is still showing", menu.isVisible());
				assertTrue("and the group", card.isVisible());
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 11A: the "When it shows" group holds the show mode, the key and the delay; the key box is greyed unless the mode is
	 * "While holding a key"; a pick keeps the group and the menu up.
	 */
	@Test
	public void theWhenGroupHoldsShowKeyAndDelay() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				final JPopupMenu when = open(menu, groupOf(menu, "When it shows"));

				assertEquals("the group's parts in order", List.of("SHOW", "On hover", "While holding a key", "On click", "Key",
					"Delay (ms)"), wordsIn(when));
				assertRadiosIn(when, CardTrigger.values(), CardTrigger.HOVER);
				final JButton keyBox = (JButton) lastOf(rowSaying(when, "Key"));
				assertFalse("the key box is greyed at 'On hover'", keyBox.isEnabled());
				assertTrue("the delay row ends in a spinner", lastOf(rowSaying(when, "Delay (ms)")) instanceof JSpinner);

				click(radioSaying(when, "While holding a key"));
				assertEquals("picking 'While holding a key' writes it", List.of(CardTrigger.HOLD_KEY), triggers);
				assertTrue("and the key box is enabled", keyBox.isEnabled());
				assertTrue("the menu is still showing", menu.isVisible());
				assertTrue("and the group", when.isVisible());

				click(radioSaying(when, "On click"));
				assertEquals("picking 'On click' writes it", List.of(CardTrigger.HOLD_KEY, CardTrigger.CLICK), triggers);
				assertFalse("and the key box is greyed again", keyBox.isEnabled());
				assertTrue("the menu is still showing", menu.isVisible());
				assertTrue("and the group", when.isVisible());
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 11A: the "What gets a card" group holds the four switches and the examine line's choice; a click keeps the group and
	 * the menu up.
	 */
	@Test
	public void theWhatGroupHoldsFourSwitchesAndTheExamineLine() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				final JPopupMenu what = open(menu, groupOf(menu, "What gets a card"));

				assertEquals("the group's parts in order", List.of("Boss cards", "Examine cards", "Wiki text", "Wiki pictures",
					"EXAMINE LINE", "Item name first", "Description only"), wordsIn(what));
				for (int i = 0; i < 4; i++)
				{
					assertTrue("a switch, ticked", what.getComponent(i) instanceof JCheckBoxMenuItem
						&& ((JCheckBoxMenuItem) what.getComponent(i)).isSelected());
				}
				assertRadiosIn(what, ExamineLine.values(), ExamineLine.NAME_FIRST);

				click(checkSaying(what, "Boss cards"));
				assertEquals("clicking 'Boss cards' writes false", List.of(false), bosses);
				assertTrue("the menu is still showing", menu.isVisible());
				assertTrue("and the group", what.isVisible());

				click(radioSaying(what, "Description only"));
				assertEquals("picking 'Description only' writes it", List.of(ExamineLine.DESCRIPTION), examineLines);
				assertTrue("and selects it", radioSaying(what, "Description only").isSelected());
				assertTrue("the menu is still showing", menu.isVisible());
				assertTrue("and the group", what.isVisible());
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 11A: the "Colours" group holds the colouring switch, the hover colour, the underline switch and colour and the
	 * sidebar's names; a press on the names' swatch takes the menu down for the picker (titled "Name colour"), and when the
	 * picker closes the menu is back with the Colours group open.
	 */
	@Test
	public void theColoursGroupBringsItselfBackAfterThePicker() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);
				final JMenu group = groupOf(menu, "Colours");
				final JPopupMenu colours = open(menu, group);

				assertEquals("the group's parts in order", List.of("Colour names", "Hover colour", "Underline level-ups",
					"Underline colour", "Sidebar names"), wordsIn(colours));
				assertTrue("the first part is a switch", colours.getComponent(0) instanceof JCheckBoxMenuItem);
				assertTrue("the third part is a switch", colours.getComponent(2) instanceof JCheckBoxMenuItem);

				final JButton names = (JButton) lastOf(rowSaying(colours, "Sidebar names"));
				assertEquals(new Color(220, 138, 0), names.getBackground());
				names.doClick(0);
				assertFalse("the menu goes while the picker is open", menu.isVisible());
				assertEquals("the picker was asked once", 1, pickerCalls.size());
				assertEquals("its title is still 'Name colour'", "Name colour", pickerCalls.get(0)[1]);

				pickerOnClose.accept(new Color(220, 138, 0));
				assertTrue("the menu comes back when the picker closes", menu.isVisible());
				assertTrue("with the Colours group showing", group.getPopupMenu().isVisible());
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 11A: a theme row shows its theme in grey at the right and keeps its caption as its text; a pick changes the value
	 * and never the row's text or width, and the value is drawn at the row's right side.
	 */
	@Test
	public void aThemeRowShowsItsValueAtTheRight() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);

				final ValueMenu theme = valueMenuSaying(menu, "Card theme");
				assertEquals("Card theme", theme.getText());
				assertEquals("Classic", theme.value());
				final int themeWidth = theme.getPreferredSize().width;
				MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu, theme, theme.getPopupMenu()});
				click(radio(theme, CardTheme.STONE.ordinal()));
				assertEquals("a pick makes the value 'Stone'", "Stone", theme.value());
				assertEquals("and keeps the text", "Card theme", theme.getText());
				assertEquals("and the width", themeWidth, theme.getPreferredSize().width);
				assertEquals("and writes the pick", List.of(CardTheme.STONE), themes);

				final ValueMenu sidebar = valueMenuSaying(menu, "Sidebar theme");
				assertEquals("Sidebar theme", sidebar.getText());
				assertEquals("Same as cards", sidebar.value());
				final int sidebarWidth = sidebar.getPreferredSize().width;
				MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu, sidebar, sidebar.getPopupMenu()});
				click(radio(sidebar, SidebarTheme.RUNELITE.ordinal()));
				assertEquals("a pick makes the value 'RuneLite'", "RuneLite", sidebar.value());
				assertEquals("and keeps the text", "Sidebar theme", sidebar.getText());
				assertEquals("and the width", sidebarWidth, sidebar.getPreferredSize().width);

				// The value is drawn: a row with a value differs from the same row without one, on its right half only.
				theme.setValue("Classic");
				final BufferedImage again = paintOf(theme);
				theme.setValue("");
				final BufferedImage without = paintOf(theme);
				theme.setValue("Stone");
				int leftDiffs = 0;
				int rightDiffs = 0;
				for (int x = 0; x < again.getWidth(); x++)
				{
					for (int y = 0; y < again.getHeight(); y++)
					{
						if (again.getRGB(x, y) != without.getRGB(x, y))
						{
							if (x < again.getWidth() / 2)
							{
								leftDiffs++;
							}
							else
							{
								rightDiffs++;
							}
						}
					}
				}
				assertEquals("the caption's side is the same", 0, leftDiffs);
				assertTrue("the value is drawn on the right side", rightDiffs > 0);
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/**
	 * Lot 11A: the groups that hold a typing box open with a focusable first part (a heading), so that the group's window takes
	 * the keyboard and a press can move it to a box; no text field, spinner box, key box or small button in any group is
	 * focusable at rest.
	 */
	@Test
	public void theGroupsTakeTheKeyboardOnlyWhenPressed() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = newPanel();
			final JFrame frame = frameOf(panel);
			try
			{
				press(settingsIcon(panel));
				final JPopupMenu menu = popupMenu(frame);
				assertNotNull("the menu is on screen", menu);

				final JPopupMenu card = groupOf(menu, "Card").getPopupMenu();
				final JPopupMenu when = groupOf(menu, "When it shows").getPopupMenu();
				assertTrue("the Card group's first part is focusable", card.getComponent(0).isFocusable());
				assertTrue("the When it shows group's first part is focusable", when.getComponent(0).isFocusable());
				assertEquals("it is the heading", "WIDTH", wordsOf(card.getComponent(0)));
				assertEquals("it is the heading", "SHOW", wordsOf(when.getComponent(0)));

				for (final String name : List.of("Card", "When it shows", "What gets a card", "Colours"))
				{
					final JPopupMenu popup = groupOf(menu, name).getPopupMenu();
					for (final JTextField field : textFieldsIn(popup))
					{
						assertFalse("a text field in '" + name + "' is not focusable at rest", field.isFocusable());
					}
					for (final JToggleButton toggle : togglesIn(popup))
					{
						assertFalse("a small button in '" + name + "' is not focusable", toggle.isFocusable());
					}
				}
				assertFalse("the key box is not focusable at rest", lastOf(rowSaying(when, "Key")).isFocusable());
				assertFalse("the delay box is not focusable at rest", ((JSpinner.DefaultEditor)
					((JSpinner) lastOf(rowSaying(when, "Delay (ms)"))).getEditor()).getTextField().isFocusable());
				assertFalse("the size box is not focusable at rest", ((JSpinner.DefaultEditor)
					((JSpinner) lastOf(rowSaying(card, "Text size"))).getEditor()).getTextField().isFocusable());
				menu.setVisible(false);
			}
			finally
			{
				frame.dispose();
			}
		});
	}

	/** The group of the menu whose text is {@code name}. */
	private static JMenu groupOf(JPopupMenu menu, String name)
	{
		for (final Component part : menu.getComponents())
		{
			if (part instanceof JMenu && !(part instanceof ValueMenu) && name.equals(((JMenu) part).getText()))
			{
				return (JMenu) part;
			}
		}
		throw new AssertionError("no group is called: " + name);
	}

	/**
	 * Opens {@code group} the way the mouse does: the selected path runs through the menu, the group and its popup. Gives
	 * the group's popup, which is up with the menu under it.
	 */
	private static JPopupMenu open(JPopupMenu menu, JMenu group)
	{
		MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu, group, group.getPopupMenu()});
		assertTrue("the group '" + group.getText() + "' is up", group.getPopupMenu().isVisible());
		assertTrue("the menu is still up under it", menu.isVisible());
		return group.getPopupMenu();
	}

	/** The theme row of the menu whose text is {@code text}. */
	private static ValueMenu valueMenuSaying(JPopupMenu menu, String text)
	{
		for (final Component part : menu.getComponents())
		{
			if (part instanceof ValueMenu && text.equals(((ValueMenu) part).getText()))
			{
				return (ValueMenu) part;
			}
		}
		throw new AssertionError("no theme row is called: " + text);
	}

	/**
	 * The words of a part of the menu or of a group: a check, an item, a submenu or a small button says its text; a row of
	 * small buttons says theirs, joined by bars; a heading or a row says the text of its first label.
	 */
	private static String wordsOf(Component part)
	{
		if (part instanceof AbstractButton)
		{
			return ((AbstractButton) part).getText();
		}
		if (part instanceof JPopupMenu.Separator)
		{
			return "-";
		}
		if (part instanceof Container)
		{
			final List<JToggleButton> toggles = togglesIn((Container) part);
			if (!toggles.isEmpty())
			{
				final StringBuilder joined = new StringBuilder();
				for (final JToggleButton toggle : toggles)
				{
					joined.append(joined.length() == 0 ? "" : "|").append(toggle.getText());
				}
				return joined.toString();
			}
			for (final JLabel label : labelsIn((Container) part))
			{
				return plain(label.getText());
			}
		}
		return "";
	}

	/** The words of every part of the popup, in order. */
	private static List<String> wordsIn(JPopupMenu popup)
	{
		final List<String> words = new ArrayList<>();
		for (final Component part : popup.getComponents())
		{
			words.add(wordsOf(part));
		}
		return words;
	}

	/** The check of the popup whose text is {@code words}. */
	private static JCheckBoxMenuItem checkSaying(JPopupMenu popup, String words)
	{
		for (final Component part : popup.getComponents())
		{
			if (part instanceof JCheckBoxMenuItem && words.equals(((JCheckBoxMenuItem) part).getText()))
			{
				return (JCheckBoxMenuItem) part;
			}
		}
		throw new AssertionError("no check says: " + words);
	}

	/** The radio item of the popup whose text is {@code words}. */
	private static JRadioButtonMenuItem radioSaying(JPopupMenu popup, String words)
	{
		for (final JRadioButtonMenuItem item : radiosIn(popup))
		{
			if (words.equals(item.getText()))
			{
				return item;
			}
		}
		throw new AssertionError("no radio item says: " + words);
	}

	/** The radio items of the popup, in order. */
	private static List<JRadioButtonMenuItem> radiosIn(JPopupMenu popup)
	{
		final List<JRadioButtonMenuItem> found = new ArrayList<>();
		for (final Component part : popup.getComponents())
		{
			if (part instanceof JRadioButtonMenuItem)
			{
				found.add((JRadioButtonMenuItem) part);
			}
		}
		return found;
	}

	/** The row of the popup whose caption (its first label) is {@code caption}. */
	private static JPanel rowSaying(JPopupMenu popup, String caption)
	{
		for (final Component part : popup.getComponents())
		{
			if (part instanceof JPanel && togglesIn((JPanel) part).isEmpty() && caption.equals(wordsOf(part)))
			{
				return (JPanel) part;
			}
		}
		throw new AssertionError("no row says: " + caption);
	}

	/** The small button of the popup whose text is {@code words}. */
	private static JToggleButton toggleSaying(JPopupMenu popup, String words)
	{
		for (final JToggleButton toggle : togglesIn(popup))
		{
			if (words.equals(toggle.getText()))
			{
				return toggle;
			}
		}
		throw new AssertionError("no small button says: " + words);
	}

	/** Every small button (toggle button) under {@code root}, in the tree's order. */
	private static List<JToggleButton> togglesIn(Container root)
	{
		final List<JToggleButton> found = new ArrayList<>();
		for (final Component child : root.getComponents())
		{
			if (child instanceof JToggleButton)
			{
				found.add((JToggleButton) child);
			}
			else if (child instanceof Container)
			{
				found.addAll(togglesIn((Container) child));
			}
		}
		return found;
	}

	/** Every text field under {@code root}, in the tree's order. */
	private static List<JTextField> textFieldsIn(Container root)
	{
		final List<JTextField> found = new ArrayList<>();
		for (final Component child : root.getComponents())
		{
			if (child instanceof JTextField)
			{
				found.add((JTextField) child);
			}
			if (child instanceof Container)
			{
				found.addAll(textFieldsIn((Container) child));
			}
		}
		return found;
	}

	/** The popup holds one radio item per value, in the enum's order, and only the current value's is selected. */
	private static void assertRadiosIn(JPopupMenu popup, Object[] values, Object current)
	{
		final List<JRadioButtonMenuItem> items = radiosIn(popup);
		assertEquals("one radio item per choice", values.length, items.size());
		for (int i = 0; i < values.length; i++)
		{
			assertEquals("the choice's words, in the enum's order", String.valueOf(values[i]), items.get(i).getText());
			assertEquals("only the current one is selected", values[i] == current, items.get(i).isSelected());
		}
	}

	/**
	 * The {@code rowIndex}th row of small buttons in the popup holds one button per value, in the enum's order, and only the
	 * current value's is selected.
	 */
	private static void assertTogglesIn(JPopupMenu popup, int rowIndex, Object[] values, Object current)
	{
		final List<JPanel> rows = new ArrayList<>();
		for (final Component part : popup.getComponents())
		{
			if (part instanceof JPanel && !togglesIn((JPanel) part).isEmpty())
			{
				rows.add((JPanel) part);
			}
		}
		final List<JToggleButton> toggles = togglesIn(rows.get(rowIndex));
		assertEquals("one small button per choice", values.length, toggles.size());
		for (int i = 0; i < values.length; i++)
		{
			assertEquals("the choice's words, in the enum's order", String.valueOf(values[i]), toggles.get(i).getText());
			assertEquals("only the current one is selected", values[i] == current, toggles.get(i).isSelected());
		}
	}

	/** The component as it paints itself, at its own size. */
	private static BufferedImage paintOf(Component part)
	{
		final BufferedImage image = new BufferedImage(Math.max(1, part.getWidth()), Math.max(1, part.getHeight()),
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			part.paint(g);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/** Arms {@code item} of {@code submenu} the way the mouse does: the selection path runs through the submenu to it. */
	private static void arm(JPopupMenu menu, JMenu submenu, JMenuItem item)
	{
		MenuSelectionManager.defaultManager().setSelectedPath(
			new MenuElement[] {menu, submenu, submenu.getPopupMenu(), item});
		assertTrue("the item is armed", item.isArmed());
	}

	/** The how-to's action line as written in its label, tags and all. */
	private static String rawActionLine(Container root)
	{
		for (final JLabel label : labelsIn(root))
		{
			final String text = plain(label.getText());
			if (text.startsWith("Hover the ") || text.startsWith("Hover a name"))
			{
				return label.getText();
			}
		}
		throw new AssertionError("no action line");
	}

	/** The how-to's action line as a player reads it, tags taken out. */
	private static String actionLine(Container root)
	{
		return plain(rawActionLine(root));
	}

	/** The how-to's title bar under {@code root}: the panel that carries the bar's name. */
	private static JPanel howToBarIn(Container root)
	{
		final JPanel bar = findHowToBar(root);
		assertNotNull("the panel has a how-to bar", bar);
		return bar;
	}

	/** The panel under {@code root} that carries the how-to bar's name, or null. */
	private static JPanel findHowToBar(Container root)
	{
		for (final Component child : root.getComponents())
		{
			if (child instanceof JPanel && ClanHubPanel.HOW_TO_BAR_NAME.equals(child.getName()))
			{
				return (JPanel) child;
			}
			if (child instanceof Container)
			{
				final JPanel found = findHowToBar((Container) child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	/** The how-to's rows under {@code root}, the first at the top. */
	private static List<JPanel> rowsOfTheHowTo(Container root)
	{
		final List<JPanel> found = new ArrayList<>();
		for (final Component child : root.getComponents())
		{
			if (child instanceof JPanel && ClanHubPanel.HOW_TO_ROW_NAME.equals(child.getName()))
			{
				found.add((JPanel) child);
			}
			else if (child instanceof Container)
			{
				found.addAll(rowsOfTheHowTo((Container) child));
			}
		}
		return found;
	}

	/** The icon box of a how-to row: the label that carries its picture. */
	private static JLabel iconBoxOf(JPanel howToRow)
	{
		for (final JLabel label : labelsIn(howToRow))
		{
			if (ClanHubPanel.HOW_TO_ICON_NAME.equals(label.getName()))
			{
				return label;
			}
		}
		throw new AssertionError("a how-to row has no icon box");
	}

	/** The how-to's tip: the label that says the theme tip inside the how-to's box (the bar's parent). */
	private static JLabel howToTipIn(Container root)
	{
		final Container box = howToBarIn(root).getParent();
		for (final JLabel label : labelsIn(root))
		{
			if (THEME_TIP.equals(plain(label.getText())) && isInside(label, box))
			{
				return label;
			}
		}
		throw new AssertionError("no label inside the how-to's box says: " + THEME_TIP);
	}

	/** The empty list's tip: the label that says {@link #EMPTY_LIST_TIP} outside the how-to's box. */
	private static JLabel emptyListTipIn(Container root)
	{
		final Container box = howToBarIn(root).getParent();
		for (final JLabel label : labelsIn(root))
		{
			if (EMPTY_LIST_TIP.equals(plain(label.getText())) && !isInside(label, box))
			{
				return label;
			}
		}
		throw new AssertionError("no label outside the how-to's box says: " + EMPTY_LIST_TIP);
	}

	/** Whether {@code part} is {@code box} or lies anywhere under it. */
	private static boolean isInside(Component part, Container box)
	{
		for (Component at = part; at != null; at = at.getParent())
		{
			if (at == box)
			{
				return true;
			}
		}
		return false;
	}

	/** An info card of a title, a wiki page and a line, with no facts, notes or picture, as the other tests make one. */
	private static InfoCard card(String title, String page, String line)
	{
		return new InfoCard(title, Collections.<HoverCard.Pair>emptyList(), Collections.<String>emptyList(), page, line, 0,
			line.length(), -1);
	}

	/** The panel in a visible frame that never takes the keyboard, laid out. */
	private static JFrame frameOf(ClanHubPanel panel)
	{
		final JFrame frame = new JFrame();
		frame.setFocusableWindowState(false);
		frame.add(panel);
		frame.setBounds(100, 100, 500, 600);
		frame.setVisible(true);
		frame.validate();
		return frame;
	}

	/** Lets everything already queued for the event thread run, from a thread that is not the event thread. */
	private static void flush() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { });
		SwingUtilities.invokeAndWait(() -> { });
	}

	/** Every label under {@code root}, in the tree's order. */
	private static List<JLabel> labelsIn(Container root)
	{
		final List<JLabel> found = new ArrayList<>();
		for (final Component child : root.getComponents())
		{
			if (child instanceof JLabel)
			{
				found.add((JLabel) child);
			}
			if (child instanceof Container)
			{
				found.addAll(labelsIn((Container) child));
			}
		}
		return found;
	}

	/** The words of every label under {@code root}, tags taken out. */
	private static List<String> textsIn(Container root)
	{
		final List<String> texts = new ArrayList<>();
		for (final JLabel label : labelsIn(root))
		{
			texts.add(plain(label.getText()));
		}
		return texts;
	}

	/** A label's text without its HTML tags, a line break read as a space. */
	private static String plain(String text)
	{
		return text == null ? "" : text.replaceAll("<br>", " ").replaceAll("<[^>]*>", "").trim();
	}

	/** The label under {@code root} whose words are {@code words}. */
	private static JLabel labelSaying(Container root, String words)
	{
		for (final JLabel label : labelsIn(root))
		{
			if (words.equals(plain(label.getText())))
			{
				return label;
			}
		}
		throw new AssertionError("no label says: " + words);
	}

	/** Sizes the frame so that the history's list is {@code width} px wide and lays it out. */
	private static void listWidthTo(JFrame frame, JList<CardHistory.Entry> list, int width)
	{
		frame.setSize(Math.max(1, frame.getWidth() + width - list.getWidth()), frame.getHeight());
		frame.validate();
		assertEquals("the list is as wide as asked", width, list.getWidth());
	}

	/**
	 * How wide the widest title of a sweep is at most, in px: wider than the room a first line has at the sidebar's width
	 * (172 px) beside any time, so the last title takes two lines beside any time, and short enough that its second line
	 * fits.
	 */
	private static final int SWEEP_END_PX = 190;

	/**
	 * Lot 12A: the titles of a sweep that widens a few px at a time: "Abyssal i", then one more "i" at each step, so that
	 * none is more than 3 px wider (in the row's title font, bold at 12 px) than the one before it, up to the width
	 * {@link #SWEEP_END_PX}. In a list as wide as the sidebar one of them fits beside a short time and not beside a longer
	 * one. The first word is short, so the first line of a title of two lines is the same word and the second line is the
	 * rest, which fits a line whole.
	 */
	private static List<String> sweepTitles()
	{
		final FontMetrics metrics = new JLabel().getFontMetrics(FontManager.getDefaultBoldFont().deriveFont(12f));
		final List<String> titles = new ArrayList<>();
		String title = "Abyssal i";
		int width = metrics.stringWidth(title);
		while (width < SWEEP_END_PX)
		{
			titles.add(title);
			final String next = title + "i";
			final int nextWidth = metrics.stringWidth(next);
			assertTrue("each title is at most 3 px wider than the one before it", nextWidth - width <= 3);
			title = next;
			width = nextWidth;
		}
		return titles;
	}

	/** Gives {@code cell} the size of its cell in the list and lays out everything in it, top down, as a draw would. */
	private static void layOut(Container cell, Rectangle bounds)
	{
		cell.setSize(bounds.width, bounds.height);
		layOutBelow(cell);
	}

	private static void layOutBelow(Container parent)
	{
		parent.doLayout();
		for (final Component child : parent.getComponents())
		{
			if (child instanceof Container)
			{
				layOutBelow((Container) child);
			}
		}
	}

	/** The top of {@code part} in the coordinates of {@code cell}. */
	private static int yIn(Container cell, Component part)
	{
		return SwingUtilities.convertPoint(part, 0, 0, cell).y;
	}

	/** No label in {@code cell} is given less room than its text needs, so none cuts its text with "...". */
	private static void assertWhole(Container cell)
	{
		for (final JLabel label : labelsIn(cell))
		{
			if (!label.getText().isEmpty())
			{
				assertTrue("'" + label.getText() + "' has the room it needs", label.getWidth() >= label.getPreferredSize().width);
			}
		}
	}

	/** The history's list under {@code root}, or null. */
	@SuppressWarnings("unchecked")
	private static JList<CardHistory.Entry> historyListIn(Container root)
	{
		for (final Component child : root.getComponents())
		{
			if (child instanceof JList && ClanHubPanel.HISTORY_LIST_NAME.equals(child.getName()))
			{
				return (JList<CardHistory.Entry>) child;
			}
			if (child instanceof Container)
			{
				final JList<CardHistory.Entry> found = historyListIn((Container) child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	/** The history's entries as the list's model holds them, the first at the top. */
	private static List<CardHistory.Entry> modelOf(JList<CardHistory.Entry> list)
	{
		final List<CardHistory.Entry> entries = new ArrayList<>();
		for (int i = 0; i < list.getModel().getSize(); i++)
		{
			entries.add(list.getModel().getElementAt(i));
		}
		return entries;
	}

	/** What the list's renderer draws for the entry at {@code index}: the cell, with its labels under it. */
	private static Container cellOf(JList<CardHistory.Entry> list, int index)
	{
		return (Container) list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(index),
			index, false, false);
	}

	/** The row inside the cell the renderer draws for {@code index}: the part that takes the hover ground. */
	private static Component rowOf(JList<CardHistory.Entry> list, int index)
	{
		return cellOf(list, index).getComponent(0);
	}

	/** The middle of the cell at {@code index}, in the list's own coordinates. */
	private static Point middleOf(JList<CardHistory.Entry> list, int index)
	{
		final Rectangle bounds = list.getCellBounds(index, index);
		return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
	}

	/** A mouse event of {@code type} on the list at {@code at}, delivered the way the event queue delivers it. */
	private static void mouse(JList<CardHistory.Entry> list, int type, Point at)
	{
		list.dispatchEvent(new MouseEvent(list, type, System.currentTimeMillis(), 0, at.x, at.y, 0, false));
	}

	/** The nearest component at or above {@code part} that listens to the mouse: where a press on {@code part} lands. */
	private static Component pressTarget(Component part)
	{
		Component at = part;
		while (at != null && at.getMouseListeners().length == 0)
		{
			at = at.getParent();
		}
		assertNotNull("a press on the part lands somewhere", at);
		return at;
	}

	/** A left-button press on {@code target}, delivered the way the event queue delivers it. */
	private static void leftPress(Component target)
	{
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, 3, 3, 1, false, MouseEvent.BUTTON1));
	}

	/** A left-button press on {@code target} at ({@code x}, {@code y}), delivered the way the event queue delivers it. */
	private static void leftPressAt(Component target, int x, int y)
	{
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
	}

	/** A key press on {@code source}, answering the extended key code as the AWT toolkit does, which {@link Keybind} reads. */
	private static KeyEvent keyPress(Component source, int keyCode, int modifiers)
	{
		return new KeyEvent(source, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode,
			KeyEvent.CHAR_UNDEFINED)
		{
			@Override
			public int getExtendedKeyCode()
			{
				return getKeyCode();
			}
		};
	}

	/** The mouse comes onto {@code part}: a MOUSE_ENTERED delivered to every listener on it. */
	private static void enter(Component part)
	{
		part.dispatchEvent(new MouseEvent(part, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 1, 1, 0, false));
	}

	/** The menu selection's path is {@code menu} alone: the menu is up and no submenu is open. */
	private static void assertPathIsThe(JPopupMenu menu)
	{
		final MenuElement[] path = MenuSelectionManager.defaultManager().getSelectedPath();
		assertEquals("the path ends at the menu", 1, path.length);
		assertSame("and is the menu", menu, path[0]);
	}

	/** The submenu's {@code index}th choice. */
	private static JRadioButtonMenuItem radio(JMenu submenu, int index)
	{
		return (JRadioButtonMenuItem) submenu.getMenuComponent(index);
	}

	/** The submenu holds one radio item per value, in the enum's order, and only the current value's is selected. */
	private static void assertRadios(JMenu submenu, Object[] values, Object current)
	{
		assertEquals("one radio item per choice", values.length, submenu.getMenuComponentCount());
		for (int i = 0; i < values.length; i++)
		{
			assertTrue("a radio item", submenu.getMenuComponent(i) instanceof JRadioButtonMenuItem);
			final JRadioButtonMenuItem item = radio(submenu, i);
			assertEquals("the choice's words, in the enum's order", String.valueOf(values[i]), item.getText());
			assertEquals("only the current one is selected", values[i] == current, item.isSelected());
		}
	}

	/** A left-button press and release inside the item, delivered the way the event queue delivers them. */
	private static void click(AbstractButton item)
	{
		final int x = Math.max(1, item.getWidth() / 2);
		final int y = Math.max(1, item.getHeight() / 2);
		item.dispatchEvent(new MouseEvent(item, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK,
			x, y, 1, false, MouseEvent.BUTTON1));
		item.dispatchEvent(new MouseEvent(item, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, 1, false,
			MouseEvent.BUTTON1));
	}

	/** The last component of a row: the control at its right. */
	private static Component lastOf(JPanel row)
	{
		return row.getComponent(row.getComponentCount() - 1);
	}

	/** Runs {@code body} on the event thread and rethrows what it threw. */
	private static void onEdt(Runnable body) throws Exception
	{
		final Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				body.run();
			}
			catch (Throwable thrown)
			{
				failure[0] = thrown;
			}
		});
		if (failure[0] instanceof Error)
		{
			throw (Error) failure[0];
		}
		if (failure[0] instanceof RuntimeException)
		{
			throw (RuntimeException) failure[0];
		}
	}

	/** The settings icon: the label with the settings tip. */
	private static JLabel settingsIcon(Container root)
	{
		for (final Component child : root.getComponents())
		{
			if (child instanceof JLabel && SupportLinks.SETTINGS_TIP.equals(((JLabel) child).getToolTipText()))
			{
				return (JLabel) child;
			}
			if (child instanceof Container)
			{
				final JLabel found = settingsIcon((Container) child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	/** A left-button press on the label, delivered to every mouse listener on it. */
	private static void press(JLabel label)
	{
		assertNotNull("the settings icon is in the panel", label);
		final MouseEvent event = new MouseEvent(label, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 1, 1, 1,
			false, MouseEvent.BUTTON1);
		for (final MouseListener listener : label.getMouseListeners())
		{
			listener.mousePressed(event);
		}
	}

	/** The popup menu showing in the frame, or in any other window the menu may have opened in. */
	private static JPopupMenu popupMenu(JFrame frame)
	{
		final JPopupMenu inFrame = popupIn(frame.getLayeredPane());
		if (inFrame != null)
		{
			return inFrame;
		}
		for (final Window window : Window.getWindows())
		{
			final JPopupMenu inWindow = popupIn(window);
			if (inWindow != null)
			{
				return inWindow;
			}
		}
		return null;
	}

	private static JPopupMenu popupIn(Component component)
	{
		if (component instanceof JPopupMenu)
		{
			return (JPopupMenu) component;
		}
		if (component instanceof Container)
		{
			for (final Component child : ((Container) component).getComponents())
			{
				final JPopupMenu found = popupIn(child);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}
}
