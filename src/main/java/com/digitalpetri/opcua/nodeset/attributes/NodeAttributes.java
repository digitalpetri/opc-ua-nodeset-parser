package com.digitalpetri.opcua.nodeset.attributes;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

public abstract class NodeAttributes {

    private final NodeId nodeId;
    private final NodeClass nodeClass;
    private final QualifiedName browseName;
    private final LocalizedText displayName;
    private final LocalizedText description;
    private final UInteger writeMask;
    private final UInteger userWriteMask;

    public NodeAttributes(
        NodeId nodeId,
        NodeClass nodeClass,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask
    ) {

        this.nodeId = nodeId;
        this.nodeClass = nodeClass;
        this.browseName = browseName;
        this.displayName = displayName;
        this.description = description;
        this.writeMask = writeMask;
        this.userWriteMask = userWriteMask;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public NodeClass getNodeClass() {
        return nodeClass;
    }

    public QualifiedName getBrowseName() {
        return browseName;
    }

    public LocalizedText getDisplayName() {
        return displayName;
    }

    public LocalizedText getDescription() {
        return description;
    }

    public UInteger getWriteMask() {
        return writeMask;
    }

    public UInteger getUserWriteMask() {
        return userWriteMask;
    }

    @Override
    public String toString() {
        return "NodeAttributes{" +
            "nodeId=" + nodeId +
            ", nodeClass=" + nodeClass +
            ", browseName=" + browseName +
            ", displayName=" + displayName +
            ", description=" + description +
            ", writeMask=" + writeMask +
            ", userWriteMask=" + userWriteMask +
            '}';
    }

}
