package com.claudeexport;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Filepath;

@Slf4j
@PluginDescriptor(
	name = "Claude Export",
	description = "Exports your account state to a local JSON file",
	tags = {"export", "json", "stats", "bank"},
	// Folder name under .runelite/plugin-data/ that this plugin may write to
	internalName = "claude-export"
)
public class ClaudeExportPlugin extends Plugin
{
	static final String STATE_FILE = "state.json";
	private static final long MIN_WRITE_INTERVAL_MS = 5000;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClaudeExportConfig config;

	@Inject
	private Gson gson;

	// RuneLite's shared background thread. Runs one task at a time, so writes never overlap.
	// Shared with other plugins, so we must never shut it down.
	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private StateCollector collector;

	private Gson prettyGson;
	// Read from the background thread when updating the status line, so volatile
	private volatile ClaudeExportPanel panel;
	private NavigationButton navButton;

	// Only touched on the client thread (event handlers), so no locking needed.
	private boolean dirty;
	private long lastExportMs;
	private boolean bankCacheDirty;

	// Set from the client thread and from the background load, so marked volatile.
	// The object itself is never changed after creation; it's only swapped for a new one.
	private volatile BankCache bankCache;
	private volatile StorageCache storageCache;
	private boolean storageCacheDirty;
	// Potion storage is read on the tick after the bank opens (scripts can't run inside an item event)
	private boolean potionsPending;

