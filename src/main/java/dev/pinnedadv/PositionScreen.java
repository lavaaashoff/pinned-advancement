package dev.pinnedadv;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag the HUD window to any place on the screen; snaps to edges and centre lines. */
public final class PositionScreen extends Screen {
	private static final int MARGIN = 4;
	private static final float SNAP = 0.03f;

	private final Screen parent;
	private boolean dragging;
	private double grabX, grabY;
	private int[] lastRect = {0, 0, 0, 0};

	public PositionScreen(Screen parent) {
		super(Component.translatable("pinnedadvancement.position.title"));
		this.parent = parent;
	}

	private static HudData data() {
		HudData d = PinnedHud.currentData();
		return d != null ? d : HudData.sample();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		// guide lines (edges / centre) for snapping
		int c = 0x40FFFFFF;
		g.fill(width / 2, 0, width / 2 + 1, height, c);
		g.fill(0, height / 2, width, height / 2 + 1, c);

		lastRect = PinnedHud.render(g, data(), Config.get());
		int col = dragging ? 0xFFFFFF55 : 0xAAFFFFFF;
		g.outline(lastRect[0] - 1, lastRect[1] - 1, lastRect[2] + 2, lastRect[3] + 2, col);

		Component help = Component.translatable("pinnedadvancement.position.help");
		int tw = font.width(help);
		int hy = lastRect[1] < 30 && lastRect[0] < width / 2 + tw / 2 && lastRect[0] + lastRect[2] > width / 2 - tw / 2 ? height - 20 : 8;
		g.fill(width / 2 - tw / 2 - 4, hy - 3, width / 2 + tw / 2 + 4, hy + 11, 0x90000000);
		g.centeredText(font, help, width / 2, hy, 0xFFFFFFFF);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (minecraft.level == null) super.extractBackground(g, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		if (e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
			double mx = e.x(), my = e.y();
			int[] r = lastRect;
			if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
				dragging = true;
				grabX = mx - r[0];
				grabY = my - r[1];
				return true;
			}
		}
		return super.mouseClicked(e, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (dragging) {
			Config cfg = Config.get();
			int[] size = PinnedHud.size(data(), cfg);
			float freeW = Math.max(1, width - size[0] - 2 * MARGIN);
			float freeH = Math.max(1, height - size[1] - 2 * MARGIN);
			cfg.posX = snap((float) ((e.x() - grabX - MARGIN) / freeW));
			cfg.posY = snap((float) ((e.y() - grabY - MARGIN) / freeH));
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (dragging) {
			dragging = false;
			Config.get().save();
			return true;
		}
		return super.mouseReleased(e);
	}

	private static float snap(float v) {
		v = Math.clamp(v, 0f, 1f);
		for (float t : new float[]{0f, 0.5f, 1f}) if (Math.abs(v - t) < SNAP) return t;
		return v;
	}

	@Override
	public void onClose() {
		Config.get().save();
		minecraft.gui.setScreen(parent);
	}
}
