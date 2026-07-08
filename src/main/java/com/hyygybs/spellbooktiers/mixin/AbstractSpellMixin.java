package com.hyygybs.spellbooktiers.mixin;

import com.hyygybs.spellbooktiers.SpellbookTiersHooks;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
abstract class AbstractSpellMixin {
    @Inject(method = "getMaxRarity", at = @At("HEAD"), cancellable = true, remap = false)
    private void spellbooktiers$getMaxRarity(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getMaxSpellRarity());
    }

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private void spellbooktiers$getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getExtendedMaxLevel((AbstractSpell) (Object) this));
    }

    @Inject(method = "getRarity(I)Lio/redspace/ironsspellbooks/api/spells/SpellRarity;", at = @At("HEAD"), cancellable = true, remap = false)
    private void spellbooktiers$getRarity(int level, CallbackInfoReturnable<SpellRarity> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getRarityForLevel((AbstractSpell) (Object) this, level));
    }

    @Inject(method = "getMinLevelForRarity(Lio/redspace/ironsspellbooks/api/spells/SpellRarity;)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void spellbooktiers$getMinLevelForRarity(SpellRarity rarity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SpellbookTiersHooks.getMinLevelForRarity((AbstractSpell) (Object) this, rarity));
    }
}
