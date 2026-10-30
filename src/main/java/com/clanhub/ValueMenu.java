package com.clanhub;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JMenu;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.ui.ColorScheme;

/**
 * A submenu row that shows its current choice in grey on a second line under its caption ("Card theme", then "Snow
 * Village, Dawn Rose"): the caption is the row's text, and the two lines are drawn by the look and feel from an HTML view
 * the row sets itself, so a pick changes what is drawn and never the row's text.
 *
 * <p>Choice (the user's pick D of docs/art-research/round2/menu-width-options-1.png): the choice under the caption and not
 * beside it, so the menu is as narrow as its other rows; a choice wider than {@link #CHOICE_WIDTH} is cut with an
 * ellipsis, and the row is as wide for every choice, so a pick never changes its width and the menu under it never jumps.
 */
final class ValueMenu extends JMenu
{
	/** The widest a choice is drawn, in px; a wider one is cut to fit with an ellipsis. */
	static final int CHOICE_WIDTH = 150;
	private static final String ELLIPSIS = "…";
	private static final Color CHOICE_COLOUR = ColorScheme.LIGHT_GRAY_COLOR;

	private final String caption;
	private String value = "";
	/** The two-line view the row set last; the look and feel's own view is put back to it. */
	private View shown;

	/**
	 * @param text the row's caption
	 * @param choices the choices the row may show; the first is shown until {@link #setValue} says otherwise
	 */
	ValueMenu(String text, Object[] choices)
	{
		super(text);
		this.caption = text;
		// The look and feel sets the row's HTML view again from its plain caption whenever the font, the colour or the
		// screen changes, which drops the second line: the row puts its two lines back, drawn in the current font.
		addPropertyChangeListener(BasicHTML.propertyKey, e ->
		{
			if (e.getNewValue() != shown && getFont() != null)
			{
				showLines();
			}
		});
		setValue(choices.length > 0 ? String.valueOf(choices[0]) : "");
	}

	/** Shows {@code choice} on the row's second line; the row's text and width stay. */
	void setValue(String choice)
	{
		this.value = choice == null ? "" : choice;
		if (getFont() != null)
		{
			showLines();
		}
	}

	/** The choice the row shows now, whole (the drawn one may be cut). */
	String value()
	{
		return value;
	}

	/** The choice as drawn: whole when it fits {@link #CHOICE_WIDTH}, else cut to fit with an ellipsis. */
	String shownValue()
	{
		final Font font = getFont();
		if (font == null)
		{
			return value();
		}
		final FontMetrics metrics = getFontMetrics(font);
		final String whole = value();
		if (metrics.stringWidth(whole) <= CHOICE_WIDTH)
		{
			return whole;
		}
		String cut = whole;
		while (cut.length() > 1 && metrics.stringWidth(cut.trim() + ELLIPSIS) > CHOICE_WIDTH)
		{
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut.trim() + ELLIPSIS;
	}

	/** Sets the two lines again (a new choice), and lays the row out again. */
	private void showLines()
	{
		shown = lines();
		putClientProperty(BasicHTML.propertyKey, shown);
		revalidate();
		repaint();
	}

	/** The HTML view the look and feel draws: the caption, then the choice in grey under it. */
	private View lines()
	{
		// The body is as wide as the widest a line may be drawn, so every pick gives the row the same width; no choice
		// still keeps the second line, so the row's height stays too. Swing's HTML draws a "pt" as one pixel (a "px" as
		// 1.3), so the width is given in pt.
		final int width = Math.max(getFontMetrics(getFont()).stringWidth(caption), CHOICE_WIDTH);
		final String html = "<html><body style='width:" + width + "pt'>" + escape(caption) + "<br><font color='"
			+ hex(CHOICE_COLOUR) + "'>" + (value().isEmpty() ? "&nbsp;" : escape(shownValue())) + "</font></body></html>";
		return BasicHTML.createHTMLView(this, html);
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String hex(Color colour)
	{
		return String.format("#%02x%02x%02x", colour.getRed(), colour.getGreen(), colour.getBlue());
	}
}
