/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.serialization.impl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.packaging.DistributionPackage;
import org.apache.sling.distribution.serialization.DistributionPackageBuilder;
import org.apache.sling.distribution.serialization.DistributionPackageBuildingException;
import org.apache.sling.distribution.serialization.DistributionPackageReadingException;

public class ResourceSharedDistributionPackageBuilder implements DistributionPackageBuilder {

    private final String PN_ORIGINAL_ID = "original.package.id";
    private final String PN_ORIGINAL_REQUEST_TYPE = "original.package.request.type";
    private final String PN_ORIGINAL_PATHS = "original.package.paths";

    private final String PACKAGE_NAME_PREFIX = "distrpackage";
    private final String SHARED_PACKAGES_ROOT = "/var/sling/distribution/packages";

    private final DistributionPackageBuilder distributionPackageBuilder;

    public ResourceSharedDistributionPackageBuilder(DistributionPackageBuilder distributionPackageExporter) {
        this.distributionPackageBuilder = distributionPackageExporter;
    }

    public String getType() {
        return distributionPackageBuilder.getType();
    }

    @CheckForNull
    public DistributionPackage createPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionRequest request) throws DistributionPackageBuildingException {
        DistributionPackage distributionPackage = distributionPackageBuilder.createPackage(resourceResolver, request);

        if (distributionPackage == null) {
            return null;
        }

        try {
            String packageName = generateNameFromId(resourceResolver, distributionPackage);
            String packagePath = getPathFromName(packageName);


            return new ResourceSharedDistributionPackage(resourceResolver, packageName, packagePath, distributionPackage);
        }
        catch (PersistenceException e) {
            throw new DistributionPackageBuildingException(e);
        }
    }

    @CheckForNull
    public DistributionPackage readPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull InputStream stream) throws DistributionPackageReadingException {
        DistributionPackage distributionPackage = distributionPackageBuilder.readPackage(resourceResolver, stream);

        if (distributionPackage == null) {
            return null;
        }

        try {
            String packageName = generateNameFromId(resourceResolver, distributionPackage);
            String packagePath = getPathFromName(packageName);

            return new ResourceSharedDistributionPackage(resourceResolver, packageName, packagePath, distributionPackage);
        }
        catch (PersistenceException e) {
            throw new DistributionPackageReadingException(e);
        }
    }

    public DistributionPackage getPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull String distributionPackageId) {
        String packageName = distributionPackageId;
        String originalPackageId = retrieveIdFromName(resourceResolver, packageName);

        if (originalPackageId == null) {
            return null;
        }

        DistributionPackage distributionPackage = distributionPackageBuilder.getPackage(resourceResolver, originalPackageId);

        if (distributionPackage == null) {
            return null;
        }
        String packagePath = getPathFromName(packageName);
        return new ResourceSharedDistributionPackage(resourceResolver, packageName, packagePath, distributionPackage);
    }

    public boolean installPackage(@Nonnull ResourceResolver resourceResolver, @Nonnull DistributionPackage distributionPackage) throws DistributionPackageReadingException {
        if (! (distributionPackage instanceof ResourceSharedDistributionPackage)) {
            return false;
        }

        ResourceSharedDistributionPackage shareddistributionPackage = (ResourceSharedDistributionPackage) distributionPackage;

        DistributionPackage originalPackage = shareddistributionPackage.getPackage();
        return distributionPackageBuilder.installPackage(resourceResolver, originalPackage);
    }


    private String generateNameFromId(ResourceResolver resourceResolver, DistributionPackage distributionPackage) throws PersistenceException {
        String name = PACKAGE_NAME_PREFIX + "_" + System.currentTimeMillis() + "_" +  UUID.randomUUID();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PN_ORIGINAL_ID, distributionPackage.getId());

        // save the info just for debugging purposes
        if (distributionPackage.getInfo().getRequestType() != null) {
            properties.put(PN_ORIGINAL_REQUEST_TYPE, distributionPackage.getInfo().getRequestType());

        }
        if (distributionPackage.getInfo().getPaths() != null) {
            properties.put(PN_ORIGINAL_PATHS, distributionPackage.getInfo().getPaths());
        }

        String packagePath = getPathFromName(name);

        Resource resource = ResourceUtil.getOrCreateResource(resourceResolver, packagePath,
                "sling:Folder", "sling:Folder", false);

        ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
        valueMap.putAll(properties);

        resourceResolver.create(resource, ResourceSharedDistributionPackage.REFERENCE_ROOT_NODE,
                Collections.singletonMap(ResourceResolver.PROPERTY_RESOURCE_TYPE, (Object)"sling:Folder"));

        resourceResolver.commit();
        return name;
    }

    private String getPathFromName(String name) {
        String packagePath = SHARED_PACKAGES_ROOT + "/" + name;
        return packagePath;
    }

    private String retrieveIdFromName(ResourceResolver resourceResolver, String name) {
        String packagePath = getPathFromName(name);

        Resource resource = resourceResolver.getResource(packagePath);

        if (resource == null) {
            return null;
        }

        ValueMap properties = resource.adaptTo(ValueMap.class);

        if (properties == null) {
            return null;
        }


        return properties.get(PN_ORIGINAL_ID, null);
    }
}
