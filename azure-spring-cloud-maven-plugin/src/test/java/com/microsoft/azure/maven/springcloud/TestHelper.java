/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TestHelper {
    public static Model readMavenModel(File pomFile) throws IOException {
        final ModelReader reader = new DefaultModelReader();
        return reader.read(pomFile, Collections.emptyMap());
    }
}