	@Override
	protected void startUp() throws Exception
	{
		// serializeNulls so "lastSeen": null and "slayer": null appear in the file instead of vanishing
		prettyGson = gson.newBuilder().setPrettyPrinting().serializeNulls().create();

		panel = new ClaudeExportPanel(this::requestManualExport);
		navButton = NavigationButton.builder()
			.tooltip("Claude Export")
			.icon(createIcon())
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		bankCache = null;
		bankCacheDirty = false;
		storageCache = null;
		storageCacheDirty = false;
		potionsPending = false;
		executor.execute(this::loadBankCache);
		executor.execute(this::loadStorageCache);

		// If the plugin is turned on while already logged in, export right away
		dirty = client.getGameState() == GameState.LOGGED_IN;
		lastExportMs = 0;
		log.info("Claude Export started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		dirty = false;
		log.info("Claude Export stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		dirty = true;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int varp = event.getVarpId();
		// Quest points (a quest was completed) or slayer task/count changed
		if (varp == VarPlayerID.QP || varp == VarPlayerID.SLAYER_COUNT || varp == VarPlayerID.SLAYER_TARGET)
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int id = event.getContainerId();
		if (id == InventoryID.BANK)
		{
			if (!config.exportBank())
			{
				return;
			}
			try
			{
				// The bank is only readable while open, so remember what's in it now
				bankCache = collector.captureBank(event.getItemContainer());
				bankCacheDirty = true;
				dirty = true;
			}
			catch (RuntimeException e)
			{
				log.warn("Claude Export: could not read the bank", e);
			}
			potionsPending = config.exportStorage();
		}
		else if (id == InventoryID.SEED_VAULT)
		{
			if (!config.exportStorage())
			{
				return;
			}
			try
			{
				storageCache = collector.captureSeedVault(event.getItemContainer(), storageCache);
				storageCacheDirty = true;
				dirty = true;
			}
			catch (RuntimeException e)
			{
				log.warn("Claude Export: could not read the seed vault", e);
			}
		}
		else if (id == InventoryID.INV || id == InventoryID.WORN)
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			// Export on the next tick, skipping the 5-second wait
			dirty = true;
			lastExportMs = 0;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ClaudeExportConfig.GROUP.equals(event.getGroup()))
		{
			// Re-export so the file reflects the new settings. Hop to the client thread,
			// since this event can arrive on the UI thread and 'dirty' belongs to the client thread.
			clientThread.invokeLater(() -> dirty = true);
		}
	}

	/**
	 * Runs every game tick (0.6s). This is the debounce: changes only set {@code dirty},
	 * and the export happens here at most once every 5 seconds.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (potionsPending)
		{
			potionsPending = false;
			try
			{
				storageCache = collector.capturePotions(storageCache);
				storageCacheDirty = true;
				dirty = true;
			}
			catch (RuntimeException e)
			{
				log.warn("Claude Export: could not read potion storage", e);
			}
		}
		if (dirty && System.currentTimeMillis() - lastExportMs >= MIN_WRITE_INTERVAL_MS)
		{
			exportNow();
		}
	}

	/** Called by the panel's "Export now" button (on the Swing UI thread). */
	private void requestManualExport()
	{
		// Hop over to the client thread, where game state can be read safely
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN)
			{
				setPanelStatus("Log in first");
				return;
			}
			exportNow();
		});
	}

	/** Must run on the client thread. Reads game state, then hands the file write to the background thread. */
	private void exportNow()
	{
		// Wait until the player has fully loaded after login
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null)
		{
			return;
		}

		dirty = false;
		lastExportMs = System.currentTimeMillis();

		ExportState state;
		try
		{
			state = collector.collect(player, bankCache, storageCache);
		}
		catch (RuntimeException e)
		{
			// A game-data read failed (e.g. after a game update). Skip this export; the next change retries.
			log.warn("Claude Export: could not read game state", e);
			setPanelStatus("Export failed, see client log");
			return;
		}
		BankCache cacheToSave = bankCacheDirty ? bankCache : null;
		bankCacheDirty = false;
		StorageCache storageToSave = storageCacheDirty ? storageCache : null;
		storageCacheDirty = false;

		executor.execute(() -> writeFiles(state, cacheToSave, storageToSave));
	}

	/** Runs on the background executor. Errors are logged, never thrown, so the client can't crash. */
	private void writeFiles(ExportState state, BankCache cacheToSave, StorageCache storageToSave)
	{
		try
		{
			Filepath dir = getPluginDirectory();
			StateWriter.writeAtomically(dir, STATE_FILE, prettyGson.toJson(state));
			if (cacheToSave != null)
			{
				StateWriter.writeAtomically(dir, BankCache.FILE, gson.toJson(cacheToSave));
			}
			if (storageToSave != null)
			{
				StateWriter.writeAtomically(dir, StorageCache.FILE, gson.toJson(storageToSave));
			}
			log.debug("Wrote {}", STATE_FILE);
			setPanelStatus("Last export: " + LocalTime.now().format(TIME));
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Claude Export: could not write {}", STATE_FILE, e);
			setPanelStatus("Export failed, see client log");
		}
	}

	/** Runs on the background executor at startup: restores seed vault + potion storage from the last session. */
	private void loadStorageCache()
	{
		try
		{
			Filepath file = getPluginDirectory().joinSegment(StorageCache.FILE);
			if (!file.exists())
			{
				return;
			}
			try (Reader reader = file.openReader())
			{
				StorageCache loaded = gson.fromJson(reader, StorageCache.class);
				// Only use it if nothing newer was captured while we were loading
				if (loaded != null && storageCache == null)
				{
					storageCache = loaded;
				}
			}
		}
		catch (IOException | RuntimeException e)
		{
			// A damaged file just means starting empty; it refills next time you open the vault or bank
			log.warn("Claude Export: could not read {}", StorageCache.FILE, e);
		}
	}

	/** Runs on the background executor at startup: restores the bank from the last session. */
	private void loadBankCache()
	{
		try
		{
			Filepath file = getPluginDirectory().joinSegment(BankCache.FILE);
			if (!file.exists())
			{
				return;
			}
			try (Reader reader = file.openReader())
			{
				BankCache loaded = gson.fromJson(reader, BankCache.class);
				// Only use it if the bank wasn't already opened while we were loading
				if (loaded != null && bankCache == null)
				{
					bankCache = loaded;
				}
			}
		}
		catch (IOException | RuntimeException e)
		{
			// Includes a damaged/edited file (Gson's JsonParseException is a RuntimeException).
			// We just start with no cached bank; it refills the next time the bank is opened.
			log.warn("Claude Export: could not read {}", BankCache.FILE, e);
		}
	}

	private void setPanelStatus(String text)
	{
		ClaudeExportPanel p = panel;
		if (p != null)
		{
			p.setStatus(text);
		}
	}

	/** Draws the sidebar icon in code, so the plugin needs no image file. */
	private static BufferedImage createIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(217, 119, 87));
		g.fillRoundRect(0, 0, 16, 16, 5, 5);
		g.setColor(Color.WHITE);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		g.drawString("C", 4, 13);
		g.dispose();
		return img;
	}

	@Provides
	ClaudeExportConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClaudeExportConfig.class);
	}
}
