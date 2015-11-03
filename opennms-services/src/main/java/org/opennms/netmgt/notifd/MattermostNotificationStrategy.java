/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.notifd;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opennms.core.web.HttpClientWrapper;
import org.opennms.netmgt.config.NotificationManager;
import org.opennms.netmgt.model.notifd.Argument;
import org.opennms.netmgt.model.notifd.NotificationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>MattermostNotificationStrategy class.</p>
 *
 * @author <a href="mailto:jeffg@opennms.org">Jeff Gehlbach</a>
 * @version $Id: $
 */
public class MattermostNotificationStrategy implements NotificationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(MattermostNotificationStrategy.class);
    
    private static final String MM_URL_PROPERTY = "org.opennms.netmgt.notifd.mattermost.webhookURL";
    private static final String MM_USERNAME_PROPERTY = "org.opennms.netmgt.notifd.mattermost.username";
    private static final String MM_ICONURL_PROPERTY = "org.opennms.netmgt.notifd.mattermost.iconURL";
    private static final String MM_CHANNEL_PROPERTY = "org.opennms.netmgt.notifd.mattermost.channel";
    
    private List<Argument> m_arguments;

    /* (non-Javadoc)
     * @see org.opennms.netmgt.notifd.NotificationStrategy#send(java.util.List)
     */
    /** {@inheritDoc} */
    @Override
    public int send(List<Argument> arguments) {

        m_arguments = arguments;

        String url = getUrl();
        if (url == null) {
            LOG.error("send: url must not be null");
            return 1;
        }
        String iconUrl = getIconUrl();
        String channel = getChannel();
        String message = buildMessage(arguments);

        final HttpClientWrapper clientWrapper = HttpClientWrapper.create()
                .setConnectionTimeout(3000)
                .setSocketTimeout(3000)
                .useSystemProxySettings();

        HttpPost postMethod = new HttpPost(url);

        JSONObject jsonData = new JSONObject();
        jsonData.put("username", getUsername());
        if (iconUrl != null) {
        	jsonData.put("icon_url", iconUrl);
        }
        if (channel != null) {
        	jsonData.put("channel", channel);
        }
        jsonData.put("text", message);

        LOG.debug("Prepared JSON POST data for webhook is: {}", jsonData.toJSONString());
        final HttpEntity entity = new StringEntity(jsonData.toJSONString(), ContentType.APPLICATION_JSON);
        postMethod.setEntity(entity);
        // Mattermost 1.1.0 does not like having charset specified alongside Content-Type
        postMethod.setHeader("Content-Type", "application/json");

        String contents = null;
        int statusCode = -1;
        try {
            CloseableHttpResponse response = clientWrapper.getClient().execute(postMethod);
            statusCode = response.getStatusLine().getStatusCode();
            contents = EntityUtils.toString(response.getEntity());
            LOG.debug("send: Contents is: {}", contents);
        } catch (IOException e) {
            LOG.error("send: I/O problem with webhook post/response: {}", e);
            throw new RuntimeException("Problem with webhook post: "+e.getMessage());
        } finally {
            IOUtils.closeQuietly(clientWrapper);
        }
        
        if ("ok".equals(contents)) {
        	LOG.debug("Got 'ok' back from webhook, indicating success.");
        	statusCode = 0;
        } else {
        	LOG.info("Got a non-ok response from webhook, attempting to dissect response.");
        	LOG.error("Webhook returned non-OK response to notification post: {}", formatWebhookErrorResponse(statusCode, contents));
        	statusCode = 1;
        }

        return statusCode;
    }
    
    protected String formatWebhookErrorResponse(int statusCode, String contents) {
    	StringBuilder bldr = new StringBuilder("Response code: ");
    	bldr.append(statusCode);
    	
    	JSONObject errorJson = new JSONObject();
    	JSONParser jp = new JSONParser();
    	try {
			Object parsedError = jp.parse(contents);
			if (parsedError instanceof JSONObject) {
				LOG.debug("Got back some JSON. Parsing for dissection.");
				errorJson = (JSONObject)parsedError;
			}
		} catch (ParseException e) {
			LOG.warn("Got some non-JSON error.");
			bldr.append(" Contents:").append(contents);
			return bldr.toString();
		}
    	
    	bldr.append("; Message: ").append(errorJson.get("message"));
    	bldr.append("; Detailed error: ").append(errorJson.get("detailed_error"));
    	bldr.append("; Request ID: ").append(errorJson.get("request_id"));
    	bldr.append("; Status code: ").append(errorJson.get("status_code"));
    	bldr.append("; Is OAUTH?: ").append(errorJson.get("is_oauth"));
    	return bldr.toString();
    }

    protected String getUrl() {
    	String url = getValueFromSwitchOrProp("Webhook URL", "url", getUrlPropertyName());

    	if (url == null) {
    		LOG.error("No webhook URL specified as a notification command switch or via system property {}. Cannot continue.", getUrlPropertyName());
    	}
    	return url;
    }
    
    protected String getUsername() {
    	String username = getValueFromSwitchOrProp("Bot username", "username", getUsernamePropertyName());
    	
    	if (username == null) {
    		LOG.warn("No bot username specified as a notification command switch or via system property {}. Using default value opennms.", getUsernamePropertyName());
    		return "opennms";
    	}
    	return username;
    }
    
    protected String getIconUrl() {
    	String iconurl = getValueFromSwitchOrProp("Icon URL", "iconurl", getIconUrlPropertyName());
    	
    	if (iconurl == null) {
    		LOG.warn("No icon URL specified as a notification command switch or via system property {}. Not setting one.", getIconUrlPropertyName());
    	}
    	return iconurl;
    }
    
    protected String getChannel() {
    	String channel = getValueFromSwitchOrProp("Channel name", "channel", getChannelPropertyName());
    	
    	if (channel == null) {
    		LOG.warn("No channel specified as a notification command switch or via system property {}. Not setting one.", getChannelPropertyName());
    	}
    	return channel;
    }

    
    protected String getValueFromSwitchOrProp(String what, String switchName, String propName) {
    	LOG.debug("Trying to get {} from notification switch {}", what, switchName);
    	String val = getSwitchValue(switchName);
    	if (val != null) {
    		LOG.info("Using {} value {} from notification switch {}", what, val, switchName);
    		return val;
    	}
    	LOG.debug("Trying to get {} from system property {}", what, propName);
    	val = System.getProperty(propName);
    	if (val != null) {
    		LOG.info("Using {} value {} from system property {}", what, val, propName);
    		return val;
    	}
    	
    	LOG.warn("Could not determine value for {} from notification command switch {} or system property {}", what, switchName, propName);
    	return null;
    }

    /**
     * Helper method to look into the Argument list and return the associated value.
     * If the value is an empty String, this method returns null.
     * @param argSwitch
     * @return
     */
    private String getSwitchValue(String argSwitch) {
        String value = null;
        for (Iterator<Argument> it = m_arguments.iterator(); it.hasNext();) {
            Argument arg = it.next();
            if (arg.getSwitch().equals(argSwitch)) {
                value = arg.getValue();
            }
        }
        if (value != null && value.equals(""))
            value = null;

        return value;
    }
    
    protected String buildMessage(List<Argument> args) {
    	StringBuilder bldr = new StringBuilder("#### ");
    	for (Argument arg : args) {
    		if (NotificationManager.PARAM_SUBJECT.equals(arg.getSwitch())) {
    			bldr.append(arg.getValue());
    			bldr.append("\n");
    		}
    	}
    	for (Argument arg : args) {
    		if (NotificationManager.PARAM_TEXT_MSG.equals(arg.getSwitch())) {
    			bldr.append(arg.getValue());
    		}
    	}
    	return bldr.toString();
    }
    
    protected String getUrlPropertyName() {
    	return MM_URL_PROPERTY;
    }
    
    protected String getUsernamePropertyName() {
    	return MM_USERNAME_PROPERTY;
    }
    
    protected String getIconUrlPropertyName() {
    	return MM_ICONURL_PROPERTY;
    }
    
    protected String getChannelPropertyName() {
    	return MM_CHANNEL_PROPERTY;
    }
}