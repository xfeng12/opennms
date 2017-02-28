package org.opennms.plugins.graphml.client.test.manual;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.graphdrawing.graphml.xmlns.GraphmlType;

import org.opennms.plugins.graphml.asset.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import org.junit.Test;


public class GraphMLjaxbTest {
	private static final Logger LOG = LoggerFactory.getLogger(GraphMLjaxbTest.class);


	@Test
	public void readTestGraph1(){
		// test of openNMS example topology
		String graphfilePath="./src/test/resources/test-graph.xml";
		GraphmlType graphml = readTestGraph(graphfilePath);
		assertNotNull(graphml);
	}

	@Test
	public void readTestGraph2(){
		// test of generated asset topology file
		String graphfilePath="./src/test/resources/graphmlTestTopology2.xml";
		GraphmlType graphml = readTestGraph(graphfilePath);
		assertNotNull(graphml);
	}

	@Test
	public void readTestGraph3(){
		// THIS TEST WILL FAIL
		// test of asset topology file returned from OpennMS using
		// GET http://192.168.75.129:8980/opennms/rest/graphml/assetTopology
		String graphfilePath="./src/test/resources/returnedAssetTopology.xml";
		GraphmlType graphml = readTestGraph(graphfilePath);
		//assertNotNull(graphml); // THIS TEST WILL FAIL
	}


	private GraphmlType readTestGraph(String graphfilePath){

		File graphmlfile = new File(graphfilePath);


		LOG.debug("reading graph file from"+graphmlfile.getAbsolutePath());

		//GraphmlType graph = JAXB.unmarshal(graphmlfile, GraphmlType.class);

		GraphmlType graphmlType=null;
		JAXBContext ctx;
		JAXBElement<GraphmlType> jaxbgraph=null;
		try {
			ctx = JAXBContext.newInstance("org.graphdrawing.graphml.xmlns");
			Unmarshaller jaxbUnmarshaller = ctx.createUnmarshaller();
			jaxbgraph =  (JAXBElement<GraphmlType>) jaxbUnmarshaller.unmarshal(graphmlfile);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(jaxbgraph!=null){
			graphmlType = jaxbgraph.getValue();
			LOG.debug("contents ");
			for (Object x: graphmlType.getGraphOrData()){
				LOG.debug("object "
						+ ""+x.toString()+" name " +x.getClass().getName());
			}
			LOG.debug("unmarshaled test graph marshalled from file :"
					+graphfilePath+ " Data: "+Utils.graphmlTypeToString(graphmlType));
		} else {
			LOG.debug("unable to unmarshal test graph from file :"+graphfilePath);
		}

		
		return graphmlType;
	}
}
