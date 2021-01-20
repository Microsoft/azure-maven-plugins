/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service;


public interface IAppServicePlanCreator {
    IAppServicePlanCreator withResourceGroup(String name);

    IAppServicePlan create();
}
