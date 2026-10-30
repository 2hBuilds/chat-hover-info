package com.clanhub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Font;
import javax.swing.JMenu;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import org.junit.Test;

/** The theme rows of the settings menu: the choice on a second line under the caption (the user's pick D). */
public class ValueMenuTest
{
	private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
	private static final String LONG = "The Lighthouse Keeper's Cottage in the Storm";

	private static ValueMenu row(String... choices)
	{
		final ValueMenu row = new ValueMenu("Card theme", choices);
		row.setFont(FONT);
		return row;
	}

	@Test
	public void theTextStaysTheCaption()
	{
		final ValueMenu row = row("Classic", "Snow Village, Dawn Rose");
		row.setValue("Snow Village, Dawn Rose");
		assertEquals("Card theme", row.getText());
		assertEquals("Snow Village, Dawn Rose", row.value());
	}

	@Test
	public void theChoiceIsDrawnUnderTheCaption()
	{
		final ValueMenu row = row("Classic", "Snow Village, Dawn Rose");
		row.setValue("Snow Village, Dawn Rose");
		final Object view = row.getClientProperty(BasicHTML.propertyKey);
		assertTrue("the row draws an HTML view", view instanceof View);
		final JMenu oneLine = new JMenu("Card theme");
		oneLine.setFont(FONT);
		assertTrue("two lines are taller than one", row.getPreferredSize().height > oneLine.getPreferredSize().height);
	}

	@Test
	public void aShortChoiceIsWhole()
	{
		final ValueMenu row = row("Classic");
		assertEquals("Classic", row.shownValue());
	}

	@Test
	public void aLongChoiceIsCutToFit()
	{
		final ValueMenu row = row("Classic", LONG);
		row.setValue(LONG);
		final String shown = row.shownValue();
		assertTrue("cut with an ellipsis: " + shown, shown.endsWith("…") && shown.length() < LONG.length());
		assertTrue("within the width", row.getFontMetrics(FONT).stringWidth(shown) <= ValueMenu.CHOICE_WIDTH);
		assertEquals("the whole name is kept", LONG, row.value());
	}

	@Test
	public void everyPickGivesTheSameWidth()
	{
		final ValueMenu row = row("Classic", "Snow Village, Dawn Rose", LONG);
		final int classic = row.getPreferredSize().width;
		row.setValue("Snow Village, Dawn Rose");
		final int middle = row.getPreferredSize().width;
		row.setValue(LONG);
		final int longest = row.getPreferredSize().width;
		assertEquals(classic, middle);
		assertEquals(classic, longest);
	}

	@Test
	public void theNamesHtmlIsEscaped()
	{
		final ValueMenu row = row("Fish & <Chips>");
		assertEquals("Fish & <Chips>", row.shownValue());
		assertEquals("Card theme", row.getText());
	}
}
