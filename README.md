# Aeronautics Scripting Systems

NeoForge 1.21.1 addon project for Create: Aeronautics / Create: Simulated.

## Current implementation

The project contains the mod entry point, a small, deliberately restricted text-engine prototype, and the first typed visual-program model. The Code Workplace opens a basic in-game program editor with templates for altitude, speed, pitch, and a redstone-link output, plus a one-line command input, remove-last, and save controls. Programs are stored in the block entity with a revision number to reject stale saves.

This is an initial command-list editor, not yet a draggable Scratch-style editor. The editor validates scripts locally and the server validates them again before storing them; saves also check interaction permission, distance, loaded chunk, target block, and revision. The script engine is deliberately restricted and does not evaluate arbitrary Java, JavaScript, Lua, PHP, or other code.

Telemetry collection, display rendering, and Create Redstone Link integration are not wired up yet. Sensor values currently use placeholder zeroes during validation and must be connected to Aeronautics/Simulated's runtime API before aircraft-specific sensors or outputs can work. Program data is stored in the block entity; schematic save/copy behavior has not been implemented or verified.

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

The local runtime configuration includes Create: Aeronautics, Sable, and Create for the NeoForge development run. The script engine and visual program runtime are covered by unit tests and do not execute arbitrary code.
