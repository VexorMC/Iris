/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;

public class UploadTokenCache {
    private static final Long2ObjectMap<String> TOKEN_CACHE = new Long2ObjectOpenHashMap();

    public static String get(long l) {
        return (String)TOKEN_CACHE.get(l);
    }

    public static void invalidate(long l) {
        TOKEN_CACHE.remove(l);
    }

    public static void put(long l, @Nullable String string) {
        TOKEN_CACHE.put(l, (Object)string);
    }
}

