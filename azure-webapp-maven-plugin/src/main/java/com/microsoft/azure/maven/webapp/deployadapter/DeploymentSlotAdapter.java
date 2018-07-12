/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.deployadapter;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.PublishingProfile;

import java.io.File;

public class DeploymentSlotAdapter implements IDeployTargetAdapter {
    private DeploymentSlot slot;

    public DeploymentSlotAdapter(DeploymentSlot slot) {
        this.slot = slot;
    }
    @Override
    public void warDeploy(File war, String contextPath) {
        slot.warDeploy(war, contextPath);
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return slot.getPublishingProfile();
    }
}
