package com.clanhub;

import com.clanhub.ClanHubConfig.CardTrigger;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import net.runelite.client.config.Keybind;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link CardKeyListener}: the card key is taken from the game only in the key mode with the cards on and the mouse on a
 * card; every other key and every other case types as usual.
 */
public class CardKeyListenerTest
{
	private final JPanel source = new JPanel();
	private final List<Boolean> held = new ArrayList<>();
	private Keybind key = new Keybind(KeyEvent.VK_G, 0);
	private CardTrigger trigger = CardTrigger.HOLD_KEY;
	private boolean cardsOn = true;
	private HoverCard hovered;

	private CardKeyListener listener()
	{
		return new CardKeyListener(() -> key, () -> trigger, () -> cardsOn, () -> hovered, held::add);
	}

	/**
	 * A key event as the game's window sends it. The constructor leaves the extended key code at 0, and only the AWT
	 * toolkit sets it (to the key code for a key like G), which RuneLite's {@code Keybind.matches} reads; so the double
	 * answers it as the toolkit does.
	 */
	private KeyEvent event(int id, int keyCode, char keyChar, int modifiers)
	{
		return new KeyEvent(source, id, 0L, modifiers, keyCode, keyChar)
		{
			@Override
			public int getExtendedKeyCode()
			{
				return getKeyCode();
			}
		};
	}

	private KeyEvent press(int keyCode, char keyChar)
	{
		return event(KeyEvent.KEY_PRESSED, keyCode, keyChar, 0);
	}

	private KeyEvent typed(char keyChar)
	{
		return event(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, keyChar, 0);
	}

	private KeyEvent release(int keyCode, char keyChar)
	{
		return event(KeyEvent.KEY_RELEASED, keyCode, keyChar, 0);
	}

	// L8Z-1
	@Test
	public void aLetterKeyTypesWhenNoCardIsUnderTheMouse()
	{
		final CardKeyListener listener = listener();
		hovered = null;

		final KeyEvent press = press(KeyEvent.VK_G, 'g');
		listener.keyPressed(press);
		assertFalse("the press types as usual", press.isConsumed());
		assertEquals("the overlay is told the key is down", List.of(true), held);

		final KeyEvent typed = typed('g');
		listener.keyTyped(typed);
		assertFalse("the typed character reaches the chat", typed.isConsumed());

		final KeyEvent release = release(KeyEvent.VK_G, 'g');
		listener.keyReleased(release);
		assertFalse("the release is passed on", release.isConsumed());
		assertEquals("the overlay is told the key is up", List.of(true, false), held);
	}

	// L8Z-2
	@Test
	public void theKeyIsTakenOnlyOverACardInTheKeyMode()
	{
		hovered = Mockito.mock(HoverCard.class);
		final CardKeyListener listener = listener();

		final KeyEvent press = press(KeyEvent.VK_G, 'g');
		listener.keyPressed(press);
		assertTrue("the press is taken over a card in the key mode", press.isConsumed());
		assertEquals(List.of(true), held);

		final KeyEvent typed = typed('g');
		listener.keyTyped(typed);
		assertTrue("the character of a taken key never reaches the chat", typed.isConsumed());

		final KeyEvent release = release(KeyEvent.VK_G, 'g');
		listener.keyReleased(release);
		assertTrue("the release of a taken key is taken", release.isConsumed());
		assertEquals(List.of(true, false), held);

		trigger = CardTrigger.HOVER;
		final KeyEvent onHover = press(KeyEvent.VK_G, 'g');
		listener().keyPressed(onHover);
		assertFalse("on hover the key types", onHover.isConsumed());

		trigger = CardTrigger.CLICK;
		final KeyEvent onClick = press(KeyEvent.VK_G, 'g');
		listener().keyPressed(onClick);
		assertFalse("on click the key types", onClick.isConsumed());

		trigger = CardTrigger.HOLD_KEY;
		cardsOn = false;
		final KeyEvent cardsOff = press(KeyEvent.VK_G, 'g');
		listener().keyPressed(cardsOff);
		assertFalse("with the cards off the key types", cardsOff.isConsumed());
	}

	// L8Z-3
	@Test
	public void anotherKeyPassesUntouched()
	{
		hovered = Mockito.mock(HoverCard.class);
		final CardKeyListener listener = listener();

		final KeyEvent press = press(KeyEvent.VK_H, 'h');
		listener.keyPressed(press);
		final KeyEvent typed = typed('h');
		listener.keyTyped(typed);
		final KeyEvent release = release(KeyEvent.VK_H, 'h');
		listener.keyReleased(release);

		assertFalse(press.isConsumed());
		assertFalse(typed.isConsumed());
		assertFalse(release.isConsumed());
		assertTrue("the overlay is never told", held.isEmpty());
	}

	// L8Z-4
	@Test
	public void losingFocusLetsGo()
	{
		hovered = Mockito.mock(HoverCard.class);
		final CardKeyListener listener = listener();

		final KeyEvent press = press(KeyEvent.VK_G, 'g');
		listener.keyPressed(press);
		assertTrue("the press was taken", press.isConsumed());
		assertEquals(List.of(true), held);

		listener.focusLost();
		assertEquals("the overlay is told the key is up", List.of(true, false), held);

		final KeyEvent typed = typed('g');
		listener.keyTyped(typed);
		assertFalse("a character after the focus was lost types", typed.isConsumed());

		final KeyEvent release = release(KeyEvent.VK_G, 'g');
		listener.keyReleased(release);
		assertFalse("a release after the focus was lost is passed on", release.isConsumed());
		assertEquals("the overlay is not told again", List.of(true, false), held);
	}

	// L8Z-5
	@Test
	public void theDefaultAltStillHolds()
	{
		key = Keybind.ALT;
		hovered = null;
		final CardKeyListener listener = listener();

		final KeyEvent press = event(KeyEvent.KEY_PRESSED, KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED,
			KeyEvent.ALT_DOWN_MASK);
		listener.keyPressed(press);
		assertFalse("Alt is not taken with no card under the mouse", press.isConsumed());
		assertEquals(List.of(true), held);

		final KeyEvent release = event(KeyEvent.KEY_RELEASED, KeyEvent.VK_ALT, KeyEvent.CHAR_UNDEFINED, 0);
		listener.keyReleased(release);
		assertFalse(release.isConsumed());
		assertEquals(List.of(true, false), held);
	}
}
