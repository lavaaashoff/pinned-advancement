package dev.pinnedadv;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Settings + pinned advancement per world/server, stored in config/pinnedadvancement.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pinnedadvancement.json");

	public static final int MIN_WIDTH = 140, MAX_WIDTH = 360;
	public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2.0f;
	public static final int MIN_LINES = 1, MAX_LINES = 20;
	public static final int MIN_COLS = 3, MAX_COLS = 18, MIN_ROWS = 1, MAX_ROWS = 10;

	/** Window width in GUI pixels (before scale). */
	public int width = 200;
	public float scale = 1.0f;
	/** Background opacity 0..100. */
	public int opacity = 85;
	/** Max requirement rows. */
	public int maxLines = 6;
	/** true = show requirement list, false = only title/description/progress. */
	public boolean detailed = true;
	/** Thin percentage progress bar under the "remaining" line. */
	public boolean progressBar = true;
	/** Stretch the advancement screen over the whole window and add a search box. */
	public boolean expandWindow = true;
	/** Size of the missing-requirements grid. */
	public int gridCols = 9, gridRows = 6;
	/** Position of the window inside the free screen space, 0..1. */
	public float posX = 0f, posY = 0.3f;
	/** HUD visibility (toggled by hotkey). */
	public boolean visible = true;
	/** Show/hide hotkey: key code (-1 = none) plus required modifiers. */
	public int toggleKey = -1;
	public boolean toggleCtrl, toggleShift, toggleAlt;
	/** world/server key -> advancement id. */
	public Map<String, String> pins = new HashMap<>();

	private static Config instance;

	public static Config get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		Config c = null;
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				c = GSON.fromJson(r, Config.class);
			} catch (Exception e) {
				PinnedAdvancement.LOGGER.warn("Failed to read {}", FILE, e);
			}
		}
		if (c == null) c = new Config();
		c.clamp();
		instance = c;
	}

	public void clamp() {
		width = Math.clamp(width, MIN_WIDTH, MAX_WIDTH);
		scale = Math.clamp(scale, MIN_SCALE, MAX_SCALE);
		opacity = Math.clamp(opacity, 0, 100);
		maxLines = Math.clamp(maxLines, MIN_LINES, MAX_LINES);
		gridCols = Math.clamp(gridCols, MIN_COLS, MAX_COLS);
		gridRows = Math.clamp(gridRows, MIN_ROWS, MAX_ROWS);
		posX = Math.clamp(posX, 0f, 1f);
		posY = Math.clamp(posY, 0f, 1f);
		if (pins == null) pins = new HashMap<>();
	}

	public void save() {
		clamp();
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(this, w);
			}
		} catch (Exception e) {
			PinnedAdvancement.LOGGER.warn("Failed to write {}", FILE, e);
		}
	}
}
