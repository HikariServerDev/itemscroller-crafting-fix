package fi.dy.masa.itemscroller.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import fi.dy.masa.itemscroller.util.InputUtils;

@Mixin(StatusEffectsDisplay.class)
public abstract class MixinStatusEffectsDisplay
{
    @Inject(method = "drawStatusEffects", at = @At("HEAD"), cancellable = true)
    private void preventPotionEffectRendering(CallbackInfo ci)
    {
        if (InputUtils.isRecipeViewOpen())
        {
            ci.cancel();
        }
    }
}
