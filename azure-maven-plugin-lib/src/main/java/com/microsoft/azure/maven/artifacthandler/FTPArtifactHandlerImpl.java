/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.artifacthandler;

import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.FTPUploader;
import com.microsoft.azure.maven.Utils;
import com.microsoft.azure.maven.deploytarget.DeployTarget;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class FTPArtifactHandlerImpl<T extends AbstractAppServiceMojo> implements ArtifactHandler {
    private static final String DEFAULT_WEBAPP_ROOT = "/site/wwwroot";
    private static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private static final String NO_RESOURCES_CONFIG = "No resources specified in pom.xml. Skip artifacts deployment.";
    private static final String WEBAPP_PLUGIN_NAME = "azure-webapp-maven-plugin";
    private static final String MAVEN_PLUGIN_POSTFIX = "-maven-plugin";

    protected T mojo;

    public FTPArtifactHandlerImpl(final T mojo) {
        this.mojo = mojo;
    }

    protected String getDeploymentStageDirectory() {
        final String outputFolder = this.mojo.getPluginName().replaceAll(MAVEN_PLUGIN_POSTFIX, "");
        return Paths.get(mojo.getBuildDirectoryAbsolutePath(), outputFolder, this.mojo.getAppName()).toString();
    }

    @Override
    public void publish(DeployTarget target) throws IOException, MojoExecutionException {
        final FTPUploader uploader = new FTPUploader(mojo.getLog());
        final PublishingProfile profile = target.getPublishingProfile();
        final String serverUrl = profile.ftpUrl().split("/", 2)[0];

        if (this.mojo.getPluginName().equalsIgnoreCase(WEBAPP_PLUGIN_NAME)) {
            final List<Resource> resources = this.mojo.getResources();
            if (resources == null || resources.isEmpty()) {
                mojo.getLog().info(NO_RESOURCES_CONFIG);
                return;
            }
            Utils.copyResources(mojo.getProject(),
                mojo.getSession(),
                mojo.getMavenResourcesFiltering(),
                resources,
                getDeploymentStageDirectory());
        }

        uploader.uploadDirectoryWithRetries(serverUrl,
            profile.ftpUsername(),
            profile.ftpPassword(),
            getDeploymentStageDirectory(),
            DEFAULT_WEBAPP_ROOT,
            DEFAULT_MAX_RETRY_TIMES);
    }
}
