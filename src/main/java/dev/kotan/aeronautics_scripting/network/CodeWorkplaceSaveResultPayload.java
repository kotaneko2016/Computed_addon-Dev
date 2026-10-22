package dev.kotan.aeronautics_scripting.network;

import dev.kotan.aeronautics_scripting.AeronauticsScripting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CodeWorkplaceSaveResultPayload(
    BlockPos pos,
    boolean accepted,
    long revision,
    String message
) implements CustomPacketPayload {
    public static final Type<CodeWorkplaceSaveResultPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(AeronauticsScripting.MOD_ID, "code_workplace_save_result")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CodeWorkplaceSaveResultPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CodeWorkplaceSaveResultPayload::pos,
            ByteBufCodecs.BOOL,
            CodeWorkplaceSaveResultPayload::accepted,
            ByteBufCodecs.VAR_LONG,
            CodeWorkplaceSaveResultPayload::revision,
            ByteBufCodecs.stringUtf8(256),
            CodeWorkplaceSaveResultPayload::message,
            CodeWorkplaceSaveResultPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
