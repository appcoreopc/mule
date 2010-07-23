/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ServiceNotificationListener;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.MessageReceiver;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.spring.SpringRegistry;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServiceNotification;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.model.seda.SedaService;
import org.mule.object.SingletonObjectFactory;
import org.mule.transport.AbstractConnector;
import org.mule.transport.cxf.transport.MuleUniversalTransport;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.springframework.context.ApplicationContext;

/**
 * Connects Mule to a CXF bus instance.
 */
public class CxfConnector extends AbstractConnector implements ServiceNotificationListener<ServiceNotification>
{

    public static final String CXF = "cxf";
    public static final String CXF_SERVICE_COMPONENT_NAME = "_cxfServiceComponent";
    public static final String CONFIGURATION_LOCATION = "configurationLocation";
    public static final String DEFAULT_MULE_NAMESPACE_URI = "http://www.muleumo.org";
    public static final String BUS_PROPERTY = CXF;

    // The CXF Bus object
    private Bus bus;
    private String configurationLocation;
    private String defaultFrontend = CxfConstants.JAX_WS_FRONTEND;
    private Map<String, Set<Service>> serviceToProtocolServices = Collections.synchronizedMap(new HashMap<String, Set<Service>>());
    private Map<String, Server> uriToServer = new HashMap<String, Server>();
    private boolean initializeStaticBusInstance = true;

    public CxfConnector(MuleContext context)
    {
        super(context);
        registerProtocols();
    }

    protected void registerProtocols()
    {
        registerSupportedProtocol("http");
        registerSupportedProtocol("https");
        registerSupportedProtocol("jms");
        registerSupportedProtocol("vm");
        registerSupportedProtocol("jetty");
    }

    @Override
    public boolean supportsProtocol(String protocol)
    {
        // we can listen on any protocol
        return protocol.startsWith("cxf:") || super.supportsProtocol(protocol);
    }

