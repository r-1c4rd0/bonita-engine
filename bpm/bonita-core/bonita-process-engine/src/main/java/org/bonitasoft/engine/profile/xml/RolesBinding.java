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
 **/
package org.bonitasoft.engine.profile.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.xml.ElementBinding;

/**
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class RolesBinding extends ElementBinding {

    private List<String> roles;

    @Override
    public void setAttributes(final Map<String, String> attributes) {
        roles = new ArrayList<String>(5);
    }

    @Override
    public void setChildElement(final String name, final String value, final Map<String, String> attributes) {
        if ("role".equals(name)) {
            roles.add(value);
        }
    }

    @Override
    public void setChildObject(final String name, final Object value) {
    }

    @Override
    public List<String> getObject() {
        return roles;
    }

    @Override
    public String getElementTag() {
        return "roles";
    }

}
