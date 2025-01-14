/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.gui.screens.RealmsSlotOptionsScreen;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;

public class RealmsBackupInfoScreen
extends RealmsScreen {
    private static final Component TITLE = Component.translatable("mco.backup.info.title");
    private static final Component UNKNOWN = Component.translatable("mco.backup.unknown");
    private final Screen lastScreen;
    final Backup backup;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private BackupInfoList backupInfoList;

    public RealmsBackupInfoScreen(Screen screen, Backup backup) {
        super(TITLE);
        this.lastScreen = screen;
        this.backup = backup;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.backupInfoList = this.layout.addToContents(new BackupInfoList(this.minecraft));
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.repositionElements();
        this.layout.visitWidgets(guiEventListener -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
        });
    }

    @Override
    protected void repositionElements() {
        this.backupInfoList.setSize(this.width, this.layout.getContentHeight());
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    Component checkForSpecificMetadata(String string, String string2) {
        String string3 = string.toLowerCase(Locale.ROOT);
        if (string3.contains("game") && string3.contains("mode")) {
            return this.gameModeMetadata(string2);
        }
        if (string3.contains("game") && string3.contains("difficulty")) {
            return this.gameDifficultyMetadata(string2);
        }
        return Component.literal(string2);
    }

    private Component gameDifficultyMetadata(String string) {
        try {
            return RealmsSlotOptionsScreen.DIFFICULTIES.get(Integer.parseInt(string)).getDisplayName();
        }
        catch (Exception exception) {
            return UNKNOWN;
        }
    }

    private Component gameModeMetadata(String string) {
        try {
            return RealmsSlotOptionsScreen.GAME_MODES.get(Integer.parseInt(string)).getShortDisplayName();
        }
        catch (Exception exception) {
            return UNKNOWN;
        }
    }

    class BackupInfoList
    extends ObjectSelectionList<BackupInfoListEntry> {
        public BackupInfoList(Minecraft minecraft) {
            super(minecraft, RealmsBackupInfoScreen.this.width, RealmsBackupInfoScreen.this.layout.getContentHeight(), RealmsBackupInfoScreen.this.layout.getHeaderHeight(), 36);
            if (RealmsBackupInfoScreen.this.backup.changeList != null) {
                RealmsBackupInfoScreen.this.backup.changeList.forEach((string, string2) -> this.addEntry(new BackupInfoListEntry((String)string, (String)string2)));
            }
        }
    }

    class BackupInfoListEntry
    extends ObjectSelectionList.Entry<BackupInfoListEntry> {
        private static final Component TEMPLATE_NAME = Component.translatable("mco.backup.entry.templateName");
        private static final Component GAME_DIFFICULTY = Component.translatable("mco.backup.entry.gameDifficulty");
        private static final Component NAME = Component.translatable("mco.backup.entry.name");
        private static final Component GAME_SERVER_VERSION = Component.translatable("mco.backup.entry.gameServerVersion");
        private static final Component UPLOADED = Component.translatable("mco.backup.entry.uploaded");
        private static final Component ENABLED_PACK = Component.translatable("mco.backup.entry.enabledPack");
        private static final Component DESCRIPTION = Component.translatable("mco.backup.entry.description");
        private static final Component GAME_MODE = Component.translatable("mco.backup.entry.gameMode");
        private static final Component SEED = Component.translatable("mco.backup.entry.seed");
        private static final Component WORLD_TYPE = Component.translatable("mco.backup.entry.worldType");
        private static final Component UNDEFINED = Component.translatable("mco.backup.entry.undefined");
        private final String key;
        private final String value;

        public BackupInfoListEntry(String string, String string2) {
            this.key = string;
            this.value = string2;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            guiGraphics.drawString(RealmsBackupInfoScreen.this.font, this.translateKey(this.key), n3, n2, -6250336);
            guiGraphics.drawString(RealmsBackupInfoScreen.this.font, RealmsBackupInfoScreen.this.checkForSpecificMetadata(this.key, this.value), n3, n2 + 12, -1);
        }

        private Component translateKey(String string) {
            return switch (string) {
                case "template_name" -> TEMPLATE_NAME;
                case "game_difficulty" -> GAME_DIFFICULTY;
                case "name" -> NAME;
                case "game_server_version" -> GAME_SERVER_VERSION;
                case "uploaded" -> UPLOADED;
                case "enabled_packs" -> ENABLED_PACK;
                case "description" -> DESCRIPTION;
                case "game_mode" -> GAME_MODE;
                case "seed" -> SEED;
                case "world_type" -> WORLD_TYPE;
                default -> UNDEFINED;
            };
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.key + " " + this.value);
        }
    }
}

