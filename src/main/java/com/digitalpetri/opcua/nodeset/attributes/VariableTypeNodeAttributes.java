package com.digitalpetri.opcua.nodeset.attributes;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.Marshaller;

import com.digitalpetri.opcua.nodeset.util.AttributeUtil;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.opcfoundation.ua.generated.UAVariableType;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class VariableTypeNodeAttributes extends NodeAttributes {

    private final DataValue value;
    private final NodeId dataType;
    private final int valueRank;
    private final UInteger[] arrayDimensions;
    private final boolean isAbstract;

    public VariableTypeNodeAttributes(
        NodeId nodeId,
        QualifiedName browseName,
        LocalizedText displayName,
        LocalizedText description,
        UInteger writeMask,
        UInteger userWriteMask,
        DataValue value,
        NodeId dataType,
        int valueRank,
        UInteger[] arrayDimensions,
        boolean isAbstract
    ) {

        super(nodeId, NodeClass.VariableType, browseName, displayName, description, writeMask, userWriteMask);

        this.value = value;
        this.dataType = dataType;
        this.valueRank = valueRank;
        this.arrayDimensions = arrayDimensions;
        this.isAbstract = isAbstract;
    }

    public DataValue getValue() {
        return value;
    }

    public NodeId getDataType() {
        return dataType;
    }

    public int getValueRank() {
        return valueRank;
    }

    public UInteger[] getArrayDimensions() {
        return arrayDimensions;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public String toString() {
        return "VariableTypeNodeAttributes{" +
            "value=" + value +
            ", dataType=" + dataType +
            ", valueRank=" + valueRank +
            ", arrayDimensions=" + Arrays.toString(arrayDimensions) +
            ", isAbstract=" + isAbstract +
            "} " + super.toString();
    }

    public static VariableTypeNodeAttributes fromGenerated(
        UAVariableType gNode,
        Marshaller marshaller,
        Map<String, NodeId> aliasMap, Map<NodeId, String> rawXmlValues) {

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

        DataValue value = value(gNode.getValue(), marshaller, nodeId, rawXmlValues)
            .orElse(new DataValue(Variant.NULL_VALUE));
        NodeId dataType = AttributeUtil.parseDataType(gNode.getDataType(), aliasMap);
        int valueRank = gNode.getValueRank();
        UInteger[] arrayDimensions = AttributeUtil.parseArrayDimensions(gNode.getArrayDimensions());
        boolean isAbstract = gNode.isIsAbstract();

        return new VariableTypeNodeAttributes(
            nodeId,
            browseName,
            displayName,
            description,
            writeMask,
            userWriteMask,
            value,
            dataType,
            valueRank,
            arrayDimensions,
            isAbstract
        );
    }

    private static Optional<DataValue> value(
        UAVariableType.Value gValue,
        Marshaller marshaller,
        NodeId nodeId,
        Map<NodeId, String> rawXmlValues
    ) {
        
        if (gValue == null) return Optional.empty();

        return Optional.of(AttributeUtil.parseValue(gValue.getAny(), marshaller, nodeId, rawXmlValues));
    }

}
