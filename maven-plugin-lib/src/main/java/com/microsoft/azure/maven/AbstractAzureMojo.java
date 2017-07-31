/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.auth.AuthConfiguration;
import com.microsoft.azure.maven.auth.AzureAuthHelper;
import com.microsoft.azure.maven.telemetry.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import static com.microsoft.azure.maven.telemetry.AppInsightsProxy.*;

/**
 * Base abstract class for shared configurations and operations.
 */
public abstract class AbstractAzureMojo extends AbstractMojo
        implements TelemetryConfiguration, AuthConfiguration {
    public static final String AZURE_INIT_FAIL = "Failed to initialize Azure client object.";
    public static final String FAILURE_REASON = "failureReason";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    protected File buildDirectory;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor plugin;

    /**
     * The system settings for Maven. This is the instance resulting from
     * merging global and user-level settings files.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    @Component(role = MavenResourcesFiltering.class, hint = "default")
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Parameter
    protected AuthenticationSetting authentication;

    @Parameter
    protected String subscriptionId = "";

    @Parameter(property = "allowTelemetry", defaultValue = "true")
    protected boolean allowTelemetry;

    @Parameter(property = "failsOnError", defaultValue = "true")
    protected boolean failsOnError;

    private Azure azure;

    private TelemetryProxy telemetryProxy;

    private String sessionId = UUID.randomUUID().toString();

    private String installationId = GetHashMac.getHashMac();

    public MavenProject getProject() {
        return project;
    }

    public MavenSession getSession() {
        return session;
    }

    public String getBuildDirectoryAbsolutePath() {
        return buildDirectory.getAbsolutePath();
    }

    public MavenResourcesFiltering getMavenResourcesFiltering() {
        return mavenResourcesFiltering;
    }

    public Settings getSettings() {
        return settings;
    }

    public AuthenticationSetting getAuthenticationSetting() {
        return authentication;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public boolean isTelemetryAllowed() {
        return allowTelemetry;
    }

    public boolean isFailingOnError() {
        return failsOnError;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInstallationId() {
        return installationId;
    }

    public String getPluginName() {
        return plugin.getArtifactId();
    }

    public String getPluginVersion() {
        return plugin.getVersion();
    }

    public String getUserAgent() {
        return String.format("%s/%s %s:%s %s:%s",
                getPluginName(), getPluginVersion(),
                INSTALLATION_ID_KEY, getInstallationId(),
                SESSION_ID_KEY, getSessionId());
    }

    public Azure getAzureClient() {
        if (azure == null) {
            initAzureClient();
        }
        return azure;
    }

    protected void initAzureClient() {
        azure = new AzureAuthHelper(this).getAzureClient();
    }

    public TelemetryProxy getTelemetryProxy() {
        if (telemetryProxy == null) {
            initTelemetry();
        }
        return telemetryProxy;
    }

    protected void initTelemetry() {
        telemetryProxy = new AppInsightsProxy(this);
        if (!isTelemetryAllowed()) {
            telemetryProxy.trackEvent(TelemetryEvent.TELEMETRY_NOT_ALLOWED);
            telemetryProxy.disable();
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (getAzureClient() == null) {
                getTelemetryProxy().trackEvent(TelemetryEvent.INIT_FAILURE);
                throw new MojoExecutionException(AZURE_INIT_FAIL);
            } else {
                // Repopulate subscriptionId in case it is not configured.
                getTelemetryProxy().addDefaultProperty(SUBSCRIPTION_ID_KEY, getAzureClient().subscriptionId());
            }

            trackMojoStart();

            doExecute();

            trackMojoSuccess();
        } catch (Exception e) {
            processException(e);
        }
    }

    /**
     * Sub-class should implement this method to do real work.
     *
     * @throws Exception
     */
    protected abstract void doExecute() throws Exception;

    protected void trackMojoStart() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".start");
    }

    protected void trackMojoSuccess() {
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".success");
    }

    protected void trackMojoFailure(final String message) {
        final HashMap<String, String> failureReason = new HashMap<>();
        failureReason.put(FAILURE_REASON, message);
        getTelemetryProxy().trackEvent(this.getClass().getSimpleName() + ".failure");
    }

    protected void processException(final Exception exception) throws MojoExecutionException {
        final String message = exception.getMessage();
        if (StringUtils.isEmpty(message)) {
            trackMojoFailure(exception.toString());
        } else {
            trackMojoFailure(message);
        }

        if (isFailingOnError()) {
            throw new MojoExecutionException(message, exception);
        } else {
            getLog().error(message);
        }
    }
}
