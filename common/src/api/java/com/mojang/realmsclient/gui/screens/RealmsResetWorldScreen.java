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
import com.mojang.realmsclient.client.worldupload.RealmsCreateWorldFlow;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsSelectFileToUploadScreen;
import com.mojang.realmsclient.gui.screens.RealmsSelectWorldTemplateScreen;
import com.mojang.realmsclient.util.task.LongRunningTask;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import com.mojang.realmsclient.util.task.ResettingTemplateWorldTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.slf4j.Logger;

public class RealmsResetWorldScreen
extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Component CREATE_REALM_TITLE = Component.translatable("mco.selectServer.create");
    private static final Component CREATE_REALM_SUBTITLE = Component.translatable("mco.selectServer.create.subtitle");
    private static final Component CREATE_WORLD_TITLE = Component.translatable("mco.configure.world.switch.slot");
    private static final Component CREATE_WORLD_SUBTITLE = Component.translatable("mco.configure.world.switch.slot.subtitle");
    private static final Component GENERATE_NEW_WORLD = Component.translatable("mco.reset.world.generate");
    private static final Component RESET_WORLD_TITLE = Component.translatable("mco.reset.world.title");
    private static final Component RESET_WORLD_SUBTITLE = Component.translatable("mco.reset.world.warning");
    public static final Component CREATE_WORLD_RESET_TASK_TITLE = Component.translatable("mco.create.world.reset.title");
    private static final Component RESET_WORLD_RESET_TASK_TITLE = Component.translatable("mco.reset.world.resetting.screen.title");
    private static final Component WORLD_TEMPLATES_TITLE = Component.translatable("mco.reset.world.template");
    private static final Component ADVENTURES_TITLE = Component.translatable("mco.reset.world.adventure");
    private static final Component EXPERIENCES_TITLE = Component.translatable("mco.reset.world.experience");
    private static final Component INSPIRATION_TITLE = Component.translatable("mco.reset.world.inspiration");
    private final Screen lastScreen;
    private final RealmsServer serverData;
    private final Component subtitle;
    private final int subtitleColor;
    private final Component resetTaskTitle;
    private static final ResourceLocation UPLOAD_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/upload.png");
    private static final ResourceLocation ADVENTURE_MAP_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/adventure.png");
    private static final ResourceLocation SURVIVAL_SPAWN_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/survival_spawn.png");
    private static final ResourceLocation NEW_WORLD_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/new_world.png");
    private static final ResourceLocation EXPERIENCE_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/experience.png");
    private static final ResourceLocation INSPIRATION_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/inspiration.png");
    WorldTemplatePaginatedList templates;
    WorldTemplatePaginatedList adventuremaps;
    WorldTemplatePaginatedList experiences;
    WorldTemplatePaginatedList inspirations;
    public final int slot;
    @Nullable
    private final RealmCreationTask realmCreationTask;
    private final Runnable resetWorldRunnable;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    private RealmsResetWorldScreen(Screen screen, RealmsServer realmsServer, int n, Component component, Component component2, int n2, Component component3, Runnable runnable) {
        this(screen, realmsServer, n, component, component2, n2, component3, null, runnable);
    }

    public RealmsResetWorldScreen(Screen screen, RealmsServer realmsServer, int n, Component component, Component component2, int n2, Component component3, @Nullable RealmCreationTask realmCreationTask, Runnable runnable) {
        super(component);
        this.lastScreen = screen;
        this.serverData = realmsServer;
        this.slot = n;
        this.subtitle = component2;
        this.subtitleColor = n2;
        this.resetTaskTitle = component3;
        this.realmCreationTask = realmCreationTask;
        this.resetWorldRunnable = runnable;
    }

    public static RealmsResetWorldScreen forNewRealm(Screen screen, RealmsServer realmsServer, RealmCreationTask realmCreationTask, Runnable runnable) {
        return new RealmsResetWorldScreen(screen, realmsServer, realmsServer.activeSlot, CREATE_REALM_TITLE, CREATE_REALM_SUBTITLE, -6250336, CREATE_WORLD_RESET_TASK_TITLE, realmCreationTask, runnable);
    }

    public static RealmsResetWorldScreen forEmptySlot(Screen screen, int n, RealmsServer realmsServer, Runnable runnable) {
        return new RealmsResetWorldScreen(screen, realmsServer, n, CREATE_WORLD_TITLE, CREATE_WORLD_SUBTITLE, -6250336, CREATE_WORLD_RESET_TASK_TITLE, runnable);
    }

    public static RealmsResetWorldScreen forResetSlot(Screen screen, RealmsServer realmsServer, Runnable runnable) {
        return new RealmsResetWorldScreen(screen, realmsServer, realmsServer.activeSlot, RESET_WORLD_TITLE, RESET_WORLD_SUBTITLE, -65536, RESET_WORLD_RESET_TASK_TITLE, runnable);
    }

    @Override
    public void init() {
        LinearLayout linearLayout = this.layout.addToHeader(LinearLayout.vertical());
        linearLayout.defaultCellSetting().padding(this.font.lineHeight / 3);
        linearLayout.addChild(new StringWidget(this.title, this.font), LayoutSettings::alignHorizontallyCenter);
        linearLayout.addChild(new StringWidget(this.subtitle, this.font).setColor(this.subtitleColor), LayoutSettings::alignHorizontallyCenter);
        new Thread("Realms-reset-world-fetcher"){

            @Override
            public void run() {
                RealmsClient realmsClient = RealmsClient.create();
                try {
                    WorldTemplatePaginatedList worldTemplatePaginatedList = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.NORMAL);
                    WorldTemplatePaginatedList worldTemplatePaginatedList2 = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.ADVENTUREMAP);
                    WorldTemplatePaginatedList worldTemplatePaginatedList3 = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.EXPERIENCE);
                    WorldTemplatePaginatedList worldTemplatePaginatedList4 = realmsClient.fetchWorldTemplates(1, 10, RealmsServer.WorldType.INSPIRATION);
                    RealmsResetWorldScreen.this.minecraft.execute(() -> {
                        RealmsResetWorldScreen.this.templates = worldTemplatePaginatedList;
                        RealmsResetWorldScreen.this.adventuremaps = worldTemplatePaginatedList2;
                        RealmsResetWorldScreen.this.experiences = worldTemplatePaginatedList3;
                        RealmsResetWorldScreen.this.inspirations = worldTemplatePaginatedList4;
                    });
                }
                catch (RealmsServiceException realmsServiceException) {
                    LOGGER.error("Couldn't fetch templates in reset world", (Throwable)realmsServiceException);
                }
            }
        }.start();
        GridLayout gridLayout = this.layout.addToContents(new GridLayout());
        GridLayout.RowHelper rowHelper = gridLayout.createRowHelper(3);
        rowHelper.defaultCellSetting().paddingHorizontal(16);
        rowHelper.addChild(new FrameButton(this.minecraft.font, GENERATE_NEW_WORLD, NEW_WORLD_LOCATION, button -> RealmsCreateWorldFlow.createWorld(this.minecraft, this.lastScreen, this, this.slot, this.serverData, this.realmCreationTask)));
        rowHelper.addChild(new FrameButton(this.minecraft.font, RealmsSelectFileToUploadScreen.TITLE, UPLOAD_LOCATION, button -> this.minecraft.setScreen(new RealmsSelectFileToUploadScreen(this.realmCreationTask, this.serverData.id, this.slot, this))));
        rowHelper.addChild(new FrameButton(this.minecraft.font, WORLD_TEMPLATES_TITLE, SURVIVAL_SPAWN_LOCATION, button -> this.minecraft.setScreen(new RealmsSelectWorldTemplateScreen(WORLD_TEMPLATES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.NORMAL, this.templates))));
        rowHelper.addChild(SpacerElement.height(16), 3);
        rowHelper.addChild(new FrameButton(this.minecraft.font, ADVENTURES_TITLE, ADVENTURE_MAP_LOCATION, button -> this.minecraft.setScreen(new RealmsSelectWorldTemplateScreen(ADVENTURES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.ADVENTUREMAP, this.adventuremaps))));
        rowHelper.addChild(new FrameButton(this.minecraft.font, EXPERIENCES_TITLE, EXPERIENCE_LOCATION, button -> this.minecraft.setScreen(new RealmsSelectWorldTemplateScreen(EXPERIENCES_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.EXPERIENCE, this.experiences))));
        rowHelper.addChild(new FrameButton(this.minecraft.font, INSPIRATION_TITLE, INSPIRATION_LOCATION, button -> this.minecraft.setScreen(new RealmsSelectWorldTemplateScreen(INSPIRATION_TITLE, this::templateSelectionCallback, RealmsServer.WorldType.INSPIRATION, this.inspirations))));
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose()).build());
        this.layout.visitWidgets(guiEventListener -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.getTitle(), this.subtitle);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void templateSelectionCallback(@Nullable WorldTemplate worldTemplate) {
        this.minecraft.setScreen(this);
        if (worldTemplate != null) {
            this.runResetTasks(new ResettingTemplateWorldTask(worldTemplate, this.serverData.id, this.resetTaskTitle, this.resetWorldRunnable));
        }
        RealmsMainScreen.refreshServerList();
    }

    private void runResetTasks(LongRunningTask longRunningTask) {
        ArrayList<LongRunningTask> arrayList = new ArrayList<LongRunningTask>();
        if (this.realmCreationTask != null) {
            arrayList.add(this.realmCreationTask);
        }
        if (this.slot != this.serverData.activeSlot) {
            arrayList.add(new SwitchSlotTask(this.serverData.id, this.slot, () -> {}));
        }
        arrayList.add(longRunningTask);
        this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, arrayList.toArray(new LongRunningTask[0])));
    }

    class FrameButton
    extends Button {
        private static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
        private static final int FRAME_SIZE = 60;
        private static final int FRAME_WIDTH = 2;
        private static final int IMAGE_SIZE = 56;
        private final ResourceLocation image;

        FrameButton(Font font, Component component, ResourceLocation resourceLocation, Button.OnPress onPress) {
            super(0, 0, 60, 60 + font.lineHeight, component, onPress, DEFAULT_NARRATION);
            this.image = resourceLocation;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int n, int n2, float f) {
            boolean bl = this.isHoveredOrFocused();
            int n3 = -1;
            if (bl) {
                n3 = ARGB.colorFromFloat(1.0f, 0.56f, 0.56f, 0.56f);
            }
            int n4 = this.getX();
            int n5 = this.getY();
            guiGraphics.blit(RenderType::guiTextured, this.image, n4 + 2, n5 + 2, 0.0f, 0.0f, 56, 56, 56, 56, 56, 56, n3);
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_FRAME_SPRITE, n4, n5, 60, 60, n3);
            int n6 = bl ? -6250336 : -1;
            guiGraphics.drawCenteredString(RealmsResetWorldScreen.this.font, this.getMessage(), n4 + 28, n5 - 14, n6);
        }
    }
}