    public String getProtocol()
    {
        return CXF;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        ApplicationContext context = (ApplicationContext) muleContext.getRegistry().lookupObject(SpringRegistry.SPRING_APPLICATION_CONTEXT);

        if (configurationLocation != null)
        {
            bus = new SpringBusFactory(context).createBus(configurationLocation, true);
        }
        else
        {
            bus = new SpringBusFactory(context).createBus((String) null, true);
        }

        if (!initializeStaticBusInstance)
        {
            BusFactory.setDefaultBus(null);
        }

        try
        {
            muleContext.registerListener(this);
        }
        catch (NotificationException e)
        {
            throw new InitialisationException(e, this);
        }

        MuleUniversalTransport transport = new MuleUniversalTransport(this);
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/http", transport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http", transport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http/configuration", transport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/http/", transport);
        dfm.registerDestinationFactory("http://www.w3.org/2003/05/soap/bindings/HTTP/", transport);
        dfm.registerDestinationFactory(MuleUniversalTransport.TRANSPORT_ID, transport);

        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        try
        {
            // Force the HTTP transport to load if available, otherwise it could
            // overwrite our namespaces later
            extension.getConduitInitiator("http://schemas.xmlsoap.org/soap/http");
        }
        catch (BusException e1)
        {
            // If unavailable eat exception and continue
        }
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http/", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/http", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/http/", transport);
        extension.registerConduitInitiator("http://www.w3.org/2003/05/soap/bindings/HTTP/", transport);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/http/configuration", transport);
        extension.registerConduitInitiator("http://cxf.apache.org/bindings/xformat", transport);
        extension.registerConduitInitiator(MuleUniversalTransport.TRANSPORT_ID, transport);
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        bus.shutdown(true);
    }

    public Bus getCxfBus()
    {
        return bus;
    }

    public void setCxfBus(Bus bus)
    {
        this.bus = bus;
    }

    public String getConfigurationLocation()
    {
        return configurationLocation;
    }

    public void setConfigurationLocation(String configurationLocation)
    {
        this.configurationLocation = configurationLocation;
    }

    public String getDefaultFrontend()
    {
        return defaultFrontend;
    }

    public void setDefaultFrontend(String defaultFrontend)
    {
        this.defaultFrontend = defaultFrontend;
    }

    @SuppressWarnings("unchecked")
    protected void registerReceiverWithMuleService(MessageReceiver receiver, EndpointURI ep) throws MuleException
    {
        CxfMessageReceiver cxfReceiver = (CxfMessageReceiver) receiver;
        Server server = cxfReceiver.getServer();
        String flowConstruct = cxfReceiver.getFlowConstruct().getName();
        
        uriToServer.put(server.getEndpoint().getEndpointInfo().getAddress(), server);

        // TODO MULE-2228 Simplify this API
        SedaService outerProtocolService = new SedaService(muleContext);
        outerProtocolService.setName(flowConstruct + "_cxfComponent");
        outerProtocolService.setModel(muleContext.getRegistry().lookupSystemModel());

        CxfServiceComponent svcComponent = new CxfServiceComponent(this, (CxfMessageReceiver) receiver);
        svcComponent.setBus(bus);

        final DefaultJavaComponent component = new DefaultJavaComponent(new SingletonObjectFactory(svcComponent));
        component.setMuleContext(muleContext);
        outerProtocolService.setComponent(component);

        // No determine if the endpointUri requires a new connector to be
        // registed in the case of http we only need to register the new
        // endpointUri if the port is different
        String endpoint = receiver.getEndpointURI().getAddress();
        String scheme = ep.getScheme().toLowerCase();

        InboundEndpoint originalEndpoint = receiver.getEndpoint();

        // If we are using sockets then we need to set the endpoint name appropiately
        // and if using http/https
        // we need to default to POST and set the Content-Type
        if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ssl") || scheme.equals("tcp")
                || scheme.equals("servlet"))
        {
            originalEndpoint.getProperties().put(HttpConnector.HTTP_METHOD_PROPERTY, "POST");
            originalEndpoint.getProperties().put(HttpConstants.HEADER_CONTENT_TYPE, "text/xml");
        }

        QName serviceName = server.getEndpoint().getEndpointInfo().getName();

        EndpointBuilder protocolEndpointBuilder = new EndpointURIEndpointBuilder(endpoint, muleContext);
        protocolEndpointBuilder.setExchangePattern(originalEndpoint.getExchangePattern());
        protocolEndpointBuilder.setName(ep.getScheme() + ":" + serviceName.getLocalPart());
        protocolEndpointBuilder.setTransactionConfig(originalEndpoint.getTransactionConfig());

        EndpointBuilder receiverEndpointBuilder = new EndpointURIEndpointBuilder(originalEndpoint);

        // Apply the transformers to the correct endpoint
        EndpointBuilder transformerEndpoint;
        if (cxfReceiver.isApplyTransformersToProtocol())
        {
            transformerEndpoint = protocolEndpointBuilder;
            receiverEndpointBuilder.setTransformers(Collections.<Transformer>emptyList());
            receiverEndpointBuilder.setResponseTransformers(Collections.<Transformer>emptyList());
        }
        else
        {
            transformerEndpoint = receiverEndpointBuilder;
        }

        // apply the filters to the correct endpoint
        EndpointBuilder filterEndpoint;
        if (cxfReceiver.isApplyFiltersToProtocol())
        {
            filterEndpoint = protocolEndpointBuilder;
            receiverEndpointBuilder.setFilter(null);
        }
        else
        {
            filterEndpoint = receiverEndpointBuilder;  
        }
        filterEndpoint.setFilter(originalEndpoint.getFilter());

        // apply the security filter to the correct endpoint
        EndpointBuilder secFilterEndpoint;
        if (cxfReceiver.isApplySecurityToProtocol())
        {
            secFilterEndpoint = protocolEndpointBuilder;
            receiverEndpointBuilder.setSecurityFilter(null);
        }
        else
        {
            secFilterEndpoint = receiverEndpointBuilder;
        }
        secFilterEndpoint.setSecurityFilter(originalEndpoint.getSecurityFilter());

        String connectorName = (String) originalEndpoint.getProperty(CxfConstants.PROTOCOL_CONNECTOR);
        if (connectorName != null)
        {
            protocolEndpointBuilder.setConnector(muleContext.getRegistry().lookupConnector(connectorName));
        }

        InboundEndpoint protocolEndpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint(protocolEndpointBuilder);

        InboundEndpoint receiverEndpoint = muleContext.getRegistry()
                .lookupEndpointFactory()
                .getInboundEndpoint(receiverEndpointBuilder);

        receiver.setEndpoint(receiverEndpoint);

        outerProtocolService.getMessageSource().setEndpoints(CollectionUtils.singletonList(protocolEndpoint));

        // Add outer services to map so that we can easily look them on on user
        // service lifecycle notifications
        if (!serviceToProtocolServices.containsKey(flowConstruct))
        {
            serviceToProtocolServices.put(flowConstruct, new HashSet());
        }
        serviceToProtocolServices.get(flowConstruct).add(outerProtocolService);

    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param service  the service for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the service
     * @return the key to store the newly created receiver against. In this case it
     *         is the service name, which is equivilent to the Axis service name.
     */
    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        if (flowConstruct.getName().startsWith(CXF_SERVICE_COMPONENT_NAME))
        {
            return flowConstruct.getName();
        }
        else
        {
            return endpoint.getEndpointURI().getAddress();
        }
    }

    public void onNotification(ServiceNotification event)
    {
        // Only register/start the outer (CxfServiceComponent/protocol) services once
        // the inner (user) service is started
        if (event.getAction() == ServiceNotification.SERVICE_STARTED
                && serviceToProtocolServices.get(event.getSource()) != null)
        {
            try
            {
                for (Service outerService : serviceToProtocolServices.get(event.getSource()))
                {
                    muleContext.getRegistry().registerService(outerService);
                }
            }
            catch (MuleException e)
            {
                handleException(e);
            }
        }
        // We need to stop the outer services first if they are not already stopped
        // to avoid request failures.
         else if (event.getAction() == ServiceNotification.SERVICE_STOPPED
                && serviceToProtocolServices.get(event.getSource()) != null)
        {
            try
            {
                for (Service outerService : serviceToProtocolServices.get(event.getSource()))
                {
                    muleContext.getRegistry().unregisterService(outerService.getName());
                    serviceToProtocolServices.remove(event.getSource());
                }
            }
            catch (MuleException e)
            {
                handleException(e);
            }
        }
    }

    public Server getServer(String uri)
    {
        return uriToServer.get(uri);
    }

    public boolean isInitializeStaticBusInstance()
    {
        return initializeStaticBusInstance;
    }

    public void setInitializeStaticBusInstance(boolean initializeStaticBusInstance)
    {
        this.initializeStaticBusInstance = initializeStaticBusInstance;
    }

    @Override
    protected void doUnregisterListener(FlowConstruct flowConstruct, InboundEndpoint endpoint, MessageReceiver receiver)
    {
        uriToServer.remove(((CxfMessageReceiver) receiver).getServer().getEndpoint().getEndpointInfo().getAddress());
    }

}
