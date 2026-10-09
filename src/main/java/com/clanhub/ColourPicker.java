package com.clanhub;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Consumer;

/**
 * What the sidebar panel needs of a colour picker: open one on a colour, hear each new colour, and hear it close. The
 * plugin backs it with RuneLite's own picker; a test backs it with a stub that keeps the callbacks, so the panel never
 * touches RuneLite's picker class.
 */
interface ColourPicker
{
	/**
	 * Opens the picker.
	 *
	 * @param parent the component the picker belongs to
	 * @param title the picker window's title
	 * @param current the colour the picker starts on
	 * @param onChange told each colour the picker settles on while it is open
	 * @param onClose told the colour the picker holds when it closes
	 */
	void pick(Component parent, String title, Color current, Consumer<Color> onChange, Consumer<Color> onClose);
}
