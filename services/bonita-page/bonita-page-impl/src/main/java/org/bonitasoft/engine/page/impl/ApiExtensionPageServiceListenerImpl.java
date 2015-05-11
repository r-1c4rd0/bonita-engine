/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 */
package org.bonitasoft.engine.page.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import org.bonitasoft.engine.commons.exceptions.SObjectCreationException;
import org.bonitasoft.engine.page.PageMappingService;
import org.bonitasoft.engine.page.PageServiceListener;
import org.bonitasoft.engine.page.SContentType;
import org.bonitasoft.engine.page.SInvalidPageZipMissingPropertiesException;
import org.bonitasoft.engine.page.SPage;

/**
 * @author Laurent Leseigneur
 */
public class ApiExtensionPageServiceListenerImpl implements PageServiceListener {

    private final PageMappingService pageMappingService;

    private final SPageContentHelper helper;

    public ApiExtensionPageServiceListenerImpl(final PageMappingService pageMappingService) {
        this(pageMappingService, new SPageContentHelper());
    }

    public ApiExtensionPageServiceListenerImpl(final PageMappingService pageMappingService, final SPageContentHelper helper) {
        super();
        this.pageMappingService = pageMappingService;
        this.helper = helper;
    }

    @Override
    public void pageInserted(final SPage sPage, final byte[] content) throws SObjectCreationException {
        if (SContentType.API_EXTENSION.equals(sPage.getContentType())){
            try {
                addPageMapping(sPage, content);
            } catch (final IOException | SInvalidPageZipMissingPropertiesException e) {
                throw new SObjectCreationException(e);
            }
        }
    }

    private void addPageMapping(final SPage sPage, final byte[] content) throws SObjectCreationException, IOException,
            SInvalidPageZipMissingPropertiesException {
        final Properties apiProperties = helper.loadPageProperties(content);
        final String apiExtensions = getProperty(apiProperties, "apiExtensions");
        final String[] resourceNames = apiExtensions.split(",");
        for (final String resource : resourceNames) {
            final String resourceName = resource.trim();
            final String method = getProperty(apiProperties, resourceName + ".method");
            final String pathTemplate = getProperty(apiProperties, resourceName + ".pathTemplate");
            getProperty(apiProperties, resourceName + ".classFileName");
            pageMappingService.create("apiExtension|" + method + "|" + pathTemplate, sPage.getId(), Collections.EMPTY_LIST);
        }
    }

    private String getProperty(final Properties properties, final String propertyName) throws SObjectCreationException {
        final String property = (String) properties.get(propertyName);
        if (property == null || property.trim().length() == 0) {
            throw new SObjectCreationException("the property '" + propertyName + "' is missing or is empty");
        }
        return property;
    }

}
