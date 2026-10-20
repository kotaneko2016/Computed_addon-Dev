package dev.kotan.aeronautics_scripting.program;

import java.util.Objects;
import java.util.regex.Pattern;

public sealed interface ProgramValue permits
    ProgramValue.NumberValue,
    ProgramValue.SensorValue,
    ProgramValue.VariableValue,
    ProgramValue.ArithmeticValue,
    ProgramValue.ClampValue {

    Pattern VARIABLE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,31}");

    enum Sensor {
        ALTITUDE("altitude"),
        PITCH("pitch"),
        ROLL("roll"),
        SPEED("speed"),
        REDSTONE("redstone"),
        POSITION_X("position_x"),
        POSITION_Y("position_y"),
        POSITION_Z("position_z");

        private final String id;

        Sensor(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    enum ArithmeticOperator {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE
    }

    enum ComparisonOperator {
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        EQUAL,
        NOT_EQUAL
    }

    record NumberValue(double value) implements ProgramValue {
        public NumberValue {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Number blocks must contain a finite value");
            }
        }
    }

    record SensorValue(Sensor sensor) implements ProgramValue {
        public SensorValue {
            Objects.requireNonNull(sensor, "sensor");
        }
    }

    record VariableValue(String name) implements ProgramValue {
        public VariableValue {
            validateVariableName(name);
        }
    }

    record ArithmeticValue(ArithmeticOperator operator, ProgramValue left, ProgramValue right)
        implements ProgramValue {
        public ArithmeticValue {
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }
    }

    record ClampValue(ProgramValue value, ProgramValue minimum, ProgramValue maximum) implements ProgramValue {
        public ClampValue {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(minimum, "minimum");
            Objects.requireNonNull(maximum, "maximum");
        }
    }

    sealed interface Condition permits ComparisonCondition, NotCondition {
    }

    record ComparisonCondition(
        ComparisonOperator operator,
        ProgramValue left,
        ProgramValue right
    ) implements Condition {
        public ComparisonCondition {
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }
    }

    record NotCondition(Condition condition) implements Condition {
        public NotCondition {
            Objects.requireNonNull(condition, "condition");
        }
    }

    private static void validateVariableName(String name) {
        if (name == null || !VARIABLE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Variable names must contain 1-32 letters, digits, or underscores");
        }
    }
}
