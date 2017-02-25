/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.graphml.asset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.graphdrawing.graphml.xmlns.DataType;
import org.graphdrawing.graphml.xmlns.EdgeType;
import org.graphdrawing.graphml.xmlns.GraphEdgedefaultType;
import org.graphdrawing.graphml.xmlns.GraphType;
import org.graphdrawing.graphml.xmlns.KeyForType;
import org.graphdrawing.graphml.xmlns.KeyType;
import org.graphdrawing.graphml.xmlns.KeyTypeType;
import org.graphdrawing.graphml.xmlns.NodeType;
import org.graphdrawing.graphml.xmlns.GraphmlType;
import org.graphdrawing.graphml.xmlns.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetTopologyMapperImpl implements AssetTopologyMapper {

	private static final Logger LOG = LoggerFactory.getLogger(AssetTopologyMapperImpl.class);

	private ObjectFactory of = new ObjectFactory();

	private static final String[] defaultHierarchy= {NodeParamLabels.ASSET_REGION,
		NodeParamLabels.ASSET_BUILDING,
		NodeParamLabels.ASSET_RACK};

	private List<String> layerHierarchy  = Arrays.asList(defaultHierarchy);

	private String preferredLayout="Grid Layout";

	private String menuLabelStr="Asset Topology";

	public List<String> getLayerHierarchy() {
		return layerHierarchy;
	}


	/**
	 * Sets a list layerHierarchy. Each entry will determine the name and odrer of heirarchical topology graphs
	 * the list must only contain names corresponding to values in org.opennms.plugins.graphml.asset.NodeParamLabels which
	 * are keys to access the OpenNMS assets
	 * @param layerHierarchy
	 */
	public void setLayerHierarchy(List<String> layerHierarchy) {
		this.layerHierarchy = layerHierarchy;
	}

	/**
	 * Sets a list layerHierarchy from a comma separated String property. Ther must be no spaces in the string
	 * and each value should be a key from org.opennms.plugins.graphml.asset.NodeParamLabels
	 * @param layerHierarchyProperty
	 */
	public void setLayerHierarchyFromProperty(String layerHierarchyProperty){
		String[] lh = layerHierarchyProperty.split(",");
		layerHierarchy  = Arrays.asList(lh);

		if (LOG.isDebugEnabled()){
			StringBuffer msg = new StringBuffer("Hierarchy loaded from property:");
			for (String layer:layerHierarchy) msg.append(layer+",");
			LOG.debug(msg.toString());
		}

	}

	public String getPreferredLayout() {
		return preferredLayout;
	}

	/**
	 * determine the preferred layout of generated graphs 
	 * defaults to "Grid Layout"
	 * @param preferredLayout
	 */
	public void setPreferredLayout(String preferredLayout) {
		this.preferredLayout = preferredLayout;
	}


	@Override
	public String getMenuLabelStr() {
		return menuLabelStr;
	}

	/**
	 * Sets the the label which will appear in the OpenNMS topology menu
	 * used to set the graphml <data key="label"></data>
	 */
	@Override
	public void setMenuLabelStr(String menuLabelStr) {
		this.menuLabelStr = menuLabelStr;
	}


	/**
	 * This method generates the layer hierarchy graphml file from the
	 * node asset information supplied in nodeInfoRepository
	 */
	@Override
	public GraphmlType nodeInfoToTopology(NodeInfoRepository nodeInfoRepository) {

		// print log info for graph definition
		StringBuffer msg = new StringBuffer("Creating topology "+menuLabelStr+" for layerHierarchy :");
		if(layerHierarchy.size()==0){
			msg.append("EMPTY");
		} else {
			for(String layer:layerHierarchy){
				msg.append(layer+",");
			}
		}
		LOG.info(msg.toString());

		GraphmlType graphmlType = createGraphML(menuLabelStr);

		Map<String, Map<String, String>> onmsNodeInfo = nodeInfoRepository.getNodeInfo();

		if (layerHierarchy.size()==0){
			//create simple graph with all nodes if layerHierarchy is empty
			if( LOG.isDebugEnabled()) LOG.debug("creating a simple graph containing all nodes as layerHierarchy is empty");

			Integer semanticZoomLevel=0;
			String graphId ="all nodes";
			String descriptionStr="A simple graph containing all nodes created because layerHierarchy property is empty";
			GraphType graph = createGraphInGraphmlType(graphmlType, graphId, descriptionStr, preferredLayout, semanticZoomLevel);

			Map<String, Map<String, String>> nodeInfo = nodeInfoRepository.getNodeInfo();
			addOpenNMSNodes(graph, nodeInfo);

		} else {
			// create graphs for all possible layers in hierarchy
			msg = new StringBuffer("create graphs from asset and layerHierarchy: ");

			List<GraphType> graphList = new ArrayList<GraphType>();
			Integer semanticZoomLevel=0;

			String descriptionStr=null;
			// create graph for each layer in hierarchy
			for(String graphname:layerHierarchy){
				//(GraphmlType graphmlType, String graphId, String descriptionStr, String preferredLayout, Integer semanticZoomLevelInt)
				GraphType graph = createGraphInGraphmlType(graphmlType, graphname, descriptionStr, preferredLayout, semanticZoomLevel);
				graphList.add(graph);
				msg.append(graphname+",");
				semanticZoomLevel++;
			}

			//create graph for nodes layer (last layer in hierarchy)
			String graphname="nodes";
			GraphType nodegraph = createGraphInGraphmlType(graphmlType, graphname, descriptionStr, preferredLayout, semanticZoomLevel);
			graphList.add(nodegraph);
			msg.append(graphname);

			if( LOG.isDebugEnabled()) LOG.debug(msg.toString());

			// create graph hierarchy according to asset table contents

			// used to store all nodes which have been allocated to a graph layer
			Map<String, Map<String, String>> allocatedNodeInfo = new LinkedHashMap<String, Map<String, String>>();

			// add layer graphs for defined layerHierarchy
			String parentNodeId=null;
			recursivelyAddlayers(layerHierarchy, 0,   onmsNodeInfo,  allocatedNodeInfo,  graphmlType, graphList, parentNodeId);

			// add unallocated nodes into a default unallocated_Nodes graph
			Map<String, Map<String, String>> unAllocatedNodeInfo = new LinkedHashMap<String, Map<String, String>>();
			unAllocatedNodeInfo.putAll(onmsNodeInfo); //initialise with full list of nodes

			for (String allocatedNodeId:allocatedNodeInfo.keySet()){
				unAllocatedNodeInfo.remove(allocatedNodeId);
			}

			graphname="unallocated_Nodes";
			descriptionStr="A graph containing all nodes which cannot be placed in topology hierarchy";
			semanticZoomLevel=0;
			GraphType graph = createGraphInGraphmlType(graphmlType, graphname, descriptionStr, preferredLayout, semanticZoomLevel);
			addOpenNMSNodes(graph, unAllocatedNodeInfo);
		}

		return graphmlType;
	}


	/**
	 * Recursive function to add OpenNMS nodes defined in nodeInfo into hierarchy of created in the given graphmlType
	 * returns list of nodes added in the next layer for use in edges in this layer
	 * @param layerHierarchy static list of layer names which correspond to asset table keys defined in NodeParamLabels
	 * @param layerHierarchyIndex the current layer for which this function is called (initialise to 0) subsequent recursive calls will increment the number until layerHierarchy.size()
	 * @param nodeInfo nodeInfo map with values Map<nodeId, Map<nodeParamLabelKey, nodeParamValue>>
	 *        nodeParamLabelKey a node asset parameter key (from those defined in org.opennms.plugins.graphml.asset.NodeParamLabels)
	 *        nodeParamValue a node asset value ( e.g. key NodeParamLabels.ASSET_RACK ('asset-rack') value: rack1
	 * @param allocatedNodeInfo this contains all of the nodes which a recursive call to this method has added. i.e. once the function is finished,
	 * all of the nodes placed in a graph are included in this list. The list can then be used to determine the unallocated nodes.
	 * @param graphmlType the parent graphmltype into which all the created graphe muse be placed
	 * @param graphList a list of pre created graphs which are in the same order and should  be pre-named with the names in the layerHierarchy
	 * @param parentNodeId the nodeId of the parent node which the edges generated for the next layer must reference
	 * @return addedNodes returns list of nodes which have been added by this recursive call. These nodes are used to create the edges in the previous layer
	 */
	private Map<String, Map<String, String>> recursivelyAddlayers(List<String> layerHierarchy, int layerHierarchyIndex,  Map<String, Map<String, String>> nodeInfo, Map<String, Map<String, String>> allocatedNodeInfo, GraphmlType graphmlType, List<GraphType> graphList, String parentNodeId){
		if(layerHierarchy==null||layerHierarchy.size()==0 ) throw new RuntimeException("AssetTopologyMapperImpl layerHierarchy must not be null or empty");

		// returns list of nodes added - either OpenNMS nodes or higher level graphs
		Map<String, Map<String, String>> addedNodes=null;

		if( LOG.isDebugEnabled()) LOG.debug("recursivelyAddlayers called for layerHierarchyIndex:"+layerHierarchyIndex+" parentNodeId="+parentNodeId);

		// add nodes to graph
		if(layerHierarchyIndex>=layerHierarchy.size()){
			// we are at bottom of hierarchy so add real opennms nodes and edges

			//get hierarchy name for the previous layer
			String layerNodeParamLabel= layerHierarchy.get(layerHierarchyIndex-1);

			// get graph for this layer
			if( LOG.isDebugEnabled()) LOG.debug("populating graph with OpenNMS nodes for layer="+layerNodeParamLabel);
			// this will return the nodes graph - the last  graph in graphList
			GraphType graph = graphList.get(layerHierarchyIndex); // this will return the nodes graph - the last  graph in graphList

			//add real opennms nodes to graph
			addOpenNMSNodes(graph, nodeInfo);

			// add these nodes to the allocated node set
			allocatedNodeInfo.putAll(nodeInfo);

			if (LOG.isDebugEnabled()) {
				StringBuffer msg= new StringBuffer("adding opennms nodes to graphId="+layerNodeParamLabel+ " nodes:");

				for (String targetNodeId:nodeInfo.keySet()){
					msg.append(targetNodeId+",");
				}
			}

			addedNodes=nodeInfo;

		} else {
			// else create and add the parent nodes for the next layer
			if( LOG.isDebugEnabled()) LOG.debug("populating parent graph for index "+layerHierarchyIndex);

			//get hierarchy name for this layer
			String layerNodeParamLabelKey= layerHierarchy.get(layerHierarchyIndex);
			if( LOG.isDebugEnabled()) LOG.debug("parent graph name="+layerNodeParamLabelKey);

			// get graph for this layer
			GraphType graph = graphList.get(layerHierarchyIndex);

			// find all values corresponding to nodeParamLabelKey in this layer
			Set<String> layerNodeParamLabelValues = new TreeSet<String>();
			for (String nodeId: nodeInfo.keySet()){
				String nodeParamValue = nodeInfo.get(nodeId).get(layerNodeParamLabelKey);
				if(nodeParamValue!=null){
					layerNodeParamLabelValues.add(nodeParamValue);
				}
			}

			if (LOG.isDebugEnabled()){
				StringBuffer msg=new StringBuffer("values corresponding to layerNodeParamLabelKey="+layerNodeParamLabelKey+ " in this layer :");
				for (String nodeParamValue:layerNodeParamLabelValues){
					msg.append(nodeParamValue+",");
				}
				LOG.debug(msg.toString());
			}

			// create added nodes to return. These are the nodes which have been added in this layer
			// and are used to populate the edges in the previous layer
			addedNodes = new LinkedHashMap<String, Map<String, String>>();

			// iterate over values in this layer
			for (String nodeParamLabelValue:layerNodeParamLabelValues){

				// create new node for each value in this layer
				String graphmlNodeId= (parentNodeId==null) ? nodeParamLabelValue : parentNodeId+"."+nodeParamLabelValue;
				NodeType node = createNodeType(graphmlNodeId,nodeParamLabelValue);
				graph.getDataOrNodeOrEdge().add(node);
				StringBuffer msg=new StringBuffer("created childNode graphmlNodeId="+graphmlNodeId+" nodeParamLabelValue="+nodeParamLabelValue+ " in  graphId="+layerNodeParamLabelKey);

				// create sub list of nodes corresponding to param label 
				Map<String, Map<String, String>> nodeInfoSubList =createNodeInfoSubList(layerNodeParamLabelKey, nodeParamLabelValue, nodeInfo);

				// recursively add graphs and nodes until complete
				int nextLayerHierarchyIndex=layerHierarchyIndex+1;
				Map<String, Map<String, String>> nextLayerNodesAdded = recursivelyAddlayers(layerHierarchy, nextLayerHierarchyIndex, nodeInfoSubList, allocatedNodeInfo, graphmlType, graphList, graphmlNodeId );

				// we are now using data returned from a recursive call to recursivelyAddlayers
				// nextLayerNodesAdded contains the nodes added in the lower layer
				// and these can be used to populates edges in this layer

				// create edge for each node in returned nextLayerNodesAdded
				if (nextLayerHierarchyIndex<layerHierarchy.size()){
					// if not lowest layer then add edges pointing next layers
					msg.append(" edges added for next graph layer: " );
					for (String targetNodeId:nextLayerNodesAdded.keySet()){
						Map<String, String> nodeParamaters = nextLayerNodesAdded.get(targetNodeId);
						String labelStr = nodeParamaters.get(layerHierarchy.get(nextLayerHierarchyIndex));
						String childNodeLabelStr= graphmlNodeId+"."+labelStr;
						EdgeType edge = addEdgeToGraph(graph, graphmlNodeId, childNodeLabelStr);
						msg.append(edge.getId()+",");

						addedNodes.put(targetNodeId, nodeParamaters);
					}
				} else {
					// if lowest layer then add node ids (i.e. opennms node labels)
					msg.append(" edges added for opennms nodes: " );
					for (String targetNodeId:nextLayerNodesAdded.keySet()){
						Map<String, String> nodeParamaters = nextLayerNodesAdded.get(targetNodeId);
						String nodeLabelStr = nodeParamaters.get(NodeParamLabels.NODE_NODELABEL);
						EdgeType edge = addEdgeToGraph(graph, graphmlNodeId, nodeLabelStr);
						msg.append(edge.getId()+",");

						addedNodes.put(targetNodeId, nodeParamaters);
					}
				}

				if( LOG.isDebugEnabled()){
					LOG.debug(msg.toString());
				}

			}

		}
		if( LOG.isDebugEnabled()) LOG.debug("returning from recursivelyAddlayers called for layerHierarchyIndex:"+layerHierarchyIndex);
		return addedNodes;
	}

	/**
	 * searches the supplied nodeInfo for nodes with matching paramaters for nodeParamLabelKey and nodeParamValue
	 * returns a sub list of nodeInfo only for the nodes with the matching parameter
	 * @param nodeParamLabelKey a node asset parameter key (from those defined in org.opennms.plugins.graphml.asset.NodeParamLabels)
	 * @param nodeParamValue a node asset value ( e.g. key NodeParamLabels.ASSET_RACK ('asset-rack') value: rack1
	 * @param nodeInfo Map<String, Map<String, String>> with values Map<nodeId, Map<nodeParamLabelKey, nodeParamValue>>
	 * @return
	 */
	private Map<String, Map<String, String>> createNodeInfoSubList(String nodeParamLabelKey, String nodeParamValue, Map<String, Map<String, String>> nodeInfo){
		if(nodeParamLabelKey==null) throw new RuntimeException("createNodeInfoSubList nodeParamLabel cannot be null");
		if(nodeParamValue==null) throw new RuntimeException("createNodeInfoSubList nodeParamValue cannot be null");

		Map<String,Map<String,String>> nodeInfoSubList = new LinkedHashMap<String, Map<String, String>>();

		StringBuffer msg = new StringBuffer("creating NodeInfoSubList for nodeParamLabelKey:"+nodeParamLabelKey+" nodeParamValue:"+nodeParamValue+ " sublist nodeIds:");
		for (String nodeId:nodeInfo.keySet()){
			Map<String, String> nodeParams = nodeInfo.get(nodeId);
			if(nodeParamValue.equals(nodeParams.get(nodeParamLabelKey))){
				nodeInfoSubList.put(nodeId, nodeParams );
				msg.append(nodeId+",");
			}
		}
		if( LOG.isDebugEnabled()) LOG.debug(msg.toString());
		return nodeInfoSubList;
	}

	/**
	 * Creates a new edge and adds it to a given graphml graph. The graph is first searched and the edge is only added if
	 * its id is not already defined. The id is concatenated from the sourceIdStr and the targetIdStr
	 * @param graph
	 * @param sourceIdStr source nodeId for this edge (nodeId represents the graphml nodeId unique in the graph namespace)
	 * @param targetIdStr target nodeId for this edge
	 * @return
	 */
	private EdgeType addEdgeToGraph(GraphType graph, String sourceIdStr, String targetIdStr){
		EdgeType edge = of.createEdgeType();
		String id =sourceIdStr+"_"+targetIdStr;
		edge.setId(id);
		edge.setSource(sourceIdStr);
		edge.setTarget(targetIdStr);
		if( LOG.isDebugEnabled()) LOG.debug("adding edge id="+id+ " to graph:"+graph.getId());
		List<Object> edgeList = graph.getDataOrNodeOrEdge();
		// check if edge is already added before adding
		boolean addedge=true;
		for(Object foundEdge:edgeList){
			if(foundEdge instanceof EdgeType){
				EdgeType f= (EdgeType) foundEdge;
				if (id.equals(f.getId())) addedge=false;
			}
		}
		if(addedge) graph.getDataOrNodeOrEdge().add(edge);
		return edge;
	}

	/**
	 * Creates a new graphml node with a nodeId and nodeLabel
	 * note that the nodeId is the unique id in the graph name space. The nodelabel is the value which shows
	 * up on the rendered graph
	 * @param nodeId
	 * @param nodeLabel
	 * @return
	 */
	private NodeType createNodeType(String nodeId, String nodeLabel){
		NodeType node = of.createNodeType();

		node.setId(nodeId);
		DataType nodeLabelDataType = of.createDataType();
		nodeLabelDataType.setKey(GraphMLKeyNames.LABEL);
		nodeLabelDataType.setContent(nodeLabel);
		node.getDataOrPort().add(nodeLabelDataType);

		return node;
	}

	/**
	 * Creates a new empty graphml graph with a predefined breadcrumb strategy for the OpenNMS use case
	 * @param menuLabelStr
	 * @return
	 */
	private GraphmlType createGraphML(String menuLabelStr){
		GraphmlType graphmltype = new GraphmlType();

		addKeyTypes(graphmltype);

		// <!-- shows up in the menu -->
		// <data key="label">Minimalistic GraphML Topology Provider</data>
		DataType menuLabel = of.createDataType();
		menuLabel.setKey(GraphMLKeyNames.LABEL);
		menuLabel.setContent(menuLabelStr);
		graphmltype.getGraphOrData().add(menuLabel);

		//<data key="breadcrumb-strategy">SHORTEST_PATH_TO_ROOT</data>
		DataType breadcrumbStrategy = of.createDataType();
		breadcrumbStrategy.setKey(GraphMLKeyNames.BREADCRUMB_STRATEGY);
		breadcrumbStrategy.setContent("SHORTEST_PATH_TO_ROOT");
		graphmltype.getGraphOrData().add(breadcrumbStrategy);

		return graphmltype;
	}

	/**
	 * Adds default keys for the OpenNMS use case to the graphml object 
	 * @param graphmlType
	 * @return
	 */
	private GraphmlType addKeyTypes(GraphmlType graphmlType) {

		// <key id="label" for="graphml" attr.name="label"
		// attr.type="string"></key>
		KeyType graphmlLabel = of.createKeyType();
		graphmlLabel.setAttrName(GraphMLKeyNames.LABEL);
		graphmlLabel.setAttrType(KeyTypeType.STRING);
		graphmlLabel.setId(GraphMLKeyNames.LABEL);
		graphmlLabel.setFor(KeyForType.GRAPHML);
		graphmlType.getKey().add(graphmlLabel);

		// <key id="breadcrumb-strategy" for="graphml" attr.name="breadcrumb-strategy" attr.type="string" />
		KeyType breadcrumbStrategy = of.createKeyType();
		breadcrumbStrategy.setAttrName(GraphMLKeyNames.BREADCRUMB_STRATEGY);
		breadcrumbStrategy.setAttrType(KeyTypeType.STRING);
		breadcrumbStrategy.setId(GraphMLKeyNames.BREADCRUMB_STRATEGY);
		breadcrumbStrategy.setFor(KeyForType.GRAPHML);
		graphmlType.getKey().add(breadcrumbStrategy);

		// <key id="label" for="graph" attr.name="label"
		// attr.type="string"></key>
		KeyType graphLabelKey = of.createKeyType();
		graphLabelKey.setAttrName(GraphMLKeyNames.LABEL);
		graphLabelKey.setAttrType(KeyTypeType.STRING);
		graphLabelKey.setId(GraphMLKeyNames.LABEL);
		graphLabelKey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(graphLabelKey);

		// <key id="description" for="graph" attr.name="description"
		// attr.type="string"></key>
		KeyType graphDescriptionKey = of.createKeyType();
		graphDescriptionKey.setAttrName(GraphMLKeyNames.DESCRIPTION);
		graphDescriptionKey.setAttrType(KeyTypeType.STRING);
		graphDescriptionKey.setId(GraphMLKeyNames.DESCRIPTION);
		graphDescriptionKey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(graphDescriptionKey);

		// <key id="namespace" for="graph" attr.name="namespace"
		// attr.type="string"></key>
		KeyType graphNamespacekey = of.createKeyType();
		graphNamespacekey.setAttrName(GraphMLKeyNames.NAMESPACE);
		graphNamespacekey.setAttrType(KeyTypeType.STRING);
		graphNamespacekey.setId(GraphMLKeyNames.NAMESPACE);
		graphNamespacekey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(graphNamespacekey);

		// <key id="preferred-layout" for="graph" attr.name="preferred-layout"
		// attr.type="string"></key>
		KeyType preferredLayoutKey = of.createKeyType();
		preferredLayoutKey.setAttrName(GraphMLKeyNames.PREFERRED_LAYOUT);
		preferredLayoutKey.setAttrType(KeyTypeType.STRING);
		preferredLayoutKey.setId(GraphMLKeyNames.PREFERRED_LAYOUT);
		preferredLayoutKey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(preferredLayoutKey);

		// <key id="focus-strategy" for="graph" attr.name="focus-strategy"
		// attr.type="string"></key>
		KeyType focusStrategyKey = of.createKeyType();
		focusStrategyKey.setAttrName(GraphMLKeyNames.FOCUS_STRATEGY);
		focusStrategyKey.setAttrType(KeyTypeType.STRING);
		focusStrategyKey.setId(GraphMLKeyNames.FOCUS_STRATEGY);
		focusStrategyKey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(focusStrategyKey);

		// <key id="focus-ids" for="graph" attr.name="focus-ids"
		// attr.type="string"></key>
		KeyType focusIdsKey = of.createKeyType();
		focusIdsKey.setAttrName(GraphMLKeyNames.FOCUS_IDS);
		focusIdsKey.setAttrType(KeyTypeType.STRING);
		focusIdsKey.setId(GraphMLKeyNames.FOCUS_IDS);
		focusIdsKey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(focusIdsKey);

		// <key id="semantic-zoom-level" for="graph"
		// attr.name="semantic-zoom-level" attr.type="int"/>
		KeyType semanticZoomlevelKey = of.createKeyType();
		semanticZoomlevelKey.setAttrName(GraphMLKeyNames.SEMANTIC_ZOOM_LEVEL);
		semanticZoomlevelKey.setAttrType(KeyTypeType.INT);
		semanticZoomlevelKey.setId(GraphMLKeyNames.SEMANTIC_ZOOM_LEVEL);
		semanticZoomlevelKey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(semanticZoomlevelKey);

		// <key id="label" for="node" attr.name="label"
		// attr.type="string"></key>
		KeyType nodelabelKey = of.createKeyType();
		nodelabelKey.setAttrName(GraphMLKeyNames.LABEL);
		nodelabelKey.setAttrType(KeyTypeType.STRING);
		nodelabelKey.setId(GraphMLKeyNames.LABEL);
		nodelabelKey.setFor(KeyForType.NODE);
		graphmlType.getKey().add(nodelabelKey);

		// key id="foreignSource" for="node" attr.name="foreignSource"
		// attr.type="string"></key>
		KeyType foreignSourcekey = of.createKeyType();
		foreignSourcekey.setAttrName(GraphMLKeyNames.FOREIGN_SOURCE);
		foreignSourcekey.setAttrType(KeyTypeType.STRING);
		foreignSourcekey.setId(GraphMLKeyNames.FOREIGN_SOURCE);
		foreignSourcekey.setFor(KeyForType.NODE);
		graphmlType.getKey().add(foreignSourcekey);

		// <key id="foreignID" for="node" attr.name="foreignID"
		// attr.type="string"></key>
		KeyType foreignIDkey = of.createKeyType();
		foreignIDkey.setAttrName(GraphMLKeyNames.FOREIGN_ID);
		foreignIDkey.setAttrType(KeyTypeType.STRING);
		foreignIDkey.setId(GraphMLKeyNames.FOREIGN_ID);
		foreignIDkey.setFor(KeyForType.NODE);
		graphmlType.getKey().add(foreignIDkey);

		// <key id="nodeID" for="node" attr.name="nodeID" attr.type="int"></key>
		KeyType nodeIdKey = new KeyType();
		nodeIdKey.setAttrName(GraphMLKeyNames.NODE_ID);
		nodeIdKey.setAttrType(KeyTypeType.INT);
		nodeIdKey.setId(GraphMLKeyNames.NODE_ID);
		nodeIdKey.setFor(KeyForType.NODE);
		graphmlType.getKey().add(nodeIdKey);
		
		// <key id="vertex-status-provider" for="graph" attr.name="vertex-status-provider" attr.type="boolean"/>
		KeyType vertexStatusProviderKey = new KeyType();
		vertexStatusProviderKey.setAttrName(GraphMLKeyNames.VERTEX_STATUS_PROVIDER);
		vertexStatusProviderKey.setAttrType(KeyTypeType.BOOLEAN);
		vertexStatusProviderKey.setId(GraphMLKeyNames.VERTEX_STATUS_PROVIDER);
		vertexStatusProviderKey.setFor(KeyForType.GRAPH);
		graphmlType.getKey().add(vertexStatusProviderKey);

		return graphmlType;

	}

	/**
	 * Creates a new graph in the graphml type with default data values for he opennms keys
	 * @param graphmlType
	 * @param graphId
	 * @param descriptionStr
	 * @param preferredLayout
	 * @param semanticZoomLevelInt
	 * @return
	 */
	private GraphType createGraphInGraphmlType(GraphmlType graphmlType, String graphId, String descriptionStr, String preferredLayout, Integer semanticZoomLevelInt) {

		// <graph id="graph1">
		GraphType graph = of.createGraphType();
		graph.setId(graphId);
		graph.setEdgedefault(GraphEdgedefaultType.UNDIRECTED);
		graphmlType.getGraphOrData().add(graph);

		// <data key="namespace">minimalistic</data>
		DataType namespace = of.createDataType();
		namespace.setKey(GraphMLKeyNames.NAMESPACE);
		namespace.setContent(graphId+"_ns");
		graph.getDataOrNodeOrEdge().add(namespace);

		// <data key="focus-strategy">ALL</data>
		DataType focusStrategy = of.createDataType();
		focusStrategy.setKey(GraphMLKeyNames.FOCUS_STRATEGY);
		focusStrategy.setContent("ALL");
		graph.getDataOrNodeOrEdge().add(focusStrategy);

		// <data key="preferred-layout">Circle Layout</data>
		if (preferredLayout!=null){
			DataType preferredLayoutDataType = of.createDataType();
			preferredLayoutDataType.setKey(GraphMLKeyNames.PREFERRED_LAYOUT);
			preferredLayoutDataType.setContent(preferredLayout);
			graph.getDataOrNodeOrEdge().add(preferredLayoutDataType);
		}

		// <data key="description">The Regions Layer.</data>
		if(descriptionStr!=null){
			DataType description = of.createDataType();
			description.setKey(GraphMLKeyNames.DESCRIPTION);
			description.setContent(descriptionStr);
			graph.getDataOrNodeOrEdge().add(description);
		}

		// <data key="semantic-zoom-level">0</data>
		if (semanticZoomLevelInt!=null){
			DataType semanticZoomLevel = of.createDataType();
			semanticZoomLevel.setKey(GraphMLKeyNames.SEMANTIC_ZOOM_LEVEL);
			semanticZoomLevel.setContent(Integer.toString(semanticZoomLevelInt));
			graph.getDataOrNodeOrEdge().add(semanticZoomLevel);
		}

		return graph;
	}


	/**
	 * Adds all of the OpenNMS nodes defined in the nodeInfo type to a graph. Adds nodeID and foreignsource / foreignid values if defined
	 * @param graph
	 * @param nodeInfo map with values Map<nodeId, Map<nodeParamLabelKey, nodeParamValue>>
	 *        nodeParamLabelKey a node asset parameter key (from those defined in org.opennms.plugins.graphml.asset.NodeParamLabels)
	 *        nodeParamValue a node asset value ( e.g. key NodeParamLabels.ASSET_RACK ('asset-rack') value: rack1
	 */
	private void addOpenNMSNodes(GraphType graph, Map<String, Map<String, String>> nodeInfo) {
		
		// set vertex-status-provider true for nodes graph
		//<data key="vertex-status-provider">true</data>
		DataType vertexStatusProvider = of.createDataType();
		vertexStatusProvider.setKey(GraphMLKeyNames.VERTEX_STATUS_PROVIDER);
		vertexStatusProvider.setContent("true");
		graph.getDataOrNodeOrEdge().add(vertexStatusProvider);

		for (String nodeId:nodeInfo.keySet()){

			NodeType node = of.createNodeType();
			graph.getDataOrNodeOrEdge().add(node);

			Map<String, String> nodeParamaters = nodeInfo.get(nodeId);
			String foreignSourceStr= nodeParamaters.get(NodeParamLabels.NODE_FOREIGNSOURCE);
			String foreignIdStr= nodeParamaters.get(NodeParamLabels.NODE_FOREIGNID);

			if (foreignIdStr==null
					|| "".equals(foreignIdStr) 
					|| foreignSourceStr==null 
					|| "".equals(foreignSourceStr)){
				// <data key="nodeID">1</data>
				DataType nodeID = of.createDataType();
				nodeID.setKey(GraphMLKeyNames.NODE_ID);
				nodeID.setContent(nodeId);
				node.getDataOrPort().add(nodeID);
			} else {
				// <data key="foreignSource">opennms-test</data>
				DataType foreignSource = of.createDataType();
				foreignSource.setKey(GraphMLKeyNames.FOREIGN_SOURCE);
				foreignSource.setContent(foreignSourceStr);
				node.getDataOrPort().add(foreignSource);

				// <data key="foreignID">1483632606160</data>
				DataType foreignID = of.createDataType();
				foreignID.setKey(GraphMLKeyNames.FOREIGN_ID);
				foreignID.setContent(foreignIdStr);
				node.getDataOrPort().add(foreignID);
			}

			String nodeLabelStr = nodeParamaters.get(NodeParamLabels.NODE_NODELABEL);
			node.setId(nodeLabelStr);

			DataType nodeLabel = of.createDataType();
			nodeLabel.setKey(GraphMLKeyNames.LABEL);
			nodeLabel.setContent(nodeLabelStr);
			node.getDataOrPort().add(nodeLabel);

		}


	}

}
