/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Utility class
 */
public final class Utils {
    /**
     * Get server credential from Maven settings by server Id.
     *
     * @param settings Maven settings object.
     * @param serverId Server Id.
     * @return Server object if it exists in settings. Otherwise return null.
     */
    public static Server getServer(final Settings settings, final String serverId) {
        if (settings == null || StringUtils.isEmpty(serverId)) {
            return null;
        }
        return settings.getServer(serverId);
    }

    /**
     * Get string value from server configuration section in settings.xml.
     *
     * @param server Server object.
     * @param key    Key string.
     * @return String value if key exists; otherwise, return null.
     */
    public static String getValueFromServerConfiguration(final Server server, final String key) {
        if (server == null) {
            return null;
        }

        final Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
        if (configuration == null) {
            return null;
        }

        final Xpp3Dom node = configuration.getChild(key);
        if (node == null) {
            return null;
        }

        return node.getValue();
    }

    /**
     * Get string value from plugin descriptor file, namely /META-INF/maven/pugin.xml .
     *
     * @param tagName Valid tagName in /META-INF/maven/plugin.xml, such as "artifactId", "version" etc..
     * @return String value of target property.
     */
    public static String getValueFromPluginDescriptor(final String tagName) {
        try (final InputStream is = Utils.class.getResourceAsStream("/META-INF/maven/plugin.xml")) {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            return doc.getElementsByTagName(tagName).item(0).getTextContent();
        } catch (Exception e) {
        }
        return null;
    }

    public static void copyResources(final MavenProject project, final MavenSession session,
                                     final MavenResourcesFiltering filtering, final List<Resource> resources,
                                     final String targetDirectory) throws IOException {
        for (final Resource resource : resources) {
            resource.setTargetPath(targetDirectory + "/" + resource.getTargetPath());
            resource.setFiltering(false);
        }

        final MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
                resources,
                new File(targetDirectory),
                project,
                "UTF-8",
                null,
                Collections.EMPTY_LIST,
                session
        );

        // Configure executor
        mavenResourcesExecution.setEscapeWindowsPaths(true);
        mavenResourcesExecution.setInjectProjectBuildFilters(false);
        mavenResourcesExecution.setOverwrite(true);
        mavenResourcesExecution.setIncludeEmptyDirs(false);
        mavenResourcesExecution.setSupportMultiLineFiltering(false);

        // Filter resources
        try {
            filtering.filterResources(mavenResourcesExecution);
        } catch (MavenFilteringException ex) {
            throw new IOException("Failed to copy resources", ex);
        }
    }
}
