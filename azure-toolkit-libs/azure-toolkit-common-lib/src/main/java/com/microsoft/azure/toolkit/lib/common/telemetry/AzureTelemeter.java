/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AzureTelemeter {
    private static final String SERVICE_NAME = "serviceName";
    private static final String OPERATION_NAME = "operationName";
    private static final String OP_ID = "op_id";
    private static final String OP_NAME = "op_name";
    private static final String OP_TYPE = "op_type";
    private static final String OP_PARENT_ID = "op_parentId";

    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_MSG = "message";
    private static final String ERROR_TYPE = "errorType";
    private static final String ERROR_CLASSNAME = "errorClassName";
    private static final String ERROR_STACKTRACE = "errorStackTrace";
    @Getter
    @Setter
    private static String eventNamePrefix;
    @Getter
    @Setter
    private static Map<String, String> commonProperties;
    @Getter
    @Setter
    private static TelemetryClient client;

    public static void afterCreate(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context context = op.get(AzureTelemetry.Context.class, new AzureTelemetry.Context());
        context.setCreateAt(Instant.now());
    }

    public static void beforeEnter(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context context = op.get(AzureTelemetry.Context.class, new AzureTelemetry.Context());
        context.setEnterAt(Instant.now());
    }

    public static void afterExit(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context context = op.get(AzureTelemetry.Context.class, new AzureTelemetry.Context());
        context.setExitAt(Instant.now());
        AzureTelemeter.log(AzureTelemetry.Type.INFO, serialize(op));
    }

    public static void onError(@Nonnull final IAzureOperation op, Throwable error) {
        final AzureTelemetry.Context context = op.get(AzureTelemetry.Context.class, new AzureTelemetry.Context());
        context.setExitAt(Instant.now());
        AzureTelemeter.log(AzureTelemetry.Type.ERROR, serialize(op), error);
    }

    public static void log(final AzureTelemetry.Type type, final Map<String, String> properties, final Throwable e) {
        if (Objects.nonNull(e)) {
            properties.putAll(serialize(e));
        }
        AzureTelemeter.log(type, properties);
    }

    public static void log(final AzureTelemetry.Type type, final Map<String, String> properties) {
        if (client != null) {
            properties.putAll(getCommonProperties());
            final String eventName = getEventNamePrefix() + "/" + type.name();
            client.trackEvent(eventName, properties, null);
            client.flush();
        }
    }

    @Nonnull
    private static Map<String, String> serialize(@Nonnull final IAzureOperation op) {
        final AzureTelemetry.Context operationContext = op.get(AzureTelemetry.Context.class, new AzureTelemetry.Context());
        final AzureTelemetry.Context actionContext = Optional.ofNullable(op.getActionParent())
                .map(o -> o.get(AzureTelemetry.Context.class))
                .orElse(new AzureTelemetry.Context());
        final Optional<IAzureOperation> parent = Optional.ofNullable(op.getParent());
        final Map<String, String> properties = new HashMap<>();
        final String name = op.getName().replaceAll("\\(.+\\)", "(***)"); // e.g. `appservice|file.list.dir`
        final String[] parts = name.split("\\."); // ["appservice|file", "list", "dir"]
        final String[] compositeServiceName = parts[0].split("\\|"); // ["appservice", "file"]
        final String mainServiceName = compositeServiceName[0]; // "appservice"
        final String operationName = compositeServiceName.length > 1 ? parts[1] + "_" + compositeServiceName[1] : parts[1]; // "list_file"
        properties.put(SERVICE_NAME, mainServiceName);
        properties.put(OPERATION_NAME, operationName);
        properties.put(OP_ID, op.getId());
        properties.put(OP_PARENT_ID, parent.map(IAzureOperation::getId).orElse("/"));
        properties.put(OP_NAME, name);
        properties.put(OP_TYPE, op.getType());
        properties.putAll(actionContext.getProperties());
        properties.putAll(operationContext.getProperties());
        return properties;
    }

    @Nonnull
    private static HashMap<String, String> serialize(@Nonnull Throwable e) {
        final HashMap<String, String> properties = new HashMap<>();
        final ErrorType type = ErrorType.userError; // TODO: (@wangmi & @Hanxiao.Liu)decide error type based on the type of ex.
        properties.put(ERROR_CLASSNAME, e.getClass().getName());
        properties.put(ERROR_TYPE, type.name());
        properties.put(ERROR_MSG, e.getMessage());
        properties.put(ERROR_STACKTRACE, ExceptionUtils.getStackTrace(e));
        return properties;
    }

    private enum ErrorType {
        userError,
        systemError,
        serviceError,
        toolError,
        unclassifiedError
    }
}
