/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.mojang.logging.LogUtils
 *  com.mojang.util.UndashedUuid
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClientConfig;
import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.client.Request;
import com.mojang.realmsclient.dto.BackupList;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.dto.PendingInvitesList;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsDescriptionDto;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerAddress;
import com.mojang.realmsclient.dto.RealmsServerList;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.RealmsWorldResetDto;
import com.mojang.realmsclient.dto.ServerActivityList;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.dto.WorldTemplatePaginatedList;
import com.mojang.realmsclient.exception.RealmsHttpException;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.UploadTokenCache;
import com.mojang.util.UndashedUuid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class RealmsClient {
    public static final Environment ENVIRONMENT = Optional.ofNullable(System.getenv("realms.environment")).or(() -> Optional.ofNullable(System.getProperty("realms.environment"))).flatMap(Environment::byName).orElse(Environment.PRODUCTION);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String sessionId;
    private final String username;
    private final Minecraft minecraft;
    private static final String WORLDS_RESOURCE_PATH = "worlds";
    private static final String INVITES_RESOURCE_PATH = "invites";
    private static final String MCO_RESOURCE_PATH = "mco";
    private static final String SUBSCRIPTION_RESOURCE = "subscriptions";
    private static final String ACTIVITIES_RESOURCE = "activities";
    private static final String OPS_RESOURCE = "ops";
    private static final String REGIONS_RESOURCE = "regions/ping/stat";
    private static final String TRIALS_RESOURCE = "trial";
    private static final String NOTIFICATIONS_RESOURCE = "notifications";
    private static final String PATH_LIST_ALL_REALMS = "/listUserWorldsOfType/any";
    private static final String PATH_CREATE_SNAPSHOT_REALM = "/$PARENT_WORLD_ID/createPrereleaseRealm";
    private static final String PATH_SNAPSHOT_ELIGIBLE_REALMS = "/listPrereleaseEligibleWorlds";
    private static final String PATH_INITIALIZE = "/$WORLD_ID/initialize";
    private static final String PATH_GET_ACTIVTIES = "/$WORLD_ID";
    private static final String PATH_GET_LIVESTATS = "/liveplayerlist";
    private static final String PATH_GET_SUBSCRIPTION = "/$WORLD_ID";
    private static final String PATH_OP = "/$WORLD_ID/$PROFILE_UUID";
    private static final String PATH_PUT_INTO_MINIGAMES_MODE = "/minigames/$MINIGAME_ID/$WORLD_ID";
    private static final String PATH_AVAILABLE = "/available";
    private static final String PATH_TEMPLATES = "/templates/$WORLD_TYPE";
    private static final String PATH_WORLD_JOIN = "/v1/$ID/join/pc";
    private static final String PATH_WORLD_GET = "/$ID";
    private static final String PATH_WORLD_INVITES = "/$WORLD_ID";
    private static final String PATH_WORLD_UNINVITE = "/$WORLD_ID/invite/$UUID";
    private static final String PATH_PENDING_INVITES_COUNT = "/count/pending";
    private static final String PATH_PENDING_INVITES = "/pending";
    private static final String PATH_ACCEPT_INVITE = "/accept/$INVITATION_ID";
    private static final String PATH_REJECT_INVITE = "/reject/$INVITATION_ID";
    private static final String PATH_UNINVITE_MYSELF = "/$WORLD_ID";
    private static final String PATH_WORLD_UPDATE = "/$WORLD_ID";
    private static final String PATH_SLOT = "/$WORLD_ID/slot/$SLOT_ID";
    private static final String PATH_WORLD_OPEN = "/$WORLD_ID/open";
    private static final String PATH_WORLD_CLOSE = "/$WORLD_ID/close";
    private static final String PATH_WORLD_RESET = "/$WORLD_ID/reset";
    private static final String PATH_DELETE_WORLD = "/$WORLD_ID";
    private static final String PATH_WORLD_BACKUPS = "/$WORLD_ID/backups";
    private static final String PATH_WORLD_DOWNLOAD = "/$WORLD_ID/slot/$SLOT_ID/download";
    private static final String PATH_WORLD_UPLOAD = "/$WORLD_ID/backups/upload";
    private static final String PATH_CLIENT_COMPATIBLE = "/client/compatible";
    private static final String PATH_TOS_AGREED = "/tos/agreed";
    private static final String PATH_NEWS = "/v1/news";
    private static final String PATH_MARK_NOTIFICATIONS_SEEN = "/seen";
    private static final String PATH_DISMISS_NOTIFICATIONS = "/dismiss";
    private static final GuardedSerializer GSON = new GuardedSerializer();

    public static RealmsClient create() {
        Minecraft minecraft = Minecraft.getInstance();
        return RealmsClient.create(minecraft);
    }

    public static RealmsClient create(Minecraft minecraft) {
        String string = minecraft.getUser().getName();
        String string2 = minecraft.getUser().getSessionId();
        return new RealmsClient(string2, string, minecraft);
    }

    public RealmsClient(String string, String string2, Minecraft minecraft) {
        this.sessionId = string;
        this.username = string2;
        this.minecraft = minecraft;
        RealmsClientConfig.setProxy(minecraft.getProxy());
    }

    public RealmsServerList listRealms() throws RealmsServiceException {
        Object object = this.url(WORLDS_RESOURCE_PATH);
        if (RealmsMainScreen.isSnapshot()) {
            object = (String)object + PATH_LIST_ALL_REALMS;
        }
        String string = this.execute(Request.get((String)object));
        return RealmsServerList.parse(string);
    }

    public List<RealmsServer> listSnapshotEligibleRealms() throws RealmsServiceException {
        String string = this.url("worlds/listPrereleaseEligibleWorlds");
        String string2 = this.execute(Request.get(string));
        return RealmsServerList.parse((String)string2).servers;
    }

    public RealmsServer createSnapshotRealm(Long l) throws RealmsServiceException {
        String string = String.valueOf(l);
        String string2 = this.url(WORLDS_RESOURCE_PATH + PATH_CREATE_SNAPSHOT_REALM.replace("$PARENT_WORLD_ID", string));
        return RealmsServer.parse(this.execute(Request.post(string2, string)));
    }

    public List<RealmsNotification> getNotifications() throws RealmsServiceException {
        String string = this.url(NOTIFICATIONS_RESOURCE);
        String string2 = this.execute(Request.get(string));
        return RealmsNotification.parseList(string2);
    }

    private static JsonArray uuidListToJsonArray(List<UUID> list) {
        JsonArray jsonArray = new JsonArray();
        for (UUID uUID : list) {
            if (uUID == null) continue;
            jsonArray.add(uUID.toString());
        }
        return jsonArray;
    }

    public void notificationsSeen(List<UUID> list) throws RealmsServiceException {
        String string = this.url("notifications/seen");
        this.execute(Request.post(string, GSON.toJson((JsonElement)RealmsClient.uuidListToJsonArray(list))));
    }

    public void notificationsDismiss(List<UUID> list) throws RealmsServiceException {
        String string = this.url("notifications/dismiss");
        this.execute(Request.post(string, GSON.toJson((JsonElement)RealmsClient.uuidListToJsonArray(list))));
    }

    public RealmsServer getOwnRealm(long l) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_GET.replace("$ID", String.valueOf(l)));
        String string2 = this.execute(Request.get(string));
        return RealmsServer.parse(string2);
    }

    public ServerActivityList getActivity(long l) throws RealmsServiceException {
        String string = this.url(ACTIVITIES_RESOURCE + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(l)));
        String string2 = this.execute(Request.get(string));
        return ServerActivityList.parse(string2);
    }

    public RealmsServerPlayerLists getLiveStats() throws RealmsServiceException {
        String string = this.url("activities/liveplayerlist");
        String string2 = this.execute(Request.get(string));
        return RealmsServerPlayerLists.parse(string2);
    }

    public RealmsServerAddress join(long l) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_JOIN.replace("$ID", "" + l));
        String string2 = this.execute(Request.get(string, 5000, 30000));
        return RealmsServerAddress.parse(string2);
    }

    public void initializeRealm(long l, String string, String string2) throws RealmsServiceException {
        RealmsDescriptionDto realmsDescriptionDto = new RealmsDescriptionDto(string, string2);
        String string3 = this.url(WORLDS_RESOURCE_PATH + PATH_INITIALIZE.replace("$WORLD_ID", String.valueOf(l)));
        String string4 = GSON.toJson(realmsDescriptionDto);
        this.execute(Request.post(string3, string4, 5000, 10000));
    }

    public boolean hasParentalConsent() throws RealmsServiceException {
        String string = this.url("mco/available");
        String string2 = this.execute(Request.get(string));
        return Boolean.parseBoolean(string2);
    }

    public CompatibleVersionResponse clientCompatible() throws RealmsServiceException {
        CompatibleVersionResponse compatibleVersionResponse;
        String string = this.url("mco/client/compatible");
        String string2 = this.execute(Request.get(string));
        try {
            compatibleVersionResponse = CompatibleVersionResponse.valueOf(string2);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            throw new RealmsServiceException(RealmsError.CustomError.unknownCompatibilityResponse(string2));
        }
        return compatibleVersionResponse;
    }

    public void uninvite(long l, UUID uUID) throws RealmsServiceException {
        String string = this.url(INVITES_RESOURCE_PATH + PATH_WORLD_UNINVITE.replace("$WORLD_ID", String.valueOf(l)).replace("$UUID", UndashedUuid.toString((UUID)uUID)));
        this.execute(Request.delete(string));
    }

    public void uninviteMyselfFrom(long l) throws RealmsServiceException {
        String string = this.url(INVITES_RESOURCE_PATH + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(l)));
        this.execute(Request.delete(string));
    }

    public RealmsServer invite(long l, String string) throws RealmsServiceException {
        PlayerInfo playerInfo = new PlayerInfo();
        playerInfo.setName(string);
        String string2 = this.url(INVITES_RESOURCE_PATH + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(l)));
        String string3 = this.execute(Request.post(string2, GSON.toJson(playerInfo)));
        return RealmsServer.parse(string3);
    }

    public BackupList backupsFor(long l) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_BACKUPS.replace("$WORLD_ID", String.valueOf(l)));
        String string2 = this.execute(Request.get(string));
        return BackupList.parse(string2);
    }

    public void update(long l, String string, String string2) throws RealmsServiceException {
        RealmsDescriptionDto realmsDescriptionDto = new RealmsDescriptionDto(string, string2);
        String string3 = this.url(WORLDS_RESOURCE_PATH + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(l)));
        this.execute(Request.post(string3, GSON.toJson(realmsDescriptionDto)));
    }

    public void updateSlot(long l, int n, RealmsWorldOptions realmsWorldOptions) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_SLOT.replace("$WORLD_ID", String.valueOf(l)).replace("$SLOT_ID", String.valueOf(n)));
        String string2 = realmsWorldOptions.toJson();
        this.execute(Request.post(string, string2));
    }

    public boolean switchSlot(long l, int n) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_SLOT.replace("$WORLD_ID", String.valueOf(l)).replace("$SLOT_ID", String.valueOf(n)));
        String string2 = this.execute(Request.put(string, ""));
        return Boolean.valueOf(string2);
    }

    public void restoreWorld(long l, String string) throws RealmsServiceException {
        String string2 = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_BACKUPS.replace("$WORLD_ID", String.valueOf(l)), "backupId=" + string);
        this.execute(Request.put(string2, "", 40000, 600000));
    }

    public WorldTemplatePaginatedList fetchWorldTemplates(int n, int n2, RealmsServer.WorldType worldType) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_TEMPLATES.replace("$WORLD_TYPE", worldType.toString()), String.format(Locale.ROOT, "page=%d&pageSize=%d", n, n2));
        String string2 = this.execute(Request.get(string));
        return WorldTemplatePaginatedList.parse(string2);
    }

    public Boolean putIntoMinigameMode(long l, String string) throws RealmsServiceException {
        String string2 = PATH_PUT_INTO_MINIGAMES_MODE.replace("$MINIGAME_ID", string).replace("$WORLD_ID", String.valueOf(l));
        String string3 = this.url(WORLDS_RESOURCE_PATH + string2);
        return Boolean.valueOf(this.execute(Request.put(string3, "")));
    }

    public Ops op(long l, UUID uUID) throws RealmsServiceException {
        String string = PATH_OP.replace("$WORLD_ID", String.valueOf(l)).replace("$PROFILE_UUID", UndashedUuid.toString((UUID)uUID));
        String string2 = this.url(OPS_RESOURCE + string);
        return Ops.parse(this.execute(Request.post(string2, "")));
    }

    public Ops deop(long l, UUID uUID) throws RealmsServiceException {
        String string = PATH_OP.replace("$WORLD_ID", String.valueOf(l)).replace("$PROFILE_UUID", UndashedUuid.toString((UUID)uUID));
        String string2 = this.url(OPS_RESOURCE + string);
        return Ops.parse(this.execute(Request.delete(string2)));
    }

    public Boolean open(long l) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_OPEN.replace("$WORLD_ID", String.valueOf(l)));
        String string2 = this.execute(Request.put(string, ""));
        return Boolean.valueOf(string2);
    }

    public Boolean close(long l) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_CLOSE.replace("$WORLD_ID", String.valueOf(l)));
        String string2 = this.execute(Request.put(string, ""));
        return Boolean.valueOf(string2);
    }

    public Boolean resetWorldWithTemplate(long l, String string) throws RealmsServiceException {
        RealmsWorldResetDto realmsWorldResetDto = new RealmsWorldResetDto(null, Long.valueOf(string), -1, false, Set.of());
        String string2 = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_RESET.replace("$WORLD_ID", String.valueOf(l)));
        String string3 = this.execute(Request.post(string2, GSON.toJson(realmsWorldResetDto), 30000, 80000));
        return Boolean.valueOf(string3);
    }

    public Subscription subscriptionFor(long l) throws RealmsServiceException {
        String string = this.url(SUBSCRIPTION_RESOURCE + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(l)));
        String string2 = this.execute(Request.get(string));
        return Subscription.parse(string2);
    }

    public int pendingInvitesCount() throws RealmsServiceException {
        return this.pendingInvites().pendingInvites.size();
    }

    public PendingInvitesList pendingInvites() throws RealmsServiceException {
        String string = this.url("invites/pending");
        String string2 = this.execute(Request.get(string));
        PendingInvitesList pendingInvitesList = PendingInvitesList.parse(string2);
        pendingInvitesList.pendingInvites.removeIf(this::isBlocked);
        return pendingInvitesList;
    }

    private boolean isBlocked(PendingInvite pendingInvite) {
        return this.minecraft.getPlayerSocialManager().isBlocked(pendingInvite.realmOwnerUuid);
    }

    public void acceptInvitation(String string) throws RealmsServiceException {
        String string2 = this.url(INVITES_RESOURCE_PATH + PATH_ACCEPT_INVITE.replace("$INVITATION_ID", string));
        this.execute(Request.put(string2, ""));
    }

    public WorldDownload requestDownloadInfo(long l, int n) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_DOWNLOAD.replace("$WORLD_ID", String.valueOf(l)).replace("$SLOT_ID", String.valueOf(n)));
        String string2 = this.execute(Request.get(string));
        return WorldDownload.parse(string2);
    }

    @Nullable
    public UploadInfo requestUploadInfo(long l) throws RealmsServiceException {
        String string;
        String string2 = this.url(WORLDS_RESOURCE_PATH + PATH_WORLD_UPLOAD.replace("$WORLD_ID", String.valueOf(l)));
        UploadInfo uploadInfo = UploadInfo.parse(this.execute(Request.put(string2, UploadInfo.createRequest(string = UploadTokenCache.get(l)))));
        if (uploadInfo != null) {
            UploadTokenCache.put(l, uploadInfo.getToken());
        }
        return uploadInfo;
    }

    public void rejectInvitation(String string) throws RealmsServiceException {
        String string2 = this.url(INVITES_RESOURCE_PATH + PATH_REJECT_INVITE.replace("$INVITATION_ID", string));
        this.execute(Request.put(string2, ""));
    }

    public void agreeToTos() throws RealmsServiceException {
        String string = this.url("mco/tos/agreed");
        this.execute(Request.post(string, ""));
    }

    public RealmsNews getNews() throws RealmsServiceException {
        String string = this.url("mco/v1/news");
        String string2 = this.execute(Request.get(string, 5000, 10000));
        return RealmsNews.parse(string2);
    }

    public void sendPingResults(PingResult pingResult) throws RealmsServiceException {
        String string = this.url(REGIONS_RESOURCE);
        this.execute(Request.post(string, GSON.toJson(pingResult)));
    }

    public Boolean trialAvailable() throws RealmsServiceException {
        String string = this.url(TRIALS_RESOURCE);
        String string2 = this.execute(Request.get(string));
        return Boolean.valueOf(string2);
    }

    public void deleteRealm(long l) throws RealmsServiceException {
        String string = this.url(WORLDS_RESOURCE_PATH + "/$WORLD_ID".replace("$WORLD_ID", String.valueOf(l)));
        this.execute(Request.delete(string));
    }

    private String url(String string) {
        return this.url(string, null);
    }

    private String url(String string, @Nullable String string2) {
        try {
            return new URI(RealmsClient.ENVIRONMENT.protocol, RealmsClient.ENVIRONMENT.baseUrl, "/" + string, string2, null).toASCIIString();
        }
        catch (URISyntaxException uRISyntaxException) {
            throw new IllegalArgumentException(string, uRISyntaxException);
        }
    }

    private String execute(Request<?> request) throws RealmsServiceException {
        request.cookie("sid", this.sessionId);
        request.cookie("user", this.username);
        request.cookie("version", SharedConstants.getCurrentVersion().getName());
        request.addSnapshotHeader(RealmsMainScreen.isSnapshot());
        try {
            int n = request.responseCode();
            if (n == 503 || n == 277) {
                int n2 = request.getRetryAfterHeader();
                throw new RetryCallException(n2, n);
            }
            String string = request.text();
            if (n < 200 || n >= 300) {
                if (n == 401) {
                    String string2 = request.getHeader("WWW-Authenticate");
                    LOGGER.info("Could not authorize you against Realms server: {}", (Object)string2);
                    throw new RealmsServiceException(new RealmsError.AuthenticationError(string2));
                }
                RealmsError realmsError = RealmsError.parse(n, string);
                throw new RealmsServiceException(realmsError);
            }
            return string;
        }
        catch (RealmsHttpException realmsHttpException) {
            throw new RealmsServiceException(RealmsError.CustomError.connectivityError(realmsHttpException));
        }
    }

    public static enum CompatibleVersionResponse {
        COMPATIBLE,
        OUTDATED,
        OTHER;

    }

    public static enum Environment {
        PRODUCTION("pc.realms.minecraft.net", "https"),
        STAGE("pc-stage.realms.minecraft.net", "https"),
        LOCAL("localhost:8080", "http");

        public final String baseUrl;
        public final String protocol;

        private Environment(String string2, String string3) {
            this.baseUrl = string2;
            this.protocol = string3;
        }

        public static Optional<Environment> byName(String string) {
            return switch (string.toLowerCase(Locale.ROOT)) {
                case "production" -> Optional.of(PRODUCTION);
                case "local" -> Optional.of(LOCAL);
                case "stage", "staging" -> Optional.of(STAGE);
                default -> Optional.empty();
            };
        }
    }
}

