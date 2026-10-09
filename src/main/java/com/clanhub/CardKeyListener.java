package com.clanhub;

import com.clanhub.ClanHubConfig.CardTrigger;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;

/**
 * The listener of the card key. It tells the overlay when the key goes down and up, and lets every key through to the
 * game except in one case: the key is taken (the press, the character it would type and the release are consumed) only
 * while "Show a card" is "While holding a key", "Show hover cards" is on and the mouse is on a card's words. Everywhere
 * else the key types as usual, so a letter set as the card key can still be typed in the chat.
 *
 * <p>Choice: not RuneLite's {@code HotkeyListener}, which consumes every press and release of its key in every show
 * mode, whether the cards are on or off and whether or not the mouse is on a card, so a letter key set as the card key
 * could not be typed into the chat (the user hit it with 'g'). The take is decided at the press and kept to its
 * release and its typed character, so a key taken at its press is never half-taken: the character does not reach the
 * chat and the game does not see a release without a press. All of it runs on the AWT thread only, so the state needs
 * no lock.
 */
final class CardKeyListener implements KeyListener
{
	private final Supplier<Keybind> key;
	private final Supplier<CardTrigger> trigger;
	private final BooleanSupplier cardsOn;
	private final Supplier<HoverCard> hovered;
	private final Consumer<Boolean> held;

	/** The key code of the matching press not yet released, {@link KeyEvent#VK_UNDEFINED} for none. */
	private int pressedCode = KeyEvent.VK_UNDEFINED;
	/** Whether the press now down was taken from the game. */
	private boolean using;

	/**
	 * @param key the card key setting
	 * @param trigger the show mode setting
	 * @param cardsOn whether "Show hover cards" is on
	 * @param hovered the card under the mouse as of the last frame, or null
	 * @param held told true when the card key goes down and false when it comes up or the window loses focus
	 */
	CardKeyListener(Supplier<Keybind> key, Supplier<CardTrigger> trigger, BooleanSupplier cardsOn,
		Supplier<HoverCard> hovered, Consumer<Boolean> held)
	{
		this.key = key;
		this.trigger = trigger;
		this.cardsOn = cardsOn;
		this.hovered = hovered;
		this.held = held;
	}

	/** Consumes the character of a key that was taken at its press, so it never reaches the chat. */
	@Override
	public void keyTyped(KeyEvent e)
	{
		if (using)
		{
			e.consume();
		}
	}

	/**
	 * Tells the overlay the card key is down, and takes the press from the game when the key does something: the show mode
	 * is "While holding a key", the cards are on and the mouse is on a card. A key that is not the card key is left alone.
	 */
	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!key.get().matches(e))
		{
			return;
		}
		held.accept(true);
		pressedCode = e.getKeyCode();
		if (cardsOn.getAsBoolean() && trigger.get() == CardTrigger.HOLD_KEY && hovered.get() != null)
		{
			e.consume();
			using = true;
		}
	}

	/** Tells the overlay the card key is up, and takes the release from the game if its press was taken. */
	@Override
	public void keyReleased(KeyEvent e)
	{
		if (pressedCode != KeyEvent.VK_UNDEFINED && e.getKeyCode() == pressedCode)
		{
			held.accept(false);
			pressedCode = KeyEvent.VK_UNDEFINED;
			if (using)
			{
				e.consume();
				using = false;
			}
		}
	}

	/** The window lost focus, so no release will come: the key is up and nothing is taken. */
	@Override
	public void focusLost()
	{
		held.accept(false);
		pressedCode = KeyEvent.VK_UNDEFINED;
		using = false;
	}
}
