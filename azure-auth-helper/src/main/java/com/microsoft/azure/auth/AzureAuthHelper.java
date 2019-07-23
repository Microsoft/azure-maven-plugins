/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.azure.AzureEnvironment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureAuthHelper {
    /**
     * Performs an OAuth 2.0 login.
     *
     * @param env the azure environment
     * @return the azure credential
     * @throws AzureLoginFailureException when there are some errors during login.
     */
    public static AzureCredential oAuthLogin(AzureEnvironment env) throws AzureLoginFailureException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Performs a device login.
     *
     * @param env the azure environment
     * @return the azure credential through
     * @throws AzureLoginFailureException when there are some errors during login.
     */
    public static AzureCredential deviceLogin(AzureEnvironment env) throws AzureLoginFailureException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Refresh an azure credential using refresh token.
     *
     * @param env the azure environment
     * @param refreshToken the refresh token
     *
     * @return the azure credential
     * @throws AzureLoginFailureException when there are some errors during refreshing.
     */
    public static AzureCredential refreshToken(AzureEnvironment env, String refreshToken) throws AzureLoginFailureException {
        if (env == null) {
            throw new NullPointerException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(refreshToken)) {
            throw new IllegalArgumentException("Parameter 'refreshToken' cannot be empty.");
        }
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            // TODO: handle proxy
            final AuthenticationContext authenticationContext =
                    new AuthenticationContext(baseURL(env), true, executorService);
            return AzureCredential.fromAuthenticationResult(authenticationContext.acquireTokenByRefreshToken(refreshToken,
                            Constants.CLIENT_ID, env.managementEndpoint(), null).get());
        } catch (MalformedURLException | InterruptedException | ExecutionException e) {
            throw new AzureLoginFailureException(e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * Get the corresponding azure environment
     * .
     * @param environment the environment key
     * @return the AzureEnvironment instance
     */
    public static AzureEnvironment getAzureEnvironment(String environment) {
        if (StringUtils.isEmpty(environment)) {
            return AzureEnvironment.AZURE;
        }

        switch (environment.toUpperCase(Locale.ENGLISH)) {
            case "AZURE_CHINA":
                return AzureEnvironment.AZURE_CHINA;
            case "AZURE_GERMANY":
                return AzureEnvironment.AZURE_GERMANY;
            case "AZURE_US_GOVERNMENT":
                return AzureEnvironment.AZURE_US_GOVERNMENT;
            default:
                return AzureEnvironment.AZURE;
        }
    }

    /**
     * Get the azure-secret.json file according to environment variable, the default location is $HOME/.azure/azure-secret.json
     */
    public static File getAzureSecretFile() {
        return (StringUtils.isBlank(System.getProperty(Constants.AZURE_HOME_KEY)) ?
              Paths.get(System.getProperty(Constants.USER_HOME_KEY), Constants.AZURE_HOME_DEFAULT, Constants.AZURE_SECRET_FILE)
              : Paths.get(System.getProperty(Constants.AZURE_HOME_KEY), Constants.AZURE_SECRET_FILE)).toFile();
    }

    /**
     * Check whether the azure-secret.json file exists and is not empty.
     */
    public static boolean existsAzureSecretFile() {
        final File azureSecretFile = AzureAuthHelper.getAzureSecretFile();
        return azureSecretFile.exists() && azureSecretFile.isFile() && azureSecretFile.length() > 0;
    }

    /**
     * Delete the azure-secret.json.
     *
     * @return true if the file is deleted.
     */
    public static boolean deleteAzureSecretFile() {
        if (existsAzureSecretFile()) {
            return FileUtils.deleteQuietly(AzureAuthHelper.getAzureSecretFile());
        }
        return false;
    }

    /***
     * Save the credential to a file.
     *
     * @param cred the credential
     * @param file the file name to save the credential
     * @throws IOException if there is any IO error.
     */
    public static void writeAzureCredentials(AzureCredential cred, File file) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /***
     * Read the credential from a file.
     *
     * @param cred the credential
     * @param file the file to be read
     * @return the saved credential
     * @throws IOException if there is any IO error.
     */
    public static AzureCredential readAzureCredentials(File file) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    static String authorizationUrl(AzureEnvironment env, String redirectUrl) {
        if (env == null) {
            throw new NullPointerException("Parameter 'env' cannot be null.");
        }
        if (StringUtils.isBlank(redirectUrl)) {
            throw new IllegalArgumentException("Parameter 'redirectUrl' cannot be empty.");
        }
        return String.format(
                "%s/oauth2/authorize?client_id=%s&response_type=code" + "&redirect_uri=%s&prompt=select_account&resource=%s",
                baseURL(env), Constants.CLIENT_ID, redirectUrl, env.managementEndpoint());
    }

    static String baseURL(AzureEnvironment env) {
        return env.activeDirectoryEndpoint() + Constants.COMMON_TENANT;
    }

    private AzureAuthHelper() {

    }
}
