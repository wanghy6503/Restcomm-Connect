package org.restcomm.connect.application;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.servlet.sip.SipConnector;
import org.restcomm.connect.application.config.ConfigurationStringLookup;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.loader.ObjectFactory;
import org.restcomm.connect.commons.loader.ObjectInstantiationException;
import org.restcomm.connect.commons.util.DNSUtils;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.InstanceId;
import org.restcomm.connect.dao.entities.Organization;
import org.restcomm.connect.dao.entities.shiro.ShiroResources;
import org.restcomm.connect.extension.controller.ExtensionBootstrapper;
import org.restcomm.connect.identity.IdentityContext;
import org.restcomm.connect.monitoringservice.MonitoringService;
import org.restcomm.connect.mrb.api.StartMediaResourceBroker;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;
import org.restcomm.connect.mscontrol.api.MediaServerInfo;
import org.restcomm.connect.mscontrol.jsr309.Jsr309ControllerFactory;
import org.restcomm.connect.mscontrol.mms.MmsControllerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import scala.concurrent.ExecutionContext;

/**
 *
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author maria-farooq@live.com (Maria Farooq)
 *
 */

public final class Bootstrapper extends SipServlet implements SipServletListener {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Bootstrapper.class);

    private ActorSystem system;
    private ExecutionContext ec;

    public Bootstrapper() {
        super();
    }

    @Override
    public void destroy() {
        system.shutdown();
        system.awaitTermination();
    }

    private MediaServerControllerFactory mediaServerControllerFactory(final Configuration configuration, ClassLoader loader, DaoManager storage)
            throws ServletException {
        Configuration settings ;
        String compatibility = configuration.subset("mscontrol").getString("compatibility", "rms");

        MediaServerControllerFactory factory;
        switch (compatibility) {
            case "rms":
                try {
                    settings = configuration.subset("media-server-manager");
                    ActorRef mrb = mediaResourceBroker(settings, storage, loader);
                    factory = new MmsControllerFactory(mrb);
                } catch (UnknownHostException e) {
                    throw new ServletException(e);
                }
                break;

            case "xms":
                try {
                    settings = configuration.subset("mscontrol");
                    // Load JSR 309 driver
                    final String driverName = settings.getString("media-server[@class]");
                    Driver driver = DriverManager.getDriver(driverName);
                    DriverManager.registerDriver(driver);

                    // Configure properties
                    Properties properties = getDialogicXmsProperties(settings);

                    // Create JSR 309 factory
                    MsControlFactory msControlFactory = driver.getFactory(properties);
                    MediaServerInfo mediaServerInfo = mediaServerInfo(settings);
                    factory = new Jsr309ControllerFactory(mediaServerInfo, msControlFactory);
                } catch (UnknownHostException | MsControlException e) {
                    throw new ServletException(e);
                }
                break;

            default:
                throw new IllegalArgumentException("MSControl unknown compatibility mode: " + compatibility);
        }
        return factory;
    }

    private MediaServerInfo mediaServerInfo(final Configuration configuration) throws UnknownHostException {
        final String name = configuration.getString("media-server[@name]");
        final String address = configuration.getString("media-server.address");
        final int port = configuration.getInt("media-server.port");
        final int timeout = configuration.getInt("media-server.timeout", 5);
        return new MediaServerInfo(name, DNSUtils.getByName(address), port, timeout);
    }

    private Properties getDialogicXmsProperties(final Configuration configuration) {
        // New set of properties that will be used to configure the connector
        Properties properties = new Properties();

        // Tell the driver we are configuring it programmatically
        // properties.setProperty("connector.dynamic.configuration", "yes");

        // Configure the transport to be used by the connector
        final String mediaTransport = configuration.getString("media-server.transport", "udp");
        if(logger.isInfoEnabled()) {
            logger.info("JSR 309 - media-server.transport: udp");
        }
        properties.setProperty("connector.sip.transport", mediaTransport);

        // Configure SIP connector using RestComm binding address
        SipURI sipURI = outboundInterface(getServletContext(), mediaTransport);
        properties.setProperty("connector.sip.address", sipURI.getHost());
        if(logger.isInfoEnabled()) {
            logger.info("JSR 309 - connector.sip.address: " + sipURI.getHost());
        }
        properties.setProperty("connector.sip.port", String.valueOf(sipURI.getPort()));
        if(logger.isInfoEnabled()) {
            logger.info("JSR 309 - connector.sip.port: " + String.valueOf(sipURI.getPort()));
        }

        // Configure Media Server address based on restcomm configuration file
        final String mediaAddress = configuration.getString("media-server.address", "127.0.0.1");
        properties.setProperty("mediaserver.sip.ipaddress", mediaAddress);
        if(logger.isInfoEnabled()) {
            logger.info("JSR 309 - mediaserver.sip.ipaddress: " + mediaAddress);
        }

        final String mediaPort = configuration.getString("media-server.port", "5060");
        properties.setProperty("mediaserver.sip.port", mediaPort);
        if(logger.isInfoEnabled()) {
            logger.info("JSR 309 - mediaserver.sip.port: " + mediaPort);
        }

        // Let RestComm control call legs
        properties.setProperty("connector.conferenceControlLeg", "no");

        return properties;
    }

    @SuppressWarnings("unchecked")
    private SipURI outboundInterface(ServletContext context, String transport) {
        SipURI result = null;
        final List<SipURI> uris = (List<SipURI>) context.getAttribute(OUTBOUND_INTERFACES);
        if (uris != null && uris.size() > 0) {
            for (final SipURI uri : uris) {
                final String interfaceTransport = uri.getTransportParam();
                if (transport.equalsIgnoreCase(interfaceTransport)) {
                    result = uri;
                }
            }
            if (logger.isInfoEnabled()) {
                if (result == null) {
                    logger.info("Outbound interface is NULL! Looks like there was no "+transport+" in the list of connectors");
                } else {
                    logger.info("Outbound interface found: "+result.toString());
                }
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("ServletContext return null or empty list of connectors");
            }
        }
        return result;
    }

    private ActorRef mediaResourceBroker(final Configuration configuration, final DaoManager storage, final ClassLoader loader) throws UnknownHostException{
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                final String classpath = configuration.getString("mrb[@class]");
                return (UntypedActor) new ObjectFactory(loader).getObjectInstance(classpath);
            }
        });
        ActorRef mrb = system.actorOf(props);
        mrb.tell(new StartMediaResourceBroker(configuration, storage, loader), null);
        return mrb;
    }

    private String home(final ServletContext context) {
        final String path = context.getRealPath("/");
        logger.info("Home path:" + path);
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    private DaoManager storage(final Configuration configuration, Configuration daoManagerConfiguration, final ClassLoader loader, final ExecutionContext ec) throws ObjectInstantiationException {
        final String classpath = daoManagerConfiguration.getString("dao-manager[@class]");
        final DaoManager daoManager = (DaoManager) new ObjectFactory(loader).getObjectInstance(classpath);
        daoManager.configure(configuration, daoManagerConfiguration, ec );
        daoManager.start();
        return daoManager;
    }

    private ActorRef monitoringService(final Configuration configuration, final DaoManager daoManager, final ClassLoader loader) {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new MonitoringService(daoManager);
            }
        });
        return system.actorOf(props);

    }

    private String uri(final ServletContext context) {
        return context.getContextPath();
    }

    /**
     * generateDefaultDomainName based on RC hostname
     * https://github.com/RestComm/Restcomm-Connect/issues/2085
     * @param configuration
     * @param storage
     */
    private boolean generateDefaultDomainName (final Configuration configuration, final DaoManager storage) {
        try{
            final Sid defaultOrganization = new Sid("ORafbe225ad37541eba518a74248f0ac4c");
            final String hostname = configuration.getString("hostname");

            if(hostname != null && !hostname.trim().equals("")){
                if(logger.isInfoEnabled())
                    logger.info("Generate Default Domain Name based on RC hostname: "+hostname);
            }else{
                logger.error("Unable to generateDefaultDomainName hostname property is null in restcomm.xml");
                return false;
            }

            Organization organization = storage.getOrganizationsDao().getOrganization(defaultOrganization);
            if(organization == null){
                storage.getOrganizationsDao().addOrganization(new Organization(defaultOrganization, hostname, DateTime.now(), DateTime.now()));
            }else{
                organization = organization.setDomainName(hostname);
                storage.getOrganizationsDao().updateOrganization(organization);
            }
        }catch(Exception e){
            logger.error("Unable to generateDefaultDomainName {}", e);
            return false;
        }
        return true;
    }
    @Override
    public void servletInitialized(SipServletContextEvent event) {
        if (event.getSipServlet().getClass().equals(Bootstrapper.class)) {
            final ServletContext context = event.getServletContext();
            final String path = context.getRealPath("WEB-INF/conf/restcomm.xml");
            final String extensionConfigurationPath = context.getRealPath("WEB-INF/conf/extensions.xml");
            final String daoManagerConfigurationPath = context.getRealPath("WEB-INF/conf/dao-manager.xml");
            // Initialize the configuration interpolator.
            final ConfigurationStringLookup strings = new ConfigurationStringLookup();
            strings.addProperty("home", home(context));
            strings.addProperty("uri", uri(context));
            ConfigurationInterpolator.registerGlobalLookup("restcomm", strings);
            // Load the RestComm configuration file.
            Configuration xml = null;
            XMLConfiguration extensionConf = null;
            XMLConfiguration daoManagerConf = null;
            try {
                XMLConfiguration xmlConfiguration = new XMLConfiguration();
                xmlConfiguration.setDelimiterParsingDisabled(true);
                xmlConfiguration.setAttributeSplittingDisabled(true);
                xmlConfiguration.load(path);
                xml = xmlConfiguration;

                extensionConf = new XMLConfiguration();
                extensionConf.setDelimiterParsingDisabled(true);
                extensionConf.setAttributeSplittingDisabled(true);
                extensionConf.load(extensionConfigurationPath);

                daoManagerConf = new XMLConfiguration();
                daoManagerConf.setDelimiterParsingDisabled(true);
                daoManagerConf.setAttributeSplittingDisabled(true);
                daoManagerConf.load(daoManagerConfigurationPath);

            } catch (final ConfigurationException exception) {
                logger.error(exception);
            }
            xml.setProperty("runtime-settings.home-directory", home(context));
            xml.setProperty("runtime-settings.root-uri", uri(context));
            // initialize DnsUtilImpl ClassName
            DNSUtils.initializeDnsUtilImplClassName(xml);
            // Create high-level restcomm configuration
            RestcommConfiguration.createOnce(xml);
            context.setAttribute(Configuration.class.getName(), xml);
            context.setAttribute("ExtensionConfiguration", extensionConf);
            // Initialize global dependencies.
            final ClassLoader loader = getClass().getClassLoader();
            // Create the actor system.
            final Config settings = ConfigFactory.load();
            system = ActorSystem.create("RestComm", settings, loader);
            // Share the actor system with other servlets.
            context.setAttribute(ActorSystem.class.getName(), system);
            ec = system.dispatchers().lookup("restcomm-blocking-dispatcher");
            // Create the storage system.
            DaoManager storage = null;
            try {
                storage = storage(xml, daoManagerConf, loader, ec);
            } catch (final ObjectInstantiationException exception) {
                logger.error("ObjectInstantiationException during initialization: ", exception);
            }
            context.setAttribute(DaoManager.class.getName(), storage);
            //ShiroResources.getInstance().set(DaoManager.class, storage);
            ShiroResources.getInstance().set(Configuration.class, xml.subset("runtime-settings"));
            // Initialize identityContext
            IdentityContext identityContext = new IdentityContext(xml);
            context.setAttribute(IdentityContext.class.getName(), identityContext);

            // Create the media gateway.

            //Initialize Monitoring Service
            ActorRef monitoring = monitoringService(xml, storage, loader);
            if (monitoring != null) {
                context.setAttribute(MonitoringService.class.getName(), monitoring);
                if(logger.isInfoEnabled()) {
                    logger.info("Monitoring Service created and stored in the context");
                }
            } else {
                logger.error("Monitoring Service is null");
            }

            //Initialize Extensions
            Configuration extensionConfiguration = null;
            try {
                extensionConfiguration = new XMLConfiguration(extensionConfigurationPath);
            } catch (final ConfigurationException exception) {
//                logger.error(exception);
            }

            ExtensionBootstrapper extensionBootstrapper = new ExtensionBootstrapper(context, extensionConfiguration);
            try {
                extensionBootstrapper.start();
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                logger.error("Exception during extension scanner start: "+e.getStackTrace());
            }

            // Create the media server controller factory
            MediaServerControllerFactory mscontrollerFactory = null;
            try {
                mscontrollerFactory = mediaServerControllerFactory(xml, loader, storage);
            } catch (ServletException exception) {
                logger.error("ServletException during initialization: ", exception);
            }
            context.setAttribute(MediaServerControllerFactory.class.getName(), mscontrollerFactory);

            Boolean rvdMigrationEnabled = new Boolean(xml.subset("runtime-settings").getString("rvd-workspace-migration-enabled", "false"));
            if (rvdMigrationEnabled) {
                //Replicate RVD Projects as database entities
                try {
                    RvdProjectsMigrator rvdProjectMigrator = new RvdProjectsMigrator(context, xml);
                    rvdProjectMigrator.executeMigration();
                } catch (Exception exception) {
                    logger.error("RVD Porjects migration failed during initialization: ", exception);
                }
            }

            //Last, print Version and send PING if needed
            Version.printVersion();
            GenerateInstanceId generateInstanceId = null;
            InstanceId instanceId = null;
            try {
                SipURI sipURI = outboundInterface(context,"udp");
                if (sipURI != null) {
                    generateInstanceId = new GenerateInstanceId(context, sipURI);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("SipURI is NULL!!! Cannot proceed to generate InstanceId");
                    }
                }
                instanceId = generateInstanceId.instanceId();
            } catch (UnknownHostException e) {
                logger.error("UnknownHostException during the generation of InstanceId: "+e);
            }

            context.setAttribute(InstanceId.class.getName(), instanceId);
            monitoring.tell(instanceId, null);
            RestcommConfiguration.getInstance().getMain().setInstanceId(instanceId.getId().toString());

            if(!generateDefaultDomainName(xml.subset("http-client"), storage)){
                logger.error("Unable to generate DefaultDomainName, Restcomm Akka system will exit now...");
                system.shutdown();
                system.awaitTermination();
            }

            // https://github.com/RestComm/Restcomm-Connect/issues/1285 Pass InstanceId to the Load Balancer for LCM stickiness
            SipConnector[] connectors = (SipConnector[]) context.getAttribute("org.mobicents.servlet.sip.SIP_CONNECTORS");
            Properties loadBalancerCustomInfo = new Properties();
            loadBalancerCustomInfo.setProperty("Restcomm-Instance-Id", instanceId.getId().toString());
            for (SipConnector sipConnector : connectors) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Passing InstanceId " + instanceId.getId().toString() + " to connector " + sipConnector);
                }
                sipConnector.setLoadBalancerCustomInformation(loadBalancerCustomInfo);
            }
            //Depreciated
//            Ping ping = new Ping(xml, context);
//            ping.sendPing();
        }
    }
}
