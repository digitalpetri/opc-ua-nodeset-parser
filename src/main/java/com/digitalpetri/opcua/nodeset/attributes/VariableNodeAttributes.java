package com.digitalpetri.opcua.nodeset.attributes;

import java.util.Arrays;
import java.util.Map;
import javax.xml.bind.Marshaller;

import com.digitalpetri.opcua.nodeset.util.AttributeUtil;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.opcfoundation.ua.generated.UAVariable;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class VariableNodeAttributes extends NodeAttributes {

    private final DataValue value;
    private final NodeId dataType;
    private final int valueRank;
    private final UInteger[] arrayDimensions;
    private final UByte accessLevel;
    private final UByte userAccessLevel;
    private final Double minimumSamplingInterval;
    private final boolean historizing;

    public VariableNodeAttributes(
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
        UByte accessLevel,
        UByte userAccessLevel,
        Double minimumSamplingInterval,
        boolean historizing
    ) {

        super(nodeId, NodeClass.Variable, browseName, displayName, description, writeMask, userWriteMask);

        this.value = value;
        this.dataType = dataType;
        this.valueRank = valueRank;
        this.arrayDimensions = arrayDimensions;
        this.accessLevel = accessLevel;
        this.userAccessLevel = userAccessLevel;
        this.minimumSamplingInterval = minimumSamplingInterval;
        this.historizing = historizing;
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

    public UByte getAccessLevel() {
        return accessLevel;
    }

    public UByte getUserAccessLevel() {
        return userAccessLevel;
    }

    public Double getMinimumSamplingInterval() {
        return minimumSamplingInterval;
    }

    public boolean isHistorizing() {
        return historizing;
    }

    @Override
    public String toString() {
        return "VariableNodeAttributes{" +
            "value=" + value +
            ", dataType=" + dataType +
            ", valueRank=" + valueRank +
            ", arrayDimensions=" + Arrays.toString(arrayDimensions) +
            ", accessLevel=" + accessLevel +
            ", userAccessLevel=" + userAccessLevel +
            ", minimumSamplingInterval=" + minimumSamplingInterval +
            ", historizing=" + historizing +
            "} " + super.toString();
    }

    public static VariableNodeAttributes fromGenerated(
        UAVariable gNode,
        Marshaller marshaller,
        Map<String, NodeId> aliasMap,
        Map<NodeId, String> rawXmlValues
    ) {

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

        DataValue value = value(gNode.getValue(), marshaller, nodeId, rawXmlValues);
        NodeId dataType = AttributeUtil.parseDataType(gNode.getDataType(), aliasMap);
        int valueRank = gNode.getValueRank();
        UInteger[] arrayDimensions = AttributeUtil.parseArrayDimensions(gNode.getArrayDimensions());
        UByte accessLevel = ubyte(gNode.getAccessLevel());
        UByte userAccessLevel = ubyte(gNode.getUserAccessLevel());
        Double minimumSamplingInterval = gNode.getMinimumSamplingInterval();
        boolean historizing = gNode.isHistorizing();

        return new VariableNodeAttributes(
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
            accessLevel,
            userAccessLevel,
            minimumSamplingInterval,
            historizing
        );
    }

    private static DataValue value(
        UAVariable.Value gValue,
        Marshaller marshaller,
        NodeId nodeId,
        Map<NodeId, String> rawXmlValues
    ) {

        if (gValue == null || gValue.getAny() == null) {
            return new DataValue(Variant.NULL_VALUE);
        }

        return AttributeUtil.parseValue(gValue.getAny(), marshaller, nodeId, rawXmlValues);
    }

}
