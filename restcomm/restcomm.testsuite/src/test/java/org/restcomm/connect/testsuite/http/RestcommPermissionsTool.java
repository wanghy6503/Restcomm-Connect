package org.restcomm.connect.testsuite.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

public class RestcommPermissionsTool {
    private static Logger logger = Logger.getLogger(RestcommPermissionsTool.class.getName());

    private static RestcommPermissionsTool instance;
    private static String accountsUrl;

    private RestcommPermissionsTool() {    }

    public static RestcommPermissionsTool getInstance() {
        if (instance == null)
            instance = new RestcommPermissionsTool();

        return instance;
    }

    private String getUrl(String deploymentUrl, Boolean xml) {
            if (deploymentUrl.endsWith("/")) {
                deploymentUrl = deploymentUrl.substring(0, deploymentUrl.length() - 1);
            }
            if(xml){
                accountsUrl = deploymentUrl + "/2012-04-24/Permissions";
            } else {
                accountsUrl = deploymentUrl + "/2012-04-24/Permissions.json";
            }

        return accountsUrl;
    }

    public JsonObject postPermission(String deploymentUrl, String adminUsername, String adminAuthToken,
                                        MultivaluedMap<String, String> permissionParams) {
        return postPermission(deploymentUrl, adminUsername, adminAuthToken, permissionParams, false);
    }

    public JsonObject postPermission(String deploymentUrl, String adminUsername, String adminAuthToken,
                                        MultivaluedMap<String, String> permissionParams, Boolean xml) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String url = getUrl(deploymentUrl, xml);

        WebResource webResource = jerseyClient.resource(url);

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, permissionParams);
        jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        return jsonResponse;
    }

    public JsonObject getPermission(String deploymentUrl, String adminUsername, String adminAuthToken, String permissionSid) {
        return getPermission(deploymentUrl, adminUsername, adminAuthToken, permissionSid, false);
    }

    public JsonObject getPermission(String deploymentUrl, String adminUsername, String adminAuthToken, String permissionSid, Boolean xml) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        WebResource webResource = jerseyClient.resource(getUrl(deploymentUrl, xml));

        String response = webResource.path(permissionSid).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();

        return jsonResponse;
    }

    public JsonObject updatePermission(String deploymentUrl, String adminUsername, String adminAuthToken, String objectSid,
                                          MultivaluedMap<String, String> configurationParams) {
        return updatePermission(deploymentUrl, adminUsername, adminAuthToken, objectSid, configurationParams, false);
    }

    public JsonObject updatePermission(String deploymentUrl, String adminUsername, String adminAuthToken, String objectSid,
                                          MultivaluedMap<String, String> configurationParams, Boolean xml) {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = null;

        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String url = getUrl(deploymentUrl, xml);

        WebResource webResource = jerseyClient.resource(url).path(objectSid);

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, configurationParams);
        jsonResponse = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        return jsonResponse;
    }

    public void removePermission(String deploymentUrl, String adminUsername, String adminAuthToken, String objectSid) {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String url = getUrl(deploymentUrl, true);

        WebResource webResource = jerseyClient.resource(url).path(objectSid);
        webResource.accept(MediaType.APPLICATION_JSON).delete();
    }

}
