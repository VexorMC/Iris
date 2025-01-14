/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package com.mojang.realmsclient.gui.screens;

import javax.annotation.Nullable;

public class UploadResult {
    public final int statusCode;
    @Nullable
    public final String errorMessage;

    UploadResult(int n, String string) {
        this.statusCode = n;
        this.errorMessage = string;
    }

    @Nullable
    public String getSimplifiedErrorMessage() {
        if (this.statusCode < 200 || this.statusCode >= 300) {
            if (this.statusCode == 400 && this.errorMessage != null) {
                return this.errorMessage;
            }
            return String.valueOf(this.statusCode);
        }
        return null;
    }

    public static class Builder {
        private int statusCode = -1;
        private String errorMessage;

        public Builder withStatusCode(int n) {
            this.statusCode = n;
            return this;
        }

        public Builder withErrorMessage(@Nullable String string) {
            this.errorMessage = string;
            return this;
        }

        public UploadResult build() {
            return new UploadResult(this.statusCode, this.errorMessage);
        }
    }
}

