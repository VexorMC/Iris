/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.LongRunningTask;
import net.minecraft.client.gui.screen.Screen;

public class RealmsLongRunningMcoTickTaskScreen
extends RealmsLongRunningMcoTaskScreen {
    private final LongRunningTask task;

    public RealmsLongRunningMcoTickTaskScreen(Screen screen, LongRunningTask longRunningTask) {
        super(screen, longRunningTask);
        this.task = longRunningTask;
    }

    @Override
    public void tick() {
        super.tick();
        this.task.tick();
    }

    @Override
    protected void cancel() {
        this.task.abortTask();
        super.cancel();
    }
}

