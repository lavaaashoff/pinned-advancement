package dev.pinnedadv.mixin;

import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AdvancementsScreen.class)
public interface AdvancementsScreenMixin {
	@Accessor("selectedTab")
	AdvancementTab pinnedadv$getSelectedTab();
}
