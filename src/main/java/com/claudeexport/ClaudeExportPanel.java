package com.claudeexport;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The small side panel: an "Export now" button and a status line.
 */
class ClaudeExportPanel extends PluginPanel
{
	private final JLabel status = new JLabel("No export yet");

	ClaudeExportPanel(Runnable onExportNow)
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel title = new JLabel("Claude Export");
		title.setFont(FontManager.getRunescapeBoldFont());

		JButton exportButton = new JButton("Export now");
		exportButton.addActionListener(e -> onExportNow.run());

		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JLabel path = new JLabel("<html>Saved to .runelite\\plugin-data\\claude-export\\state.json</html>");
		path.setFont(FontManager.getRunescapeSmallFont());
		path.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);

		JPanel content = new JPanel(new GridLayout(0, 1, 0, 8));
		content.add(title);
		content.add(exportButton);
		content.add(status);
		content.add(path);
		add(content, BorderLayout.NORTH);
	}

	/** Safe to call from any thread; Swing changes must happen on the UI thread. */
	void setStatus(String text)
	{
		SwingUtilities.invokeLater(() -> status.setText(text));
	}
}
