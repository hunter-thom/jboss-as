/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.logging.Level;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
class RootLoggerAdd implements ModelUpdateOperationHandler, RuntimeOperationHandler {

    static final RootLoggerAdd INSTANCE = new RootLoggerAdd();

    static final String OPERATION_NAME = "set-root-logger";

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set("remove-root-logger");

        final String level = operation.require(CommonAttributes.LEVEL).asString();
        final ModelNode handlers = operation.get(CommonAttributes.HANDLERS);

        if(context instanceof RuntimeOperationContext) {
            final RuntimeOperationContext runtimeContext = (RuntimeOperationContext) context;
            final ServiceTarget target = runtimeContext.getServiceTarget();
            try {
                final RootLoggerService service = new RootLoggerService();
                service.setLevel(Level.parse(level));
                target.addService(LogServices.ROOT_LOGGER, service)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            } catch (Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
                return Cancellable.NULL;
            }
            try {
                // install logger handler services
                if(handlers.getType() != ModelType.UNDEFINED) {
                    LogServices.installLoggerHandlers(target, "", handlers);
                }
            } catch (Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
                return Cancellable.NULL;
            }
        }

        final ModelNode subModel = context.getSubModel();
        subModel.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.LEVEL).set(level);
        subModel.get(CommonAttributes.ROOT_LOGGER, CommonAttributes.HANDLERS).set(handlers);

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
