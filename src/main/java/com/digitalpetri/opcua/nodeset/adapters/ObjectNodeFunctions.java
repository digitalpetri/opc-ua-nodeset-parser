package com.digitalpetri.opcua.nodeset.adapters;

import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.opcfoundation.ua.generated.UAObject;

public abstract class ObjectNodeFunctions {

    private ObjectNodeFunctions() {}

    public static UaObjectNode fromGenerated(UAObject generated) {
        return null;
    }

    public static UAObject toGenerated(UaObjectNode node) {
        return null;
    }

}
