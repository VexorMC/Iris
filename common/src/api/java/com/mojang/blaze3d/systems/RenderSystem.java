/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.common.collect.Queues
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.IntConsumer
 *  javax.annotation.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fStack
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWErrorCallbackI
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.vexor.shaders.BlendFactor;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import net.minecraft.client.render.DiffuseLighting;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

public class RenderSystem {
    private static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
    @Nullable
    private static Thread renderThread;
    private static int MAX_SUPPORTED_TEXTURE_SIZE;
    private static boolean isInInit;
    private static double lastDrawTime;
    private static Matrix4f projectionMatrix;
    private static Matrix4f savedProjectionMatrix;
    private static final Matrix4fStack modelViewStack;
    private static Matrix4f textureMatrix;
    private static final int[] shaderTextures;
    private static final float[] shaderColor;
    private static float shaderGlintAlpha;
    private static final Vector3f[] shaderLightDirections;
    private static float shaderGameTime;
    private static float shaderLineWidth;
    private static String apiDescription;
    private static final AtomicLong pollEventsWaitStart;
    private static final AtomicBoolean pollingEvents;


    public static boolean isOnRenderThread() {
		return true;
    }

    public static void assertOnRenderThreadOrInit() {
    }

    public static void assertOnRenderThread() {
    }


