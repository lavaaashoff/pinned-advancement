package dev.pinnedadv;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a criterion name into a readable name + icon.
 * On a server the client only knows criterion names (not their item conditions), and datapacks such as
 * BACAP name them loosely ("prismarine_bricks_stairs", "smite4_book", "farmer_snow", "coast_armor_trim"),
 * so besides the exact lookups AdvancementInfo does, this tries a few forgiving matches.
 */
public final class Criteria {
	public record Resolved(Component name, ItemStack icon) {}

	private static final String[] KEY_PREFIXES = {"item", "block", "entity", "effect", "biome", "enchantment", "trim_pattern", "trim_material", "color"};
	private static final String[] SUFFIXES = {"_from_honeycomb", "_book", "_mainhand", "_offhand", "_melee", "_arrow", "_directly", "_direct", "_item", "_block"};
	private static final Pattern LEVEL = Pattern.compile("^(.*?)_?(\\d+)$");

	private static final Map<String, Resolved> cache = new HashMap<>();
	private static Object cacheKey;
	private static Map<String, Resolved> fuzzyIndex;
	private static Map<String, Resolved> advIndex = Map.of();

	private Criteria() {}

	public static Resolved resolve(String criterion) {
		ClientAdvancements adv = PinnedAdvancement.advancements();
		Object key = List.of(Language.getInstance(), adv == null ? "" : adv, adv == null ? 0 : adv.tree().nodes().size());
		if (!key.equals(cacheKey)) {
			cache.clear();
			advIndex = buildAdvIndex(adv);
			cacheKey = key;
		}
		return cache.computeIfAbsent(criterion, Criteria::compute);
	}

	private static Resolved compute(String criterion) {
		String ns = "minecraft", path = criterion;
		int colon = criterion.indexOf(':');
		if (colon > 0) {
			ns = criterion.substring(0, colon);
			path = criterion.substring(colon + 1);
		}
		if (path.startsWith("textures/entity/")) { // AdvancementInfo: cat variants
			path = path.substring("textures/entity/".length());
			int dot = path.indexOf('.');
			if (dot > 0) path = path.substring(0, dot);
		}
		Resolved r = single(ns, path);
		if (r != null) return r;
		r = twoParts(ns, path);
		if (r != null) return r;
		String nice = path.replace('_', ' ').replace('/', ' ');
		if (!nice.isEmpty()) nice = Character.toUpperCase(nice.charAt(0)) + nice.substring(1);
		return new Resolved(Component.literal(nice), ItemStack.EMPTY);
	}

	/** Everything except splitting into two names. */
	private static Resolved single(String ns, String path) {
		Resolved r = exact(ns, path);
		if (r != null) return r;
		r = advIndex.get(path);
		if (r != null) return r;
		for (String suf : SUFFIXES) {
			if (path.endsWith(suf) && path.length() > suf.length()) {
				r = single(ns, path.substring(0, path.length() - suf.length()));
				if (r != null) return r;
			}
		}
		// enchantment levels: smite4, power_1 ...
		Matcher m = LEVEL.matcher(path);
		if (m.matches() && !m.group(1).isEmpty()) {
			Resolved base = single(ns, m.group(1));
			if (base != null) {
				String lv = "enchantment.level." + m.group(2);
				Component name = Language.getInstance().has(lv)
						? Component.empty().append(base.name()).append(" ").append(Component.translatable(lv))
						: Component.empty().append(base.name()).append(" " + m.group(2));
				return new Resolved(name, base.icon().isEmpty() ? new ItemStack(Items.ENCHANTED_BOOK) : base.icon());
			}
		}
		r = special(ns, path);
		if (r != null) return r;
		return fuzzy(path);
	}

	private static Resolved exact(String ns, String path) {
		Language lang = Language.getInstance();
		String dotted = path.replace('/', '.');
		for (String prefix : KEY_PREFIXES) {
			String k = prefix + "." + ns + "." + dotted;
			if (lang.has(k)) return new Resolved(Component.translatable(k), iconById(ns, path, prefix));
		}
		if (lang.has("container." + dotted)) return new Resolved(Component.translatable("container." + dotted), ItemStack.EMPTY);
		return null;
	}

