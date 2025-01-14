package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.util.Map;

public class HandRenderer {
	public static final HandRenderer INSTANCE = new HandRenderer();

	private boolean ACTIVE;
	private boolean renderingSolid;
	public static final float DEPTH = 0.125F;

	private void setupGlState(GameRenderer gameRenderer, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();

		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		float f = 0.07F;

		Project.gluPerspective(gameRenderer.getFov(tickDelta, false), (float)client.width / (float)client.height, 0.05F, gameRenderer.viewDistance * 2.0F);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();

		GlStateManager.pushMatrix();
		gameRenderer.bobViewWhenHurt(tickDelta);
		if (client.options.bobView) {
			gameRenderer.bobView(tickDelta);
		}


		GlStateManager.popMatrix();
	}

	private boolean canNotRender() {
		MinecraftClient mc = MinecraftClient.getInstance();

		return mc.options.perspective != 0 ||
			mc.player.isSleeping() ||
			mc.options.hudHidden ||
			mc.player.isSpectator();
	}

	public boolean isHandTranslucent() {
		ItemStack heldItem = MinecraftClient.getInstance().player.getStackInHand();
		if (heldItem == null) {
			return false;
		}
		Item item = heldItem.getItem();

		if (item instanceof BlockItem itemBlock) {
			return itemBlock.getBlock().getRenderLayerType() == RenderLayer.TRANSLUCENT;
		}

		return false;
	}

	public boolean isAnyHandTranslucent() {
		return isHandTranslucent();
	}

	public void renderSolid(float tickDelta, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
		if (!canNotRender() || IrisApi.getInstance().isShaderPackInUse()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();

		ACTIVE = true;

		pipeline.setPhase(WorldRenderingPhase.HAND_SOLID);

		GL11.glPushMatrix();
		GL11.glDepthMask(true); // actually write to the depth buffer, it's normally disabled at this point

		client.profiler.push("iris_hand");

		setupGlState(gameRenderer, tickDelta);

		renderingSolid = true;

		gameRenderer.enableLightmap();
		gameRenderer.firstPersonRenderer.renderArmHoldingItem(tickDelta);
		gameRenderer.disableLightmap();

		RenderSystem.defaultBlendFunc();
		GL11.glDepthMask(false);
		GL11.glPopMatrix();

		client.profiler.pop();

		resetProjectionMatrix();

		renderingSolid = false;

		pipeline.setPhase(WorldRenderingPhase.NONE);

		ACTIVE = false;
	}


	// TODO: RenderType
	public void renderTranslucent(float tickDelta, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
		if (!canNotRender() || !isAnyHandTranslucent() || IrisApi.getInstance().isShaderPackInUse()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();

		ACTIVE = true;

		pipeline.setPhase(WorldRenderingPhase.HAND_TRANSLUCENT);

		GL11.glPushMatrix();

		client.profiler.push("iris_hand_translucent");

		setupGlState(gameRenderer, tickDelta);

		gameRenderer.enableLightmap();
		gameRenderer.firstPersonRenderer.renderArmHoldingItem(tickDelta);
		gameRenderer.disableLightmap();

		GL11.glPopMatrix();

		resetProjectionMatrix();

		client.profiler.pop();

		pipeline.setPhase(WorldRenderingPhase.NONE);

		ACTIVE = false;
	}

	private void resetProjectionMatrix() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glMultMatrixf(CapturedRenderingState.INSTANCE.getGbufferProjection().get(new float[16]));
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}

	public boolean isActive() {
		return ACTIVE;
	}

	public boolean isRenderingSolid() {
		return renderingSolid;
	}
}
