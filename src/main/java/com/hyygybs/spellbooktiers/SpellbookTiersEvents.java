package com.hyygybs.spellbooktiers;

import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.player.AdditionalWanderingTrades;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpellbookTiers.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpellbookTiersEvents {
    private SpellbookTiersEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addWanderingTrades(WandererTradesEvent event) {
        if (!ServerConfigs.ADDITIONAL_WANDERING_TRADER_TRADES.get()) {
            return;
        }

        event.getGenericTrades().add(new AdditionalWanderingTrades.InkBuyTrade((InkItem) SpellbookTiers.MYTHIC_INK.get()));
        event.getGenericTrades().add(new AdditionalWanderingTrades.InkBuyTrade((InkItem) SpellbookTiers.ANCIENT_INK.get()));
        event.getGenericTrades().add(new AdditionalWanderingTrades.InkSellTrade((InkItem) SpellbookTiers.MYTHIC_INK.get()));
        event.getGenericTrades().add(new AdditionalWanderingTrades.InkSellTrade((InkItem) SpellbookTiers.ANCIENT_INK.get()));
    }
}
