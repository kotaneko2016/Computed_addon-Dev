package dev.kotan.aeronautics_scripting.network;

import dev.kotan.aeronautics_scripting.AeronauticsScripting;
import dev.kotan.aeronautics_scripting.script.ScriptEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCodeWorkplacePayload(BlockPos pos, long revision, String program) implements CustomPacketPayload {
    public static final Type<OpenCodeWorkplacePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(AeronauticsScripting.MOD_ID, "open_code_workplace")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCodeWorkplacePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            OpenCodeWorkplacePayload::pos,
            ByteBufCodecs.VAR_LONG,
            OpenCodeWorkplacePayload::revision,
            ByteBufCodecs.stringUtf8(ScriptEngine.MAX_SOURCE_LENGTH),
            OpenCodeWorkplacePayload::program,
            OpenCodeWorkplacePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
