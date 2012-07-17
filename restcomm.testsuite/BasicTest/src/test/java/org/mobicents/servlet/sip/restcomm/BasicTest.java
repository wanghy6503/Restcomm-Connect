/**
 * 
 */
package org.mobicents.servlet.sip.restcomm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.CallFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.resource.instance.Call;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 * 
 */
@RunWith(Arquillian.class)
public class BasicTest {
  private static final String applications = "/home/thomas/Applications";
  private static final String projects = "/home/thomas/Projects";

  @ArquillianResource private Deployer deployer;

  @BeforeClass public static void beforeClass(){
    // using ProcessBuilder to spawn an process
	final ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", applications + "/mms-server/bin/run.sh");

	// set up the working directory.
	// Note the method is called "directory" not "setDirectory"
	builder.directory(new File( applications + "/mms-server/bin/" ));

	// merge child's error and normal output streams.
	// Note it is not called setRedirectErrorStream.
	builder.redirectErrorStream(true);
		
	try {
	  builder.start();
	} catch(final IOException exception) {
	  exception.printStackTrace();
	}
  }

  @AfterClass public static void afterClass(){
    /*
     * In order to kill the previously create process and its childs create a kill.sh script that will contain the following:
     *    #!/bin/bash
     *    pkill -f run.sh
     */
	try {
	  new ProcessBuilder("/bin/bash", applications + "/mms-server/bin/kill.sh").start();
	} catch(final IOException exception) {
	  exception.printStackTrace();
	}
  }

  @Deployment(name="restcomm", managed=false, testable=false)
    public static WebArchive createTestArchive() {
    DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
    final File directory = new File(projects + "/RestComm/restcomm/restcomm.core/target/restcomm/");
    // Load archive from exploded directory
    WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
    archive.as(ExplodedImporter.class).importDirectory(directory);
    System.out.println(archive.toString());
    return archive;
  }

  @Test public void testSayVerb() throws InterruptedException, TwilioRestException {
    // Deploy application archive to the container.
    deployer.deploy("restcomm");

	final TwilioRestClient client = new TwilioRestClient("ACae6e420f425248d6a26948c17a9e2acf",
        "77f8c12cc7b8f8423e5c38b035249166");
	final Account account = client.getAccount();
	final CallFactory factory = account.getCallFactory();
	final Map<String, String> parameters = new HashMap<String, String>();
	parameters.put("To", "+15126002188");
	parameters.put("From", "(512) 600-2188");
	parameters.put("Url", "http://192.168.1.106:8080/restcomm/tests/SayVerb");
	final Call call = factory.create(parameters);
	wait(5 * 1000);
	call.hangup();
  }
}
