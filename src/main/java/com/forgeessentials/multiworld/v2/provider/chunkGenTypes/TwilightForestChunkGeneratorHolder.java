package com.forgeessentials.multiworld.v2.provider.chunkGenTypes;

import java.util.function.Supplier;

import com.forgeessentials.multiworld.v2.provider.ChunkGeneratorHolderBase;
import com.forgeessentials.multiworld.v2.provider.FEChunkGenProvider;
import com.forgeessentials.multiworld.v2.provider.ProvidersReflection;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import ChunkGenerator;

@FEChunkGenProvider(providerName = "twilightforest:featured_noise")
public class TwilightForestChunkGeneratorHolder extends ChunkGeneratorHolderBase {
	@Override
	public ChunkGenerator createChunkGenerator(Registry<Biome> biomes, long seed, BiomeSource biome,
			Supplier<NoiseGeneratorSettings> dimSettings) {
		return ProvidersReflection.getChunkProvider(getClassName(),
				new Class<?>[] { BiomeSource.class, long.class, Supplier.class },
				new Object[] { biome, seed, dimSettings });
	}

	@Override
	public String getClassName() {
		return "twilightforest.world.ChunkGeneratorTwilightForest";
	}
}
