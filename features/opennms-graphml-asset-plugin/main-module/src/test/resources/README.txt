This folder contains test resources.
------------------------------------

Data for test AssetTopologyMapperImplXMLDataTest
------------------------------------------------
nodeInfoMockTestData.xml is data to load into a nodeInforepository to simulate OpenNMS node 
asset data when generating a topology

graphmlTestTopology.xml is the graph which should be generated from nodeInfoMockTestData.xml
by the test.

The opennms inventory which matches nodeInfoMockTestData.xml is in the requisition file
TestAssetOpenNMSInventory.xml

Data for test GraphMLRestJerseyClientTest
-----------------------------------------
test-graph.xml is used  by this test to post a graphml topology to OpenNMS.
Please note that this test can only be run manually against a running OpenNMS 
at localhost.