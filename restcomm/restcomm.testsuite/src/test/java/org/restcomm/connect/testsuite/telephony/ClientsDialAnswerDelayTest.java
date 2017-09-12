package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.CreateClientsTool;

import javax.sip.Dialog;
import javax.sip.address.SipURI;
import javax.sip.header.UserAgentHeader;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.restcomm.connect.testsuite.NetworkPortAssigner;
import org.restcomm.connect.testsuite.WebArchiveUtil;

/**
 * Test for clients with or without VoiceURL (Bitbucket issue 115). Clients without VoiceURL can dial anything.
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
@RunWith(Arquillian.class)
public class ClientsDialAnswerDelayTest {

    private static final String version = Version.getVersion();

    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
        53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
        48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
        13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
        86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
    private static final String body = new String(bytes);

    private static final String webRtcBody = "v=0\n" +
            "o=- 655836341935372758 2 IN IP4 24.217.142.20\n" +
            "s=Restcomm B2BUA\n" +
            "t=0 0\n" +
            "a=group:BUNDLE audio video\n" +
            "a=msid-semantic:WMS ARDAMS\n" +
            "m=audio 9 UDP/TLS/RTP/SAVPF 111 103 9 102 0 8 106 105 13 126\n" +
            "c=IN IP4 24.217.142.20\n" +
            "a=rtcp:9 IN IP4 0.0.0.0\n" +
            "a=candidate:704553097 1 udp 2122260223 192.168.1.3 60475 typ host generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:2158047068 1 udp 1686052607 24.217.142.20 60475 typ srflx raddr 192.168.1.3 rport 60475 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:152961445 1 udp 41885695 50.97.253.79 55954 typ relay raddr 24.217.142.20 rport 60475 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:152961445 1 udp 41886207 50.97.253.79 49214 typ relay raddr 24.217.142.20 rport 60475 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:1201536341 1 udp 25108735 50.97.253.79 63824 typ relay raddr 24.217.142.20 rport 54517 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:1201536341 1 udp 25108223 50.97.253.79 50246 typ relay raddr 24.217.142.20 rport 33927 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=ice-ufrag:PFs2\n" +
            "a=ice-pwd:MZHsJBkJ+vdWaNvb4Z7iCejt\n" +
            "a=fingerprint:sha-256 56:B1:98:06:06:EA:B1:BB:31:51:28:35:C7:8C:45:11:CD:3A:0D:28:C1:10:5B:D5:99:EA:21:46:8E:ED:08:89\n" +
            "a=setup:actpass\n" +
            "a=mid:audio\n" +
            "a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level\n" +
            "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\n" +
            "a=sendrecv\n" +
            "a=rtcp-mux\n" +
            "a=rtpmap:111 opus/48000/2\n" +
            "a=rtcp-fb:111 transport-cc\n" +
            "a=fmtp:111 minptime=10;useinbandfec=1\n" +
            "a=rtpmap:103 ISAC/16000\n" +
            "a=rtpmap:9 G722/8000\n" +
            "a=rtpmap:102 ILBC/8000\n" +
            "a=rtpmap:0 PCMU/8000\n" +
            "a=rtpmap:8 PCMA/8000\n" +
            "a=rtpmap:106 CN/32000\n" +
            "a=rtpmap:105 CN/16000\n" +
            "a=rtpmap:13 CN/8000\n" +
            "a=rtpmap:126 telephone-event/8000\n" +
            "a=ssrc:743432057 cname:USPxCkO2V7yoEmg/\n" +
            "a=ssrc:743432057 msid:ARDAMS ARDAMSa0\n" +
            "a=ssrc:743432057 mslabel:ARDAMS\n" +
            "a=ssrc:743432057 label:ARDAMSa0\n" +
            "m=video 9 UDP/TLS/RTP/SAVPF 100 101 116 117 121 96 97 98 99\n" +
            "c=IN IP4 24.217.142.20\n" +
            "a=rtcp:9 IN IP4 0.0.0.0\n" +
            "a=candidate:704553097 1 udp 2122260223 192.168.1.3 40839 typ host generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:2158047068 1 udp 1686052607 24.217.142.20 40839 typ srflx raddr 192.168.1.3 rport 40839 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:152961445 1 udp 41886207 50.97.253.79 53691 typ relay raddr 24.217.142.20 rport 40839 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:152961445 1 udp 41885695 50.97.253.79 63503 typ relay raddr 24.217.142.20 rport 40839 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:1201536341 1 udp 25108735 50.97.253.79 56359 typ relay raddr 24.217.142.20 rport 33643 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=candidate:1201536341 1 udp 25108223 50.97.253.79 54107 typ relay raddr 24.217.142.20 rport 44827 generation 0 ufrag PFs2 network-id 3 network-cost 10\n" +
            "a=ice-ufrag:PFs2\n" +
            "a=ice-pwd:MZHsJBkJ+vdWaNvb4Z7iCejt\n" +
            "a=fingerprint:sha-256 56:B1:98:06:06:EA:B1:BB:31:51:28:35:C7:8C:45:11:CD:3A:0D:28:C1:10:5B:D5:99:EA:21:46:8E:ED:08:89\n" +
            "a=setup:actpass\n" +
            "a=mid:video\n" +
            "a=extmap:2 urn:ietf:params:rtp-hdrext:toffset\n" +
            "a=extmap:3 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time\n" +
            "a=extmap:4 urn:3gpp:video-orientation\n" +
            "a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay\n" +
            "a=sendrecv\n" +
            "a=rtcp-mux\n" +
            "a=rtcp-rsize\n" +
            "a=rtpmap:100 VP8/90000\n" +
            "a=rtcp-fb:100 ccm fir\n" +
            "a=rtcp-fb:100 nack\n" +
            "a=rtcp-fb:100 nack pli\n" +
            "a=rtcp-fb:100 goog-remb\n" +
            "a=rtcp-fb:100 transport-cc\n" +
            "a=rtpmap:101 VP9/90000\n" +
            "a=rtcp-fb:101 ccm fir\n" +
            "a=rtcp-fb:101 nack\n" +
            "a=rtcp-fb:101 nack pli\n" +
            "a=rtcp-fb:101 goog-remb\n" +
            "a=rtcp-fb:101 transport-cc\n" +
            "a=rtpmap:116 red/90000\n" +
            "a=rtpmap:117 ulpfec/90000\n" +
            "a=rtpmap:121 H264/90000\n" +
            "a=rtcp-fb:121 ccm fir\n" +
            "a=rtcp-fb:121 nack\n" +
            "a=rtcp-fb:121 nack pli\n" +
            "a=rtcp-fb:121 goog-remb\n" +
            "a=rtcp-fb:121 transport-cc\n" +
            "a=rtpmap:96 rtx/90000\n" +
            "a=fmtp:96 apt=100\n" +
            "a=rtpmap:97 rtx/90000\n" +
            "a=fmtp:97 apt=101\n" +
            "a=rtpmap:98 rtx/90000\n" +
            "a=fmtp:98 apt=116\n" +
            "a=rtpmap:99 rtx/90000\n" +
            "a=fmtp:99 apt=121\n" +
            "a=ssrc-group:FID 2457543170 3154322644\n" +
            "a=ssrc:2457543170 cname:USPxCkO2V7yoEmg/\n" +
            "a=ssrc:2457543170 msid:ARDAMS ARDAMSv0\n" +
            "a=ssrc:2457543170 mslabel:ARDAMS\n" +
            "a=ssrc:2457543170 label:ARDAMSv0\n" +
            "a=ssrc:3154322644 cname:USPxCkO2V7yoEmg/\n" +
            "a=ssrc:3154322644 msid:ARDAMS ARDAMSv0\n" +
            "a=ssrc:3154322644 mslabel:ARDAMS\n" +
            "a=ssrc:3154322644 label:ARDAMSv0";

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    static int mockPort = NetworkPortAssigner.retrieveNextPortByFile();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(mockPort);
    
    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;
    private static SipStackTool tool5;
    private static SipStackTool tool6;
    private static SipStackTool tool7;
    private static SipStackTool tool8;
    private static SipStackTool tool9;

    private String pstnNumber = "+151261006100";

    private String clientPassword = "qwerty1234RT";
    
    // Maria is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack mariaSipStack;
    private SipPhone mariaPhone;
    private static String mariaPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String mariaContact = "sip:maria@127.0.0.1:" + mariaPort;    
    private String mariaRestcommClientSid;

    // Dimitris is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack dimitriSipStack;
    private SipPhone dimitriPhone;
    private static String dimitriPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String dimitriContact = "sip:dimitri@127.0.0.1:" + dimitriPort;
    private String dimitriRestcommClientSid;

    // Alice is a Restcomm Client with VoiceURL. This Restcomm Client can register with Restcomm and whatever will dial the RCML
    // of the VoiceURL will be executed.
    private SipStack aliceSipStack;
    private SipPhone alicePhone;
    private static String alicePort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());    
    private String aliceContact = "sip:alice@127.0.0.1:" + alicePort;
    

    private SipStack aliceSipStack2;
    private SipPhone alicePhone2;
    private static String alicePort2 = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());     
    private String aliceContact2 = "sip:alice@127.0.0.1:" + alicePort2;

    // George is a simple SIP Client. Will not register with Restcomm
    private SipStack georgeSipStack;
    private SipPhone georgePhone;
    private static String georgePort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());    
    private String georgeContact = "sip:"+pstnNumber+"@127.0.0.1:" + georgePort;

    private SipStack clientWithAppSipStack;
    private SipPhone clientWithAppPhone;
    private static String clientWithAppPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile()); 
    private String clientWithAppContact = "sip:clientWithApp@127.0.0.1:" + clientWithAppPort;
    private String clientWithAppClientSid;

    private SipStack fotiniSipStackTcp;
    private SipPhone fotiniPhoneTcp;
    private static String fotiniPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String fotiniContactTcp = "sip:fotini@127.0.0.1:" + fotiniPort;
    private String fotiniClientSid;

    private SipStack bobSipStackTcp;
    private SipPhone bobPhoneTcp;
    private static String bobPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String bobContactTcp = "sip:bob@127.0.0.1:" + bobPort;

    private SipStack leftySipStack;
    private SipPhone leftyPhone;
    private static String leftyPort = String.valueOf(NetworkPortAssigner.retrieveNextPortByFile());
    private String leftyContact = "sip:lefty@127.0.0.1:" + leftyPort;
    private String leftyRestcommClientSid;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    
    private static int restcommPort = 5080;
    private static int restcommHTTPPort = 8080;    
    private static String restcommContact = "127.0.0.1:" + restcommPort;     

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("ClientsDialAnswerDelayTest1");
        tool2 = new SipStackTool("ClientsDialAnswerDelayTest2");
        tool3 = new SipStackTool("ClientsDialAnswerDelayTest3");
        tool4 = new SipStackTool("ClientsDialAnswerDelayTest4");
        tool5 = new SipStackTool("ClientsDialAnswerDelayTest5");
        tool6 = new SipStackTool("ClientsDialAnswerDelayTest6");
        tool7 = new SipStackTool("ClientsDialAnswerDelayTest7");
        tool8 = new SipStackTool("ClientsDialAnswerDelayTest8");
        tool9 = new SipStackTool("ClientsDialAnswerDelayTest9");
        
        if (System.getProperty("arquillian_sip_port") != null) {
            restcommPort = Integer.valueOf(System.getProperty("arquillian_sip_port"));
            restcommContact = "127.0.0.1:" + restcommPort;          
        } 
        if (System.getProperty("arquillian_http_port") != null) {
            restcommHTTPPort = Integer.valueOf(System.getProperty("arquillian_http_port"));       
        }         
    }

    @Before
    public void before() throws Exception {

        aliceSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", alicePort, restcommContact);
        alicePhone = aliceSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, aliceContact);

        aliceSipStack2 = tool5.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", alicePort2, restcommContact);
        alicePhone2 = aliceSipStack2.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, aliceContact2);

        mariaSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", mariaPort, restcommContact);
        mariaPhone = mariaSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, mariaContact);

        dimitriSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", dimitriPort, restcommContact);
        dimitriPhone = dimitriSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, dimitriContact);

        georgeSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", georgePort, restcommContact);
        georgePhone = georgeSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, georgeContact);

        clientWithAppSipStack = tool6.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", clientWithAppPort, restcommContact);
        clientWithAppPhone = clientWithAppSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, clientWithAppContact);

        mariaRestcommClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "maria", clientPassword, null);
        dimitriRestcommClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "dimitri", clientPassword, null);
        clientWithAppClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "clientWithApp", clientPassword, "http://127.0.0.1:" + mockPort + "/1111");

        fotiniSipStackTcp = tool7.initializeSipStack(SipStack.PROTOCOL_TCP, "127.0.0.1", fotiniPort, restcommContact);
        fotiniPhoneTcp = fotiniSipStackTcp.createSipPhone("127.0.0.1", SipStack.PROTOCOL_TCP, restcommPort, fotiniContactTcp);
        fotiniClientSid = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "fotini", clientPassword, null);

        bobSipStackTcp = tool8.initializeSipStack(SipStack.PROTOCOL_TCP, "127.0.0.1", bobPort, restcommContact);
        bobPhoneTcp = bobSipStackTcp.createSipPhone("127.0.0.1", SipStack.PROTOCOL_TCP, restcommPort, bobContactTcp);

        leftySipStack = tool9.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", leftyPort, restcommContact);
        leftyPhone = leftySipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, leftyContact);
    }

    @After
    public void after() throws Exception {
        if (mariaPhone != null) {
            mariaPhone.dispose();
        }
        if (mariaSipStack != null) {
            mariaSipStack.dispose();
        }

        if (aliceSipStack != null) {
            aliceSipStack.dispose();
        }
        if (alicePhone != null) {
            alicePhone.dispose();
        }

        if (dimitriSipStack != null) {
            dimitriSipStack.dispose();
        }
        if (dimitriPhone != null) {
            dimitriPhone.dispose();
        }

        if (georgePhone != null) {
            georgePhone.dispose();
        }
        if (georgeSipStack != null) {
            georgeSipStack.dispose();
        }

        if (clientWithAppPhone != null) {
            clientWithAppPhone.dispose();
        }
        if (clientWithAppSipStack != null) {
            clientWithAppSipStack.dispose();
        }
        if (fotiniSipStackTcp != null) {
            fotiniSipStackTcp.dispose();
        }
        if (fotiniPhoneTcp != null) {
            fotiniPhoneTcp.dispose();
        }
        if (bobSipStackTcp != null) {
            bobSipStackTcp.dispose();
        }
        if (bobPhoneTcp != null) {
            bobPhoneTcp.dispose();
        }
        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(3000);
    }
    
    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testClientDialOutPstnSimulateWebRTCClient() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(mariaPhone.register(uri, "maria", clientPassword, mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", clientPassword);
        mariaPhone.addUpdateCredential(c);

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();


        Thread.sleep(1000);

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = mariaSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // Maria initiates a call to Dimitri
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+pstnNumber+"@" + restcommContact, null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
            assertNotNull(mariaDialog);
        }

        assertTrue(georgeCall.waitForIncomingCall(5 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "RINGING-George", 3600));

        SipRequest lastReceivedRequest = georgeCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, mariaCall.getLastReceivedResponse().getStatusCode());
        assertTrue(mariaCall.sendInviteOkAck());
        
        //        For a reason the ACK will never reach Restcomm. This is only when working with the sipUnit
        //        assertTrue(georgeCall.waitForAck(5 * 1000));

        Thread.sleep(3000);
        georgeCall.listenForDisconnect();
        assertTrue(mariaCall.disconnect());

        //        assertTrue(georgeCall.waitForDisconnect(5 * 1000));
        //        assertTrue(georgeCall.respondToDisconnect());
    }
    
    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testClientDialOutPstnSimulateWebRTCClientBusy() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(mariaPhone.register(uri, "maria", clientPassword, mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", clientPassword);
        mariaPhone.addUpdateCredential(c);

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();


        Thread.sleep(1000);

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = mariaSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // Maria initiates a call to Dimitri
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+pstnNumber+"@" + restcommContact, null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
            assertNotNull(mariaDialog);
        }

        assertTrue(georgeCall.waitForIncomingCall(5 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "RINGING-George", 3600));

        SipRequest lastReceivedRequest = georgeCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.BUSY_HERE, "Busy-George", 3600));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.BUSY_HERE, mariaCall.getLastReceivedResponse().getStatusCode());
    }
    
    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testClientDialOutPstnSimulateWebRTCClientNoAnswer() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(mariaPhone.register(uri, "maria", clientPassword, mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", clientPassword);
        mariaPhone.addUpdateCredential(c);

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();


        Thread.sleep(1000);

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = mariaSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // Maria initiates a call to Dimitri
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+pstnNumber+"@" + restcommContact, null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
            assertNotNull(mariaDialog);
        }

        assertTrue(georgeCall.waitForIncomingCall(5 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "RINGING-George", 3600));

        SipRequest lastReceivedRequest = georgeCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());

        assertTrue(mariaCall.waitOutgoingCallResponse(120 * 1000));
        assertEquals(Response.REQUEST_TIMEOUT, mariaCall.getLastReceivedResponse().getStatusCode());
    }
    
    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testClientDialOutPstnSimulateWebRTCClientServiceUnavailable() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(mariaPhone.register(uri, "maria", clientPassword, mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", clientPassword);
        mariaPhone.addUpdateCredential(c);

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();


        Thread.sleep(1000);

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = mariaSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // Maria initiates a call to Dimitri
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+pstnNumber+"@" + restcommContact, null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
            assertNotNull(mariaDialog);
        }

        assertTrue(georgeCall.waitForIncomingCall(5 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "RINGING-George", 3600));

        SipRequest lastReceivedRequest = georgeCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.SERVICE_UNAVAILABLE, "RequestTerminated-George", 3600));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.SERVICE_UNAVAILABLE, mariaCall.getLastReceivedResponse().getStatusCode());
    }
    
    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testClientDialOutPstnSimulateWebRTCClientCancelBefore200() throws ParseException, InterruptedException {

        assertNotNull(mariaRestcommClientSid);
        assertNotNull(dimitriRestcommClientSid);

        SipURI uri = mariaSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(mariaPhone.register(uri, "maria", clientPassword, mariaContact, 14400, 3600));
        Thread.sleep(3000);

        Credential c = new Credential("127.0.0.1", "maria", clientPassword);
        mariaPhone.addUpdateCredential(c);

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();


        Thread.sleep(1000);

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = mariaSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // Maria initiates a call to Dimitri
        final SipCall mariaCall = mariaPhone.createSipCall();
        mariaCall.initiateOutgoingCall(mariaContact, "sip:"+pstnNumber+"@" + restcommContact, null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(mariaCall);
        assertTrue(mariaCall.waitForAuthorisation(3000));

        assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
        int responseMaria = mariaCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseMaria == Response.TRYING || responseMaria == Response.RINGING);

        Dialog mariaDialog = null;

        if (responseMaria == Response.TRYING) {
            assertTrue(mariaCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, mariaCall.getLastReceivedResponse().getStatusCode());
            mariaDialog = mariaCall.getDialog();
            assertNotNull(mariaDialog);
        }

        assertTrue(georgeCall.waitForIncomingCall(5 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "RINGING-George", 3600));

        georgeCall.listenForCancel();
        
        SipTransaction mariaCancelTransaction = mariaCall.sendCancel();
        assertTrue(mariaCancelTransaction != null);

        SipTransaction georgeCancelTransaction = georgeCall.waitForCancel(5 * 1000);
        assertTrue(georgeCancelTransaction != null);
        georgeCall.respondToCancel(georgeCancelTransaction, 200, "OK-George", 3600);
    }
    
    private String dialClientRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>alice</Client></Dial></Response>";
    @Test
    public synchronized void testDialClientAlice() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialClientRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@" + restcommContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        Thread.sleep(3000);

        // hangup.
        georgeCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
    }
    
    public synchronized void testDialClientForkWithWebRTCAliceFromAnotherInstance() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialWebRTCClientForkRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@" + restcommContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));
        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        Thread.sleep(3000);

        // hangup.
        georgeCall.disconnect();

        aliceCall.listenForDisconnect();
        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());
    }
    
    private String dialAliceDimitriRcml= "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>alice</Client><Sip>"+dimitriContact+"</Sip></Dial></Response>";
    @Test
    public synchronized void testDialForkClient_AliceMultipleRegistrations_George() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialAliceDimitriRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        assertTrue(alicePhone2.register(uri, "alice", "1234", aliceContact2, 3600, 3600));


        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        SipCall aliceCall2 = alicePhone2.createSipCall();
        aliceCall2.listenForIncomingCall();

        SipCall dimitriCall = dimitriPhone.createSipCall();
        dimitriCall.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@" + restcommContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));

        assertTrue(aliceCall2.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall2.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice2", 3600));
        aliceCall2.listenForCancel();

        assertTrue(dimitriCall.waitForIncomingCall(30 * 1000));
        assertTrue(dimitriCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Dimitri", 3600));
        dimitriCall.listenForCancel();

        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        SipTransaction aliceCall2CancelTransaction = aliceCall2.waitForCancel(5000);
        SipTransaction dimitriCallCancelTransaction = dimitriCall.waitForCancel(5000);
        assertNotNull(aliceCall2CancelTransaction);
        assertNotNull(dimitriCallCancelTransaction);
        aliceCall2.respondToCancel(aliceCall2CancelTransaction, 200, "OK-2-Cancel-Alice2", 3600);
        dimitriCall.respondToCancel(dimitriCallCancelTransaction, 200, "OK-2-Cancel-Dimitr", 3600);

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        Thread.sleep(3000);

        // hangup.
        aliceCall.listenForDisconnect();

        georgeCall.disconnect();

        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());

        Thread.sleep(500);
    }
    
    private String dialWebRTCClientForkRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Client>bob</Client><Client>alice</Client></Dial></Response>";
    @Test
    public synchronized void testDialForkClientWebRTCBob_And_AliceWithMultipleRegistrations() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialWebRTCClientForkRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        assertTrue(alicePhone2.register(uri, "alice", "1234", aliceContact2, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        SipCall aliceCall2 = alicePhone2.createSipCall();
        aliceCall2.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@" + restcommContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));


        assertTrue(aliceCall2.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall2.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice2", 3600));
        aliceCall2.listenForCancel();

        String receivedBody = new String(aliceCall.getLastReceivedRequest().getRawContent());
        assertTrue(aliceCall.sendIncomingCallResponse(Response.OK, "OK-Alice", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(aliceCall.waitForAck(50 * 1000));

        SipTransaction aliceCall2CancelTransaction = aliceCall2.waitForCancel(5000);
        assertNotNull(aliceCall2CancelTransaction);
        aliceCall2.respondToCancel(aliceCall2CancelTransaction, 200, "OK-2-Cancel-Alice2", 3600);

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, georgeCall.getLastReceivedResponse().getStatusCode());

        georgeCall.sendInviteOkAck();
        assertTrue(!(georgeCall.getLastReceivedResponse().getStatusCode() >= 400));

        Thread.sleep(3000);

        // hangup.
        aliceCall.listenForDisconnect();

        georgeCall.disconnect();

        assertTrue(aliceCall.waitForDisconnect(30 * 1000));
        assertTrue(aliceCall.respondToDisconnect());

        Thread.sleep(500);
    }

    @Test
    public synchronized void testDialForkClientWebRTCBob_And_AliceWithMultipleRegistrationsBusyServiceUnavailable() throws InterruptedException, ParseException {
        stubFor(get(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(dialWebRTCClientForkRcml)));

        // Phone2 register as alice
        SipURI uri = aliceSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(alicePhone.register(uri, "alice", "1234", aliceContact, 3600, 3600));
        assertTrue(alicePhone2.register(uri, "alice", "1234", aliceContact2, 3600, 3600));

        // Prepare second phone to receive call
        SipCall aliceCall = alicePhone.createSipCall();
        aliceCall.listenForIncomingCall();

        SipCall aliceCall2 = alicePhone2.createSipCall();
        aliceCall2.listenForIncomingCall();

        // Create outgoing call with first phone
        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.initiateOutgoingCall(georgeContact, "sip:1111@" + restcommContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(georgeCall);
        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        final int response = georgeCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, georgeCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(aliceCall.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice", 3600));


        assertTrue(aliceCall2.waitForIncomingCall(30 * 1000));
        assertTrue(aliceCall2.sendIncomingCallResponse(Response.RINGING, "Ringing-Alice2", 3600));
        assertTrue(aliceCall2.sendIncomingCallResponse(Response.BUSY_HERE, "Busy-Alice2", 3600));
        
        Thread.sleep(200);

        assertTrue(aliceCall.sendIncomingCallResponse(Response.SERVICE_UNAVAILABLE, "ServiceUnavailable-Alice", 3600));

        assertTrue(georgeCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.SERVICE_UNAVAILABLE, georgeCall.getLastReceivedResponse().getStatusCode());

        Thread.sleep(500);
    }
    
    private String clientWithAppHostedAppRcml = "<Response><Dial timeLimit=\"10\" timeout=\"10\"><Number>+151261006100</Number></Dial></Response>";
    @Test
    public synchronized void testClientWithHostedApplication() throws InterruptedException, ParseException {
        stubFor(post(urlPathEqualTo("/1111"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(clientWithAppHostedAppRcml)));

        assertNotNull(clientWithAppClientSid);

        SipURI uri = clientWithAppSipStack.getAddressFactory().createSipURI(null, restcommContact);
        assertTrue(clientWithAppPhone.register(uri, "clientWithApp", clientPassword, clientWithAppContact, 3600, 3600));
        Credential c = new Credential("127.0.0.1", "clientWithApp", clientPassword);
        clientWithAppPhone.addUpdateCredential(c);

        final SipCall georgeCall = georgePhone.createSipCall();
        georgeCall.listenForIncomingCall();

        SipCall clientWithAppCall = clientWithAppPhone.createSipCall();
        clientWithAppCall.initiateOutgoingCall(clientWithAppContact, "sip:3090909090@" + restcommContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(clientWithAppCall);
        assertTrue(clientWithAppCall.waitForAuthorisation(5000));
        assertTrue(clientWithAppCall.waitOutgoingCallResponse(5000));
        final int response = clientWithAppCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.TRYING || response == Response.RINGING);

        if (response == Response.TRYING) {
            assertTrue(clientWithAppCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, clientWithAppCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(georgeCall.waitForIncomingCall(30 * 1000));
        assertTrue(georgeCall.sendIncomingCallResponse(Response.RINGING, "Ringing-George", 3600));
        String receivedBody = new String(georgeCall.getLastReceivedRequest().getRawContent());
        assertTrue(georgeCall.sendIncomingCallResponse(Response.OK, "OK-George", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(georgeCall.waitForAck(50 * 1000));

        assertTrue(clientWithAppCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, clientWithAppCall.getLastReceivedResponse().getStatusCode());

        clientWithAppCall.sendInviteOkAck();
        assertTrue(!(clientWithAppCall.getLastReceivedResponse().getStatusCode() >= 400));

        Thread.sleep(3000);

        // hangup.
        clientWithAppCall.disconnect();

        georgeCall.listenForDisconnect();
        assertTrue(georgeCall.waitForDisconnect(30 * 1000));
        assertTrue(georgeCall.respondToDisconnect());
    }

    @Deployment(name = "ClientsDialAnswerDelayTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {

        Map<String,String> replacements = new HashMap();
        replacements.put("8080", String.valueOf(restcommHTTPPort));
        replacements.put("8090", String.valueOf(mockPort));
        replacements.put("5080", String.valueOf(restcommPort));
        replacements.put("5070", String.valueOf(georgePort));        
        replacements.put("5090", String.valueOf(bobPort));
        replacements.put("5091", String.valueOf(alicePort));
        replacements.put("5092", String.valueOf(mariaPort));
        replacements.put("5093", String.valueOf(dimitriPort));
        replacements.put("5094", String.valueOf(alicePort2));
        replacements.put("5095", String.valueOf(clientWithAppPort));
        replacements.put("5096", String.valueOf(fotiniPort));
        replacements.put("5097", String.valueOf(bobPort));
        replacements.put("5098", String.valueOf(leftyPort));
        
        List<String> resources = new ArrayList(
                Arrays.asList(
                "dial-conference-entry.xml",
                "dial-fork-entry.xml",
                "dial-uri-entry.xml",
                "dial-client-entry.xml",
                "dial-number-entry.xml"));
        return WebArchiveUtil.createWebArchiveNoGw(
                "restcomm-delay.xml", 
                "restcomm.script_dialTest",
                resources,
                replacements);
    }    

}
