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
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Backup;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsBackupInfoScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.DownloadTask;
import com.mojang.realmsclient.util.task.RestoreTask;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;
import org.slf4j.Logger;

public class RealmsBackupScreen
extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.configure.world.backup");
    static final Component RESTORE_TOOLTIP = Component.translatable("mco.backup.button.restore");
    static final Component HAS_CHANGES_TOOLTIP = Component.translatable("mco.backup.changes.tooltip");
    private static final Component NO_BACKUPS_LABEL = Component.translatable("mco.backup.nobackups");
    private static final Component DOWNLOAD_LATEST = Component.translatable("mco.backup.button.download");
    private static final String UPLOADED_KEY = "uploaded";
    private static final int PADDING = 8;
    final RealmsConfigureWorldScreen lastScreen;
    List<Backup> backups = Collections.emptyList();
    @Nullable
    BackupObjectSelectionList backupList;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final int slotId;
    @Nullable
    Button downloadButton;
    final RealmsServer serverData;
    boolean noBackups = false;

    public RealmsBackupScreen(RealmsConfigureWorldScreen realmsConfigureWorldScreen, RealmsServer realmsServer, int n) {
        super(TITLE);
        this.lastScreen = realmsConfigureWorldScreen;
        this.serverData = realmsServer;
        this.slotId = n;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        this.backupList = this.layout.addToContents(new BackupObjectSelectionList(this));
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.downloadButton = linearLayout.addChild(Button.builder(DOWNLOAD_LATEST, button -> this.downloadClicked()).build());
        this.downloadButton.active = false;
        linearLayout.addChild(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.layout.visitWidgets(guiEventListener -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
        });
        this.repositionElements();
        this.fetchRealmsBackups();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        if (this.noBackups && this.backupList != null) {
            guiGraphics.drawString(this.font, NO_BACKUPS_LABEL, this.width / 2 - this.font.width(NO_BACKUPS_LABEL) / 2, this.backupList.getY() + this.backupList.getHeight() / 2 - this.font.lineHeight / 2, -1);
        }
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.backupList != null) {
            this.backupList.updateSize(this.width, this.layout);
        }
    }

    private void fetchRealmsBackups() {
        new Thread("Realms-fetch-backups"){

            @Override
            public void run() {
                RealmsClient realmsClient = RealmsClient.create();
                try {
                    List<Backup> list = realmsClient.backupsFor((long)RealmsBackupScreen.this.serverData.id).backups;
                    RealmsBackupScreen.this.minecraft.execute(() -> {
                        RealmsBackupScreen.this.backups = list;
                        RealmsBackupScreen.this.noBackups = RealmsBackupScreen.this.backups.isEmpty();
                        if (!RealmsBackupScreen.this.noBackups && RealmsBackupScreen.this.downloadButton != null) {
                            RealmsBackupScreen.this.downloadButton.active = true;
                        }
                        if (RealmsBackupScreen.this.backupList != null) {
                            RealmsBackupScreen.this.backupList.replaceEntries(RealmsBackupScreen.this.backups.stream().map(backup -> new Entry((Backup)backup)).toList());
                        }
                    });
                }
                catch (RealmsServiceException realmsServiceException) {
                    LOGGER.error("Couldn't request backups", (Throwable)realmsServiceException);
                }
            }
        }.start();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void downloadClicked() {
        this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, Component.translatable("mco.configure.world.restore.download.question.line1"), popupScreen -> this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen.getNewScreen(), new DownloadTask(this.serverData.id, this.slotId, Objects.requireNonNullElse(this.serverData.name, "") + " (" + this.serverData.slots.get(this.serverData.activeSlot).getSlotName(this.serverData.activeSlot) + ")", this)))));
    }

    class BackupObjectSelectionList
    extends ContainerObjectSelectionList<Entry> {
        private static final int ITEM_HEIGHT = 36;

        public BackupObjectSelectionList(RealmsBackupScreen realmsBackupScreen) {
            super(Minecraft.getInstance(), realmsBackupScreen.width, realmsBackupScreen.layout.getContentHeight(), realmsBackupScreen.layout.getHeaderHeight(), 36);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    class Entry
    extends ContainerObjectSelectionList.Entry<Entry> {
        private static final int Y_PADDING = 2;
        private final Backup backup;
        @Nullable
        private Button restoreButton;
        @Nullable
        private Button changesButton;
        private final List<AbstractWidget> children = new ArrayList<AbstractWidget>();

        public Entry(Backup backup) {
            this.backup = backup;
            this.populateChangeList(backup);
            if (!backup.changeList.isEmpty()) {
                this.changesButton = Button.builder(HAS_CHANGES_TOOLTIP, button -> RealmsBackupScreen.this.minecraft.setScreen(new RealmsBackupInfoScreen(RealmsBackupScreen.this, this.backup))).width(8 + RealmsBackupScreen.this.font.width(HAS_CHANGES_TOOLTIP)).createNarration(supplier -> CommonComponents.joinForNarration(Component.translatable("mco.backup.narration", this.getShortBackupDate()), (Component)supplier.get())).build();
                this.children.add(this.changesButton);
            }
            if (!RealmsBackupScreen.this.serverData.expired) {
                this.restoreButton = Button.builder(RESTORE_TOOLTIP, button -> this.restoreClicked()).width(8 + RealmsBackupScreen.this.font.width(HAS_CHANGES_TOOLTIP)).createNarration(supplier -> CommonComponents.joinForNarration(Component.translatable("mco.backup.narration", this.getShortBackupDate()), (Component)supplier.get())).build();
                this.children.add(this.restoreButton);
            }
        }

        private void populateChangeList(Backup backup) {
            int n = RealmsBackupScreen.this.backups.indexOf(backup);
            if (n == RealmsBackupScreen.this.backups.size() - 1) {
                return;
            }
            Backup backup2 = RealmsBackupScreen.this.backups.get(n + 1);
            for (String string : backup.metadata.keySet()) {
                if (!string.contains(RealmsBackupScreen.UPLOADED_KEY) && backup2.metadata.containsKey(string)) {
                    if (backup.metadata.get(string).equals(backup2.metadata.get(string))) continue;
                    this.addToChangeList(string);
                    continue;
                }
                this.addToChangeList(string);
            }
        }

        private void addToChangeList(String string) {
            if (string.contains(RealmsBackupScreen.UPLOADED_KEY)) {
                String string2 = DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
                this.backup.changeList.put(string, string2);
                this.backup.setUploadedVersion(true);
            } else {
                this.backup.changeList.put(string, this.backup.metadata.get(string));
            }
        }

        private String getShortBackupDate() {
            return DateFormat.getDateTimeInstance(3, 3).format(this.backup.lastModifiedDate);
        }

        private void restoreClicked() {
            Component component = RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate);
            MutableComponent mutableComponent = Component.translatable("mco.configure.world.restore.question.line1", this.getShortBackupDate(), component);
            RealmsBackupScreen.this.minecraft.setScreen(RealmsPopups.warningPopupScreen(RealmsBackupScreen.this, mutableComponent, popupScreen -> RealmsBackupScreen.this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(RealmsBackupScreen.this.lastScreen.getNewScreen(), new RestoreTask(this.backup, RealmsBackupScreen.this.serverData.id, RealmsBackupScreen.this.lastScreen)))));
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            int n8 = n2 + n5 / 2;
            int n9 = n8 - ((RealmsBackupScreen)RealmsBackupScreen.this).font.lineHeight - 2;
            int n10 = n8 + 2;
            int n11 = this.backup.isUploadedVersion() ? -8388737 : -1;
            guiGraphics.drawString(RealmsBackupScreen.this.font, Component.translatable("mco.backup.entry", RealmsUtil.convertToAgePresentationFromInstant(this.backup.lastModifiedDate)), n3, n9, n11);
            guiGraphics.drawString(RealmsBackupScreen.this.font, this.getMediumDatePresentation(this.backup.lastModifiedDate), n3, n10, 0x4C4C4C);
            int n12 = 0;
            int n13 = n2 + n5 / 2 - 10;
            if (this.restoreButton != null) {
                this.restoreButton.setX(n3 + n4 - (n12 += this.restoreButton.getWidth() + 8));
                this.restoreButton.setY(n13);
                this.restoreButton.render(guiGraphics, n6, n7, f);
            }
            if (this.changesButton != null) {
                this.changesButton.setX(n3 + n4 - (n12 += this.changesButton.getWidth() + 8));
                this.changesButton.setY(n13);
                this.changesButton.render(guiGraphics, n6, n7, f);
            }
        }

        private String getMediumDatePresentation(Date date) {
            return DateFormat.getDateTimeInstance(3, 3).format(date);
        }
    }
}

