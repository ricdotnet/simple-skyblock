package dev.ricr.skyblock.utils;

import dev.ricr.skyblock.CustomStructures;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.plugin.Plugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Random;

public class StructureUtils {
    public static void placeStructure(Plugin plugin, Location location, CustomStructures customStructure) {
        try {
            // Get the structure file from resources
            InputStream structureStream = plugin.getResource(customStructure.getLabel());
            if (structureStream == null) {
                plugin.getLogger().severe(String.format("Could not find %s in plugin resources!", customStructure.getLabel()));
                return;
            }

            // Create a temporary file to store the structure
            File tempStructureFile = new File(plugin.getDataFolder(), String.format("temp_%s", customStructure.getLabel()));
            tempStructureFile.getParentFile().mkdirs();

            // Copy the structure from resources to temp file
            Files.copy(structureStream, tempStructureFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            structureStream.close();

            // Get the StructureManager
            StructureManager structureManager = Bukkit.getServer().getStructureManager();

            // Load the structure from the file
            Structure structure = structureManager.loadStructure(tempStructureFile);

            // Place the structure at the location
            // The structure will be placed with the origin at the specified location
            structure.place(
                    location,
                    true, // need the chest with items
                    StructureRotation.NONE,
                    Mirror.NONE,
                    0,
                    1,
                    new Random()
            );

            // Clean up temp file
            tempStructureFile.delete();

        } catch (IOException e) {
            plugin.getLogger().severe("Error loading structure file: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().severe("Error placing structure: " + e.getMessage());
        }
    }
}
