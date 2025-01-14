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
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RowButton;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsPendingInvitesScreen
extends RealmsScreen {
    static final ResourceLocation ACCEPT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("pending_invite/accept_highlighted");
    static final ResourceLocation ACCEPT_SPRITE = ResourceLocation.withDefaultNamespace("pending_invite/accept");
    static final ResourceLocation REJECT_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("pending_invite/reject_highlighted");
    static final ResourceLocation REJECT_SPRITE = ResourceLocation.withDefaultNamespace("pending_invite/reject");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component NO_PENDING_INVITES_TEXT = Component.translatable("mco.invites.nopending");
    static final Component ACCEPT_INVITE = Component.translatable("mco.invites.button.accept");
    static final Component REJECT_INVITE = Component.translatable("mco.invites.button.reject");
    private final Screen lastScreen;
    private final CompletableFuture<List<PendingInvite>> pendingInvites = CompletableFuture.supplyAsync(() -> {
        try {
            return RealmsClient.create().pendingInvites().pendingInvites;
        }
        catch (RealmsServiceException realmsServiceException) {
            LOGGER.error("Couldn't list invites", (Throwable)realmsServiceException);
            return List.of();
        }
    }, Util.ioPool());
    @Nullable
    Component toolTip;
    PendingInvitationSelectionList pendingInvitationSelectionList;
    private Button acceptButton;
    private Button rejectButton;

    public RealmsPendingInvitesScreen(Screen screen, Component component) {
        super(component);
        this.lastScreen = screen;
    }

    @Override
    public void init() {
        RealmsMainScreen.refreshPendingInvites();
        this.pendingInvitationSelectionList = new PendingInvitationSelectionList();
        this.pendingInvites.thenAcceptAsync(list -> {
            List<Entry> list2 = list.stream().map(pendingInvite -> new Entry((PendingInvite)pendingInvite)).toList();
            this.pendingInvitationSelectionList.replaceEntries(list2);
            if (list2.isEmpty()) {
                this.minecraft.getNarrator().say(NO_PENDING_INVITES_TEXT);
            }
        }, this.screenExecutor);
        this.addRenderableWidget(this.pendingInvitationSelectionList);
        this.acceptButton = this.addRenderableWidget(Button.builder(ACCEPT_INVITE, button -> this.handleInvitation(true)).bounds(this.width / 2 - 174, this.height - 32, 100, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 50, this.height - 32, 100, 20).build());
        this.rejectButton = this.addRenderableWidget(Button.builder(REJECT_INVITE, button -> this.handleInvitation(false)).bounds(this.width / 2 + 74, this.height - 32, 100, 20).build());
        this.updateButtonStates();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    void handleInvitation(boolean bl) {
        Object object = this.pendingInvitationSelectionList.getSelected();
        if (object instanceof Entry) {
            Entry entry = (Entry)object;
            object = entry.pendingInvite.invitationId;
            CompletableFuture.supplyAsync(() -> {
                try {
                    RealmsClient realmsClient = RealmsClient.create();
                    if (bl) {
                        realmsClient.acceptInvitation((String)object);
                    } else {
                        realmsClient.rejectInvitation((String)object);
                    }
                    return true;
                }
                catch (RealmsServiceException realmsServiceException) {
                    LOGGER.error("Couldn't handle invite", (Throwable)realmsServiceException);
                    return false;
                }
            }, Util.ioPool()).thenAcceptAsync(bl2 -> {
                if (bl2.booleanValue()) {
                    this.pendingInvitationSelectionList.removeInvitation(entry);
                    this.updateButtonStates();
                    RealmsDataFetcher realmsDataFetcher = this.minecraft.realmsDataFetcher();
                    if (bl) {
                        realmsDataFetcher.serverListUpdateTask.reset();
                    }
                    realmsDataFetcher.pendingInvitesTask.reset();
                }
            }, this.screenExecutor);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        this.toolTip = null;
        super.render(guiGraphics, n, n2, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, -1);
        if (this.toolTip != null) {
            guiGraphics.renderTooltip(this.font, this.toolTip, n, n2);
        }
        if (this.pendingInvites.isDone() && this.pendingInvitationSelectionList.hasPendingInvites()) {
            guiGraphics.drawCenteredString(this.font, NO_PENDING_INVITES_TEXT, this.width / 2, this.height / 2 - 20, -1);
        }
    }

    void updateButtonStates() {
        Entry entry = (Entry)this.pendingInvitationSelectionList.getSelected();
        this.acceptButton.visible = entry != null;
        this.rejectButton.visible = entry != null;
    }

    class PendingInvitationSelectionList
    extends ObjectSelectionList<Entry> {
        public PendingInvitationSelectionList() {
            super(Minecraft.getInstance(), RealmsPendingInvitesScreen.this.width, RealmsPendingInvitesScreen.this.height - 72, 32, 36);
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        @Override
        public void setSelectedIndex(int n) {
            super.setSelectedIndex(n);
            RealmsPendingInvitesScreen.this.updateButtonStates();
        }

        public boolean hasPendingInvites() {
            return this.getItemCount() == 0;
        }

        public void removeInvitation(Entry entry) {
            this.removeEntry(entry);
        }
    }

    class Entry
    extends ObjectSelectionList.Entry<Entry> {
        private static final int TEXT_LEFT = 38;
        final PendingInvite pendingInvite;
        private final List<RowButton> rowButtons;

        Entry(PendingInvite pendingInvite) {
            this.pendingInvite = pendingInvite;
            this.rowButtons = Arrays.asList(new AcceptRowButton(), new RejectRowButton());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            this.renderPendingInvitationItem(guiGraphics, this.pendingInvite, n3, n2, n6, n7);
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            RowButton.rowButtonMouseClicked(RealmsPendingInvitesScreen.this.pendingInvitationSelectionList, this, this.rowButtons, n, d, d2);
            return super.mouseClicked(d, d2, n);
        }

        private void renderPendingInvitationItem(GuiGraphics guiGraphics, PendingInvite pendingInvite, int n, int n2, int n3, int n4) {
            guiGraphics.drawString(RealmsPendingInvitesScreen.this.font, pendingInvite.realmName, n + 38, n2 + 1, -1);
            guiGraphics.drawString(RealmsPendingInvitesScreen.this.font, pendingInvite.realmOwnerName, n + 38, n2 + 12, 0x6C6C6C);
            guiGraphics.drawString(RealmsPendingInvitesScreen.this.font, RealmsUtil.convertToAgePresentationFromInstant(pendingInvite.date), n + 38, n2 + 24, 0x6C6C6C);
            RowButton.drawButtonsInRow(guiGraphics, this.rowButtons, RealmsPendingInvitesScreen.this.pendingInvitationSelectionList, n, n2, n3, n4);
            RealmsUtil.renderPlayerFace(guiGraphics, n, n2, 32, pendingInvite.realmOwnerUuid);
        }

        @Override
        public Component getNarration() {
            Component component = CommonComponents.joinLines(Component.literal(this.pendingInvite.realmName), Component.literal(this.pendingInvite.realmOwnerName), RealmsUtil.convertToAgePresentationFromInstant(this.pendingInvite.date));
            return Component.translatable("narrator.select", component);
        }

        class AcceptRowButton
        extends RowButton {
            AcceptRowButton() {
                super(15, 15, 215, 5);
            }

            @Override
            protected void draw(GuiGraphics guiGraphics, int n, int n2, boolean bl) {
                guiGraphics.blitSprite(RenderType::guiTextured, bl ? ACCEPT_HIGHLIGHTED_SPRITE : ACCEPT_SPRITE, n, n2, 18, 18);
                if (bl) {
                    RealmsPendingInvitesScreen.this.toolTip = ACCEPT_INVITE;
                }
            }

            @Override
            public void onClick(int n) {
                RealmsPendingInvitesScreen.this.handleInvitation(true);
            }
        }

        class RejectRowButton
        extends RowButton {
            RejectRowButton() {
                super(15, 15, 235, 5);
            }

            @Override
            protected void draw(GuiGraphics guiGraphics, int n, int n2, boolean bl) {
                guiGraphics.blitSprite(RenderType::guiTextured, bl ? REJECT_HIGHLIGHTED_SPRITE : REJECT_SPRITE, n, n2, 18, 18);
                if (bl) {
                    RealmsPendingInvitesScreen.this.toolTip = REJECT_INVITE;
                }
            }

            @Override
            public void onClick(int n) {
                RealmsPendingInvitesScreen.this.handleInvitation(false);
            }
        }
    }
}

