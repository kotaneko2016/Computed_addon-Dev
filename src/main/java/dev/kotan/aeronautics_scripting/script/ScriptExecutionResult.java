package dev.kotan.aeronautics_scripting.script;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public record ScriptExecutionResult(
    List<String> displayLines,
    OptionalInt redstonePower,
    Map<String, Double> variables
) {
    public ScriptExecutionResult {
        displayLines = List.copyOf(displayLines);
        variables = Map.copyOf(variables);
    }
}
