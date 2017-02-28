package org.opennms.plugins.graphml.client.test.manual;

import static org.junit.Assert.*;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.graphdrawing.graphml.xmlns.GraphmlType;
import org.junit.Test;
import org.opennms.plugins.graphml.asset.Utils;
import org.opennms.plugins.graphml.client.GraphMLRestJerseyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * test-graph.xml is used  by this test to post a graphml topology to OpenNMS.
 * Please note that this test can only be run manually against a running OpenNMS at localhost.
 *
 */
public class GraphMLRestJerseyClientTest {
	private static final Logger LOG = LoggerFactory.getLogger(GraphMLRestJerseyClientTest.class);


	private String baseUrl = "http://localhost:8980";
	private String basePath = "/opennms/rest";
	private String userName = "admin"; // If userName is null no basic authentication is generated
	private String password = "admin";

	private String graphname="assetTopology";

	GraphMLRestJerseyClient client = null;
	
	@Test
	public void testsInOrder(){
		LOG.debug("testsInOrder() START");
		// choose which tests you want to run by un-commenting below
		//readTestGraph();	
		//testCreateGraph();
		testGetGraph(); 
		//testDeleteGraph();
		LOG.debug("testsInOrder() FINISH");
	}

	private GraphMLRestJerseyClient getClient(){
		GraphMLRestJerseyClient client = new GraphMLRestJerseyClient();
		client.setBasePath(basePath);
		client.setBaseUrl(baseUrl);
		client.setPassword(password);
		client.setUserName(userName);
		return client;
	}
	


	private GraphmlType readTestGraph(){
		//File graphmlfile = new File("./src/test/resources/test-graph.xml");
		//File graphmlfile = new File("./src/test/resources/graphmlTestTopology2.xml");
		File graphmlfile = new File("./src/test/resources/returnedAssetTopology.xml");
		
		
		LOG.debug("reading graph file from"+graphmlfile.getAbsolutePath());

		//GraphmlType graph = JAXB.unmarshal(graphmlfile, GraphmlType.class);

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
		
		GraphmlType graph = jaxbgraph.getValue();
		LOG.debug("contents ");
		for (Object x: graph.getGraphOrData()){
			LOG.debug("object "
					+ ""+x.toString()+" name " +x.getClass().getName());
		}
		
		LOG.debug("unmarshaled test graph marshalled from file :"+Utils.graphmlTypeToString(graph));
		return graph;
	}
	


	//@Test
	public void testCreateGraph() {
		LOG.debug("testCreateGraph() START");
		GraphmlType graphmlType = readTestGraph();

		try {
			GraphMLRestJerseyClient client = getClient();
			client.createGraph(graphname, graphmlType) ;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.debug("testCreateGraph() FINISH");

	}

	//@Test 
	public void testDeleteGraph() {
		LOG.debug("testdDeleteGraph() START");
		try {
			GraphMLRestJerseyClient client = getClient();
            client.deleteGraph(graphname);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.debug("testdDeleteGraph() FINISH");
	}

	//@Test
	public void testGetGraph() {
		LOG.debug("testGetGraph() START");
		try {
			GraphMLRestJerseyClient client = getClient();
			GraphmlType graphmlType = client.getGraph(graphname);

			String gmlstr=null;
			if (graphmlType !=null){
				gmlstr=Utils.graphmlTypeToString(graphmlType);
			}

			LOG.debug("returned graphml "+ gmlstr);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		LOG.debug("testGetGraph() FINISH");
	}




}
