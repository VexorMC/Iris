/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Date;
import java.util.UUID;
import net.minecraft.Util;
import org.slf4j.Logger;

public class PendingInvite
extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    public String invitationId;
    public String realmName;
    public String realmOwnerName;
    public UUID realmOwnerUuid;
    public Date date;

    public static PendingInvite parse(JsonObject jsonObject) {
        PendingInvite pendingInvite = new PendingInvite();
        try {
            pendingInvite.invitationId = JsonUtils.getStringOr("invitationId", jsonObject, "");
            pendingInvite.realmName = JsonUtils.getStringOr("worldName", jsonObject, "");
            pendingInvite.realmOwnerName = JsonUtils.getStringOr("worldOwnerName", jsonObject, "");
            pendingInvite.realmOwnerUuid = JsonUtils.getUuidOr("worldOwnerUuid", jsonObject, Util.NIL_UUID);
            pendingInvite.date = JsonUtils.getDateOr("date", jsonObject);
        }
        catch (Exception exception) {
            LOGGER.error("Could not parse PendingInvite: {}", (Object)exception.getMessage());
        }
        return pendingInvite;
    }
}

