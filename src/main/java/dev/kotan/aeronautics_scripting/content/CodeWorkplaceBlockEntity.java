package dev.kotan.aeronautics_scripting.content;

import dev.kotan.aeronautics_scripting.program.ProgramException;
import dev.kotan.aeronautics_scripting.program.ProgramValue;
import dev.kotan.aeronautics_scripting.script.ScriptEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public final class CodeWorkplaceBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodeWorkplaceBlockEntity.class);
    private static final String PROGRAM_TAG = "Program";
    private static final String REVISION_TAG = "ProgramRevision";
    private static final Map<String, Double> VALIDATION_INPUTS = createValidationInputs();

    private final ScriptEngine scriptEngine = new ScriptEngine();
    private String program = "";
    private long revision;

    public CodeWorkplaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.CODE_WORKPLACE_BLOCK_ENTITY.get(), pos, state);
    }

    public String getProgram() {
        return program;
    }

    public long getRevision() {
        return revision;
    }

    public void validateProgram(String source) {
        scriptEngine.execute(source, VALIDATION_INPUTS);
    }

    public boolean setProgram(String source, long expectedRevision) {
        if (expectedRevision != revision) {
            return false;
        }
        if (source == null || source.length() > ScriptEngine.MAX_SOURCE_LENGTH) {
            throw new ProgramException("Program exceeds the maximum allowed size");
        }
        validateProgram(source);
        program = source;
        revision++;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(PROGRAM_TAG, program);
        tag.putLong(REVISION_TAG, revision);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String savedProgram = tag.getString(PROGRAM_TAG);
        if (savedProgram.length() > ScriptEngine.MAX_SOURCE_LENGTH) {
            LOGGER.warn("Discarding oversized saved program at {}", worldPosition);
            program = "";
            revision = Math.max(0, tag.getLong(REVISION_TAG));
            return;
        }
        try {
            validateProgram(savedProgram);
            program = savedProgram;
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Discarding invalid saved program at {}: {}", worldPosition, exception.getMessage());
            program = "";
        }
        revision = Math.max(0, tag.getLong(REVISION_TAG));
    }

    private static Map<String, Double> createValidationInputs() {
        Map<String, Double> inputs = new java.util.HashMap<>();
        for (ProgramValue.Sensor sensor : ProgramValue.Sensor.values()) {
            inputs.put(sensor.id(), 0.0);
        }
        return Map.copyOf(inputs);
    }
}
