package dev.pinnedadv.mixin;

import dev.pinnedadv.WideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Tabs on the right / bottom edge follow the stretched window. */
@Mixin(targets = "net.minecraft.client.gui.screens.advancements.AdvancementTabType")
public abstract class AdvancementTabTypeWideMixin {
	@ModifyConstant(method = "getX", constant = @Constant(intValue = 248))
	private int pinnedadv$rightX(int v) {
		Screen s = Minecraft.getInstance().gui.screen();
		return WideScreen.enabled() && s instanceof AdvancementsScreen ? WideScreen.windowW(s.width) - 4 : v;
	}

	@ModifyConstant(method = "getY", constant = @Constant(intValue = 136))
	private int pinnedadv$bottomY(int v) {
		Screen s = Minecraft.getInstance().gui.screen();
		return WideScreen.enabled() && s instanceof AdvancementsScreen ? WideScreen.windowH(s.height) - 4 : v;
	}
}
