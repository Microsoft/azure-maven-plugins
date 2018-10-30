/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.functions.annotation.Cardinality;
import com.microsoft.azure.functions.annotation.EventHubOutput;
import com.microsoft.azure.functions.annotation.EventHubTrigger;

import java.util.Locale;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EventHubBinding extends BaseBinding {
    public static final String EVENT_HUB_TRIGGER = "eventHubTrigger";
    public static final String EVENT_HUB = "eventHub";

    private String eventHubName = "";

    private String consumerGroup = "";

    private String connection = "";

    private String cardinality = Cardinality.MANY.name();

    public EventHubBinding(final EventHubTrigger eventHubTrigger) {
        super(eventHubTrigger.name(), EVENT_HUB_TRIGGER, Direction.IN, eventHubTrigger.dataType());

        eventHubName = eventHubTrigger.eventHubName();
        consumerGroup = eventHubTrigger.consumerGroup();
        connection = eventHubTrigger.connection();
        cardinality = eventHubTrigger.cardinality().name();
    }

    public EventHubBinding(final EventHubOutput eventHubOutput) {
        super(eventHubOutput.name(), EVENT_HUB, Direction.OUT, eventHubOutput.dataType());

        eventHubName = eventHubOutput.eventHubName();
        connection = eventHubOutput.connection();
        cardinality = ""; // Cardinality is for input/trigger only.
    }

    @JsonGetter
    public String getCardinality() {
        return cardinality.toLowerCase(Locale.ENGLISH);
    }

    @JsonGetter("eventHubName")
    public String getEventHubName() {
        return eventHubName;
    }

    @JsonGetter
    public String getConsumerGroup() {
        return consumerGroup;
    }

    @JsonGetter
    public String getConnection() {
        return connection;
    }
}
