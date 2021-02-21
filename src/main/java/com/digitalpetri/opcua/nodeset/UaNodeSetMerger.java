package com.digitalpetri.opcua.nodeset;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.digitalpetri.opcua.nodeset.attributes.DataTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.MethodNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.NodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ObjectNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ObjectTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ReferenceTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.VariableNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.VariableTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ViewNodeAttributes;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.serialization.EncodingLimits;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.types.DataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.OpcUaDataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.util.ArrayUtil;
import org.opcfoundation.ua.generated.DataTypeDefinition;
import org.opcfoundation.ua.generated.DataTypeField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UaNodeSetMerger {

    private static final Logger LOGGER = LoggerFactory.getLogger(UaNodeSetMerger.class);

    private UaNodeSetMerger() {}

    public static UaNodeSet merge(UaNodeSet nodeSet1, UaNodeSet nodeSet2) {
        Map<NodeId, NodeAttributes> nodes = new HashMap<>(nodeSet1.getNodes());
        ListMultimap<NodeId, Reference> explicitReferences =
            ArrayListMultimap.create(nodeSet1.getExplicitReferences());
        ListMultimap<NodeId, Reference> implicitReferences =
            ArrayListMultimap.create(nodeSet1.getImplicitReferences());
        NamespaceTable namespaceTable = nodeSet1.getNamespaceTable();
        Map<String, NodeId> aliasTable = new HashMap<>(nodeSet1.getAliasTable());
        Map<NodeId, DataTypeDefinition> dataTypeDefinitions = new HashMap<>(nodeSet1.getDataTypeDefinitions());
        Map<NodeId, String> rawXmlValues = new HashMap<>(nodeSet1.getRawXmlValues());

        for (String uri : nodeSet2.getNamespaceTable().toArray()) {
            UShort index = namespaceTable.getIndex(uri);
            if (index == null) {
                namespaceTable.addUri(uri);
            }
        }

        nodeSet2.getAliasTable().forEach((alias, nodeId) -> {
            NodeId newNodeId = reindex(
                nodeId,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );

            NodeId collision = aliasTable.putIfAbsent(alias, newNodeId);

            if (collision != null && !Objects.equals(collision, newNodeId)) {
                String warning = String.format(
                    "Alias collision: \"%s\". " +
                        "NodeId in nodeSet1=%s, NodeId in nodeSet2=%s",
                    alias, collision, newNodeId
                );
                System.err.println(warning);
            }
        });

        nodeSet2.getNodes().forEach((nodeId, nodeAttributes) -> {
            NodeAttributes newNodeAttributes = reindex(
                nodeAttributes,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );
            nodes.put(newNodeAttributes.getNodeId(), newNodeAttributes);
        });

        nodeSet2.getExplicitReferences().forEach((nodeId, reference) -> {
            NodeId newNodeId = reindex(
                nodeId,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );

            Reference newReference = reindex(
                reference,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );

            explicitReferences.put(newNodeId, newReference);
        });

        nodeSet2.getImplicitReferences().forEach((nodeId, reference) -> {
            NodeId newNodeId = reindex(
                nodeId,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );

            Reference newReference = reindex(
                reference,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );

            implicitReferences.put(newNodeId, newReference);
        });

        nodeSet2.getDataTypeDefinitions().forEach((nodeId, definition) -> {
            NodeId newNodeId = reindex(
                nodeId,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );

            reindex(definition, aliasTable, namespaceTable, nodeSet2.getNamespaceTable());

            dataTypeDefinitions.put(newNodeId, definition);
        });

        nodeSet2.getRawXmlValues().forEach((nodeId, value) -> {
            NodeId newNodeId = reindex(
                nodeId,
                namespaceTable,
                nodeSet2.getNamespaceTable()
            );

            // TODO reindex value
            rawXmlValues.put(newNodeId, value);
        });

        return new UaNodeSet(
            nodes,
            explicitReferences,
            implicitReferences,
            namespaceTable,
            aliasTable,
            dataTypeDefinitions,
            rawXmlValues
        );
    }

    private static NodeId reindex(
        NodeId nodeId,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        String namespaceUri = originalNamespaceTable.getUri(nodeId.getNamespaceIndex());

        return nodeId.reindex(currentNamespaceTable, namespaceUri);
    }

    /**
     * Re-index {@code expandedNodeId} from its original namespace index to the corresponding
     * index in the server for its namespace URI derived from the original namespace index.
     *
     * @param expandedNodeId         an {@link ExpandedNodeId} from the {@link UaNodeSet}.
     * @param currentNamespaceTable  the current {@link NamespaceTable}.
     * @param originalNamespaceTable the original {@link NamespaceTable}.
     * @return a {@link ExpandedNodeId} that has been re-indexed for the current NamespaceTable.
     */
    private static ExpandedNodeId reindex(
        ExpandedNodeId expandedNodeId,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        if (expandedNodeId.isAbsolute()) {
            return expandedNodeId;
        } else {
            String namespaceUri = originalNamespaceTable.getUri(expandedNodeId.getNamespaceIndex());

            return expandedNodeId.reindex(currentNamespaceTable, namespaceUri);
        }
    }

    private static QualifiedName reindex(
        QualifiedName browseName,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        String namespaceUri = originalNamespaceTable.getUri(browseName.getNamespaceIndex());

        return browseName.reindex(currentNamespaceTable, namespaceUri);
    }

    private static Reference reindex(
        Reference reference,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        String sourceNamespaceUri = originalNamespaceTable.getUri(
            reference.getSourceNodeId().getNamespaceIndex()
        );
        String referenceNamespaceUri = originalNamespaceTable.getUri(
            reference.getReferenceTypeId().getNamespaceIndex()
        );
        String targetNamespaceUri = reference.getTargetNodeId().getNamespaceUri();

        if (targetNamespaceUri == null) {
            targetNamespaceUri = originalNamespaceTable.getUri(
                reference.getTargetNodeId().getNamespaceIndex()
            );
        }

        return reference.reindex(currentNamespaceTable, sourceNamespaceUri, referenceNamespaceUri, targetNamespaceUri);
    }

    private static NodeAttributes reindex(
        NodeAttributes nodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        if (nodeAttributes instanceof DataTypeNodeAttributes) {
            return reindex((DataTypeNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else if (nodeAttributes instanceof MethodNodeAttributes) {
            return reindex((MethodNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else if (nodeAttributes instanceof ObjectNodeAttributes) {
            return reindex((ObjectNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else if (nodeAttributes instanceof ObjectTypeNodeAttributes) {
            return reindex((ObjectTypeNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else if (nodeAttributes instanceof ReferenceTypeNodeAttributes) {
            return reindex((ReferenceTypeNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else if (nodeAttributes instanceof VariableNodeAttributes) {
            return reindex((VariableNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else if (nodeAttributes instanceof VariableTypeNodeAttributes) {
            return reindex((VariableTypeNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else if (nodeAttributes instanceof ViewNodeAttributes) {
            return reindex((ViewNodeAttributes) nodeAttributes, currentNamespaceTable, originalNamespaceTable);
        } else {
            throw new IllegalArgumentException("nodeAttributes: " + nodeAttributes);
        }
    }

    private static NodeAttributes reindex(
        DataTypeNodeAttributes dataTypeNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            dataTypeNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            dataTypeNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new DataTypeNodeAttributes(
            newNodeId,
            newBrowseName,
            dataTypeNodeAttributes.getDisplayName(),
            dataTypeNodeAttributes.getDescription(),
            dataTypeNodeAttributes.getWriteMask(),
            dataTypeNodeAttributes.getUserWriteMask(),
            dataTypeNodeAttributes.isAbstract()
        );
    }

    private static NodeAttributes reindex(
        MethodNodeAttributes methodNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            methodNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            methodNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new MethodNodeAttributes(
            newNodeId,
            newBrowseName,
            methodNodeAttributes.getDisplayName(),
            methodNodeAttributes.getDescription(),
            methodNodeAttributes.getWriteMask(),
            methodNodeAttributes.getUserWriteMask(),
            methodNodeAttributes.isExecutable(),
            methodNodeAttributes.isUserExecutable()
        );
    }

    private static NodeAttributes reindex(
        ObjectNodeAttributes objectNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            objectNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            objectNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new ObjectNodeAttributes(
            newNodeId,
            newBrowseName,
            objectNodeAttributes.getDisplayName(),
            objectNodeAttributes.getDescription(),
            objectNodeAttributes.getWriteMask(),
            objectNodeAttributes.getUserWriteMask(),
            objectNodeAttributes.getEventNotifier()
        );
    }

    private static ObjectTypeNodeAttributes reindex(
        ObjectTypeNodeAttributes objectTypeNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            objectTypeNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            objectTypeNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new ObjectTypeNodeAttributes(
            newNodeId,
            newBrowseName,
            objectTypeNodeAttributes.getDisplayName(),
            objectTypeNodeAttributes.getDescription(),
            objectTypeNodeAttributes.getWriteMask(),
            objectTypeNodeAttributes.getUserWriteMask(),
            objectTypeNodeAttributes.isAbstract()
        );
    }

    private static ReferenceTypeNodeAttributes reindex(
        ReferenceTypeNodeAttributes referenceTypeNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            referenceTypeNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            referenceTypeNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new ReferenceTypeNodeAttributes(
            newNodeId,
            newBrowseName,
            referenceTypeNodeAttributes.getDisplayName(),
            referenceTypeNodeAttributes.getDescription(),
            referenceTypeNodeAttributes.getWriteMask(),
            referenceTypeNodeAttributes.getUserWriteMask(),
            referenceTypeNodeAttributes.isAbstract(),
            referenceTypeNodeAttributes.isSymmetric(),
            referenceTypeNodeAttributes.getInverseName()
        );
    }

    private static VariableNodeAttributes reindex(
        VariableNodeAttributes variableNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            variableNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            variableNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        NodeId newDataTypeId = reindex(
            variableNodeAttributes.getDataType(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        DataValue newValue = reindex(
            variableNodeAttributes.getValue(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new VariableNodeAttributes(
            newNodeId,
            newBrowseName,
            variableNodeAttributes.getDisplayName(),
            variableNodeAttributes.getDescription(),
            variableNodeAttributes.getWriteMask(),
            variableNodeAttributes.getUserWriteMask(),
            newValue,
            newDataTypeId,
            variableNodeAttributes.getValueRank(),
            variableNodeAttributes.getArrayDimensions(),
            variableNodeAttributes.getAccessLevel(),
            variableNodeAttributes.getUserAccessLevel(),
            variableNodeAttributes.getMinimumSamplingInterval(),
            variableNodeAttributes.isHistorizing()
        );
    }

    private static VariableTypeNodeAttributes reindex(
        VariableTypeNodeAttributes variableTypeNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            variableTypeNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            variableTypeNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        NodeId newDataTypeId = reindex(
            variableTypeNodeAttributes.getDataType(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        DataValue newValue = reindex(
            variableTypeNodeAttributes.getValue(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new VariableTypeNodeAttributes(
            newNodeId,
            newBrowseName,
            variableTypeNodeAttributes.getDisplayName(),
            variableTypeNodeAttributes.getDescription(),
            variableTypeNodeAttributes.getWriteMask(),
            variableTypeNodeAttributes.getUserWriteMask(),
            newValue,
            newDataTypeId,
            variableTypeNodeAttributes.getValueRank(),
            variableTypeNodeAttributes.getArrayDimensions(),
            variableTypeNodeAttributes.isAbstract()
        );
    }

    private static ViewNodeAttributes reindex(
        ViewNodeAttributes viewNodeAttributes,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        NodeId newNodeId = reindex(
            viewNodeAttributes.getNodeId(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        QualifiedName newBrowseName = reindex(
            viewNodeAttributes.getBrowseName(),
            currentNamespaceTable,
            originalNamespaceTable
        );

        return new ViewNodeAttributes(
            newNodeId,
            newBrowseName,
            viewNodeAttributes.getDisplayName(),
            viewNodeAttributes.getDescription(),
            viewNodeAttributes.getWriteMask(),
            viewNodeAttributes.getUserWriteMask(),
            viewNodeAttributes.isContainsNoLoops(),
            viewNodeAttributes.getEventNotifier()
        );
    }

    private static void reindex(
        DataTypeDefinition definition,
        Map<String, NodeId> aliasTable,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        QualifiedName newName = reindex(
            QualifiedName.parse(definition.getName()),
            currentNamespaceTable,
            originalNamespaceTable
        );

        definition.setName(newName.toParseableString());

        definition.getField().forEach(field ->
            reindex(
                field,
                aliasTable,
                currentNamespaceTable,
                originalNamespaceTable
            )
        );
    }

    private static void reindex(
        DataTypeField field,
        Map<String, NodeId> aliasTable,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        QualifiedName newName = reindex(
            QualifiedName.parse(field.getName()),
            currentNamespaceTable,
            originalNamespaceTable
        );
        field.setName(newName.toParseableString());

        String dataType = field.getDataType();
        if (dataType != null) {
            NodeId nodeId = aliasTable.get(dataType);
            if (nodeId == null) nodeId = NodeId.parse(dataType);
            NodeId newDataType = reindex(
                nodeId,
                currentNamespaceTable,
                originalNamespaceTable
            );
            field.setDataType(newDataType.toParseableString());
        }

        DataTypeDefinition innerDefinition = field.getDefinition();
        if (innerDefinition != null) {
            reindex(innerDefinition, aliasTable, currentNamespaceTable, originalNamespaceTable);
        }
    }

    /**
     * Re-indexes a {@link DataValue} if necessary.
     * <p>
     * If {@code value} contains an ExtensionObject the encodingId is re-indexed. Then the struct is decoded and any
     * fields that qualify are also re-indexed (e.g. the dataType field in {@link Argument}).
     * <p>
     * This is verging on major hack because the OPC UA modelling concept is somewhat flawed when it comes to encoding
     * embedded values that reference non-absolute namespaces.
     *
     * @param value the {@link DataValue} to re-index.
     * @return a {@link DataValue} that has been re-indexed for the current server.
     */
    private static DataValue reindex(
        DataValue value,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        try {
            if (value == null) return null;
            Variant variant = value.getValue();
            if (variant == null) return value;
            Object o = variant.getValue();
            if (o == null) return value;
            return new DataValue(new Variant(reindexValue(o, currentNamespaceTable, originalNamespaceTable)));
        } catch (Throwable t) {
            LOGGER.warn("Re-indexing failed: {}", value, t);
            return value;
        }
    }

    private static Object reindexValue(
        Object value,
        NamespaceTable currentNamespaceTable,
        NamespaceTable originalNamespaceTable
    ) {

        if (value == null) return null;

        Class<?> clazz = value.getClass();

        if (clazz.isArray()) {
            @SuppressWarnings("rawtypes")
            Class componentType = ArrayUtil.getType(value);

            if (componentType != NodeId.class
                && componentType != ExpandedNodeId.class
                && componentType != QualifiedName.class
                && componentType != ExtensionObject.class
            ) {

                return value;
            } else {
                //noinspection unchecked
                return ArrayUtil.transformArray(
                    value,
                    o -> reindexValue(o, currentNamespaceTable, originalNamespaceTable),
                    componentType
                );
            }
        } else {
            if (clazz == NodeId.class) {
                return reindex((NodeId) value, currentNamespaceTable, originalNamespaceTable);
            } else if (clazz == ExpandedNodeId.class) {
                return reindex((ExpandedNodeId) value, currentNamespaceTable, originalNamespaceTable);
            } else if (clazz == QualifiedName.class) {
                return reindex((QualifiedName) value, currentNamespaceTable, originalNamespaceTable);
            } else if (clazz == ExtensionObject.class) {
                ExtensionObject xo = (ExtensionObject) value;

                if (xo.getBodyType() == ExtensionObject.BodyType.ByteString) {
                    xo = new ExtensionObject(
                        (ByteString) xo.getBody(),
                        reindex(xo.getEncodingId(), currentNamespaceTable, originalNamespaceTable)
                    );
                } else if (xo.getBodyType() == ExtensionObject.BodyType.XmlElement) {
                    xo = new ExtensionObject(
                        (XmlElement) xo.getBody(),
                        reindex(xo.getEncodingId(), currentNamespaceTable, originalNamespaceTable)
                    );
                }

                try {
                    Object struct = xo.decode(SERIALIZATION_CONTEXT);

                    if (struct instanceof Argument) {
                        Argument argument = (Argument) struct;

                        return ExtensionObject.encode(
                            SERIALIZATION_CONTEXT,
                            new Argument(
                                argument.getName(),
                                reindex(argument.getDataType(), currentNamespaceTable, originalNamespaceTable),
                                argument.getValueRank(),
                                argument.getArrayDimensions(),
                                argument.getDescription()
                            )
                        );
                    } else {
                        return xo;
                    }
                } catch (Throwable t) {
                    LOGGER.warn("Decoding failed: {}", xo, t);
                    return xo;
                }
            } else {
                return value;
            }
        }
    }

    /**
     * A default {@link SerializationContext} that can be used to decode OPC UA built-in types.
     */
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

}
