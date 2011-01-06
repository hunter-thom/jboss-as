/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface OperationHandler {

    /**
     * Get the description for this operation, without address information.
     *
     * @return the description
     */
    ModelNode getOperationDescription();

    /**
     * Apply this update to the given model entity. This method should either
     * successfully change the model entity, or leave the entity unchanged
     * and throw an {@code UpdateFailedException}.
     *
     * @param submodel the model entity to which the update should be applied
     * @param operation the operation description
     * @throws IllegalArgumentException if the update is not valid
     */
    void applyModelUpdate(ModelNode submodel, ModelNode operation) throws IllegalArgumentException;
}