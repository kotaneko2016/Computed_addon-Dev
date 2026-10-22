package dev.kotan.aeronautics_scripting.network;

import dev.kotan.aeronautics_scripting.client.CodeWorkplaceScreen;
import net.minecraft.client.Minecraft;

public final class CodeWorkplaceClient {
    private CodeWorkplaceClient() {
    }

    public static void open(OpenCodeWorkplacePayload payload) {
        Minecraft.getInstance().setScreen(new CodeWorkplaceScreen(payload.pos(), payload.revision(), payload.program()));
    }

    public static void saveResult(CodeWorkplaceSaveResultPayload payload) {
        if (Minecraft.getInstance().screen instanceof CodeWorkplaceScreen screen) {
            screen.handleSaveResult(payload);
        }
    }
}
