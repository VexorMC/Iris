/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.hash.Hashing
 *  com.google.common.io.Files
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  org.apache.commons.compress.archivers.tar.TarArchiveInputStream
 *  org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.io.IOUtils
 *  org.apache.commons.io.output.CountingOutputStream
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.http.client.config.RequestConfig
 *  org.apache.http.client.methods.CloseableHttpResponse
 *  org.apache.http.client.methods.HttpGet
 *  org.apache.http.client.methods.HttpUriRequest
 *  org.apache.http.impl.client.CloseableHttpClient
 *  org.apache.http.impl.client.HttpClientBuilder
 *  org.slf4j.Logger
 */
package com.mojang.realmsclient.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsDefaultUncaughtExceptionHandler;
import com.mojang.realmsclient.gui.screens.RealmsDownloadLatestWorldScreen;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;

public class FileDownload {
    static final Logger LOGGER = LogUtils.getLogger();
    volatile boolean cancelled;
    volatile boolean finished;
    volatile boolean error;
    volatile boolean extracting;
    @Nullable
    private volatile File tempFile;
    volatile File resourcePackPath;
    @Nullable
    private volatile HttpGet request;
    @Nullable
    private Thread currentThread;
    private final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(120000).setConnectTimeout(120000).build();
    private static final String[] INVALID_FILE_NAMES = new String[]{"CON", "COM", "PRN", "AUX", "CLOCK$", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public long contentLength(String string) {
        CloseableHttpClient closeableHttpClient = null;
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet(string);
            closeableHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute((HttpUriRequest)httpGet);
            long l = Long.parseLong(closeableHttpResponse.getFirstHeader("Content-Length").getValue());
            return l;
        }
        catch (Throwable throwable) {
            LOGGER.error("Unable to get content length for download");
            long l = 0L;
            return l;
        }
        finally {
            if (httpGet != null) {
                httpGet.releaseConnection();
            }
            if (closeableHttpClient != null) {
                try {
                    closeableHttpClient.close();
                }
                catch (IOException iOException) {
                    LOGGER.error("Could not close http client", (Throwable)iOException);
                }
            }
        }
    }

