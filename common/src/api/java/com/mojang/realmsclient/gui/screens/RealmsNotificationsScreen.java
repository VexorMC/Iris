/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.RealmsAvailability;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.task.DataFetcher;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;

public class RealmsNotificationsScreen
extends RealmsScreen {
    private static final ResourceLocation UNSEEN_NOTIFICATION_SPRITE = ResourceLocation.withDefaultNamespace("icon/unseen_notification");
    private static final ResourceLocation NEWS_SPRITE = ResourceLocation.withDefaultNamespace("icon/news");
    private static final ResourceLocation INVITE_SPRITE = ResourceLocation.withDefaultNamespace("icon/invite");
    private static final ResourceLocation TRIAL_AVAILABLE_SPRITE = ResourceLocation.withDefaultNamespace("icon/trial_available");
    private final CompletableFuture<Boolean> validClient = RealmsAvailability.get().thenApply(result -> result.type() == RealmsAvailability.Type.SUCCESS);
    @Nullable
    private DataFetcher.Subscription realmsDataSubscription;
    @Nullable
    private DataFetcherConfiguration currentConfiguration;
    private volatile int numberOfPendingInvites;
    private static boolean trialAvailable;
    private static boolean hasUnreadNews;
    private static boolean hasUnseenNotifications;
    private final DataFetcherConfiguration showAll = new DataFetcherConfiguration(){

        @Override
        public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher realmsDataFetcher) {
            DataFetcher.Subscription subscription = realmsDataFetcher.dataFetcher.createSubscription();
            RealmsNotificationsScreen.this.addNewsAndInvitesSubscriptions(realmsDataFetcher, subscription);
            RealmsNotificationsScreen.this.addNotificationsSubscriptions(realmsDataFetcher, subscription);
            return subscription;
        }

        @Override
        public boolean showOldNotifications() {
            return true;
        }
    };
    private final DataFetcherConfiguration onlyNotifications = new DataFetcherConfiguration(){

        @Override
        public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher realmsDataFetcher) {
            DataFetcher.Subscription subscription = realmsDataFetcher.dataFetcher.createSubscription();
            RealmsNotificationsScreen.this.addNotificationsSubscriptions(realmsDataFetcher, subscription);
            return subscription;
        }

        @Override
        public boolean showOldNotifications() {
            return false;
        }
    };

    public RealmsNotificationsScreen() {
        super(GameNarrator.NO_TITLE);
    }

    @Override
    public void init() {
        if (this.realmsDataSubscription != null) {
            this.realmsDataSubscription.forceUpdate();
        }
    }

    @Override
    public void added() {
        super.added();
        this.minecraft.realmsDataFetcher().notificationsTask.reset();
    }

    @Nullable
    private DataFetcherConfiguration getConfiguration() {
        boolean bl;
        boolean bl2 = bl = this.inTitleScreen() && this.validClient.getNow(false) != false;
        if (!bl) {
            return null;
        }
        return this.getRealmsNotificationsEnabled() ? this.showAll : this.onlyNotifications;
    }

    @Override
    public void tick() {
        DataFetcherConfiguration dataFetcherConfiguration = this.getConfiguration();
        if (!Objects.equals(this.currentConfiguration, dataFetcherConfiguration)) {
            this.currentConfiguration = dataFetcherConfiguration;
            this.realmsDataSubscription = this.currentConfiguration != null ? this.currentConfiguration.initDataFetcher(this.minecraft.realmsDataFetcher()) : null;
        }
        if (this.realmsDataSubscription != null) {
            this.realmsDataSubscription.tick();
        }
    }

    private boolean getRealmsNotificationsEnabled() {
        return this.minecraft.options.realmsNotifications().get();
    }

    private boolean inTitleScreen() {
        return this.minecraft.screen instanceof TitleScreen;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        if (this.validClient.getNow(false).booleanValue()) {
            this.drawIcons(guiGraphics);
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int n, int n2, float f) {
    }

    private void drawIcons(GuiGraphics guiGraphics) {
        int n = this.numberOfPendingInvites;
        int n2 = 24;
        int n3 = this.height / 4 + 48;
        int n4 = this.width / 2 + 100;
        int n5 = n3 + 48 + 2;
        int n6 = n4 - 3;
        if (hasUnseenNotifications) {
            guiGraphics.blitSprite(RenderType::guiTextured, UNSEEN_NOTIFICATION_SPRITE, n6 - 12, n5 + 3, 10, 10);
            n6 -= 16;
        }
        if (this.currentConfiguration != null && this.currentConfiguration.showOldNotifications()) {
            if (hasUnreadNews) {
                guiGraphics.blitSprite(RenderType::guiTextured, NEWS_SPRITE, n6 - 14, n5 + 1, 14, 14);
                n6 -= 16;
            }
            if (n != 0) {
                guiGraphics.blitSprite(RenderType::guiTextured, INVITE_SPRITE, n6 - 14, n5 + 1, 14, 14);
                n6 -= 16;
            }
            if (trialAvailable) {
                guiGraphics.blitSprite(RenderType::guiTextured, TRIAL_AVAILABLE_SPRITE, n6 - 10, n5 + 4, 8, 8);
            }
        }
    }

    void addNewsAndInvitesSubscriptions(RealmsDataFetcher realmsDataFetcher, DataFetcher.Subscription subscription) {
        subscription.subscribe(realmsDataFetcher.pendingInvitesTask, n -> {
            this.numberOfPendingInvites = n;
        });
        subscription.subscribe(realmsDataFetcher.trialAvailabilityTask, bl -> {
            trialAvailable = bl;
        });
        subscription.subscribe(realmsDataFetcher.newsTask, realmsNews -> {
            realmsDataFetcher.newsManager.updateUnreadNews((RealmsNews)realmsNews);
            hasUnreadNews = realmsDataFetcher.newsManager.hasUnreadNews();
        });
    }

    void addNotificationsSubscriptions(RealmsDataFetcher realmsDataFetcher, DataFetcher.Subscription subscription) {
        subscription.subscribe(realmsDataFetcher.notificationsTask, list -> {
            hasUnseenNotifications = false;
            for (RealmsNotification realmsNotification : list) {
                if (realmsNotification.seen()) continue;
                hasUnseenNotifications = true;
                break;
            }
        });
    }

    static interface DataFetcherConfiguration {
        public DataFetcher.Subscription initDataFetcher(RealmsDataFetcher var1);

        public boolean showOldNotifications();
    }
}

