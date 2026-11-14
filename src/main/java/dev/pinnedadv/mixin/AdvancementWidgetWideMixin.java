package dev.pinnedadv.mixin;

import dev.pinnedadv.WideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hover tooltip flips at the new window height; search dims everything that does not match. */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetWideMixin {
	@Shadow @Final private int x;
	@Shadow @Final private int y;

	@ModifyConstant(method = "extractHover", constant = @Constant(intValue = 113))
	private int pinnedadv$innerH(int v) {
		Screen s = Minecraft.getInstance().gui.screen();
		return WideScreen.enabled() && s instanceof AdvancementsScreen ? WideScreen.innerH(s.height) : v;
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void pinnedadv$searchMark(GuiGraphicsExtractor g, int xo, int yo, CallbackInfo ci) {
		if (!WideScreen.searching()) return;
		if (WideScreen.matches((AdvancementWidget) (Object) this)) return;
		int fx = xo + x + 3, fy = yo + y;
		g.fill(fx, fy, fx + 26, fy + 26, 0xB0000000);
	}
}
