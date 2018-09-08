package com.digitalpetri.opcua.nodeset;

import com.google.common.base.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReferenceDetails that = (ReferenceDetails) o;
        return forward == that.forward &&
            Objects.equal(sourceNodeId, that.sourceNodeId) &&
            Objects.equal(targetNodeId, that.targetNodeId) &&
            Objects.equal(referenceTypeId, that.referenceTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sourceNodeId, targetNodeId, referenceTypeId, forward);
    }

}
