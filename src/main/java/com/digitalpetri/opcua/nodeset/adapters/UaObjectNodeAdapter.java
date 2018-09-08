package com.digitalpetri.opcua.nodeset.adapters;

import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.opcfoundation.ua.generated.UAObject;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class UaObjectNodeAdapter extends UaNodeAdapter<UaObjectNode, UAObject> {

    public UaObjectNodeAdapter(ServerNodeMap nodeMap) {
        super(nodeMap);
    }

    @Override
    public UaObjectNode marshal(UAObject object) throws Exception {
        NodeId nodeId = NodeId.parse(object.getNodeId());
        QualifiedName browseName = QualifiedName.parse(object.getBrowseName());

        LocalizedText displayName = object.getDisplayName().stream()
            .findFirst()
            .map(gLocalizedText -> LocalizedText.english(gLocalizedText.getValue()))
            .orElse(LocalizedText.english(browseName.getName()));

        LocalizedText description = object.getDescription().stream()
            .findFirst()
            .map(gLocalizedText -> LocalizedText.english(gLocalizedText.getValue()))
            .orElse(LocalizedText.NULL_VALUE);

        UInteger writeMask = uint(object.getWriteMask());
        UInteger userWriteMask = uint(object.getUserWriteMask());

        UByte eventNotifier = ubyte(object.getEventNotifier());

        return new UaObjectNode(
            getNodeMap(),
            nodeId,
            browseName,
            displayName,
            description,
            writeMask,
            userWriteMask,
            eventNotifier
        );
    }

    @Override
    public UAObject unmarshal(UaObjectNode node) throws Exception {
        UAObject object = new UAObject();

        setBaseAttributes(node, object);

        object.setEventNotifier(node.getEventNotifier().shortValue());

        return object;
    }

}
