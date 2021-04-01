/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.util;

import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.util.CoreUtils;
import com.azure.identity.CredentialUnavailableException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCliSubscriptionEntity;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AzureCliUtils {
    private static final boolean isWindows = System.getProperty("os.name").contains("Windows");
    private static final String WINDOWS_STARTER = "cmd.exe";
    private static final String LINUX_MAC_STARTER = "/bin/sh";
    private static final String WINDOWS_SWITCHER = "/c";
    private static final String LINUX_MAC_SWITCHER = "-c";
    private static final String DEFAULT_WINDOWS_SYSTEM_ROOT = System.getenv("SystemRoot");
    private static final String DEFAULT_MAC_LINUX_PATH = "/bin/";
    private static final String WINDOWS_PROCESS_ERROR_MESSAGE = "'az' is not recognized";
    private static final String LINUX_MAC_PROCESS_ERROR_MESSAGE = "(.*)az:(.*)not found";

    public static boolean checkCliVersion() {
        try {
            final JsonObject result = AzureCliUtils.executeAzCommandJson("az version --output json").getAsJsonObject();
            String cliVersion = result.get("azure-cli").getAsString();
            // we require at least azure cli version 2.11.0
            if (compareVersion(cliVersion, "2.11.0", 3) < 0) {
                throw new AzureToolkitAuthenticationException(String.format("Your azure cli version '%s' is too old, " +
                        "you need to upgrade your CLI with 'az upgrade'.", cliVersion));
            }
            return true;
        } catch (NullPointerException | NumberFormatException ex) {
            return false;
        }
    }

    private static int compareVersion(String version1, String version2, int maxVersionCount) {
        String[] v1s = version1.split("\\.");
        String[] v2s = version2.split("\\.");
        int i = 0;
        for (; i < v1s.length && i < v2s.length && i < maxVersionCount; i++) {
            int v1 = Integer.parseInt(v1s[i]);
            int v2 = Integer.parseInt(v2s[i]);
            if (v1 > v2) {
                return 1;
            }
            if (v2 > v1) {
                return -1;
            }
        }
        if (i < v1s.length) {
            for (; i < v1s.length; i++) {
                if (Integer.parseInt(v1s[i]) != 0) {
                    return 1;
                }
            }
        }
        if (i < v2s.length) {
            for (; i < v2s.length; i++) {
                if (Integer.parseInt(v2s[i]) != 0) {
                    return -1;
                }
            }
        }
        return 0;
    }

    public static List<AzureCliSubscriptionEntity> listSubscriptions() {
        final JsonArray result = executeAzCommandJson("az account list --output json").getAsJsonArray();
        final List<AzureCliSubscriptionEntity> list = new ArrayList<>();
        if (result != null) {
            result.forEach(j -> {
                JsonObject accountObject = j.getAsJsonObject();
                if (!accountObject.has("id")) {
                    return;
                }
                // TODO: use utility to handle the json mapping
                String tenantId = accountObject.get("tenantId").getAsString();
                String subscriptionId = accountObject.get("id").getAsString();
                String subscriptionName = accountObject.get("name").getAsString();
                String state = accountObject.get("state").getAsString();
                String cloud = accountObject.get("cloudName").getAsString();
                String email = accountObject.get("user").getAsJsonObject().get("name").getAsString();

                if (StringUtils.equals(state, "Enabled") && StringUtils.isNoneBlank(subscriptionId, subscriptionName)) {
                    AzureCliSubscriptionEntity entity = new AzureCliSubscriptionEntity();
                    entity.setId(subscriptionId);
                    entity.setName(subscriptionName);
                    entity.setSelected(accountObject.get("isDefault").getAsBoolean());
                    entity.setTenantId(tenantId);
                    entity.setEmail(email);
                    entity.setEnvironment(AzureEnvironmentUtils.stringToAzureEnvironment(cloud));
                    list.add(entity);
                }
            });
            return list;
        }
        return null;
    }

    /**
     * Modified code based on https://github.com/Azure/azure-sdk-for-java/blob/master
     * /sdk/identity/azure-identity/src/main/java/com/azure/identity/implementation/IdentityClient.java#L411
     *
     * @param command the az command to be executed
     */
    public static JsonElement executeAzCommandJson(String command) {
        BufferedReader reader = null;
        try {
            final String starter;
            final String switcher;
            if (isWindows) {
                starter = WINDOWS_STARTER;
                switcher = WINDOWS_SWITCHER;
            } else {
                starter = LINUX_MAC_STARTER;
                switcher = LINUX_MAC_SWITCHER;
            }

            final ProcessBuilder builder = new ProcessBuilder(starter, switcher, command);
            final String workingDirectory = getSafeWorkingDirectory();
            if (workingDirectory != null) {
                builder.directory(new File(workingDirectory));
            } else {
                throw new IllegalStateException("A Safe Working directory could not be found to execute command from.");
            }
            builder.redirectErrorStream(true);
            final Process process = builder.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            final StringBuilder output = new StringBuilder();
            while (true) {
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith(WINDOWS_PROCESS_ERROR_MESSAGE) || line.matches(LINUX_MAC_PROCESS_ERROR_MESSAGE)) {
                    throw new CredentialUnavailableException(
                            "AzureCliTenantCredential authentication unavailable. Azure CLI not installed.");
                }
                output.append(line);
            }

            final String processOutput = output.toString();
            process.waitFor(10, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                if (processOutput.length() > 0) {
                    final String redactedOutput = redactInfo("\"accessToken\": \"(.*?)(\"|$)", processOutput);
                    if (redactedOutput.contains("az login") || redactedOutput.contains("az account set")) {
                        throw new CredentialUnavailableException(
                                "AzureCliTenantCredential authentication unavailable. Please run 'az login' to set up account.");
                    }
                    throw new ClientAuthenticationException(redactedOutput, null);
                } else {
                    throw new ClientAuthenticationException("Failed to invoke Azure CLI ", null);
                }
            }

            try {
                if (StringUtils.startsWith(StringUtils.trim(processOutput), "[")) {
                    return JsonUtils.getGson().fromJson(output.toString(), JsonArray.class);
                } else {
                    return JsonUtils.getGson().fromJson(output.toString(), JsonObject.class);
                }
            } catch (JsonParseException ex) {
                throw new AzureToolkitAuthenticationException(String.format("Cannot execute command '%s', the output '%s' cannot be parsed as a JSON.",
                        command, output.toString()));
            }
        } catch (IOException | InterruptedException e) {
            throw new AzureToolkitAuthenticationException(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static String getSafeWorkingDirectory() {
        if (isWindows) {
            if (CoreUtils.isNullOrEmpty(DEFAULT_WINDOWS_SYSTEM_ROOT)) {
                return null;
            }
            return DEFAULT_WINDOWS_SYSTEM_ROOT + "\\system32";
        } else {
            return DEFAULT_MAC_LINUX_PATH;
        }
    }

    private static String redactInfo(String regex, String input) {
        return input.replaceAll(regex, "****");
    }
}
