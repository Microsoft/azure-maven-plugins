/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FTPUploaderTest {
    @Mock
    Log log;

    private FTPUploader ftpUploader = null;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ftpUploader = new FTPUploader(log);
    }

    @Test
    public void uploadDirectoryWithRetries() throws Exception {
        final FTPUploader uploaderSpy = spy(ftpUploader);

        // Failure
        MojoExecutionException exception = null;
        doReturn(false)
                .when(uploaderSpy)
                .uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
        try {
            uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password",
                    "sourceDir", "targetDir", 1);
        } catch (MojoExecutionException e) {
            exception = e;
        } finally {
            assertNotNull(exception);
        }

        // Success
        doReturn(true)
                .when(uploaderSpy)
                .uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
        uploaderSpy.uploadDirectoryWithRetries("ftpServer", "username", "password",
                "sourceDir", "targetDir", 1);
    }

    @Test
    public void uploadDirectory() throws Exception {
        final FTPUploader uploaderSpy = spy(ftpUploader);
        final FTPClient ftpClient = mock(FTPClient.class);
        doReturn(ftpClient).when(uploaderSpy).getFTPClient();
        doNothing().when(uploaderSpy).uploadDirectory(any(FTPClient.class), anyString(), anyString(), anyString());

        uploaderSpy.uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(ftpClient, times(1)).connect(anyString());
        verify(ftpClient, times(1)).login(anyString(), anyString());
        verify(ftpClient, times(1)).setFileType(FTP.BINARY_FILE_TYPE);
        verify(ftpClient, times(1)).enterLocalPassiveMode();
        verify(ftpClient, times(1)).disconnect();
        verifyNoMoreInteractions(ftpClient);
        verify(uploaderSpy, times(1)).uploadDirectory(any(FTPClient.class), anyString(), anyString(), anyString());
        verify(uploaderSpy, times(1)).getFTPClient();
        verify(uploaderSpy, times(1)).uploadDirectory(anyString(), anyString(), anyString(), anyString(), anyString());
        verifyNoMoreInteractions(uploaderSpy);
    }

    @Test
    public void getFTPClient() throws Exception {
        assertTrue(ftpUploader.getFTPClient() instanceof FTPClient);
    }
}
