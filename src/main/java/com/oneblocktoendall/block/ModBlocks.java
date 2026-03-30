package com.oneblocktoendall.block;

import com.oneblocktoendall.OneBlockMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registers all custom blocks and their item forms.
 *
 * In 1.21.4, both Block.Settings and Item.Settings require a registryKey
 * to be set BEFORE the block/item is constructed.
 */
public class ModBlocks {

    public static final Block ONE_BLOCK = registerBlock("one_block",
            new OneBlock(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK,
                            Identifier.of(OneBlockMod.MOD_ID, "one_block")))
                    .strength(0.5f)
                    .sounds(BlockSoundGroup.STONE)));

    private static Block registerBlock(String name, Block block) {
        // Register the block item with its own registry key
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM,
                Identifier.of(OneBlockMod.MOD_ID, name));
        Registry.register(Registries.ITEM, itemKey,
                new BlockItem(block, new Item.Settings().registryKey(itemKey)));
        // Register the block itself
        return Registry.register(Registries.BLOCK,
                Identifier.of(OneBlockMod.MOD_ID, name), block);
    }

    /** Call from mod initializer to force static initialization. */
    public static void register() {
        OneBlockMod.LOGGER.info("Registering blocks for " + OneBlockMod.MOD_ID);
    }
}
