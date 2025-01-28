package com.digitalpetri.opcua.nodeset.attributes;

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.opcfoundation.ua.generated.UAMethod;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class MethodNodeAttributes extends NodeAttributes {

    private final boolean executable;
    private final boolean userExecutable;

    public MethodNodeAttributes(
        NodeId nodeId,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask,
        boolean executable,
        boolean userExecutable
    ) {

        super(nodeId, NodeClass.Method, browseName, displayName, description, writeMask, userWriteMask);

        this.executable = executable;
        this.userExecutable = userExecutable;
    }

    public boolean isExecutable() {
        return executable;
    }

    public boolean isUserExecutable() {
        return userExecutable;
    }

    @Override
    public String toString() {
        return "MethodNodeAttributes{" +
            "executable=" + executable +
            ", userExecutable=" + userExecutable +
            "} " + super.toString();
    }

    public static MethodNodeAttributes fromGenerated(UAMethod gNode) {
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

        boolean executable = gNode.isExecutable();
        boolean userExecutable = gNode.isUserExecutable();

        return new MethodNodeAttributes(
            nodeId,
            browseName,
            displayName,
            description,
            writeMask,
            userWriteMask,
            executable,
            userExecutable
        );
    }

}
