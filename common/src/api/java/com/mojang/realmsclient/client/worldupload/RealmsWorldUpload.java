/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.FileUpload;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.worldupload.RealmsUploadCanceledException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadFailedException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadWorldNotClosedException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadWorldPacker;
import com.mojang.realmsclient.client.worldupload.RealmsWorldUploadStatusTracker;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.UploadResult;
import com.mojang.realmsclient.util.UploadTokenCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.User;
import org.slf4j.Logger;

public class RealmsWorldUpload {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int UPLOAD_RETRIES = 20;
    private final RealmsClient client = RealmsClient.create();
    private final Path worldFolder;
    private final RealmsWorldOptions worldOptions;
    private final User user;
    private final long realmId;
    private final int slotId;
    private final RealmsWorldUploadStatusTracker statusCallback;
    private volatile boolean cancelled;
    @Nullable
    private FileUpload uploadTask;

    public RealmsWorldUpload(Path path, RealmsWorldOptions realmsWorldOptions, User user, long l, int n, RealmsWorldUploadStatusTracker realmsWorldUploadStatusTracker) {
        this.worldFolder = path;
        this.worldOptions = realmsWorldOptions;
        this.user = user;
        this.realmId = l;
        this.slotId = n;
        this.statusCallback = realmsWorldUploadStatusTracker;
    }

    public CompletableFuture<?> packAndUpload() {
        return CompletableFuture.runAsync(() -> {
            File file = null;
            try {
                FileUpload fileUpload;
                UploadInfo uploadInfo = this.requestUploadInfoWithRetries();
                file = RealmsUploadWorldPacker.pack(this.worldFolder, () -> this.cancelled);
                this.statusCallback.setUploading();
                this.uploadTask = fileUpload = new FileUpload(file, this.realmId, this.slotId, uploadInfo, this.user, SharedConstants.getCurrentVersion().getName(), this.worldOptions.version, this.statusCallback.getUploadStatus());
                UploadResult uploadResult = fileUpload.upload();
                String string = uploadResult.getSimplifiedErrorMessage();
                if (string != null) {
                    throw new RealmsUploadFailedException(string);
                }
                UploadTokenCache.invalidate(this.realmId);
                this.client.updateSlot(this.realmId, this.slotId, this.worldOptions);
            }
            catch (IOException iOException) {
                throw new RealmsUploadFailedException(iOException.getMessage());
            }
            catch (RealmsServiceException realmsServiceException) {
                throw new RealmsUploadFailedException(realmsServiceException.realmsError.errorMessage());
            }
            catch (InterruptedException | CancellationException exception) {
                throw new RealmsUploadCanceledException();
            }
            finally {
                if (file != null) {
                    LOGGER.debug("Deleting file {}", (Object)file.getAbsolutePath());
                    file.delete();
                }
            }
        }, Util.backgroundExecutor());
    }

    public void cancel() {
        this.cancelled = true;
        if (this.uploadTask != null) {
            this.uploadTask.cancel();
            this.uploadTask = null;
        }
    }

    private UploadInfo requestUploadInfoWithRetries() throws RealmsServiceException, InterruptedException {
        for (int i = 0; i < 20; ++i) {
            try {
                UploadInfo uploadInfo = this.client.requestUploadInfo(this.realmId);
                if (this.cancelled) {
                    throw new RealmsUploadCanceledException();
                }
                if (uploadInfo == null) continue;
                if (!uploadInfo.isWorldClosed()) {
                    throw new RealmsUploadWorldNotClosedException();
                }
                return uploadInfo;
            }
            catch (RetryCallException retryCallException) {
                Thread.sleep((long)retryCallException.delaySeconds * 1000L);
            }
        }
        throw new RealmsUploadWorldNotClosedException();
    }
}

