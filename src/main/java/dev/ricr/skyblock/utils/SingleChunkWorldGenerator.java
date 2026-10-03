package dev.ricr.skyblock.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class SingleChunkWorldGenerator extends ChunkGenerator {
  private final int chunkX;
  private final int chunkZ;

  // Takes world (block) coordinates and converts them to chunk coordinates
  public SingleChunkWorldGenerator(int blockX, int blockZ) {
    this.chunkX = blockX >> 4;
    this.chunkZ = blockZ >> 4;
  }

  private boolean isTargetChunk(int x, int z) {
    return x == this.chunkX && z == this.chunkZ;
  }

  @Override
  public boolean shouldGenerateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z) {
    return this.isTargetChunk(x, z);
  }

  @Override
  public boolean shouldGenerateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z) {
    return this.isTargetChunk(x, z);
  }

  @Override
  public boolean shouldGenerateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z) {
    return this.isTargetChunk(x, z);
  }

  @Override
  public boolean shouldGenerateDecorations(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z) {
    return this.isTargetChunk(x, z);
  }

  @Override
  public boolean shouldGenerateMobs(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z) {
    return this.isTargetChunk(x, z);
  }

  @Override
  public boolean shouldGenerateStructures(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z) {
    return this.isTargetChunk(x, z);
  }

  @Override
  public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
    var x = (this.chunkX << 4) + 8;
    var z = (this.chunkZ << 4) + 8;
    return new Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1, z + 0.5);
  }
}
