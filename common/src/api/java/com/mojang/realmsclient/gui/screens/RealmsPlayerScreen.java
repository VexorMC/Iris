/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfirmScreen;
import com.mojang.realmsclient.gui.screens.RealmsInviteScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsPlayerScreen
extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.configure.world.players.title");
    static final Component QUESTION_TITLE = Component.translatable("mco.question");
    private static final int PADDING = 8;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final RealmsConfigureWorldScreen lastScreen;
    final RealmsServer serverData;
    @Nullable
    private InvitedObjectSelectionList invitedList;
    boolean stateChanged;

    public RealmsPlayerScreen(RealmsConfigureWorldScreen realmsConfigureWorldScreen, RealmsServer realmsServer) {
        super(TITLE);
        this.lastScreen = realmsConfigureWorldScreen;
        this.serverData = realmsServer;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.invitedList = this.layout.addToContents(new InvitedObjectSelectionList());
        this.repopulateInvitedList();
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearLayout.addChild(Button.builder(Component.translatable("mco.configure.world.buttons.invite"), button -> this.minecraft.setScreen(new RealmsInviteScreen(this.lastScreen, this, this.serverData))).build());
        linearLayout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.layout.visitWidgets(guiEventListener -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.invitedList != null) {
            this.invitedList.updateSize(this.width, this.layout);
        }
    }

    void repopulateInvitedList() {
        if (this.invitedList == null) {
            return;
        }
        this.invitedList.children().clear();
        for (PlayerInfo playerInfo : this.serverData.players) {
            this.invitedList.children().add(new Entry(playerInfo));
        }
    }

    @Override
    public void onClose() {
        this.backButtonClicked();
    }

    private void backButtonClicked() {
        if (this.stateChanged) {
            this.minecraft.setScreen(this.lastScreen.getNewScreen());
        } else {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    class InvitedObjectSelectionList
    extends ContainerObjectSelectionList<Entry> {
        private static final int ITEM_HEIGHT = 36;

        public InvitedObjectSelectionList() {
            Minecraft minecraft = Minecraft.getInstance();
            int n = RealmsPlayerScreen.this.width;
            int n2 = RealmsPlayerScreen.this.layout.getContentHeight();
            int n3 = RealmsPlayerScreen.this.layout.getHeaderHeight();
            Objects.requireNonNull(RealmsPlayerScreen.this.font);
            super(minecraft, n, n2, n3, 36, (int)(9.0f * 1.5f));
        }

        @Override
        protected void renderHeader(GuiGraphics guiGraphics, int n, int n2) {
            String string = RealmsPlayerScreen.this.serverData.players != null ? Integer.toString(RealmsPlayerScreen.this.serverData.players.size()) : "0";
            MutableComponent mutableComponent = Component.translatable("mco.configure.world.invited.number", string).withStyle(ChatFormatting.UNDERLINE);
            guiGraphics.drawString(RealmsPlayerScreen.this.font, mutableComponent, n + this.getRowWidth() / 2 - RealmsPlayerScreen.this.font.width(mutableComponent) / 2, n2, -1);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    class Entry
    extends ContainerObjectSelectionList.Entry<Entry> {
        private static final Component NORMAL_USER_TEXT = Component.translatable("mco.configure.world.invites.normal.tooltip");
        private static final Component OP_TEXT = Component.translatable("mco.configure.world.invites.ops.tooltip");
        private static final Component REMOVE_TEXT = Component.translatable("mco.configure.world.invites.remove.tooltip");
        private static final ResourceLocation MAKE_OP_SPRITE = ResourceLocation.withDefaultNamespace("player_list/make_operator");
        private static final ResourceLocation REMOVE_OP_SPRITE = ResourceLocation.withDefaultNamespace("player_list/remove_operator");
        private static final ResourceLocation REMOVE_PLAYER_SPRITE = ResourceLocation.withDefaultNamespace("player_list/remove_player");
        private static final int ICON_WIDTH = 8;
        private static final int ICON_HEIGHT = 7;
        private final PlayerInfo playerInfo;
        private final Button removeButton;
        private final Button makeOpButton;
        private final Button removeOpButton;

        public Entry(PlayerInfo playerInfo) {
            this.playerInfo = playerInfo;
            int n = RealmsPlayerScreen.this.serverData.players.indexOf(this.playerInfo);
            this.makeOpButton = SpriteIconButton.builder(NORMAL_USER_TEXT, button -> this.op(n), false).sprite(MAKE_OP_SPRITE, 8, 7).width(16 + RealmsPlayerScreen.this.font.width(NORMAL_USER_TEXT)).narration(supplier -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", playerInfo.getName()), (Component)supplier.get(), Component.translatable("narration.cycle_button.usage.focused", OP_TEXT))).build();
            this.removeOpButton = SpriteIconButton.builder(OP_TEXT, button -> this.deop(n), false).sprite(REMOVE_OP_SPRITE, 8, 7).width(16 + RealmsPlayerScreen.this.font.width(OP_TEXT)).narration(supplier -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", playerInfo.getName()), (Component)supplier.get(), Component.translatable("narration.cycle_button.usage.focused", NORMAL_USER_TEXT))).build();
            this.removeButton = SpriteIconButton.builder(REMOVE_TEXT, button -> this.uninvite(n), false).sprite(REMOVE_PLAYER_SPRITE, 8, 7).width(16 + RealmsPlayerScreen.this.font.width(REMOVE_TEXT)).narration(supplier -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", playerInfo.getName()), (Component)supplier.get())).build();
            this.updateOpButtons();
        }

        private void op(int n) {
            RealmsClient realmsClient = RealmsClient.create();
            UUID uUID = RealmsPlayerScreen.this.serverData.players.get(n).getUuid();
            try {
                this.updateOps(realmsClient.op(RealmsPlayerScreen.this.serverData.id, uUID));
            }
            catch (RealmsServiceException realmsServiceException) {
                LOGGER.error("Couldn't op the user", (Throwable)realmsServiceException);
            }
            this.updateOpButtons();
        }

        private void deop(int n) {
            RealmsClient realmsClient = RealmsClient.create();
            UUID uUID = RealmsPlayerScreen.this.serverData.players.get(n).getUuid();
            try {
                this.updateOps(realmsClient.deop(RealmsPlayerScreen.this.serverData.id, uUID));
            }
            catch (RealmsServiceException realmsServiceException) {
                LOGGER.error("Couldn't deop the user", (Throwable)realmsServiceException);
            }
            this.updateOpButtons();
        }

        private void uninvite(int n) {
            if (n >= 0 && n < RealmsPlayerScreen.this.serverData.players.size()) {
                PlayerInfo playerInfo = RealmsPlayerScreen.this.serverData.players.get(n);
                RealmsConfirmScreen realmsConfirmScreen = new RealmsConfirmScreen(bl -> {
                    if (bl) {
                        RealmsClient realmsClient = RealmsClient.create();
                        try {
                            realmsClient.uninvite(RealmsPlayerScreen.this.serverData.id, playerInfo.getUuid());
                        }
                        catch (RealmsServiceException realmsServiceException) {
                            LOGGER.error("Couldn't uninvite user", (Throwable)realmsServiceException);
                        }
                        RealmsPlayerScreen.this.serverData.players.remove(n);
                        RealmsPlayerScreen.this.repopulateInvitedList();
                    }
                    RealmsPlayerScreen.this.stateChanged = true;
                    RealmsPlayerScreen.this.minecraft.setScreen(RealmsPlayerScreen.this);
                }, QUESTION_TITLE, Component.translatable("mco.configure.world.uninvite.player", playerInfo.getName()));
                RealmsPlayerScreen.this.minecraft.setScreen(realmsConfirmScreen);
            }
        }

        private void updateOps(Ops ops) {
            for (PlayerInfo playerInfo : RealmsPlayerScreen.this.serverData.players) {
                playerInfo.setOperator(ops.ops.contains(playerInfo.getName()));
            }
        }

        private void updateOpButtons() {
            this.makeOpButton.visible = !this.playerInfo.isOperator();
            this.removeOpButton.visible = !this.makeOpButton.visible;
        }

        private Button activeOpButton() {
            if (this.makeOpButton.visible) {
                return this.makeOpButton;
            }
            return this.removeOpButton;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of((Object)this.activeOpButton(), (Object)this.removeButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of((Object)this.activeOpButton(), (Object)this.removeButton);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            int n8 = !this.playerInfo.getAccepted() ? -6250336 : (this.playerInfo.getOnline() ? 0x7FFF7F : -1);
            int n9 = n2 + n5 / 2 - 16;
            RealmsUtil.renderPlayerFace(guiGraphics, n3, n9, 32, this.playerInfo.getUuid());
            int n10 = n2 + n5 / 2 - ((RealmsPlayerScreen)RealmsPlayerScreen.this).font.lineHeight / 2;
            guiGraphics.drawString(RealmsPlayerScreen.this.font, this.playerInfo.getName(), n3 + 8 + 32, n10, n8);
            int n11 = n2 + n5 / 2 - 10;
            int n12 = n3 + n4 - this.removeButton.getWidth();
            this.removeButton.setPosition(n12, n11);
            this.removeButton.render(guiGraphics, n6, n7, f);
            int n13 = n12 - this.activeOpButton().getWidth() - 8;
            this.makeOpButton.setPosition(n13, n11);
            this.makeOpButton.render(guiGraphics, n6, n7, f);
            this.removeOpButton.setPosition(n13, n11);
            this.removeOpButton.render(guiGraphics, n6, n7, f);
        }
    }
}

