/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.testsuite.http;

import static org.junit.Assert.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.testsuite.http.RestcommUsageRecordsTool;

import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:abdulazizali@acm.org">abdulazizali77</a>
 */

@RunWith(Arquillian.class)
public class PermissionsTest {
    private static Logger logger = Logger.getLogger(PermissionsTest.class);

    private static final String version = Version.getVersion();
    private static final String revision = Version.getRevision();

    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @Test
    public void testGetPermission() {
        
    }

    @Test
    public void testAddPermission() {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminAccountSid, adminAuthToken));

        String url = deploymentUrl + "2012-04-24/Permissions.json";

        WebResource webResource = jerseyClient.resource(url);
        MultivaluedMap<String, String> configurationParams = new MultivaluedMapImpl();
        configurationParams.add("Name", "RestComm:*:USSD");

        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, configurationParams);

        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(clientResponse.getEntity(String.class)).getAsJsonObject();
        System.out.println(json);
        String sidStr = json.get("sid").getAsJsonObject().get("id").getAsString();

        String response = webResource.path(sidStr).get(String.class);

        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
    }
    @Test
    public void testUpdatePermissionByName() {}
    @Test
    public void testUpdatePermission() {
        String oldName1 = "RestComm:*:OLD1";
        String oldName2 = "RestComm:*:OLD2";
        String newName1 = "RestComm:*:USSD";
        String newName2 = "RestComm:*:ASR";
        String permissionSid1 = "PE00000000000000000000000000000002";
        String permissionSid2 = "PE00000000000000000000000000000002";
        MultivaluedMap<String, String> configurationParams = null;
        JsonObject permission1;
        JsonObject permission2;

        //get
        permission1 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, permissionSid1);

        //update 1
        configurationParams = new MultivaluedMapImpl();
        configurationParams.add("Name", newName1);
        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, permissionSid1);
        assertTrue(permission2.get("name").getAsString().equals(oldName1));
        permission1 = RestcommPermissionsTool.getInstance().updatePermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, permissionSid1, configurationParams);
        permission1.get("sid").getAsJsonObject().get("id").getAsString();
        permission1.get("name").getAsString();
        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, permissionSid1);
        assertTrue(permission2.get("name").getAsString().equals(newName1));

        //update2
        configurationParams = new MultivaluedMapImpl();
        configurationParams.add("Name", newName2);
        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, permissionSid2);
        assertTrue(permission2.get("name").getAsString().equals(oldName2));
        permission1 = RestcommPermissionsTool.getInstance().updatePermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, permissionSid2, configurationParams, false);
        permission1.get("sid").getAsJsonObject().get("id").getAsString();
        permission1.get("name").getAsString();
        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, permissionSid2);
        assertTrue(permission2.get("name").getAsString().equals(newName2));
    }

    @Test
    public void testDeletePermission() {
        //delete
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminAccountSid, adminAuthToken));

        String url = deploymentUrl + "2012-04-24/Permissions.json";

        WebResource webResource = jerseyClient.resource(url);
        Sid sid1 = new Sid("PE00000000000000000000000000000002");
        Sid sid2 = new Sid("PE00000000000000000000000000000003");

        //get
        ClientResponse clientResponse = webResource.path(sid1.toString()).get(ClientResponse.class);
        //delete
        clientResponse = webResource.path(sid1.toString()).delete(ClientResponse.class);
        //get
        clientResponse = webResource.path(sid1.toString()).delete(ClientResponse.class);

    }
    @Test
    public void testDeletePermissionByName() {
        
    }

    @Test
    public void testGetPermissionsList() {
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminAccountSid, adminAuthToken));

        String url = deploymentUrl + "2012-04-24/Permissions.json";

        WebResource webResource = jerseyClient.resource(url);

        String response = webResource.accept(MediaType.APPLICATION_JSON).get(String.class);
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(response).getAsJsonObject();
    }

    @Deployment(name = "PermissionsTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        // archive.delete("/WEB-INF/data/hsql/restcomm.properties");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_permissions_test", "data/hsql/restcomm.script");
        // archive.addAsWebInfResource("restcomm.properties", "data/hsql/restcomm.properties");

        //archive.delete("/WEB-INF/conf/mybatis.xml");
        //archive.addAsWebInfResource("mybatis_win.xml", "conf/mybatis.xml");
        return archive;
    }
}