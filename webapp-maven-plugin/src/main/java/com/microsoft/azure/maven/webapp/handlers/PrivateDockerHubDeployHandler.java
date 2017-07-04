/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.webapp.configuration.ContainerSetting;
import com.microsoft.azure.maven.webapp.DeployMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;

/**
 * Deployment handler for private Docker Hub image
 */
public class PrivateDockerHubDeployHandler extends ContainerDeployHandler {
    /**
     * Constructor
     *
     * @param mojo
     */
    public PrivateDockerHubDeployHandler(final DeployMojo mojo) {
        super(mojo);
    }

    /**
     * Create or update web app
     *
     * @param app
     * @throws Exception
     */
    @Override
    public void deploy(final WebApp app) throws Exception {
        final ContainerSetting containerSetting = mojo.getContainerSettings();
        final Server server = Utils.getServer(mojo.getSettings(), containerSetting.getServerId());
        if (server == null) {
            throw new MojoExecutionException(SERVER_ID_NOT_FOUND + containerSetting.getServerId());
        }

        if (app == null) {
            defineApp()
                    .withPrivateDockerHubImage(containerSetting.getImageName())
                    .withCredentials(server.getUsername(), server.getPassword())
                    .withStartUpCommand(containerSetting.getStartUpFile())
                    .withAppSettings(mojo.getAppSettings())
                    .create();
        } else {
            updateApp(app)
                    .withPrivateDockerHubImage(containerSetting.getImageName())
                    .withCredentials(server.getUsername(), server.getPassword())
                    .withStartUpCommand(containerSetting.getStartUpFile())
                    .withAppSettings(mojo.getAppSettings())
                    .apply();
        }
    }
}
