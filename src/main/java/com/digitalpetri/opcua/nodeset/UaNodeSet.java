package com.digitalpetri.opcua.nodeset;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.digitalpetri.opcua.nodeset.attributes.DataTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.MethodNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.NodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ObjectNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ObjectTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ReferenceTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.VariableNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.VariableTypeNodeAttributes;
import com.digitalpetri.opcua.nodeset.attributes.ViewNodeAttributes;
import com.digitalpetri.opcua.nodeset.util.AttributeUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.opcfoundation.ua.generated.AliasTable;
import org.opcfoundation.ua.generated.DataTypeDefinition;
import org.opcfoundation.ua.generated.NodeIdAlias;
import org.opcfoundation.ua.generated.ObjectFactory;
import org.opcfoundation.ua.generated.Reference;
import org.opcfoundation.ua.generated.UADataType;
import org.opcfoundation.ua.generated.UAMethod;
import org.opcfoundation.ua.generated.UANodeSet;
import org.opcfoundation.ua.generated.UAObject;
import org.opcfoundation.ua.generated.UAObjectType;
import org.opcfoundation.ua.generated.UAReferenceType;
import org.opcfoundation.ua.generated.UAVariable;
import org.opcfoundation.ua.generated.UAVariableType;
import org.opcfoundation.ua.generated.UAView;

public class UaNodeSet {

    private ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> combinedReferences;

    private final Map<NodeId, NodeAttributes> nodes;
    private final ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> explicitReferences;
    private final ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> implicitReferences;
    private final NamespaceTable namespaceTable;
    private final Map<String, NodeId> aliasTable;
    private final Map<NodeId, DataTypeDefinition> dataTypeDefinitions;
    private final Map<NodeId, String> rawXmlValues;

    public UaNodeSet(
        Map<NodeId, NodeAttributes> nodes,
        ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> explicitReferences,
        ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> implicitReferences,
        NamespaceTable namespaceTable,
        Map<String, NodeId> aliasTable,
        Map<NodeId, DataTypeDefinition> dataTypeDefinitions,
        Map<NodeId, String> rawXmlValues
    ) {

        this.nodes = nodes;
        this.explicitReferences = explicitReferences;
        this.implicitReferences = implicitReferences;
        this.namespaceTable = namespaceTable;
        this.aliasTable = aliasTable;
        this.dataTypeDefinitions = dataTypeDefinitions;
        this.rawXmlValues = rawXmlValues;
    }

    UaNodeSet(UANodeSet nodeSet) throws JAXBException {
        aliasTable = new HashMap<>();
        namespaceTable = new NamespaceTable();
        explicitReferences = ArrayListMultimap.create();
        implicitReferences = ArrayListMultimap.create();
        nodes = new HashMap<>();
        dataTypeDefinitions = new HashMap<>();
        rawXmlValues = new HashMap<>();

        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Marshaller marshaller = jaxbContext.createMarshaller();

        // Alias Map
        AliasTable aliasTable = nodeSet.getAliases();
        if (aliasTable != null) {
            List<NodeIdAlias> aliases = aliasTable.getAlias();
            if (aliases != null) {
                aliases.forEach(a -> this.aliasTable.put(a.getAlias(), NodeId.parse(a.getValue())));
            }
        }

        // Namespace URI List
        if (nodeSet.getNamespaceUris() != null) {
            List<String> uris = nodeSet.getNamespaceUris().getUri();
            uris.forEach(namespaceTable::addUri);
        }

        // Reference Details
        nodeSet.getUAObjectOrUAVariableOrUAMethod().forEach(gNode -> {
            NodeId sourceNodeId = AttributeUtil.tryParseNodeId(gNode.getNodeId(), this.aliasTable);

            Optional.ofNullable(gNode.getReferences()).ifPresent(gReferences -> gReferences.getReference().forEach(
                gReference -> {
                    org.eclipse.milo.opcua.sdk.core.Reference reference =
                        referenceFromGenerated(sourceNodeId, gReference);

                    explicitReferences.put(sourceNodeId, reference);

                    reference.invert(namespaceTable).ifPresent(
                        inverseReference ->
                            implicitReferences.put(inverseReference.getSourceNodeId(), inverseReference)
                    );
                }
            ));
        });

        // Node Attributes
        nodeSet.getUAObjectOrUAVariableOrUAMethod().forEach(gNode -> {
            NodeAttributes attributes = null;

            if (gNode instanceof UADataType) {
                UADataType gDataTypeNode = (UADataType) gNode;

                attributes = DataTypeNodeAttributes.fromGenerated(gDataTypeNode);

                DataTypeDefinition definition = gDataTypeNode.getDefinition();

                if (definition != null) {
                    dataTypeDefinitions.put(attributes.getNodeId(), definition);
                }
            } else if (gNode instanceof UAMethod) {
                attributes = MethodNodeAttributes.fromGenerated((UAMethod) gNode);
            } else if (gNode instanceof UAObject) {
                attributes = ObjectNodeAttributes.fromGenerated((UAObject) gNode);
            } else if (gNode instanceof UAObjectType) {
                attributes = ObjectTypeNodeAttributes.fromGenerated((UAObjectType) gNode);
            } else if (gNode instanceof UAReferenceType) {
                attributes = ReferenceTypeNodeAttributes.fromGenerated((UAReferenceType) gNode);
            } else if (gNode instanceof UAVariable) {
                attributes = VariableNodeAttributes.fromGenerated(
                    (UAVariable) gNode, marshaller, this.aliasTable, rawXmlValues
                );
            } else if (gNode instanceof UAVariableType) {
                attributes = VariableTypeNodeAttributes.fromGenerated(
                    (UAVariableType) gNode, marshaller, this.aliasTable, rawXmlValues
                );
            } else if (gNode instanceof UAView) {
                attributes = ViewNodeAttributes.fromGenerated((UAView) gNode);
            }

            if (attributes != null) {
                nodes.put(attributes.getNodeId(), attributes);
            }
        });
    }

