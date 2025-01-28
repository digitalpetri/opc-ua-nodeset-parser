package com.digitalpetri.opcua.nodeset.attributes;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.opcfoundation.ua.generated.UAObject;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class ObjectNodeAttributes extends NodeAttributes {

    private final UByte eventNotifier;

    public ObjectNodeAttributes(
        NodeId nodeId,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask,
        UByte eventNotifier
    ) {

        super(nodeId, NodeClass.Object, browseName, displayName, description, writeMask, userWriteMask);

        this.eventNotifier = eventNotifier;
    }

    public UByte getEventNotifier() {
        return eventNotifier;
    }

    @Override
    public String toString() {
        return "ObjectNodeAttributes{" +
            "eventNotifier=" + eventNotifier +
            "} " + super.toString();
    }

    public static ObjectNodeAttributes fromGenerated(UAObject gNode) {
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

        UByte eventNotifier = ubyte(gNode.getEventNotifier());

        return new ObjectNodeAttributes(
            nodeId,
            browseName,
            displayName,
            description,
            writeMask,
            userWriteMask,
            eventNotifier
        );
    }

}
