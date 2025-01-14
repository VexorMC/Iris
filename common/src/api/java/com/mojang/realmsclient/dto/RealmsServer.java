/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ComparisonChain
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Maps
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.apache.commons.lang3.builder.EqualsBuilder
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsSettings;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ServerData;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;

public class RealmsServer
extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NO_VALUE = -1;
    public long id;
    @Nullable
    public String remoteSubscriptionId;
    @Nullable
    public String name;
    public String motd;
    public State state;
    @Nullable
    public String owner;
    public UUID ownerUUID = Util.NIL_UUID;
    public List<PlayerInfo> players;
    public Map<Integer, RealmsWorldOptions> slots;
    public boolean expired;
    public boolean expiredTrial;
    public int daysLeft;
    public WorldType worldType;
    public boolean isHardcore;
    public int gameMode;
    public int activeSlot;
    @Nullable
    public String minigameName;
    public int minigameId;
    @Nullable
    public String minigameImage;
    public long parentRealmId = -1L;
    @Nullable
    public String parentWorldName;
    public String activeVersion = "";
    public Compatibility compatibility = Compatibility.UNVERIFIABLE;

    public String getDescription() {
        return this.motd;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getMinigameName() {
        return this.minigameName;
    }

    public void setName(String string) {
        this.name = string;
    }

    public void setDescription(String string) {
        this.motd = string;
    }

    public static RealmsServer parse(JsonObject jsonObject) {
        RealmsServer realmsServer = new RealmsServer();
        try {
            realmsServer.id = JsonUtils.getLongOr("id", jsonObject, -1L);
            realmsServer.remoteSubscriptionId = JsonUtils.getStringOr("remoteSubscriptionId", jsonObject, null);
            realmsServer.name = JsonUtils.getStringOr("name", jsonObject, null);
            realmsServer.motd = JsonUtils.getStringOr("motd", jsonObject, "");
            realmsServer.state = RealmsServer.getState(JsonUtils.getStringOr("state", jsonObject, State.CLOSED.name()));
            realmsServer.owner = JsonUtils.getStringOr("owner", jsonObject, null);
            if (jsonObject.get("players") != null && jsonObject.get("players").isJsonArray()) {
                realmsServer.players = RealmsServer.parseInvited(jsonObject.get("players").getAsJsonArray());
                RealmsServer.sortInvited(realmsServer);
            } else {
                realmsServer.players = Lists.newArrayList();
            }
            realmsServer.daysLeft = JsonUtils.getIntOr("daysLeft", jsonObject, 0);
            realmsServer.expired = JsonUtils.getBooleanOr("expired", jsonObject, false);
            realmsServer.expiredTrial = JsonUtils.getBooleanOr("expiredTrial", jsonObject, false);
            realmsServer.worldType = RealmsServer.getWorldType(JsonUtils.getStringOr("worldType", jsonObject, WorldType.NORMAL.name()));
            realmsServer.isHardcore = JsonUtils.getBooleanOr("isHardcore", jsonObject, false);
            realmsServer.gameMode = JsonUtils.getIntOr("gameMode", jsonObject, -1);
            realmsServer.ownerUUID = JsonUtils.getUuidOr("ownerUUID", jsonObject, Util.NIL_UUID);
            realmsServer.slots = jsonObject.get("slots") != null && jsonObject.get("slots").isJsonArray() ? RealmsServer.parseSlots(jsonObject.get("slots").getAsJsonArray()) : RealmsServer.createEmptySlots();
            realmsServer.minigameName = JsonUtils.getStringOr("minigameName", jsonObject, null);
            realmsServer.activeSlot = JsonUtils.getIntOr("activeSlot", jsonObject, -1);
            realmsServer.minigameId = JsonUtils.getIntOr("minigameId", jsonObject, -1);
            realmsServer.minigameImage = JsonUtils.getStringOr("minigameImage", jsonObject, null);
            realmsServer.parentRealmId = JsonUtils.getLongOr("parentWorldId", jsonObject, -1L);
            realmsServer.parentWorldName = JsonUtils.getStringOr("parentWorldName", jsonObject, null);
            realmsServer.activeVersion = JsonUtils.getStringOr("activeVersion", jsonObject, "");
            realmsServer.compatibility = RealmsServer.getCompatibility(JsonUtils.getStringOr("compatibility", jsonObject, Compatibility.UNVERIFIABLE.name()));
        }
        catch (Exception exception) {
            LOGGER.error("Could not parse McoServer: {}", (Object)exception.getMessage());
        }
        return realmsServer;
    }

    private static void sortInvited(RealmsServer realmsServer) {
        realmsServer.players.sort((playerInfo, playerInfo2) -> ComparisonChain.start().compareFalseFirst(playerInfo2.getAccepted(), playerInfo.getAccepted()).compare((Comparable)((Object)playerInfo.getName().toLowerCase(Locale.ROOT)), (Comparable)((Object)playerInfo2.getName().toLowerCase(Locale.ROOT))).result());
    }

    private static List<PlayerInfo> parseInvited(JsonArray jsonArray) {
        ArrayList arrayList = Lists.newArrayList();
        for (JsonElement jsonElement : jsonArray) {
            try {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                PlayerInfo playerInfo = new PlayerInfo();
                playerInfo.setName(JsonUtils.getStringOr("name", jsonObject, null));
                playerInfo.setUuid(JsonUtils.getUuidOr("uuid", jsonObject, Util.NIL_UUID));
                playerInfo.setOperator(JsonUtils.getBooleanOr("operator", jsonObject, false));
                playerInfo.setAccepted(JsonUtils.getBooleanOr("accepted", jsonObject, false));
                playerInfo.setOnline(JsonUtils.getBooleanOr("online", jsonObject, false));
                arrayList.add(playerInfo);
            }
            catch (Exception exception) {}
        }
        return arrayList;
    }

    private static Map<Integer, RealmsWorldOptions> parseSlots(JsonArray jsonArray) {
        HashMap hashMap = Maps.newHashMap();
        for (JsonElement jsonElement : jsonArray) {
            try {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                JsonElement jsonElement2 = JsonParser.parseString((String)jsonObject.get("options").getAsString());
                RealmsSettings realmsSettings = RealmsServer.parseSettings(jsonObject.get("settings"));
                RealmsWorldOptions realmsWorldOptions = jsonElement2 == null ? RealmsWorldOptions.createDefaults() : RealmsWorldOptions.parse(jsonElement2.getAsJsonObject(), realmsSettings);
                int n = JsonUtils.getIntOr("slotId", jsonObject, -1);
                hashMap.put(n, realmsWorldOptions);
            }
            catch (Exception exception) {}
        }
        for (int i = 1; i <= 3; ++i) {
            if (hashMap.containsKey(i)) continue;
            hashMap.put(i, RealmsWorldOptions.createEmptyDefaults());
        }
        return hashMap;
    }

    private static RealmsSettings parseSettings(JsonElement jsonElement) {
        boolean bl = false;
        if (jsonElement.isJsonArray()) {
            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                JsonObject jsonObject = jsonElement2.getAsJsonObject();
                bl = RealmsServer.readBoolean(jsonObject, "hardcore", bl);
            }
        }
        return new RealmsSettings(bl);
    }

    private static boolean readBoolean(JsonObject jsonObject, String string, boolean bl) {
        String string2 = JsonUtils.getStringOr("name", jsonObject, null);
        if (string2 != null && string2.equals(string)) {
            return JsonUtils.getBooleanOr("value", jsonObject, bl);
        }
        return bl;
    }

    private static Map<Integer, RealmsWorldOptions> createEmptySlots() {
        HashMap hashMap = Maps.newHashMap();
        hashMap.put(1, RealmsWorldOptions.createEmptyDefaults());
        hashMap.put(2, RealmsWorldOptions.createEmptyDefaults());
        hashMap.put(3, RealmsWorldOptions.createEmptyDefaults());
        return hashMap;
    }

    public static RealmsServer parse(String string) {
        try {
            return RealmsServer.parse(new JsonParser().parse(string).getAsJsonObject());
        }
        catch (Exception exception) {
            LOGGER.error("Could not parse McoServer: {}", (Object)exception.getMessage());
            return new RealmsServer();
        }
    }

    private static State getState(String string) {
        try {
            return State.valueOf(string);
        }
        catch (Exception exception) {
            return State.CLOSED;
        }
    }

    private static WorldType getWorldType(String string) {
        try {
            return WorldType.valueOf(string);
        }
        catch (Exception exception) {
            return WorldType.NORMAL;
        }
    }

    public static Compatibility getCompatibility(@Nullable String string) {
        try {
            return Compatibility.valueOf(string);
        }
        catch (Exception exception) {
            return Compatibility.UNVERIFIABLE;
        }
    }

    public boolean isCompatible() {
        return this.compatibility.isCompatible();
    }

    public boolean needsUpgrade() {
        return this.compatibility.needsUpgrade();
    }

    public boolean needsDowngrade() {
        return this.compatibility.needsDowngrade();
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.id, this.name, this.motd, this.state, this.owner, this.expired});
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != this.getClass()) {
            return false;
        }
        RealmsServer realmsServer = (RealmsServer)object;
        return new EqualsBuilder().append(this.id, realmsServer.id).append((Object)this.name, (Object)realmsServer.name).append((Object)this.motd, (Object)realmsServer.motd).append((Object)this.state, (Object)realmsServer.state).append((Object)this.owner, (Object)realmsServer.owner).append(this.expired, realmsServer.expired).append((Object)this.worldType, (Object)this.worldType).isEquals();
    }

    public RealmsServer clone() {
        RealmsServer realmsServer = new RealmsServer();
        realmsServer.id = this.id;
        realmsServer.remoteSubscriptionId = this.remoteSubscriptionId;
        realmsServer.name = this.name;
        realmsServer.motd = this.motd;
        realmsServer.state = this.state;
        realmsServer.owner = this.owner;
        realmsServer.players = this.players;
        realmsServer.slots = this.cloneSlots(this.slots);
        realmsServer.expired = this.expired;
        realmsServer.expiredTrial = this.expiredTrial;
        realmsServer.daysLeft = this.daysLeft;
        realmsServer.worldType = this.worldType;
        realmsServer.isHardcore = this.isHardcore;
        realmsServer.gameMode = this.gameMode;
        realmsServer.ownerUUID = this.ownerUUID;
        realmsServer.minigameName = this.minigameName;
        realmsServer.activeSlot = this.activeSlot;
        realmsServer.minigameId = this.minigameId;
        realmsServer.minigameImage = this.minigameImage;
        realmsServer.parentWorldName = this.parentWorldName;
        realmsServer.parentRealmId = this.parentRealmId;
        realmsServer.activeVersion = this.activeVersion;
        realmsServer.compatibility = this.compatibility;
        return realmsServer;
    }

    public Map<Integer, RealmsWorldOptions> cloneSlots(Map<Integer, RealmsWorldOptions> map) {
        HashMap hashMap = Maps.newHashMap();
        for (Map.Entry<Integer, RealmsWorldOptions> entry : map.entrySet()) {
            hashMap.put(entry.getKey(), entry.getValue().clone());
        }
        return hashMap;
    }

    public boolean isSnapshotRealm() {
        return this.parentRealmId != -1L;
    }

    public boolean isMinigameActive() {
        return this.worldType == WorldType.MINIGAME;
    }

    public String getWorldName(int n) {
        if (this.name == null) {
            return this.slots.get(n).getSlotName(n);
        }
        return this.name + " (" + this.slots.get(n).getSlotName(n) + ")";
    }

    public ServerData toServerData(String string) {
        return new ServerData(Objects.requireNonNullElse(this.name, "unknown server"), string, ServerData.Type.REALM);
    }

    public /* synthetic */ Object clone() throws CloneNotSupportedException {
        return this.clone();
    }

    public static enum Compatibility {
        UNVERIFIABLE,
        INCOMPATIBLE,
        RELEASE_TYPE_INCOMPATIBLE,
        NEEDS_DOWNGRADE,
        NEEDS_UPGRADE,
        COMPATIBLE;


        public boolean isCompatible() {
            return this == COMPATIBLE;
        }

        public boolean needsUpgrade() {
            return this == NEEDS_UPGRADE;
        }

        public boolean needsDowngrade() {
            return this == NEEDS_DOWNGRADE;
        }
    }

    public static enum State {
        CLOSED,
        OPEN,
        UNINITIALIZED;

    }

    public static enum WorldType {
        NORMAL,
        MINIGAME,
        ADVENTUREMAP,
        EXPERIENCE,
        INSPIRATION;

    }

    public static class McoServerComparator
    implements Comparator<RealmsServer> {
        private final String refOwner;

        public McoServerComparator(String string) {
            this.refOwner = string;
        }

        @Override
        public int compare(RealmsServer realmsServer, RealmsServer realmsServer2) {
            return ComparisonChain.start().compareTrueFirst(realmsServer.isSnapshotRealm(), realmsServer2.isSnapshotRealm()).compareTrueFirst(realmsServer.state == State.UNINITIALIZED, realmsServer2.state == State.UNINITIALIZED).compareTrueFirst(realmsServer.expiredTrial, realmsServer2.expiredTrial).compareTrueFirst(Objects.equals(realmsServer.owner, this.refOwner), Objects.equals(realmsServer2.owner, this.refOwner)).compareFalseFirst(realmsServer.expired, realmsServer2.expired).compareTrueFirst(realmsServer.state == State.OPEN, realmsServer2.state == State.OPEN).compare(realmsServer.id, realmsServer2.id).result();
        }

        @Override
        public /* synthetic */ int compare(Object object, Object object2) {
            return this.compare((RealmsServer)object, (RealmsServer)object2);
        }
    }
}

