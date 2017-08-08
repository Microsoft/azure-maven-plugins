/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.maven.webapp.handlers.RuntimeHandler;
import com.microsoft.azure.maven.webapp.handlers.SettingsHandler;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeployFacadeImplWithCreateTest {
    @Mock
    private AbstractWebAppMojo mojo;

    @Mock
    private Log log;

    private DeployFacadeImplWithCreate facade = null;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mojo.getLog()).thenReturn(log);
        facade = new DeployFacadeImplWithCreate(mojo);
    }

    @Test
    public void setupRuntime() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);
        final RuntimeHandler handler = mock(RuntimeHandler.class);
        doReturn(handler).when(facadeSpy).getRuntimeHandler();

        facadeSpy.setupRuntime();
        verify(handler, times(1)).defineAppWithRunTime();
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void applySettings() throws Exception {
        final DeployFacadeBaseImpl facadeSpy = spy(facade);
        final SettingsHandler handler = mock(SettingsHandler.class);
        doReturn(handler).when(facadeSpy).getSettingsHandler();

        facadeSpy.applySettings();
        verify(handler, times(1)).processSettings((WebApp.DefinitionStages.WithCreate) null);
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void commitChanges() throws Exception {
        final WebApp.DefinitionStages.WithCreate withCreate = mock(WebApp.DefinitionStages.WithCreate.class);
        ReflectionTestUtils.setField(facade, "withCreate", withCreate);

        facade.commitChanges();
        verify(withCreate, times(1)).create();
        verifyNoMoreInteractions(withCreate);
    }
}
