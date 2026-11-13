package dev.pinnedadv.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.pinnedadv.PinnedAdvancement;
import dev.pinnedadv.PinnedHud;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** The pinned advancement gets a blue frame background in the advancement menu. */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {
	@Shadow @Final private AdvancementNode advancementNode;
	@Shadow @Final private DisplayInfo display;

	@WrapOperation(method = {"extractRenderState", "extractHover"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			require = 0)
	private void pinnedadv$blueFrame(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h, Operation<Void> original) {
		if (sprite.getPath().contains("_frame_") && PinnedAdvancement.isPinned(advancementNode.holder().id())) {
			g.blitSprite(pipeline, PinnedHud.frameSprite(display.type()), x, y, w, h, PinnedHud.PINNED_TINT);
			return;
		}
		original.call(g, pipeline, sprite, x, y, w, h);
	}
}
