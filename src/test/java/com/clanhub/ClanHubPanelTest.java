package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTime;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.clanhub.ClanHubConfig.ExamineLine;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
	/** What the panel wrote: the values handed to the settings' setters. */
	private final List<Boolean> shown = new ArrayList<>();
	private final List<Boolean> coloured = new ArrayList<>();
	private final List<Boolean> wiki = new ArrayList<>();
	private final List<Boolean> pictures = new ArrayList<>();
	private final List<Boolean> bosses = new ArrayList<>();
	private final List<Boolean> examines = new ArrayList<>();
	private final List<Boolean> underlines = new ArrayList<>();
	private final List<CardTrigger> triggers = new ArrayList<>();
	private final List<Keybind> keys = new ArrayList<>();
	private final List<CardPlace> places = new ArrayList<>();
	private final List<CardWidth> widths = new ArrayList<>();
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

	/** A viewer that shows nothing: it records what it was asked. */
	private final CardViewer viewer = new CardViewer()
	{
		@Override
		public void show(HoverCard card, Rectangle rowOnScreen)
		{
			viewerShows.add(new Object[] {card, rowOnScreen});
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
				// Re-aimed by the lead, 2026-10-08 (lot 8b): a third check, "Wiki descriptions".
				// Re-aimed by the lead, 2026-10-08 (lot 8c): a fourth row, "Card place".
				// Re-aimed by the lead, 2026-10-08 (lot 8j): a fourth check, "Always underline level-ups".
				// Re-aimed by the lead, 2026-10-08 (lot 8Q): a fifth row, "Card width".
				// Re-aimed by the lead, 2026-10-08 (lot 8R): a fifth check, "Boss cards".
				// Re-aimed by the lead, 2026-10-08 (lot 8U): a sixth check, "Wiki pictures".
				// Re-aimed by the lead, 2026-10-09 (lot 9B): the choice rows are submenus with radio items.
				// Re-aimed by the lead, 2026-10-09 (lot 9D): two colour rows after the number boxes.
				// Re-aimed by the lead, 2026-10-09 (lot 9F): the card key row after the show submenu.
				// Re-aimed by the lead, 2026-10-23 (lot 10A): a seventh check, "Examine cards".
				// Re-aimed by the lead, 2026-10-24 (lot 10B): a fourth choice submenu, "Examine line".
				// Re-aimed by the lead, 2026-10-25 (lot 10K): a fifth choice submenu, "Card time".
				// Re-aimed by the lead, 2026-10-25 (lot 10L): a third colour row, "Name colour".
				// Re-aimed by the lead, 2026-10-25 (lot 10M): the name swatch reads the 2h orange.
				assertEquals("header, rule, seven checks, rule, the show submenu, the key row, the delay row, two submenus,"
					+ " the examine line submenu, the card time submenu, the size row, three colour rows, rule, OK row", 23,
					parts.length);
				assertTrue("the header is the support header's one panel", parts[0] instanceof JPanel);
				assertEquals("the header's title, version and links", 3, ((JPanel) parts[0]).getComponentCount());
				assertTrue("a rule", parts[1] instanceof JPopupMenu.Separator);
				assertTrue("the first check", parts[2] instanceof JCheckBoxMenuItem);
				assertTrue("the second check", parts[3] instanceof JCheckBoxMenuItem);
				assertTrue("the third check", parts[4] instanceof JCheckBoxMenuItem);
				assertTrue("the fourth check", parts[5] instanceof JCheckBoxMenuItem);
				assertTrue("the fifth check", parts[6] instanceof JCheckBoxMenuItem);
				assertTrue("the sixth check", parts[7] instanceof JCheckBoxMenuItem);
				assertTrue("the seventh check", parts[8] instanceof JCheckBoxMenuItem);
				assertTrue("a rule", parts[9] instanceof JPopupMenu.Separator);
				assertTrue("the show submenu", parts[10] instanceof JMenu);
				assertTrue("the key row", parts[11] instanceof JPanel);
				assertTrue("the delay row", parts[12] instanceof JPanel);
				assertTrue("the place submenu", parts[13] instanceof JMenu);
				assertTrue("the width submenu", parts[14] instanceof JMenu);
				assertTrue("the examine line submenu", parts[15] instanceof JMenu);
				assertTrue("the card time submenu", parts[16] instanceof JMenu);
				assertTrue("the size row", parts[17] instanceof JPanel);
				assertTrue("the hover colour row", parts[18] instanceof JPanel);
				assertTrue("the underline colour row", parts[19] instanceof JPanel);
				assertTrue("the name colour row", parts[20] instanceof JPanel);
				assertTrue("a rule", parts[21] instanceof JPopupMenu.Separator);
				assertTrue("the OK row", parts[22] instanceof JPanel);

				final JCheckBoxMenuItem showCheck = (JCheckBoxMenuItem) parts[2];
				assertEquals("Show hover cards", showCheck.getText());
				assertTrue("ticked, as the setting is", showCheck.isSelected());
				final JCheckBoxMenuItem colourCheck = (JCheckBoxMenuItem) parts[3];
				assertEquals("Colour hoverable words", colourCheck.getText());
				assertTrue("ticked, as the setting is", colourCheck.isSelected());
				final JCheckBoxMenuItem wikiCheck = (JCheckBoxMenuItem) parts[4];
				assertEquals("Wiki descriptions", wikiCheck.getText());
				assertTrue("ticked, as the setting is", wikiCheck.isSelected());
				final JCheckBoxMenuItem picturesCheck = (JCheckBoxMenuItem) parts[5];
				assertEquals("Wiki pictures", picturesCheck.getText());
				assertTrue("ticked, as the setting is", picturesCheck.isSelected());
				final JCheckBoxMenuItem bossCheck = (JCheckBoxMenuItem) parts[6];
				assertEquals("Boss cards", bossCheck.getText());
				assertTrue("ticked, as the setting is", bossCheck.isSelected());
				// Re-aimed by the lead, 2026-10-23 (lot 10A): "Examine cards" right after "Boss cards", as the settings page has it.
				final JCheckBoxMenuItem underlineCheck = (JCheckBoxMenuItem) parts[8];
				assertEquals("Underline level-ups", underlineCheck.getText());
				assertFalse("not ticked, as the setting is", underlineCheck.isSelected());
				final JCheckBoxMenuItem examineCheck = (JCheckBoxMenuItem) parts[7];
				assertEquals("Examine cards", examineCheck.getText());
				assertTrue("ticked, as the setting is", examineCheck.isSelected());

				final JMenu showMenu = (JMenu) parts[10];
				assertEquals("Show a card: On hover", showMenu.getText());
				assertRadios(showMenu, CardTrigger.values(), CardTrigger.HOVER);

				final JPanel keyRow = (JPanel) parts[11];
				assertEquals("Card key", ((JLabel) keyRow.getComponent(0)).getText());
				assertTrue("the key row ends in a button", lastOf(keyRow) instanceof JButton);
				assertEquals("Alt", ((JButton) lastOf(keyRow)).getText());
				assertFalse("greyed unless the show mode is 'While holding a key'", lastOf(keyRow).isEnabled());

				final JPanel delayRow = (JPanel) parts[12];
				assertEquals("Card delay (ms)", ((JLabel) delayRow.getComponent(0)).getText());
				final JSpinner delaySpinner = (JSpinner) lastOf(delayRow);
				assertEquals(0, delaySpinner.getValue());
				assertTrue("no wider than four digits", ((JSpinner.DefaultEditor) delaySpinner.getEditor())
					.getTextField().getColumns() <= 4);

				final JMenu placeMenu = (JMenu) parts[13];
				assertEquals("Card place: Above the cursor", placeMenu.getText());
				assertRadios(placeMenu, CardPlace.values(), CardPlace.CURSOR);

				final JMenu widthMenu = (JMenu) parts[14];
				assertEquals("Card width: Wide", widthMenu.getText());
				assertRadios(widthMenu, CardWidth.values(), CardWidth.WIDE);

				final JMenu examineLineMenu = (JMenu) parts[15];
				assertEquals("Examine line: Item name first", examineLineMenu.getText());
				assertRadios(examineLineMenu, ExamineLine.values(), ExamineLine.NAME_FIRST);

				final JMenu cardTimeMenu = (JMenu) parts[16];
				assertEquals("Card time: 4m ago", cardTimeMenu.getText());
				assertRadios(cardTimeMenu, CardTime.values(), CardTime.AGO);

				final JPanel sizeRow = (JPanel) parts[17];
				assertFalse("see-through", sizeRow.isOpaque());
				assertEquals("Card text size", ((JLabel) sizeRow.getComponent(0)).getText());
				final JSpinner sizeSpinner = (JSpinner) lastOf(sizeRow);
				assertEquals(16, sizeSpinner.getValue());
				assertTrue("no wider than four digits", ((JSpinner.DefaultEditor) sizeSpinner.getEditor())
					.getTextField().getColumns() <= 4);

				final JPanel hoverRow = (JPanel) parts[18];
				assertEquals("Hover colour", ((JLabel) hoverRow.getComponent(0)).getText());
				assertTrue("the hover colour row ends in a swatch", lastOf(hoverRow) instanceof JButton);
				assertEquals(new Color(59, 130, 246), lastOf(hoverRow).getBackground());
				final JPanel underlineRow = (JPanel) parts[19];
				assertEquals("Underline colour", ((JLabel) underlineRow.getComponent(0)).getText());
				assertTrue("the underline colour row ends in a swatch", lastOf(underlineRow) instanceof JButton);
				assertEquals(new Color(150, 150, 150), lastOf(underlineRow).getBackground());
				final JPanel nameRow = (JPanel) parts[20];
				assertEquals("Name colour", ((JLabel) nameRow.getComponent(0)).getText());
				assertTrue("the name colour row ends in a swatch", lastOf(nameRow) instanceof JButton);
				assertEquals(new Color(220, 138, 0), lastOf(nameRow).getBackground());

				final List<String> okRow = new ArrayList<>();
				for (final Component part : ((JPanel) parts[22]).getComponents())
				{
					if (part instanceof JButton)
					{
						okRow.add(((JButton) part).getText());
					}
				}
				assertEquals("the OK row is last: Reset to default, then OK", List.of("Reset to default", "OK"), okRow);

				assertTrue("opening the menu writes nothing back", shown.isEmpty() && coloured.isEmpty() && wiki.isEmpty()
					&& pictures.isEmpty() && bosses.isEmpty() && examines.isEmpty() && underlines.isEmpty() && triggers.isEmpty() && keys.isEmpty() && places.isEmpty() && widths.isEmpty() && examineLines.isEmpty() && cardTimes.isEmpty() && delays.isEmpty()
					&& sizes.isEmpty() && hoverColours.isEmpty() && underlineColours.isEmpty() && nameColours.isEmpty());
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
				radio(showMenu, 1).doClick(0);
				assertEquals("choosing 'While holding a key' says so", List.of(CardTrigger.HOLD_KEY), triggers);
				assertEquals("and the submenu says so", "Show a card: While holding a key", showMenu.getText());
				assertTrue("and the key box is enabled", lastOf(keyRow).isEnabled());
				radio(placeMenu, 1).doClick(0);
				assertEquals("choosing 'Above the chat' says so, once", List.of(CardPlace.CHAT), places);
				assertEquals("and the trigger was not touched again", List.of(CardTrigger.HOLD_KEY), triggers);
				radio(widthMenu, 1).doClick(0);
				assertEquals("choosing 'Medium' says so, once", List.of(CardWidth.MEDIUM), widths);
				assertEquals("and the place was not touched again", List.of(CardPlace.CHAT), places);
				radio(examineLineMenu, 1).doClick(0);
				assertEquals("choosing 'Description only' says so, once", List.of(ExamineLine.DESCRIPTION), examineLines);
				assertEquals("and the submenu says so", "Examine line: Description only", examineLineMenu.getText());
				assertEquals("and the width was not touched again", List.of(CardWidth.MEDIUM), widths);
				radio(cardTimeMenu, 1).doClick(0);
				assertEquals("choosing '14:32' says so, once", List.of(CardTime.CLOCK_24), cardTimes);
				assertEquals("and the submenu says so", "Card time: 14:32", cardTimeMenu.getText());
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

				final JCheckBoxMenuItem colourCheck = (JCheckBoxMenuItem) menu.getComponents()[3];
				assertEquals("Colour hoverable words", colourCheck.getText());
				final boolean before = colourCheck.isSelected();
				assertTrue("ticked, as the setting is", before);

				click(colourCheck);
				assertTrue("the menu is still up after the click", menu.isVisible());
				assertEquals("the check flipped", !before, colourCheck.isSelected());
				assertEquals("one write, un-ticked", List.of(false), coloured);

				click(colourCheck);
				assertTrue("the menu is still up after the second click", menu.isVisible());
				assertEquals("the check flipped back", before, colourCheck.isSelected());
				assertEquals("a second write, ticked again", List.of(false, true), coloured);

				JButton ok = null;
				for (final Component part : ((JPanel) menu.getComponents()[22]).getComponents())
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
	 * Lot 9B: a click on a choice in a submenu writes it, retitles the submenu and closes the submenu alone; the menu
	 * stays up until OK takes it down.
	 */
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

				final JMenu showMenu = (JMenu) menu.getComponents()[10];
				assertEquals("Show a card: On hover", showMenu.getText());
				MenuSelectionManager.defaultManager().setSelectedPath(
					new MenuElement[] {menu, showMenu, showMenu.getPopupMenu()});
				assertTrue("the submenu is up", showMenu.getPopupMenu().isVisible());
				assertTrue("the menu is still up under it", menu.isVisible());

				final JRadioButtonMenuItem onClick = radio(showMenu, 2);
				assertEquals("On click", onClick.getText());
				click(onClick);

				assertTrue("the menu is still up after the choice", menu.isVisible());
				assertFalse("the submenu is down", showMenu.getPopupMenu().isVisible());
				assertEquals("the submenu says the choice", "Show a card: On click", showMenu.getText());
				assertTrue("the chosen item is the selected one", onClick.isSelected());
				assertEquals("one write, 'On click'", List.of(CardTrigger.CLICK), triggers);

				JButton ok = null;
				for (final Component part : ((JPanel) menu.getComponents()[22]).getComponents())
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
	 * Lot 9D: a press on a colour swatch takes the menu down and asks the picker for that colour; each colour the picker
	 * settles on is written and shown on the swatch; the picker closing brings the menu back.
	 */
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

				final JButton hoverSwatch = (JButton) lastOf((JPanel) menu.getComponents()[18]);
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

				final JButton underlineSwatch = (JButton) lastOf((JPanel) menu.getComponents()[19]);
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
	 * Lot 10M: a choice submenu left open closes when the mouse enters a plain row of the menu (a colour row, a
	 * spinner, the OK row); the menu itself stays up, and with no submenu open entering a row changes nothing.
	 */
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

				final Component[] parts = menu.getComponents();
				final JMenu cardTimeMenu = (JMenu) parts[16];
				assertEquals("Card time: 4m ago", cardTimeMenu.getText());
				final JPanel nameRow = (JPanel) parts[20];
				assertEquals("Name colour", ((JLabel) nameRow.getComponent(0)).getText());
				final JSpinner delaySpinner = (JSpinner) lastOf((JPanel) parts[12]);
				final JPanel okRow = (JPanel) parts[22];

				MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] {menu});
				enter(nameRow);
				enter(delaySpinner);
				enter(okRow);
				assertPathIsThe(menu);
				assertTrue("with no submenu open nothing happens to the menu", menu.isVisible());

				final MenuElement[] open = {menu, cardTimeMenu, cardTimeMenu.getPopupMenu()};
				MenuSelectionManager.defaultManager().setSelectedPath(open);
				assertTrue("the submenu is up", cardTimeMenu.getPopupMenu().isVisible());
				enter(nameRow);
				assertPathIsThe(menu);
				assertFalse("the Name colour row took the submenu down", cardTimeMenu.getPopupMenu().isVisible());
				assertTrue("the menu is still up", menu.isVisible());

				MenuSelectionManager.defaultManager().setSelectedPath(open);
				assertTrue("the submenu is up again", cardTimeMenu.getPopupMenu().isVisible());
				enter(delaySpinner);
				assertPathIsThe(menu);
				assertFalse("the delay row's spinner took the submenu down", cardTimeMenu.getPopupMenu().isVisible());

				MenuSelectionManager.defaultManager().setSelectedPath(open);
				assertTrue("the submenu is up a third time", cardTimeMenu.getPopupMenu().isVisible());
				enter(((JSpinner.DefaultEditor) delaySpinner.getEditor()).getTextField());
				assertPathIsThe(menu);
				assertFalse("the spinner's text field took the submenu down", cardTimeMenu.getPopupMenu().isVisible());

				MenuSelectionManager.defaultManager().setSelectedPath(open);
				assertTrue("the submenu is up a fourth time", cardTimeMenu.getPopupMenu().isVisible());
				enter(okRow);
				assertPathIsThe(menu);
				assertFalse("the OK row took the submenu down", cardTimeMenu.getPopupMenu().isVisible());
				assertTrue("the menu is still up after all of it", menu.isVisible());
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

				final JButton hoverSwatch = (JButton) lastOf((JPanel) menu.getComponents()[18]);
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
				final JButton nameSwatch = (JButton) lastOf((JPanel) menu.getComponents()[20]);
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

				final JButton box = (JButton) lastOf((JPanel) menu.getComponents()[11]);
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

				final JSpinner delaySpinner = (JSpinner) lastOf((JPanel) menu.getComponents()[12]);
				final JSpinner sizeSpinner = (JSpinner) lastOf((JPanel) menu.getComponents()[17]);
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

				final JSpinner delaySpinner = (JSpinner) lastOf((JPanel) menu.getComponents()[12]);
				final JSpinner sizeSpinner = (JSpinner) lastOf((JPanel) menu.getComponents()[17]);
				assertFalse("the delay box is not focusable at rest",
					((JSpinner.DefaultEditor) delaySpinner.getEditor()).getTextField().isFocusable());
				assertFalse("the size box is not focusable at rest",
					((JSpinner.DefaultEditor) sizeSpinner.getEditor()).getTextField().isFocusable());
				final JButton box = (JButton) lastOf((JPanel) menu.getComponents()[11]);
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
				final JCheckBoxMenuItem colourCheck = (JCheckBoxMenuItem) menu.getComponents()[3];
				assertEquals("Colour hoverable words", colourCheck.getText());
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
				assertTrue("when", texts.contains("just now"));
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
				assertTrue("at 'ago' the row reads 'just now'", textsIn(cellOf(list, 0)).contains("just now"));

				cardTime = CardTime.CLOCK_24;
				assertTrue("at the 24-hour clock the row reads the moment as HH:mm", textsIn(cellOf(list, 0)).contains(clock24));
				assertFalse("and no longer 'just now'", textsIn(cellOf(list, 0)).contains("just now"));

				cardTime = CardTime.CLOCK_12;
				assertTrue("at the 12-hour clock the row reads the moment as h:mm a", textsIn(cellOf(list, 0)).contains(clock12));
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
