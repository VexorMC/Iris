package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisModelVertexFormats;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderSectionManager.class)
public class MixinRenderSectionManager {
	@ModifyArg(method = "<init>", remap = false,
		at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer;<init>(Lnet/caffeinemc/mods/sodium/client/gl/device/RenderDevice;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;)V"))
	private ChunkVertexType iris$useExtendedVertexFormat$1(ChunkVertexType vertexType) {
		return WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() ? IrisModelVertexFormats.MODEL_VERTEX_XHFP : vertexType;
	}

	@ModifyArg(method = "<init>",
		at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkBuilder;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;)V"))
	private ChunkVertexType iris$useExtendedVertexFormat$2(ChunkVertexType vertexType) {
		return WorldRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat() ? IrisModelVertexFormats.MODEL_VERTEX_XHFP : vertexType;
	}
}
