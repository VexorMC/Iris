/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import javax.annotation.Nullable;

public class ServerActivity
extends ValueObject {
    @Nullable
    public String profileUuid;
    public long joinTime;
    public long leaveTime;

    public static ServerActivity parse(JsonObject jsonObject) {
        ServerActivity serverActivity = new ServerActivity();
        try {
            serverActivity.profileUuid = JsonUtils.getStringOr("profileUuid", jsonObject, null);
            serverActivity.joinTime = JsonUtils.getLongOr("joinTime", jsonObject, Long.MIN_VALUE);
            serverActivity.leaveTime = JsonUtils.getLongOr("leaveTime", jsonObject, Long.MIN_VALUE);
        }
        catch (Exception exception) {
            // empty catch block
        }
        return serverActivity;
    }
}

