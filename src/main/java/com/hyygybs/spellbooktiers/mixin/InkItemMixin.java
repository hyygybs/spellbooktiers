package com.hyygybs.spellbooktiers.mixin;

import com.hyygybs.spellbooktiers.SpellbookTiersHooks;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.InkItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InkItem.class, remap = false)
abstract class InkItemMixin {
    @Inject(method = "getInkForRarity", at = @At("HEAD"), cancellable = true, remap = false)
    private static void spellbooktiers$getInkForRarity(SpellRarity rarity, CallbackInfoReturnable<InkItem> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getInkForRarity(rarity));
    }
}
