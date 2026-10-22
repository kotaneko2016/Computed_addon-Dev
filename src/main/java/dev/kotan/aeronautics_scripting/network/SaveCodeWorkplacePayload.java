package dev.kotan.aeronautics_scripting.network;

import dev.kotan.aeronautics_scripting.AeronauticsScripting;
import dev.kotan.aeronautics_scripting.script.ScriptEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SaveCodeWorkplacePayload(BlockPos pos, long expectedRevision, String program)
    implements CustomPacketPayload {
    public static final Type<SaveCodeWorkplacePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(AeronauticsScripting.MOD_ID, "save_code_workplace")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveCodeWorkplacePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SaveCodeWorkplacePayload::pos,
            ByteBufCodecs.VAR_LONG,
            SaveCodeWorkplacePayload::expectedRevision,
            ByteBufCodecs.stringUtf8(ScriptEngine.MAX_SOURCE_LENGTH),
            SaveCodeWorkplacePayload::program,
            SaveCodeWorkplacePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
