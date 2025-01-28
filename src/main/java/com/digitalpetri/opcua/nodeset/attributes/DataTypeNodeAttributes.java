package com.digitalpetri.opcua.nodeset.attributes;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.opcfoundation.ua.generated.UADataType;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class DataTypeNodeAttributes extends NodeAttributes {

    private final boolean isAbstract;

    public DataTypeNodeAttributes(
        NodeId nodeId,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask,
        boolean isAbstract
    ) {

        super(nodeId, NodeClass.DataType, browseName, displayName, description, writeMask, userWriteMask);

        this.isAbstract = isAbstract;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public String toString() {
        return "DataTypeNodeAttributes{" +
            "isAbstract=" + isAbstract +
            "} " + super.toString();
    }

    public static DataTypeNodeAttributes fromGenerated(UADataType gNode) {
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

        return new DataTypeNodeAttributes(
            nodeId,
            browseName,
            displayName,
            description,
            writeMask,
            userWriteMask,
            isAbstract
        );
    }

}
