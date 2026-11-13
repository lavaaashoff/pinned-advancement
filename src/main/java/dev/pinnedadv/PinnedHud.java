package dev.pinnedadv;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Draws the pinned advancement in the style of the advancement-screen hover tooltip. */
public final class PinnedHud {
	/** Tint applied to the grey "unobtained" frame to make the pinned frame blue. */
	public static final int PINNED_TINT = 0xFF4D9BFF;

	private static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
	private static final int MARGIN = 4;
	private static final int BAR_H = 26;
	private static final int PAD = 5;
	private static final int PROGRESS_H = 2, BAR_GAP = 2;
	private static final int COLOR_BAR_BG = 0xFF3A3A3A, COLOR_BAR_FG = 0xFF55FF55;
	private static final int COLOR_TITLE = 0xFFFFFFFF;
	private static final int COLOR_DESC = 0xFF00FF00;   // vanilla hover default for descriptions
	private static final int COLOR_INFO = 0xFFAAAAAA;
	private static final int COLOR_MISSING = 0xFFFF5555; // AdvancementInfo "unfinished" red

	private PinnedHud() {}

	public static Identifier frameSprite(AdvancementType type) {
		return AdvancementWidgetType.UNOBTAINED.frameSprite(type);
	}

	/** In-game HUD entry point. */
	public static void renderInGame(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		Config cfg = Config.get();
		if (!cfg.visible || mc.player == null) return;
		if (mc.gui.screen() instanceof ConfigScreen || mc.gui.screen() instanceof PositionScreen) return; // they draw it themselves
		HudData data = currentData();
		if (data == null) return;
		render(g, data, cfg);
	}

	/** Data of the pinned advancement, or null if none / not known to this client / already done. */
	public static HudData currentData() {
		Identifier id = PinnedAdvancement.pinnedId();
		if (id == null) return null;
		ClientAdvancements adv = PinnedAdvancement.advancements();
		if (adv == null) return null;
		AdvancementHolder h = adv.get(id);
		if (h == null) return null;
		AdvancementProgress p = adv.progress().get(h);
		if (p != null && p.isDone()) return null;
		return HudData.of(h, p);
	}

	public static int[] position(GuiGraphicsExtractor g, Config cfg, int w, int h) {
		float s = cfg.scale;
		int sw = Math.round(w * s), sh = Math.round(h * s);
		int x = MARGIN + Math.round(cfg.posX * Math.max(0, g.guiWidth() - sw - 2 * MARGIN));
		int y = MARGIN + Math.round(cfg.posY * Math.max(0, g.guiHeight() - sh - 2 * MARGIN));
		return new int[]{x, y, sw, sh};
	}

	/** Renders at the configured position; returns {x, y, w, h} on screen (scaled). */
	public static int[] render(GuiGraphicsExtractor g, HudData data, Config cfg) {
		Font font = Minecraft.getInstance().font;
		Layout l = layout(font, data, cfg);
		int[] pos = position(g, cfg, cfg.width, l.height);
		g.pose().pushMatrix();
		g.pose().translate(pos[0], pos[1]);
		g.pose().scale(cfg.scale, cfg.scale);
		draw(g, font, data, cfg, l);
		g.pose().popMatrix();
		return pos;
	}

	// ---------------------------------------------------------------- layout

	private record Layout(List<FormattedCharSequence> title, List<FormattedCharSequence> desc,
						  Component info, int shownRows, int more, int height) {}

	private static Layout layout(Font font, HudData d, Config cfg) {
		int w = cfg.width;
		List<FormattedCharSequence> title = font.split(d.title(), w - 32 - 4);
		if (title.size() > 2) title = title.subList(0, 2);
		List<FormattedCharSequence> desc = font.split(d.description(), w - 6 - 2 * PAD);
		int remaining = d.total() - d.done();
		boolean multi = d.total() > 1;
		Component info = multi ? Component.translatable("pinnedadvancement.hud.remaining", remaining, d.total()) : null;
		int shown = 0, more = 0;
		if (multi && cfg.detailed) {
			shown = Math.min(d.rows().size(), cfg.maxLines);
			more = d.rows().size() - shown;
		}
		int y = BAR_H + PAD - 1;
		y += desc.size() * font.lineHeight;
		if (info != null) y += 2 + font.lineHeight;
		if (info != null && cfg.progressBar) y += BAR_GAP + PROGRESS_H;
		for (int i = 0; i < shown; i++) y += rowHeight(d.rows().get(i), font);
		if (more > 0) y += font.lineHeight;
		y += PAD - 1;
		return new Layout(title, desc, info, shown, more, Math.max(y, BAR_H + 4));
	}