    public Map<String, NodeId> getAliasTable() {
        return aliasTable;
    }

    public NamespaceTable getNamespaceTable() {
        return namespaceTable;
    }

    public Map<NodeId, NodeAttributes> getNodes() {
        return nodes;
    }

    /**
     * Get the {@link Reference}s that were explicitly defined by the NodeSet.
     *
     * @return the {@link Reference}s that were explicitly defined by the NodeSet.
     */
    public ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> getExplicitReferences() {
        return explicitReferences;
    }

    /**
     * Get the {@link Reference}s that were implicitly defined by the NodeSet, i.e. they were derived by inverting an
     * explicitly defined reference.
     *
     * @return the {@link Reference}s that were implicitly defined by the NodeSet
     */
    public ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> getImplicitReferences() {
        return implicitReferences;
    }

    /**
     * Get all {@link Reference}s defined by the NodeSet, i.e. both explicit and implicit
     * references.
     *
     * @return all {@link Reference}s defined by the NodeSet.
     * @see #getExplicitReferences()
     * @see #getImplicitReferences()
     */
    public synchronized ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> getCombinedReferences() {
        if (combinedReferences == null) {
            combinedReferences = ArrayListMultimap.create();
            combinedReferences.putAll(explicitReferences);
            combinedReferences.putAll(implicitReferences);
        }

        return combinedReferences;
    }

    public Map<NodeId, DataTypeDefinition> getDataTypeDefinitions() {
        return dataTypeDefinitions;
    }

    public Map<NodeId, String> getRawXmlValues() {
        return rawXmlValues;
    }

    /**
     * Merge another {@link UaNodeSet} into this one and return a new {@link UaNodeSet} with the
     * contents of both.
     *
     * @param other the {@link UaNodeSet} to merge.
     * @return a new {@link UANodeSet} containing the contents of {@code other} merged into this.
     */
    public UaNodeSet merge(UaNodeSet other) {
        return UaNodeSetMerger.merge(this, other);
    }

    private org.eclipse.milo.opcua.sdk.core.Reference referenceFromGenerated(
        NodeId sourceNodeId,
        Reference gReference
    ) {

        NodeId targetNodeId = AttributeUtil.tryParseNodeId(gReference.getValue(), aliasTable);
        NodeId referenceTypeId = AttributeUtil.parseReferenceTypeId(gReference, aliasTable);
        boolean isForward = gReference.isIsForward();

        return new org.eclipse.milo.opcua.sdk.core.Reference(
            sourceNodeId,
            referenceTypeId,
            targetNodeId.expanded(),
            isForward
        );
    }

    public static UaNodeSet parse(InputStream nodeSetXml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);

        UANodeSet nodeSet = (UANodeSet) jaxbContext.createUnmarshaller().unmarshal(nodeSetXml);

        return new UaNodeSet(nodeSet);
    }

}
