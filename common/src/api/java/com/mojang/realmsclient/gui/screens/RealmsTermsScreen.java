/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.CommonLinks;
import org.slf4j.Logger;

public class RealmsTermsScreen
extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.terms.title");
    private static final Component TERMS_STATIC_TEXT = Component.translatable("mco.terms.sentence.1");
    private static final Component TERMS_LINK_TEXT = CommonComponents.space().append(Component.translatable("mco.terms.sentence.2").withStyle(Style.EMPTY.withUnderlined(true)));
    private final Screen lastScreen;
    private final RealmsServer realmsServer;
    private boolean onLink;

    public RealmsTermsScreen(Screen screen, RealmsServer realmsServer) {
        super(TITLE);
        this.lastScreen = screen;
        this.realmsServer = realmsServer;
    }

    @Override
    public void init() {
        int n = this.width / 4 - 2;
        this.addRenderableWidget(Button.builder(Component.translatable("mco.terms.buttons.agree"), button -> this.agreedToTos()).bounds(this.width / 4, RealmsTermsScreen.row(12), n, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("mco.terms.buttons.disagree"), button -> this.minecraft.setScreen(this.lastScreen)).bounds(this.width / 2 + 4, RealmsTermsScreen.row(12), n, 20).build());
    }

    @Override
    public boolean keyPressed(int n, int n2, int n3) {
        if (n == 256) {
            this.minecraft.setScreen(this.lastScreen);
            return true;
        }
        return super.keyPressed(n, n2, n3);
    }

    private void agreedToTos() {
        RealmsClient realmsClient = RealmsClient.create();
        try {
            realmsClient.agreeToTos();
            this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new GetServerDetailsTask(this.lastScreen, this.realmsServer)));
        }
        catch (RealmsServiceException realmsServiceException) {
            LOGGER.error("Couldn't agree to TOS", (Throwable)realmsServiceException);
        }
    }

    @Override
    public boolean mouseClicked(double d, double d2, int n) {
        if (this.onLink) {
            this.minecraft.keyboardHandler.setClipboard(CommonLinks.REALMS_TERMS.toString());
            Util.getPlatform().openUri(CommonLinks.REALMS_TERMS);
            return true;
        }
        return super.mouseClicked(d, d2, n);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), TERMS_STATIC_TEXT).append(CommonComponents.SPACE).append(TERMS_LINK_TEXT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
        guiGraphics.drawString(this.font, TERMS_STATIC_TEXT, this.width / 2 - 120, RealmsTermsScreen.row(5), -1);
        int n3 = this.font.width(TERMS_STATIC_TEXT);
        int n4 = this.width / 2 - 121 + n3;
        int n5 = RealmsTermsScreen.row(5);
        int n6 = n4 + this.font.width(TERMS_LINK_TEXT) + 1;
        int n7 = n5 + 1 + this.font.lineHeight;
        this.onLink = n4 <= n && n <= n6 && n5 <= n2 && n2 <= n7;
        guiGraphics.drawString(this.font, TERMS_LINK_TEXT, this.width / 2 - 120 + n3, RealmsTermsScreen.row(5), this.onLink ? 7107012 : 0x3366BB);
    }
}

