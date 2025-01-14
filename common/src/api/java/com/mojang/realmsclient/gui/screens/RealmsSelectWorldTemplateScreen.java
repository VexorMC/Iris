/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.util.Either
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.TextRenderingUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonLinks;
import org.slf4j.Logger;

public class RealmsSelectWorldTemplateScreen
extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    static final ResourceLocation SLOT_FRAME_SPRITE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
    private static final Component SELECT_BUTTON_NAME = Component.translatable("mco.template.button.select");
    private static final Component TRAILER_BUTTON_NAME = Component.translatable("mco.template.button.trailer");
    private static final Component PUBLISHER_BUTTON_NAME = Component.translatable("mco.template.button.publisher");
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_SPACING = 10;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    final Consumer<WorldTemplate> callback;
    WorldTemplateList worldTemplateList;
    private final RealmsServer.WorldType worldType;
    private Button selectButton;
    private Button trailerButton;
    private Button publisherButton;
    @Nullable
    WorldTemplate selectedTemplate = null;
    @Nullable
    String currentLink;
    @Nullable
    private Component[] warning;
    @Nullable
    List<TextRenderingUtils.Line> noTemplatesMessage;

    public RealmsSelectWorldTemplateScreen(Component component, Consumer<WorldTemplate> consumer, RealmsServer.WorldType worldType) {
        this(component, consumer, worldType, null);
    }

    public RealmsSelectWorldTemplateScreen(Component component, Consumer<WorldTemplate> consumer, RealmsServer.WorldType worldType, @Nullable WorldTemplatePaginatedList worldTemplatePaginatedList) {
        super(component);
        this.callback = consumer;
        this.worldType = worldType;
        if (worldTemplatePaginatedList == null) {
            this.worldTemplateList = new WorldTemplateList();
            this.fetchTemplatesAsync(new WorldTemplatePaginatedList(10));
        } else {
            this.worldTemplateList = new WorldTemplateList(Lists.newArrayList(worldTemplatePaginatedList.templates));
            this.fetchTemplatesAsync(worldTemplatePaginatedList);
        }
    }

    public void setWarning(Component ... componentArray) {
        this.warning = componentArray;
    }

    @Override
    public void init() {
        this.layout.addTitleHeader(this.title, this.font);
        this.worldTemplateList = this.layout.addToContents(new WorldTemplateList(this.worldTemplateList.getTemplates()));
        LinearLayout linearLayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(10));
        linearLayout.defaultCellSetting().alignHorizontallyCenter();
        this.trailerButton = linearLayout.addChild(Button.builder(TRAILER_BUTTON_NAME, button -> this.onTrailer()).width(100).build());
        this.selectButton = linearLayout.addChild(Button.builder(SELECT_BUTTON_NAME, button -> this.selectTemplate()).width(100).build());
        linearLayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).width(100).build());
        this.publisherButton = linearLayout.addChild(Button.builder(PUBLISHER_BUTTON_NAME, button -> this.onPublish()).width(100).build());
        this.updateButtonStates();
        this.layout.visitWidgets(guiEventListener -> {
            AbstractWidget cfr_ignored_0 = (AbstractWidget)this.addRenderableWidget(guiEventListener);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.worldTemplateList.setSize(this.width, this.height - this.layout.getFooterHeight() - this.getHeaderHeight());
        this.layout.arrangeElements();
    }

    @Override
    public Component getNarrationMessage() {
        ArrayList arrayList = Lists.newArrayListWithCapacity((int)2);
        arrayList.add(this.title);
        if (this.warning != null) {
            arrayList.addAll(Arrays.asList(this.warning));
        }
        return CommonComponents.joinLines(arrayList);
    }

    void updateButtonStates() {
        this.publisherButton.visible = this.selectedTemplate != null && !this.selectedTemplate.link.isEmpty();
        this.trailerButton.visible = this.selectedTemplate != null && !this.selectedTemplate.trailer.isEmpty();
        this.selectButton.active = this.selectedTemplate != null;
    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

    private void selectTemplate() {
        if (this.selectedTemplate != null) {
            this.callback.accept(this.selectedTemplate);
        }
    }

    private void onTrailer() {
        if (this.selectedTemplate != null && !this.selectedTemplate.trailer.isBlank()) {
            ConfirmLinkScreen.confirmLinkNow((Screen)this, this.selectedTemplate.trailer);
        }
    }

    private void onPublish() {
        if (this.selectedTemplate != null && !this.selectedTemplate.link.isBlank()) {
            ConfirmLinkScreen.confirmLinkNow((Screen)this, this.selectedTemplate.link);
        }
    }

    private void fetchTemplatesAsync(final WorldTemplatePaginatedList worldTemplatePaginatedList) {
        new Thread("realms-template-fetcher"){

            @Override
            public void run() {
                WorldTemplatePaginatedList worldTemplatePaginatedList2 = worldTemplatePaginatedList;
                RealmsClient realmsClient = RealmsClient.create();
                while (worldTemplatePaginatedList2 != null) {
                    Either<WorldTemplatePaginatedList, Exception> either = RealmsSelectWorldTemplateScreen.this.fetchTemplates(worldTemplatePaginatedList2, realmsClient);
                    worldTemplatePaginatedList2 = RealmsSelectWorldTemplateScreen.this.minecraft.submit(() -> {
                        if (either.right().isPresent()) {
                            LOGGER.error("Couldn't fetch templates", (Throwable)either.right().get());
                            if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(I18n.get("mco.template.select.failure", new Object[0]), new TextRenderingUtils.LineSegment[0]);
                            }
                            return null;
                        }
                        WorldTemplatePaginatedList worldTemplatePaginatedList2 = (WorldTemplatePaginatedList)either.left().get();
                        for (WorldTemplate object : worldTemplatePaginatedList2.templates) {
                            RealmsSelectWorldTemplateScreen.this.worldTemplateList.addEntry(object);
                        }
                        if (worldTemplatePaginatedList2.templates.isEmpty()) {
                            if (RealmsSelectWorldTemplateScreen.this.worldTemplateList.isEmpty()) {
                                String string = I18n.get("mco.template.select.none", "%link");
                                TextRenderingUtils.LineSegment lineSegment = TextRenderingUtils.LineSegment.link(I18n.get("mco.template.select.none.linkTitle", new Object[0]), CommonLinks.REALMS_CONTENT_CREATION.toString());
                                RealmsSelectWorldTemplateScreen.this.noTemplatesMessage = TextRenderingUtils.decompose(string, lineSegment);
                            }
                            return null;
                        }
                        return worldTemplatePaginatedList2;
                    }).join();
                }
            }
        }.start();
    }

    Either<WorldTemplatePaginatedList, Exception> fetchTemplates(WorldTemplatePaginatedList worldTemplatePaginatedList, RealmsClient realmsClient) {
        try {
            return Either.left((Object)realmsClient.fetchWorldTemplates(worldTemplatePaginatedList.page + 1, worldTemplatePaginatedList.size, this.worldType));
        }
        catch (RealmsServiceException realmsServiceException) {
            return Either.right((Object)realmsServiceException);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        this.currentLink = null;
        if (this.noTemplatesMessage != null) {
            this.renderMultilineMessage(guiGraphics, n, n2, this.noTemplatesMessage);
        }
        if (this.warning != null) {
            for (int i = 0; i < this.warning.length; ++i) {
                Component component = this.warning[i];
                guiGraphics.drawCenteredString(this.font, component, this.width / 2, RealmsSelectWorldTemplateScreen.row(-1 + i), -6250336);
            }
        }
    }

    private void renderMultilineMessage(GuiGraphics guiGraphics, int n, int n2, List<TextRenderingUtils.Line> list) {
        for (int i = 0; i < list.size(); ++i) {
            TextRenderingUtils.Line line = list.get(i);
            int n3 = RealmsSelectWorldTemplateScreen.row(4 + i);
            int n4 = line.segments.stream().mapToInt(lineSegment -> this.font.width(lineSegment.renderedText())).sum();
            int n5 = this.width / 2 - n4 / 2;
            for (TextRenderingUtils.LineSegment lineSegment2 : line.segments) {
                int n6 = lineSegment2.isLink() ? 0x3366BB : -1;
                int n7 = guiGraphics.drawString(this.font, lineSegment2.renderedText(), n5, n3, n6);
                if (lineSegment2.isLink() && n > n5 && n < n7 && n2 > n3 - 3 && n2 < n3 + 8) {
                    this.setTooltipForNextRenderPass(Component.literal(lineSegment2.getLinkUrl()));
                    this.currentLink = lineSegment2.getLinkUrl();
                }
                n5 = n7;
            }
        }
    }

    int getHeaderHeight() {
        return this.warning != null ? RealmsSelectWorldTemplateScreen.row(1) : 33;
    }

    class WorldTemplateList
    extends ObjectSelectionList<Entry> {
        public WorldTemplateList() {
            this(Collections.emptyList());
        }

        public WorldTemplateList(Iterable<WorldTemplate> iterable) {
            super(Minecraft.getInstance(), RealmsSelectWorldTemplateScreen.this.width, RealmsSelectWorldTemplateScreen.this.height - 33 - RealmsSelectWorldTemplateScreen.this.getHeaderHeight(), RealmsSelectWorldTemplateScreen.this.getHeaderHeight(), 46);
            iterable.forEach(this::addEntry);
        }

        public void addEntry(WorldTemplate worldTemplate) {
            this.addEntry(new Entry(worldTemplate));
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            if (RealmsSelectWorldTemplateScreen.this.currentLink != null) {
                ConfirmLinkScreen.confirmLinkNow((Screen)RealmsSelectWorldTemplateScreen.this, RealmsSelectWorldTemplateScreen.this.currentLink);
                return true;
            }
            return super.mouseClicked(d, d2, n);
        }

        @Override
        public void setSelected(@Nullable Entry entry) {
            super.setSelected(entry);
            RealmsSelectWorldTemplateScreen.this.selectedTemplate = entry == null ? null : entry.template;
            RealmsSelectWorldTemplateScreen.this.updateButtonStates();
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        public boolean isEmpty() {
            return this.getItemCount() == 0;
        }

        public List<WorldTemplate> getTemplates() {
            return this.children().stream().map(entry -> entry.template).collect(Collectors.toList());
        }
    }

    class Entry
    extends ObjectSelectionList.Entry<Entry> {
        private static final WidgetSprites WEBSITE_LINK_SPRITES = new WidgetSprites(ResourceLocation.withDefaultNamespace("icon/link"), ResourceLocation.withDefaultNamespace("icon/link_highlighted"));
        private static final WidgetSprites TRAILER_LINK_SPRITES = new WidgetSprites(ResourceLocation.withDefaultNamespace("icon/video_link"), ResourceLocation.withDefaultNamespace("icon/video_link_highlighted"));
        private static final Component PUBLISHER_LINK_TOOLTIP = Component.translatable("mco.template.info.tooltip");
        private static final Component TRAILER_LINK_TOOLTIP = Component.translatable("mco.template.trailer.tooltip");
        public final WorldTemplate template;
        private long lastClickTime;
        @Nullable
        private ImageButton websiteButton;
        @Nullable
        private ImageButton trailerButton;

        public Entry(WorldTemplate worldTemplate) {
            this.template = worldTemplate;
            if (!worldTemplate.link.isBlank()) {
                this.websiteButton = new ImageButton(15, 15, WEBSITE_LINK_SPRITES, ConfirmLinkScreen.confirmLink((Screen)RealmsSelectWorldTemplateScreen.this, worldTemplate.link), PUBLISHER_LINK_TOOLTIP);
                this.websiteButton.setTooltip(Tooltip.create(PUBLISHER_LINK_TOOLTIP));
            }
            if (!worldTemplate.trailer.isBlank()) {
                this.trailerButton = new ImageButton(15, 15, TRAILER_LINK_SPRITES, ConfirmLinkScreen.confirmLink((Screen)RealmsSelectWorldTemplateScreen.this, worldTemplate.trailer), TRAILER_LINK_TOOLTIP);
                this.trailerButton.setTooltip(Tooltip.create(TRAILER_LINK_TOOLTIP));
            }
        }

        @Override
        public boolean mouseClicked(double d, double d2, int n) {
            RealmsSelectWorldTemplateScreen.this.selectedTemplate = this.template;
            RealmsSelectWorldTemplateScreen.this.updateButtonStates();
            if (Util.getMillis() - this.lastClickTime < 250L && this.isFocused()) {
                RealmsSelectWorldTemplateScreen.this.callback.accept(this.template);
            }
            this.lastClickTime = Util.getMillis();
            if (this.websiteButton != null) {
                this.websiteButton.mouseClicked(d, d2, n);
            }
            if (this.trailerButton != null) {
                this.trailerButton.mouseClicked(d, d2, n);
            }
            return super.mouseClicked(d, d2, n);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int n, int n2, int n3, int n4, int n5, int n6, int n7, boolean bl, float f) {
            guiGraphics.blit(RenderType::guiTextured, RealmsTextureManager.worldTemplate(this.template.id, this.template.image), n3 + 1, n2 + 1 + 1, 0.0f, 0.0f, 38, 38, 38, 38);
            guiGraphics.blitSprite(RenderType::guiTextured, SLOT_FRAME_SPRITE, n3, n2 + 1, 40, 40);
            int n8 = 5;
            int n9 = RealmsSelectWorldTemplateScreen.this.font.width(this.template.version);
            if (this.websiteButton != null) {
                this.websiteButton.setPosition(n3 + n4 - n9 - this.websiteButton.getWidth() - 10, n2);
                this.websiteButton.render(guiGraphics, n6, n7, f);
            }
            if (this.trailerButton != null) {
                this.trailerButton.setPosition(n3 + n4 - n9 - this.trailerButton.getWidth() * 2 - 15, n2);
                this.trailerButton.render(guiGraphics, n6, n7, f);
            }
            int n10 = n3 + 45 + 20;
            int n11 = n2 + 5;
            guiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.name, n10, n11, -1);
            guiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.version, n3 + n4 - n9 - 5, n11, 0x6C6C6C);
            guiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.author, n10, n11 + ((RealmsSelectWorldTemplateScreen)RealmsSelectWorldTemplateScreen.this).font.lineHeight + 5, -6250336);
            if (!this.template.recommendedPlayers.isBlank()) {
                guiGraphics.drawString(RealmsSelectWorldTemplateScreen.this.font, this.template.recommendedPlayers, n10, n2 + n5 - ((RealmsSelectWorldTemplateScreen)RealmsSelectWorldTemplateScreen.this).font.lineHeight / 2 - 5, 0x4C4C4C);
            }
        }

        @Override
        public Component getNarration() {
            Component component = CommonComponents.joinLines(Component.literal(this.template.name), Component.translatable("mco.template.select.narrate.authors", this.template.author), Component.literal(this.template.recommendedPlayers), Component.translatable("mco.template.select.narrate.version", this.template.version));
            return Component.translatable("narrator.select", component);
        }
    }
}

