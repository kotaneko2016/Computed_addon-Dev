package dev.kotan.aeronautics_scripting.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ScriptEngineTest {
    private final ScriptEngine engine = new ScriptEngine();

    @Test
    void evaluatesTelemetryDisplaysAndRedstoneOutput() {
        ScriptExecutionResult result = engine.execute("""
            let output = clamp((altitude - 100) / 10, 0, 15)
            screen "Altitude" = altitude
            screen "Pitch" = pitch
            link = output
            """, Map.of("altitude", 180.0, "pitch", -12.5));

        assertEquals(java.util.List.of("Altitude: 180", "Pitch: -12.5"), result.displayLines());
        assertEquals(8, result.redstonePower().orElseThrow());
        assertEquals(8.0, result.variables().get("output"));
    }

    @Test
    void roundsAndClampsRedstoneOutput() {
        ScriptExecutionResult result = engine.execute("link = 20.4", Map.of());

        assertEquals(15, result.redstonePower().orElseThrow());
    }

    @Test
    void reportsUnknownVariablesWithTheirSourceLine() {
        ScriptException exception = assertThrows(
            ScriptException.class,
            () -> engine.execute("# comment\nscreen \"Speed\" = airspeed", Map.of())
        );

        assertEquals(2, exception.line());
    }

    @Test
    void rejectsDivisionByZero() {
        assertThrows(ScriptException.class, () -> engine.execute("link = 1 / 0", Map.of()));
    }

    @Test
    void handlesLongUnaryChainsWithoutRecursiveStackGrowth() {
        String expression = "+".repeat(ScriptEngine.MAX_SOURCE_LENGTH - "link = ".length() - "1".length()) + "1";

        ScriptExecutionResult result = engine.execute("link = " + expression, Map.of());

        assertEquals(1, result.redstonePower().orElseThrow());
    }

    @Test
    void rejectsExcessivelyNestedExpressions() {
        String expression = "(".repeat(ScriptEngine.MAX_EXPRESSION_DEPTH + 1)
            + "1"
            + ")".repeat(ScriptEngine.MAX_EXPRESSION_DEPTH + 1);

        assertThrows(ScriptException.class, () -> engine.execute("link = " + expression, Map.of()));
    }
}
