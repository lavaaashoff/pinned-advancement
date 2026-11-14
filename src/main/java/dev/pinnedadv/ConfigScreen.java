package dev.pinnedadv;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Settings screen (opened from Mod Menu). The HUD is previewed live at its real position. */
public final class ConfigScreen extends Screen {
	private final Screen parent;
	private Button keyButton;
	private boolean listening;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("pinnedadvancement.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		Config c = Config.get();
		int w = 150, gap = 6, step = 24;
		int lx = width / 2 - w - gap / 2, rx = width / 2 + gap / 2;
		int top = Math.max(24, height / 2 - 80), y = top;

		// left column: sizes
		addRenderableWidget(new Slider(lx, y, w, (c.width - Config.MIN_WIDTH) / (double) (Config.MAX_WIDTH - Config.MIN_WIDTH),
				v -> c.width = (int) Math.round(Mth.lerp(v, Config.MIN_WIDTH, Config.MAX_WIDTH) / 5.0) * 5,
				v -> Component.translatable("pinnedadvancement.config.width", c.width)));
		y += step;
		addRenderableWidget(new Slider(lx, y, w, (c.scale - Config.MIN_SCALE) / (Config.MAX_SCALE - Config.MIN_SCALE),
				v -> c.scale = Math.round(Mth.lerp((float) v, Config.MIN_SCALE, Config.MAX_SCALE) * 20f) / 20f,
				v -> Component.translatable("pinnedadvancement.config.scale", String.format(java.util.Locale.ROOT, "%.2f×", c.scale))));
		y += step;
		addRenderableWidget(new Slider(lx, y, w, c.opacity / 100.0,
				v -> c.opacity = (int) Math.round(v * 100),
				v -> Component.translatable("pinnedadvancement.config.opacity", c.opacity)));
		y += step;
		addRenderableWidget(new Slider(lx, y, w, (c.maxLines - Config.MIN_LINES) / (double) (Config.MAX_LINES - Config.MIN_LINES),
				v -> c.maxLines = (int) Math.round(Mth.lerp(v, Config.MIN_LINES, Config.MAX_LINES)),
				v -> Component.translatable("pinnedadvancement.config.lines", c.maxLines)));
		y += step;
		addRenderableWidget(new Slider(lx, y, w, (c.gridCols - Config.MIN_COLS) / (double) (Config.MAX_COLS - Config.MIN_COLS),
				v -> c.gridCols = (int) Math.round(Mth.lerp(v, Config.MIN_COLS, Config.MAX_COLS)),
				v -> Component.translatable("pinnedadvancement.config.gridcols", c.gridCols)));
		y += step;
		addRenderableWidget(new Slider(lx, y, w, (c.gridRows - Config.MIN_ROWS) / (double) (Config.MAX_ROWS - Config.MIN_ROWS),
				v -> c.gridRows = (int) Math.round(Mth.lerp(v, Config.MIN_ROWS, Config.MAX_ROWS)),
				v -> Component.translatable("pinnedadvancement.config.gridrows", c.gridRows)));

		// right column: toggles and actions
		y = top;
		addRenderableWidget(CycleButton.booleanBuilder(
						Component.translatable("pinnedadvancement.config.details.full"),
						Component.translatable("pinnedadvancement.config.details.compact"), c.detailed)
				.create(rx, y, w, 20, Component.translatable("pinnedadvancement.config.details"), (b, v) -> c.detailed = v));
		y += step;
		addRenderableWidget(CycleButton.onOffBuilder(c.progressBar)
				.create(rx, y, w, 20, Component.translatable("pinnedadvancement.config.progressbar"), (b, v) -> c.progressBar = v));
		y += step;
		CycleButton<Boolean> wide = addRenderableWidget(CycleButton.onOffBuilder(c.expandWindow && !WideScreen.ADVANCEMENT_INFO)
				.create(rx, y, w, 20, Component.translatable("pinnedadvancement.config.wide"), (b, v) -> c.expandWindow = v));
		wide.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
				WideScreen.ADVANCEMENT_INFO ? "pinnedadvancement.config.wide.advinfo" : "pinnedadvancement.config.wide.tooltip")));
		wide.active = !WideScreen.ADVANCEMENT_INFO;
		y += step;
		keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
			listening = true;
			b.setMessage(keyLabel());
		}).bounds(rx, y, w, 20)
				.tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("pinnedadvancement.config.key.tooltip"))).build());
		y += step;
		addRenderableWidget(Button.builder(Component.translatable("pinnedadvancement.config.position"),
				b -> minecraft.gui.setScreen(new PositionScreen(this))).bounds(rx, y, w, 20).build());
		y += step;
		addRenderableWidget(Button.builder(Component.translatable("pinnedadvancement.config.reset"), b -> {
			Config d = new Config();
			c.width = d.width; c.scale = d.scale; c.opacity = d.opacity; c.maxLines = d.maxLines;
			c.detailed = d.detailed; c.progressBar = d.progressBar; c.posX = d.posX; c.posY = d.posY;
			c.gridCols = d.gridCols; c.gridRows = d.gridRows; c.expandWindow = d.expandWindow;
			rebuildWidgets();
		}).bounds(rx, y, w, 20).build());
		y += step;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(rx, y, w, 20).build());
		titleY = top - 16;
	}

	private int titleY;

	private Component keyLabel() {
		Component name = PinnedAdvancement.comboName(Config.get());
		if (name == null) name = Component.translatable("pinnedadvancement.config.key.none");
		if (listening) name = Component.translatable("pinnedadvancement.config.key.press").withStyle(ChatFormatting.YELLOW);
		return Component.translatable("pinnedadvancement.config.key", name);
	}

	private static boolean isModifier(int key) {
		return key == InputConstants.KEY_LCONTROL || key == InputConstants.KEY_RCONTROL
				|| key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT
				|| key == InputConstants.KEY_LALT || key == InputConstants.KEY_RALT;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (listening) {
			Config c = Config.get();
			if (isModifier(e.key())) return true; // wait for the main key of the combination
			if (e.key() == InputConstants.KEY_ESCAPE) {
				c.toggleKey = -1;
				c.toggleCtrl = c.toggleShift = c.toggleAlt = false;
			} else {
				c.toggleKey = e.key();
				c.toggleCtrl = e.hasControlDown();
				c.toggleShift = e.hasShiftDown();
				c.toggleAlt = e.hasAltDown();
			}
			c.save();
			listening = false;
			keyButton.setMessage(keyLabel());
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.centeredText(font, title, width / 2, titleY, 0xFFFFFFFF);
		HudData d = PinnedHud.currentData();
		PinnedHud.render(g, d != null ? d : HudData.sample(), Config.get());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// in a world: keep it visible (no blur) so the preview is seen over the game
		if (minecraft.level == null) super.extractBackground(g, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		Config.get().save();
		minecraft.gui.setScreen(parent);
	}

	private static final class Slider extends AbstractSliderButton {
		private final DoubleConsumer apply;
		private final DoubleFunction<Component> label;

		Slider(int x, int y, int w, double value, DoubleConsumer apply, DoubleFunction<Component> label) {
			super(x, y, w, 20, Component.empty(), Mth.clamp(value, 0, 1));
			this.apply = apply;
			this.label = label;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(label.apply(value));
		}

		@Override
		protected void applyValue() {
			apply.accept(value);
		}
	}
}
