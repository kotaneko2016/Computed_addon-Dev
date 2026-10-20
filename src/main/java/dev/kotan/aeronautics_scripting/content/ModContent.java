package dev.kotan.aeronautics_scripting.content;

import dev.kotan.aeronautics_scripting.AeronauticsScripting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModContent {
    private static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(Registries.BLOCK, AeronauticsScripting.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, AeronauticsScripting.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AeronauticsScripting.MOD_ID);

    public static final Supplier<Block> CODE_WORKPLACE = BLOCKS.register(
        "code_workplace",
        () -> new Block(BlockBehaviour.Properties.of().strength(3.5F))
    );
    public static final Supplier<Item> CODE_WORKPLACE_ITEM = ITEMS.register(
        "code_workplace",
        () -> new BlockItem(CODE_WORKPLACE.get(), new Item.Properties())
    );

    static {
        CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + AeronauticsScripting.MOD_ID))
                .icon(() -> CODE_WORKPLACE_ITEM.get().getDefaultInstance())
                .displayItems((parameters, output) -> output.accept(CODE_WORKPLACE_ITEM.get()))
                .build()
        );
    }

    private ModContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
