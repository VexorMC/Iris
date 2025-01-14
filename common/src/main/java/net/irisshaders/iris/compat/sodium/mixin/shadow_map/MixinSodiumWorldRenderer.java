package net.irisshaders.iris.compat.sodium.mixin.shadow_map;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedSet;

/**
 * Ensures that the state of the chunk render visibility graph gets properly swapped when in the shadow map pass,
 * because we must maintain one visibility graph for the shadow camera and one visibility graph for the player camera.
 * <p>
 * Also ensures that the visibility graph is always rebuilt in the shadow pass, since the shadow camera is generally
 * always moving.
 */
@Mixin(SodiumWorldRenderer.class)
public abstract class MixinSodiumWorldRenderer {
	private static int beList = 0;
	private static boolean renderLightsOnly;

	static {
		ShadowRenderingState.setBlockEntityRenderFunction((shadowRenderer, bufferSource, modelView, camera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, lightsOnly) -> {
			// This isn't thread safe - too bad!

			renderLightsOnly = lightsOnly;

			((SodiumWorldRendererAccessor) SodiumWorldRenderer.instance()).invokeRenderBlockEntities(modelView, Minecraft.getInstance().renderBuffers(), Long2ObjectMaps.emptyMap(), tickDelta, bufferSource, cameraX, cameraY, cameraZ, Minecraft.getInstance().getBlockEntityRenderDispatcher());
			((SodiumWorldRendererAccessor) SodiumWorldRenderer.instance()).invokeRenderGlobalBlockEntities(modelView, Minecraft.getInstance().renderBuffers(), Long2ObjectMaps.emptyMap(), tickDelta, bufferSource, cameraX, cameraY, cameraZ, Minecraft.getInstance().getBlockEntityRenderDispatcher());

			renderLightsOnly = false;

			return beList;
		});
	}

	@Shadow
	private static void renderBlockEntity(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, float tickDelta, MultiBufferSource.BufferSource immediate, double x, double y, double z, BlockEntityRenderDispatcher dispatcher, BlockEntity entity) {
		throw new IllegalStateException("maybe get Mixin?");
	}

	@Inject(method = "renderGlobalBlockEntities", remap = false, at = @At("HEAD"))
	private void resetEntityList(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, float tickDelta, MultiBufferSource.BufferSource immediate, double x, double y, double z, BlockEntityRenderDispatcher blockEntityRenderer, CallbackInfo ci) {
		beList = 0;
	}

	@Inject(method = "renderBlockEntity", at = @At("HEAD"), remap = false, cancellable = true)
	private static void addToList(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, float tickDelta, MultiBufferSource.BufferSource immediate, double h, double y, double z, BlockEntityRenderDispatcher dispatcher, BlockEntity x, CallbackInfo ci) {
		if (renderLightsOnly && x.getBlockState().getLightEmission() <= 0) {
			ci.cancel();
		}

		beList++;
	}

	@Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true, remap = false)
	private void iris$overrideEntityCulling(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) cir.setReturnValue(true);
	}

	@Redirect(method = "setupTerrain", remap = false,
		at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;needsUpdate()Z",
			remap = false))
	private boolean iris$forceChunkGraphRebuildInShadowPass(RenderSectionManager instance) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			// TODO: Detect when the sun/moon isn't moving
			return true;
		} else {
			return instance.needsUpdate();
		}
	}
}
