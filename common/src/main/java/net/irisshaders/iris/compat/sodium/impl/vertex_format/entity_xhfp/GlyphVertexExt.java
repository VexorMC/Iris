package net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.NormalHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class GlyphVertexExt {
	public static final VertexFormatDescription FORMAT = VertexFormatRegistry.instance().get(IrisVertexFormats.GLYPH);
	public static final int STRIDE = IrisVertexFormats.GLYPH.getVertexSize();

	private static final int OFFSET_POSITION = 0;
	private static final int OFFSET_COLOR = 12;
	private static final int OFFSET_TEXTURE = 16;
	private static final int OFFSET_MID_TEXTURE = 38;
	private static final int OFFSET_LIGHT = 24;
	private static final int OFFSET_NORMAL = 28;
	private static final int OFFSET_TANGENT = 46;


	private static final QuadViewEntity.QuadViewEntityUnsafe quad = new QuadViewEntity.QuadViewEntityUnsafe();
	private static final Vector3f saveNormal = new Vector3f();
	private static final Vector3f lastNormal = new Vector3f();
	private static final QuadViewEntity.QuadViewEntityUnsafe quadView = new QuadViewEntity.QuadViewEntityUnsafe();
	private static int vertexCount;
	private static float uSum;
	private static float vSum;

	public static void write(long ptr, float x, float y, float z, int color, float u, float v, int light) {
		long i = ptr;

		vertexCount++;
		uSum += u;
		vSum += v;

		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION, x);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

		MemoryUtil.memPutInt(ptr + OFFSET_COLOR, color);

		MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE, u);
		MemoryUtil.memPutFloat(ptr + OFFSET_TEXTURE + 4, v);

		MemoryUtil.memPutInt(ptr + OFFSET_LIGHT, light);

		MemoryUtil.memPutShort(ptr + 32, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
		MemoryUtil.memPutShort(ptr + 34, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
		MemoryUtil.memPutShort(ptr + 36, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());

		if (vertexCount == 4) {
			endQuad(ptr);
		}
	}

	private static void endQuad(long ptr) {
		vertexCount = 0;

		uSum *= 0.25;
		vSum *= 0.25;

		quad.setup(ptr, STRIDE);

		float normalX, normalY, normalZ;

		NormalHelper.computeFaceNormal(saveNormal, quad);
		normalX = saveNormal.x;
		normalY = saveNormal.y;
		normalZ = saveNormal.z;
		int normal = NormI8.pack(saveNormal);

		int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ, quad);

		for (long vertex = 0; vertex < 4; vertex++) {
			MemoryUtil.memPutFloat(ptr + OFFSET_MID_TEXTURE - STRIDE * vertex, uSum);
			MemoryUtil.memPutFloat(ptr + (OFFSET_MID_TEXTURE + 4) - STRIDE * vertex, vSum);
			MemoryUtil.memPutInt(ptr + OFFSET_NORMAL - STRIDE * vertex, normal);
			MemoryUtil.memPutInt(ptr + OFFSET_TANGENT - STRIDE * vertex, tangent);
		}

		uSum = 0;
		vSum = 0;
	}

	public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int light, int color) {
		Matrix3f matNormal = matrices.normal();
		Matrix4f matPosition = matrices.pose();

		try (MemoryStack stack = MemoryStack.stackPush()) {
			long buffer = stack.nmalloc(4 * STRIDE);
			long ptr = buffer;

			// The packed normal vector
			var n = quad.getLightFace().step();
			float nx = n.x;
			float ny = n.y;
			float nz = n.z;

			// The transformed normal vector
			float nxt = (matNormal.m00() * nx) + (matNormal.m10() * ny) + (matNormal.m20() * nz);
			float nyt = (matNormal.m01() * nx) + (matNormal.m11() * ny) + (matNormal.m21() * nz);
			float nzt = (matNormal.m02() * nx) + (matNormal.m12() * ny) + (matNormal.m22() * nz);

			// The packed transformed normal vector
			var nt = NormI8.pack(nxt, nyt, nzt);

			for (int i = 0; i < 4; i++) {
				// The position vector
				float x = quad.getX(i);
				float y = quad.getY(i);
				float z = quad.getZ(i);

				// The transformed position vector
				float xt = (matPosition.m00() * x) + (matPosition.m10() * y) + (matPosition.m20() * z) + matPosition.m30();
				float yt = (matPosition.m01() * x) + (matPosition.m11() * y) + (matPosition.m21() * z) + matPosition.m31();
				float zt = (matPosition.m02() * x) + (matPosition.m12() * y) + (matPosition.m22() * z) + matPosition.m32();

				write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), light);
				ptr += STRIDE;
			}

			endQuad(ptr - STRIDE, nxt, nyt, nzt);

			writer.push(stack, buffer, 4, FORMAT);
		}
	}

	private static void endQuad(long ptr, float normalX, float normalY, float normalZ) {
		quadView.setup(ptr, STRIDE);

		int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ, quadView);

		for (long vertex = 0; vertex < 4; vertex++) {
			MemoryUtil.memPutInt(ptr + 44 - STRIDE * vertex, tangent);
		}
	}

	public static void computeFaceNormal(Vector3f saveTo, ModelQuadView q) {
//		final Direction nominalFace = q.nominalFace();
//
//		if (GeometryHelper.isQuadParallelToFace(nominalFace, q)) {
//			Vec3i vec = nominalFace.getVector();
//			saveTo.set(vec.getX(), vec.getY(), vec.getZ());
//			return;
//		}

		final float x0 = q.getX(0);
		final float y0 = q.getY(0);
		final float z0 = q.getZ(0);
		final float x1 = q.getX(1);
		final float y1 = q.getY(1);
		final float z1 = q.getZ(1);
		final float x2 = q.getX(2);
		final float y2 = q.getY(2);
		final float z2 = q.getZ(2);
		final float x3 = q.getX(3);
		final float y3 = q.getY(3);
		final float z3 = q.getZ(3);

		final float dx0 = x2 - x0;
		final float dy0 = y2 - y0;
		final float dz0 = z2 - z0;
		final float dx1 = x3 - x1;
		final float dy1 = y3 - y1;
		final float dz1 = z3 - z1;

		float normX = dy0 * dz1 - dz0 * dy1;
		float normY = dz0 * dx1 - dx0 * dz1;
		float normZ = dx0 * dy1 - dy0 * dx1;

		float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

		if (l != 0) {
			normX /= l;
			normY /= l;
			normZ /= l;
		}

		saveTo.set(normX, normY, normZ);
	}
}
