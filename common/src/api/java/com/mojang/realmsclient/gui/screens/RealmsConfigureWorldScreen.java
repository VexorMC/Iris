/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.gui.screens.RealmsBackupScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPlayerScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsResetWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsSelectWorldTemplateScreen;
import com.mojang.realmsclient.gui.screens.RealmsSettingsScreen;
import com.mojang.realmsclient.gui.screens.RealmsSlotOptionsScreen;
import com.mojang.realmsclient.gui.screens.RealmsSubscriptionInfoScreen;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

public class RealmsConfigureWorldScreen
extends RealmsScreen {
    private static final ResourceLocation EXPIRED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expired");
    private static final ResourceLocation EXPIRES_SOON_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expires_soon");
    private static final ResourceLocation OPEN_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/open");
    private static final ResourceLocation CLOSED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/closed");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component WORLD_LIST_TITLE = Component.translatable("mco.configure.worlds.title");
    private static final Component TITLE = Component.translatable("mco.configure.world.title");
    private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
    private static final Component SERVER_EXPIRING_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
    private static final Component SERVER_EXPIRING_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
    private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
    private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
    private static final int DEFAULT_BUTTON_WIDTH = 80;
    private static final int DEFAULT_BUTTON_OFFSET = 5;
    @Nullable
    private Component toolTip;
    private final RealmsMainScreen lastScreen;
    @Nullable
    private RealmsServer serverData;
    private final long serverId;
    private int leftX;
    private int rightX;
    private Button playersButton;
    private Button settingsButton;
    private Button subscriptionButton;
    private Button optionsButton;
    private Button backupButton;
    private Button resetWorldButton;
    private Button switchMinigameButton;
    private boolean stateChanged;
    private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

    public RealmsConfigureWorldScreen(RealmsMainScreen realmsMainScreen, long l) {
        super(TITLE);
        this.lastScreen = realmsMainScreen;
        this.serverId = l;
    }

    @Override
    public void init() {
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        }
        this.leftX = this.width / 2 - 187;
        this.rightX = this.width / 2 + 190;
        this.playersButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.players"), button -> this.minecraft.setScreen(new RealmsPlayerScreen(this, this.serverData))).bounds(this.centerButton(0, 3), RealmsConfigureWorldScreen.row(0), 100, 20).build());
        this.settingsButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.settings"), button -> this.minecraft.setScreen(new RealmsSettingsScreen(this, this.serverData.clone()))).bounds(this.centerButton(1, 3), RealmsConfigureWorldScreen.row(0), 100, 20).build());
        this.subscriptionButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.subscription"), button -> this.minecraft.setScreen(new RealmsSubscriptionInfoScreen(this, this.serverData.clone(), this.lastScreen))).bounds(this.centerButton(2, 3), RealmsConfigureWorldScreen.row(0), 100, 20).build());
        this.slotButtonList.clear();
        for (int i = 1; i < 5; ++i) {
            this.slotButtonList.add(this.addSlotButton(i));
        }
        this.switchMinigameButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.switchminigame"), button -> this.minecraft.setScreen(new RealmsSelectWorldTemplateScreen(Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME))).bounds(this.leftButton(0), RealmsConfigureWorldScreen.row(13) - 5, 100, 20).build());
        this.optionsButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.options"), button -> this.minecraft.setScreen(new RealmsSlotOptionsScreen(this, this.serverData.slots.get(this.serverData.activeSlot).clone(), this.serverData.worldType, this.serverData.activeSlot))).bounds(this.leftButton(0), RealmsConfigureWorldScreen.row(13) - 5, 90, 20).build());
        this.backupButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.backup"), button -> this.minecraft.setScreen(new RealmsBackupScreen(this, this.serverData.clone(), this.serverData.activeSlot))).bounds(this.leftButton(1), RealmsConfigureWorldScreen.row(13) - 5, 90, 20).build());
        this.resetWorldButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.resetworld"), button -> this.minecraft.setScreen(RealmsResetWorldScreen.forResetSlot(this, this.serverData.clone(), () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen()))))).bounds(this.leftButton(2), RealmsConfigureWorldScreen.row(13) - 5, 90, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).bounds(this.rightX - 80 + 8, RealmsConfigureWorldScreen.row(13) - 5, 70, 20).build());
        this.backupButton.active = true;
        if (this.serverData == null) {
            this.hideMinigameButtons();
            this.hideRegularButtons();
            this.playersButton.active = false;
            this.settingsButton.active = false;
            this.subscriptionButton.active = false;
        } else {
            this.disableButtons();
            if (this.isMinigame()) {
                this.hideRegularButtons();
            } else {
                this.hideMinigameButtons();
            }
        }
    }

    private RealmsWorldSlotButton addSlotButton(int n) {
        int n2 = this.frame(n);
        int n3 = RealmsConfigureWorldScreen.row(5) + 5;
        RealmsWorldSlotButton realmsWorldSlotButton = new RealmsWorldSlotButton(n2, n3, 80, 80, n, button -> {
            RealmsWorldSlotButton.State state = ((RealmsWorldSlotButton)button).getState();
            if (state != null) {
                switch (state.action) {
                    case NOTHING: {
                        break;
                    }
                    case JOIN: {
                        this.joinRealm(this.serverData);
                        break;
                    }
                    case SWITCH_SLOT: {
                        if (state.minigame) {
                            this.switchToMinigame();
                            break;
                        }
                        if (state.empty) {
                            this.switchToEmptySlot(n, this.serverData);
                            break;
                        }
                        this.switchToFullSlot(n, this.serverData);
                        break;
                    }
                    default: {
                        throw new IllegalStateException("Unknown action " + String.valueOf((Object)state.action));
                    }
                }
            }
        });
        if (this.serverData != null) {
            realmsWorldSlotButton.setServerData(this.serverData);
        }
        return this.addRenderableWidget(realmsWorldSlotButton);
    }

    private int leftButton(int n) {
        return this.leftX + n * 95;
    }

    private int centerButton(int n, int n2) {
        return this.width / 2 - (n2 * 105 - 5) / 2 + n * 105;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        String string;
        super.render(guiGraphics, n, n2, f);
        this.toolTip = null;
        guiGraphics.drawCenteredString(this.font, WORLD_LIST_TITLE, this.width / 2, RealmsConfigureWorldScreen.row(4), -1);
        if (this.serverData == null) {
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
            return;
        }
        String string2 = Objects.requireNonNullElse(this.serverData.getName(), "");
        int n3 = this.font.width(string2);
        int n4 = this.serverData.state == RealmsServer.State.CLOSED ? -6250336 : 0x7FFF7F;
        int n5 = this.font.width(this.title);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, -1);
        guiGraphics.drawCenteredString(this.font, string2, this.width / 2, 24, n4);
        int n6 = Math.min(this.centerButton(2, 3) + 80 - 11, this.width / 2 + n3 / 2 + n5 / 2 + 10);
        this.drawServerStatus(guiGraphics, n6, 7, n, n2);
        if (this.isMinigame() && (string = this.serverData.getMinigameName()) != null) {
            guiGraphics.drawString(this.font, Component.translatable("mco.configure.world.minigame", string), this.leftX + 80 + 20 + 10, RealmsConfigureWorldScreen.row(13), -1);
        }
    }

    private int frame(int n) {
        return this.leftX + (n - 1) * 98;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
        if (this.stateChanged) {
            this.lastScreen.resetScreen();
        }
    }

    public void fetchServerData(long l) {
        new Thread(() -> {
            RealmsClient realmsClient = RealmsClient.create();
            try {
                RealmsServer realmsServer = realmsClient.getOwnRealm(l);
                this.minecraft.execute(() -> {
                    this.serverData = realmsServer;
                    this.disableButtons();
                    if (this.isMinigame()) {
                        this.show(this.switchMinigameButton);
                    } else {
                        this.show(this.optionsButton);
                        this.show(this.backupButton);
                        this.show(this.resetWorldButton);
                    }
                    for (RealmsWorldSlotButton realmsWorldSlotButton : this.slotButtonList) {
                        realmsWorldSlotButton.setServerData(realmsServer);
                    }
                });
            }
            catch (RealmsServiceException realmsServiceException) {
                LOGGER.error("Couldn't get own world", (Throwable)realmsServiceException);
                this.minecraft.execute(() -> this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsServiceException, (Screen)this.lastScreen)));
            }
        }).start();
    }

    private void disableButtons() {
        this.playersButton.active = !this.serverData.expired;
        this.settingsButton.active = !this.serverData.expired;
        this.subscriptionButton.active = true;
        this.switchMinigameButton.active = !this.serverData.expired;
        this.optionsButton.active = !this.serverData.expired;
        this.resetWorldButton.active = !this.serverData.expired;
    }

    private void joinRealm(RealmsServer realmsServer) {
        if (this.serverData.state == RealmsServer.State.OPEN) {
            RealmsMainScreen.play(realmsServer, this);
        } else {
            this.openTheWorld(true);
        }
    }

    private void switchToMinigame() {
        RealmsSelectWorldTemplateScreen realmsSelectWorldTemplateScreen = new RealmsSelectWorldTemplateScreen(Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME);
        realmsSelectWorldTemplateScreen.setWarning(Component.translatable("mco.minigame.world.info.line1"), Component.translatable("mco.minigame.world.info.line2"));
        this.minecraft.setScreen(realmsSelectWorldTemplateScreen);
    }

    private void switchToFullSlot(int n, RealmsServer realmsServer) {
        this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, Component.translatable("mco.configure.world.slot.switch.question.line1"), popupScreen -> {
            this.stateChanged();
            this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(realmsServer.id, n, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen())))));
        }));
    }

    private void switchToEmptySlot(int n, RealmsServer realmsServer) {
        this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, Component.translatable("mco.configure.world.slot.switch.question.line1"), popupScreen -> {
            this.stateChanged();
            RealmsResetWorldScreen realmsResetWorldScreen = RealmsResetWorldScreen.forEmptySlot(this, n, realmsServer, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen())));
            this.minecraft.setScreen(realmsResetWorldScreen);
        }));
    }

    private void drawServerStatus(GuiGraphics guiGraphics, int n, int n2, int n3, int n4) {
        if (this.serverData.expired) {
            this.drawRealmStatus(guiGraphics, n, n2, n3, n4, EXPIRED_SPRITE, () -> SERVER_EXPIRED_TOOLTIP);
        } else if (this.serverData.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(guiGraphics, n, n2, n3, n4, CLOSED_SPRITE, () -> SERVER_CLOSED_TOOLTIP);
        } else if (this.serverData.state == RealmsServer.State.OPEN) {
            if (this.serverData.daysLeft < 7) {
                this.drawRealmStatus(guiGraphics, n, n2, n3, n4, EXPIRES_SOON_SPRITE, () -> {
                    if (this.serverData.daysLeft <= 0) {
                        return SERVER_EXPIRING_SOON_TOOLTIP;
                    }
                    if (this.serverData.daysLeft == 1) {
                        return SERVER_EXPIRING_IN_DAY_TOOLTIP;
                    }
                    return Component.translatable("mco.selectServer.expires.days", this.serverData.daysLeft);
                });
            } else {
                this.drawRealmStatus(guiGraphics, n, n2, n3, n4, OPEN_SPRITE, () -> SERVER_OPEN_TOOLTIP);
            }
        }
    }

    private void drawRealmStatus(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, ResourceLocation resourceLocation, Supplier<Component> supplier) {
        guiGraphics.blitSprite(RenderType::guiTextured, resourceLocation, n, n2, 10, 28);
        if (n3 >= n && n3 <= n + 9 && n4 >= n2 && n4 <= n2 + 27) {
            this.setTooltipForNextRenderPass(supplier.get());
        }
    }

    private boolean isMinigame() {
        return this.serverData != null && this.serverData.isMinigameActive();
    }

    private void hideRegularButtons() {
        this.hide(this.optionsButton);
        this.hide(this.backupButton);
        this.hide(this.resetWorldButton);
    }

    private void hide(Button button) {
        button.visible = false;
    }

    private void show(Button button) {
        button.visible = true;
    }

    private void hideMinigameButtons() {
        this.hide(this.switchMinigameButton);
    }

    public void saveSlotSettings(RealmsWorldOptions realmsWorldOptions) {
        RealmsWorldOptions realmsWorldOptions2 = this.serverData.slots.get(this.serverData.activeSlot);
        realmsWorldOptions.templateId = realmsWorldOptions2.templateId;
        realmsWorldOptions.templateImage = realmsWorldOptions2.templateImage;
        RealmsClient realmsClient = RealmsClient.create();
        try {
            realmsClient.updateSlot(this.serverData.id, this.serverData.activeSlot, realmsWorldOptions);
            this.serverData.slots.put(this.serverData.activeSlot, realmsWorldOptions);
            if (realmsWorldOptions2.gameMode != realmsWorldOptions.gameMode || realmsWorldOptions2.hardcore != realmsWorldOptions.hardcore) {
                RealmsMainScreen.refreshServerList();
            }
        }
        catch (RealmsServiceException realmsServiceException) {
            LOGGER.error("Couldn't save slot settings", (Throwable)realmsServiceException);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsServiceException, (Screen)this));
            return;
        }
        this.minecraft.setScreen(this);
    }

    public void saveSettings(String string, String string2) {
        String string3 = StringUtil.isBlank(string2) ? "" : string2;
        RealmsClient realmsClient = RealmsClient.create();
        try {
            realmsClient.update(this.serverData.id, string, string3);
            this.serverData.setName(string);
            this.serverData.setDescription(string3);
            this.stateChanged();
        }
        catch (RealmsServiceException realmsServiceException) {
            LOGGER.error("Couldn't save settings", (Throwable)realmsServiceException);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsServiceException, (Screen)this));
            return;
        }
        this.minecraft.setScreen(this);
    }

    public void openTheWorld(boolean bl) {
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = this.getNewScreen();
        this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(realmsConfigureWorldScreen, new OpenServerTask(this.serverData, realmsConfigureWorldScreen, bl, this.minecraft)));
    }

    public void closeTheWorld() {
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = this.getNewScreen();
        this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(realmsConfigureWorldScreen, new CloseServerTask(this.serverData, realmsConfigureWorldScreen)));
    }

    public void stateChanged() {
        this.stateChanged = true;
    }

    private void templateSelectionCallback(@Nullable WorldTemplate worldTemplate) {
        if (worldTemplate != null && WorldTemplate.WorldTemplateType.MINIGAME == worldTemplate.type) {
            this.stateChanged();
            this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchMinigameTask(this.serverData.id, worldTemplate, this.getNewScreen())));
        } else {
            this.minecraft.setScreen(this);
        }
    }

    public RealmsConfigureWorldScreen getNewScreen() {
        RealmsConfigureWorldScreen realmsConfigureWorldScreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
        realmsConfigureWorldScreen.stateChanged = this.stateChanged;
        return realmsConfigureWorldScreen;
    }
}

