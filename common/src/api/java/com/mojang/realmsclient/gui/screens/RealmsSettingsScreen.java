/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.StringUtil;

public class RealmsSettingsScreen
extends RealmsScreen {
    private static final int COMPONENT_WIDTH = 212;
    private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
    private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
    private final RealmsConfigureWorldScreen configureWorldScreen;
    private final RealmsServer serverData;
    private EditBox descEdit;
    private EditBox nameEdit;

    public RealmsSettingsScreen(RealmsConfigureWorldScreen realmsConfigureWorldScreen, RealmsServer realmsServer) {
        super(Component.translatable("mco.configure.world.settings.title"));
        this.configureWorldScreen = realmsConfigureWorldScreen;
        this.serverData = realmsServer;
    }

    @Override
    public void init() {
        int n = this.width / 2 - 106;
        String string2 = this.serverData.state == RealmsServer.State.OPEN ? "mco.configure.world.buttons.close" : "mco.configure.world.buttons.open";
        Button button2 = Button.builder(Component.translatable(string2), button -> {
            if (this.serverData.state == RealmsServer.State.OPEN) {
                this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, Component.translatable("mco.configure.world.close.question.line1"), popupScreen -> this.configureWorldScreen.closeTheWorld()));
            } else {
                this.configureWorldScreen.openTheWorld(false);
            }
        }).bounds(this.width / 2 - 53, RealmsSettingsScreen.row(0), 106, 20).build();
        this.addRenderableWidget(button2);
        this.nameEdit = new EditBox(this.minecraft.font, n, RealmsSettingsScreen.row(4), 212, 20, Component.translatable("mco.configure.world.name"));
        this.nameEdit.setMaxLength(32);
        this.nameEdit.setValue(Objects.requireNonNullElse(this.serverData.getName(), ""));
        this.addRenderableWidget(this.nameEdit);
        this.descEdit = new EditBox(this.minecraft.font, n, RealmsSettingsScreen.row(8), 212, 20, Component.translatable("mco.configure.world.description"));
        this.descEdit.setMaxLength(32);
        this.descEdit.setValue(this.serverData.getDescription());
        this.addRenderableWidget(this.descEdit);
        Button button3 = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.done"), button -> this.save()).bounds(n - 2, RealmsSettingsScreen.row(12), 106, 20).build());
        this.nameEdit.setResponder(string -> {
            button.active = !StringUtil.isBlank(string);
        });
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(this.width / 2 + 2, RealmsSettingsScreen.row(12), 106, 20).build());
    }

    @Override
    protected void setInitialFocus() {
        this.setInitialFocus(this.nameEdit);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.configureWorldScreen);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
        guiGraphics.drawString(this.font, NAME_LABEL, this.width / 2 - 106, RealmsSettingsScreen.row(3), -1);
        guiGraphics.drawString(this.font, DESCRIPTION_LABEL, this.width / 2 - 106, RealmsSettingsScreen.row(7), -1);
    }

    public void save() {
        this.configureWorldScreen.saveSettings(this.nameEdit.getValue(), this.descEdit.getValue());
    }
}

