package com.clanhub;

import com.clanhub.ClanHubConfig.CardPlace;
import com.clanhub.ClanHubConfig.CardTrigger;
import com.clanhub.ClanHubConfig.CardWidth;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
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
	private final List<Boolean> underlines = new ArrayList<>();
	private final List<CardTrigger> triggers = new ArrayList<>();
	private final List<CardPlace> places = new ArrayList<>();
	private final List<CardWidth> widths = new ArrayList<>();
	private final List<Integer> delays = new ArrayList<>();
	private final List<Integer> sizes = new ArrayList<>();

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
		public CardTrigger cardTrigger()
		{
			return CardTrigger.HOVER;
		}

		@Override
		public void setCardTrigger(CardTrigger trigger)
		{
			triggers.add(trigger);
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
			final ClanHubPanel panel = new ClanHubPanel(() -> { }, settings);
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
				assertEquals("header, rule, six checks, rule, five rows, rule, OK row", 16, parts.length);
				assertTrue("the header is the support header's one panel", parts[0] instanceof JPanel);
				assertEquals("the header's title, version and links", 3, ((JPanel) parts[0]).getComponentCount());
				assertTrue("a rule", parts[1] instanceof JPopupMenu.Separator);
				assertTrue("the first check", parts[2] instanceof JCheckBoxMenuItem);
				assertTrue("the second check", parts[3] instanceof JCheckBoxMenuItem);
				assertTrue("the third check", parts[4] instanceof JCheckBoxMenuItem);
				assertTrue("the fourth check", parts[5] instanceof JCheckBoxMenuItem);
				assertTrue("the fifth check", parts[6] instanceof JCheckBoxMenuItem);
				assertTrue("the sixth check", parts[7] instanceof JCheckBoxMenuItem);
				assertTrue("a rule", parts[8] instanceof JPopupMenu.Separator);
				assertTrue("the show row", parts[9] instanceof JPanel);
				assertTrue("the delay row", parts[10] instanceof JPanel);
				assertTrue("the place row", parts[11] instanceof JPanel);
				assertTrue("the width row", parts[12] instanceof JPanel);
				assertTrue("the size row", parts[13] instanceof JPanel);
				assertTrue("a rule", parts[14] instanceof JPopupMenu.Separator);
				assertTrue("the OK row", parts[15] instanceof JPanel);

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
				final JCheckBoxMenuItem underlineCheck = (JCheckBoxMenuItem) parts[7];
				assertEquals("Underline level-ups", underlineCheck.getText());
				assertFalse("not ticked, as the setting is", underlineCheck.isSelected());

				final JPanel showRow = (JPanel) parts[9];
				assertFalse("see-through", showRow.isOpaque());
				assertEquals("Show a card", ((JLabel) showRow.getComponent(0)).getText());
				@SuppressWarnings("unchecked")
				final JComboBox<CardTrigger> combo = (JComboBox<CardTrigger>) lastOf(showRow);
				assertEquals("On hover", String.valueOf(combo.getSelectedItem()));

				final JPanel delayRow = (JPanel) parts[10];
				assertEquals("Card delay (ms)", ((JLabel) delayRow.getComponent(0)).getText());
				final JSpinner delaySpinner = (JSpinner) lastOf(delayRow);
				assertEquals(0, delaySpinner.getValue());
				assertTrue("no wider than four digits", ((JSpinner.DefaultEditor) delaySpinner.getEditor())
					.getTextField().getColumns() <= 4);

				final JPanel placeRow = (JPanel) parts[11];
				assertFalse("see-through", placeRow.isOpaque());
				assertEquals("Card place", ((JLabel) placeRow.getComponent(0)).getText());
				@SuppressWarnings("unchecked")
				final JComboBox<CardPlace> placeCombo = (JComboBox<CardPlace>) lastOf(placeRow);
				assertEquals("Above the cursor", String.valueOf(placeCombo.getSelectedItem()));

				final JPanel widthRow = (JPanel) parts[12];
				assertFalse("see-through", widthRow.isOpaque());
				assertEquals("Card width", ((JLabel) widthRow.getComponent(0)).getText());
				@SuppressWarnings("unchecked")
				final JComboBox<CardWidth> widthCombo = (JComboBox<CardWidth>) lastOf(widthRow);
				assertEquals("Wide", String.valueOf(widthCombo.getSelectedItem()));

				final JPanel sizeRow = (JPanel) parts[13];
				assertFalse("see-through", sizeRow.isOpaque());
				assertEquals("Card text size", ((JLabel) sizeRow.getComponent(0)).getText());
				final JSpinner sizeSpinner = (JSpinner) lastOf(sizeRow);
				assertEquals(16, sizeSpinner.getValue());
				assertTrue("no wider than four digits", ((JSpinner.DefaultEditor) sizeSpinner.getEditor())
					.getTextField().getColumns() <= 4);

				final List<String> okRow = new ArrayList<>();
				for (final Component part : ((JPanel) parts[15]).getComponents())
				{
					if (part instanceof JButton)
					{
						okRow.add(((JButton) part).getText());
					}
				}
				assertEquals("the OK row is last: Reset to default, then OK", List.of("Reset to default", "OK"), okRow);

				assertTrue("opening the menu writes nothing back", shown.isEmpty() && coloured.isEmpty() && wiki.isEmpty()
					&& pictures.isEmpty() && bosses.isEmpty() && underlines.isEmpty() && triggers.isEmpty() && places.isEmpty() && widths.isEmpty() && delays.isEmpty() && sizes.isEmpty());
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
				combo.setSelectedItem(CardTrigger.HOLD_KEY);
				assertEquals("choosing 'While holding a key' says so", List.of(CardTrigger.HOLD_KEY), triggers);
				placeCombo.setSelectedItem(CardPlace.CHAT);
				assertEquals("choosing 'Above the chat' says so, once", List.of(CardPlace.CHAT), places);
				assertEquals("and the trigger was not touched again", List.of(CardTrigger.HOLD_KEY), triggers);
				widthCombo.setSelectedItem(CardWidth.MEDIUM);
				assertEquals("choosing 'Medium' says so, once", List.of(CardWidth.MEDIUM), widths);
				assertEquals("and the place was not touched again", List.of(CardPlace.CHAT), places);
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
