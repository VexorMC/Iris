/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.authlib.minecraft.MinecraftSessionService
 *  com.mojang.authlib.yggdrasil.ProfileResult
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public class RealmsServerPlayerLists
extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    public Map<Long, List<ProfileResult>> servers = Map.of();

    public static RealmsServerPlayerLists parse(String string) {
        RealmsServerPlayerLists realmsServerPlayerLists = new RealmsServerPlayerLists();
        ImmutableMap.Builder builder = ImmutableMap.builder();
        try {
            JsonObject jsonObject = GsonHelper.parse(string);
            if (GsonHelper.isArrayNode(jsonObject, "lists")) {
                JsonArray jsonArray = jsonObject.getAsJsonArray("lists");
                for (JsonElement jsonElement : jsonArray) {
                    JsonElement jsonElement2;
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    String string2 = JsonUtils.getStringOr("playerList", jsonObject2, null);
                    List<Object> list = string2 != null ? ((jsonElement2 = JsonParser.parseString((String)string2)).isJsonArray() ? RealmsServerPlayerLists.parsePlayers(jsonElement2.getAsJsonArray()) : Lists.newArrayList()) : Lists.newArrayList();
                    builder.put((Object)JsonUtils.getLongOr("serverId", jsonObject2, -1L), (Object)list);
                }
            }
        }
        catch (Exception exception) {
            LOGGER.error("Could not parse RealmsServerPlayerLists: {}", (Object)exception.getMessage());
        }
        realmsServerPlayerLists.servers = builder.build();
        return realmsServerPlayerLists;
    }

    private static List<ProfileResult> parsePlayers(JsonArray jsonArray) {
        ArrayList<ProfileResult> arrayList = new ArrayList<ProfileResult>(jsonArray.size());
        MinecraftSessionService minecraftSessionService = Minecraft.getInstance().getMinecraftSessionService();
        for (JsonElement jsonElement : jsonArray) {
            UUID uUID;
            if (!jsonElement.isJsonObject() || (uUID = JsonUtils.getUuidOr("playerId", jsonElement.getAsJsonObject(), null)) == null || Minecraft.getInstance().isLocalPlayer(uUID)) continue;
            try {
                ProfileResult profileResult = minecraftSessionService.fetchProfile(uUID, false);
                if (profileResult == null) continue;
                arrayList.add(profileResult);
            }
            catch (Exception exception) {
                LOGGER.error("Could not get name for {}", (Object)uUID, (Object)exception);
            }
        }
        return arrayList;
    }

    public List<ProfileResult> getProfileResultsFor(long l) {
        List<ProfileResult> list = this.servers.get(l);
        if (list != null) {
            return list;
        }
        return List.of();
    }
}

