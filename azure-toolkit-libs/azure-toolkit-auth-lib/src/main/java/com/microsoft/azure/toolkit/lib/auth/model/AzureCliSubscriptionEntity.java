/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;

import com.azure.core.management.AzureEnvironment;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AzureCliSubscriptionEntity extends SubscriptionEntity {
    private AzureEnvironment environment;
    private String email;
}
