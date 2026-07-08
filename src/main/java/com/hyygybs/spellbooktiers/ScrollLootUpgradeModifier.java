package com.hyygybs.spellbooktiers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class ScrollLootUpgradeModifier extends LootModifier {
    public static final Codec<ScrollLootUpgradeModifier> CODEC = RecordCodecBuilder.create(inst ->
            codecStart(inst).apply(inst, ScrollLootUpgradeModifier::new));

    public ScrollLootUpgradeModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (ItemStack stack : generatedLoot) {
            upgradeScroll(stack, context.getRandom());
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    private static void upgradeScroll(ItemStack stack, RandomSource random) {
        if (!stack.is(ItemRegistry.SCROLL.get()) || !ISpellContainer.isSpellContainer(stack)) {
            return;
        }

        SpellData spellData = ISpellContainer.get(stack).getSpellAtIndex(0);
        AbstractSpell spell = spellData.getSpell();
        if (spell == SpellRegistry.none()) {
            return;
        }

        int currentLevel = spellData.getLevel();
        int maxLevel = spell.getMaxLevel();
        if (currentLevel >= maxLevel || maxLevel <= 1) {
            return;
        }

        int bonusLevels = rollBonusLevels(spellData.getRarity(), currentLevel, maxLevel, random);
        if (bonusLevels <= 0) {
            return;
        }

        int upgradedLevel = Math.min(maxLevel, currentLevel + bonusLevels);
        if (upgradedLevel <= currentLevel) {
            return;
        }

        ISpellContainer.createScrollContainer(spell, upgradedLevel, stack);
    }

    private static int rollBonusLevels(SpellRarity rarity, int currentLevel, int maxLevel, RandomSource random) {
        double proximity = (double) currentLevel / (double) maxLevel;
        double firstRollChance = switch (rarity.getValue()) {
            case 4 -> 0.40D;
            case 3 -> 0.20D;
            case 2 -> 0.08D;
            default -> 0.0D;
        };
        firstRollChance += Math.max(0D, proximity - 0.55D) * 0.35D;

        if (random.nextDouble() >= Math.min(0.85D, firstRollChance)) {
            return 0;
        }

        int bonusLevels = 1;
        if (currentLevel + bonusLevels >= maxLevel) {
            return bonusLevels;
        }

        double secondRollChance = switch (rarity.getValue()) {
            case 4 -> 0.14D;
            case 3 -> 0.06D;
            default -> 0.0D;
        };
        secondRollChance += Math.max(0D, proximity - 0.75D) * 0.25D;

        if (random.nextDouble() < Math.min(0.45D, secondRollChance)) {
            bonusLevels++;
        }

        return Math.min(bonusLevels, maxLevel - currentLevel);
    }
}
