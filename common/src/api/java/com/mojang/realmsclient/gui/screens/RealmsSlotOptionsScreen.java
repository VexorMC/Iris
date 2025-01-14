/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 */
package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class RealmsSlotOptionsScreen
extends RealmsScreen {
    private static final int DEFAULT_DIFFICULTY = 2;
    public static final List<Difficulty> DIFFICULTIES = ImmutableList.of((Object)Difficulty.PEACEFUL, (Object)Difficulty.EASY, (Object)Difficulty.NORMAL, (Object)Difficulty.HARD);
    private static final int DEFAULT_GAME_MODE = 0;
    public static final List<GameType> GAME_MODES = ImmutableList.of((Object)GameType.SURVIVAL, (Object)GameType.CREATIVE, (Object)GameType.ADVENTURE);
    private static final Component NAME_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
    static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
    private EditBox nameEdit;
    protected final RealmsConfigureWorldScreen parentScreen;
    private int column1X;
    private int columnWidth;
    private final RealmsWorldOptions options;
    private final RealmsServer.WorldType worldType;
    private Difficulty difficulty;
    private GameType gameMode;
    private final String defaultSlotName;
    private String worldName;
    private boolean pvp;
    private boolean spawnMonsters;
    int spawnProtection;
    private boolean commandBlocks;
    private boolean forceGameMode;
    SettingsSlider spawnProtectionButton;

    public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen realmsConfigureWorldScreen, RealmsWorldOptions realmsWorldOptions, RealmsServer.WorldType worldType, int n) {
        super(Component.translatable("mco.configure.world.buttons.options"));
        this.parentScreen = realmsConfigureWorldScreen;
        this.options = realmsWorldOptions;
        this.worldType = worldType;
        this.difficulty = RealmsSlotOptionsScreen.findByIndex(DIFFICULTIES, realmsWorldOptions.difficulty, 2);
        this.gameMode = RealmsSlotOptionsScreen.findByIndex(GAME_MODES, realmsWorldOptions.gameMode, 0);
        this.defaultSlotName = realmsWorldOptions.getDefaultSlotName(n);
        this.setWorldName(realmsWorldOptions.getSlotName(n));
        if (worldType == RealmsServer.WorldType.NORMAL) {
            this.pvp = realmsWorldOptions.pvp;
            this.spawnProtection = realmsWorldOptions.spawnProtection;
            this.forceGameMode = realmsWorldOptions.forceGameMode;
            this.spawnMonsters = realmsWorldOptions.spawnMonsters;
            this.commandBlocks = realmsWorldOptions.commandBlocks;
        } else {
            this.pvp = true;
            this.spawnProtection = 0;
            this.forceGameMode = false;
            this.spawnMonsters = true;
            this.commandBlocks = true;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }

    private static <T> T findByIndex(List<T> list, int n, int n2) {
        try {
            return list.get(n);
        }
        catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            return list.get(n2);
        }
    }

    private static <T> int findIndex(List<T> list, T t, int n) {
        int n2 = list.indexOf(t);
        return n2 == -1 ? n : n2;
    }

    @Override
    public void init() {
        Object object;
        this.columnWidth = 170;
        this.column1X = this.width / 2 - this.columnWidth;
        int n = this.width / 2 + 10;
        if (this.worldType != RealmsServer.WorldType.NORMAL) {
            object = this.worldType == RealmsServer.WorldType.ADVENTUREMAP ? Component.translatable("mco.configure.world.edit.subscreen.adventuremap") : (this.worldType == RealmsServer.WorldType.INSPIRATION ? Component.translatable("mco.configure.world.edit.subscreen.inspiration") : Component.translatable("mco.configure.world.edit.subscreen.experience"));
            this.addLabel(new RealmsLabel((Component)object, this.width / 2, 26, 0xFF0000));
        }
        this.nameEdit = this.addWidget(new EditBox(this.minecraft.font, this.column1X, RealmsSlotOptionsScreen.row(1), this.columnWidth, 20, null, Component.translatable("mco.configure.world.edit.slot.name")));
        this.nameEdit.setMaxLength(10);
        this.nameEdit.setValue(this.worldName);
        this.nameEdit.setResponder(this::setWorldName);
        object = this.addRenderableWidget(CycleButton.onOffBuilder(this.pvp).create(n, RealmsSlotOptionsScreen.row(1), this.columnWidth, 20, Component.translatable("mco.configure.world.pvp"), (cycleButton, bl) -> {
            this.pvp = bl;
        }));
        this.addRenderableWidget(CycleButton.builder(GameType::getShortDisplayName).withValues((Collection<GameType>)GAME_MODES).withInitialValue(this.gameMode).create(this.column1X, RealmsSlotOptionsScreen.row(3), this.columnWidth, 20, Component.translatable("selectWorld.gameMode"), (cycleButton, gameType) -> {
            this.gameMode = gameType;
        }));
        this.spawnProtectionButton = this.addRenderableWidget(new SettingsSlider(n, RealmsSlotOptionsScreen.row(3), this.columnWidth, this.spawnProtection, 0.0f, 16.0f));
        MutableComponent mutableComponent = Component.translatable("mco.configure.world.spawn_toggle.message");
        CycleButton<Boolean> cycleButton3 = CycleButton.onOffBuilder(this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters).create(n, RealmsSlotOptionsScreen.row(5), this.columnWidth, 20, Component.translatable("mco.configure.world.spawnMonsters"), this.confirmDangerousOption(mutableComponent, bl -> {
            this.spawnMonsters = bl;
        }));
        this.addRenderableWidget(CycleButton.builder(Difficulty::getDisplayName).withValues((Collection<Difficulty>)DIFFICULTIES).withInitialValue(this.difficulty).create(this.column1X, RealmsSlotOptionsScreen.row(5), this.columnWidth, 20, Component.translatable("options.difficulty"), (cycleButton2, difficulty) -> {
            this.difficulty = difficulty;
            if (this.worldType == RealmsServer.WorldType.NORMAL) {
                boolean bl;
                cycleButton.active = bl = this.difficulty != Difficulty.PEACEFUL;
                cycleButton3.setValue(bl && this.spawnMonsters);
            }
        }));
        this.addRenderableWidget(cycleButton3);
        CycleButton<Boolean> cycleButton4 = this.addRenderableWidget(CycleButton.onOffBuilder(this.forceGameMode).create(this.column1X, RealmsSlotOptionsScreen.row(7), this.columnWidth, 20, Component.translatable("mco.configure.world.forceGameMode"), (cycleButton, bl) -> {
            this.forceGameMode = bl;
        }));
        CycleButton<Boolean> cycleButton5 = this.addRenderableWidget(CycleButton.onOffBuilder(this.commandBlocks).create(n, RealmsSlotOptionsScreen.row(7), this.columnWidth, 20, Component.translatable("mco.configure.world.commandBlocks"), (cycleButton, bl) -> {
            this.commandBlocks = bl;
        }));
        if (this.worldType != RealmsServer.WorldType.NORMAL) {
            ((CycleButton)object).active = false;
            cycleButton3.active = false;
            this.spawnProtectionButton.active = false;
            cycleButton5.active = false;
            cycleButton4.active = false;
        }
        if (this.difficulty == Difficulty.PEACEFUL) {
            cycleButton3.active = false;
        }
        this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.done"), button -> this.saveSettings()).bounds(this.column1X, RealmsSlotOptionsScreen.row(13), this.columnWidth, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).bounds(n, RealmsSlotOptionsScreen.row(13), this.columnWidth, 20).build());
    }

    private CycleButton.OnValueChange<Boolean> confirmDangerousOption(Component component, Consumer<Boolean> consumer) {
        return (cycleButton, bl) -> {
            if (bl.booleanValue()) {
                consumer.accept(true);
            } else {
                this.minecraft.setScreen(RealmsPopups.warningPopupScreen(this, component, popupScreen -> {
                    consumer.accept(false);
                    popupScreen.onClose();
                }));
            }
        };
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int n, int n2, float f) {
        super.render(guiGraphics, n, n2, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
        guiGraphics.drawString(this.font, NAME_LABEL, this.column1X + this.columnWidth / 2 - this.font.width(NAME_LABEL) / 2, RealmsSlotOptionsScreen.row(0) - 5, -1);
        this.nameEdit.render(guiGraphics, n, n2, f);
    }

    private void setWorldName(String string) {
        this.worldName = string.equals(this.defaultSlotName) ? "" : string;
    }

    private void saveSettings() {
        int n = RealmsSlotOptionsScreen.findIndex(DIFFICULTIES, this.difficulty, 2);
        int n2 = RealmsSlotOptionsScreen.findIndex(GAME_MODES, this.gameMode, 0);
        if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP || this.worldType == RealmsServer.WorldType.EXPERIENCE || this.worldType == RealmsServer.WorldType.INSPIRATION) {
            this.parentScreen.saveSlotSettings(new RealmsWorldOptions(this.options.pvp, this.options.spawnMonsters, this.options.spawnProtection, this.options.commandBlocks, n, n2, this.options.hardcore, this.options.forceGameMode, this.worldName, this.options.version, this.options.compatibility));
        } else {
            boolean bl = this.worldType == RealmsServer.WorldType.NORMAL && this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters;
            this.parentScreen.saveSlotSettings(new RealmsWorldOptions(this.pvp, bl, this.spawnProtection, this.commandBlocks, n, n2, this.options.hardcore, this.forceGameMode, this.worldName, this.options.version, this.options.compatibility));
        }
    }

    class SettingsSlider
    extends AbstractSliderButton {
        private final double minValue;
        private final double maxValue;

        public SettingsSlider(int n, int n2, int n3, int n4, float f, float f2) {
            super(n, n2, n3, 20, CommonComponents.EMPTY, 0.0);
            this.minValue = f;
            this.maxValue = f2;
            this.value = (Mth.clamp((float)n4, f, f2) - f) / (f2 - f);
            this.updateMessage();
        }

        @Override
        public void applyValue() {
            if (!RealmsSlotOptionsScreen.this.spawnProtectionButton.active) {
                return;
            }
            RealmsSlotOptionsScreen.this.spawnProtection = (int)Mth.lerp(Mth.clamp(this.value, 0.0, 1.0), this.minValue, this.maxValue);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(CommonComponents.optionNameValue(SPAWN_PROTECTION_TEXT, RealmsSlotOptionsScreen.this.spawnProtection == 0 ? CommonComponents.OPTION_OFF : Component.literal(String.valueOf(RealmsSlotOptionsScreen.this.spawnProtection))));
        }
    }
}

