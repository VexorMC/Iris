package net.coderbot.iris.pipeline;

import net.coderbot.iris.layer.GbufferProgram;
import net.coderbot.iris.mixin.LevelRendererAccessor;
import net.coderbot.iris.uniforms.FrameUpdateNotifier;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;

import java.util.List;
import java.util.OptionalInt;

public interface WorldRenderingPipeline {
	void beginLevelRendering();
	void renderShadows(LevelRendererAccessor levelRenderer, Camera camera);
	void addDebugText(List<String> messages);
	OptionalInt getForcedShadowRenderDistanceChunksForDisplay();

	WorldRenderingPhase getPhase();

	void setPhase(WorldRenderingPhase phase);
	void setAtlas(TextureAtlas atlas);
	void setSimpleTexture(SimpleTexture texture);

	void beginShadowRender();
	void endShadowRender();

	void beginHand();

	void beginTranslucents();
	void pushProgram(GbufferProgram program);
	void popProgram(GbufferProgram program);
	void finalizeLevelRendering();
	void destroy();

	SodiumTerrainPipeline getSodiumTerrainPipeline();
	FrameUpdateNotifier getFrameUpdateNotifier();

	boolean shouldDisableVanillaEntityShadows();
	boolean shouldDisableDirectionalShading();
	boolean shouldRenderClouds();
	boolean shouldRenderUnderwaterOverlay();
	boolean shouldRenderVignette();
	boolean shouldWriteRainAndSnowToDepthBuffer();
	boolean shouldRenderParticlesBeforeDeferred();

	float getSunPathRotation();
}
