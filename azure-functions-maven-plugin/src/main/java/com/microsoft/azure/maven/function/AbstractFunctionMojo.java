/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.ElasticPremiumPricingTier;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.maven.AbstractAppServiceMojo;
import com.microsoft.azure.maven.auth.AzureAuthFailureException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

public abstract class AbstractFunctionMojo extends AbstractAppServiceMojo {

    private static final String FUNCTION_JAVA_VERSION_KEY = "functionJavaVersion";
    private static final String DISABLE_APP_INSIGHTS_KEY = "disableAppInsights";

    //region Properties
    /**
     * App Service pricing tier, which will only be used to create Functions App at the first time.<p>
     * Below is the list of supported pricing tier:
     * <ul>
     *     <li>F1</li>
     *     <li>D1</li>
     *     <li>B1</li>
     *     <li>B2</li>
     *     <li>B3</li>
     *     <li>S1</li>
     *     <li>S2</li>
     *     <li>S3</li>
     *     <li>P1V2</li>
     *     <li>P2V2</li>
     *     <li>P3V2</li>
     * </ul>
     */
    @Parameter(property = "functions.pricingTier")
    protected String pricingTier;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
    protected String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    /**
     * Skip execution.
     *
     * @since 0.1.0
     */
    @Parameter(property = "functions.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * App Service region, which will only be used to create App Service at the first time.
     */
    @Parameter(property = "functions.region", defaultValue = "westeurope")
    protected String region;

    @Parameter(property = "functions.runtime")
    protected RuntimeConfiguration runtime;

    @Parameter(property = "functions.appInsightsInstance")
    protected String appInsightsInstance;

    @Parameter(property = "functions.appInsightsKey")
    protected String appInsightsKey;

    @Parameter(property = "functions.disableAppInsights", defaultValue = "false")
    protected boolean disableAppInsights;

    protected Map fixedAppSettings;
    //endregion

    //region Getter

    public PricingTier getPricingTier() throws AzureExecutionException {
        if (StringUtils.isEmpty(pricingTier)) {
            return null;
        }
        final ElasticPremiumPricingTier elasticPremiumPricingTier = ElasticPremiumPricingTier.fromString(pricingTier);
        return elasticPremiumPricingTier != null ? elasticPremiumPricingTier.toPricingTier()
                : AppServiceUtils.getPricingTierFromString(pricingTier);
    }

    public String getRegion() {
        return region;
    }

    @Override
    protected boolean isSkipMojo() {
        return skip;
    }

    public String getFinalName() {
        return finalName;
    }

    public String getAppInsightsInstance() {
        return appInsightsInstance;
    }

    public String getAppInsightsKey() {
        return appInsightsKey;
    }

    public boolean isDisableAppInsights() {
        return disableAppInsights;
    }

    @Nullable
    public FunctionApp getFunctionApp() throws AzureAuthFailureException, AzureExecutionException {
        return getAzureClient().appServices().functionApps().getByResourceGroup(getResourceGroup(), getAppName());
    }

    public RuntimeConfiguration getRuntime() {
        return runtime;
    }

    @Override
    public Map<String, String> getTelemetryProperties() {
        final Map<String, String> result = super.getTelemetryProperties();
        final String javaVersion = runtime == null ? null : runtime.getJavaVersion();
        result.put(FUNCTION_JAVA_VERSION_KEY, StringUtils.isEmpty(javaVersion) ? "" : javaVersion);
        result.put(DISABLE_APP_INSIGHTS_KEY, String.valueOf(isDisableAppInsights()));
        return result;
    }
    //endregion
}
