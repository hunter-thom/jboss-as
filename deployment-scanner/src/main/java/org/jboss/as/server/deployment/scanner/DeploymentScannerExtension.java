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

package org.jboss.as.server.deployment.scanner;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author Emanuel Muckenhuber
 */
public class DeploymentScannerExtension implements Extension {
    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment.scanner");

    public static final String SUBSYSTEM_NAME = "deployment-scanner";
    private static final PathElement scannersPath = PathElement.pathElement("scanner");
    private static final DeploymentScannerParser parser = new DeploymentScannerParser();
    private static final String DEFAULT_SCANNER_NAME = "default"; // we actually need a scanner name to make it addressable

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        log.debug("Initializing Deployment Scanner Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(CommonAttributes.DEPLOYMENT_SCANNER);
        subsystem.registerXMLElementWriter(parser);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(DeploymentSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, SubsystemAdd.INSTANCE, DeploymentSubsystemProviders.SUBSYSTEM_ADD, false);
        // Register operation handlers
        final ModelNodeRegistration scanners = registration.registerSubModel(scannersPath, DeploymentSubsystemProviders.SCANNER);
        scanners.registerOperationHandler(ADD, DeploymentScannerAdd.INSTANCE, DeploymentSubsystemProviders.SCANNER_ADD, false);
        scanners.registerOperationHandler(REMOVE, DeploymentScannerRemove.INSTANCE, DeploymentSubsystemProviders.SCANNER_REMOVE, false);
        scanners.registerOperationHandler("enable", DeploymentScannerEnable.INSTANCE, DeploymentSubsystemProviders.SCANNER_ENABLE, false);
        scanners.registerOperationHandler("disable", DeploymentScannerDisable.INSTANCE, DeploymentSubsystemProviders.SCANNER_DISABLE, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parser);
    }

    /**
     * Add handler creating the subsystem
     */
    static class SubsystemAdd implements ModelAddOperationHandler {

        static final SubsystemAdd INSTANCE = new SubsystemAdd();

        /** {@inheritDoc} */
        @Override
        public Cancellable execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            final ModelNode compensatingOperation = new ModelNode();
            compensatingOperation.set(OP).set(REMOVE);
            compensatingOperation.set(OP_ADDR).set(operation.get(OP_ADDR));
            // create the scanner root
            context.getSubModel().get("scanner");

            resultHandler.handleResultComplete(compensatingOperation);
            return Cancellable.NULL;
        }
    }

    static class DeploymentScannerParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

        /** {@inheritDoc} */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
            ModelNode scanners = context.getModelNode();
            for (final Property list : scanners.asPropertyList()) {
                final ModelNode node = list.getValue();

                for (final Property scanner : node.asPropertyList()) {

                    writer.writeEmptyElement(Element.DEPLOYMENT_SCANNER.getLocalName());
                    writer.writeAttribute(Attribute.NAME.getLocalName(), scanner.getName());
                    ModelNode configuration = scanner.getValue();
                    if (has(configuration, CommonAttributes.PATH)) {
                        writer.writeAttribute(Attribute.PATH.getLocalName(), configuration.get(CommonAttributes.PATH)
                                .asString());
                    }
                    if (has(configuration, CommonAttributes.SCAN_ENABLED)) {
                        writer.writeAttribute(Attribute.SCAN_ENABLED.getLocalName(),
                                configuration.get(CommonAttributes.SCAN_ENABLED).asString());
                    }
                    if (has(configuration, CommonAttributes.SCAN_INTERVAL)) {
                        writer.writeAttribute(Attribute.SCAN_INTERVAL.getLocalName(),
                                configuration.get(CommonAttributes.SCAN_INTERVAL).asString());
                    }
                    if (configuration.has(CommonAttributes.RELATIVE_TO)) {
                        writer.writeAttribute(Attribute.RELATIVE_TO.getLocalName(),
                                configuration.get(CommonAttributes.RELATIVE_TO).asString());
                    }
                }
                writer.writeEndElement();
  }
        }

        private boolean has(ModelNode node, String name) {
            return node.has(name) && node.get(name).isDefined();
        }

        /** {@inheritDoc} */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            // no attributes
            requireNoAttributes(reader);

            final ModelNode address = new ModelNode();
            address.add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
            address.protect();

            final ModelNode subsystem = new ModelNode();
            subsystem.get(OP).set(ADD);
            subsystem.get(OP_ADDR).set(address);
            list.add(subsystem);

            // elements
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                switch (Namespace.forUri(reader.getNamespaceURI())) {
                    case DEPLOYMENT_SCANNER_1_0: {
                        final Element element = Element.forName(reader.getLocalName());
                        switch (element) {
                            case DEPLOYMENT_SCANNER: {
                                //noinspection unchecked
                                parseScanner(reader, address, list);
                                break;
                            }
                            default: throw unexpectedElement(reader);
                        }
                        break;
                    }
                    default: throw unexpectedElement(reader);
                }
            }
        }

        void parseScanner(XMLExtendedStreamReader reader, final ModelNode address, List<ModelNode> list) throws XMLStreamException {
            // Handle attributes
            boolean enabled = true;
            int interval = 0;
            String path = null;
            String name = DEFAULT_SCANNER_NAME;
            String relativeTo = null;
            final int attrCount = reader.getAttributeCount();
            for (int i = 0; i < attrCount; i++) {
                requireNoNamespaceAttribute(reader, i);
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case PATH: {
                        path = value;
                        break;
                    }
                    case NAME: {
                        name = value;
                        break;
                    }
                    case RELATIVE_TO: {
                        relativeTo = value;
                        break;
                    }
                    case SCAN_INTERVAL: {
                        interval = Integer.parseInt(value);
                        break;
                    }
                    case SCAN_ENABLED: {
                        enabled = Boolean.parseBoolean(value);
                        break;
                    }
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
            if (name == null) {
                ParseUtils.missingRequired(reader, Collections.singleton("name"));
            }
            if (path == null) {
                ParseUtils.missingRequired(reader, Collections.singleton("path"));
            }
            requireNoContent(reader);

            final ModelNode operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address).add("scanner", name);
            operation.get(CommonAttributes.PATH).set(path);
            operation.get(CommonAttributes.SCAN_INTERVAL).set(interval);
            operation.get(CommonAttributes.SCAN_ENABLED).set(enabled);
            if(relativeTo != null) operation.get(CommonAttributes.RELATIVE_TO).set(relativeTo);
            list.add(operation);
        }

    }

}
