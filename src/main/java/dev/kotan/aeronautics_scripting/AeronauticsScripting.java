package dev.kotan.aeronautics_scripting;

import dev.kotan.aeronautics_scripting.content.ModContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(AeronauticsScripting.MOD_ID)
public final class AeronauticsScripting {
    public static final String MOD_ID = "aeronautics_scripting";

    public AeronauticsScripting(IEventBus modEventBus, ModContainer container) {
        ModContent.register(modEventBus);
    }
}
