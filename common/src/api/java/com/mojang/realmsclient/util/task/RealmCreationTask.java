/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.LongRunningTask;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class RealmCreationTask
extends LongRunningTask {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.create.world.wait");
    private final String name;
    private final String motd;
    private final long realmId;

    public RealmCreationTask(long l, String string, String string2) {
        this.realmId = l;
        this.name = string;
        this.motd = string2;
    }

    @Override
    public void run() {
        RealmsClient realmsClient = RealmsClient.create();
        try {
            realmsClient.initializeRealm(this.realmId, this.name, this.motd);
        }
        catch (RealmsServiceException realmsServiceException) {
            LOGGER.error("Couldn't create world", (Throwable)realmsServiceException);
            this.error(realmsServiceException);
        }
        catch (Exception exception) {
            LOGGER.error("Could not create world", (Throwable)exception);
            this.error(exception);
        }
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }
}

