package dev.pinnedadv;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory-style grid of every requirement that is still missing for one advancement.
 * Rebuilt every frame from the live progress, so finished requirements disappear on their own.
 * Opened with right click on an advancement in the advancement menu.
 */
public final class MissingScreen extends Screen {
	private static final int SLOT = 18, PAD = 7, HEADER = 30, FOOTER = 24;
	private static final int TEXT = 0xFFFFFFFF, COUNT = 0xFFFFFF55;

	private final Screen parent;
	private final Identifier id;
	private int page;
	private int cols, rows, left, top, winW, winH;
	private Button prev, next;

	public MissingScreen(Screen parent, AdvancementHolder holder) {
		super(Component.translatable("pinnedadvancement.missing.title"));
		this.parent = parent;
		this.id = holder.id();
	}

	private HudData data() {
		ClientAdvancements adv = PinnedAdvancement.advancements();
		if (adv == null) return null;
		AdvancementHolder h = adv.get(id);
		if (h == null) return null;
		AdvancementProgress p = adv.progress().get(h);
		return HudData.of(h, p);
	}

	@Override
	protected void init() {
		Config c = Config.get();
		// configured size, shrunk if the screen is too small for it
		cols = Math.max(Config.MIN_COLS, Math.min(c.gridCols, (width - 20 - 2 * PAD) / SLOT));
		rows = Math.max(Config.MIN_ROWS, Math.min(c.gridRows, (height - 20 - HEADER - FOOTER - PAD) / SLOT));
		winW = cols * SLOT + 2 * PAD;
		winH = HEADER + rows * SLOT + FOOTER + PAD;
		left = (width - winW) / 2;
		top = (height - winH) / 2;
		int by = top + HEADER + rows * SLOT + 4;
		prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> page--).bounds(left + PAD, by, 20, 20).build());
		next = addRenderableWidget(Button.builder(Component.literal(">"), b -> page++).bounds(left + winW - PAD - 20, by, 20, 20).build());
	}

	private int perPage() {
		return cols * rows;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		HudData d = data();
		List<HudData.Row> list = d == null ? List.of() : itemRows(d);
		int pages = Math.max(1, (list.size() + perPage() - 1) / perPage());
		page = Math.clamp(page, 0, pages - 1);
		prev.active = page > 0;
		next.active = page < pages - 1;

		// window: vanilla container look
		panel(g, left, top, winW, winH);
		super.extractRenderState(g, mouseX, mouseY, delta);

		// header: icon + title on the first line, counter + progress bar on the second
		boolean titleHover = false;
		if (d != null) {
			g.fakeItem(d.icon(), left + PAD, top + 6);
			int tx = left + PAD + 20, right = left + winW - PAD;
			String title = d.title().getString();
			int tw = right - tx;
			boolean cut = font.width(title) > tw;
			String shown = cut ? font.plainSubstrByWidth(title, Math.max(0, tw - font.width("…"))) + "…" : title;
			g.text(font, shown, tx, top + 5, TEXT, true);
			titleHover = cut && mouseX >= tx && mouseX < right && mouseY >= top + 4 && mouseY < top + 14;

			boolean bar = Config.get().progressBar && d.total() > 0;
			String count = d.done() + "/" + d.total();
			if (bar) {
				String withPct = count + " · " + Math.round(100f * d.done() / d.total()) + "%";
				if (font.width(withPct) <= tw / 2 + 10) count = withPct;
			}
			int cw = font.width(count);
			g.text(font, count, right - cw, top + 17, COUNT, true);
			if (bar) {
				int bw = right - cw - 4 - tx, by = top + 20;
				if (bw > 6) {
					g.fill(tx, by, tx + bw, by + 3, 0xFF5A5A5A);
					g.fill(tx, by, tx + Math.round(bw * (float) d.done() / d.total()), by + 3, 0xFF3FBF3F);
				}
			}
		}

		// slots
		int gx = left + PAD, gy = top + HEADER;
		HudData.Row hovered = null;
		int start = page * perPage();
		long tick = System.currentTimeMillis() / 1000;
		for (int i = 0; i < perPage(); i++) {
			int sx = gx + (i % cols) * SLOT, sy = gy + (i / cols) * SLOT;
			slot(g, sx, sy);
			int idx = start + i;
			if (idx >= list.size()) continue;
			HudData.Row r = list.get(idx);
			ItemStack st = r.icons().get((int) (tick % r.icons().size()));
			g.fakeItem(st, sx + 1, sy + 1);
			if (mouseX >= sx && mouseX < sx + SLOT && mouseY >= sy && mouseY < sy + SLOT) {
				g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x80FFFFFF);
				hovered = r;
			}
		}

		// footer: page counter or "all done"
		int fy = top + HEADER + rows * SLOT + 10;
		Component footer = list.isEmpty()
				? Component.translatable("pinnedadvancement.missing.none")
				: Component.translatable("pinnedadvancement.missing.page", page + 1, pages, list.size());
		int footerSpace = winW - 2 * PAD - 2 * 22;
		if (font.width(footer) > footerSpace && !list.isEmpty()) footer = Component.literal((page + 1) + "/" + pages);
		if (font.width(footer) <= footerSpace || list.isEmpty()) g.centeredText(font, footer, left + winW / 2, fy, TEXT);

		if (titleHover && d != null) {
			g.setTooltipForNextFrame(font, d.title(), mouseX, mouseY);
			return;
		}

		if (hovered != null) {
			List<Component> lines = new ArrayList<>();
			if (hovered.options().size() > 1) {
				lines.add(Component.translatable("pinnedadvancement.missing.oneof"));
				for (Component c : hovered.options()) lines.add(Component.literal("• ").append(c));
			} else {
				lines.add(hovered.text());
			}
			g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
		}
	}

	/** Only requirements that have an item: the rest (biomes, structures...) are not shown in the grid. */
	static List<HudData.Row> itemRows(HudData d) {
		List<HudData.Row> out = new ArrayList<>();
		for (HudData.Row r : d.rows()) if (!r.icons().isEmpty()) out.add(r);
		return out;
	}

	/** The grid is offered only for advancements with several requirements, some of them items. */
	public static boolean hasItems(AdvancementHolder h) {
		ClientAdvancements adv = PinnedAdvancement.advancements();
		if (adv == null || h.value().requirements().size() < 2) return false;
		return !itemRows(HudData.of(h, adv.progress().get(h))).isEmpty();
	}

	@Override
	public boolean mouseScrolled(double x, double y, double h, double v) {
		if (v > 0) page--;
		else if (v < 0) page++;
		return true;
	}

	private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
		g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
		g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
		g.fill(x + 2, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
		g.fill(x + w - 3, y + 2, x + w - 1, y + h - 1, 0xFF555555);
	}

	private static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + SLOT, y + SLOT, 0xFF8B8B8B);
		g.fill(x, y, x + SLOT - 1, y + 1, 0xFF373737);
		g.fill(x, y, x + 1, y + SLOT - 1, 0xFF373737);
		g.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
		g.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
