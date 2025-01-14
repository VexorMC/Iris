/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.CommonLinks;
import org.slf4j.Logger;

public class RealmsSubscriptionInfoScreen
extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Component SUBSCRIPTION_TITLE = Component.translatable("mco.configure.world.subscription.title");
    private static final Component SUBSCRIPTION_START_LABEL = Component.translatable("mco.configure.world.subscription.start");
    private static final Component TIME_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.timeleft");
    private static final Component DAYS_LEFT_LABEL = Component.translatable("mco.configure.world.subscription.recurring.daysleft");
    private static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.configure.world.subscription.expired");
    private static final Component SUBSCRIPTION_LESS_THAN_A_DAY_TEXT = Component.translatable("mco.configure.world.subscription.less_than_a_day");
    private static final Component UNKNOWN = Component.translatable("mco.configure.world.subscription.unknown");
    private static final Component RECURRING_INFO = Component.translatable("mco.configure.world.subscription.recurring.info");
    private final Screen lastScreen;
    final RealmsServer serverData;
    final Screen mainScreen;
    private Component daysLeft = UNKNOWN;
    private Component startDate = UNKNOWN;
    @Nullable
    private Subscription.SubscriptionType type;

    public RealmsSubscriptionInfoScreen(Screen screen, RealmsServer realmsServer, Screen screen2) {
        super(GameNarrator.NO_TITLE);
        this.lastScreen = screen;
        this.serverData = realmsServer;
        this.mainScreen = screen2;
    }

    @Override
    public void init() {
        this.getSubscription(this.serverData.id);
        this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.subscription.extend"), button -> ConfirmLinkScreen.confirmLinkNow((Screen)this, CommonLinks.extendRealms(this.serverData.remoteSubscriptionId, this.minecraft.getUser().getProfileId()))).bounds(this.width / 2 - 100, RealmsSubscriptionInfoScreen.row(6), 200, 20).build());
        if (this.serverData.expired) {
            this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.delete.button"), button -> this.minecraft.setScreen(RealmsPopups.warningPopupScreen(this, Component.translatable("mco.configure.world.delete.question.line1"), popupScreen -> this.deleteRealm()))).bounds(this.width / 2 - 100, RealmsSubscriptionInfoScreen.row(10), 200, 20).build());
        } else if (RealmsMainScreen.isSnapshot() && this.serverData.parentWorldName != null) {
            this.addRenderableWidget(new FittingMultiLineTextWidget(this.width / 2 - 100, RealmsSubscriptionInfoScreen.row(8), 200, 46, Component.translatable("mco.snapshot.subscription.info", this.serverData.parentWorldName), this.font));
        } else {
            this.addRenderableWidget(new FittingMultiLineTextWidget(this.width / 2 - 100, RealmsSubscriptionInfoScreen.row(8), 200, 46, RECURRING_INFO, this.font));
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).bounds(this.width / 2 - 100, RealmsSubscriptionInfoScreen.row(12), 200, 20).build());
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinLines(SUBSCRIPTION_TITLE, SUBSCRIPTION_START_LABEL, this.startDate, TIME_LEFT_LABEL, this.daysLeft);
    }

    private void deleteRealm() {
        new Thread("Realms-delete-realm"){

            @Override
            public void run() {
                try {
                    RealmsClient realmsClient = RealmsClient.create();
                    realmsClient.deleteRealm(RealmsSubscriptionInfoScreen.this.serverData.id);
                }
                catch (RealmsServiceException realmsServiceException) {
                    LOGGER.error("Couldn't delete world", (Throwable)realmsServiceException);
                }
                RealmsSubscriptionInfoScreen.this.minecraft.execute(() -> RealmsSubscriptionInfoScreen.this.minecraft.setScreen(RealmsSubscriptionInfoScreen.this.mainScreen));
            }
        }.start();
        this.minecraft.setScreen(this);
    }

    private void getSubscription(long l) {
        RealmsClient realmsClient = RealmsClient.create();
        try {
            Subscription subscription = realmsClient.subscriptionFor(l);
            this.daysLeft = this.daysLeftPresentation(subscription.daysLeft);
            this.startDate = RealmsSubscriptionInfoScreen.localPresentation(subscription.startDate);
            this.type = subscription.type;
        }
        catch (RealmsServiceException realmsServiceException) {
            LOGGER.error("Couldn't get subscription", (Throwable)realmsServiceException);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsServiceException, this.lastScreen));
        }
    }

    private static Component localPresentation(long l) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getDefault());
        gregorianCalendar.setTimeInMillis(l);
        return Component.literal(DateFormat.getDateTimeInstance().format(gregorianCalendar.getTime()));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        int n3 = this.width / 2 - 100;
        guiGraphics.drawCenteredString(this.font, SUBSCRIPTION_TITLE, this.width / 2, 17, -1);
        guiGraphics.drawString(this.font, SUBSCRIPTION_START_LABEL, n3, RealmsSubscriptionInfoScreen.row(0), -6250336);
        guiGraphics.drawString(this.font, this.startDate, n3, RealmsSubscriptionInfoScreen.row(1), -1);
        if (this.type == Subscription.SubscriptionType.NORMAL) {
            guiGraphics.drawString(this.font, TIME_LEFT_LABEL, n3, RealmsSubscriptionInfoScreen.row(3), -6250336);
        } else if (this.type == Subscription.SubscriptionType.RECURRING) {
            guiGraphics.drawString(this.font, DAYS_LEFT_LABEL, n3, RealmsSubscriptionInfoScreen.row(3), -6250336);
        }
        guiGraphics.drawString(this.font, this.daysLeft, n3, RealmsSubscriptionInfoScreen.row(4), -1);
    }

    private Component daysLeftPresentation(int n) {
        boolean bl;
        if (n < 0 && this.serverData.expired) {
            return SUBSCRIPTION_EXPIRED_TEXT;
        }
        if (n <= 1) {
            return SUBSCRIPTION_LESS_THAN_A_DAY_TEXT;
        }
        int n2 = n / 30;
        int n3 = n % 30;
        boolean bl2 = n2 > 0;
        boolean bl3 = bl = n3 > 0;
        if (bl2 && bl) {
            return Component.translatable("mco.configure.world.subscription.remaining.months.days", n2, n3);
        }
        if (bl2) {
            return Component.translatable("mco.configure.world.subscription.remaining.months", n2);
        }
        if (bl) {
            return Component.translatable("mco.configure.world.subscription.remaining.days", n3);
        }
        return Component.empty();
    }
}

