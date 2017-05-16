package com.digitalpetri.opcua.nodeset;

import java.io.InputStream;
import java.util.ArrayList;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.opcfoundation.ua.generated.GeneratedReference;
import org.opcfoundation.ua.generated.GeneratedUADataType;
import org.opcfoundation.ua.generated.GeneratedUAMethod;
import org.opcfoundation.ua.generated.GeneratedUAObject;
import org.opcfoundation.ua.generated.GeneratedUAObjectType;
import org.opcfoundation.ua.generated.GeneratedUAReferenceType;
import org.opcfoundation.ua.generated.GeneratedUAVariable;
import org.opcfoundation.ua.generated.GeneratedUAVariableType;
import org.opcfoundation.ua.generated.GeneratedUAView;
import org.opcfoundation.ua.generated.ObjectFactory;
import org.opcfoundation.ua.generated.UANodeSet;

public class UaNodeSet {

    private static final String OPC_UA_NAMESPACE = "http://opcfoundation.org/UA/";

    private final Map<String, NodeId> aliasMap = new HashMap<>();
    private final List<String> namespaceUris = new ArrayList<>();

    private final Map<NodeId, NodeAttributes> nodeAttributes = new HashMap<>();
    private final ListMultimap<NodeId, ReferenceDetails> referenceDetails = ArrayListMultimap.create();

    private final Marshaller marshaller;

    private final UANodeSet nodeSet;

    UaNodeSet(UANodeSet nodeSet) throws JAXBException {
        this.nodeSet = nodeSet;

        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        marshaller = jaxbContext.createMarshaller();

        // Alias Map
        nodeSet.getAliases().getAlias().forEach(
            a -> aliasMap.put(a.getAlias(), NodeId.parse(a.getValue()))
        );

        // Namespace URI List
        namespaceUris.add(OPC_UA_NAMESPACE);
        if (nodeSet.getNamespaceUris() != null) {
            namespaceUris.addAll(nodeSet.getNamespaceUris().getUri());
        }

        // Reference Details
        nodeSet.getUAObjectOrUAVariableOrUAMethod().forEach(gNode -> {
            NodeId sourceNodeId = NodeId.parse(gNode.getNodeId());

            gNode.getReferences().getReference().forEach(gReference -> {
                referenceDetails.put(sourceNodeId, referenceDetails(sourceNodeId, gReference));
            });
        });

        // Node Attributes
        nodeSet.getUAObjectOrUAVariableOrUAMethod().forEach(gNode -> {
            NodeAttributes attributes = null;

            if (gNode instanceof GeneratedUADataType) {
                attributes = DataTypeNodeAttributes
                    .fromGenerated((GeneratedUADataType) gNode);
            } else if (gNode instanceof GeneratedUAMethod) {
                attributes = MethodNodeAttributes
                    .fromGenerated((GeneratedUAMethod) gNode);
            } else if (gNode instanceof GeneratedUAObject) {
                attributes = ObjectNodeAttributes
                    .fromGenerated((GeneratedUAObject) gNode);
            } else if (gNode instanceof GeneratedUAObjectType) {
                attributes = ObjectTypeNodeAttributes
                    .fromGenerated((GeneratedUAObjectType) gNode);
            } else if (gNode instanceof GeneratedUAReferenceType) {
                attributes = ReferenceTypeNodeAttributes
                    .fromGenerated((GeneratedUAReferenceType) gNode);
            } else if (gNode instanceof GeneratedUAVariable) {
                attributes = VariableNodeAttributes
                    .fromGenerated((GeneratedUAVariable) gNode, marshaller, aliasMap);
            } else if (gNode instanceof GeneratedUAVariableType) {
                attributes = VariableTypeNodeAttributes
                    .fromGenerated((GeneratedUAVariableType) gNode, marshaller, aliasMap);
            } else if (gNode instanceof GeneratedUAView) {
                attributes = ViewNodeAttributes
                    .fromGenerated((GeneratedUAView) gNode);
            }

            if (attributes != null) {
                nodeAttributes.put(attributes.getNodeId(), attributes);
            }
        });
    }

    public UANodeSet getNodeSet() {
        return nodeSet;
    }

    public Map<String, NodeId> getAliasMap() {
        return aliasMap;
    }

    public List<String> getNamespaceUris() {
        return namespaceUris;
    }

    public Map<NodeId, NodeAttributes> getNodeAttributes() {
        return nodeAttributes;
    }

    public ListMultimap<NodeId, ReferenceDetails> getReferenceDetails() {
        return referenceDetails;
    }

    private ReferenceDetails referenceDetails(NodeId sourceNodeId, GeneratedReference gReference) {
        NodeId targetNodeId = NodeId.parse(gReference.getValue());
        NodeId referenceTypeId = AttributeUtil.parseReferenceTypeId(gReference, aliasMap);
        boolean forward = gReference.isIsForward();

        return new ReferenceDetails(
            sourceNodeId,
            targetNodeId,
            referenceTypeId,
            forward
        );
    }

    public static UaNodeSet parse(InputStream nodeSetXml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);

        UANodeSet nodeSet = UANodeSet.class.cast(jaxbContext.createUnmarshaller().unmarshal(nodeSetXml));

        return new UaNodeSet(nodeSet);
    }

}
