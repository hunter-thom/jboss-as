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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;

/**
 * Domain extension that integrates HornetQ.
 *
 * @author Emanuel Muckenhuber
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(MessagingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, MessagingSubsystemAdd.INSTANCE, MessagingSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, MessagingSubsystemDescribeHandler.INSTANCE, MessagingSubsystemProviders.SUBSYSTEM_DESCRIBE, false);

        subsystem.registerXMLElementWriter(MessagingSubsystemParser.getInstance());

        final ModelNodeRegistration queue = registration.registerSubModel(PathElement.pathElement(QUEUE), MessagingSubsystemProviders.QUEUE_RESOURCE);
        queue.registerOperationHandler(ADD, QueueAdd.INSTANCE, QueueAdd.INSTANCE, false);
        queue.registerOperationHandler(REMOVE, QueueRemove.INSTANCE, QueueRemove.INSTANCE, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.MESSAGING_1_0.getUriString(), MessagingSubsystemParser.getInstance());
    }

}