	private static int rowHeight(HudData.Row r, Font font) {
		return r.icons().isEmpty() ? font.lineHeight + 1 : 17;
	}

	// ---------------------------------------------------------------- drawing

	private static void draw(GuiGraphicsExtractor g, Font font, HudData d, Config cfg, Layout l) {
		int w = cfg.width;
		int alpha = Math.round(cfg.opacity * 2.55f);
		int bg = (alpha << 24) | 0xFFFFFF;

		// body (dark box) under the title bar
		if (alpha > 0) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX, 3, BAR_H - 2, w - 6, l.height - (BAR_H - 2), bg);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, AdvancementWidgetType.UNOBTAINED.boxSprite(), 0, 0, w, BAR_H, bg);
		}
		// frame + icon, always opaque
		g.blitSprite(RenderPipelines.GUI_TEXTURED, frameSprite(d.type()), 0, 0, 26, 26, PINNED_TINT);
		g.fakeItem(d.icon(), 5, 5);

		int ty = (BAR_H - l.title().size() * font.lineHeight) / 2 + 1;
		for (FormattedCharSequence line : l.title()) {
			g.text(font, line, 32, ty, COLOR_TITLE, true);
			ty += font.lineHeight;
		}

		int x = 3 + PAD;
		int maxTextW = w - 6 - 2 * PAD;
		int y = BAR_H + PAD - 1;
		for (FormattedCharSequence line : l.desc()) {
			g.text(font, line, x, y, COLOR_DESC, true);
			y += font.lineHeight;
		}
		if (l.info() != null) {
			y += 2;
			g.text(font, l.info(), x, y, COLOR_INFO, true);
			if (cfg.progressBar) {
				int pct = d.total() <= 0 ? 0 : Math.round(100f * d.done() / d.total());
				String ps = pct + "%";
				g.text(font, ps, x + maxTextW - font.width(ps), y, COLOR_INFO, true);
				y += font.lineHeight + BAR_GAP - 1;
				g.fill(x, y, x + maxTextW, y + PROGRESS_H, COLOR_BAR_BG);
				g.fill(x, y, x + Math.round(maxTextW * (d.total() <= 0 ? 0f : (float) d.done() / d.total())), y + PROGRESS_H, COLOR_BAR_FG);
				y += PROGRESS_H + 1;
			} else {
				y += font.lineHeight;
			}
		}
		for (int i = 0; i < l.shownRows(); i++) {
			HudData.Row r = d.rows().get(i);
			int rh = rowHeight(r, font);
			int tx = x;
			for (ItemStack st : r.icons().subList(0, Math.min(4, r.icons().size()))) {
				g.fakeItem(st, tx, y);
				tx += 16;
			}
			if (!r.icons().isEmpty()) tx += 2;
			int textY = r.icons().isEmpty() ? y : y + 4;
			String s = r.text().getString();
			int avail = x + maxTextW - tx;
			if (font.width(s) > avail) s = font.plainSubstrByWidth(s, Math.max(0, avail - font.width("…"))) + "…";
			g.text(font, s, tx, textY, COLOR_MISSING, true);
			y += rh;
		}
		if (l.more() > 0) {
			g.text(font, Component.translatable("pinnedadvancement.hud.more", l.more()), x, y, COLOR_INFO, true);
		}
	}

	/** Size of the HUD on screen for the given data (scaled). */
	public static int[] size(HudData data, Config cfg) {
		Layout l = layout(Minecraft.getInstance().font, data, cfg);
		return new int[]{Math.round(cfg.width * cfg.scale), Math.round(l.height * cfg.scale)};
	}
}
