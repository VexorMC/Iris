/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.realmsclient.util.JsonUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class RealmsText {
    private static final String TRANSLATION_KEY = "translationKey";
    private static final String ARGS = "args";
    private final String translationKey;
    @Nullable
    private final String[] args;

    private RealmsText(String string, @Nullable String[] stringArray) {
        this.translationKey = string;
        this.args = stringArray;
    }

    public Component createComponent(Component component) {
        return Objects.requireNonNullElse(this.createComponent(), component);
    }

    @Nullable
    public Component createComponent() {
        if (!I18n.exists(this.translationKey)) {
            return null;
        }
        if (this.args == null) {
            return Component.translatable(this.translationKey);
        }
        return Component.translatable(this.translationKey, this.args);
    }

    public static RealmsText parse(JsonObject jsonObject) {
        String[] stringArray;
        String string = JsonUtils.getRequiredString(TRANSLATION_KEY, jsonObject);
        JsonElement jsonElement = jsonObject.get(ARGS);
        if (jsonElement == null || jsonElement.isJsonNull()) {
            stringArray = null;
        } else {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            stringArray = new String[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); ++i) {
                stringArray[i] = jsonArray.get(i).getAsString();
            }
        }
        return new RealmsText(string, stringArray);
    }

    public String toString() {
        return this.translationKey;
    }
}

