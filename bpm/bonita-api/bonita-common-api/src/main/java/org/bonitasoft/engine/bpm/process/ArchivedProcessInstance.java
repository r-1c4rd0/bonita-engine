/**
 * Copyright (C) 2011-2014 BonitaSoft S.A.
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
package org.bonitasoft.engine.bpm.process;

import java.util.Date;

import org.bonitasoft.engine.bpm.ArchivedElement;
import org.bonitasoft.engine.bpm.BaseElement;
import org.bonitasoft.engine.bpm.NamedElement;

/**
 * Represents a archived instance of a process.
 * Gives access to the information of the instance, whereas the state, the definition, the archived date of the process instance...
 * 
 * @author Baptiste Mesta
 * @author Elias Ricken de Medeiros
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 * @version 6.3.5
 * @since 6.0.0
 */
public interface ArchivedProcessInstance extends NamedElement, BaseElement, ArchivedElement {

    /**
     * Get the state of this process instance when it was archived.
     * 
     * @return The state of this process instance when it was archived.
     * @see ProcessInstanceState
     * @since 6.0.0
     */
    String getState();

    /**
     * Get the identifier of the state of this process instance when it was archived.
     * 
     * @return The identifier of the state of this process instance when it was archived.
     * @see ProcessInstanceState
     * @since 6.0.0
     */
    int getStateId();

    /**
     * Get the date when this process instance is started.
     * 
     * @return The date when this process instance is started.
     * @since 6.0.0
     */
    Date getStartDate();

    /**
     * Get the identifier of the user who started this process instance.
     * 
     * @return The identifier of the user who started this process instance.
     * @since 6.0.0
     */
    long getStartedBy();

    /**
     * Get the identifier of the substitute user (as Process manager or Administrator) who started this process instance.
     * 
     * @return The identifier of the substitute user (as Process manager or Administrator) who started this process instance.
     * @since 6.3.0
     */
    long getStartedBySubstitute();

    /**
     * Get the identifier of the substitute user (as Process manager or Administrator) who started this process instance.
     * 
     * @return The identifier of the substitute user (as Process manager or Administrator) who started this process instance.
     * @since 6.0.1
     * @deprecated As of version 6.3.0, replaced by {@link ArchivedProcessInstance#getStartedBySubstitute()}
     */
    @Deprecated
    long getStartedByDelegate();

    /**
     * Get the date when this process instance is finished.
     * It equals to null, if this process instance is not finished.
     * 
     * @return The date when this process instance is finished.
     * @since 6.0.0
     */
    Date getEndDate();

    /**
     * Get the date of the last update of this process instance, when it was archived.
     * 
     * @return The date of the last update of this process instance, when it was archived.
     * @since 6.0.0
     */
    Date getLastUpdate();

    /**
     * Get the identifier of the definition of this process.
     * 
     * @return The identifier of the definition of this process.
     * @see ProcessDefinition#getId()
     * @since 6.0.0
     */
    long getProcessDefinitionId();

    /**
     * Get the description of this process instance, when it was archived.
     * 
     * @return The description of this process instance, when it was archived.
     * @since 6.0.0
     */
    String getDescription();

    /**
     * Get the identifier of the root process instance of this process instance.
     * It equals to -1, if this process instance doesn't have a root process instance.
     * 
     * @return The identifier of the root process instance of this process instance.
     * @see ProcessInstance#getId()
     * @see ArchivedProcessInstance#getSourceObjectId()
     * @since 6.0.0
     */
    long getRootProcessInstanceId();

    /**
     * Get the identifier of the flow node instance who starts this process instance.
     * It equals to -1, if this process instance is not a child of an other process instance.
     * 
     * @return The identifier of the flow node instance who starts this process instance.
     * @see org.bonitasoft.engine.bpm.flownode.CallActivityInstance#getId()
     * @see SubProcessDefinition#getId()
     * @since 6.0.0
     */
    long getCallerId();

}
