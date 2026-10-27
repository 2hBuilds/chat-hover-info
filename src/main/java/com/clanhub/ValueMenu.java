package com.clanhub;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.UIManager;
import net.runelite.client.ui.ColorScheme;

/**
 * A submenu row that shows its current choice in grey at the right, before the arrow ("Card theme ... Classic >"): the
 * caption is the row's text and the choice is drawn over the row's right side by the row itself, so a pick changes
 * what is drawn and never the row's text.
 *
 * <p>Choice: the row makes room once for the widest choice of its list, so a pick never changes its width and the menu
 * under it never jumps.
 */
final class ValueMenu extends JMenu
{
	/** The room kept between the caption and the widest choice, and between the choice and the arrow, in px. */
	private static final int GAP = 12;
	private static final int ARROW_GAP = 6;
	/** The arrow's width when the look and feel gives no arrow icon, in px. */
	private static final int ARROW_FALLBACK = 8;

	private final String[] words;
	private String value;
	/** The room the widest choice takes in {@link #roomFont}, or -1 before it was measured. */
	private int room = -1;
	private Font roomFont;

	/**
	 * @param text the row's caption
	 * @param choices the choices the row may show, whose widest it makes room for; the first is shown until
	 *        {@link #setValue} says otherwise
	 */
	ValueMenu(String text, Object[] choices)
	{
		super(text);
		this.words = new String[choices.length];
		for (int i = 0; i < choices.length; i++)
		{
			words[i] = String.valueOf(choices[i]);
		}
		this.value = words.length > 0 ? words[0] : "";
	}

	/** Shows {@code choice} at the row's right; the row's text and width stay. */
	void setValue(String choice)
	{
		this.value = choice == null ? "" : choice;
		repaint();
	}

	/** The choice the row shows now. */
	String value()
	{
		return value;
	}

	/** The row's own size and the room for the widest choice, worked out the first time and again only if the font changes. */
	@Override
	public Dimension getPreferredSize()
	{
		final Dimension size = super.getPreferredSize();
		final Font font = getFont();
		if (font == null)
		{
			return size;
		}
		if (room < 0 || roomFont != font)
		{
			final FontMetrics metrics = getFontMetrics(font);
			int widest = 0;
			for (final String choice : words)
			{
				widest = Math.max(widest, metrics.stringWidth(choice));
			}
			room = GAP + widest;
			roomFont = font;
		}
		return new Dimension(size.width + room, size.height);
	}

	/** The row as the look and feel draws it, then the choice in light grey, right-aligned before the arrow. */
	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		final String choice = value();
		if (choice.isEmpty())
		{
			return;
		}
		final Graphics g2 = g.create();
		try
		{
			g2.setFont(getFont());
			g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			final FontMetrics metrics = g2.getFontMetrics();
			final Insets in = getInsets();
			final Icon arrow = UIManager.getIcon("Menu.arrowIcon");
			final int arrowWidth = arrow == null ? ARROW_FALLBACK : arrow.getIconWidth();
			final int x = getWidth() - in.right - arrowWidth - ARROW_GAP - metrics.stringWidth(choice);
			final int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
			g2.drawString(choice, x, y);
		}
		finally
		{
			g2.dispose();
		}
	}
}
