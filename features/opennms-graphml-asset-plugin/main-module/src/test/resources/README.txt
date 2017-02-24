This folder contains test resources.
------------------------------------

Data for test AssetTopologyMapperImplXMLDataTest
------------------------------------------------
nodeInfoMockTestData2.xml is data to load into a nodeInforepository to simulate OpenNMS node 
asset data when generating a topology

graphmlTestTopology2.xml is the graph which should be generated from nodeInfoMockTestData2.xml
by the test.

The opennms inventory which (nearly) matches nodeInfoMockTestData.xml is in the requisition file
This can be used to generate a demonstration OpenNMS topology
TestAssetOpenNMSInventory.xml

Data for test GraphMLRestJerseyClientTest
-----------------------------------------
test-graph.xml is used  by this test to post a graphml topology to OpenNMS.
Please note that this test can only be run manually against a running OpenNMS 
at localhost.