	private static Resolved special(String ns, String path) {
		Language lang = Language.getInstance();
		String nous = path.replace("_", "");
		for (String k : new String[]{"entity." + ns + ".villager." + path, "entity." + ns + ".villager." + nous}) {
			if (lang.has(k)) return new Resolved(Component.translatable(k), new ItemStack(Items.EMERALD));
		}
		if (path.endsWith("_armor_trim")) {
			String pat = path.substring(0, path.length() - "_armor_trim".length());
			Item tmpl = item(ns, pat + "_armor_trim_smithing_template");
			if (lang.has("trim_pattern." + ns + "." + pat))
				return new Resolved(Component.translatable("trim_pattern." + ns + "." + pat), tmpl == null ? ItemStack.EMPTY : new ItemStack(tmpl));
		}
		// tipped_arrow_fire_resistance, potion_swiftness, ...
		String[] w = path.split("_");
		for (int i = 1; i < w.length; i++) {
			String head = String.join("_", java.util.Arrays.copyOfRange(w, 0, i));
			String tail = String.join("_", java.util.Arrays.copyOfRange(w, i, w.length));
			for (String k : new String[]{"item." + ns + "." + head + ".effect." + tail, "item." + ns + "." + head + "." + tail}) {
				if (lang.has(k)) {
					Item it = item(ns, head);
					return new Resolved(Component.translatable(k), it == null ? ItemStack.EMPTY : new ItemStack(it));
				}
			}
		}
		return null;
	}

	/** "farmer_snow" -> "Farmer · Snow", when both halves are known names. */
	private static Resolved twoParts(String ns, String path) {
		String[] w = path.split("_");
		for (int i = 1; i < w.length; i++) {
			String head = String.join("_", java.util.Arrays.copyOfRange(w, 0, i));
			String tail = String.join("_", java.util.Arrays.copyOfRange(w, i, w.length));
			Resolved a = single(ns, head), b = single(ns, tail);
			if (a != null && b != null) {
				return new Resolved(Component.empty().append(a.name()).append(" · ").append(b.name()),
						!a.icon().isEmpty() ? a.icon() : b.icon());
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- fuzzy registry match

	private static String norm(String path) {
		StringBuilder sb = new StringBuilder();
		for (String word : path.split("[_/.]")) {
			if (word.length() > 3 && word.endsWith("s")) word = word.substring(0, word.length() - 1);
			sb.append(word);
		}
		return sb.toString();
	}

	private static Resolved fuzzy(String path) {
		if (fuzzyIndex == null) fuzzyIndex = buildFuzzyIndex();
		String n = norm(path);
		Resolved r = fuzzyIndex.get(n);
		if (r != null) return r;
		if (n.length() < 5) return null;
		Resolved only = null;
		for (Map.Entry<String, Resolved> e : fuzzyIndex.entrySet()) {
			if (e.getKey().startsWith(n)) {
				if (only != null && only != e.getValue()) return null; // ambiguous
				only = e.getValue();
			}
		}
		return only;
	}

	private static Map<String, Resolved> buildFuzzyIndex() {
		Map<String, Resolved> m = new HashMap<>();
		for (Item it : BuiltInRegistries.ITEM) {
			Identifier id = BuiltInRegistries.ITEM.getKey(it);
			ItemStack st = new ItemStack(it);
			if (st.isEmpty()) continue;
			m.putIfAbsent(norm(id.getPath()), new Resolved(st.getHoverName(), st));
		}
		for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(t);
			ItemStack egg = SpawnEggItem.byId(t).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
			m.putIfAbsent(norm(id.getPath()), new Resolved(t.getDescription(), egg));
		}
		return m;
	}

	private static Map<String, Resolved> buildAdvIndex(ClientAdvancements adv) {
		Map<String, Resolved> m = new HashMap<>();
		if (adv == null) return m;
		for (AdvancementNode n : adv.tree().nodes()) {
			var d = n.holder().value().display();
			if (d.isEmpty()) continue;
			Resolved r = new Resolved(d.get().title(), d.get().icon().create());
			String full = n.holder().id().getPath();
			m.putIfAbsent(full, r);
			int slash = full.lastIndexOf('/');
			if (slash >= 0) m.putIfAbsent(full.substring(slash + 1), r);
		}
		return m;
	}

	// ---------------------------------------------------------------- icons

	private static Item item(String ns, String path) {
		Identifier id = Identifier.tryParse(ns + ":" + path);
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return null;
		Item it = BuiltInRegistries.ITEM.getValue(id);
		return it == Items.AIR ? null : it;
	}

	private static ItemStack iconById(String ns, String path, String kind) {
		Item it = item(ns, path);
		if (it != null) return new ItemStack(it);
		if (kind.equals("entity")) {
			Identifier id = Identifier.tryParse(ns + ":" + path);
			if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id))
				return SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.getValue(id)).map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
		}
		if (kind.equals("enchantment")) return new ItemStack(Items.ENCHANTED_BOOK);
		return ItemStack.EMPTY;
	}

	/** Every criterion row: for search in the full list screen. */
	public static List<String> splitWords(String s) {
		return new ArrayList<>(List.of(s.toLowerCase(java.util.Locale.ROOT).split("\\s+")));
	}
}
