/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.SerializedName
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.util;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class RealmsPersistence {
    private static final String FILE_NAME = "realms_persistence.json";
    private static final GuardedSerializer GSON = new GuardedSerializer();
    private static final Logger LOGGER = LogUtils.getLogger();

    public RealmsPersistenceData read() {
        return RealmsPersistence.readFile();
    }

    public void save(RealmsPersistenceData realmsPersistenceData) {
        RealmsPersistence.writeFile(realmsPersistenceData);
    }

    public static RealmsPersistenceData readFile() {
        Path path = RealmsPersistence.getPathToData();
        try {
            String string = Files.readString(path, StandardCharsets.UTF_8);
            RealmsPersistenceData realmsPersistenceData = GSON.fromJson(string, RealmsPersistenceData.class);
            if (realmsPersistenceData != null) {
                return realmsPersistenceData;
            }
        }
        catch (NoSuchFileException noSuchFileException) {
        }
        catch (Exception exception) {
            LOGGER.warn("Failed to read Realms storage {}", (Object)path, (Object)exception);
        }
        return new RealmsPersistenceData();
    }

    public static void writeFile(RealmsPersistenceData realmsPersistenceData) {
        Path path = RealmsPersistence.getPathToData();
        try {
            Files.writeString(path, (CharSequence)GSON.toJson(realmsPersistenceData), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static Path getPathToData() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FILE_NAME);
    }

    public static class RealmsPersistenceData
    implements ReflectionBasedSerialization {
        @SerializedName(value="newsLink")
        public String newsLink;
        @SerializedName(value="hasUnreadNews")
        public boolean hasUnreadNews;
    }
}

