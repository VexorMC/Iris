/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.client.worldupload;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public abstract class RealmsUploadException
extends RuntimeException {
    @Nullable
    public Component getStatusMessage() {
        return null;
    }

    @Nullable
    public Component[] getErrorMessages() {
        return null;
    }
}

