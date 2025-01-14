/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSettings;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;

public class RealmsWorldOptions
extends ValueObject {
    public final boolean pvp;
    public final boolean spawnMonsters;
    public final int spawnProtection;
    public final boolean commandBlocks;
    public final boolean forceGameMode;
    public final int difficulty;
    public final int gameMode;
    public final boolean hardcore;
    private final String slotName;
    public final String version;
    public final RealmsServer.Compatibility compatibility;
    public long templateId;
    @Nullable
    public String templateImage;
    public boolean empty;
    private static final boolean DEFAULT_FORCE_GAME_MODE = false;
    private static final boolean DEFAULT_PVP = true;
    private static final boolean DEFAULT_SPAWN_MONSTERS = true;
    private static final int DEFAULT_SPAWN_PROTECTION = 0;
    private static final boolean DEFAULT_COMMAND_BLOCKS = false;
    private static final int DEFAULT_DIFFICULTY = 2;
    private static final int DEFAULT_GAME_MODE = 0;
    private static final boolean DEFAULT_HARDCORE_MODE = false;
    private static final String DEFAULT_SLOT_NAME = "";
    private static final String DEFAULT_VERSION = "";
    private static final RealmsServer.Compatibility DEFAULT_COMPATIBILITY = RealmsServer.Compatibility.UNVERIFIABLE;
    private static final long DEFAULT_TEMPLATE_ID = -1L;
    private static final String DEFAULT_TEMPLATE_IMAGE = null;

    public RealmsWorldOptions(boolean bl, boolean bl2, int n, boolean bl3, int n2, int n3, boolean bl4, boolean bl5, String string, String string2, RealmsServer.Compatibility compatibility) {
        this.pvp = bl;
        this.spawnMonsters = bl2;
        this.spawnProtection = n;
        this.commandBlocks = bl3;
        this.difficulty = n2;
        this.gameMode = n3;
        this.hardcore = bl4;
        this.forceGameMode = bl5;
        this.slotName = string;
        this.version = string2;
        this.compatibility = compatibility;
    }

    public static RealmsWorldOptions createDefaults() {
        return new RealmsWorldOptions(true, true, 0, false, 2, 0, false, false, "", "", DEFAULT_COMPATIBILITY);
    }

    public static RealmsWorldOptions createDefaultsWith(GameType gameType, Difficulty difficulty, boolean bl, String string, String string2) {
        return new RealmsWorldOptions(true, true, 0, false, difficulty.getId(), gameType.getId(), bl, false, string2, string, DEFAULT_COMPATIBILITY);
    }

    public static RealmsWorldOptions createFromSettings(LevelSettings levelSettings, String string) {
        return RealmsWorldOptions.createDefaultsWith(levelSettings.gameType(), levelSettings.difficulty(), levelSettings.hardcore(), string, levelSettings.levelName());
    }

    public static RealmsWorldOptions createEmptyDefaults() {
        RealmsWorldOptions realmsWorldOptions = RealmsWorldOptions.createDefaults();
        realmsWorldOptions.setEmpty(true);
        return realmsWorldOptions;
    }

    public void setEmpty(boolean bl) {
        this.empty = bl;
    }

    public static RealmsWorldOptions parse(JsonObject jsonObject, RealmsSettings realmsSettings) {
        RealmsWorldOptions realmsWorldOptions = new RealmsWorldOptions(JsonUtils.getBooleanOr("pvp", jsonObject, true), JsonUtils.getBooleanOr("spawnMonsters", jsonObject, true), JsonUtils.getIntOr("spawnProtection", jsonObject, 0), JsonUtils.getBooleanOr("commandBlocks", jsonObject, false), JsonUtils.getIntOr("difficulty", jsonObject, 2), JsonUtils.getIntOr("gameMode", jsonObject, 0), realmsSettings.hardcore(), JsonUtils.getBooleanOr("forceGameMode", jsonObject, false), JsonUtils.getRequiredStringOr("slotName", jsonObject, ""), JsonUtils.getRequiredStringOr("version", jsonObject, ""), RealmsServer.getCompatibility(JsonUtils.getRequiredStringOr("compatibility", jsonObject, RealmsServer.Compatibility.UNVERIFIABLE.name())));
        realmsWorldOptions.templateId = JsonUtils.getLongOr("worldTemplateId", jsonObject, -1L);
        realmsWorldOptions.templateImage = JsonUtils.getStringOr("worldTemplateImage", jsonObject, DEFAULT_TEMPLATE_IMAGE);
        return realmsWorldOptions;
    }

    public String getSlotName(int n) {
        if (StringUtil.isBlank(this.slotName)) {
            if (this.empty) {
                return I18n.get("mco.configure.world.slot.empty", new Object[0]);
            }
            return this.getDefaultSlotName(n);
        }
        return this.slotName;
    }

    public String getDefaultSlotName(int n) {
        return I18n.get("mco.configure.world.slot", n);
    }

    public String toJson() {
        JsonObject jsonObject = new JsonObject();
        if (!this.pvp) {
            jsonObject.addProperty("pvp", Boolean.valueOf(this.pvp));
        }
        if (!this.spawnMonsters) {
            jsonObject.addProperty("spawnMonsters", Boolean.valueOf(this.spawnMonsters));
        }
        if (this.spawnProtection != 0) {
            jsonObject.addProperty("spawnProtection", (Number)this.spawnProtection);
        }
        if (this.commandBlocks) {
            jsonObject.addProperty("commandBlocks", Boolean.valueOf(this.commandBlocks));
        }
        if (this.difficulty != 2) {
            jsonObject.addProperty("difficulty", (Number)this.difficulty);
        }
        if (this.gameMode != 0) {
            jsonObject.addProperty("gameMode", (Number)this.gameMode);
        }
        if (this.hardcore) {
            jsonObject.addProperty("hardcore", Boolean.valueOf(this.hardcore));
        }
        if (this.forceGameMode) {
            jsonObject.addProperty("forceGameMode", Boolean.valueOf(this.forceGameMode));
        }
        if (!Objects.equals(this.slotName, "")) {
            jsonObject.addProperty("slotName", this.slotName);
        }
        if (!Objects.equals(this.version, "")) {
            jsonObject.addProperty("version", this.version);
        }
        if (this.compatibility != DEFAULT_COMPATIBILITY) {
            jsonObject.addProperty("compatibility", this.compatibility.name());
        }
        return jsonObject.toString();
    }

    public RealmsWorldOptions clone() {
        return new RealmsWorldOptions(this.pvp, this.spawnMonsters, this.spawnProtection, this.commandBlocks, this.difficulty, this.gameMode, this.hardcore, this.forceGameMode, this.slotName, this.version, this.compatibility);
    }

    public /* synthetic */ Object clone() throws CloneNotSupportedException {
        return this.clone();
    }
}

