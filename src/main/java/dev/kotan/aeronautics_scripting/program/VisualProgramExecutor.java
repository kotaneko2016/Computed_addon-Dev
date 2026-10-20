package dev.kotan.aeronautics_scripting.program;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import static dev.kotan.aeronautics_scripting.program.ProgramValue.ArithmeticOperator;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.ComparisonCondition;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.ComparisonOperator;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.Condition;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.Sensor;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.ArithmeticValue;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.ClampValue;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.NotCondition;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.NumberValue;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.SensorValue;
import static dev.kotan.aeronautics_scripting.program.ProgramValue.VariableValue;

public final class VisualProgramExecutor {
    public static final int MAX_BLOCKS = 128;
    public static final int MAX_NESTING_DEPTH = 8;
    public static final int MAX_VALUE_DEPTH = 32;
    public static final int MAX_DISPLAY_LINES = 8;

    public ProgramExecutionResult execute(VisualProgram program, Map<Sensor, Double> sensorValues) {
        if (program == null || sensorValues == null) {
            throw new IllegalArgumentException("Program and sensor values are required");
        }
        Map<Sensor, Double> sensors = new EnumMap<>(Sensor.class);
        sensorValues.forEach((sensor, value) -> {
            if (sensor == null || value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Sensor values must have a valid sensor and finite number");
            }
            sensors.put(sensor, value);
        });

        validateStatements(program.blocks(), 0, new int[] {0});
        ExecutionState state = new ExecutionState(sensors);
        executeStatements(program.blocks(), state);
        return new ProgramExecutionResult(state.displayLines, state.redstonePower, state.variables);
    }

