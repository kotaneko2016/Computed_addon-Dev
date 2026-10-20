package dev.kotan.aeronautics_scripting.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CodeWorkplaceRecipeTest {
    @Test
    void definesTheExpectedCreateMechanicalCraftingRecipe() throws IOException {
        try (InputStream stream = getClass().getClassLoader()
            .getResourceAsStream("data/aeronautics_scripting/recipe/mechanical_crafting/code_workplace.json")) {
            assertNotNull(stream, "Code Workplace recipe resource must exist");

            JsonObject recipe = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
            assertEquals("create:mechanical_crafting", recipe.get("type").getAsString());
            assertEquals("aeronautics_scripting:code_workplace",
                recipe.getAsJsonObject("result").get("id").getAsString());

            JsonArray pattern = recipe.getAsJsonArray("pattern");
            assertEquals(4, pattern.size());
            for (int index = 0; index < pattern.size(); index++) {
                assertEquals(5, pattern.get(index).getAsString().length());
            }
            assertEquals(" RRR ", pattern.get(0).getAsString());
            assertEquals("MDCDM", pattern.get(1).getAsString());
            assertEquals("BBBDD", pattern.get(2).getAsString());
            assertEquals("  C  ", pattern.get(3).getAsString());

            JsonObject key = recipe.getAsJsonObject("key");
            assertEquals("create:redstone_link", key.getAsJsonObject("R").get("item").getAsString());
            assertEquals("create:precision_mechanism", key.getAsJsonObject("M").get("item").getAsString());
            assertEquals("minecraft:diamond_block", key.getAsJsonObject("D").get("item").getAsString());
            assertEquals("minecraft:copper_block", key.getAsJsonObject("C").get("item").getAsString());
            assertEquals("minecraft:buttons", key.getAsJsonObject("B").get("tag").getAsString());
            assertEquals(Map.of("R", 3, "M", 2, "D", 4, "C", 2, "B", 3), countIngredients(pattern));
        }
    }

    private static Map<String, Integer> countIngredients(JsonArray pattern) {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int row = 0; row < pattern.size(); row++) {
            String line = pattern.get(row).getAsString();
            for (int column = 0; column < line.length(); column++) {
                char symbol = line.charAt(column);
                if (symbol != ' ') {
                    counts.merge(String.valueOf(symbol), 1, Integer::sum);
                }
            }
        }
        return Map.copyOf(counts);
    }
}
