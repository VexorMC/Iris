/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.util.concurrent.RateLimiter
 *  com.mojang.authlib.yggdrasil.ProfileResult
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.RealmsAvailability;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class RealmsMainScreen
extends RealmsScreen {
    static final ResourceLocation INFO_SPRITE = ResourceLocation.withDefaultNamespace("icon/info");
    static final ResourceLocation NEW_REALM_SPRITE = ResourceLocation.withDefaultNamespace("icon/new_realm");
    static final ResourceLocation EXPIRED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expired");
    static final ResourceLocation EXPIRES_SOON_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expires_soon");
    static final ResourceLocation OPEN_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/open");
    static final ResourceLocation CLOSED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/closed");
    private static final ResourceLocation INVITE_SPRITE = ResourceLocation.withDefaultNamespace("icon/invite");
    private static final ResourceLocation NEWS_SPRITE = ResourceLocation.withDefaultNamespace("icon/news");
    public static final ResourceLocation HARDCORE_MODE_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full");
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation LOGO_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/title/realms.png");
    private static final ResourceLocation NO_REALMS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/no_realms.png");
    private static final Component TITLE = Component.translatable("menu.online");
    private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
    static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
    static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
    private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
    static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
    private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
    static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
    static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
    static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
    static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
    static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
    static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
    private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
    private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
    private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
    private static final Component INCOMPATIBLE_POPUP_TITLE = Component.translatable("mco.compatibility.incompatible.popup.title");
    private static final Component INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE = Component.translatable("mco.compatibility.incompatible.releaseType.popup.message");
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_COLUMNS = 3;
    private static final int BUTTON_SPACING = 4;
    private static final int CONTENT_WIDTH = 308;
    private static final int LOGO_WIDTH = 128;
    private static final int LOGO_HEIGHT = 34;
    private static final int LOGO_TEXTURE_WIDTH = 128;
    private static final int LOGO_TEXTURE_HEIGHT = 64;
    private static final int LOGO_PADDING = 5;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_PADDING = 11;
    private static final int NEW_REALM_SPRITE_WIDTH = 40;
    private static final int NEW_REALM_SPRITE_HEIGHT = 20;
    private static final int ENTRY_WIDTH = 216;
    private static final int ITEM_HEIGHT = 36;
    private static final boolean SNAPSHOT;
    private static boolean snapshotToggle;
    private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
    @Nullable
    private DataFetcher.Subscription dataSubscription;
    private final Set<UUID> handledSeenNotifications = new HashSet<UUID>();
    private static boolean regionsPinged;
    private final RateLimiter inviteNarrationLimiter;
    private final Screen lastScreen;
    private Button playButton;
    private Button backButton;
    private Button renewButton;
    private Button configureButton;
    private Button leaveButton;
    RealmSelectionList realmSelectionList;
    RealmsServerList serverList;
    List<RealmsServer> availableSnapshotServers = List.of();
    RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists();
    private volatile boolean trialsAvailable;
    @Nullable
    private volatile String newsLink;
    long lastClickTime;
    final List<RealmsNotification> notifications = new ArrayList<RealmsNotification>();
    private Button addRealmButton;
    private NotificationButton pendingInvitesButton;
    private NotificationButton newsButton;
    private LayoutState activeLayoutState;
    @Nullable
    private HeaderAndFooterLayout layout;

    public RealmsMainScreen(Screen screen) {
        super(TITLE);
        this.lastScreen = screen;
        this.inviteNarrationLimiter = RateLimiter.create((double)0.01666666753590107);
    }

    @Override
    public void init() {
        this.serverList = new RealmsServerList(this.minecraft);
        this.realmSelectionList = new RealmSelectionList();
        MutableComponent mutableComponent = Component.translatable("mco.invites.title");
        this.pendingInvitesButton = new NotificationButton(mutableComponent, INVITE_SPRITE, button -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, mutableComponent)));
        MutableComponent mutableComponent2 = Component.translatable("mco.news");
        this.newsButton = new NotificationButton(mutableComponent2, NEWS_SPRITE, button -> {
            String string = this.newsLink;
            if (string == null) {
                return;
            }
            ConfirmLinkScreen.confirmLinkNow((Screen)this, string);
            if (this.newsButton.notificationCount() != 0) {
                RealmsPersistence.RealmsPersistenceData realmsPersistenceData = RealmsPersistence.readFile();
                realmsPersistenceData.hasUnreadNews = false;
                RealmsPersistence.writeFile(realmsPersistenceData);
                this.newsButton.setNotificationCount(0);
            }
        });
        this.newsButton.setTooltip(Tooltip.create(mutableComponent2));
        this.playButton = Button.builder(PLAY_TEXT, button -> RealmsMainScreen.play(this.getSelectedServer(), this)).width(100).build();
        this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, button -> this.configureClicked(this.getSelectedServer())).width(100).build();
        this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, button -> this.onRenew(this.getSelectedServer())).width(100).build();
        this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, button -> this.leaveClicked(this.getSelectedServer())).width(100).build();
        this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), button -> this.openTrialAvailablePopup()).size(100, 20).build();
        this.backButton = Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).width(100).build();
        if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
            this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release")).create(5, 5, 100, 20, Component.literal("Realm"), (cycleButton, bl) -> {
                snapshotToggle = bl;
                this.availableSnapshotServers = List.of();
                this.debugRefreshDataFetchers();
            }));
        }
        this.updateLayout(LayoutState.LOADING);
        this.updateButtonStates();
        this.availability.thenAcceptAsync(result -> {
            Screen screen = result.createErrorScreen(this.lastScreen);
            if (screen == null) {
                this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
            } else {
                this.minecraft.setScreen(screen);
            }
        }, this.screenExecutor);
    }

    public static boolean isSnapshot() {
        return SNAPSHOT && snapshotToggle;
    }

    @Override
    protected void repositionElements() {
        if (this.layout != null) {
            this.realmSelectionList.updateSize(this.width, this.layout);
            this.layout.arrangeElements();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void updateLayout() {
        if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
            this.updateLayout(LayoutState.NO_REALMS);
        } else {
            this.updateLayout(LayoutState.LIST);
        }
    }

    private void updateLayout(LayoutState layoutState) {
        if (this.activeLayoutState == layoutState) {
            return;
        }
        if (this.layout != null) {
            this.layout.visitWidgets(guiEventListener -> this.removeWidget((GuiEventListener)guiEventListener));
        }
        this.layout = this.createLayout(layoutState);
        this.activeLayoutState = layoutState;
        this.layout.visitWidgets(guiEventListener -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
        });
        this.repositionElements();
    }

    private HeaderAndFooterLayout createLayout(LayoutState layoutState) {
        HeaderAndFooterLayout headerAndFooterLayout = new HeaderAndFooterLayout(this);
        headerAndFooterLayout.setHeaderHeight(44);
        headerAndFooterLayout.addToHeader(this.createHeader());
        Layout layout = this.createFooter(layoutState);
        layout.arrangeElements();
        headerAndFooterLayout.setFooterHeight(layout.getHeight() + 22);
        headerAndFooterLayout.addToFooter(layout);
        switch (layoutState.ordinal()) {
            case 0: {
                headerAndFooterLayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
                break;
            }
            case 1: {
                headerAndFooterLayout.addToContents(this.createNoRealmsContent());
                break;
            }
            case 2: {
                headerAndFooterLayout.addToContents(this.realmSelectionList);
            }
        }
        return headerAndFooterLayout;
    }

    private Layout createHeader() {
        int n = 90;
        LinearLayout linearLayout = LinearLayout.horizontal().spacing(4);
        linearLayout.defaultCellSetting().alignVerticallyMiddle();
        linearLayout.addChild(this.pendingInvitesButton);
        linearLayout.addChild(this.newsButton);
        LinearLayout linearLayout2 = LinearLayout.horizontal();
        linearLayout2.defaultCellSetting().alignVerticallyMiddle();
        linearLayout2.addChild(SpacerElement.width(90));
        linearLayout2.addChild(ImageWidget.texture(128, 34, LOGO_LOCATION, 128, 64), LayoutSettings::alignHorizontallyCenter);
        linearLayout2.addChild(new FrameLayout(90, 44)).addChild(linearLayout, LayoutSettings::alignHorizontallyRight);
        return linearLayout2;
    }

    private Layout createFooter(LayoutState layoutState) {
        GridLayout gridLayout = new GridLayout().spacing(4);
        GridLayout.RowHelper rowHelper = gridLayout.createRowHelper(3);
        if (layoutState == LayoutState.LIST) {
            rowHelper.addChild(this.playButton);
            rowHelper.addChild(this.configureButton);
            rowHelper.addChild(this.renewButton);
            rowHelper.addChild(this.leaveButton);
        }
        rowHelper.addChild(this.addRealmButton);
        rowHelper.addChild(this.backButton);
        return gridLayout;
    }

    private LinearLayout createNoRealmsContent() {
        LinearLayout linearLayout = LinearLayout.vertical().spacing(8);
        linearLayout.defaultCellSetting().alignHorizontallyCenter();
        linearLayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
        FocusableTextWidget focusableTextWidget = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false, 4);
        linearLayout.addChild(focusableTextWidget);
        return linearLayout;
    }

    void updateButtonStates() {
        RealmsServer realmsServer = this.getSelectedServer();
        this.addRealmButton.active = this.activeLayoutState != LayoutState.LOADING;
        this.playButton.active = realmsServer != null && this.shouldPlayButtonBeActive(realmsServer);
        this.renewButton.active = realmsServer != null && this.shouldRenewButtonBeActive(realmsServer);
        this.leaveButton.active = realmsServer != null && this.shouldLeaveButtonBeActive(realmsServer);
        this.configureButton.active = realmsServer != null && this.shouldConfigureButtonBeActive(realmsServer);
    }

    boolean shouldPlayButtonBeActive(RealmsServer realmsServer) {
        boolean bl = !realmsServer.expired && realmsServer.state == RealmsServer.State.OPEN;
        return bl && (realmsServer.isCompatible() || realmsServer.needsUpgrade() || RealmsMainScreen.isSelfOwnedServer(realmsServer));
    }

    private boolean shouldRenewButtonBeActive(RealmsServer realmsServer) {
        return realmsServer.expired && RealmsMainScreen.isSelfOwnedServer(realmsServer);
    }

    private boolean shouldConfigureButtonBeActive(RealmsServer realmsServer) {
        return RealmsMainScreen.isSelfOwnedServer(realmsServer) && realmsServer.state != RealmsServer.State.UNINITIALIZED;
    }

    private boolean shouldLeaveButtonBeActive(RealmsServer realmsServer) {
        return !RealmsMainScreen.isSelfOwnedServer(realmsServer);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dataSubscription != null) {
            this.dataSubscription.tick();
        }
    }

    public static void refreshPendingInvites() {
        Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
    }

    public static void refreshServerList() {
        Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
    }

    private void debugRefreshDataFetchers() {
        for (DataFetcher.Task<?> task : this.minecraft.realmsDataFetcher().getTasks()) {
            task.reset();
        }
    }

    private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher realmsDataFetcher) {
        DataFetcher.Subscription subscription = realmsDataFetcher.dataFetcher.createSubscription();
        subscription.subscribe(realmsDataFetcher.serverListUpdateTask, serverListData -> {
            this.serverList.updateServersList(serverListData.serverList());
            this.availableSnapshotServers = serverListData.availableSnapshotServers();
            this.refreshListAndLayout();
            boolean bl = false;
            for (RealmsServer realmsServer : this.serverList) {
                if (!this.isSelfOwnedNonExpiredServer(realmsServer)) continue;
                bl = true;
            }
            if (!regionsPinged && bl) {
                regionsPinged = true;
                this.pingRegions();
            }
        });
        RealmsMainScreen.callRealmsClient(RealmsClient::getNotifications, list -> {
            this.notifications.clear();
            this.notifications.addAll((Collection<RealmsNotification>)list);
            for (RealmsNotification realmsNotification : list) {
                RealmsNotification.InfoPopup infoPopup;
                PopupScreen popupScreen;
                if (!(realmsNotification instanceof RealmsNotification.InfoPopup) || (popupScreen = (infoPopup = (RealmsNotification.InfoPopup)realmsNotification).buildScreen(this, this::dismissNotification)) == null) continue;
                this.minecraft.setScreen(popupScreen);
                this.markNotificationsAsSeen(List.of(realmsNotification));
                break;
            }
            if (!this.notifications.isEmpty() && this.activeLayoutState != LayoutState.LOADING) {
                this.refreshListAndLayout();
            }
        });
        subscription.subscribe(realmsDataFetcher.pendingInvitesTask, n -> {
            this.pendingInvitesButton.setNotificationCount((int)n);
            this.pendingInvitesButton.setTooltip(n == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
            if (n > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
                this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", n));
            }
        });
        subscription.subscribe(realmsDataFetcher.trialAvailabilityTask, bl -> {
            this.trialsAvailable = bl;
        });
        subscription.subscribe(realmsDataFetcher.onlinePlayersTask, realmsServerPlayerLists -> {
            this.onlinePlayersPerRealm = realmsServerPlayerLists;
        });
        subscription.subscribe(realmsDataFetcher.newsTask, realmsNews -> {
            realmsDataFetcher.newsManager.updateUnreadNews((RealmsNews)realmsNews);
            this.newsLink = realmsDataFetcher.newsManager.newsLink();
            this.newsButton.setNotificationCount(realmsDataFetcher.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
        });
        return subscription;
    }

    void markNotificationsAsSeen(Collection<RealmsNotification> collection) {
        ArrayList<UUID> arrayList = new ArrayList<UUID>(collection.size());
        for (RealmsNotification realmsNotification : collection) {
            if (realmsNotification.seen() || this.handledSeenNotifications.contains(realmsNotification.uuid())) continue;
            arrayList.add(realmsNotification.uuid());
        }
        if (!arrayList.isEmpty()) {
            RealmsMainScreen.callRealmsClient(realmsClient -> {
                realmsClient.notificationsSeen(arrayList);
                return null;
            }, object -> this.handledSeenNotifications.addAll(arrayList));
        }
    }

    private static <T> void callRealmsClient(RealmsCall<T> realmsCall, Consumer<T> consumer) {
        Minecraft minecraft = Minecraft.getInstance();
        ((CompletableFuture)CompletableFuture.supplyAsync(() -> {
            try {
                return realmsCall.request(RealmsClient.create(minecraft));
            }
            catch (RealmsServiceException realmsServiceException) {
                throw new RuntimeException(realmsServiceException);
            }
        }).thenAcceptAsync(consumer, (Executor)minecraft)).exceptionally(throwable -> {
            LOGGER.error("Failed to execute call to Realms Service", throwable);
            return null;
        });
    }

    private void refreshListAndLayout() {
        this.realmSelectionList.refreshEntries(this, this.getSelectedServer());
        this.updateLayout();
        this.updateButtonStates();
    }

    private void pingRegions() {
        new Thread(() -> {
            List<RegionPingResult> list = Ping.pingAllRegions();
            RealmsClient realmsClient = RealmsClient.create();
            PingResult pingResult = new PingResult();
            pingResult.pingResults = list;
            pingResult.realmIds = this.getOwnedNonExpiredRealmIds();
            try {
                realmsClient.sendPingResults(pingResult);
            }
            catch (Throwable throwable) {
                LOGGER.warn("Could not send ping result to Realms: ", throwable);
            }
        }).start();
    }

    private List<Long> getOwnedNonExpiredRealmIds() {
        ArrayList arrayList = Lists.newArrayList();
        for (RealmsServer realmsServer : this.serverList) {
            if (!this.isSelfOwnedNonExpiredServer(realmsServer)) continue;
            arrayList.add(realmsServer.id);
        }
        return arrayList;
    }

    private void onRenew(@Nullable RealmsServer realmsServer) {
        if (realmsServer != null) {
            String string = CommonLinks.extendRealms(realmsServer.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), realmsServer.expiredTrial);
            this.minecraft.keyboardHandler.setClipboard(string);
            Util.getPlatform().openUri(string);
        }
    }

    private void configureClicked(@Nullable RealmsServer realmsServer) {
        if (realmsServer != null && this.minecraft.isLocalPlayer(realmsServer.ownerUUID)) {
            this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, realmsServer.id));
        }
    }

    private void leaveClicked(@Nullable RealmsServer realmsServer) {
        if (realmsServer != null && !this.minecraft.isLocalPlayer(realmsServer.ownerUUID)) {
            MutableComponent mutableComponent = Component.translatable("mco.configure.world.leave.question.line1");
            this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, mutableComponent, popupScreen -> this.leaveServer(realmsServer)));
        }
    }

    @Nullable
    private RealmsServer getSelectedServer() {
        Object e = this.realmSelectionList.getSelected();
        if (e instanceof ServerEntry) {
            ServerEntry serverEntry = (ServerEntry)e;
            return serverEntry.getServer();
        }
        return null;
    }

    private void leaveServer(final RealmsServer realmsServer) {
        new Thread("Realms-leave-server"){

            @Override
            public void run() {
                try {
                    RealmsClient realmsClient = RealmsClient.create();
                    realmsClient.uninviteMyselfFrom(realmsServer.id);
                    RealmsMainScreen.this.minecraft.execute(RealmsMainScreen::refreshServerList);
                }
                catch (RealmsServiceException realmsServiceException) {
                    LOGGER.error("Couldn't configure world", (Throwable)realmsServiceException);
                    RealmsMainScreen.this.minecraft.execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsServiceException, (Screen)RealmsMainScreen.this)));
                }
            }
        }.start();
        this.minecraft.setScreen(this);
    }

    void dismissNotification(UUID uUID) {
        RealmsMainScreen.callRealmsClient(realmsClient -> {
            realmsClient.notificationsDismiss(List.of(uUID));
            return null;
        }, object -> {
            this.notifications.removeIf(realmsNotification -> realmsNotification.dismissable() && uUID.equals(realmsNotification.uuid()));
            this.refreshListAndLayout();
        });
    }

    public void resetScreen() {
        this.realmSelectionList.setSelected((Entry)null);
        RealmsMainScreen.refreshServerList();
    }

    @Override
    public Component getNarrationMessage() {
        return switch (this.activeLayoutState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
            case 1 -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
            case 2 -> super.getNarrationMessage();
        };
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        if (RealmsMainScreen.isSnapshot()) {
            guiGraphics.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().getName(), 2, this.height - 10, -1);
        }
        if (this.trialsAvailable && this.addRealmButton.active) {
            AddRealmPopupScreen.renderDiamond(guiGraphics, this.addRealmButton);
        }
        switch (RealmsClient.ENVIRONMENT) {
            case STAGE: {
                this.renderEnvironment(guiGraphics, "STAGE!", -256);
                break;
            }
            case LOCAL: {
                this.renderEnvironment(guiGraphics, "LOCAL!", 0x7FFF7F);
            }
        }
    }

    private void openTrialAvailablePopup() {
        this.minecraft.setScreen(new AddRealmPopupScreen(this, this.trialsAvailable));
    }

    public static void play(@Nullable RealmsServer realmsServer, Screen screen) {
        RealmsMainScreen.play(realmsServer, screen, false);
    }

    public static void play(@Nullable RealmsServer realmsServer, Screen screen, boolean bl) {
        if (realmsServer != null) {
            if (!RealmsMainScreen.isSnapshot() || bl || realmsServer.isMinigameActive()) {
                Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(screen, new GetServerDetailsTask(screen, realmsServer)));
                return;
            }
            switch (realmsServer.compatibility) {
                case COMPATIBLE: {
                    Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(screen, new GetServerDetailsTask(screen, realmsServer)));
                    break;
                }
                case UNVERIFIABLE: {
                    RealmsMainScreen.confirmToPlay(realmsServer, screen, Component.translatable("mco.compatibility.unverifiable.title").withColor(-171), Component.translatable("mco.compatibility.unverifiable.message"), CommonComponents.GUI_CONTINUE);
                    break;
                }
                case NEEDS_DOWNGRADE: {
                    RealmsMainScreen.confirmToPlay(realmsServer, screen, Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128), Component.translatable("mco.compatibility.downgrade.description", Component.literal(realmsServer.activeVersion).withColor(-171), Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)), Component.translatable("mco.compatibility.downgrade"));
                    break;
                }
                case NEEDS_UPGRADE: {
                    RealmsMainScreen.upgradeRealmAndPlay(realmsServer, screen);
                    break;
                }
                case INCOMPATIBLE: {
                    Minecraft.getInstance().setScreen(new PopupScreen.Builder(screen, INCOMPATIBLE_POPUP_TITLE).setMessage(Component.translatable("mco.compatibility.incompatible.series.popup.message", Component.literal(realmsServer.activeVersion).withColor(-171), Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171))).addButton(CommonComponents.GUI_BACK, PopupScreen::onClose).build());
                    break;
                }
                case RELEASE_TYPE_INCOMPATIBLE: {
                    Minecraft.getInstance().setScreen(new PopupScreen.Builder(screen, INCOMPATIBLE_POPUP_TITLE).setMessage(INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE).addButton(CommonComponents.GUI_BACK, PopupScreen::onClose).build());
                }
            }
        }
    }

    private static void confirmToPlay(RealmsServer realmsServer, Screen screen, Component component, Component component2, Component component3) {
        Minecraft.getInstance().setScreen(new PopupScreen.Builder(screen, component).setMessage(component2).addButton(component3, popupScreen -> {
            Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(screen, new GetServerDetailsTask(screen, realmsServer)));
            RealmsMainScreen.refreshServerList();
        }).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
    }

    private static void upgradeRealmAndPlay(RealmsServer realmsServer, Screen screen) {
        MutableComponent mutableComponent = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
        MutableComponent mutableComponent2 = Component.translatable("mco.compatibility.upgrade");
        MutableComponent mutableComponent3 = Component.literal(realmsServer.activeVersion).withColor(-171);
        MutableComponent mutableComponent4 = Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171);
        MutableComponent mutableComponent5 = RealmsMainScreen.isSelfOwnedServer(realmsServer) ? Component.translatable("mco.compatibility.upgrade.description", mutableComponent3, mutableComponent4) : Component.translatable("mco.compatibility.upgrade.friend.description", mutableComponent3, mutableComponent4);
        RealmsMainScreen.confirmToPlay(realmsServer, screen, mutableComponent, mutableComponent5, mutableComponent2);
    }

    public static Component getVersionComponent(String string, boolean bl) {
        return RealmsMainScreen.getVersionComponent(string, bl ? -8355712 : -2142128);
    }

    public static Component getVersionComponent(String string, int n) {
        if (StringUtils.isBlank((CharSequence)string)) {
            return CommonComponents.EMPTY;
        }
        return Component.literal(string).withColor(n);
    }

    public static Component getGameModeComponent(int n, boolean bl) {
        if (bl) {
            return Component.translatable("gameMode.hardcore").withColor(-65536);
        }
        return GameType.byId(n).getLongDisplayName();
    }

    static boolean isSelfOwnedServer(RealmsServer realmsServer) {
        return Minecraft.getInstance().isLocalPlayer(realmsServer.ownerUUID);
    }

    private boolean isSelfOwnedNonExpiredServer(RealmsServer realmsServer) {
        return RealmsMainScreen.isSelfOwnedServer(realmsServer) && !realmsServer.expired;
    }

    private void renderEnvironment(GuiGraphics guiGraphics, String string, int n) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.width / 2 - 25, 20.0f, 0.0f);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0f));
        guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
        guiGraphics.drawString(this.font, string, 0, 0, n);
        guiGraphics.pose().popPose();
    }

    static {
        snapshotToggle = SNAPSHOT = !SharedConstants.getCurrentVersion().isStable();
    }

    class RealmSelectionList
    extends ObjectSelectionList<Entry> {
        public RealmSelectionList() {
            super(Minecraft.getInstance(), RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
        }

        @Override
        public void setSelected(@Nullable Entry entry) {
            super.setSelected(entry);
            RealmsMainScreen.this.updateButtonStates();
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        void refreshEntries(RealmsMainScreen realmsMainScreen, @Nullable RealmsServer realmsServer) {
            this.clearEntries();
            for (RealmsNotification realmsNotification : RealmsMainScreen.this.notifications) {
                if (!(realmsNotification instanceof RealmsNotification.VisitUrl)) continue;
                RealmsNotification.VisitUrl visitUrl = (RealmsNotification.VisitUrl)realmsNotification;
                this.addEntriesForNotification(visitUrl, realmsMainScreen);
                RealmsMainScreen.this.markNotificationsAsSeen(List.of(realmsNotification));
                break;
            }
            this.refreshServerEntries(realmsServer);
        }

        private void refreshServerEntries(@Nullable RealmsServer realmsServer) {
            for (RealmsServer realmsServer2 : RealmsMainScreen.this.availableSnapshotServers) {
                this.addEntry(new AvailableSnapshotEntry(realmsServer2));
            }
            for (RealmsServer realmsServer2 : RealmsMainScreen.this.serverList) {
                Entry entry;
                if (RealmsMainScreen.isSnapshot() && !realmsServer2.isSnapshotRealm()) {
                    if (realmsServer2.state == RealmsServer.State.UNINITIALIZED) continue;
                    entry = new ParentEntry(realmsServer2);
                } else {
                    entry = new ServerEntry(realmsServer2);
                }
                this.addEntry(entry);
                if (realmsServer == null || realmsServer.id != realmsServer2.id) continue;
                this.setSelected(entry);
            }
        }

        private void addEntriesForNotification(RealmsNotification.VisitUrl visitUrl, RealmsMainScreen realmsMainScreen) {
            Component component = visitUrl.getMessage();
            int n = RealmsMainScreen.this.font.wordWrapHeight(component, 216);
            int n2 = Mth.positiveCeilDiv(n + 7, 36) - 1;
            this.addEntry(new NotificationMessageEntry(component, n2 + 2, visitUrl));
            for (int i = 0; i < n2; ++i) {
                this.addEntry(new EmptyEntry(RealmsMainScreen.this));
            }
            this.addEntry(new ButtonEntry(visitUrl.buildOpenLinkButton(realmsMainScreen)));
        }
    }

    static class NotificationButton
    extends SpriteIconButton.CenteredIcon {
        private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{ResourceLocation.withDefaultNamespace("notification/1"), ResourceLocation.withDefaultNamespace("notification/2"), ResourceLocation.withDefaultNamespace("notification/3"), ResourceLocation.withDefaultNamespace("notification/4"), ResourceLocation.withDefaultNamespace("notification/5"), ResourceLocation.withDefaultNamespace("notification/more")};
        private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
        private static final int SIZE = 20;
        private static final int SPRITE_SIZE = 14;
        private int notificationCount;

        public NotificationButton(Component component, ResourceLocation resourceLocation, Button.OnPress onPress) {
            super(20, 20, component, 14, 14, resourceLocation, onPress, null);
        }

        int notificationCount() {
            return this.notificationCount;
        }

        public void setNotificationCount(int n) {
            this.notificationCount = n;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int n, int n2, float f) {
            super.renderWidget(guiGraphics, n, n2, f);
            if (this.active && this.notificationCount != 0) {
                this.drawNotificationCounter(guiGraphics);
            }
        }

        private void drawNotificationCounter(GuiGraphics guiGraphics) {
            guiGraphics.blitSprite(RenderType::guiTextured, NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8);
        }
    }

    static enum LayoutState {
        LOADING,
        NO_REALMS,
        LIST;

    }

    static interface RealmsCall<T> {
        public T request(RealmsClient var1) throws RealmsServiceException;
    }

    class ServerEntry
    extends Entry {
        private static final Component ONLINE_PLAYERS_TOOLTIP_HEADER = Component.translatable("mco.onlinePlayers");
        private static final int PLAYERS_ONLINE_SPRITE_SIZE = 9;
        private static final int SKIN_HEAD_LARGE_WIDTH = 36;
        private final RealmsServer serverData;
        private final WidgetTooltipHolder tooltip;

        public ServerEntry(RealmsServer realmsServer) {
            this.tooltip = new WidgetTooltipHolder();
            this.serverData = realmsServer;
            boolean bl = RealmsMainScreen.isSelfOwnedServer(realmsServer);
            if (RealmsMainScreen.isSnapshot() && bl && realmsServer.isSnapshotRealm()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", realmsServer.parentWorldName)));
            } else if (!bl && realmsServer.needsDowngrade()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", realmsServer.activeVersion)));
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                guiGraphics.blitSprite(RenderType::guiTextured, NEW_REALM_SPRITE, n3 - 5, n2 + n5 / 2 - 10, 40, 20);
                int n8 = n2 + n5 / 2 - ((RealmsMainScreen)RealmsMainScreen.this).font.lineHeight / 2;
                guiGraphics.drawString(RealmsMainScreen.this.font, SERVER_UNITIALIZED_TEXT, n3 + 40 - 2, n8, 0x7FFF7F);
                return;
            }
            this.renderStatusLights(this.serverData, guiGraphics, n3 + 36, n2, n6, n7);
            RealmsUtil.renderPlayerFace(guiGraphics, n3, n2, 32, this.serverData.ownerUUID);
            this.renderFirstLine(guiGraphics, n2, n3, n4);
            this.renderSecondLine(guiGraphics, n2, n3, n4);
            this.renderThirdLine(guiGraphics, n2, n3, this.serverData);
            boolean bl2 = this.renderOnlinePlayers(guiGraphics, n2, n3, n4, n5, n6, n7);
            this.renderStatusLights(this.serverData, guiGraphics, n3 + n4, n2, n6, n7);
            if (!bl2) {
                this.tooltip.refreshTooltipForNextRenderPass(bl, this.isFocused(), new ScreenRectangle(n3, n2, n4, n5));
            }
        }

        private void renderFirstLine(GuiGraphics guiGraphics, int n, int n2, int n3) {
            int n4 = this.textX(n2);
            int n5 = this.firstLineY(n);
            Component component = RealmsMainScreen.getVersionComponent(this.serverData.activeVersion, this.serverData.isCompatible());
            int n6 = this.versionTextX(n2, n3, component);
            this.renderClampedString(guiGraphics, this.serverData.getName(), n4, n5, n6, -1);
            if (component != CommonComponents.EMPTY && !this.serverData.isMinigameActive()) {
                guiGraphics.drawString(RealmsMainScreen.this.font, component, n6, n5, -8355712);
            }
        }

        private void renderSecondLine(GuiGraphics guiGraphics, int n, int n2, int n3) {
            int n4 = this.textX(n2);
            int n5 = this.firstLineY(n);
            int n6 = this.secondLineY(n5);
            String string = this.serverData.getMinigameName();
            boolean bl = this.serverData.isMinigameActive();
            if (bl && string != null) {
                MutableComponent mutableComponent = Component.literal(string).withStyle(ChatFormatting.GRAY);
                guiGraphics.drawString(RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", mutableComponent).withColor(-171), n4, n6, -1);
            } else {
                int n7 = this.renderGameMode(this.serverData, guiGraphics, n2, n3, n5);
                this.renderClampedString(guiGraphics, this.serverData.getDescription(), n4, this.secondLineY(n5), n7, -8355712);
            }
        }

        private boolean renderOnlinePlayers(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6) {
            List<ProfileResult> list = RealmsMainScreen.this.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
            if (!list.isEmpty()) {
                int n7 = n2 + n3 - 21;
                int n8 = n + n4 - 9 - 2;
                int n9 = n7;
                for (int i = 0; i < list.size(); ++i) {
                    PlayerFaceRenderer.draw(guiGraphics, Minecraft.getInstance().getSkinManager().getInsecureSkin(list.get(i).profile()), n9 -= 9 + (i == 0 ? 0 : 3), n8, 9);
                }
                if (n5 >= n9 && n5 <= n7 && n6 >= n8 && n6 <= n8 + 9) {
                    guiGraphics.renderTooltip(RealmsMainScreen.this.font, List.of(ONLINE_PLAYERS_TOOLTIP_HEADER), Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip(list)), n5, n6);
                    return true;
                }
            }
            return false;
        }

        private void playRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
        }

        private void createUnitializedRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            RealmsCreateRealmScreen realmsCreateRealmScreen = new RealmsCreateRealmScreen(RealmsMainScreen.this, this.serverData, this.serverData.isSnapshotRealm());
            RealmsMainScreen.this.minecraft.setScreen(realmsCreateRealmScreen);
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                this.createUnitializedRealm();
            } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
                if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
                    this.playRealm();
                }
                RealmsMainScreen.this.lastClickTime = Util.getMillis();
            }
            return true;
        }

        @Override
        public boolean keyPressed(int n, int n2, int n3) {
            if (CommonInputs.selected(n)) {
                if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                    this.createUnitializedRealm();
                    return true;
                }
                if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
                    this.playRealm();
                    return true;
                }
            }
            return super.keyPressed(n, n2, n3);
        }

        @Override
        public Component getNarration() {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                return UNITIALIZED_WORLD_NARRATION;
            }
            return Component.translatable("narrator.select", Objects.requireNonNullElse(this.serverData.name, "unknown server"));
        }

        public RealmsServer getServer() {
            return this.serverData;
        }
    }

    abstract class Entry
    extends ObjectSelectionList.Entry<Entry> {
        protected static final int STATUS_LIGHT_WIDTH = 10;
        private static final int STATUS_LIGHT_HEIGHT = 28;
        protected static final int PADDING_X = 7;
        protected static final int PADDING_Y = 2;

        Entry() {
        }

        protected void renderStatusLights(RealmsServer realmsServer, GuiGraphics guiGraphics, int n, int n2, int n3, int n4) {
            int n5 = n - 10 - 7;
            int n6 = n2 + 2;
            if (realmsServer.expired) {
                this.drawRealmStatus(guiGraphics, n5, n6, n3, n4, EXPIRED_SPRITE, () -> SERVER_EXPIRED_TOOLTIP);
            } else if (realmsServer.state == RealmsServer.State.CLOSED) {
                this.drawRealmStatus(guiGraphics, n5, n6, n3, n4, CLOSED_SPRITE, () -> SERVER_CLOSED_TOOLTIP);
            } else if (RealmsMainScreen.isSelfOwnedServer(realmsServer) && realmsServer.daysLeft < 7) {
                this.drawRealmStatus(guiGraphics, n5, n6, n3, n4, EXPIRES_SOON_SPRITE, () -> {
                    if (realmsServer.daysLeft <= 0) {
                        return SERVER_EXPIRES_SOON_TOOLTIP;
                    }
                    if (realmsServer.daysLeft == 1) {
                        return SERVER_EXPIRES_IN_DAY_TOOLTIP;
                    }
                    return Component.translatable("mco.selectServer.expires.days", realmsServer.daysLeft);
                });
            } else if (realmsServer.state == RealmsServer.State.OPEN) {
                this.drawRealmStatus(guiGraphics, n5, n6, n3, n4, OPEN_SPRITE, () -> SERVER_OPEN_TOOLTIP);
            }
        }

        private void drawRealmStatus(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, ResourceLocation resourceLocation, Supplier<Component> supplier) {
            guiGraphics.blitSprite(RenderType::guiTextured, resourceLocation, n, n2, 10, 28);
            if (RealmsMainScreen.this.realmSelectionList.isMouseOver(n3, n4) && n3 >= n && n3 <= n + 10 && n4 >= n2 && n4 <= n2 + 28) {
                RealmsMainScreen.this.setTooltipForNextRenderPass(supplier.get());
            }
        }

        protected void renderThirdLine(GuiGraphics guiGraphics, int n, int n2, RealmsServer realmsServer) {
            int n3 = this.textX(n2);
            int n4 = this.firstLineY(n);
            int n5 = this.thirdLineY(n4);
            if (!RealmsMainScreen.isSelfOwnedServer(realmsServer)) {
                guiGraphics.drawString(RealmsMainScreen.this.font, realmsServer.owner, n3, this.thirdLineY(n4), -8355712);
            } else if (realmsServer.expired) {
                Component component = realmsServer.expiredTrial ? TRIAL_EXPIRED_TEXT : SUBSCRIPTION_EXPIRED_TEXT;
                guiGraphics.drawString(RealmsMainScreen.this.font, component, n3, n5, -2142128);
            }
        }

        protected void renderClampedString(GuiGraphics guiGraphics, @Nullable String string, int n, int n2, int n3, int n4) {
            if (string == null) {
                return;
            }
            int n5 = n3 - n;
            if (RealmsMainScreen.this.font.width(string) > n5) {
                String string2 = RealmsMainScreen.this.font.plainSubstrByWidth(string, n5 - RealmsMainScreen.this.font.width("... "));
                guiGraphics.drawString(RealmsMainScreen.this.font, string2 + "...", n, n2, n4);
            } else {
                guiGraphics.drawString(RealmsMainScreen.this.font, string, n, n2, n4);
            }
        }

        protected int versionTextX(int n, int n2, Component component) {
            return n + n2 - RealmsMainScreen.this.font.width(component) - 20;
        }

        protected int gameModeTextX(int n, int n2, Component component) {
            return n + n2 - RealmsMainScreen.this.font.width(component) - 20;
        }

        protected int renderGameMode(RealmsServer realmsServer, GuiGraphics guiGraphics, int n, int n2, int n3) {
            boolean bl = realmsServer.isHardcore;
            int n4 = realmsServer.gameMode;
            int n5 = n;
            if (GameType.isValidId(n4)) {
                Component component = RealmsMainScreen.getGameModeComponent(n4, bl);
                n5 = this.gameModeTextX(n, n2, component);
                guiGraphics.drawString(RealmsMainScreen.this.font, component, n5, this.secondLineY(n3), -8355712);
            }
            if (bl) {
                guiGraphics.blitSprite(RenderType::guiTextured, HARDCORE_MODE_SPRITE, n5 -= 10, this.secondLineY(n3), 8, 8);
            }
            return n5;
        }

        protected int firstLineY(int n) {
            return n + 1;
        }

        protected int lineHeight() {
            return 2 + ((RealmsMainScreen)RealmsMainScreen.this).font.lineHeight;
        }

        protected int textX(int n) {
            return n + 36 + 2;
        }

        protected int secondLineY(int n) {
            return n + this.lineHeight();
        }

        protected int thirdLineY(int n) {
            return n + this.lineHeight() * 2;
        }
    }

    static class CrossButton
    extends ImageButton {
        private static final WidgetSprites SPRITES = new WidgetSprites(ResourceLocation.withDefaultNamespace("widget/cross_button"), ResourceLocation.withDefaultNamespace("widget/cross_button_highlighted"));

        protected CrossButton(Button.OnPress onPress, Component component) {
            super(0, 0, 14, 14, SPRITES, onPress);
            this.setTooltip(Tooltip.create(component));
        }
    }

    class ParentEntry
    extends Entry {
        private final RealmsServer server;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public ParentEntry(RealmsServer realmsServer) {
            this.server = realmsServer;
            if (!realmsServer.expired) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip")));
            }
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            int n8 = this.textX(n3);
            int n9 = this.firstLineY(n2);
            RealmsUtil.renderPlayerFace(guiGraphics, n3, n2, 32, this.server.ownerUUID);
            Component component = RealmsMainScreen.getVersionComponent(this.server.activeVersion, -8355712);
            int n10 = this.versionTextX(n3, n4, component);
            this.renderClampedString(guiGraphics, this.server.getName(), n8, n9, n10, -8355712);
            if (component != CommonComponents.EMPTY) {
                guiGraphics.drawString(RealmsMainScreen.this.font, component, n10, n9, -8355712);
            }
            int n11 = n3;
            if (!this.server.isMinigameActive()) {
                n11 = this.renderGameMode(this.server, guiGraphics, n3, n4, n9);
            }
            this.renderClampedString(guiGraphics, this.server.getDescription(), n8, this.secondLineY(n9), n11, -8355712);
            this.renderThirdLine(guiGraphics, n2, n3, this.server);
            this.renderStatusLights(this.server, guiGraphics, n3 + n4, n2, n6, n7);
            this.tooltip.refreshTooltipForNextRenderPass(bl, this.isFocused(), new ScreenRectangle(n3, n2, n4, n5));
        }

        @Override
        public Component getNarration() {
            return Component.literal(Objects.requireNonNullElse(this.server.name, "unknown server"));
        }
    }

    class AvailableSnapshotEntry
    extends Entry {
        private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
        private static final int TEXT_PADDING = 5;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
        private final RealmsServer parent;

        public AvailableSnapshotEntry(RealmsServer realmsServer) {
            this.parent = realmsServer;
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.tooltip")));
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            guiGraphics.blitSprite(RenderType::guiTextured, NEW_REALM_SPRITE, n3 - 5, n2 + n5 / 2 - 10, 40, 20);
            int n8 = n2 + n5 / 2 - ((RealmsMainScreen)RealmsMainScreen.this).font.lineHeight / 2;
            guiGraphics.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, n3 + 40 - 2, n8 - 5, 0x7FFF7F);
            guiGraphics.drawString(RealmsMainScreen.this.font, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server")), n3 + 40 - 2, n8 + 5, -8355712);
            this.tooltip.refreshTooltipForNextRenderPass(bl, this.isFocused(), new ScreenRectangle(n3, n2, n4, n5));
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            this.addSnapshotRealm();
            return true;
        }

        @Override
        public boolean keyPressed(int n, int n2, int n3) {
            if (CommonInputs.selected(n)) {
                this.addSnapshotRealm();
                return false;
            }
            return super.keyPressed(n, n2, n3);
        }

        private void addSnapshotRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            RealmsMainScreen.this.minecraft.setScreen(new PopupScreen.Builder(RealmsMainScreen.this, Component.translatable("mco.snapshot.createSnapshotPopup.title")).setMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text")).addButton(Component.translatable("mco.selectServer.create"), popupScreen -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent, true))).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
        }

        @Override
        public Component getNarration() {
            return Component.translatable("gui.narrate.button", CommonComponents.joinForNarration(START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server"))));
        }
    }

    class ButtonEntry
    extends Entry {
        private final Button button;

        public ButtonEntry(Button button) {
            this.button = button;
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            this.button.mouseClicked(d, d2, n);
            return super.mouseClicked(d, d2, n);
        }

        @Override
        public boolean keyPressed(int n, int n2, int n3) {
            if (this.button.keyPressed(n, n2, n3)) {
                return true;
            }
            return super.keyPressed(n, n2, n3);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            this.button.setPosition(RealmsMainScreen.this.width / 2 - 75, n2 + 4);
            this.button.render(guiGraphics, n6, n7, f);
        }

        @Override
        public void setFocused(boolean bl) {
            super.setFocused(bl);
            this.button.setFocused(bl);
        }

        @Override
        public Component getNarration() {
            return this.button.getMessage();
        }
    }

    class EmptyEntry
    extends Entry {
        EmptyEntry(RealmsMainScreen realmsMainScreen) {
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }

    class NotificationMessageEntry
    extends Entry {
        private static final int SIDE_MARGINS = 40;
        private static final int OUTLINE_COLOR = -12303292;
        private final Component text;
        private final int frameItemHeight;
        private final List<AbstractWidget> children = new ArrayList<AbstractWidget>();
        @Nullable
        private final CrossButton dismissButton;
        private final MultiLineTextWidget textWidget;
        private final GridLayout gridLayout;
        private final FrameLayout textFrame;
        private int lastEntryWidth = -1;

        public NotificationMessageEntry(Component component, int n, RealmsNotification realmsNotification) {
            this.text = component;
            this.frameItemHeight = n;
            this.gridLayout = new GridLayout();
            int n2 = 7;
            this.gridLayout.addChild(ImageWidget.sprite(20, 20, INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
            this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
            this.textFrame = this.gridLayout.addChild(new FrameLayout(0, ((RealmsMainScreen)RealmsMainScreen.this).font.lineHeight * 3 * (n - 1)), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
            this.textWidget = this.textFrame.addChild(new MultiLineTextWidget(component, RealmsMainScreen.this.font).setCentered(true), this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop());
            this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
            this.dismissButton = realmsNotification.dismissable() ? this.gridLayout.addChild(new CrossButton(button -> RealmsMainScreen.this.dismissNotification(realmsNotification.uuid()), Component.translatable("mco.notification.dismiss")), 0, 2, this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)) : null;
            this.gridLayout.visitWidgets(this.children::add);
        }

        @Override
        public boolean keyPressed(int n, int n2, int n3) {
            if (this.dismissButton != null && this.dismissButton.keyPressed(n, n2, n3)) {
                return true;
            }
            return super.keyPressed(n, n2, n3);
        }

        private void updateEntryWidth(int n) {
            if (this.lastEntryWidth != n) {
                this.refreshLayout(n);
                this.lastEntryWidth = n;
            }
        }

        private void refreshLayout(int n) {
            int n2 = n - 80;
            this.textFrame.setMinWidth(n2);
            this.textWidget.setMaxWidth(n2);
            this.gridLayout.arrangeElements();
        }

        @Override
        public void renderBack(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            super.renderBack(guiGraphics, n, n2, n3, n4, n5, n6, n7, bl, f);
            guiGraphics.renderOutline(n3 - 2, n2 - 2, n4, 36 * this.frameItemHeight - 2, -12303292);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            this.gridLayout.setPosition(n3, n2);
            this.updateEntryWidth(n4 - 4);
            this.children.forEach(abstractWidget -> abstractWidget.render(guiGraphics, n6, n7, f));
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            if (this.dismissButton != null) {
                this.dismissButton.mouseClicked(d, d2, n);
            }
            return super.mouseClicked(d, d2, n);
        }

        @Override
        public Component getNarration() {
            return this.text;
        }
    }
}

