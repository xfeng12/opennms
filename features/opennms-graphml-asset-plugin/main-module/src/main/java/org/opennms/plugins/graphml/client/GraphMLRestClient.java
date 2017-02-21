package org.opennms.plugins.graphml.client;

import org.graphdrawing.graphml.xmlns.GraphmlType;

public interface GraphMLRestClient {

	/**
	 * @return the userName
	 */
	String getUserName();

	/**
	 * User name to use in basic authentication
	 * If userName is null then no basic authentication is generated
	 * @param userName the userName to set
	 */
	void setUserName(String userName);

	/**
	 * @return the password
	 */
	String getPassword();

	/**
	 * password to use in basic authentication.
	 * password must not be set to null but if not set, password will default to empty string "".
	 * @param password the password to set
	 */
	void setPassword(String password);

	/**
	 * base URL of service as http://HOSTNAME:PORT e.g http://localhost:8181
	 * @return baseUrl
	 */
	String getBaseUrl();

	/**
	 * base URL of service as http://HOSTNAME:PORT/ e.g http://localhost:8181
	 * @param baseUrl
	 */
	void setBaseUrl(String baseUrl);

	/**
	 * base path of service starting with '/' such that service is accessed using baseUrl/basePath... 
	 * e.g http://localhost:8181/opennms/rest
	 * @return basePath
	 */
	String getBasePath();

	/**
	 * base path of service starting with '/' such that service is accessed using baseUrl/basePath... 
	 * e.g http://localhost:8181/featuremgr
	 * @return basePath
	 */
	void setBasePath(String basePath);

	/**
	 * calls opennms rest method to create new graph using a graphmlType
	 * @param graphname
	 * @param graphmlType
	 * @throws GraphmlClientException if graph cannot be created or connectivity fails
	 */
	void createGraph(String graphname, GraphmlType graphmlType)
			throws GraphmlClientException;

	/**
	 * deletes graph with name graphname. GraphmlClientException if graph cannot be deleted because not found or if IO exception
	 * @param graphname
	 * @throws GraphmlClientException
	 */
	void deleteGraph(String graphname) throws GraphmlClientException;

	/**
	 * returns a GraphmlType graph from opennms if graph is available. Returns null if graph doesn't exist 
	 * @param graphname
	 * @return
	 * @throws GraphmlClientException
	 */
	GraphmlType getGraph(String graphname) throws GraphmlClientException;

}