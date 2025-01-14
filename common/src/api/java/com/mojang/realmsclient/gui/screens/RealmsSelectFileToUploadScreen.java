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
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsResetWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsUploadScreen;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.slf4j.Logger;

public class RealmsSelectFileToUploadScreen
extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Component TITLE = Component.translatable("mco.upload.select.world.title");
    private static final Component UNABLE_TO_LOAD_WORLD = Component.translatable("selectWorld.unable_to_load");
    static final Component WORLD_TEXT = Component.translatable("selectWorld.world");
    private static final Component HARDCORE_TEXT = Component.translatable("mco.upload.hardcore").withColor(-65536);
    private static final Component COMMANDS_TEXT = Component.translatable("selectWorld.commands");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    @Nullable
    private final RealmCreationTask realmCreationTask;
    private final RealmsResetWorldScreen lastScreen;
    private final long realmId;
    private final int slotId;
    Button uploadButton;
    List<LevelSummary> levelList = Lists.newArrayList();
    int selectedWorld = -1;
    WorldSelectionList worldSelectionList;

    public RealmsSelectFileToUploadScreen(@Nullable RealmCreationTask realmCreationTask, long l, int n, RealmsResetWorldScreen realmsResetWorldScreen) {
        super(TITLE);
        this.realmCreationTask = realmCreationTask;
        this.lastScreen = realmsResetWorldScreen;
        this.realmId = l;
        this.slotId = n;
    }

    private void loadLevelList() {
        LevelStorageSource.LevelCandidates levelCandidates = this.minecraft.getLevelSource().findLevelCandidates();
        this.levelList = this.minecraft.getLevelSource().loadLevelSummaries(levelCandidates).join().stream().filter(LevelSummary::canUpload).collect(Collectors.toList());
        for (LevelSummary levelSummary : this.levelList) {
            this.worldSelectionList.addEntry(levelSummary);
        }
    }

    @Override
    public void init() {
        this.worldSelectionList = this.addRenderableWidget(new WorldSelectionList());
        try {
            this.loadLevelList();
        }
        catch (Exception exception) {
            LOGGER.error("Couldn't load level list", (Throwable)exception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(UNABLE_TO_LOAD_WORLD, Component.nullToEmpty(exception.getMessage()), this.lastScreen));
            return;
        }
        this.uploadButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.upload.button.name"), button -> this.upload()).bounds(this.width / 2 - 154, this.height - 32, 153, 20).build());
        this.uploadButton.active = this.selectedWorld >= 0 && this.selectedWorld < this.levelList.size();
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.lastScreen)).bounds(this.width / 2 + 6, this.height - 32, 153, 20).build());
        this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.subtitle"), this.width / 2, RealmsSelectFileToUploadScreen.row(-1), -6250336));
        if (this.levelList.isEmpty()) {
            this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.none"), this.width / 2, this.height / 2 - 20, -1));
        }
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
    }

    private void upload() {
        if (this.selectedWorld != -1 && !this.levelList.get(this.selectedWorld).isHardcore()) {
            LevelSummary levelSummary = this.levelList.get(this.selectedWorld);
            this.minecraft.setScreen(new RealmsUploadScreen(this.realmCreationTask, this.realmId, this.slotId, this.lastScreen, levelSummary));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 13, -1);
    }

    @Override
    public boolean keyPressed(int n, int n2, int n3) {
        if (n == 256) {
            this.minecraft.setScreen(this.lastScreen);
            return true;
        }
        return super.keyPressed(n, n2, n3);
    }

    static Component gameModeName(LevelSummary levelSummary) {
        return levelSummary.getGameMode().getLongDisplayName();
    }

    static String formatLastPlayed(LevelSummary levelSummary) {
        return DATE_FORMAT.format(new Date(levelSummary.getLastPlayed()));
    }

    class WorldSelectionList
    extends ObjectSelectionList<Entry> {
        public WorldSelectionList() {
            super(Minecraft.getInstance(), RealmsSelectFileToUploadScreen.this.width, RealmsSelectFileToUploadScreen.this.height - 40 - RealmsSelectFileToUploadScreen.row(0), RealmsSelectFileToUploadScreen.row(0), 36);
        }

        public void addEntry(LevelSummary levelSummary) {
            this.addEntry(new Entry(levelSummary));
        }

        @Override
        public void setSelected(@Nullable Entry entry) {
            super.setSelected(entry);
            RealmsSelectFileToUploadScreen.this.selectedWorld = this.children().indexOf(entry);
            RealmsSelectFileToUploadScreen.this.uploadButton.active = RealmsSelectFileToUploadScreen.this.selectedWorld >= 0 && RealmsSelectFileToUploadScreen.this.selectedWorld < this.getItemCount() && !RealmsSelectFileToUploadScreen.this.levelList.get(RealmsSelectFileToUploadScreen.this.selectedWorld).isHardcore();
        }

        @Override
        public int getRowWidth() {
            return (int)((double)this.width * 0.6);
        }
    }

    class Entry
    extends ObjectSelectionList.Entry<Entry> {
        private final LevelSummary levelSummary;
        private final String name;
        private final Component id;
        private final Component info;

        public Entry(LevelSummary levelSummary) {
            this.levelSummary = levelSummary;
            this.name = levelSummary.getLevelName();
            this.id = Component.translatable("mco.upload.entry.id", levelSummary.getLevelId(), RealmsSelectFileToUploadScreen.formatLastPlayed(levelSummary));
            this.info = levelSummary.getInfo();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            this.renderItem(guiGraphics, n, n3, n2);
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            RealmsSelectFileToUploadScreen.this.worldSelectionList.setSelectedIndex(RealmsSelectFileToUploadScreen.this.levelList.indexOf(this.levelSummary));
            return super.mouseClicked(d, d2, n);
        }

        protected void renderItem(GuiGraphics guiGraphics, int n, int n2, int n3) {
            Object object = this.name.isEmpty() ? String.valueOf(WORLD_TEXT) + " " + (n + 1) : this.name;
            guiGraphics.drawString(RealmsSelectFileToUploadScreen.this.font, (String)object, n2 + 2, n3 + 1, -1);
            guiGraphics.drawString(RealmsSelectFileToUploadScreen.this.font, this.id, n2 + 2, n3 + 12, -8355712);
            guiGraphics.drawString(RealmsSelectFileToUploadScreen.this.font, this.info, n2 + 2, n3 + 12 + 10, -8355712);
        }

        @Override
        public Component getNarration() {
            Component component = CommonComponents.joinLines(Component.literal(this.levelSummary.getLevelName()), Component.literal(RealmsSelectFileToUploadScreen.formatLastPlayed(this.levelSummary)), RealmsSelectFileToUploadScreen.gameModeName(this.levelSummary));
            return Component.translatable("narrator.select", component);
        }
    }
}

