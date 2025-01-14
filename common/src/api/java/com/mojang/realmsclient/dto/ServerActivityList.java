/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package com.mojang.realmsclient.dto;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.realmsclient.dto.ServerActivity;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.List;

public class ServerActivityList
extends ValueObject {
    public long periodInMillis;
    public List<ServerActivity> serverActivities = Lists.newArrayList();

    public static ServerActivityList parse(String string) {
        ServerActivityList serverActivityList = new ServerActivityList();
        JsonParser jsonParser = new JsonParser();
        try {
            JsonElement jsonElement = jsonParser.parse(string);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            serverActivityList.periodInMillis = JsonUtils.getLongOr("periodInMillis", jsonObject, -1L);
            JsonElement jsonElement2 = jsonObject.get("playerActivityDto");
            if (jsonElement2 != null && jsonElement2.isJsonArray()) {
                JsonArray jsonArray = jsonElement2.getAsJsonArray();
                for (JsonElement jsonElement3 : jsonArray) {
                    ServerActivity serverActivity = ServerActivity.parse(jsonElement3.getAsJsonObject());
                    serverActivityList.serverActivities.add(serverActivity);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return serverActivityList;
    }
}

