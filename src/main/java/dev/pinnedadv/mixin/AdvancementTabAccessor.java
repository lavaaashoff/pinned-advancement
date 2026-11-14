package dev.pinnedadv.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(AdvancementTab.class)
public interface AdvancementTabAccessor {
	@Accessor("hovered")
	AdvancementWidget pinnedadv$getHovered();

	@Accessor("widgets")
	Map<AdvancementHolder, AdvancementWidget> pinnedadv$getWidgets();

	@Accessor("scrollX")
	void pinnedadv$setScrollX(double v);

	@Accessor("scrollY")
	void pinnedadv$setScrollY(double v);

	@Accessor("centered")
	void pinnedadv$setCentered(boolean v);

	@Accessor("minX")
	int pinnedadv$getMinX();

	@Accessor("maxX")
	int pinnedadv$getMaxX();

	@Accessor("minY")
	int pinnedadv$getMinY();

	@Accessor("maxY")
	int pinnedadv$getMaxY();
}
