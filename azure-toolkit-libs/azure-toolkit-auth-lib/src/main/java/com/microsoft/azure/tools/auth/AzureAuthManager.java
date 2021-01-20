/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth;

import com.google.common.base.MoreObjects;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.tools.auth.core.ChainedCredentialRetriever;
import com.microsoft.azure.tools.auth.core.ICredentialRetriever;
import com.microsoft.azure.tools.auth.core.azurecli.AzureCliCredentialRetriever;
import com.microsoft.azure.tools.auth.core.devicecode.DeviceCodeCredentialRetriever;
import com.microsoft.azure.tools.auth.core.managedidentity.ManagedIdentityCredentialRetriever;
import com.microsoft.azure.tools.auth.core.oauth.OAuthCredentialRetriever;
import com.microsoft.azure.tools.auth.core.serviceprincipal.ServicePrincipalCredentialRetriever;
import com.microsoft.azure.tools.auth.core.visualstudio.VisualStudioCredentialRetriever;
import com.microsoft.azure.tools.auth.core.vscode.VisualStudioCodeCredentialRetriever;
import com.microsoft.azure.tools.auth.exception.LoginFailureException;
import com.microsoft.azure.tools.auth.model.AuthConfiguration;
import com.microsoft.azure.tools.auth.model.AuthType;
import com.microsoft.azure.tools.auth.model.AzureCredentialWrapper;
import rx.Single;

import java.util.LinkedHashMap;
import java.util.Map;

public class AzureAuthManager {
    public static Single<AzureCredentialWrapper> getAzureCredentialWrapper(AuthConfiguration configuration) {
        AuthConfiguration auth = MoreObjects.firstNonNull(configuration, new AuthConfiguration());
        AuthHelper.setupAzureEnvironment(auth.getEnvironment());
        ChainedCredentialRetriever chainedCredentialRetriever = new ChainedCredentialRetriever();
        AuthType authType = MoreObjects.firstNonNull(auth.getType(), AuthType.AUTO);
        Map<AuthType, ICredentialRetriever> allRetrievers = buildCredentialRetrievers(configuration);
        if (authType.equals(AuthType.AUTO)) {
            for (ICredentialRetriever retriever : allRetrievers.values()) {
                chainedCredentialRetriever.addRetriever(retriever);
            }
        } else {
            // for specific auth type:
            if (!allRetrievers.containsKey(authType)) {
                return Single.error(new UnsupportedOperationException(String.format("authType '%s' not supported.", authType)));
            }
            chainedCredentialRetriever.addRetriever(allRetrievers.get(authType));
        }
        return chainedCredentialRetriever.retrieve().onErrorResumeNext(e ->
                Single.error(new LoginFailureException(String.format("Cannot get credentials from authType '%s' due to error: %s", authType, e.getMessage())))
        );
    }

    private static Map<AuthType, ICredentialRetriever> buildCredentialRetrievers(AuthConfiguration auth) {
        AzureEnvironment env = auth.getEnvironment();
        Map<AuthType, ICredentialRetriever> map = new LinkedHashMap<>();
        map.put(AuthType.SERVICE_PRINCIPAL, new ServicePrincipalCredentialRetriever(auth, env));
        map.put(AuthType.MANAGED_IDENTITY, new ManagedIdentityCredentialRetriever(env));
        map.put(AuthType.AZURE_CLI, new AzureCliCredentialRetriever(env));
        map.put(AuthType.VSCODE, new VisualStudioCodeCredentialRetriever(env));
        map.put(AuthType.VISUAL_STUDIO, new VisualStudioCredentialRetriever(env));
        map.put(AuthType.OAUTH2, new OAuthCredentialRetriever(env));
        map.put(AuthType.DEVICE_CODE, new DeviceCodeCredentialRetriever(env));
        return map;
    }
}
