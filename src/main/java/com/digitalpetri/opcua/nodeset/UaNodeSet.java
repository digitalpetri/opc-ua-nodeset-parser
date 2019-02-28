package com.digitalpetri.opcua.nodeset;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String OPC_UA_NAMESPACE = "http://opcfoundation.org/UA/";

    private final Map<NodeId, NodeAttributes> nodeAttributes;
    private final ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> referenceDetails;
    private final NamespaceTable namespaceTable;
    private final Map<String, NodeId> aliasMap;
    private final Map<NodeId, DataTypeDefinition> dataTypeDefinitions;

    public UaNodeSet(
        Map<NodeId, NodeAttributes> nodeAttributes,
        ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> referenceDetails,
        NamespaceTable namespaceTable,
        Map<String, NodeId> aliasMap,
        Map<NodeId, DataTypeDefinition> dataTypeDefinitions) {

        this.nodeAttributes = nodeAttributes;
        this.referenceDetails = referenceDetails;
        this.namespaceTable = namespaceTable;
        this.aliasMap = aliasMap;
        this.dataTypeDefinitions = dataTypeDefinitions;
    }

    UaNodeSet(UANodeSet nodeSet) throws JAXBException {
        aliasMap = new HashMap<>();
        namespaceTable = new NamespaceTable();
        referenceDetails = ArrayListMultimap.create();
        nodeAttributes = new HashMap<>();
        dataTypeDefinitions = new HashMap<>();

        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        Marshaller marshaller = jaxbContext.createMarshaller();

        // Alias Map
        AliasTable aliasTable = nodeSet.getAliases();
        if (aliasTable != null) {
            List<NodeIdAlias> aliases = aliasTable.getAlias();
            if (aliases != null) {
                aliases.forEach(a -> aliasMap.put(a.getAlias(), NodeId.parse(a.getValue())));
            }
        }

        // Namespace URI List
        if (nodeSet.getNamespaceUris() != null) {
            List<String> uris = nodeSet.getNamespaceUris().getUri();
            uris.forEach(namespaceTable::addUri);
        }

        // Reference Details
        nodeSet.getUAObjectOrUAVariableOrUAMethod().forEach(gNode -> {
            NodeId sourceNodeId = NodeId.parse(gNode.getNodeId());

            gNode.getReferences().getReference().forEach(
                gReference -> {
                    org.eclipse.milo.opcua.sdk.core.Reference reference = reference(sourceNodeId, gReference);
                    referenceDetails.put(sourceNodeId, reference);

                    reference.invert().ifPresent(
                        inverse ->
                            referenceDetails.put(inverse.getSourceNodeId(), inverse)
                    );

                }
            );
        });

        // Node Attributes
        nodeSet.getUAObjectOrUAVariableOrUAMethod().forEach(gNode -> {
            NodeAttributes attributes = null;

            if (gNode instanceof UADataType) {
                UADataType gDataTypeNode = (UADataType) gNode;

                attributes = DataTypeNodeAttributes.fromGenerated(gDataTypeNode);

                DataTypeDefinition definition = gDataTypeNode.getDefinition();

                dataTypeDefinitions.put(attributes.getNodeId(), definition);
            } else if (gNode instanceof UAMethod) {
                attributes = MethodNodeAttributes.fromGenerated((UAMethod) gNode);
            } else if (gNode instanceof UAObject) {
                attributes = ObjectNodeAttributes.fromGenerated((UAObject) gNode);
            } else if (gNode instanceof UAObjectType) {
                attributes = ObjectTypeNodeAttributes.fromGenerated((UAObjectType) gNode);
            } else if (gNode instanceof UAReferenceType) {
                attributes = ReferenceTypeNodeAttributes.fromGenerated((UAReferenceType) gNode);
            } else if (gNode instanceof UAVariable) {
                attributes = VariableNodeAttributes.fromGenerated((UAVariable) gNode, marshaller, aliasMap);
            } else if (gNode instanceof UAVariableType) {
                attributes = VariableTypeNodeAttributes.fromGenerated((UAVariableType) gNode, marshaller, aliasMap);
            } else if (gNode instanceof UAView) {
                attributes = ViewNodeAttributes.fromGenerated((UAView) gNode);
            }

            if (attributes != null) {
                nodeAttributes.put(attributes.getNodeId(), attributes);
            }
        });
    }

    public Map<String, NodeId> getAliasMap() {
        return aliasMap;
    }

    public NamespaceTable getNamespaceTable() {
        return namespaceTable;
    }

    public Map<NodeId, NodeAttributes> getNodeAttributes() {
        return nodeAttributes;
    }

    public ListMultimap<NodeId, org.eclipse.milo.opcua.sdk.core.Reference> getReferenceDetails() {
        return referenceDetails;
    }

    public Map<NodeId, DataTypeDefinition> getDataTypeDefinitions() {
        return dataTypeDefinitions;
    }

    private org.eclipse.milo.opcua.sdk.core.Reference reference(NodeId sourceNodeId, Reference gReference) {
        NodeId targetNodeId = NodeId.parse(gReference.getValue());
        NodeId referenceTypeId = AttributeUtil.parseReferenceTypeId(gReference, aliasMap);
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
