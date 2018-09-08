package com.digitalpetri.opcua.nodeset.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.opcfoundation.ua.generated.LocalizedText;
import org.opcfoundation.ua.generated.UANode;

public abstract class UaNodeAdapter<ValueType extends UaNode, BoundType> extends XmlAdapter<ValueType, BoundType> {

    private final ServerNodeMap nodeMap;

    public UaNodeAdapter(ServerNodeMap nodeMap) {
        this.nodeMap = nodeMap;
    }

    protected ServerNodeMap getNodeMap() {
        return nodeMap;
    }

    protected void setBaseAttributes(UaNode node, UANode generatedNode) {
        generatedNode.setNodeId(node.getNodeId().toParseableString());

        generatedNode.setBrowseName(node.getBrowseName().toParseableString());

        LocalizedText displayName = new LocalizedText();
        displayName.setLocale(node.getDisplayName().getLocale());
        displayName.setValue(node.getDisplayName().getText());
        generatedNode.getDisplayName().add(displayName);

        LocalizedText description = new LocalizedText();
        description.setLocale(node.getDescription().getLocale());
        description.setValue(node.getDescription().getText());
        generatedNode.getDescription().add(description);

        generatedNode.setWriteMask(node.getWriteMask().longValue());

        generatedNode.setUserWriteMask(node.getUserWriteMask().longValue());
    }

}

