package dev.pinnedadv.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.pinnedadv.PinnedAdvancement;
import dev.pinnedadv.WideScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/** Stretches the advancement window over the screen and adds a search box with a result dropdown. */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenWideMixin extends Screen {
	@Shadow @Final private static Identifier WINDOW_LOCATION;
	@Shadow @Final private static Component TITLE;
	@Shadow @Final private HeaderAndFooterLayout layout;
	@Shadow @Final private ClientAdvancements advancements;
	@Shadow @Final private Map<AdvancementHolder, AdvancementTab> tabs;
	@Shadow private AdvancementTab selectedTab;
	@Shadow private int leftPos;
	@Shadow private int topPos;

	@Unique private static final int ROW = 18, MAX_ROWS = 8;
	@Unique private EditBox pinnedadv$search;
	@Unique private int pinnedadv$listX, pinnedadv$listY, pinnedadv$listW, pinnedadv$rows;

	protected AdvancementsScreenWideMixin(Component title) {
		super(title);
	}

	// ---------------------------------------------------------------- size

	@ModifyConstant(method = {"repositionElements", "mouseClicked"}, constant = @Constant(intValue = 252))
	private int pinnedadv$windowW(int v) {
		return WideScreen.enabled() ? WideScreen.windowW(width) : v;
	}

	@ModifyConstant(method = {"repositionElements", "mouseClicked"}, constant = @Constant(intValue = 140))
	private int pinnedadv$windowH(int v) {
		return WideScreen.enabled() ? WideScreen.windowH(height) : v;
	}

	@ModifyConstant(method = "extractInside", constant = @Constant(intValue = 234))
	private int pinnedadv$insideW(int v) {
		return WideScreen.enabled() ? WideScreen.innerW(width) : v;
	}

	@ModifyConstant(method = "extractInside", constant = @Constant(intValue = 113))
	private int pinnedadv$insideH(int v) {
		return WideScreen.enabled() ? WideScreen.innerH(height) : v;
	}

	@ModifyConstant(method = "extractInside", constant = @Constant(intValue = 117))
	private int pinnedadv$insideMidX(int v) {
		return WideScreen.enabled() ? WideScreen.innerW(width) / 2 : v;
	}

	@ModifyConstant(method = "extractInside", constant = @Constant(intValue = 56))
	private int pinnedadv$insideMidY(int v) {
		return WideScreen.enabled() ? WideScreen.innerH(height) / 2 : v;
	}

	/** The header title and the Done button would sit on top of the tabs: move them to the right edge. */
	@Inject(method = "repositionElements", at = @At("TAIL"))
	private void pinnedadv$moveHeaderFooter(CallbackInfo ci) {
		if (!WideScreen.enabled()) return;
		int right = leftPos + WideScreen.windowW(width);
		int bottom = topPos + WideScreen.windowH(height);
		// vanilla fills 8 tabs above, then up to 8 below (32 px apart); keep the button clear of those
		int below = Mth.clamp(tabs.size() - 8, 0, 8);
		int free = right - (leftPos + below * 32 + 4);
		layout.visitWidgets(w -> {
			if (w instanceof Button) {
				w.setWidth(Mth.clamp(free, 40, 200));
				w.setX(right - w.getWidth());
				w.setY(bottom + 3);
			} else if (w instanceof StringWidget) {
				w.setX(right - w.getWidth());
				w.setY(topPos - font.lineHeight - 4);
			}
		});
		pinnedadv$placeSearch();
	}

	@Inject(method = "extractWindow", at = @At("HEAD"), cancellable = true)
	private void pinnedadv$extractWindow(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
		if (!WideScreen.enabled()) return;
		int x = leftPos, y = topPos, w = WideScreen.windowW(width), h = WideScreen.windowH(height);
		pinnedadv$frame(g, x, y, w, h);
		if (tabs.size() > 1) {
			for (AdvancementTab tab : tabs.values()) tab.extractTab(g, x, y, mouseX, mouseY, tab == selectedTab);
			for (AdvancementTab tab : tabs.values()) tab.extractIcon(g, x, y);
		}

		int titleRight = x + w - 8;
		if (pinnedadv$search != null) {
			pinnedadv$placeSearch();
			titleRight = pinnedadv$search.getX() - 6;
			if (WideScreen.searching()) {
				int n = WideScreen.count(), i = WideScreen.currentIndex();
				String counter = n == 0 ? "0" : (i < 0 ? "" + n : (i + 1) + "/" + n);
				int cw = font.width(counter);
				g.text(font, counter, titleRight - cw, y + 6, n == 0 ? 0xFFB03030 : 0xFF404040, false);
				titleRight -= cw + 6;
			}
		}

		String title = (selectedTab != null ? selectedTab.getTitle() : TITLE).getString();
		int max = titleRight - (x + 8);
		if (font.width(title) > max) title = font.plainSubstrByWidth(title, Math.max(0, max - font.width("…"))) + "…";
		g.text(font, title, x + 8, y + 6, 0xFF404040, false);

		if (pinnedadv$search != null) pinnedadv$search.extractRenderState(g, mouseX, mouseY, 0);
		ci.cancel();
	}

	/** window.png stretched: corners as they are, edges tiled, the transparent middle skipped. */
	@Unique
	private static void pinnedadv$frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		final int l = 16, r = 16, t = 24, b = 16, midW = 252 - l - r, midH = 140 - t - b;
		pinnedadv$tile(g, x, y, l, t, 0, 0, l, t);
		pinnedadv$tile(g, x + w - r, y, r, t, 252 - r, 0, r, t);
		pinnedadv$tile(g, x, y + h - b, l, b, 0, 140 - b, l, b);
		pinnedadv$tile(g, x + w - r, y + h - b, r, b, 252 - r, 140 - b, r, b);
		pinnedadv$tile(g, x + l, y, w - l - r, t, l, 0, midW, t);
		pinnedadv$tile(g, x + l, y + h - b, w - l - r, b, l, 140 - b, midW, b);
		pinnedadv$tile(g, x, y + t, l, h - t - b, 0, t, l, midH);
		pinnedadv$tile(g, x + w - r, y + t, r, h - t - b, 252 - r, t, r, midH);
	}

	@Unique
	private static void pinnedadv$tile(GuiGraphicsExtractor g, int x, int y, int w, int h, int u, int v, int uw, int vh) {
		for (int dx = 0; dx < w; dx += uw) {
			for (int dy = 0; dy < h; dy += vh) {
				g.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION, x + dx, y + dy, u, v,
						Math.min(uw, w - dx), Math.min(vh, h - dy), 256, 256);
			}
		}
	}

	// ---------------------------------------------------------------- search box

	@Inject(method = "init", at = @At("TAIL"))
	private void pinnedadv$initSearch(CallbackInfo ci) {
		pinnedadv$search = null;
		WideScreen.dropdown = null;
		if (!WideScreen.enabled()) return;
		int w = Mth.clamp(WideScreen.windowW(width) / 3, 60, 120);
		EditBox s = new EditBox(font, 0, 0, w, 12, Component.translatable("pinnedadvancement.search.hint"));
		s.setHint(Component.translatable("pinnedadvancement.search.hint").withStyle(ChatFormatting.DARK_GRAY));
		s.setMaxLength(64);
		s.setValue(WideScreen.query());
		s.setResponder(q -> {
			WideScreen.setQuery(q, tabs.values());
			pinnedadv$updateTooltip();
		});
		addWidget(s);
		pinnedadv$search = s;
		WideScreen.setQuery(s.getValue(), tabs.values());
		pinnedadv$updateTooltip();
		pinnedadv$placeSearch();
	}

	/** The hint tooltip would cover the dropdown, so it is shown only while the box is empty. */
	@Unique
	private void pinnedadv$updateTooltip() {
		EditBox s = pinnedadv$search;
		s.setTooltip(s.getValue().isEmpty() ? Tooltip.create(Component.translatable("pinnedadvancement.search.tooltip")) : null);
	}

	@Unique
	private void pinnedadv$placeSearch() {
		EditBox s = pinnedadv$search;
		if (s == null) return;
		int w = WideScreen.windowW(width), h = WideScreen.windowH(height);
		s.setX(leftPos + w - 8 - s.getWidth());
		s.setY(topPos + 4);
		pinnedadv$listW = Math.max(s.getWidth(), Math.min(200, w - 16));
		pinnedadv$listX = s.getX() + s.getWidth() - pinnedadv$listW;
		pinnedadv$listY = s.getY() + s.getHeight() + 1;
		int fit = (topPos + h - 10 - pinnedadv$listY - 2) / ROW;
		pinnedadv$rows = Mth.clamp(Math.min(fit, WideScreen.count()), 1, MAX_ROWS);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void pinnedadv$tickSearch(CallbackInfo ci) {
		if (WideScreen.searching()) WideScreen.refresh(tabs.values());
	}

	// ---------------------------------------------------------------- dropdown

	@Unique
	private boolean pinnedadv$listOpen() {
		return pinnedadv$search != null && pinnedadv$search.isFocused() && WideScreen.searching();
	}

	@Unique
	private int pinnedadv$listHeight() {
		return Math.max(1, Math.min(pinnedadv$rows, WideScreen.count())) * ROW + 2;
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void pinnedadv$extractList(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (!pinnedadv$listOpen()) {
			WideScreen.dropdown = null;
			return;
		}
		pinnedadv$placeSearch();
		int x = pinnedadv$listX, y = pinnedadv$listY, w = pinnedadv$listW, h = pinnedadv$listHeight();
		int n = WideScreen.count(), rows = pinnedadv$rows;
		WideScreen.dropdown = new int[]{x, y, w, h};
		WideScreen.listScroll = Mth.clamp(WideScreen.listScroll, 0, Math.max(0, n - rows));

		g.nextStratum();
		g.fill(x, y, x + w, y + h, 0xFF101010);
		g.outline(x, y, w, h, 0xFFA0A0A0);
		if (n == 0) {
			g.text(font, Component.translatable("pinnedadvancement.search.none"), x + 6, y + 1 + (ROW - 8) / 2, 0xFF808080, false);
			return;
		}
		boolean bar = n > rows;
		int textW = w - 24 - (bar ? 4 : 0);
		int current = WideScreen.currentIndex();
		for (int i = 0; i < rows && WideScreen.listScroll + i < n; i++) {
			int idx = WideScreen.listScroll + i;
			AdvancementWidget aw = WideScreen.get(idx);
			int ry = y + 1 + i * ROW;
			boolean hover = mouseX >= x + 1 && mouseX < x + w - 1 && mouseY >= ry && mouseY < ry + ROW;
			if (idx == current) g.fill(x + 1, ry, x + w - 1, ry + ROW, 0xFF34346A);
			else if (hover) g.fill(x + 1, ry, x + w - 1, ry + ROW, 0x40FFFFFF);
			g.fakeItem(((AdvancementWidgetAccessor) aw).pinnedadv$getIcon(), x + 3, ry + 1);
			String t = aw.getDisplay().title().getString();
			if (font.width(t) > textW) t = font.plainSubstrByWidth(t, textW - font.width("…")) + "…";
			AdvancementProgress p = PinnedAdvancement.progressOf(aw.getAdvancement());
			g.text(font, t, x + 22, ry + (ROW - 8) / 2, p != null && p.isDone() ? 0xFF909090 : 0xFFFFFFFF, true);
		}
		if (bar) {
			int trackH = h - 2, thumbH = Math.max(6, trackH * rows / n);
			int thumbY = y + 1 + (trackH - thumbH) * WideScreen.listScroll / (n - rows);
			g.fill(x + w - 4, y + 1, x + w - 1, y + h - 1, 0xFF303030);
			g.fill(x + w - 4, thumbY, x + w - 1, thumbY + thumbH, 0xFFA0A0A0);
		}
	}

	/** No advancement tooltip under the open dropdown. */
	@Inject(method = "extractTooltips", at = @At("HEAD"), cancellable = true)
	private void pinnedadv$noTooltipUnderList(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
		if (pinnedadv$listOpen() && WideScreen.overDropdown(mouseX, mouseY)) ci.cancel();
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void pinnedadv$clickSearch(MouseButtonEvent e, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		EditBox s = pinnedadv$search;
		if (s == null) return;
		if (pinnedadv$listOpen() && WideScreen.overDropdown(e.x(), e.y())) {
			int idx = WideScreen.listScroll + (int) ((e.y() - pinnedadv$listY - 1) / ROW);
			if (idx >= 0 && idx < WideScreen.count()) {
				pinnedadv$show(WideScreen.select(idx));
				pinnedadv$closeList();
			}
			cir.setReturnValue(true);
			return;
		}
		if (s.isFocused() && !s.isMouseOver(e.x(), e.y())) pinnedadv$closeList();
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void pinnedadv$scrollList(double mx, double my, double sx, double sy, CallbackInfoReturnable<Boolean> cir) {
		if (pinnedadv$listOpen() && WideScreen.overDropdown(mx, my)) {
			WideScreen.listScroll = Mth.clamp(WideScreen.listScroll - (int) Math.signum(sy), 0,
					Math.max(0, WideScreen.count() - pinnedadv$rows));
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void pinnedadv$searchKeys(KeyEvent e, CallbackInfoReturnable<Boolean> cir) {
		EditBox s = pinnedadv$search;
		if (s == null) return;
		int key = e.key();
		boolean enter = key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER;
		if (!s.isFocused()) {
			if (enter && WideScreen.searching()) { // without the box focused Enter cycles through the matches
				pinnedadv$show(WideScreen.step(e.hasShiftDown() ? -1 : 1));
				cir.setReturnValue(true);
			}
			return;
		}
		if (key == InputConstants.KEY_ESCAPE) {
			pinnedadv$closeList();
		} else if (key == InputConstants.KEY_DOWN || key == InputConstants.KEY_UP) {
			pinnedadv$show(WideScreen.step(key == InputConstants.KEY_DOWN ? 1 : -1));
		} else if (enter) {
			if (WideScreen.currentIndex() < 0) pinnedadv$show(WideScreen.step(1));
			pinnedadv$closeList();
		} else {
			s.keyPressed(e);
		}
		cir.setReturnValue(true); // also keeps the advancements key (L) from closing the screen while typing
	}

	@Unique
	private void pinnedadv$closeList() {
		pinnedadv$search.setFocused(false);
		setFocused(null);
		WideScreen.dropdown = null;
	}

	/** Switches to the tab of the advancement, scrolls it into the middle and into view in the dropdown. */
	@Unique
	private void pinnedadv$show(AdvancementWidget target) {
		if (target == null) return;
		int idx = WideScreen.currentIndex();
		if (idx < WideScreen.listScroll) WideScreen.listScroll = idx;
		else if (idx >= WideScreen.listScroll + pinnedadv$rows) WideScreen.listScroll = idx - pinnedadv$rows + 1;
		for (AdvancementTab tab : tabs.values()) {
			AdvancementTabAccessor a = (AdvancementTabAccessor) tab;
			if (!a.pinnedadv$getWidgets().containsValue(target)) continue;
			if (tab != selectedTab) advancements.setSelectedTab(tab.getRootAdvancement(), true);
			int iw = WideScreen.innerW(width), ih = WideScreen.innerH(height);
			// while the dropdown is open, centre in the part of the tree it does not cover
			int visibleW = pinnedadv$listOpen() ? Math.max(60, pinnedadv$listX - (leftPos + 9) - 4) : iw;
			double sx = pinnedadv$center(iw, target.getX() + 16, a.pinnedadv$getMinX(), a.pinnedadv$getMaxX());
			if (visibleW < iw && a.pinnedadv$getMaxX() - a.pinnedadv$getMinX() > iw) {
				sx = Mth.clamp(visibleW / 2.0 - (target.getX() + 16), -(a.pinnedadv$getMaxX() - iw), -a.pinnedadv$getMinX());
			}
			a.pinnedadv$setScrollX(sx);
			a.pinnedadv$setScrollY(pinnedadv$center(ih, target.getY() + 13, a.pinnedadv$getMinY(), a.pinnedadv$getMaxY()));
			a.pinnedadv$setCentered(true);
			return;
		}
	}

	/** Scroll that puts pos in the middle, clamped like AdvancementTab.scroll; small trees stay centred. */
	@Unique
	private static double pinnedadv$center(int size, int pos, int min, int max) {
		if (max - min <= size) return size / 2.0 - (max + min) / 2.0;
		return Mth.clamp(size / 2.0 - pos, -(max - size), -min);
	}
}
