package com.digitalpetri.opcua.nodeset.attributes;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.opcfoundation.ua.generated.UAReferenceType;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class ReferenceTypeNodeAttributes extends NodeAttributes {

    private final boolean isAbstract;
    private final boolean symmetric;
    private final LocalizedText inverseName;

    public ReferenceTypeNodeAttributes(
        NodeId nodeId,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask,
        boolean isAbstract,
        boolean symmetric,
        LocalizedText inverseName
    ) {

        super(nodeId, NodeClass.ReferenceType, browseName, displayName, description, writeMask, userWriteMask);

        this.isAbstract = isAbstract;
        this.symmetric = symmetric;
        this.inverseName = inverseName;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isSymmetric() {
        return symmetric;
    }

    public LocalizedText getInverseName() {
        return inverseName;
    }

    @Override
    public String toString() {
        return "ReferenceTypeNodeAttributes{" +
            "isAbstract=" + isAbstract +
            ", symmetric=" + symmetric +
            ", inverseName=" + inverseName +
            "} " + super.toString();
    }

    public static ReferenceTypeNodeAttributes fromGenerated(UAReferenceType gNode) {
        NodeId nodeId = NodeId.parse(gNode.getNodeId());
        QualifiedName browseName = QualifiedName.parse(gNode.getBrowseName());

        LocalizedText displayName = gNode.getDisplayName().stream()
            .findFirst()
            .map(gLocalizedText -> LocalizedText.english(gLocalizedText.getValue()))
            .orElse(LocalizedText.english(browseName.getName()));

        LocalizedText description = gNode.getDescription().stream()
            .findFirst()
            .map(gLocalizedText -> LocalizedText.english(gLocalizedText.getValue()))
            .orElse(LocalizedText.NULL_VALUE);

        UInteger writeMask = uint(gNode.getWriteMask());
        UInteger userWriteMask = uint(gNode.getUserWriteMask());

        boolean isAbstract = gNode.isIsAbstract();
        boolean symmetric = gNode.isSymmetric();

        LocalizedText inverseName = gNode.getInverseName().stream()
            .findFirst()
            .map(gLocalizedText -> LocalizedText.english(gLocalizedText.getValue()))
            .orElse(LocalizedText.NULL_VALUE);

        return new ReferenceTypeNodeAttributes(
            nodeId,
            browseName,
            displayName,
            description,
            writeMask,
            userWriteMask,
            isAbstract,
            symmetric,
            inverseName
        );
    }

}
