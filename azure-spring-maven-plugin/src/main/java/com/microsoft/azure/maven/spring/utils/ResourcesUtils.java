/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.utils;

import org.apache.maven.model.Resource;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.Arrays;
import java.util.List;

public class ResourcesUtils {
    private static final String DEFAULT_DIRECTORY = "${project.basedir}/target";
    private static final String DEFAULT_INCLUDE = "*.jar";

    public static List<Resource> getDefaultResources() {
        final Resource resource = new Resource();
        resource.setDirectory(DEFAULT_DIRECTORY);
        resource.addInclude(DEFAULT_INCLUDE);
        return Arrays.asList(resource);
    }

    public static void applyDefaultResourcesToDom4j(Element root) {
        final DOMElement resourceRootNode = new DOMElement("resources");
        for (final Resource resource : getDefaultResources()) {
            final DOMElement resourceNode = new DOMElement("resource");

            XmlUtils.addNotEmptyElement(resourceNode, "filtering", resource.getFiltering());
            XmlUtils.addNotEmptyElement(resourceNode, "mergeId", resource.getMergeId());
            XmlUtils.addNotEmptyElement(resourceNode, "targetPath", resource.getTargetPath());
            XmlUtils.addNotEmptyElement(resourceNode, "directory", resource.getDirectory());
            XmlUtils.addNotEmptyListElement(resourceNode, "includes", "include", resource.getIncludes());
            XmlUtils.addNotEmptyListElement(resourceNode, "excludes", "exclude", resource.getExcludes());

            resourceRootNode.add(resourceNode);
        }
        root.add(resourceRootNode);
    }

    private ResourcesUtils() {

    }
}
