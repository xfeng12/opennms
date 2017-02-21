package org.opennms.plugins.graphml.asset.test;

import static org.junit.Assert.*;

import java.util.Map;

import org.graphdrawing.graphml.xmlns.GraphmlType;
import org.junit.Test;
import org.opennms.plugins.graphml.asset.AssetTopologyMapperImpl;
import org.opennms.plugins.graphml.asset.NodeInfoRepository;
import org.opennms.plugins.graphml.asset.Utils;
import org.opennms.plugins.graphml.asset.xml.NodeInfoRepositoryXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetTopologyMapperImplXMLDataTest {
	private static final Logger LOG = LoggerFactory.getLogger(AssetTopologyMapperImplXMLDataTest .class);
	
	public static final String TEST_RESOURCE_FOLDER="./src/test/resources";
	public static final String NODE_TEST_DATA_FILE_NAME="nodeInfoMockTestData.xml";
	public static final String GRAPHML_TEST_TOPOLOGY_FILE_NAME="graphmlTestTopology.xml";
	
	// set this to match value of <data key="label">Asset Topology Mon 20.02.2017_11:34:03</data> in graphmlTestTopology.xml
	public static final String menuLabelStr="Asset Topology Mon 20.02.2017_11:34:03";

	@Test
	public void assetTopologyMapperImplTest() {
		LOG.debug("start of assetTopologyMapperImplTest");
		//create and populate new NodeInfoRepository from xml file
		NodeInfoRepository nodeInfoRepository= new NodeInfoRepository();
		Map<String, Map<String, String>> nodeInfo = nodeInfoRepository.getNodeInfo();

		String nodeInfoXmlStr = Utils.readFileFromDisk(TEST_RESOURCE_FOLDER, NODE_TEST_DATA_FILE_NAME);
		NodeInfoRepositoryXML.XMLtoNodeInfo(nodeInfo, nodeInfoXmlStr);

		//map to graphml file
		AssetTopologyMapperImpl assetTopologyMapperImpl= new AssetTopologyMapperImpl();
		assetTopologyMapperImpl.setMenuLabelStr(menuLabelStr);
		
		String layerHierarchyProperty="asset-address1,asset-address2";
		assetTopologyMapperImpl.setLayerHierarchyFromProperty(layerHierarchyProperty);
		String preferredLayout="Grid Layout";
		assetTopologyMapperImpl.setPreferredLayout(preferredLayout);

		GraphmlType graphmlType = assetTopologyMapperImpl.nodeInfoToTopology(nodeInfoRepository);
		
		//test graphml file matches stored graphml test resource
		String graphmlStr = Utils.graphmlTypeToString(graphmlType);
		LOG.debug("created graphml data: \n"+graphmlStr);
		
		String graphmlCompareStr = Utils.readFileFromDisk(TEST_RESOURCE_FOLDER, GRAPHML_TEST_TOPOLOGY_FILE_NAME);
		
		assertEquals(graphmlCompareStr,graphmlStr);
		
		LOG.debug("end of assetTopologyMapperImplTest");
	}


}
