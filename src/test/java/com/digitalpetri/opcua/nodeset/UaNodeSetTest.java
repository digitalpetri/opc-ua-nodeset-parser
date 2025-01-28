package com.digitalpetri.opcua.nodeset;

import java.io.InputStream;



import org.junit.Test;

import jakarta.xml.bind.JAXBException;


public class UaNodeSetTest {

    @Test
    public void testParseNodeSet2() throws Exception {
        parse("Opc.Ua.NodeSet2.xml");
    }

    @Test
    public void testParseNodeSet2_Part3() throws Exception {
        parse("Opc.Ua.NodeSet2.Part3.xml");
    }

    @Test
    public void testParseNodeSet2_Part4() throws Exception {
        parse("Opc.Ua.NodeSet2.Part4.xml");
    }

    @Test
    public void testParseNodeSet2_Part5() throws Exception {
        parse("Opc.Ua.NodeSet2.Part5.xml");
    }

    @Test
    public void testParseNodeSet2_Part9() throws Exception {
        parse("Opc.Ua.NodeSet2.Part9.xml");
    }

    @Test
    public void testParseNodeSet2_Part10() throws Exception {
        parse("Opc.Ua.NodeSet2.Part10.xml");
    }

    @Test
    public void testParseNodeSet2_Part11() throws Exception {
        parse("Opc.Ua.NodeSet2.Part11.xml");
    }

    @Test
    public void testParseNodeSet2_Part13() throws Exception {
        parse("Opc.Ua.NodeSet2.Part13.xml");
    }

    @Test
    public void testParseAdiNodeSet() throws Exception {
        parse("adi/Opc.Ua.Adi.NodeSet2.xml");
    }

    @Test
    public void testParseAMLBaseTypes() throws Exception {
        parse("aml/Opc.Ua.AMLBaseTypes.NodeSet2.xml");
    }

    @Test
    public void testParseAMLLibraries() throws Exception {
        parse("aml/Opc.Ua.AMLLibraries.NodeSet2.xml");
    }

    @Test
    public void testParseAutoIdNodeSet() throws Exception {
        parse("autoid/Opc.Ua.AutoID.NodeSet2.xml");
    }

    @Test
    public void testParseDiNodeSet() throws Exception {
        parse("di/Opc.Ua.Di.NodeSet2.xml");
    }

    @Test
    public void testParseMdisNodeSet() throws Exception {
        parse("mdis/OPC.MDIS.NodeSet2.xml");
    }

    @Test
    public void testParsePlcNodeSet() throws Exception {
        parse("plc/Opc.Ua.Plc.NodeSet2.xml");
    }

    @Test
    public void testParseSchemaCheckNodeSet() throws Exception {
        parse("schema/Opc.Ua.NodeSet.Schema.Check.xml");
    }

    private void parse(String nodeSetFilename) throws JAXBException {
        InputStream nodeSetXml = getClass().getClassLoader().getResourceAsStream(nodeSetFilename);

        UaNodeSet nodeSet = UaNodeSet.parse(nodeSetXml);

        System.out.println("Parsed " + nodeSetFilename + " and generated " + nodeSet.getNodes().size() + " nodes.");
    }

}
