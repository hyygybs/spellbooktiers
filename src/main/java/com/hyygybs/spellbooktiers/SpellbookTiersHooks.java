package com.hyygybs.spellbooktiers;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.loading.FMLPaths;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SpellbookTiersHooks {
    private static final String MYTHIC_NAME = "MYTHIC";
    private static final String ANCIENT_NAME = "ANCIENT";
    private static final String ORIGIN_NAME = "ORIGIN";
    private static final List<Double> EXTENDED_DEFAULT = List.of(0.30d, 0.25d, 0.20d, 0.15d, 0.06d, 0.03d, 0.01d);
    private static final double EPSILON = 1.0E-6D;
    private static final List<String> FANTASY_ENDING_ORIGIN_SPELLS = List.of(
            "fantasy_ending:multiple_eldritch_blast",
            "fantasy_ending:time_stop"
    );
    private static final Unsafe UNSAFE = getUnsafe();
    private static final long ENUM_NAME_OFFSET = objectFieldOffset(Enum.class, "name");
    private static final long ENUM_ORDINAL_OFFSET = objectFieldOffset(Enum.class, "ordinal");
    private static final long SPELL_RARITY_VALUE_OFFSET = objectFieldOffset(SpellRarity.class, "value");
    private static final long CLASS_ENUM_CONSTANTS_OFFSET = objectFieldOffset(Class.class, "enumConstants");
    private static final long CLASS_ENUM_DIRECTORY_OFFSET = objectFieldOffset(Class.class, "enumConstantDirectory");

    private static boolean bootstrapped;
    private static Field valuesField;

    private SpellbookTiersHooks() {
    }

    public static synchronized void bootstrapRarities() {
        if (bootstrapped) {
            return;
        }

        try {
            valuesField = findValuesField();
            createEnumIfMissing(MYTHIC_NAME, 5);
            createEnumIfMissing(ANCIENT_NAME, 6);
            bootstrapped = true;
            SpellbookTiers.LOGGER.info("Extended Iron's SpellRarity with Mythic and Ancient tiers through mixin/unsafe bootstrap.");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to bootstrap extended SpellRarity through unsafe enum extension.", exception);
        }
    }

    public static SpellRarity getMythicRarity() {
        return getRarityByName(MYTHIC_NAME, 5);
    }

    public static SpellRarity getAncientRarity() {
        return getRarityByName(ANCIENT_NAME, 6);
    }

    public static ChatFormatting getChatFormatting(SpellRarity rarity) {
        if (isNamedRarity(rarity, MYTHIC_NAME)) {
            return ChatFormatting.RED;
        }
        if (isNamedRarity(rarity, ANCIENT_NAME)) {
            return ChatFormatting.DARK_BLUE;
        }
        return switch (rarity.getValue()) {
            case 0 -> ChatFormatting.GRAY;
            case 1 -> ChatFormatting.GREEN;
            case 2 -> ChatFormatting.AQUA;
            case 3 -> ChatFormatting.LIGHT_PURPLE;
            case 4 -> ChatFormatting.GOLD;
            default -> ChatFormatting.GOLD;
        };
    }

    public static MutableComponent getDisplayName(SpellRarity rarity) {
        return Component.translatable("rarity.irons_spellbooks." + rarity.name().toLowerCase(Locale.ROOT))
                .withStyle(getChatFormatting(rarity));
    }

    public static boolean shouldOverrideRarityDisplay(SpellRarity rarity) {
        return isNamedRarity(rarity, MYTHIC_NAME) || isNamedRarity(rarity, ANCIENT_NAME);
    }

    public static List<Double> getRawRarityConfig(List<Double> fromConfig, List<Double> configDefault) {
        int expectedSize = getExpectedRarityConfigSize();
        if (isNormalized(fromConfig) && fromConfig.size() == expectedSize) {
            return List.copyOf(fromConfig);
        }

        if (hasOriginRarity() && isNormalized(fromConfig) && fromConfig.size() == 7) {
            return appendOriginWeight(fromConfig);
        }

        if (!hasOriginRarity() && isNormalized(fromConfig) && fromConfig.size() == 7) {
            return List.copyOf(fromConfig);
        }

        if (isNormalized(fromConfig) && fromConfig.size() == 5) {
            return adaptExpandedConfig(expandLegacyRarityConfig(fromConfig));
        }

        if (isNormalized(configDefault) && configDefault.size() == expectedSize) {
            SpellbookTiers.LOGGER.info("Using 7-entry fallback rarity config: {}", configDefault);
            return List.copyOf(configDefault);
        }

        if (hasOriginRarity() && isNormalized(configDefault) && configDefault.size() == 7) {
            var appended = appendOriginWeight(configDefault);
            SpellbookTiers.LOGGER.info("Appended FantasyEnding origin slot to 7-entry rarity config: {}", appended);
            return appended;
        }

        if (isNormalized(configDefault) && configDefault.size() == 5) {
            var expanded = adaptExpandedConfig(expandLegacyRarityConfig(configDefault));
            SpellbookTiers.LOGGER.info("Expanded legacy 5-entry rarity config into 7 tiers: {}", expanded);
            return expanded;
        }

        var builtIn = adaptExpandedConfig(EXTENDED_DEFAULT);
        SpellbookTiers.LOGGER.info("Using built-in rarity defaults: {}", builtIn);
        return builtIn;
    }

    public static InkItem getInkForRarity(SpellRarity rarity) {
        if (isNamedRarity(rarity, MYTHIC_NAME)) {
            return (InkItem) SpellbookTiers.MYTHIC_INK.get();
        }
        if (isNamedRarity(rarity, ANCIENT_NAME)) {
            return (InkItem) SpellbookTiers.ANCIENT_INK.get();
        }
        return switch (rarity.getValue()) {
            case 0 -> (InkItem) ItemRegistry.INK_COMMON.get();
            case 1 -> (InkItem) ItemRegistry.INK_UNCOMMON.get();
            case 2 -> (InkItem) ItemRegistry.INK_RARE.get();
            case 3 -> (InkItem) ItemRegistry.INK_EPIC.get();
            case 4 -> (InkItem) ItemRegistry.INK_LEGENDARY.get();
            default -> (InkItem) ItemRegistry.INK_COMMON.get();
        };
    }

    public static int getRarityWeight(SpellRarity rarity) {
        if (isNamedRarity(rarity, MYTHIC_NAME)) {
            return 2;
        }
        if (isNamedRarity(rarity, ANCIENT_NAME) || isOriginRarity(rarity)) {
            return 1;
        }
        return switch (rarity.getValue()) {
            case 0 -> 40;
            case 1 -> 30;
            case 2 -> 15;
            case 3 -> 8;
            case 4 -> 4;
            default -> 1;
        };
    }

    public static int getMaxSpellRarity(AbstractSpell spell) {
        if (isFantasyEndingOriginSpell(spell)) {
            SpellRarity origin = getOriginRarity();
            return origin != null ? origin.getValue() : SpellRarity.LEGENDARY.getValue();
        }
        if (!supportsExtendedTiers(spell)) {
            return SpellRarity.LEGENDARY.getValue();
        }
        SpellRarity highestExtraRarity = getHighestExtraRarity();
        return highestExtraRarity != null ? highestExtraRarity.getValue() : SpellRarity.LEGENDARY.getValue();
    }

    public static int getExtendedMaxLevel(AbstractSpell spell) {
        int baseMaxLevel = getBaseMaxLevel(spell);
        if (!supportsExtendedTiers(spell)) {
            return baseMaxLevel;
        }
        return baseMaxLevel + getAdditionalSpellLevels();
    }

    public static SpellRarity getRarityForLevel(AbstractSpell spell, int level) {
        int minRarity = spell.getMinRarity();
        int baseMaxLevel = getBaseMaxLevel(spell);

        if (!supportsExtendedTiers(spell)) {
            if (baseMaxLevel <= 1) {
                return SpellRarity.values()[minRarity];
            }
            if (level >= baseMaxLevel) {
                return SpellRarity.LEGENDARY;
            }

            List<Double> rarityWeights = getBaseRarityWeights(minRarity);
            double percentOfMaxLevel = (double) level / (double) baseMaxLevel;
            int lookupOffset = SpellRarity.LEGENDARY.getValue() + 1 - rarityWeights.size();

            for (int i = 0; i < rarityWeights.size(); i++) {
                if (percentOfMaxLevel <= rarityWeights.get(i)) {
                    return SpellRarity.values()[i + lookupOffset];
                }
            }

            return SpellRarity.values()[minRarity];
        }

        if (level > baseMaxLevel) {
            return getExtraRarityForLevel(level, baseMaxLevel);
        }

        if (level >= baseMaxLevel) {
            return SpellRarity.LEGENDARY;
        }

        List<Double> rarityWeights = getBaseRarityWeights(minRarity);
        double percentOfMaxLevel = (double) level / (double) baseMaxLevel;
        int lookupOffset = SpellRarity.LEGENDARY.getValue() + 1 - rarityWeights.size();

        for (int i = 0; i < rarityWeights.size(); i++) {
            if (percentOfMaxLevel <= rarityWeights.get(i)) {
                return SpellRarity.values()[i + lookupOffset];
            }
        }

        return SpellRarity.COMMON;
    }

    public static int getMinLevelForRarity(AbstractSpell spell, SpellRarity rarity) {
        int minRarity = spell.getMinRarity();
        int baseMaxLevel = getBaseMaxLevel(spell);

        if (rarity.getValue() < minRarity) {
            return 0;
        }

        if (isFantasyEndingOriginSpell(spell)) {
            return getMinLevelForSpecialRarity(spell, rarity, baseMaxLevel);
        }

        if (!supportsExtendedTiers(spell)) {
            if (baseMaxLevel <= 1) {
                return rarity.getValue() == minRarity ? 1 : 0;
            }

            if (rarity.getValue() > SpellRarity.LEGENDARY.getValue() && !isOriginRarity(rarity)) {
                return 0;
            }

            if (rarity.getValue() == minRarity) {
                return 1;
            }

            List<Double> rarityWeights = getBaseRarityWeights(minRarity);
            int rarityIndex = rarity.getValue() - (1 + minRarity);
            if (rarityIndex < 0) {
                return 1;
            }
            if (rarityIndex >= rarityWeights.size()) {
                return baseMaxLevel;
            }

            return (int) (rarityWeights.get(rarityIndex) * baseMaxLevel) + 1;
        }

        if (baseMaxLevel <= 1) {
            return rarity.getValue() == minRarity ? 1 : 0;
        }

        if (rarity.getValue() > SpellRarity.LEGENDARY.getValue()) {
            List<SpellRarity> extraRarities = getExtraRarities();
            for (int i = 0; i < extraRarities.size(); i++) {
                if (extraRarities.get(i) == rarity) {
                    return Math.min(baseMaxLevel + i + 1, getExtendedMaxLevel(spell));
                }
            }
            return 0;
        }

        if (rarity.getValue() == minRarity) {
            return 1;
        }

        List<Double> rarityWeights = getBaseRarityWeights(minRarity);
        int rarityIndex = rarity.getValue() - (1 + minRarity);
        if (rarityIndex < 0) {
            return 1;
        }
        if (rarityIndex >= rarityWeights.size()) {
            return baseMaxLevel;
        }

        return (int) (rarityWeights.get(rarityIndex) * baseMaxLevel) + 1;
    }

    private static SpellRarity getRarityByName(String name, int value) {
        bootstrapRarities();
        SpellRarity existing = findRarity(name);
        if (existing != null) {
            return existing;
        }
        throw new IllegalStateException("Failed to retrieve dynamic SpellRarity " + name);
    }

    private static void createEnumIfMissing(String name, int value) throws ReflectiveOperationException {
        if (findRarity(name) != null) {
            return;
        }

        SpellRarity[] currentValues = getCurrentValues();
        SpellRarity created = (SpellRarity) UNSAFE.allocateInstance(SpellRarity.class);
        UNSAFE.putObject(created, ENUM_NAME_OFFSET, name);
        UNSAFE.putInt(created, ENUM_ORDINAL_OFFSET, currentValues.length);
        UNSAFE.putInt(created, SPELL_RARITY_VALUE_OFFSET, value);

        SpellRarity[] newValues = Arrays.copyOf(currentValues, currentValues.length + 1);
        newValues[currentValues.length] = created;
        Object staticBase = UNSAFE.staticFieldBase(valuesField);
        long staticOffset = UNSAFE.staticFieldOffset(valuesField);
        UNSAFE.putObjectVolatile(staticBase, staticOffset, newValues);
        clearEnumCache();
    }

    private static boolean isNormalized(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        return Math.abs(sum - 1.0D) <= EPSILON;
    }

    private static List<Double> expandLegacyRarityConfig(List<Double> legacyConfig) {
        double tail = legacyConfig.get(4);
        return normalize(List.of(
                legacyConfig.get(0),
                legacyConfig.get(1),
                legacyConfig.get(2),
                legacyConfig.get(3),
                tail * 0.60D,
                tail * 0.30D,
                tail * 0.10D
        ));
    }

    private static List<Double> normalize(List<Double> values) {
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        List<Double> normalized = new ArrayList<>(values.size());
        for (Double value : values) {
            normalized.add(value / sum);
        }
        return List.copyOf(normalized);
    }

    private static int getBaseMaxLevel(AbstractSpell spell) {
        int defaultMaxLevel = getDefaultMaxLevel(spell);
        if (defaultMaxLevel <= 1) {
            return 1;
        }
        if (hasExplicitSpellConfigOverride(spell)) {
            return Math.max(2, getConfiguredMaxLevel(spell));
        }
        return defaultMaxLevel;
    }

    private static int getConfiguredMaxLevel(AbstractSpell spell) {
        return ServerConfigs.getSpellConfig(spell).maxLevel();
    }

    private static int getDefaultMaxLevel(AbstractSpell spell) {
        DefaultConfig defaultConfig = spell.getDefaultConfig();
        if (defaultConfig == null) {
            return getConfiguredMaxLevel(spell);
        }
        return defaultConfig.maxLevel;
    }

    private static boolean supportsExtendedTiers(AbstractSpell spell) {
        return getBaseMaxLevel(spell) > 1 && !isFantasyEndingOriginSpell(spell);
    }

    private static boolean hasExplicitSpellConfigOverride(AbstractSpell spell) {
        Path configPath = FMLPaths.CONFIGDIR.get()
                .resolve("irons_spellbooks_spell_config")
                .resolve(spell.getSpellResource().getNamespace())
                .resolve(spell.getSpellResource().getPath() + ".json");
        return Files.isRegularFile(configPath);
    }

    private static int getAdditionalSpellLevels() {
        return getExtraRarities().size();
    }

    private static SpellRarity getExtraRarityForLevel(int level, int baseMaxLevel) {
        List<SpellRarity> extraRarities = getExtraRarities();
        if (extraRarities.isEmpty()) {
            return SpellRarity.LEGENDARY;
        }
        int rarityIndex = Math.min(Math.max(1, level - baseMaxLevel) - 1, extraRarities.size() - 1);
        return extraRarities.get(rarityIndex);
    }

    private static List<Double> getBaseRarityWeights(int minRarity) {
        List<Double> rarityRawConfig = collapseToBaseConfig(SpellRarity.getRawRarityConfig());
        if (minRarity != 0) {
            var subList = rarityRawConfig.subList(minRarity, SpellRarity.LEGENDARY.getValue() + 1);
            double subtotal = subList.stream().reduce(0d, Double::sum);
            List<Double> rarityRawWeights = subList.stream()
                    .map(item -> ((item / subtotal) * (1 - subtotal)) + item)
                    .toList();
            return cumulative(rarityRawWeights);
        }
        return cumulative(rarityRawConfig);
    }

    private static List<Double> collapseToBaseConfig(List<Double> config) {
        if (config.size() <= 5) {
            return List.copyOf(config);
        }
        double legendaryPool = 0d;
        for (int i = 4; i < config.size(); i++) {
            legendaryPool += config.get(i);
        }
        return normalize(List.of(
                config.get(0),
                config.get(1),
                config.get(2),
                config.get(3),
                legendaryPool
        ));
    }

    private static List<Double> cumulative(List<Double> values) {
        double total = 0d;
        List<Double> cumulative = new ArrayList<>(values.size());
        for (Double value : values) {
            total += value;
            cumulative.add(total);
        }
        return List.copyOf(cumulative);
    }

    private static SpellRarity findRarity(String name) {
        for (SpellRarity rarity : getCurrentValues()) {
            if (rarity.name().equals(name)) {
                return rarity;
            }
        }
        return null;
    }

    private static SpellRarity[] getCurrentValues() {
        try {
            return ((SpellRarity[]) valuesField.get(null)).clone();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to read SpellRarity values field.", exception);
        }
    }

    private static Field findValuesField() throws ReflectiveOperationException {
        for (Field field : SpellRarity.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().isArray() && field.getType().getComponentType() == SpellRarity.class) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("Unable to locate SpellRarity values array.");
    }

    private static void clearEnumCache() {
        UNSAFE.putObject(SpellRarity.class, CLASS_ENUM_CONSTANTS_OFFSET, null);
        UNSAFE.putObject(SpellRarity.class, CLASS_ENUM_DIRECTORY_OFFSET, null);
    }

    private static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Unsafe for SpellRarity extension.", exception);
        }
    }

    private static long objectFieldOffset(Class<?> owner, String fieldName) {
        try {
            return UNSAFE.objectFieldOffset(owner.getDeclaredField(fieldName));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve field offset for " + owner.getName() + "#" + fieldName, exception);
        }
    }

    private static SpellRarity getOriginRarity() {
        return findRarity(ORIGIN_NAME);
    }

    private static boolean hasOriginRarity() {
        return getOriginRarity() != null;
    }

    private static boolean isOriginRarity(SpellRarity rarity) {
        return rarity != null && isNamedRarity(rarity, ORIGIN_NAME);
    }

    private static boolean isNamedRarity(SpellRarity rarity, String name) {
        return rarity != null && rarity.name().equals(name);
    }

    private static List<SpellRarity> getExtraRarities() {
        List<SpellRarity> rarities = new ArrayList<>(2);
        SpellRarity mythic = findRarity(MYTHIC_NAME);
        SpellRarity ancient = findRarity(ANCIENT_NAME);
        if (mythic != null) {
            rarities.add(mythic);
        }
        if (ancient != null) {
            rarities.add(ancient);
        }
        return List.copyOf(rarities);
    }

    private static SpellRarity getHighestExtraRarity() {
        List<SpellRarity> extraRarities = getExtraRarities();
        return extraRarities.isEmpty() ? null : extraRarities.get(extraRarities.size() - 1);
    }

    private static int getExpectedRarityConfigSize() {
        return EXTENDED_DEFAULT.size() + (hasOriginRarity() ? 1 : 0);
    }

    private static List<Double> adaptExpandedConfig(List<Double> config) {
        if (hasOriginRarity()) {
            return appendOriginWeight(config);
        }
        return List.copyOf(config);
    }

    private static List<Double> appendOriginWeight(List<Double> config) {
        List<Double> expanded = new ArrayList<>(config.size() + 1);
        expanded.addAll(config);
        expanded.add(0d);
        return List.copyOf(expanded);
    }

    private static boolean isFantasyEndingOriginSpell(AbstractSpell spell) {
        SpellRarity origin = getOriginRarity();
        if (origin == null) {
            return false;
        }
        DefaultConfig defaultConfig = spell.getDefaultConfig();
        if (defaultConfig != null && defaultConfig.minRarity == origin) {
            return true;
        }
        return FANTASY_ENDING_ORIGIN_SPELLS.contains(spell.getSpellId());
    }

    private static int getMinLevelForSpecialRarity(AbstractSpell spell, SpellRarity rarity, int baseMaxLevel) {
        for (int level = spell.getMinLevel(); level <= baseMaxLevel; level++) {
            if (spell.getRarity(level) == rarity) {
                return level;
            }
        }
        return 0;
    }
}
