/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import org.apache.commons.lang3.StringUtils;

/**
 * Runtime Setting
 */
public class MavenRuntimeConfig {

    /**
     * OS of Web App Below is the list of supported JVM versions:
     * <ul>
     * <li>Windows</li>
     * <li>Linux</li>
     * <li>Docker</li>
     * </ul>
     */
    protected String os;

    /**
     * Java version of Web App Below is the list of supported Java versions:
     * <ul>
     * <li>Java 8</li>
     * <li>Java 11</li>
     * </ul>
     */
    protected String javaVersion;

    /**
     * Web container type and version within Web App. Below is the list of supported
     * web container types(JBoss is only supported on java 8 and linux webapps):
     * <ul>
     * <li>Java SE</li>
     * <li>Tomcat 7.0</li>
     * <li>Tomcat 8.5</li>
     * <li>Tomcat 9.0</li>
     * <li>JBoss EAP 7.2</li>
     * </ul>
     */
    protected String webContainer;

    /**
     * Settings of docker image name within Web App. This only applies to Docker Web
     * App.
     */
    protected String image;

    /**
     * Settings of credentials to access docker image. Use it when you are using
     * private Docker Hub
     */
    protected String serverId;

    /**
     * Settings of specifies your docker image registry URL. Use it when you are
     * using private registry.
     */
    protected String registryUrl;

    public static final String RUNTIME_CONFIG_REFERENCE = "https://aka.ms/maven_webapp_runtime";

    public String getOs() {
        return this.os;
    }

    public JavaVersion getJavaVersion() {
        return toJavaVersion(this.javaVersion);
    }

    public WebContainer getWebContainer() {
        if (!checkWebContainer(webContainer)) {
            return null;
        }
        return WebContainer.fromString(webContainer);
    }

    public String getWebContainerRaw() {
        return webContainer;
    }

    public String getJavaVersionRaw() {
        return javaVersion;
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

    protected boolean checkWebContainer(String value) {
        return StringUtils.isNotBlank(value) && (
                WebContainer.fromString(value) != WebContainer.JAVA_OFF);
    }

    private static JavaVersion toJavaVersion(String javaVersion) {
        if (StringUtils.isEmpty(javaVersion)) {
            return null;
        }
        final JavaVersion newJavaVersion = JavaVersion.fromString(javaVersion);
        if (newJavaVersion == JavaVersion.OFF) {
            throw new AzureToolkitRuntimeException(String.format("Cannot parse java version: '%s'.", javaVersion));
        }
        return newJavaVersion;
    }

}
