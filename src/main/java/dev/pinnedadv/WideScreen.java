package dev.pinnedadv;

import dev.pinnedadv.mixin.AdvancementTabAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Full-screen advancement window (like AdvancementInfo) and the state of its search box. */
public final class WideScreen {
	public static final boolean ADVANCEMENT_INFO = FabricLoader.getInstance().isModLoaded("advancementinfo");
	public static final int MARGIN = 30;
	public static final int VANILLA_W = 252, VANILLA_H = 140;

	private WideScreen() {}

	public static boolean enabled() {
		return !ADVANCEMENT_INFO && Config.get().expandWindow;
	}

	public static int windowW(int screenW) { return Math.max(VANILLA_W, screenW - 2 * MARGIN); }
	public static int windowH(int screenH) { return Math.max(VANILLA_H, screenH - 2 * MARGIN); }
	public static int innerW(int screenW) { return windowW(screenW) - 18; }
	public static int innerH(int screenH) { return windowH(screenH) - 27; }

	// ---------------------------------------------------------------- search

	private static String query = "";
	private static final Set<AdvancementWidget> matches = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final List<AdvancementWidget> ordered = new ArrayList<>();
	private static AdvancementWidget current;

	public static String query() { return query; }
	public static boolean searching() { return enabled() && !query.isEmpty(); }
	public static boolean matches(AdvancementWidget w) { return matches.contains(w); }
	public static int count() { return ordered.size(); }
	public static int currentIndex() { return ordered.indexOf(current); }
	public static AdvancementWidget get(int i) { return ordered.get(i); }

	public static AdvancementWidget select(int i) {
		current = ordered.get(i);
		return current;
	}

	/** First visible row of the result dropdown. */
	public static int listScroll;
	/** Screen area of the open dropdown (x, y, w, h), or null; clicks there must not reach the tree. */
	public static int[] dropdown;

	public static boolean overDropdown(double mx, double my) {
		int[] d = dropdown;
		return d != null && mx >= d[0] && mx < d[0] + d[2] && my >= d[1] && my < d[1] + d[3];
	}

	public static void setQuery(String q, Collection<AdvancementTab> tabs) {
		query = q.trim().toLowerCase(Locale.ROOT);
		current = null;
		listScroll = 0;
		refresh(tabs);
	}

	/** Recomputes the matches; widgets are recreated when the screen is re-initialised. */
	public static void refresh(Collection<AdvancementTab> tabs) {
		matches.clear();
		ordered.clear();
		if (query.isEmpty()) return;
		for (AdvancementTab tab : tabs) {
			List<AdvancementWidget> inTab = new ArrayList<>();
			for (AdvancementWidget w : ((AdvancementTabAccessor) tab).pinnedadv$getWidgets().values()) {
				if (isVisible(w) && textMatches(w.getDisplay())) inTab.add(w);
			}
			inTab.sort(Comparator.comparingInt(AdvancementWidget::getX).thenComparingInt(AdvancementWidget::getY));
			ordered.addAll(inTab);
		}
		matches.addAll(ordered);
		if (current != null && !matches.contains(current)) current = null;
	}

	/** Next (dir = 1) or previous (dir = -1) match, wrapping around. */
	public static AdvancementWidget step(int dir) {
		if (ordered.isEmpty()) return null;
		int i = ordered.indexOf(current);
		i = i < 0 ? (dir > 0 ? 0 : ordered.size() - 1) : Math.floorMod(i + dir, ordered.size());
		current = ordered.get(i);
		return current;
	}

	private static boolean textMatches(DisplayInfo d) {
		return d.title().getString().toLowerCase(Locale.ROOT).contains(query)
				|| d.description().getString().toLowerCase(Locale.ROOT).contains(query);
	}

	/** Same rule as AdvancementWidget's renderer: hidden advancements are drawn only once completed. */
	private static boolean isVisible(AdvancementWidget w) {
		if (!w.getDisplay().hidden()) return true;
		AdvancementProgress p = PinnedAdvancement.progressOf(w.getAdvancement());
		return p != null && p.isDone();
	}
}
