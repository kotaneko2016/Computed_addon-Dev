package dev.kotan.aeronautics_scripting.client;

import dev.kotan.aeronautics_scripting.network.CodeWorkplaceSaveResultPayload;
import dev.kotan.aeronautics_scripting.network.SaveCodeWorkplacePayload;
import dev.kotan.aeronautics_scripting.program.ProgramValue;
import dev.kotan.aeronautics_scripting.script.ScriptEngine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CodeWorkplaceScreen extends Screen {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 252;
    private static final int MAX_LINES = ScriptEngine.MAX_STATEMENTS;

    private final BlockPos pos;
    private final List<String> lines = new ArrayList<>();
    private long revision;
    private int left;
    private int top;
    private EditBox lineInput;
    private Component status = Component.translatable("screen.aeronautics_scripting.code_workplace.ready");
    private boolean saving;

    public CodeWorkplaceScreen(BlockPos pos, long revision, String source) {
        super(Component.translatable("screen.aeronautics_scripting.code_workplace.title"));
        this.pos = pos;
        this.revision = revision;
        if (source != null && !source.isBlank()) {
            this.lines.addAll(List.of(source.split("\\R", -1)));
        }
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        lineInput = new EditBox(
            font,
            left + 12,
            top + 172,
            238,
            20,
            Component.translatable("screen.aeronautics_scripting.code_workplace.line_input")
        );
        lineInput.setMaxLength(256);
        lineInput.setHint(Component.translatable("screen.aeronautics_scripting.code_workplace.line_hint"));
        addRenderableWidget(lineInput);

        addRenderableWidget(Button.builder(
            Component.translatable("screen.aeronautics_scripting.code_workplace.add_line"),
            button -> addLineFromInput()
        ).bounds(left + 256, top + 172, 132, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("+ Altitude"),
            button -> addStatement("screen \"Altitude\" = altitude")
        ).bounds(left + 12, top + 198, 90, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("+ Speed"),
            button -> addStatement("screen \"Speed\" = speed")
        ).bounds(left + 108, top + 198, 78, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("+ Pitch"),
            button -> addStatement("screen \"Pitch\" = pitch")
        ).bounds(left + 192, top + 198, 78, 20).build());
        addRenderableWidget(Button.builder(
            Component.literal("+ Link"),
            button -> addStatement("link = 0")
        ).bounds(left + 276, top + 198, 62, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("screen.aeronautics_scripting.code_workplace.remove_line"),
            button -> removeLastLine()
        ).bounds(left + 12, top + 224, 108, 20).build());
        addRenderableWidget(Button.builder(
            Component.translatable("screen.aeronautics_scripting.code_workplace.save"),
            button -> saveProgram()
        ).bounds(left + 280, top + 224, 108, 20).build());
        setInitialFocus(lineInput);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF30343B);
        graphics.fill(left + 8, top + 34, left + PANEL_WIDTH - 8, top + 164, 0xFF1F2228);
        graphics.drawString(
            font,
            Component.translatable("screen.aeronautics_scripting.code_workplace.title"),
            left + 12,
            top + 10,
            0xFFFFFFFF
        );
        graphics.drawString(font, status, left + 12, top + 24, 0xFFB8D7FF);
        graphics.drawString(
            font,
            Component.translatable("screen.aeronautics_scripting.code_workplace.program"),
            left + 12,
            top + 38,
            0xFFF1D982
        );
        renderProgramLines(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void handleSaveResult(CodeWorkplaceSaveResultPayload payload) {
        if (!payload.pos().equals(pos)) {
            return;
        }
        saving = false;
        if (payload.accepted()) {
            revision = payload.revision();
            status = Component.translatable("screen.aeronautics_scripting.code_workplace.saved");
        } else {
            status = Component.literal(payload.message());
        }
    }

    private void renderProgramLines(GuiGraphics graphics) {
        int visibleLines = 7;
        int firstLine = Math.max(0, lines.size() - visibleLines);
        for (int index = firstLine; index < lines.size(); index++) {
            int visibleIndex = index - firstLine;
            graphics.drawString(
                font,
                (index + 1) + ". " + lines.get(index),
                left + 14,
                top + 54 + visibleIndex * 15,
                0xFFE6E9EF,
                false
            );
        }
        if (lines.isEmpty()) {
            graphics.drawString(
                font,
                Component.translatable("screen.aeronautics_scripting.code_workplace.empty"),
                left + 14,
                top + 57,
                0xFF9098A5
            );
        }
        graphics.drawString(
            font,
            lines.size() + "/" + MAX_LINES,
            left + PANEL_WIDTH - 42,
            top + 38,
            lines.size() >= MAX_LINES ? 0xFFFF7777 : 0xFF9098A5
        );
    }

    private void addLineFromInput() {
        if (lineInput == null) {
            return;
        }
        addStatement(lineInput.getValue());
        lineInput.setValue("");
        setFocused(lineInput);
    }

    private void addStatement(String statement) {
        String trimmed = statement.strip();
        if (trimmed.isEmpty()) {
            status = Component.translatable("screen.aeronautics_scripting.code_workplace.empty_line");
            return;
        }
        if (lines.size() >= MAX_LINES) {
            status = Component.translatable("screen.aeronautics_scripting.code_workplace.too_many_lines");
            return;
        }
        int sourceLength = lines.stream().mapToInt(String::length).sum() + lines.size() + trimmed.length();
        if (sourceLength > ScriptEngine.MAX_SOURCE_LENGTH) {
            status = Component.translatable("screen.aeronautics_scripting.code_workplace.program_too_large");
            return;
        }
        lines.add(trimmed);
        status = Component.translatable("screen.aeronautics_scripting.code_workplace.unsaved");
    }

    private void removeLastLine() {
        if (!lines.isEmpty()) {
            lines.remove(lines.size() - 1);
            status = Component.translatable("screen.aeronautics_scripting.code_workplace.unsaved");
        }
    }

    private void saveProgram() {
        if (saving) {
            return;
        }
        String source = String.join("\n", lines);
        try {
            new ScriptEngine().execute(source, validationInputs());
        } catch (IllegalArgumentException exception) {
            status = Component.literal(exception.getMessage() == null ? "Invalid program." : exception.getMessage());
            return;
        }
        PacketDistributor.sendToServer(new SaveCodeWorkplacePayload(pos, revision, source));
        saving = true;
        status = Component.translatable("screen.aeronautics_scripting.code_workplace.saving");
    }

    private static Map<String, Double> validationInputs() {
        return java.util.Arrays.stream(ProgramValue.Sensor.values())
            .collect(java.util.stream.Collectors.toUnmodifiableMap(ProgramValue.Sensor::id, sensor -> 0.0));
    }
}
