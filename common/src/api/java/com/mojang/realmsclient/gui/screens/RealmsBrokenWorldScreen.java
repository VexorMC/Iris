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
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class RealmsBrokenWorldScreen
extends RealmsScreen {
    private static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_BUTTON_WIDTH = 80;
    private final Screen lastScreen;
    @Nullable
    private RealmsServer serverData;
    private final long serverId;
    private final Component[] message = new Component[]{Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")};
    private int leftX;
    private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
    private int animTick;

    public RealmsBrokenWorldScreen(Screen screen, long l, boolean bl) {
        super(bl ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
        this.lastScreen = screen;
        this.serverId = l;
    }

    @Override
    public void init() {
        this.leftX = this.width / 2 - 150;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).bounds((this.width - 150) / 2, RealmsBrokenWorldScreen.row(13) - 5, 150, 20).build());
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        } else {
            this.addButtons();
        }
    }

    @Override
    public Component getNarrationMessage() {
        return ComponentUtils.formatList(Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), CommonComponents.SPACE);
    }

    private void addButtons() {
        for (Map.Entry<Integer, RealmsWorldOptions> entry : this.serverData.slots.entrySet()) {
            Button button2;
            boolean bl;
            int n = entry.getKey();
            boolean bl2 = bl = n != this.serverData.activeSlot || this.serverData.isMinigameActive();
            if (bl) {
                button2 = Button.builder(Component.translatable("mco.brokenworld.play"), button -> this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, n, this::doSwitchOrReset)))).bounds(this.getFramePositionX(n), RealmsBrokenWorldScreen.row(8), 80, 20).build();
                button2.active = !this.serverData.slots.get((Object)Integer.valueOf((int)n)).empty;
            } else {
                button2 = Button.builder(Component.translatable("mco.brokenworld.download"), button -> this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, Component.translatable("mco.configure.world.restore.download.question.line1"), popupScreen -> this.downloadWorld(n)))).bounds(this.getFramePositionX(n), RealmsBrokenWorldScreen.row(8), 80, 20).build();
            }
            if (this.slotsThatHasBeenDownloaded.contains(n)) {
                button2.active = false;
                button2.setMessage(Component.translatable("mco.brokenworld.downloaded"));
            }
            this.addRenderableWidget(button2);
        }
    }

    @Override
    public void tick() {
        ++this.animTick;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
        for (int i = 0; i < this.message.length; ++i) {
            guiGraphics.drawCenteredString(this.font, this.message[i], this.width / 2, RealmsBrokenWorldScreen.row(-1) + 3 + i * 12, -6250336);
        }
        if (this.serverData == null) {
            return;
        }
        for (Map.Entry<Integer, RealmsWorldOptions> entry : this.serverData.slots.entrySet()) {
            if (entry.getValue().templateImage != null && entry.getValue().templateId != -1L) {
                this.drawSlotFrame(guiGraphics, this.getFramePositionX(entry.getKey()), RealmsBrokenWorldScreen.row(1) + 5, n, n2, this.serverData.activeSlot == entry.getKey() && !this.isMinigame(), entry.getValue().getSlotName(entry.getKey()), entry.getKey(), entry.getValue().templateId, entry.getValue().templateImage, entry.getValue().empty);
                continue;
            }
            this.drawSlotFrame(guiGraphics, this.getFramePositionX(entry.getKey()), RealmsBrokenWorldScreen.row(1) + 5, n, n2, this.serverData.activeSlot == entry.getKey() && !this.isMinigame(), entry.getValue().getSlotName(entry.getKey()), entry.getKey(), -1L, null, entry.getValue().empty);
        }
    }

    private int getFramePositionX(int n) {
        return this.leftX + (n - 1) * 110;
    }

    private void fetchServerData(long l) {
        new Thread(() -> {
            RealmsClient realmsClient = RealmsClient.create();
            try {
                this.serverData = realmsClient.getOwnRealm(l);
                this.addButtons();
            }
            catch (RealmsServiceException realmsServiceException) {
                LOGGER.error("Couldn't get own world", (Throwable)realmsServiceException);
                this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsServiceException, this.lastScreen));
            }
        }).start();
    }

    public void doSwitchOrReset() {
        new Thread(() -> {
            RealmsClient realmsClient = RealmsClient.create();
            if (this.serverData.state == RealmsServer.State.CLOSED) {
                this.minecraft.execute(() -> this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, true, this.minecraft))));
            } else {
                try {
                    RealmsServer realmsServer = realmsClient.getOwnRealm(this.serverId);
                    this.minecraft.execute(() -> RealmsMainScreen.play(realmsServer, this));
                }
                catch (RealmsServiceException realmsServiceException) {
                    LOGGER.error("Couldn't get own world", (Throwable)realmsServiceException);
                    this.minecraft.execute(() -> this.minecraft.setScreen(this.lastScreen));
                }
            }
        }).start();
    }

    private void downloadWorld(int n) {
        RealmsClient realmsClient = RealmsClient.create();
        try {
            WorldDownload worldDownload = realmsClient.requestDownloadInfo(this.serverData.id, n);
            RealmsDownloadLatestWorldScreen realmsDownloadLatestWorldScreen = new RealmsDownloadLatestWorldScreen(this, worldDownload, this.serverData.getWorldName(n), bl -> {
                if (bl) {
                    this.slotsThatHasBeenDownloaded.add(n);
                    this.clearWidgets();
                    this.addButtons();
                } else {
                    this.minecraft.setScreen(this);
                }
            });
            this.minecraft.setScreen(realmsDownloadLatestWorldScreen);
        }
        catch (RealmsServiceException realmsServiceException) {
            LOGGER.error("Couldn't download world data", (Throwable)realmsServiceException);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsServiceException, (Screen)this));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private boolean isMinigame() {
        return this.serverData != null && this.serverData.isMinigameActive();
    }

    private void drawSlotFrame(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, boolean bl, String string, int n5, long l, @Nullable String string2, boolean bl2) {
        ResourceLocation resourceLocation = bl2 ? RealmsWorldSlotButton.EMPTY_SLOT_LOCATION : (string2 != null && l != -1L ? RealmsTextureManager.worldTemplate(String.valueOf(l), string2) : (n5 == 1 ? RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1 : (n5 == 2 ? RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2 : (n5 == 3 ? RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3 : RealmsTextureManager.worldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage)))));
        if (bl) {
            float f = 0.9f + 0.1f * Mth.cos((float)this.animTick * 0.2f);
            guiGraphics.blit(RenderType::guiTextured, resourceLocation, n + 3, n2 + 3, 0.0f, 0.0f, 74, 74, 74, 74, 74, 74, ARGB.colorFromFloat(1.0f, f, f, f));
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_FRAME_SPRITE, n, n2, 80, 80);
        } else {
            int n6 = ARGB.colorFromFloat(1.0f, 0.56f, 0.56f, 0.56f);
            guiGraphics.blit(RenderType::guiTextured, resourceLocation, n + 3, n2 + 3, 0.0f, 0.0f, 74, 74, 74, 74, 74, 74, n6);
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_FRAME_SPRITE, n, n2, 80, 80, n6);
        }
        guiGraphics.drawCenteredString(this.font, string, n + 40, n2 + 66, -1);
    }
}

