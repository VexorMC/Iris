/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.realmsclient.util.task;

import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.task.ResettingWorldTask;
import net.minecraft.network.chat.Component;

public class ResettingTemplateWorldTask
extends ResettingWorldTask {
    private final WorldTemplate template;

    public ResettingTemplateWorldTask(WorldTemplate worldTemplate, long l, Component component, Runnable runnable) {
        super(l, component, runnable);
        this.template = worldTemplate;
    }

    @Override
    protected void sendResetRequest(RealmsClient realmsClient, long l) throws RealmsServiceException {
        realmsClient.resetWorldWithTemplate(l, this.template.id);
    }
}

