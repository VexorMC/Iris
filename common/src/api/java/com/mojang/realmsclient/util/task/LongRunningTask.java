/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public abstract class LongRunningTask
implements Runnable {
    protected static final int NUMBER_OF_RETRIES = 25;
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean aborted = false;

    protected static void pause(long l) {
        try {
            Thread.sleep(l * 1000L);
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            LOGGER.error("", (Throwable)interruptedException);
        }
    }

    public static void setScreen(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(screen));
    }

    protected void error(Component component) {
        this.abortTask();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new RealmsGenericErrorScreen(component, (Screen)new RealmsMainScreen(new TitleScreen()))));
    }

    protected void error(Exception exception) {
        if (exception instanceof RealmsServiceException) {
            RealmsServiceException realmsServiceException = (RealmsServiceException)exception;
            this.error(realmsServiceException.realmsError.errorMessage());
        } else {
            this.error(Component.literal(exception.getMessage()));
        }
    }

    protected void error(RealmsServiceException realmsServiceException) {
        this.error(realmsServiceException.realmsError.errorMessage());
    }

    public abstract Component getTitle();

    public boolean aborted() {
        return this.aborted;
    }

    public void tick() {
    }

    public void init() {
    }

    public void abortTask() {
        this.aborted = true;
    }
}

