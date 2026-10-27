package com.clanhub;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 * The small window a card shows in while the game is stretched or scaled: a window with no decorations owned by the
 * window the game is in, so it moves and minimizes with it and never sits over other applications, never taking the
 * keyboard, drawing the card at device pixels with whole pixels per card pixel and nearest neighbour, so the pixel font
 * stays as sharp as the screen can show it. Made on first use. The calls come from the client thread and hop to the event
 * thread, the latest call winning.
 */
final class SharpCardWindow implements SharpCards
{
	/** A change that is not made yet, or null; the latest call replaces the one before it. */
	private final AtomicReference<Runnable> latest = new AtomicReference<>();
	/** The component whose window owns this one, asked for when the window is first made. */
	private final Supplier<? extends Component> game;
	/** The window, made on first use; the event thread only. */
	private JWindow window;
	/** What the window draws; the event thread only. */
	private Surface surface;

	SharpCardWindow(Supplier<? extends Component> game)
	{
		this.game = game;
	}

	/**
	 * The scale of the graphics configuration of {@code component}, device pixels per AWT pixel: 1 when it is null, has
	 * none, or reports none.
	 */
	static double deviceScaleOf(Component component)
	{
		final GraphicsConfiguration configuration = component == null ? null : component.getGraphicsConfiguration();
		if (configuration == null)
		{
			return 1;
		}
		final double scale = configuration.getDefaultTransform().getScaleX();
		return scale > 0 ? scale : 1;
	}

	/**
	 * Device pixels per card pixel when the game is drawn {@code ratio} times its size on a screen of {@code deviceScale}:
	 * the product rounded to a whole number, at least 1, so the pixel font is never smeared.
	 */
	static int scaleFor(double ratio, double deviceScale)
	{
		return Math.max(1, (int) Math.round(ratio * deviceScale));
	}

	/** Whether the card should leave the game's picture for the window: the game is shown at other than 1:1 on the screen. */
	static boolean needsWindow(double ratio, double deviceScale)
	{
		return Math.abs(ratio * deviceScale - 1) > 0.01;
	}

	/**
	 * The size in AWT pixels of a card of {@code card} size drawn {@code scale} device pixels to a card pixel on a screen
	 * of {@code deviceScale}.
	 */
	static Dimension sizeOf(Dimension card, int scale, double deviceScale)
	{
		return new Dimension((int) Math.round(card.width * scale / deviceScale),
			(int) Math.round(card.height * scale / deviceScale));
	}

	@Override
	public void show(BufferedImage card, int scale, Rectangle onScreen)
	{
		post(() -> apply(card, scale, onScreen));
	}

	@Override
	public void hide()
	{
		post(() ->
		{
			if (window != null)
			{
				window.setVisible(false);
			}
		});
	}

	/** Takes the window away for good: called when the plugin shuts down. */
	void dispose()
	{
		post(() ->
		{
			if (window != null)
			{
				window.dispose();
				window = null;
				surface = null;
			}
		});
	}

	/** Leaves {@code change} for the event thread; a change still waiting there is replaced by it. */
	private void post(Runnable change)
	{
		if (latest.getAndSet(change) == null)
		{
			SwingUtilities.invokeLater(() ->
			{
				final Runnable next = latest.getAndSet(null);
				if (next != null)
				{
					next.run();
				}
			});
		}
	}

	/** Puts the card in the window at the rectangle and shows it. The event thread only. */
	private void apply(BufferedImage card, int scale, Rectangle onScreen)
	{
		if (window == null)
		{
			final Component component = game.get();
			final Window owner = component == null ? null : SwingUtilities.getWindowAncestor(component);
			window = owner == null ? new JWindow() : new JWindow(owner);
			window.setFocusableWindowState(false);
			surface = new Surface();
			window.getContentPane().add(surface);
		}
		surface.set(card, scale);
		window.setBounds(onScreen);
		window.setVisible(true);
		surface.repaint();
	}

	/** A component that draws a card at device pixels, {@code scale} of them to a card pixel, nearest neighbour. */
	static final class Surface extends JComponent
	{
		private BufferedImage card;
		private int scale = 1;

		/** Sets the card to draw and the device pixels each of its pixels takes. */
		void set(BufferedImage card, int scale)
		{
			this.card = card;
			this.scale = Math.max(1, scale);
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			final BufferedImage drawn = card;
			if (drawn == null)
			{
				return;
			}
			final Graphics2D g = (Graphics2D) graphics.create();
			try
			{
				// Undo the screen's own scale, so one unit here is one device pixel.
				g.scale(1 / g.getTransform().getScaleX(), 1 / g.getTransform().getScaleY());
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g.drawImage(drawn, 0, 0, drawn.getWidth() * scale, drawn.getHeight() * scale, null);
			}
			finally
			{
				g.dispose();
			}
		}
	}
}
