/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.RealmsText;
import com.mojang.realmsclient.util.JsonUtils;
import java.lang.invoke.MethodHandle;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class RealmsNotification {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String NOTIFICATION_UUID = "notificationUuid";
    private static final String DISMISSABLE = "dismissable";
    private static final String SEEN = "seen";
    private static final String TYPE = "type";
    private static final String VISIT_URL = "visitUrl";
    private static final String INFO_POPUP = "infoPopup";
    static final Component BUTTON_TEXT_FALLBACK = Component.translatable("mco.notification.visitUrl.buttonText.default");
    final UUID uuid;
    final boolean dismissable;
    final boolean seen;
    final String type;

    RealmsNotification(UUID uUID, boolean bl, boolean bl2, String string) {
        this.uuid = uUID;
        this.dismissable = bl;
        this.seen = bl2;
        this.type = string;
    }

    public boolean seen() {
        return this.seen;
    }

    public boolean dismissable() {
        return this.dismissable;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public static List<RealmsNotification> parseList(String string) {
        ArrayList<RealmsNotification> arrayList = new ArrayList<RealmsNotification>();
        try {
            JsonArray jsonArray = JsonParser.parseString((String)string).getAsJsonObject().get("notifications").getAsJsonArray();
            for (JsonElement jsonElement : jsonArray) {
                arrayList.add(RealmsNotification.parse(jsonElement.getAsJsonObject()));
            }
        }
        catch (Exception exception) {
            LOGGER.error("Could not parse list of RealmsNotifications", (Throwable)exception);
        }
        return arrayList;
    }

    private static RealmsNotification parse(JsonObject jsonObject) {
        UUID uUID = JsonUtils.getUuidOr(NOTIFICATION_UUID, jsonObject, null);
        if (uUID == null) {
            throw new IllegalStateException("Missing required property notificationUuid");
        }
        boolean bl = JsonUtils.getBooleanOr(DISMISSABLE, jsonObject, true);
        boolean bl2 = JsonUtils.getBooleanOr(SEEN, jsonObject, false);
        String string = JsonUtils.getRequiredString(TYPE, jsonObject);
        RealmsNotification realmsNotification = new RealmsNotification(uUID, bl, bl2, string);
        return switch (string) {
            case VISIT_URL -> VisitUrl.parse(realmsNotification, jsonObject);
            case INFO_POPUP -> InfoPopup.parse(realmsNotification, jsonObject);
            default -> realmsNotification;
        };
    }

    public static class VisitUrl
    extends RealmsNotification {
        private static final String URL = "url";
        private static final String BUTTON_TEXT = "buttonText";
        private static final String MESSAGE = "message";
        private final String url;
        private final RealmsText buttonText;
        private final RealmsText message;

        private VisitUrl(RealmsNotification realmsNotification, String string, RealmsText realmsText, RealmsText realmsText2) {
            super(realmsNotification.uuid, realmsNotification.dismissable, realmsNotification.seen, realmsNotification.type);
            this.url = string;
            this.buttonText = realmsText;
            this.message = realmsText2;
        }

        public static VisitUrl parse(RealmsNotification realmsNotification, JsonObject jsonObject) {
            String string = JsonUtils.getRequiredString(URL, jsonObject);
            RealmsText realmsText = JsonUtils.getRequired(BUTTON_TEXT, jsonObject, RealmsText::parse);
            RealmsText realmsText2 = JsonUtils.getRequired(MESSAGE, jsonObject, RealmsText::parse);
            return new VisitUrl(realmsNotification, string, realmsText, realmsText2);
        }

        public Component getMessage() {
            return this.message.createComponent(Component.translatable("mco.notification.visitUrl.message.default"));
        }

        public Button buildOpenLinkButton(Screen screen) {
            Component component = this.buttonText.createComponent(BUTTON_TEXT_FALLBACK);
            return Button.builder(component, ConfirmLinkScreen.confirmLink(screen, this.url)).build();
        }
    }

    public static class InfoPopup
    extends RealmsNotification {
        private static final String TITLE = "title";
        private static final String MESSAGE = "message";
        private static final String IMAGE = "image";
        private static final String URL_BUTTON = "urlButton";
        private final RealmsText title;
        private final RealmsText message;
        private final ResourceLocation image;
        @Nullable
        private final UrlButton urlButton;

        private InfoPopup(RealmsNotification realmsNotification, RealmsText realmsText, RealmsText realmsText2, ResourceLocation resourceLocation, @Nullable UrlButton urlButton) {
            super(realmsNotification.uuid, realmsNotification.dismissable, realmsNotification.seen, realmsNotification.type);
            this.title = realmsText;
            this.message = realmsText2;
            this.image = resourceLocation;
            this.urlButton = urlButton;
        }

        public static InfoPopup parse(RealmsNotification realmsNotification, JsonObject jsonObject) {
            RealmsText realmsText = JsonUtils.getRequired(TITLE, jsonObject, RealmsText::parse);
            RealmsText realmsText2 = JsonUtils.getRequired(MESSAGE, jsonObject, RealmsText::parse);
            ResourceLocation resourceLocation = ResourceLocation.parse(JsonUtils.getRequiredString(IMAGE, jsonObject));
            UrlButton urlButton = JsonUtils.getOptional(URL_BUTTON, jsonObject, UrlButton::parse);
            return new InfoPopup(realmsNotification, realmsText, realmsText2, resourceLocation, urlButton);
        }

        @Nullable
        public PopupScreen buildScreen(Screen screen, Consumer<UUID> consumer) {
            Component component = this.title.createComponent();
            if (component == null) {
                LOGGER.warn("Realms info popup had title with no available translation: {}", (Object)this.title);
                return null;
            }
            PopupScreen.Builder builder = new PopupScreen.Builder(screen, component).setImage(this.image).setMessage(this.message.createComponent(CommonComponents.EMPTY));
            if (this.urlButton != null) {
                builder.addButton(this.urlButton.urlText.createComponent(BUTTON_TEXT_FALLBACK), popupScreen -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(new ConfirmLinkScreen(bl -> {
                        if (bl) {
                            Util.getPlatform().openUri(this.urlButton.url);
                            minecraft.setScreen(screen);
                        } else {
                            minecraft.setScreen((Screen)popupScreen);
                        }
                    }, this.urlButton.url, true));
                    consumer.accept(this.uuid());
                });
            }
            builder.addButton(CommonComponents.GUI_OK, popupScreen -> {
                popupScreen.onClose();
                consumer.accept(this.uuid());
            });
            builder.onClose(() -> consumer.accept(this.uuid()));
            return builder.build();
        }
    }

    static final class UrlButton
    extends Record {
        final String url;
        final RealmsText urlText;
        private static final String URL = "url";
        private static final String URL_TEXT = "urlText";

        private UrlButton(String string, RealmsText realmsText) {
            this.url = string;
            this.urlText = realmsText;
        }

        public static UrlButton parse(JsonObject jsonObject) {
            String string = JsonUtils.getRequiredString(URL, jsonObject);
            RealmsText realmsText = JsonUtils.getRequired(URL_TEXT, jsonObject, RealmsText::parse);
            return new UrlButton(string, realmsText);
        }

        @Override
        public final String toString() {
            return ObjectMethods.bootstrap("toString", new MethodHandle[]{UrlButton.class, "url;urlText", "url", "urlText"}, this);
        }

        @Override
        public final int hashCode() {
            return (int)ObjectMethods.bootstrap("hashCode", new MethodHandle[]{UrlButton.class, "url;urlText", "url", "urlText"}, this);
        }

        @Override
        public final boolean equals(Object object) {
            return (boolean)ObjectMethods.bootstrap("equals", new MethodHandle[]{UrlButton.class, "url;urlText", "url", "urlText"}, this, object);
        }

        public String url() {
            return this.url;
        }

        public RealmsText urlText() {
            return this.urlText;
        }
    }
}

