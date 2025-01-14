/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.yggdrasil.ProfileResult
 */
package com.mojang.realmsclient.util;

import com.mojang.authlib.yggdrasil.ProfileResult;
import java.util.Date;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;

public class RealmsUtil {
    private static final Component RIGHT_NOW = Component.translatable("mco.util.time.now");
    private static final int MINUTES = 60;
    private static final int HOURS = 3600;
    private static final int DAYS = 86400;

    public static Component convertToAgePresentation(long l) {
        if (l < 0L) {
            return RIGHT_NOW;
        }
        long l2 = l / 1000L;
        if (l2 < 60L) {
            return Component.translatable("mco.time.secondsAgo", l2);
        }
        if (l2 < 3600L) {
            long l3 = l2 / 60L;
            return Component.translatable("mco.time.minutesAgo", l3);
        }
        if (l2 < 86400L) {
            long l4 = l2 / 3600L;
            return Component.translatable("mco.time.hoursAgo", l4);
        }
        long l5 = l2 / 86400L;
        return Component.translatable("mco.time.daysAgo", l5);
    }

    public static Component convertToAgePresentationFromInstant(Date date) {
        return RealmsUtil.convertToAgePresentation(System.currentTimeMillis() - date.getTime());
    }

    public static void renderPlayerFace(GuiGraphics guiGraphics, int n, int n2, int n3, UUID uUID) {
        Minecraft minecraft = Minecraft.getInstance();
        ProfileResult profileResult = minecraft.getMinecraftSessionService().fetchProfile(uUID, false);
        PlayerSkin playerSkin = profileResult != null ? minecraft.getSkinManager().getInsecureSkin(profileResult.profile()) : DefaultPlayerSkin.get(uUID);
        PlayerFaceRenderer.draw(guiGraphics, playerSkin, n, n2, n3);
    }
}

