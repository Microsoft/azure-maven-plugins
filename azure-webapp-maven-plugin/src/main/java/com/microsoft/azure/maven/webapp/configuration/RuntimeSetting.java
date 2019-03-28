/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import com.microsoft.azure.maven.webapp.utils.RuntimeStackUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Runtime Setting
 */
public class RuntimeSetting {

    protected String os;
    protected String javaVersion;
    protected String webContainer;
    protected String image;
    protected String serverId;
    protected String registryUrl;

    public static final String RUNTIME_CONFIG_REFERENCE = "https://aka.ms/maven_webapp_runtime";

    public String getOs() {
        return this.os;
    }

    public JavaVersion getJavaVersion() {
        return StringUtils.isEmpty(javaVersion) ? null : JavaVersion.fromString(javaVersion);
    }

    public RuntimeStack getLinuxRuntime() throws MojoExecutionException {
        // todo: add unit tests
        final RuntimeStack result = RuntimeStackUtils.getRuntimeStack(javaVersion, webContainer);
        if (result == null) {
            throw new MojoExecutionException(String.format("Unsupported values for linux runtime, please refer %s " +
                "more information", RUNTIME_CONFIG_REFERENCE));
        }
        return result;
    }

    public WebContainer getWebContainer() {
        if (StringUtils.isEmpty(webContainer)) {
            return WebContainer.TOMCAT_8_5_NEWEST;
        }
        return WebContainer.fromString(webContainer);
    }

    public String getImage() {
        return this.image;
    }

    public String getServerId() {
        return this.serverId;
    }

    public String getRegistryUrl() {
        return this.registryUrl;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(this.os) && StringUtils.isEmpty(this.javaVersion) &&
            StringUtils.isEmpty(this.webContainer) && StringUtils.isEmpty(image) &&
            StringUtils.isEmpty(this.serverId) && StringUtils.isEmpty(this.registryUrl);
    }
}
