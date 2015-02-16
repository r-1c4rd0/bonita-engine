/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.commons;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.DateConverter;

/**
 * @author Laurent Leseigneur
 */

public class TypeConverterUtil {

    private ConvertUtilsBean convertUtilsBean;

    public TypeConverterUtil(String[] datePatterns) {
        convertUtilsBean = new ConvertUtilsBean();
        final DateConverter dateConverter = new DateConverter();
        dateConverter.setPatterns(datePatterns);
        convertUtilsBean.register(dateConverter, Date.class);
    }

    public Object convertToType(Class<? extends Serializable> clazz, Serializable parameterValue) {
        try {
            return convertUtilsBean.convert(parameterValue, clazz);
        } catch (ConversionException e) {
            throw new IllegalArgumentException("unable to parse '" + parameterValue + "' to type " + clazz.getName());
        }
    }

}