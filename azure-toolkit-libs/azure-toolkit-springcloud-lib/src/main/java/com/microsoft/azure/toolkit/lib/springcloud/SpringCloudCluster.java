/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.appplatform.models.SpringApp;
import com.azure.resourcemanager.appplatform.models.SpringService;
import com.azure.resourcemanager.appplatform.models.SpringServices;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SpringCloudCluster extends AbstractAzureEntityManager<SpringCloudCluster, SpringCloudClusterEntity, SpringService> implements AzureOperationEvent.Source<SpringCloudCluster> {
    @Getter
    @Nonnull
    final SpringServices client;

    public SpringCloudCluster(@Nonnull SpringCloudClusterEntity entity, @Nonnull SpringServices client) {
        super(entity);
        this.client = client;
    }

    @Override
    SpringService loadRemote() {
        try {
            return this.client.getByResourceGroup(entity.getResourceGroup(), entity.getName());
        } catch (ManagementException e) { // if cluster with specified resourceGroup/name removed.
            return null;
        }
    }

    @Nonnull
    @Override
    @CacheEvict(cacheName = "asc/cluster/{}/apps", key = "${this.name()}")
    public SpringCloudCluster refresh() {
        return super.refresh();
    }

    @Nonnull
    @Cacheable(cacheName = "asc/cluster/{}/app/{}", key = "${this.name()}/$name")
    public SpringCloudApp app(final String name) {
        if (this.exists()) {
            try {
                final SpringApp app = Objects.requireNonNull(this.remote()).apps().getByName(name);
                return this.app(app);
            } catch (ManagementException ignored) {
            }
        }
        // if app with `name` not exist or this cluster removed?
        return this.app(new SpringCloudAppEntity(name, this.entity));
    }

    @Nonnull
    SpringCloudApp app(@Nonnull SpringApp app) {
        return this.app(new SpringCloudAppEntity(app, this.entity()));
    }

    @Nonnull
    public SpringCloudApp app(@Nonnull SpringCloudAppEntity app) {
        return new SpringCloudApp(app, this);
    }

    @Nonnull
    @Cacheable(cacheName = "asc/cluster/{}/apps", key = "${this.name()}")
    public List<SpringCloudApp> apps() {
        if (this.exists()) {
            return Objects.requireNonNull(this.remote()).apps().list().stream().map(this::app).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
