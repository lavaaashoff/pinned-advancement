package dev.pinnedadv.mixin;

import dev.pinnedadv.PinnedAdvancement;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unpin as soon as the server reports the pinned advancement as completed. */
@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin {
	@Inject(method = "update", at = @At("TAIL"))
	private void pinnedadv$afterUpdate(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
		PinnedAdvancement.onAdvancementsUpdated();
	}
}
