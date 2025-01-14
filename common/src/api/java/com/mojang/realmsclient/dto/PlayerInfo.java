/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.ValueObject;
import java.util.UUID;
import javax.annotation.Nullable;

public class PlayerInfo
extends ValueObject
implements ReflectionBasedSerialization {
    @SerializedName(value="name")
    @Nullable
    private String name;
    @SerializedName(value="uuid")
    private UUID uuid;
    @SerializedName(value="operator")
    private boolean operator;
    @SerializedName(value="accepted")
    private boolean accepted;
    @SerializedName(value="online")
    private boolean online;

    public String getName() {
        if (this.name == null) {
            return "";
        }
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uUID) {
        this.uuid = uUID;
    }

    public boolean isOperator() {
        return this.operator;
    }

    public void setOperator(boolean bl) {
        this.operator = bl;
    }

    public boolean getAccepted() {
        return this.accepted;
    }

    public void setAccepted(boolean bl) {
        this.accepted = bl;
    }

    public boolean getOnline() {
        return this.online;
    }

    public void setOnline(boolean bl) {
        this.online = bl;
    }
}

