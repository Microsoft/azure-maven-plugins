/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.microsoft.azure.maven.appservice.OperatingSystemEnum;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

public class RuntimeConfiguration {

    public static final OperatingSystemEnum DEFAULT_OS = OperatingSystemEnum.Windows;

    protected String os;
    protected String image;
    protected String serverId;
    protected String registryUrl;

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public OperatingSystemEnum getOperationSystemEnum() throws MojoExecutionException {
        return StringUtils.isEmpty(os) ? DEFAULT_OS : OperatingSystemEnum.fromString(os);
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }
}
