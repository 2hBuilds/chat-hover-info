package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import com.clanhub.ClanHubConfig.ExamineLine;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import net.runelite.client.config.Keybind;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
	private final List<Integer> delays = new ArrayList<>();
	private final List<Integer> sizes = new ArrayList<>();
	private final List<Color> hoverColours = new ArrayList<>();
	private final List<Color> underlineColours = new ArrayList<>();

	/** The key and the show mode the settings stub answers: a test may set them before the menu opens. */
	private Keybind cardKey = Keybind.ALT;
	private CardTrigger trigger = CardTrigger.HOVER;

	/** What the picker stub was asked: the parent, the title and the starting colour of each call. */
	private final List<Object[]> pickerCalls = new ArrayList<>();
	private Consumer<Color> pickerOnChange;
	private Consumer<Color> pickerOnClose;

	/** A colour picker that opens nothing: it records the call and keeps the callbacks for the test to call. */
	private final ColourPicker picker = (parent, title, current, onChange, onClose) ->
	{
		pickerCalls.add(new Object[] {parent, title, current});
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
			return true;
		}

		@Override
		public void setColourHoverables(boolean colour)
		{
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
	};

	@Test
	public void theMenuHoldsTheSwitchesTheRowsAndTheOkRow() throws Exception
	{
		onEdt(() ->
		{
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings, picker);
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
				assertEquals("header, rule, seven checks, rule, the show submenu, the key row, the delay row, two submenus,"
					+ " the examine line submenu, the size row, two colour rows, rule, OK row", 21, parts.length);
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
				assertTrue("the size row", parts[16] instanceof JPanel);
				assertTrue("the hover colour row", parts[17] instanceof JPanel);
				assertTrue("the underline colour row", parts[18] instanceof JPanel);
				assertTrue("a rule", parts[19] instanceof JPopupMenu.Separator);
				assertTrue("the OK row", parts[20] instanceof JPanel);

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

				final JPanel sizeRow = (JPanel) parts[16];
				assertFalse("see-through", sizeRow.isOpaque());
				assertEquals("Card text size", ((JLabel) sizeRow.getComponent(0)).getText());
				final JSpinner sizeSpinner = (JSpinner) lastOf(sizeRow);
				assertEquals(16, sizeSpinner.getValue());
				assertTrue("no wider than four digits", ((JSpinner.DefaultEditor) sizeSpinner.getEditor())
					.getTextField().getColumns() <= 4);

				final JPanel hoverRow = (JPanel) parts[17];
				assertEquals("Hover colour", ((JLabel) hoverRow.getComponent(0)).getText());
				assertTrue("the hover colour row ends in a swatch", lastOf(hoverRow) instanceof JButton);
				assertEquals(new Color(59, 130, 246), lastOf(hoverRow).getBackground());
				final JPanel underlineRow = (JPanel) parts[18];
				assertEquals("Underline colour", ((JLabel) underlineRow.getComponent(0)).getText());
				assertTrue("the underline colour row ends in a swatch", lastOf(underlineRow) instanceof JButton);
				assertEquals(new Color(150, 150, 150), lastOf(underlineRow).getBackground());

				final List<String> okRow = new ArrayList<>();
				for (final Component part : ((JPanel) parts[20]).getComponents())
				{
					if (part instanceof JButton)
					{
						okRow.add(((JButton) part).getText());
					}
				}
				assertEquals("the OK row is last: Reset to default, then OK", List.of("Reset to default", "OK"), okRow);

				assertTrue("opening the menu writes nothing back", shown.isEmpty() && coloured.isEmpty() && wiki.isEmpty()
					&& pictures.isEmpty() && bosses.isEmpty() && examines.isEmpty() && underlines.isEmpty() && triggers.isEmpty() && keys.isEmpty() && places.isEmpty() && widths.isEmpty() && examineLines.isEmpty() && delays.isEmpty()
					&& sizes.isEmpty() && hoverColours.isEmpty() && underlineColours.isEmpty());
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
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings, picker);
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
				for (final Component part : ((JPanel) menu.getComponents()[20]).getComponents())
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
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings, picker);
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
				for (final Component part : ((JPanel) menu.getComponents()[20]).getComponents())
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
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings, picker);
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

				final JButton hoverSwatch = (JButton) lastOf((JPanel) menu.getComponents()[17]);
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

				final JButton underlineSwatch = (JButton) lastOf((JPanel) menu.getComponents()[18]);
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
	 * Lot 9F: the card key box waits for a press, takes the next key (Escape keeps the old one, Backspace clears it, a
	 * modifier alone is a key), and holds the keyboard only while it waits.
	 */
	@Test
	public void theKeyBoxTakesTheNextKey() throws Exception
	{
		trigger = CardTrigger.HOLD_KEY;
		onEdt(() ->
		{
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings, picker);
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
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings, picker);
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
				final JSpinner sizeSpinner = (JSpinner) lastOf((JPanel) menu.getComponents()[16]);
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
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings, picker);
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
				final JSpinner sizeSpinner = (JSpinner) lastOf((JPanel) menu.getComponents()[16]);
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
