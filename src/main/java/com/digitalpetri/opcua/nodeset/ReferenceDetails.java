package com.digitalpetri.opcua.nodeset;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class ReferenceDetails {
    private final NodeId sourceNodeId;
    private final NodeId targetNodeId;
    private final NodeId referenceTypeId;
    private final boolean forward;

    public ReferenceDetails(NodeId sourceNodeId, NodeId targetNodeId, NodeId referenceTypeId, boolean forward) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.referenceTypeId = referenceTypeId;
        this.forward = forward;
    }

    public NodeId getSourceNodeId() {
        return sourceNodeId;
    }

    public NodeId getTargetNodeId() {
        return targetNodeId;
    }

    public NodeId getReferenceTypeId() {
        return referenceTypeId;
    }

    public boolean isForward() {
        return forward;
    }

}
