/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.Objects;

public class AzureOperationBundle {

    @NonNls
    public static final String TITLES = "com.microsoft.azure.toolkit.operation.titles";
    private static Provider provider;

    public static synchronized void register(final Provider provider) {
        if (AzureOperationBundle.provider == null) {
            AzureOperationBundle.provider = provider;
        }
    }

    public static IAzureOperationTitle title(@NotNull @PropertyKey(resourceBundle = TITLES) String name, @NotNull Object... params) {
        return MessageBundleBasedOperationTitle.builder().name(name).params(params).build();
    }

    @Builder
    @Getter
    public static class MessageBundleBasedOperationTitle implements IAzureOperationTitle {
        private final String name;
        private final Object[] params;
        private String title;

        public String toString() {
            if (Objects.isNull(this.title)) {
                this.title = provider.getMessage(this.name, params);
            }
            return this.title;
        }
    }

    public interface Provider {
        String getMessage(@NotNull String key, @NotNull Object... params);
    }
}