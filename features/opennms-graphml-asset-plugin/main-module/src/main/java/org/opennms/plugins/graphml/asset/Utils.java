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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.graphdrawing.graphml.xmlns.GraphmlType;
import org.graphdrawing.graphml.xmlns.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
	
	public static void writeFileToDisk(String content, String filename, String filefolder ){
		File folder = new File(filefolder);
		File file = new File(folder, filename );
		LOG.info("writing to file:"+file.getAbsolutePath());
		PrintWriter writer=null;
		try{
			folder.mkdirs();
			writer = new PrintWriter(file, "UTF-8");
			writer.println(content);
		} catch (IOException e) {
			LOG.error("problem writing file:"+file.getAbsolutePath(),e);
		}finally{
			if (writer!=null) writer.close();
		}
	}

	public static String readFileFromDisk(String filefolder, String fileName){
		String xmlString=null;
		BufferedReader bufReader=null;
		try{
			File folder = new File(filefolder);
			File file = new File(folder, fileName );

			// get file contents as String using BufferedReader
			Reader fileReader = new FileReader(file);
			bufReader = new BufferedReader(fileReader);

			StringBuilder sb = new StringBuilder();
			String line = bufReader.readLine();
			while( line != null){
				sb.append(line).append("\n");
				line = bufReader.readLine();
			}

			xmlString = sb.toString();
		} catch(Exception e){
			throw new RuntimeException("problem reading file ",e);
		} finally{
			if (bufReader!=null) try{
				bufReader.close();
			} catch (Exception e){}
		}

		return xmlString;
	}

	public static String graphmlTypeToString(GraphmlType graphmlType){
		try {
			
		      // Marshal graphmlType
	        ObjectFactory objectFactory = new ObjectFactory();
	        JAXBElement<GraphmlType> je =  objectFactory.createGraphml(graphmlType);
	        
			StringWriter sw = new StringWriter();
			JAXBContext ctx = JAXBContext.newInstance(org.graphdrawing.graphml.xmlns.ObjectFactory.class);

			Marshaller marshaller = ctx.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(je, sw);

			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException("problem marshaling graphml to string ",e);
		}
		
	}
	

	public static GraphmlType readTopologyFileFromDisk(String topologyFileFolder, String topologyFilename ){
		File topologyFolder = new File(topologyFileFolder);
		File graphmlfile = new File(topologyFolder, topologyFilename);
		LOG.info("reading from file:"+graphmlfile.getAbsolutePath());

		JAXBContext ctx;
		JAXBElement<GraphmlType> jaxbgraph=null;
		GraphmlType graph=null;
		try {
			//ctx = JAXBContext.newInstance("org.graphdrawing.graphml.xmlns");
			ctx = JAXBContext.newInstance(org.graphdrawing.graphml.xmlns.ObjectFactory.class);
			Unmarshaller jaxbUnmarshaller = ctx.createUnmarshaller();
			jaxbgraph =  (JAXBElement<GraphmlType>) jaxbUnmarshaller.unmarshal(graphmlfile);
			graph = jaxbgraph.getValue();

			return graph;
		} catch (JAXBException e) {
			LOG.error("problem reading file:"+graphmlfile.getAbsolutePath(),e);
			throw new RuntimeException("problem reading file:"+graphmlfile.getAbsolutePath(),e);
		}
	}

}
