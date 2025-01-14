/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.lwjgl.system.MemoryUtil
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public class RealmsTextureManager {
    private static final Map<String, RealmsTexture> TEXTURES = Maps.newHashMap();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TEMPLATE_ICON_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/presets/isles.png");

    public static ResourceLocation worldTemplate(String string, @Nullable String string2) {
        if (string2 == null) {
            return TEMPLATE_ICON_LOCATION;
        }
        return RealmsTextureManager.getTexture(string, string2);
    }

    private static ResourceLocation getTexture(String string, String string2) {
        RealmsTexture realmsTexture = TEXTURES.get(string);
        if (realmsTexture != null && realmsTexture.image().equals(string2)) {
            return realmsTexture.textureId;
        }
        NativeImage nativeImage = RealmsTextureManager.loadImage(string2);
        if (nativeImage == null) {
            ResourceLocation resourceLocation = MissingTextureAtlasSprite.getLocation();
            TEXTURES.put(string, new RealmsTexture(string2, resourceLocation));
            return resourceLocation;
        }
        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("realms", "dynamic/" + string);
        Minecraft.getInstance().getTextureManager().register(resourceLocation, new DynamicTexture(nativeImage));
        TEXTURES.put(string, new RealmsTexture(string2, resourceLocation));
        return resourceLocation;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    private static NativeImage loadImage(String string) {
        byte[] byArray = Base64.getDecoder().decode(string);
        ByteBuffer byteBuffer = MemoryUtil.memAlloc((int)byArray.length);
        try {
            NativeImage nativeImage = NativeImage.read(byteBuffer.put(byArray).flip());
            return nativeImage;
        }
        catch (IOException iOException) {
            LOGGER.warn("Failed to load world image: {}", (Object)string, (Object)iOException);
        }
        finally {
            MemoryUtil.memFree((Buffer)byteBuffer);
        }
        return null;
    }

    public static final class RealmsTexture
    extends Record {
        private final String image;
        final ResourceLocation textureId;

        public RealmsTexture(String string, ResourceLocation resourceLocation) {
            this.image = string;
            this.textureId = resourceLocation;
        }

        @Override
        public final String toString() {
            return ObjectMethods.bootstrap("toString", new MethodHandle[]{RealmsTexture.class, "image;textureId", "image", "textureId"}, this);
        }

        @Override
        public final int hashCode() {
            return (int)ObjectMethods.bootstrap("hashCode", new MethodHandle[]{RealmsTexture.class, "image;textureId", "image", "textureId"}, this);
        }

        @Override
        public final boolean equals(Object object) {
            return (boolean)ObjectMethods.bootstrap("equals", new MethodHandle[]{RealmsTexture.class, "image;textureId", "image", "textureId"}, this, object);
        }

        public String image() {
            return this.image;
        }

        public ResourceLocation textureId() {
            return this.textureId;
        }
    }
}

