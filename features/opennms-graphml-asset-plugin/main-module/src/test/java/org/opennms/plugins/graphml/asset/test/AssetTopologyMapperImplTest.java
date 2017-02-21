package org.opennms.plugins.graphml.asset.test;

import static org.junit.Assert.*;

import org.graphdrawing.graphml.xmlns.GraphmlType;

import org.junit.Test;

import org.opennms.plugins.graphml.asset.AssetTopologyMapper;
import org.opennms.plugins.graphml.asset.AssetTopologyMapperImpl;
import org.opennms.plugins.graphml.asset.NodeInfoRepository;
import org.opennms.plugins.graphml.asset.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetTopologyMapperImplTest {
	private static final Logger LOG = LoggerFactory.getLogger(AssetTopologyMapperImplTest.class);

	@Test
	public void test() {
		
		AssetTopologyMapper assetTopologyMapper=new AssetTopologyMapperImpl();
		NodeInfoRepository nodeInfoRepository= NodeInfoRepositoryTest.getMockNodeInfoRepository();
	
		GraphmlType graphml = assetTopologyMapper.nodeInfoToTopology(nodeInfoRepository);
		assertNotNull(graphml);
		LOG.debug("graphml:\n"+Utils.graphmlTypeToString(graphml));
		
	}

}
