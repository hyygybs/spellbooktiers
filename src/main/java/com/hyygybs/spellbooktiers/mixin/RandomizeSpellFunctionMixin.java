package com.hyygybs.spellbooktiers.mixin;

import com.hyygybs.spellbooktiers.SpellbookTiersHooks;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.loot.RandomizeSpellFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RandomizeSpellFunction.class, remap = false)
abstract class RandomizeSpellFunctionMixin {
    @Inject(method = "getWeightFromRarity", at = @At("HEAD"), cancellable = true, remap = false)
    private void spellbooktiers$getWeightFromRarity(SpellRarity rarity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getRarityWeight(rarity));
    }
}
