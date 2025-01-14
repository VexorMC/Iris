/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;

public class RealmsGenericErrorScreen
extends RealmsScreen {
    private final Screen nextScreen;
    private final ErrorMessage lines;
    private MultiLineLabel line2Split = MultiLineLabel.EMPTY;

    public RealmsGenericErrorScreen(RealmsServiceException realmsServiceException, Screen screen) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = screen;
        this.lines = RealmsGenericErrorScreen.errorMessage(realmsServiceException);
    }

    public RealmsGenericErrorScreen(Component component, Screen screen) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = screen;
        this.lines = RealmsGenericErrorScreen.errorMessage(component);
    }

    public RealmsGenericErrorScreen(Component component, Component component2, Screen screen) {
        super(GameNarrator.NO_TITLE);
        this.nextScreen = screen;
        this.lines = RealmsGenericErrorScreen.errorMessage(component, component2);
    }

    private static ErrorMessage errorMessage(RealmsServiceException realmsServiceException) {
        RealmsError realmsError = realmsServiceException.realmsError;
        return RealmsGenericErrorScreen.errorMessage(Component.translatable("mco.errorMessage.realmsService.realmsError", realmsError.errorCode()), realmsError.errorMessage());
    }

    private static ErrorMessage errorMessage(Component component) {
        return RealmsGenericErrorScreen.errorMessage(Component.translatable("mco.errorMessage.generic"), component);
    }

    private static ErrorMessage errorMessage(Component component, Component component2) {
        return new ErrorMessage(component, component2);
    }

    @Override
    public void init() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, button -> this.onClose()).bounds(this.width / 2 - 100, this.height - 52, 200, 20).build());
        this.line2Split = MultiLineLabel.create(this.font, this.lines.detail, this.width * 3 / 4);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.nextScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return Component.empty().append(this.lines.title).append(": ").append(this.lines.detail);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        guiGraphics.drawCenteredString(this.font, this.lines.title, this.width / 2, 80, -1);
        this.line2Split.renderCentered(guiGraphics, this.width / 2, 100, this.minecraft.font.lineHeight, -2142128);
    }

    static final class ErrorMessage
    extends Record {
        final Component title;
        final Component detail;

        ErrorMessage(Component component, Component component2) {
            this.title = component;
            this.detail = component2;
        }

        @Override
        public final String toString() {
            return ObjectMethods.bootstrap("toString", new MethodHandle[]{ErrorMessage.class, "title;detail", "title", "detail"}, this);
        }

        @Override
        public final int hashCode() {
            return (int)ObjectMethods.bootstrap("hashCode", new MethodHandle[]{ErrorMessage.class, "title;detail", "title", "detail"}, this);
        }

        @Override
        public final boolean equals(Object object) {
            return (boolean)ObjectMethods.bootstrap("equals", new MethodHandle[]{ErrorMessage.class, "title;detail", "title", "detail"}, this, object);
        }

        public Component title() {
            return this.title;
        }

        public Component detail() {
            return this.detail;
        }
    }
}

