/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class RealmsAvailability {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private static CompletableFuture<Result> future;

    public static CompletableFuture<Result> get() {
        if (future == null || RealmsAvailability.shouldRefresh(future)) {
            future = RealmsAvailability.check();
        }
        return future;
    }

    private static boolean shouldRefresh(CompletableFuture<Result> completableFuture) {
        Result result = completableFuture.getNow(null);
        return result != null && result.exception() != null;
    }

    private static CompletableFuture<Result> check() {
        User user = Minecraft.getInstance().getUser();
        if (user.getType() != User.Type.MSA) {
            return CompletableFuture.completedFuture(new Result(Type.AUTHENTICATION_ERROR));
        }
        return CompletableFuture.supplyAsync(() -> {
            RealmsClient realmsClient = RealmsClient.create();
            try {
                if (realmsClient.clientCompatible() != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                    return new Result(Type.INCOMPATIBLE_CLIENT);
                }
                if (!realmsClient.hasParentalConsent()) {
                    return new Result(Type.NEEDS_PARENTAL_CONSENT);
                }
                return new Result(Type.SUCCESS);
            }
            catch (RealmsServiceException realmsServiceException) {
                LOGGER.error("Couldn't connect to realms", (Throwable)realmsServiceException);
                if (realmsServiceException.realmsError.errorCode() == 401) {
                    return new Result(Type.AUTHENTICATION_ERROR);
                }
                return new Result(realmsServiceException);
            }
        }, Util.ioPool());
    }

    public record Result(Type type, @Nullable RealmsServiceException exception) {
        public Result(Type type) {
            this(type, null);
        }

        public Result(RealmsServiceException realmsServiceException) {
            this(Type.UNEXPECTED_ERROR, realmsServiceException);
        }

        @Nullable
        public Screen createErrorScreen(Screen screen) {
            return switch (this.type.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> null;
                case 1 -> new RealmsClientOutdatedScreen(screen);
                case 2 -> new RealmsParentalConsentScreen(screen);
                case 3 -> new RealmsGenericErrorScreen(Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), screen);
                case 4 -> new RealmsGenericErrorScreen(Objects.requireNonNull(this.exception), screen);
            };
        }
    }

    public static enum Type {
        SUCCESS,
        INCOMPATIBLE_CLIENT,
        NEEDS_PARENTAL_CONSENT,
        AUTHENTICATION_ERROR,
        UNEXPECTED_ERROR;

    }
}