    public static Set<String> availableSensors() {
        return Arrays.stream(Sensor.values())
            .map(Sensor::id)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static void validateStatements(List<ProgramStatement> statements, int depth, int[] blockCount) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new ProgramException("Program exceeds maximum nesting depth of " + MAX_NESTING_DEPTH);
        }
        for (ProgramStatement statement : statements) {
            countBlock(blockCount);
            if (statement instanceof ProgramStatement.SetVariable setVariable) {
                validateValueDepth(setVariable.value(), 0, blockCount);
            } else if (statement instanceof ProgramStatement.DisplayValue displayValue) {
                validateValueDepth(displayValue.value(), 0, blockCount);
            } else if (statement instanceof ProgramStatement.SetRedstone setRedstone) {
                validateValueDepth(setRedstone.power(), 0, blockCount);
            } else if (statement instanceof ProgramStatement.If conditional) {
                validateConditionDepth(conditional.condition(), 0, blockCount);
                validateStatements(conditional.thenBlocks(), depth + 1, blockCount);
            }
        }
    }

    private static void validateValueDepth(ProgramValue value, int depth, int[] blockCount) {
        if (depth > MAX_VALUE_DEPTH) {
            throw new ProgramException("Block inputs exceed maximum depth of " + MAX_VALUE_DEPTH);
        }
        countBlock(blockCount);
        if (value instanceof ArithmeticValue arithmetic) {
            validateValueDepth(arithmetic.left(), depth + 1, blockCount);
            validateValueDepth(arithmetic.right(), depth + 1, blockCount);
        } else if (value instanceof ClampValue clamp) {
            validateValueDepth(clamp.value(), depth + 1, blockCount);
            validateValueDepth(clamp.minimum(), depth + 1, blockCount);
            validateValueDepth(clamp.maximum(), depth + 1, blockCount);
        }
    }

    private static void validateConditionDepth(Condition condition, int depth, int[] blockCount) {
        if (depth > MAX_VALUE_DEPTH) {
            throw new ProgramException("Block inputs exceed maximum depth of " + MAX_VALUE_DEPTH);
        }
        countBlock(blockCount);
        if (condition instanceof ComparisonCondition comparison) {
            validateValueDepth(comparison.left(), depth, blockCount);
            validateValueDepth(comparison.right(), depth, blockCount);
        } else if (condition instanceof NotCondition not) {
            validateConditionDepth(not.condition(), depth + 1, blockCount);
        }
    }

    private static void countBlock(int[] blockCount) {
        if (++blockCount[0] > MAX_BLOCKS) {
            throw new ProgramException("Program exceeds maximum size of " + MAX_BLOCKS + " blocks");
        }
    }

    private static void executeStatements(List<ProgramStatement> statements, ExecutionState state) {
        for (ProgramStatement statement : statements) {
            if (statement instanceof ProgramStatement.SetVariable setVariable) {
                state.variables.put(setVariable.name(), evaluate(setVariable.value(), state, 0));
            } else if (statement instanceof ProgramStatement.DisplayValue displayValue) {
                if (state.displayLines.size() >= MAX_DISPLAY_LINES) {
                    throw new ProgramException("Program can display at most " + MAX_DISPLAY_LINES + " lines");
                }
                state.displayLines.add(displayValue.label() + ": " + format(evaluate(displayValue.value(), state, 0)));
            } else if (statement instanceof ProgramStatement.SetRedstone setRedstone) {
                double power = evaluate(setRedstone.power(), state, 0);
                state.redstonePower = OptionalInt.of((int) Math.round(Math.max(0, Math.min(power, 15))));
            } else if (statement instanceof ProgramStatement.If conditional
                && evaluate(conditional.condition(), state, 0)) {
                executeStatements(conditional.thenBlocks(), state);
            }
        }
    }

    private static double evaluate(ProgramValue value, ExecutionState state, int depth) {
        if (depth > MAX_VALUE_DEPTH) {
            throw new ProgramException("Block inputs exceed maximum depth of " + MAX_VALUE_DEPTH);
        }
        double result;
        if (value instanceof NumberValue number) {
            result = number.value();
        } else if (value instanceof SensorValue sensorValue) {
            Double sensor = state.sensors.get(sensorValue.sensor());
            if (sensor == null) {
                throw new ProgramException("Sensor value is unavailable: " + sensorValue.sensor().id());
            }
            result = sensor;
        } else if (value instanceof VariableValue variableValue) {
            Double variable = state.variables.get(variableValue.name());
            if (variable == null) {
                throw new ProgramException("Variable has not been set: " + variableValue.name());
            }
            result = variable;
        } else if (value instanceof ArithmeticValue arithmetic) {
            double left = evaluate(arithmetic.left(), state, depth + 1);
            double right = evaluate(arithmetic.right(), state, depth + 1);
            result = switch (arithmetic.operator()) {
                case ADD -> left + right;
                case SUBTRACT -> left - right;
                case MULTIPLY -> left * right;
                case DIVIDE -> {
                    if (right == 0) {
                        throw new ProgramException("Division by zero");
                    }
                    yield left / right;
                }
            };
        } else if (value instanceof ClampValue clamp) {
            double input = evaluate(clamp.value(), state, depth + 1);
            double minimum = evaluate(clamp.minimum(), state, depth + 1);
            double maximum = evaluate(clamp.maximum(), state, depth + 1);
            if (minimum > maximum) {
                throw new ProgramException("Clamp minimum must not exceed maximum");
            }
            result = Math.max(minimum, Math.min(input, maximum));
        } else {
            throw new ProgramException("Unsupported value block");
        }
        if (!Double.isFinite(result)) {
            throw new ProgramException("Block calculation did not produce a finite number");
        }
        return result;
    }

    private static boolean evaluate(Condition condition, ExecutionState state, int depth) {
        if (depth > MAX_VALUE_DEPTH) {
            throw new ProgramException("Block inputs exceed maximum depth of " + MAX_VALUE_DEPTH);
        }
        if (condition instanceof NotCondition not) {
            return !evaluate(not.condition(), state, depth + 1);
        }
        if (condition instanceof ComparisonCondition comparison) {
            double left = evaluate(comparison.left(), state, depth + 1);
            double right = evaluate(comparison.right(), state, depth + 1);
            ComparisonOperator operator = comparison.operator();
            return switch (operator) {
                case GREATER_THAN -> left > right;
                case GREATER_THAN_OR_EQUAL -> left >= right;
                case LESS_THAN -> left < right;
                case LESS_THAN_OR_EQUAL -> left <= right;
                case EQUAL -> left == right;
                case NOT_EQUAL -> left != right;
            };
        }
        throw new ProgramException("Unsupported condition block");
    }

    private static String format(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static final class ExecutionState {
        private final Map<Sensor, Double> sensors;
        private final Map<String, Double> variables = new HashMap<>();
        private final List<String> displayLines = new ArrayList<>();
        private OptionalInt redstonePower = OptionalInt.empty();

        private ExecutionState(Map<Sensor, Double> sensors) {
            this.sensors = sensors;
        }
    }
}
