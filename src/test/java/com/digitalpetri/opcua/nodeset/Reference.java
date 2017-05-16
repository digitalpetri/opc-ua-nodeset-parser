package com.digitalpetri.opcua.nodeset;

import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

public class Reference {

    private final NodeId sourceNodeId;
    private final NodeId referenceTypeId;
    private final ExpandedNodeId targetNodeId;
    private final NodeClass targetNodeClass;
    private final boolean forward;

    public Reference(NodeId sourceNodeId,
                     NodeId referenceTypeId,
                     ExpandedNodeId targetNodeId,
                     NodeClass targetNodeClass,
                     boolean forward) {
        this.sourceNodeId = sourceNodeId;
        this.referenceTypeId = referenceTypeId;
        this.targetNodeId = targetNodeId;
        this.targetNodeClass = targetNodeClass;
        this.forward = forward;
    }

    @Override
    public String toString() {
        return "Reference{" +
                "sourceNodeId=" + sourceNodeId +
                ", referenceTypeId=" + referenceTypeId +
                ", targetNodeId=" + targetNodeId +
                ", targetNodeClass=" + targetNodeClass +
                ", forward=" + forward +
                '}';
    }

}
