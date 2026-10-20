package dev.kotan.aeronautics_scripting.program;

import static dev.kotan.aeronautics_scripting.program.ProgramValue.ArithmeticOperator;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.ComparisonOperator;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.Sensor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VisualProgramExecutorTest {
    private final VisualProgramExecutor executor = new VisualProgramExecutor();

    @Test
    void runsSensorMathVariablesConditionalDisplayAndRedstoneBlocks() {
        ProgramValue speed = new ProgramValue.SensorValue(Sensor.SPEED);
        ProgramValue threshold = new ProgramValue.ArithmeticValue(
            ArithmeticOperator.MULTIPLY,
            new ProgramValue.NumberValue(2),
            new ProgramValue.NumberValue(5)
        );
        VisualProgram program = new VisualProgram(List.of(
            new ProgramStatement.SetVariable(
                "target",
                new ProgramValue.ClampValue(
                    new ProgramValue.ArithmeticValue(
                        ArithmeticOperator.SUBTRACT,
                        speed,
                        new ProgramValue.NumberValue(10)
                    ),
                    new ProgramValue.NumberValue(0),
                    new ProgramValue.NumberValue(15)
                )
            ),
            new ProgramStatement.If(
                new ProgramValue.ComparisonCondition(
                    ComparisonOperator.GREATER_THAN,
                    speed,
                    threshold
                ),
                List.of(
                    new ProgramStatement.DisplayValue("Speed", new ProgramValue.VariableValue("target")),
                    new ProgramStatement.SetRedstone(new ProgramValue.VariableValue("target"))
                )
            )
        ));

        ProgramExecutionResult result = executor.execute(program, Map.of(Sensor.SPEED, 25.0));

        assertEquals(List.of("Speed: 15"), result.displayLines());
        assertEquals(15, result.redstonePower().orElseThrow());
        assertEquals(15.0, result.variables().get("target"));
    }

    @Test
    void doesNotExecuteFalseConditionalBlocks() {
        VisualProgram program = new VisualProgram(List.of(
            new ProgramStatement.If(
                new ProgramValue.ComparisonCondition(
                    ComparisonOperator.LESS_THAN,
                    new ProgramValue.SensorValue(Sensor.ALTITUDE),
                    new ProgramValue.NumberValue(0)
                ),
                List.of(new ProgramStatement.SetRedstone(new ProgramValue.NumberValue(15)))
            )
        ));

        ProgramExecutionResult result = executor.execute(program, Map.of(Sensor.ALTITUDE, 100.0));

        assertEquals(java.util.OptionalInt.empty(), result.redstonePower());
    }

    @Test
    void reportsMissingSensorsAndVariables() {
        assertThrows(
            ProgramException.class,
            () -> executor.execute(
                new VisualProgram(List.of(new ProgramStatement.DisplayValue(
                    "Altitude",
                    new ProgramValue.SensorValue(Sensor.ALTITUDE)
                ))),
                Map.of()
            )
        );
        assertThrows(
            ProgramException.class,
            () -> executor.execute(
                new VisualProgram(List.of(new ProgramStatement.SetRedstone(new ProgramValue.VariableValue("unset")))),
                Map.of()
            )
        );
    }

    @Test
    void rejectsOversizedProgramsBeforeExecutingThem() {
        List<ProgramStatement> blocks = java.util.stream.IntStream.range(0, VisualProgramExecutor.MAX_BLOCKS + 1)
            .mapToObj(index -> (ProgramStatement) new ProgramStatement.SetRedstone(new ProgramValue.NumberValue(index)))
            .toList();

        assertThrows(
            ProgramException.class,
            () -> executor.execute(new VisualProgram(blocks), Map.of())
        );
    }

    @Test
    void countsNestedValueBlocksAgainstTheProgramLimit() {
        ProgramValue expression = new ProgramValue.NumberValue(1);
        for (int index = 0; index < 7; index++) {
            expression = new ProgramValue.ArithmeticValue(
                ArithmeticOperator.ADD,
                expression,
                expression
            );
        }
        VisualProgram program = new VisualProgram(List.of(new ProgramStatement.SetRedstone(expression)));

        assertThrows(ProgramException.class, () -> executor.execute(program, Map.of()));
    }

    @Test
    void clampsNegativeRedstoneOutputToZero() {
        ProgramExecutionResult result = executor.execute(
            new VisualProgram(List.of(new ProgramStatement.SetRedstone(new ProgramValue.NumberValue(-4)))),
            Map.of()
        );

        assertEquals(0, result.redstonePower().orElseThrow());
    }
}
