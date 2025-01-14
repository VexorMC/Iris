/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.ValueObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;

public class UploadInfo
extends ValueObject {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_SCHEMA = "http://";
    private static final int DEFAULT_PORT = 8080;
    private static final Pattern URI_SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z][-a-zA-Z0-9+.]+:");
    private final boolean worldClosed;
    @Nullable
    private final String token;
    private final URI uploadEndpoint;

    private UploadInfo(boolean bl, @Nullable String string, URI uRI) {
        this.worldClosed = bl;
        this.token = string;
        this.uploadEndpoint = uRI;
    }

    @Nullable
    public static UploadInfo parse(String string) {
        try {
            int n;
            URI uRI;
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(string).getAsJsonObject();
            String string2 = JsonUtils.getStringOr("uploadEndpoint", jsonObject, null);
            if (string2 != null && (uRI = UploadInfo.assembleUri(string2, n = JsonUtils.getIntOr("port", jsonObject, -1))) != null) {
                boolean bl = JsonUtils.getBooleanOr("worldClosed", jsonObject, false);
                String string3 = JsonUtils.getStringOr("token", jsonObject, null);
                return new UploadInfo(bl, string3, uRI);
            }
        }
        catch (Exception exception) {
            LOGGER.error("Could not parse UploadInfo: {}", (Object)exception.getMessage());
        }
        return null;
    }

    @Nullable
    @VisibleForTesting
    public static URI assembleUri(String string, int n) {
        Matcher matcher = URI_SCHEMA_PATTERN.matcher(string);
        String string2 = UploadInfo.ensureEndpointSchema(string, matcher);
        try {
            URI uRI = new URI(string2);
            int n2 = UploadInfo.selectPortOrDefault(n, uRI.getPort());
            if (n2 != uRI.getPort()) {
                return new URI(uRI.getScheme(), uRI.getUserInfo(), uRI.getHost(), n2, uRI.getPath(), uRI.getQuery(), uRI.getFragment());
            }
            return uRI;
        }
        catch (URISyntaxException uRISyntaxException) {
            LOGGER.warn("Failed to parse URI {}", (Object)string2, (Object)uRISyntaxException);
            return null;
        }
    }

    private static int selectPortOrDefault(int n, int n2) {
        if (n != -1) {
            return n;
        }
        if (n2 != -1) {
            return n2;
        }
        return 8080;
    }

    private static String ensureEndpointSchema(String string, Matcher matcher) {
        if (matcher.find()) {
            return string;
        }
        return DEFAULT_SCHEMA + string;
    }

    public static String createRequest(@Nullable String string) {
        JsonObject jsonObject = new JsonObject();
        if (string != null) {
            jsonObject.addProperty("token", string);
        }
        return jsonObject.toString();
    }

    @Nullable
    public String getToken() {
        return this.token;
    }

    public URI getUploadEndpoint() {
        return this.uploadEndpoint;
    }

    public boolean isWorldClosed() {
        return this.worldClosed;
    }
}

