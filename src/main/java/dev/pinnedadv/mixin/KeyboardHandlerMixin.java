package dev.pinnedadv.mixin;

import dev.pinnedadv.PinnedAdvancement;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Show/hide hotkey: reacts to the key press itself (physical key, so any keyboard layout works). */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void pinnedadv$hotkey(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (PinnedAdvancement.onKey(action, event)) ci.cancel(); // the combo is ours: vanilla must not also act on it
		else if (PinnedAdvancement.onCopyKey(action, event)) ci.cancel(); // copied the hovered advancement's id
	}
}
