/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.common.util.concurrent.RateLimiter
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.UploadStatus;
import com.mojang.realmsclient.client.worldupload.RealmsUploadException;
import com.mojang.realmsclient.client.worldupload.RealmsWorldUpload;
import com.mojang.realmsclient.client.worldupload.RealmsWorldUploadStatusTracker;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsResetWorldScreen;
import com.mojang.realmsclient.util.task.LongRunningTask;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;

public class RealmsUploadScreen
extends RealmsScreen
implements RealmsWorldUploadStatusTracker {
    private static final int BAR_WIDTH = 200;
    private static final int BAR_TOP = 80;
    private static final int BAR_BOTTOM = 95;
    private static final int BAR_BORDER = 1;
    private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
    private static final Component VERIFYING_TEXT = Component.translatable("mco.upload.verifying");
    private final RealmsResetWorldScreen lastScreen;
    private final LevelSummary selectedLevel;
    @Nullable
    private final RealmCreationTask realmCreationTask;
    private final long realmId;
    private final int slotId;
    final AtomicReference<RealmsWorldUpload> currentUpload = new AtomicReference();
    private final UploadStatus uploadStatus;
    private final RateLimiter narrationRateLimiter;
    @Nullable
    private volatile Component[] errorMessage;
    private volatile Component status = Component.translatable("mco.upload.preparing");
    @Nullable
    private volatile String progress;
    private volatile boolean cancelled;
    private volatile boolean uploadFinished;
    private volatile boolean showDots = true;
    private volatile boolean uploadStarted;
    @Nullable
    private Button backButton;
    @Nullable
    private Button cancelButton;
    private int tickCount;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public RealmsUploadScreen(@Nullable RealmCreationTask realmCreationTask, long l, int n, RealmsResetWorldScreen realmsResetWorldScreen, LevelSummary levelSummary) {
        super(GameNarrator.NO_TITLE);
        this.realmCreationTask = realmCreationTask;
        this.realmId = l;
        this.slotId = n;
        this.lastScreen = realmsResetWorldScreen;
        this.selectedLevel = levelSummary;
        this.uploadStatus = new UploadStatus();
        this.narrationRateLimiter = RateLimiter.create((double)0.1f);
    }

    @Override
    public void init() {
        this.backButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onBack()).build());
        this.backButton.visible = false;
        this.cancelButton = this.layout.addToFooter(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel()).build());
        if (!this.uploadStarted) {
            if (this.lastScreen.slot == -1) {
                this.uploadStarted = true;
                this.upload();
            } else {
                ArrayList<LongRunningTask> arrayList = new ArrayList<LongRunningTask>();
                if (this.realmCreationTask != null) {
                    arrayList.add(this.realmCreationTask);
                }
                arrayList.add(new SwitchSlotTask(this.realmId, this.lastScreen.slot, () -> {
                    if (!this.uploadStarted) {
                        this.uploadStarted = true;
                        this.minecraft.execute(() -> {
                            this.minecraft.setScreen(this);
                            this.upload();
                        });
                    }
                }));
                this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, arrayList.toArray(new LongRunningTask[0])));
            }
        }
        this.layout.visitWidgets(guiEventListener -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    private void onBack() {
        this.minecraft.setScreen(new RealmsConfigureWorldScreen(new RealmsMainScreen(new TitleScreen()), this.realmId));
    }

    private void onCancel() {
        this.cancelled = true;
        RealmsWorldUpload realmsWorldUpload = this.currentUpload.get();
        if (realmsWorldUpload != null) {
            realmsWorldUpload.cancel();
        } else {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    @Override
    public boolean keyPressed(int n, int n2, int n3) {
        if (n == 256) {
            if (this.showDots) {
                this.onCancel();
            } else {
                this.onBack();
            }
            return true;
        }
        return super.keyPressed(n, n2, n3);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        Component[] componentArray;
        super.render(guiGraphics, n, n2, f);
        if (!this.uploadFinished && this.uploadStatus.uploadStarted() && this.uploadStatus.uploadCompleted() && this.cancelButton != null) {
            this.status = VERIFYING_TEXT;
            this.cancelButton.active = false;
        }
        guiGraphics.drawCenteredString(this.font, this.status, this.width / 2, 50, -1);
        if (this.showDots) {
            guiGraphics.drawString(this.font, DOTS[this.tickCount / 10 % DOTS.length], this.width / 2 + this.font.width(this.status) / 2 + 5, 50, -1);
        }
        if (this.uploadStatus.uploadStarted() && !this.cancelled) {
            this.drawProgressBar(guiGraphics);
            this.drawUploadSpeed(guiGraphics);
        }
        if ((componentArray = this.errorMessage) != null) {
            for (int i = 0; i < componentArray.length; ++i) {
                guiGraphics.drawCenteredString(this.font, componentArray[i], this.width / 2, 110 + 12 * i, -65536);
            }
        }
    }

    private void drawProgressBar(GuiGraphics guiGraphics) {
        double d = this.uploadStatus.getPercentage();
        this.progress = String.format(Locale.ROOT, "%.1f", d * 100.0);
        int n = (this.width - 200) / 2;
        int n2 = n + (int)Math.round(200.0 * d);
        guiGraphics.fill(n - 1, 79, n2 + 1, 96, -1);
        guiGraphics.fill(n, 80, n2, 95, -8355712);
        guiGraphics.drawCenteredString(this.font, Component.translatable("mco.upload.percent", this.progress), this.width / 2, 84, -1);
    }

    private void drawUploadSpeed(GuiGraphics guiGraphics) {
        this.drawUploadSpeed0(guiGraphics, this.uploadStatus.getBytesPerSecond());
    }

    private void drawUploadSpeed0(GuiGraphics guiGraphics, long l) {
        String string = this.progress;
        if (l > 0L && string != null) {
            int n = this.font.width(string);
            String string2 = "(" + Unit.humanReadable(l) + "/s)";
            guiGraphics.drawString(this.font, string2, this.width / 2 + n / 2 + 15, 84, -1);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ++this.tickCount;
        this.uploadStatus.refreshBytesPerSecond();
        if (this.narrationRateLimiter.tryAcquire(1)) {
            Component component = this.createProgressNarrationMessage();
            this.minecraft.getNarrator().sayNow(component);
        }
    }

    private Component createProgressNarrationMessage() {
        Component[] componentArray;
        ArrayList arrayList = Lists.newArrayList();
        arrayList.add(this.status);
        if (this.progress != null) {
            arrayList.add(Component.translatable("mco.upload.percent", this.progress));
        }
        if ((componentArray = this.errorMessage) != null) {
            arrayList.addAll(Arrays.asList(componentArray));
        }
        return CommonComponents.joinLines(arrayList);
    }

    private void upload() {
        RealmsWorldOptions realmsWorldOptions;
        Path path = this.minecraft.gameDirectory.toPath().resolve("saves").resolve(this.selectedLevel.getLevelId());
        RealmsWorldUpload realmsWorldUpload = new RealmsWorldUpload(path, realmsWorldOptions = RealmsWorldOptions.createFromSettings(this.selectedLevel.getSettings(), this.selectedLevel.levelVersion().minecraftVersionName()), this.minecraft.getUser(), this.realmId, this.slotId, this);
        if (!this.currentUpload.compareAndSet(null, realmsWorldUpload)) {
            throw new IllegalStateException("Tried to start uploading but was already uploading");
        }
        realmsWorldUpload.packAndUpload().handleAsync((object, throwable) -> {
            if (throwable != null) {
                RuntimeException runtimeException;
                if (throwable instanceof CompletionException) {
                    runtimeException = (CompletionException)throwable;
                    throwable = runtimeException.getCause();
                }
                if (throwable instanceof RealmsUploadException) {
                    runtimeException = (RealmsUploadException)throwable;
                    if (((RealmsUploadException)runtimeException).getStatusMessage() != null) {
                        this.status = ((RealmsUploadException)runtimeException).getStatusMessage();
                    }
                    this.setErrorMessage(((RealmsUploadException)runtimeException).getErrorMessages());
                } else {
                    this.status = Component.translatable("mco.upload.failed", throwable.getMessage());
                }
            } else {
                this.status = Component.translatable("mco.upload.done");
                if (this.backButton != null) {
                    this.backButton.setMessage(CommonComponents.GUI_DONE);
                }
            }
            this.uploadFinished = true;
            this.showDots = false;
            if (this.backButton != null) {
                this.backButton.visible = true;
            }
            if (this.cancelButton != null) {
                this.cancelButton.visible = false;
            }
            this.currentUpload.set(null);
            return null;
        }, (Executor)this.minecraft);
    }

    private void setErrorMessage(Component ... componentArray) {
        this.errorMessage = componentArray;
    }

    @Override
    public UploadStatus getUploadStatus() {
        return this.uploadStatus;
    }

    @Override
    public void setUploading() {
        this.status = Component.translatable("mco.upload.uploading", this.selectedLevel.getLevelName());
    }
}

