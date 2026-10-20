package dev.kotan.aeronautics_scripting.program;

import java.util.List;
import java.util.Objects;

public sealed interface ProgramStatement permits
    ProgramStatement.SetVariable,
    ProgramStatement.DisplayValue,
    ProgramStatement.SetRedstone,
    ProgramStatement.If {

    record SetVariable(String name, ProgramValue value) implements ProgramStatement {
        public SetVariable {
            new ProgramValue.VariableValue(name);
            Objects.requireNonNull(value, "value");
        }
    }

    record DisplayValue(String label, ProgramValue value) implements ProgramStatement {
        public DisplayValue {
            if (label == null || label.isBlank() || label.length() > 40 || label.indexOf('\n') >= 0) {
                throw new IllegalArgumentException("Display labels must contain 1-40 characters on one line");
            }
            Objects.requireNonNull(value, "value");
        }
    }

    record SetRedstone(ProgramValue power) implements ProgramStatement {
        public SetRedstone {
            Objects.requireNonNull(power, "power");
        }
    }

    record If(ProgramValue.Condition condition, List<ProgramStatement> thenBlocks) implements ProgramStatement {
        public If {
            Objects.requireNonNull(condition, "condition");
            thenBlocks = List.copyOf(thenBlocks);
        }
    }
}
