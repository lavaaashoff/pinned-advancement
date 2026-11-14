package dev.pinnedadv.mixin;

import dev.pinnedadv.WideScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The tree area of a tab follows the stretched window (234×113 in vanilla). */
@Mixin(AdvancementTab.class)
public abstract class AdvancementTabWideMixin {
	@Shadow @Final private AdvancementsScreen screen;

	@ModifyConstant(method = {"extractContents", "extractTooltips", "tick", "scroll", "canScrollHorizontally"},
			constant = @Constant(intValue = 234))
	private int pinnedadv$innerW(int v) {
		return WideScreen.enabled() ? WideScreen.innerW(screen.width) : v;
	}

	@ModifyConstant(method = {"extractContents", "extractTooltips", "tick", "scroll", "canScrollVertically"},
			constant = @Constant(intValue = 113))
	private int pinnedadv$innerH(int v) {
		return WideScreen.enabled() ? WideScreen.innerH(screen.height) : v;
	}

	@ModifyConstant(method = "extractContents", constant = @Constant(intValue = 117))
	private int pinnedadv$centerX(int v) {
		return WideScreen.enabled() ? WideScreen.innerW(screen.width) / 2 : v;
	}

	@ModifyConstant(method = "extractContents", constant = @Constant(intValue = 56))
	private int pinnedadv$centerY(int v) {
		return WideScreen.enabled() ? WideScreen.innerH(screen.height) / 2 : v;
	}

	/** Last background tile column / row (vanilla loops -1..15 and -1..8). */
	@ModifyConstant(method = "extractContents", constant = @Constant(intValue = 15))
	private int pinnedadv$tilesX(int v) {
		return WideScreen.enabled() ? (WideScreen.innerW(screen.width) + 15) / 16 : v;
	}

	@ModifyConstant(method = "extractContents", constant = @Constant(intValue = 8))
	private int pinnedadv$tilesY(int v) {
		return WideScreen.enabled() ? (WideScreen.innerH(screen.height) + 15) / 16 : v;
	}
}
