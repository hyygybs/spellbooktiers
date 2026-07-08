package com.hyygybs.spellbooktiers.mixin;

import com.hyygybs.spellbooktiers.SpellbookTiersHooks;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = SpellRarity.class, remap = false)
abstract class SpellRarityMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void spellbooktiers$bootstrap(CallbackInfo ci) {
        SpellbookTiersHooks.bootstrapRarities();
    }

    @Inject(method = "getChatFormatting", at = @At("HEAD"), cancellable = true, remap = false)
    private void spellbooktiers$getChatFormatting(CallbackInfoReturnable<ChatFormatting> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getChatFormatting((SpellRarity) (Object) this));
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true, remap = false)
    private void spellbooktiers$getDisplayName(CallbackInfoReturnable<MutableComponent> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getDisplayName((SpellRarity) (Object) this));
    }

    @Inject(method = "getRawRarityConfigInternal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void spellbooktiers$getRawRarityConfigInternal(CallbackInfoReturnable<List<Double>> cir) {
        @SuppressWarnings("unchecked")
        List<Double> fromConfig = (List<Double>) ServerConfigs.RARITY_CONFIG.get();
        @SuppressWarnings("unchecked")
        List<Double> configDefault = (List<Double>) ServerConfigs.RARITY_CONFIG.getDefault();
        cir.setReturnValue(SpellbookTiersHooks.getRawRarityConfig(fromConfig, configDefault));
    }
}
