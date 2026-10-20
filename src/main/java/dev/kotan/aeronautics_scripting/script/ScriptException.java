package dev.kotan.aeronautics_scripting.script;

public final class ScriptException extends IllegalArgumentException {
    private final int line;

    public ScriptException(int line, String message) {
        super("Line " + line + ": " + message);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