    public static void disableDepthTest() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.disableDepthTest();
    }

    public static void enableDepthTest() {
        GlStateManager.enableDepthTest();
    }

    public static void depthFunc(int n) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.depthFunc(n);
    }

    public static void depthMask(boolean bl) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.depthMask(bl);
    }

    public static void enableBlend() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.enableBlend();
    }

    public static void disableBlend() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.disableBlend();
    }

    public static void blendFunc(BlendFactor.SourceFactor sourceFactor, BlendFactor.DestFactor destFactor) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.blendFunc(sourceFactor.value, destFactor.value);
    }

    public static void blendFunc(int n, int n2) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.blendFunc(n, n2);
    }

    public static void blendFuncSeparate(BlendFactor.SourceFactor sourceFactor, BlendFactor.DestFactor destFactor, BlendFactor.SourceFactor sourceFactor2, BlendFactor.DestFactor destFactor2) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.blendFuncSeparate(sourceFactor.value, destFactor.value, sourceFactor2.value, destFactor2.value);
    }

    public static void blendFuncSeparate(int n, int n2, int n3, int n4) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.blendFuncSeparate(n, n2, n3, n4);
    }

    public static void enableCull() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.enableCull();
    }

    public static void disableCull() {
        RenderSystem.assertOnRenderThread();
        GlStateManager.disableCull();
    }

    public static void polygonOffset(float f, float f2) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.polygonOffset(f, f2);
    }
    public static void activeTexture(int n) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.activeTexture(n);
    }


    public static void deleteTexture(int n) {
        GlStateManager.deleteTexture(n);
    }

    public static void bindTextureForSetup(int n) {
        RenderSystem.bindTexture(n);
    }

    public static void bindTexture(int n) {
        GlStateManager.bindTexture(n);
    }

    public static void viewport(int n, int n2, int n3, int n4) {
        GlStateManager.viewport(n, n2, n3, n4);
    }

    public static void colorMask(boolean bl, boolean bl2, boolean bl3, boolean bl4) {
        RenderSystem.assertOnRenderThread();
        GlStateManager.colorMask(bl, bl2, bl3, bl4);
    }


    public static void clearDepth(double d) {
        GlStateManager.clearDepth(d);
    }

    public static void clearColor(float f, float f2, float f3, float f4) {
        GlStateManager.clearColor(f, f2, f3, f4);
    }

    public static void clear(int n) {
        GlStateManager.clear(n);
    }

    public static void setShaderGlintAlpha(double d) {
        RenderSystem.setShaderGlintAlpha((float)d);
    }

    public static void setShaderGlintAlpha(float f) {
        RenderSystem.assertOnRenderThread();
        shaderGlintAlpha = f;
    }

    public static float getShaderGlintAlpha() {
        RenderSystem.assertOnRenderThread();
        return shaderGlintAlpha;
    }

    public static void setShaderLights(Vector3f vector3f, Vector3f vector3f2) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.shaderLightDirections[0] = vector3f;
        RenderSystem.shaderLightDirections[1] = vector3f2;
    }


    public static void setShaderColor(float f, float f2, float f3, float f4) {
        RenderSystem.assertOnRenderThread();
		GL11.glColor4f(f, f2, f3, f4);
    }

    public static float[] getShaderColor() {
        RenderSystem.assertOnRenderThread();
        return shaderColor;
    }

    public static void drawElements(int n, int n2, int n3) {
        RenderSystem.assertOnRenderThread();
        GL11.glDrawElements(n, n2, n3, 0L);
    }

    public static void lineWidth(float f) {
        RenderSystem.assertOnRenderThread();
        shaderLineWidth = f;
    }

    public static float getShaderLineWidth() {
        RenderSystem.assertOnRenderThread();
        return shaderLineWidth;
    }

    public static void pixelStore(int n, int n2) {
        GL11.glPixelStorei(n, n2);
    }

    public static void readPixels(int n, int n2, int n3, int n4, int n5, int n6, ByteBuffer byteBuffer) {
        RenderSystem.assertOnRenderThread();
        GL11.glReadPixels(n, n2, n3, n4, n5, n6, byteBuffer);
    }

    public static void getString(int n, Consumer<String> consumer) {
        RenderSystem.assertOnRenderThread();
        consumer.accept(GL11.glGetString(n));
    }

    public static String getBackendDescription() {
        return String.format(Locale.ROOT, "LWJGL version %s", Version.getVersion());
    }

    public static String getApiDescription() {
        return apiDescription;
    }

    public static String getCapsString() {
        RenderSystem.assertOnRenderThread();
        return "Using framebuffer using OpenGL 3.2";
    }

    public static void setupDefaultState(int n, int n2, int n3, int n4) {
        GlStateManager.clearDepth(1.0);
        GlStateManager.enableDepthTest();
        GlStateManager.depthFunc(515);
        projectionMatrix.identity();
        savedProjectionMatrix.identity();
        modelViewStack.clear();
        textureMatrix.identity();
        GlStateManager.viewport(n, n2, n3, n4);
    }

    public static int maxSupportedTextureSize() {
        if (MAX_SUPPORTED_TEXTURE_SIZE == -1) {
            RenderSystem.assertOnRenderThreadOrInit();
            int n = GL11.glGetInteger(3379);
            for (int i = Math.max(32768, n); i >= 1024; i >>= 1) {
                GL11.glTexImage2D(32868, 0, 6408, i, i, 0, 6408, 5121, (ByteBuffer) null);
                int n2 = GL11.glGetTexLevelParameteri(32868, 0, 4096);
                if (n2 == 0) continue;
                MAX_SUPPORTED_TEXTURE_SIZE = i;
                return i;
            }
            MAX_SUPPORTED_TEXTURE_SIZE = Math.max(n, 1024);
			System.out.printf("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = %s %n", (Object)MAX_SUPPORTED_TEXTURE_SIZE);
        }
        return MAX_SUPPORTED_TEXTURE_SIZE;
    }

    public static void glBindBuffer(int n, int n2) {
        GL15.glBindBuffer(n, n2);
    }

    public static void glBindVertexArray(int n) {
        GL30.glBindVertexArray(n);
    }

    public static void glBufferData(int n, ByteBuffer byteBuffer, int n2) {
        RenderSystem.assertOnRenderThreadOrInit();
		GL15.glBufferData(n, byteBuffer, n2);
    }

    public static void glDeleteBuffers(int n) {
        RenderSystem.assertOnRenderThread();
		GL15.glDeleteBuffers(n);
    }

    public static void glDeleteVertexArrays(int n) {
        RenderSystem.assertOnRenderThread();
        GL30.glDeleteVertexArrays(n);
    }

    public static void glUniform1i(int n, int n2) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform1i(n, n2);
    }

    public static void glUniform1(int n, IntBuffer intBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform1iv(n, intBuffer);
    }

    public static void glUniform2(int n, IntBuffer intBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform2iv(n, intBuffer);
    }

    public static void glUniform3(int n, IntBuffer intBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform3iv(n, intBuffer);
    }

    public static void glUniform4(int n, IntBuffer intBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform4iv(n, intBuffer);
    }

    public static void glUniform1(int n, FloatBuffer floatBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform1fv(n, floatBuffer);
    }

    public static void glUniform2(int n, FloatBuffer floatBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform2fv(n, floatBuffer);
    }

    public static void glUniform3(int n, FloatBuffer floatBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform3fv(n, floatBuffer);
    }

    public static void glUniform4(int n, FloatBuffer floatBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniform4fv(n, floatBuffer);
    }

    public static void glUniformMatrix2(int n, boolean bl, FloatBuffer floatBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniformMatrix2fv(n, bl, floatBuffer);
    }

    public static void glUniformMatrix3(int n, boolean bl, FloatBuffer floatBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniformMatrix3fv(n, bl, floatBuffer);
    }

    public static void glUniformMatrix4(int n, boolean bl, FloatBuffer floatBuffer) {
        RenderSystem.assertOnRenderThread();
		GL20.glUniformMatrix4fv(n, bl, floatBuffer);
    }


    public static void setupLevelDiffuseLighting(Vector3f vector3f, Vector3f vector3f2) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.setShaderLights(vector3f, vector3f2);
    }

    public static void setupGuiFlatDiffuseLighting(Vector3f vector3f, Vector3f vector3f2) {
        RenderSystem.assertOnRenderThread();
		DiffuseLighting.enable();
    }

    public static void setupGui3DDiffuseLighting(Vector3f vector3f, Vector3f vector3f2) {
        RenderSystem.assertOnRenderThread();
		DiffuseLighting.enableNormally();
    }
    public static void defaultBlendFunc() {
        RenderSystem.blendFuncSeparate(BlendFactor.SourceFactor.SRC_ALPHA, BlendFactor.DestFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.SourceFactor.ONE, BlendFactor.DestFactor.ZERO);
    }

	private static FloatBuffer projMat = BufferUtils.createFloatBuffer(16);

    public static Matrix4f getProjectionMatrix() {
        RenderSystem.assertOnRenderThread();
		GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projMat);
        return new Matrix4f(projMat);
    }

    public static Matrix4f getModelViewMatrix() {
        RenderSystem.assertOnRenderThread();
        return modelViewStack;
    }

    public static Matrix4fStack getModelViewStack() {
        RenderSystem.assertOnRenderThread();
        return modelViewStack;
    }

    public static Matrix4f getTextureMatrix() {
        RenderSystem.assertOnRenderThread();
        return textureMatrix;
    }

    public static void setShaderGameTime(long l, float f) {
        RenderSystem.assertOnRenderThread();
        shaderGameTime = ((float)(l % 24000L) + f) / 24000.0f;
    }

    public static float getShaderGameTime() {
        RenderSystem.assertOnRenderThread();
        return shaderGameTime;
    }

    static {
        MAX_SUPPORTED_TEXTURE_SIZE = -1;
        lastDrawTime = Double.MIN_VALUE;
        projectionMatrix = new Matrix4f();
        savedProjectionMatrix = new Matrix4f();
        modelViewStack = new Matrix4fStack(16);
        textureMatrix = new Matrix4f();
        shaderTextures = new int[12];
        shaderColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        shaderGlintAlpha = 1.0f;
        shaderLightDirections = new Vector3f[2];
        shaderLineWidth = 1.0f;
        apiDescription = "Unknown";
        pollEventsWaitStart = new AtomicLong();
        pollingEvents = new AtomicBoolean(false);
    }
}

