/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import org.slf4j.Logger;

public class WorldDownload
extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    public String downloadLink;
    public String resourcePackUrl;
    public String resourcePackHash;

    public static WorldDownload parse(String string) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(string).getAsJsonObject();
        WorldDownload worldDownload = new WorldDownload();
        try {
            worldDownload.downloadLink = JsonUtils.getStringOr("downloadLink", jsonObject, "");
            worldDownload.resourcePackUrl = JsonUtils.getStringOr("resourcePackUrl", jsonObject, "");
            worldDownload.resourcePackHash = JsonUtils.getStringOr("resourcePackHash", jsonObject, "");
        }
        catch (Exception exception) {
            LOGGER.error("Could not parse WorldDownload: {}", (Object)exception.getMessage());
        }
        return worldDownload;
    }
}

