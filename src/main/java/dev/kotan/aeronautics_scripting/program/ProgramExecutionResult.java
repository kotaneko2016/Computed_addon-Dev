package dev.kotan.aeronautics_scripting.program;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public record ProgramExecutionResult(
    List<String> displayLines,
    OptionalInt redstonePower,
    Map<String, Double> variables
) {
    public ProgramExecutionResult {
        displayLines = List.copyOf(displayLines);
        variables = Map.copyOf(variables);
    }
}