    public void download(WorldDownload worldDownload, String string, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, LevelStorageSource levelStorageSource) {
        if (this.currentThread != null) {
            return;
        }
        this.currentThread = new Thread(() -> {
            CloseableHttpClient closeableHttpClient = null;
            try {
                this.tempFile = File.createTempFile("backup", ".tar.gz");
                this.request = new HttpGet(worldDownload.downloadLink);
                closeableHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(this.requestConfig).build();
                CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute((HttpUriRequest)this.request);
                downloadStatus.totalBytes = Long.parseLong(closeableHttpResponse.getFirstHeader("Content-Length").getValue());
                if (closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                    this.error = true;
                    this.request.abort();
                    return;
                }
                FileOutputStream fileOutputStream = new FileOutputStream(this.tempFile);
                ActionListener actionListener = new ProgressListener(string.trim(), this.tempFile, levelStorageSource, downloadStatus);
                DownloadCountingOutputStream downloadCountingOutputStream = new DownloadCountingOutputStream(fileOutputStream);
                downloadCountingOutputStream.setListener(actionListener);
                IOUtils.copy((InputStream)closeableHttpResponse.getEntity().getContent(), (OutputStream)((Object)downloadCountingOutputStream));
                return;
            }
            catch (Exception exception) {
                LOGGER.error("Caught exception while downloading: {}", (Object)exception.getMessage());
                this.error = true;
                return;
            }
            finally {
                block40: {
                    block41: {
                        CloseableHttpResponse closeableHttpResponse;
                        this.request.releaseConnection();
                        if (this.tempFile != null) {
                            this.tempFile.delete();
                        }
                        if (this.error) break block40;
                        if (worldDownload.resourcePackUrl.isEmpty() || worldDownload.resourcePackHash.isEmpty()) break block41;
                        try {
                            this.tempFile = File.createTempFile("resources", ".tar.gz");
                            this.request = new HttpGet(worldDownload.resourcePackUrl);
                            closeableHttpResponse = closeableHttpClient.execute((HttpUriRequest)this.request);
                            downloadStatus.totalBytes = Long.parseLong(closeableHttpResponse.getFirstHeader("Content-Length").getValue());
                            if (closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                                this.error = true;
                                this.request.abort();
                                return;
                            }
                        }
                        catch (Exception exception) {
                            LOGGER.error("Caught exception while downloading: {}", (Object)exception.getMessage());
                            this.error = true;
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(this.tempFile);
                        ResourcePackProgressListener resourcePackProgressListener = new ResourcePackProgressListener(this.tempFile, downloadStatus, worldDownload);
                        DownloadCountingOutputStream downloadCountingOutputStream = new DownloadCountingOutputStream(fileOutputStream);
                        downloadCountingOutputStream.setListener(resourcePackProgressListener);
                        IOUtils.copy((InputStream)closeableHttpResponse.getEntity().getContent(), (OutputStream)((Object)downloadCountingOutputStream));
                        break block40;
                        finally {
                            this.request.releaseConnection();
                            if (this.tempFile != null) {
                                this.tempFile.delete();
                            }
                        }
                    }
                    this.finished = true;
                }
                if (closeableHttpClient != null) {
                    try {
                        closeableHttpClient.close();
                    }
                    catch (IOException iOException) {
                        LOGGER.error("Failed to close Realms download client");
                    }
                }
            }
        });
        this.currentThread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
        this.currentThread.start();
    }

    public void cancel() {
        if (this.request != null) {
            this.request.abort();
        }
        if (this.tempFile != null) {
            this.tempFile.delete();
        }
        this.cancelled = true;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isError() {
        return this.error;
    }

    public boolean isExtracting() {
        return this.extracting;
    }

    public static String findAvailableFolderName(String object) {
        object = ((String)object).replaceAll("[\\./\"]", "_");
        for (String string : INVALID_FILE_NAMES) {
            if (!((String)object).equalsIgnoreCase(string)) continue;
            object = "_" + (String)object + "_";
        }
        return object;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void untarGzipArchive(String string, @Nullable File file, LevelStorageSource levelStorageSource) throws IOException {
        Object object;
        Pattern pattern = Pattern.compile(".*-([0-9]+)$");
        int n = 1;
        for (char c : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
            string = string.replace(c, '_');
        }
        if (StringUtils.isEmpty((CharSequence)string)) {
            string = "Realm";
        }
        string = FileDownload.findAvailableFolderName(string);
        try {
            Object object2 = levelStorageSource.findLevelCandidates().iterator();
            while (object2.hasNext()) {
                LevelStorageSource.LevelDirectory levelDirectory = (LevelStorageSource.LevelDirectory)object2.next();
                String string2 = levelDirectory.directoryName();
                if (!string2.toLowerCase(Locale.ROOT).startsWith(string.toLowerCase(Locale.ROOT))) continue;
                Matcher matcher = pattern.matcher(string2);
                if (matcher.matches()) {
                    int n2 = Integer.parseInt(matcher.group(1));
                    if (n2 <= n) continue;
                    n = n2;
                    continue;
                }
                ++n;
            }
        }
        catch (Exception exception) {
            LOGGER.error("Error getting level list", (Throwable)exception);
            this.error = true;
            return;
        }
        if (!levelStorageSource.isNewLevelIdAcceptable(string) || n > 1) {
            object = string + (String)(n == 1 ? "" : "-" + n);
            if (!levelStorageSource.isNewLevelIdAcceptable((String)object)) {
                boolean bl = false;
                while (!bl) {
                    if (!levelStorageSource.isNewLevelIdAcceptable((String)(object = string + (String)(++n == 1 ? "" : "-" + n)))) continue;
                    bl = true;
                }
            }
        } else {
            object = string;
        }
        TarArchiveInputStream tarArchiveInputStream = null;
        File file2 = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "saves");
        try {
            file2.mkdir();
            tarArchiveInputStream = new TarArchiveInputStream((InputStream)new GzipCompressorInputStream((InputStream)new BufferedInputStream(new FileInputStream(file))));
            Object object3 = tarArchiveInputStream.getNextTarEntry();
            while (object3 != null) {
                File file3 = new File(file2, object3.getName().replace("world", (CharSequence)object));
                if (object3.isDirectory()) {
                    file3.mkdirs();
                } else {
                    file3.createNewFile();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file3);){
                        IOUtils.copy((InputStream)tarArchiveInputStream, (OutputStream)fileOutputStream);
                    }
                }
                object3 = tarArchiveInputStream.getNextTarEntry();
            }
        }
        catch (Exception exception) {
            LOGGER.error("Error extracting world", (Throwable)exception);
            this.error = true;
        }
        finally {
            if (tarArchiveInputStream != null) {
                tarArchiveInputStream.close();
            }
            if (file != null) {
                file.delete();
            }
            try (LevelStorageSource.LevelStorageAccess levelStorageAccess = levelStorageSource.validateAndCreateAccess((String)object);){
                levelStorageAccess.renameAndDropPlayer((String)object);
            }
            catch (IOException | NbtException | ReportedNbtException exception) {
                LOGGER.error("Failed to modify unpacked realms level {}", object, (Object)exception);
            }
            catch (ContentValidationException contentValidationException) {
                LOGGER.warn("{}", (Object)contentValidationException.getMessage());
            }
            this.resourcePackPath = new File(file2, (String)object + File.separator + "resources.zip");
        }
    }

