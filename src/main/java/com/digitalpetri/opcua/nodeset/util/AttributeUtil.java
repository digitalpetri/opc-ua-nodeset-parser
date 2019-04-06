package com.digitalpetri.opcua.nodeset.util;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.serialization.EncodingLimits;
import org.eclipse.milo.opcua.stack.core.serialization.OpcUaXmlStreamDecoder;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.types.DataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.OpcUaDataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.opcfoundation.ua.generated.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class AttributeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeUtil.class);

    private static final SerializationContext SERIALIZATION_CONTEXT = new SerializationContext() {

        private final NamespaceTable namespaceTable = new NamespaceTable();

        @Override
        public EncodingLimits getEncodingLimits() {
            return EncodingLimits.DEFAULT;
        }

        @Override
        public NamespaceTable getNamespaceTable() {
            return namespaceTable;
        }

        @Override
        public DataTypeManager getDataTypeManager() {
            return OpcUaDataTypeManager.getInstance();
        }
    };

    public static NodeId parseDataType(String dataType, Map<String, NodeId> aliases) {
        return tryParseNodeId(aliases, dataType);
    }

    public static NodeId parseReferenceTypeId(Reference gReference, Map<String, NodeId> aliases) {
        String referenceType = gReference.getReferenceType();

        return tryParseNodeId(aliases, referenceType);
    }

    private static NodeId tryParseNodeId(Map<String, NodeId> aliases, String id) {
        return NodeId.parseSafe(id).orElseGet(() -> {
            if (aliases.containsKey(id)) {
                return aliases.get(id);
            } else {
                // Ok, last effort...
                Optional<NodeId> nodeId = Arrays.stream(Identifiers.class.getFields())
                    .filter(field -> field.getName().equals(id))
                    .findFirst()
                    .map(field -> {
                        try {
                            return (NodeId) field.get(null);
                        } catch (Throwable ex) {
                            throw new RuntimeException("Couldn't get NodeId field: " + id, ex);
                        }
                    });

                return nodeId.orElseThrow(RuntimeException::new);
            }
        });
    }

    public static DataValue parseValue(Object value, Marshaller marshaller) {
        StringWriter sw = new StringWriter();

        if (value instanceof JAXBElement) {
            JAXBElement<?> jaxbElement = (JAXBElement) value;

            try {
                marshaller.marshal(jaxbElement, sw);
            } catch (JAXBException e) {
                LOGGER.warn("unable to marshal JAXB element: " + jaxbElement, e);
                return new DataValue(Variant.NULL_VALUE);
            }
        } else if (value instanceof Node) {
            Node node = (Node) value;

            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty("omit-xml-declaration", "yes");
                transformer.transform(new DOMSource(node), new StreamResult(sw));
            } catch (TransformerException e) {
                LOGGER.warn("unable to transform dom node: " + node, e);
                return new DataValue(Variant.NULL_VALUE);
            }
        }

        String xmlString = sw.toString();
        try {
            OpcUaXmlStreamDecoder xmlReader = new OpcUaXmlStreamDecoder(SERIALIZATION_CONTEXT);
            xmlReader.setInput(new StringReader(xmlString));

            Object valueObject = xmlReader.readVariantValue();

            return new DataValue(new Variant(valueObject));
        } catch (Throwable t) {
            LOGGER.warn("unable to parse Value: " + xmlString, t);
            return new DataValue(Variant.NULL_VALUE);
        }
    }

    public static UInteger[] parseArrayDimensions(List<String> list) {
        if (list.isEmpty()) {
            return new UInteger[0];
        } else {
            String[] ss = list.get(0).split(",");
            UInteger[] dimensions = new UInteger[ss.length];

            for (int i = 0; i < ss.length; i++) {
                dimensions[i] = uint(Integer.parseInt(ss[i]));
            }

            return dimensions;
        }
    }

}
