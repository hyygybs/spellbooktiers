package com.hyygybs.spellbooktiers;

import com.mojang.serialization.Codec;
import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.fluids.NoopFluid;
import io.redspace.ironsspellbooks.fluids.SimpleTintedClientFluidType;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.util.ItemPropertiesHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod(SpellbookTiers.MODID)
public class SpellbookTiers {
    public static final String MODID = "spellbooktiers";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    public static final RegistryObject<Codec<ScrollLootUpgradeModifier>> SCROLL_LOOT_UPGRADE =
            LOOT_MODIFIERS.register("scroll_loot_upgrade", () -> ScrollLootUpgradeModifier.CODEC);

    public static final RegistryObject<FluidType> MYTHIC_INK_TYPE = registerTintedFluidType("mythic_ink", 0xffd43c3c);
    public static final RegistryObject<FluidType> ANCIENT_INK_TYPE = registerTintedFluidType("ancient_ink", 0xffd8d8d8);
    public static final RegistryObject<Fluid> MYTHIC_INK_FLUID = registerNoopFluid("mythic_ink", MYTHIC_INK_TYPE);
    public static final RegistryObject<Fluid> ANCIENT_INK_FLUID = registerNoopFluid("ancient_ink", ANCIENT_INK_TYPE);

    public static final RegistryObject<Item> MYTHIC_INK = ITEMS.register("mythic_ink",
            () -> new InkItem(SpellbookTiersHooks.getMythicRarity(), MYTHIC_INK_FLUID, ItemPropertiesHelper.material()));
    public static final RegistryObject<Item> ANCIENT_INK = ITEMS.register("ancient_ink",
            () -> new InkItem(SpellbookTiersHooks.getAncientRarity(), ANCIENT_INK_FLUID, ItemPropertiesHelper.material()));
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.spellbooktiers.main"))
                    .withTabsBefore(CreativeModeTabs.INGREDIENTS)
                    .icon(() -> MYTHIC_INK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(MYTHIC_INK.get());
                        output.accept(ANCIENT_INK.get());
                    })
                    .build());

    public SpellbookTiers(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(MYTHIC_INK);
            event.accept(ANCIENT_INK);
        }
    }

    private static RegistryObject<FluidType> registerTintedFluidType(String name, int color) {
        return FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new SimpleTintedClientFluidType(new ResourceLocation("minecraft", "block/water_still"), color));
            }
        });
    }

    private static RegistryObject<Fluid> registerNoopFluid(String name, Supplier<FluidType> fluidType) {
        RegistryObject<Fluid> fluid = RegistryObject.create(new ResourceLocation(MODID, name), ForgeRegistries.FLUIDS);
        ForgeFlowingFluid.Properties properties = new ForgeFlowingFluid.Properties(fluidType, fluid, fluid)
                .bucket(() -> Items.AIR);
        FLUIDS.register(name, () -> new NoopFluid(properties));
        return fluid;
    }
}
