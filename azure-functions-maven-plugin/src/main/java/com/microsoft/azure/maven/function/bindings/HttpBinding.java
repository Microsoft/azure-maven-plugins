/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.HttpOutput;
import com.microsoft.azure.serverless.functions.annotation.HttpTrigger;

import java.util.Locale;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HttpBinding extends BaseBinding {
    private String route = "";

    private String webHookType = "";

    private String authLevel = "";

    private String[] methods = {};

    public HttpBinding(final HttpTrigger httpTrigger) {
        setDirection("in");
        setType("httpTrigger");
        setName(httpTrigger.name());

        route = httpTrigger.route();
        authLevel = httpTrigger.authLevel().toString().toLowerCase(Locale.ENGLISH);
        methods = httpTrigger.methods();
        webHookType = httpTrigger.webHookType();
    }

    public HttpBinding(final HttpOutput httpOutput) {
        setDirection("out");
        setType("http");
        setName(httpOutput.name());
    }

    public HttpBinding() {
        setDirection("out");
        setType("http");
        setName("$return");
    }

    @JsonGetter
    public String getRoute() {
        return route;
    }

    @JsonGetter
    public String getWebHookType() {
        return webHookType;
    }

    @JsonGetter
    public String getAuthLevel() {
        return authLevel;
    }

    @JsonGetter
    public String[] getMethods() {
        return methods.clone();
    }
}
