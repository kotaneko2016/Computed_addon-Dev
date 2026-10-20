package dev.kotan.aeronautics_scripting.program;

import java.util.List;

public record VisualProgram(List<ProgramStatement> blocks) {
    public VisualProgram {
        blocks = List.copyOf(blocks);
    }
}
