package dev.pinnedadv;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What the HUD shows. Requirement rows are built the same way AdvancementInfo does it
 * (criterion names translated as item / block / entity / effect / biome), but grouped by the
 * advancement's requirement groups, so "any one of" groups take one row.
 */
public record HudData(ItemStack icon, AdvancementType type, Component title, Component description,
					  int total, int done, List<Row> rows) {

	public record Row(List<ItemStack> icons, Component text, List<Component> options) {}

	private static final String[] KEY_PREFIXES = {"item", "block", "entity", "effect", "biome", "enchantment", "trim_pattern", "trim_material"};

	// tiny cache so translations are not looked up every frame
	private static Object cacheKey;
	private static HudData cacheValue;

	public static HudData of(AdvancementHolder holder, AdvancementProgress progress) {
		Object key = List.of(holder, progress == null ? "" : progress.toString(), Language.getInstance());
		if (key.equals(cacheKey)) return cacheValue;
		HudData d = build(holder, progress);
		cacheKey = key;
		cacheValue = d;
		return d;
	}

	private static HudData build(AdvancementHolder holder, AdvancementProgress progress) {
		Optional<DisplayInfo> disp = holder.value().display();
		ItemStack icon = disp.map(d -> d.icon().create()).orElse(new ItemStack(Items.BOOK));
		AdvancementType type = disp.map(DisplayInfo::type).orElse(AdvancementType.TASK);
		Component title = disp.map(DisplayInfo::title).orElse(Component.literal(holder.id().toString()));
		Component desc = disp.map(DisplayInfo::description).orElse(Component.empty());

		List<List<String>> groups = holder.value().requirements().requirements();
		int done = 0;
		List<Row> rows = new ArrayList<>();
		for (List<String> group : groups) {
			boolean groupDone = false;
			for (String c : group) {
				CriterionProgress cp = progress == null ? null : progress.getCriterion(c);
				if (cp != null && cp.isDone()) { groupDone = true; break; }
			}
			if (groupDone) { done++; continue; }
			if (group.size() == 1) {
				String c = group.getFirst();
				ItemStack st = iconFor(c);
				rows.add(new Row(st.isEmpty() ? List.of() : List.of(st), label(c), List.of(label(c))));
			} else {
				List<ItemStack> icons = new ArrayList<>();
				List<Component> options = new ArrayList<>();
				MutableComponent names = Component.empty();
				for (int i = 0; i < group.size(); i++) {
					String c = group.get(i);
					ItemStack st = iconFor(c);
					if (!st.isEmpty()) icons.add(st);
					options.add(label(c));
					if (i > 0) names.append(", ");
					names.append(label(c));
				}
				rows.add(new Row(icons, Component.translatable("pinnedadvancement.hud.oneof", names), options));
			}
		}
		return new HudData(icon, type, title, desc, groups.size(), done, rows);
	}

	public static Component label(String criterion) {
		return Criteria.resolve(criterion).name();
	}

	public static ItemStack iconFor(String criterion) {
		return Criteria.resolve(criterion).icon();
	}

	/** Shown in the settings screens when nothing is pinned yet. */
	public static HudData sample() {
		List<Row> rows = new ArrayList<>();
		for (var item : List.of(Items.CARROT, Items.BAKED_POTATO, Items.COOKED_BEEF, Items.PUMPKIN_PIE, Items.GOLDEN_APPLE,
				Items.SWEET_BERRIES, Items.HONEY_BOTTLE, Items.DRIED_KELP)) {
			ItemStack st = new ItemStack(item);
			rows.add(new Row(List.of(st), st.getHoverName(), List.of(st.getHoverName())));
		}
		return new HudData(new ItemStack(Items.APPLE), AdvancementType.CHALLENGE,
				Component.translatable("pinnedadvancement.sample.title"),
				Component.translatable("pinnedadvancement.sample.desc"),
				12, 4, rows);
	}
}
