/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.handlers;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.WebAppUtils;

public class JavaRuntimeHandlerImpl implements RuntimeHandler {
    private AbstractWebAppMojo mojo;

    public JavaRuntimeHandlerImpl(final AbstractWebAppMojo mojo) {
        this.mojo = mojo;
    }

    @Override
    public WebApp.DefinitionStages.WithCreate defineAppWithRunTime() throws Exception {
        final WebApp.DefinitionStages.WithCreate withCreate = WebAppUtils.defineApp(mojo)
                .withNewWindowsPlan(mojo.getPricingTier());

        withCreate.withJavaVersion(mojo.getJavaVersion())
                .withWebContainer(mojo.getJavaWebContainer());
        return withCreate;
    }

    @Override
    public WebApp.Update updateAppRuntime() throws Exception {
        final WebApp app = mojo.getWebApp();
        WebAppUtils.assureWindowsWebApp(app);
        WebAppUtils.clearTags(app);

        final WebApp.Update update = app.update();
        update.withJavaVersion(mojo.getJavaVersion())
                .withWebContainer(mojo.getJavaWebContainer());
        return update;
    }
}
