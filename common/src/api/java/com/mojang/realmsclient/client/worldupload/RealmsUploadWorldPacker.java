/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.compress.archivers.tar.TarArchiveEntry
 *  org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
 */
package com.mojang.realmsclient.client.worldupload;

import com.mojang.realmsclient.client.worldupload.RealmsUploadCanceledException;
import com.mojang.realmsclient.client.worldupload.RealmsUploadTooLargeException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class RealmsUploadWorldPacker {
    private static final long SIZE_LIMIT = 0x140000000L;
    private static final String WORLD_FOLDER_NAME = "world";
    private final BooleanSupplier isCanceled;
    private final Path directoryToPack;

    public static File pack(Path path, BooleanSupplier booleanSupplier) throws IOException {
        return new RealmsUploadWorldPacker(path, booleanSupplier).tarGzipArchive();
    }

    private RealmsUploadWorldPacker(Path path, BooleanSupplier booleanSupplier) {
        this.isCanceled = booleanSupplier;
        this.directoryToPack = path;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private File tarGzipArchive() throws IOException {
        try (TarArchiveOutputStream tarArchiveOutputStream = null;){
            File file = File.createTempFile("realms-upload-file", ".tar.gz");
            tarArchiveOutputStream = new TarArchiveOutputStream((OutputStream)new GZIPOutputStream(new FileOutputStream(file)));
            tarArchiveOutputStream.setLongFileMode(3);
            this.addFileToTarGz(tarArchiveOutputStream, this.directoryToPack, WORLD_FOLDER_NAME, true);
            if (this.isCanceled.getAsBoolean()) {
                throw new RealmsUploadCanceledException();
            }
            tarArchiveOutputStream.finish();
            this.verifyBelowSizeLimit(file.length());
            File file2 = file;
            return file2;
        }
    }

    private void addFileToTarGz(TarArchiveOutputStream tarArchiveOutputStream, Path path, String string, boolean bl) throws IOException {
        if (this.isCanceled.getAsBoolean()) {
            throw new RealmsUploadCanceledException();
        }
        this.verifyBelowSizeLimit(tarArchiveOutputStream.getBytesWritten());
        File file = path.toFile();
        String string2 = bl ? string : string + file.getName();
        TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file, string2);
        tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
        if (file.isFile()) {
            try (FileInputStream fileInputStream = new FileInputStream(file);){
                ((InputStream)fileInputStream).transferTo((OutputStream)tarArchiveOutputStream);
            }
            tarArchiveOutputStream.closeArchiveEntry();
        } else {
            tarArchiveOutputStream.closeArchiveEntry();
            File[] fileArray = file.listFiles();
            if (fileArray != null) {
                for (File file2 : fileArray) {
                    this.addFileToTarGz(tarArchiveOutputStream, file2.toPath(), string2 + "/", false);
                }
            }
        }
    }

    private void verifyBelowSizeLimit(long l) {
        if (l > 0x140000000L) {
            throw new RealmsUploadTooLargeException(0x140000000L);
        }
    }
}

