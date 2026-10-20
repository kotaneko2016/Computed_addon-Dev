# Aeronautics Scripting Systems

NeoForge 1.21.1 addon project for Create: Aeronautics / Create: Simulated.

## Current implementation

The project contains the mod entry point, a small, deliberately restricted text-engine prototype, and the first typed visual-program model. Visual programs are represented as connected numeric/condition values and statement blocks, ready for an in-game Scratch-like editor. The initial block runtime supports sensor inputs, variables, arithmetic, comparisons, clamp, conditionals, information lines, and Redstone power output.

The in-game editor, placeable blocks, telemetry collection, display rendering, and Create Redstone Link integration are not wired up yet. In particular, telemetry values must be connected to Aeronautics/Simulated's runtime API before aircraft-specific sensors can work.

The first placeable content is the Code Workplace block. Its 5-by-4 Create Mechanical Crafter recipe uses three Create Redstone Links, two Precision Mechanisms, four Diamond Blocks, two Copper Blocks, and three buttons.

Scripts currently support numeric variables, arithmetic, comparisons, `abs`, `min`, `max`, `clamp`, display lines, and a redstone output from 0 to 15:

```text
let thrust = clamp((altitude - 100) / 10, 0, 15)
screen "Altitude" = altitude
screen "Pitch" = pitch
screen "Speed" = speed
link = thrust
```

Available sensor variables are supplied by the caller when executing a script. Unknown variables, malformed expressions, divide-by-zero, oversized scripts, and invalid outputs produce explicit errors. Script execution does not evaluate Java, JavaScript, Lua, or other arbitrary code.

## Build and test

Use JDK 21 and Gradle:

```text
.\gradlew.bat test
.\gradlew.bat build
```

The local runtime configuration includes Create: Aeronautics, Sable, and Create for the NeoForge development run. The visual program runtime is covered by unit tests and does not execute arbitrary code.
