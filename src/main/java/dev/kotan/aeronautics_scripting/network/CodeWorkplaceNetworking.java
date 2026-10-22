package dev.kotan.aeronautics_scripting.network;

import dev.kotan.aeronautics_scripting.content.CodeWorkplaceBlockEntity;
import dev.kotan.aeronautics_scripting.script.ScriptEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CodeWorkplaceNetworking {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0;

    private CodeWorkplaceNetworking() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CodeWorkplaceNetworking::registerPayloads);
    }

    public static void openEditor(ServerPlayer player, BlockPos pos, String program, long revision) {
        PacketDistributor.sendToPlayer(player, new OpenCodeWorkplacePayload(pos, revision, program));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
            OpenCodeWorkplacePayload.TYPE,
            OpenCodeWorkplacePayload.STREAM_CODEC,
            CodeWorkplaceNetworking::handleOpen
        );
        registrar.playToServer(
            SaveCodeWorkplacePayload.TYPE,
            SaveCodeWorkplacePayload.STREAM_CODEC,
            CodeWorkplaceNetworking::handleSave
        );
        registrar.playToClient(
            CodeWorkplaceSaveResultPayload.TYPE,
            CodeWorkplaceSaveResultPayload.STREAM_CODEC,
            CodeWorkplaceNetworking::handleSaveResult
        );
    }

    private static void handleOpen(OpenCodeWorkplacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                CodeWorkplaceClient.open(payload);
            }
        });
    }

    private static void handleSaveResult(CodeWorkplaceSaveResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                CodeWorkplaceClient.saveResult(payload);
            }
        });
    }

    private static void handleSave(SaveCodeWorkplacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = payload.pos();
            if (!isAuthorized(player, pos)) {
                sendResult(player, pos, false, payload.expectedRevision(), "You are not allowed to edit here.");
                return;
            }
            if (!(player.level().getBlockEntity(pos) instanceof CodeWorkplaceBlockEntity workplace)) {
                sendResult(player, pos, false, payload.expectedRevision(), "Code Workplace is no longer available.");
                return;
            }
            try {
                if (!workplace.setProgram(payload.program(), payload.expectedRevision())) {
                    sendResult(player, pos, false, workplace.getRevision(), "Program changed; reopen the editor.");
                    return;
                }
                sendResult(player, pos, true, workplace.getRevision(), "Program saved.");
            } catch (IllegalArgumentException exception) {
                String message = exception.getMessage();
                sendResult(
                    player,
                    pos,
                    false,
                    workplace.getRevision(),
                    message == null ? "Program validation failed." : message
                );
            }
        });
    }

    private static boolean isAuthorized(ServerPlayer player, BlockPos pos) {
        return player.level().hasChunkAt(pos)
            && player.distanceToSqr(Vec3.atCenterOf(pos)) <= MAX_INTERACTION_DISTANCE_SQUARED
            && player.mayBuild()
            && player.level().mayInteract(player, pos);
    }

    private static void sendResult(
        ServerPlayer player,
        BlockPos pos,
        boolean accepted,
        long revision,
        String message
    ) {
        String boundedMessage = message.codePoints()
            .limit(64)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
        PacketDistributor.sendToPlayer(
            player,
            new CodeWorkplaceSaveResultPayload(pos, accepted, revision, boundedMessage)
        );
    }
}
