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
package org.bonitasoft.engine.bpm.flownode.impl.internal;

import org.bonitasoft.engine.bpm.flownode.FlowNodeType;
import org.bonitasoft.engine.bpm.flownode.SubProcessActivityInstance;

/**
 * @author Elias Ricken de Medeiros
 * @author Celine Souchet
 */
public class SubProcessActivityInstanceImpl extends ActivityInstanceImpl implements SubProcessActivityInstance {

    private static final long serialVersionUID = 2652709303320068129L;

    private final boolean triggeredByEvent;

    public SubProcessActivityInstanceImpl(final String name, final long flownodeDefinitionId, final boolean triggeredByEvent) {
        super(name, flownodeDefinitionId);
        this.triggeredByEvent = triggeredByEvent;
    }

    @Override
    public FlowNodeType getType() {
        return FlowNodeType.SUB_PROCESS;
    }

    @Override
    public boolean isTriggeredByEvent() {
        return triggeredByEvent;
    }

}