    class ResourcePackProgressListener
    implements ActionListener {
        private final File tempFile;
        private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;
        private final WorldDownload worldDownload;

        ResourcePackProgressListener(File file, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus, WorldDownload worldDownload) {
            this.tempFile = file;
            this.downloadStatus = downloadStatus;
            this.worldDownload = worldDownload;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            this.downloadStatus.bytesWritten = ((DownloadCountingOutputStream)((Object)actionEvent.getSource())).getByteCount();
            if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled) {
                try {
                    String string = Hashing.sha1().hashBytes(Files.toByteArray((File)this.tempFile)).toString();
                    if (string.equals(this.worldDownload.resourcePackHash)) {
                        FileUtils.copyFile((File)this.tempFile, (File)FileDownload.this.resourcePackPath);
                        FileDownload.this.finished = true;
                    } else {
                        LOGGER.error("Resourcepack had wrong hash (expected {}, found {}). Deleting it.", (Object)this.worldDownload.resourcePackHash, (Object)string);
                        FileUtils.deleteQuietly((File)this.tempFile);
                        FileDownload.this.error = true;
                    }
                }
                catch (IOException iOException) {
                    LOGGER.error("Error copying resourcepack file: {}", (Object)iOException.getMessage());
                    FileDownload.this.error = true;
                }
            }
        }
    }

    static class DownloadCountingOutputStream
    extends CountingOutputStream {
        @Nullable
        private ActionListener listener;

        public DownloadCountingOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        public void setListener(ActionListener actionListener) {
            this.listener = actionListener;
        }

        protected void afterWrite(int n) throws IOException {
            super.afterWrite(n);
            if (this.listener != null) {
                this.listener.actionPerformed(new ActionEvent((Object)this, 0, null));
            }
        }
    }

    class ProgressListener
    implements ActionListener {
        private final String worldName;
        private final File tempFile;
        private final LevelStorageSource levelStorageSource;
        private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;

        ProgressListener(String string, File file, LevelStorageSource levelStorageSource, RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus) {
            this.worldName = string;
            this.tempFile = file;
            this.levelStorageSource = levelStorageSource;
            this.downloadStatus = downloadStatus;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            this.downloadStatus.bytesWritten = ((DownloadCountingOutputStream)((Object)actionEvent.getSource())).getByteCount();
            if (this.downloadStatus.bytesWritten >= this.downloadStatus.totalBytes && !FileDownload.this.cancelled && !FileDownload.this.error) {
                try {
                    FileDownload.this.extracting = true;
                    FileDownload.this.untarGzipArchive(this.worldName, this.tempFile, this.levelStorageSource);
                }
                catch (IOException iOException) {
                    LOGGER.error("Error extracting archive", (Throwable)iOException);
                    FileDownload.this.error = true;
                }
            }
        }
    }
}

