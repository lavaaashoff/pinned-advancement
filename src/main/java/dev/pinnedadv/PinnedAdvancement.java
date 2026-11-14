package dev.pinnedadv;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import dev.pinnedadv.mixin.AdvancementTabAccessor;
import dev.pinnedadv.mixin.AdvancementsScreenMixin;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PinnedAdvancement implements ClientModInitializer {
	public static final String MOD_ID = "pinnedadvancement";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static boolean hudErrorLogged;

	/** "Ctrl + Shift + P", or null when no hotkey is set. */
	public static net.minecraft.network.chat.Component comboName(Config c) {
		if (c.toggleKey < 0) return null;
		net.minecraft.network.chat.MutableComponent m = net.minecraft.network.chat.Component.empty();
		if (c.toggleCtrl) m.append("Ctrl + ");
		if (c.toggleShift) m.append("Shift + ");
		if (c.toggleAlt) m.append("Alt + ");
		return m.append(KeyNames.name(c.toggleKey));
	}

	/** Called for every key event; toggles the HUD on a press of the configured combination in game. */
	public static boolean onKey(int action, net.minecraft.client.input.KeyEvent e) {
		Config c = Config.get();
		if (c.toggleKey < 0 || e.key() != c.toggleKey) return false;
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.gui.screen() != null) return false;
		if (e.hasControlDown() != c.toggleCtrl || e.hasShiftDown() != c.toggleShift || e.hasAltDown() != c.toggleAlt) return false;
		if (action == 1) { // press; repeats and the release are swallowed too
			c.visible = !c.visible;
			c.save();
		}
		return true;
	}

	@Override
	public void onInitializeClient() {
		Config.load();
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "pinned"), (g, delta) -> {
			try {
				PinnedHud.renderInGame(g);
			} catch (Throwable t) {
				if (!hudErrorLogged) LOGGER.error("Pinned HUD failed", t);
				hudErrorLogged = true;
			}
		});


		// Shift + left click on an advancement pins / unpins it. Fabric's screen event fires before the
		// screen's own mouseClicked, so other mods hooking that method (e.g. AdvancementInfo) don't swallow it.
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof AdvancementsScreen)) return;
			ScreenMouseEvents.allowMouseClick(screen).register((s, e) -> {
				boolean pin = e.button() == InputConstants.MOUSE_BUTTON_LEFT && e.hasShiftDown();
				boolean list = e.button() == InputConstants.MOUSE_BUTTON_RIGHT;
				if (!pin && !list) return true;
				if (WideScreen.overDropdown(e.x(), e.y())) return true;
				AdvancementTab tab = ((AdvancementsScreenMixin) s).pinnedadv$getSelectedTab();
				if (tab == null) return true;
				AdvancementWidget hovered = ((AdvancementTabAccessor) tab).pinnedadv$getHovered();
				if (hovered == null) return true;
				if (list) {
					if (!MissingScreen.hasItems(hovered.getAdvancement())) return true;
					client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					client.gui.setScreen(new MissingScreen(s, hovered.getAdvancement()));
					return false;
				}
				togglePin(hovered.getAdvancement());
				client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return false;
			});
		});

	}

	// ---------------------------------------------------------------- pin state

	/** Identifies the current world / server so each one keeps its own pin. Null when not in a world. */
	public static String contextKey() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) return null;
		IntegratedServer sp = mc.getSingleplayerServer();
		if (sp != null) {
			try {
				return "world:" + sp.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
			} catch (Exception e) {
				return "world:" + sp.getWorldData().getLevelName();
			}
		}
		ServerData sd = mc.getCurrentServer();
		if (sd != null) return "server:" + sd.ip.toLowerCase(java.util.Locale.ROOT);
		return "unknown";
	}

	public static Identifier pinnedId() {
		String key = contextKey();
		if (key == null) return null;
		String v = Config.get().pins.get(key);
		return v == null ? null : Identifier.tryParse(v);
	}

	public static boolean isPinned(Identifier id) {
		return id != null && id.equals(pinnedId());
	}

	public static void setPinned(Identifier id) {
		String key = contextKey();
		if (key == null) return;
		Config c = Config.get();
		if (id == null) c.pins.remove(key);
		else c.pins.put(key, id.toString());
		c.save();
	}

	/** Pin, or unpin when it is already pinned. Completed advancements are not pinned. */
	public static void togglePin(AdvancementHolder holder) {
		if (isPinned(holder.id())) {
			setPinned(null);
			return;
		}
		AdvancementProgress p = progressOf(holder);
		if (p != null && p.isDone()) return;
		setPinned(holder.id());
	}

	public static ClientAdvancements advancements() {
		ClientPacketListener c = Minecraft.getInstance().getConnection();
		return c == null ? null : c.getAdvancements();
	}

	public static AdvancementProgress progressOf(AdvancementHolder holder) {
		ClientAdvancements a = advancements();
		return a == null ? null : a.progress().get(holder);
	}

	/** Called after every advancement packet: drop the pin once the advancement is completed. */
	public static void onAdvancementsUpdated() {
		Identifier id = pinnedId();
		if (id == null) return;
		ClientAdvancements a = advancements();
		if (a == null) return;
		AdvancementHolder h = a.get(id);
		if (h == null) return;
		AdvancementProgress p = a.progress().get(h);
		if (p != null && p.isDone()) setPinned(null);
	}
}
