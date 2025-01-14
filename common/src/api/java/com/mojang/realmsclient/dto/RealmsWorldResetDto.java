/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 */
package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import com.mojang.realmsclient.dto.ValueObject;
import java.util.Set;

public class RealmsWorldResetDto
extends ValueObject
implements ReflectionBasedSerialization {
    @SerializedName(value="seed")
    private final String seed;
    @SerializedName(value="worldTemplateId")
    private final long worldTemplateId;
    @SerializedName(value="levelType")
    private final int levelType;
    @SerializedName(value="generateStructures")
    private final boolean generateStructures;
    @SerializedName(value="experiments")
    private final Set<String> experiments;

    public RealmsWorldResetDto(String string, long l, int n, boolean bl, Set<String> set) {
        this.seed = string;
        this.worldTemplateId = l;
        this.levelType = n;
        this.generateStructures = bl;
        this.experiments = set;
    }
}

