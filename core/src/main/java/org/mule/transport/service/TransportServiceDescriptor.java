/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.service;

import java.util.List;
import java.util.Properties;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURIBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.MessageRequesterFactory;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.api.transport.SessionHandler;

/**
 * <code>TransportServiceDescriptor</code> describes the necessary information for
 * creating a connector from a service descriptor. A service descriptor should be
 * located at META-INF/services/org/mule/providers/<protocol> where protocol is the
 * protocol of the connector to be created The service descriptor is in the form of
 * string key value pairs and supports a number of properties, descriptions of which
 * can be found here: http://www.muledocs.org/Transport+Service+Descriptors.
 */
public interface TransportServiceDescriptor extends ServiceDescriptor, MuleContextAware
{
    public static final String OSGI_HEADER_TRANSPORT = "Mule-Transport";

    MuleMessageFactory createMuleMessageFactory() throws TransportServiceException;
    
    SessionHandler createSessionHandler() throws TransportServiceException;

    MessageReceiver createMessageReceiver(Connector connector,
                                                 FlowConstruct flowConstruct,
                                                 InboundEndpoint endpoint) throws MuleException;

    MessageReceiver createMessageReceiver(Connector connector,
                                                 FlowConstruct flowConstruct,
                                                 InboundEndpoint endpoint,
                                                 Object[] args) throws MuleException;

    MessageDispatcherFactory createDispatcherFactory() throws TransportServiceException;

    MessageRequesterFactory createRequesterFactory() throws TransportServiceException;

    TransactionFactory createTransactionFactory() throws TransportServiceException;

    Connector createConnector() throws TransportServiceException;

    List<Transformer> createInboundTransformers(InboundEndpoint endpoint) throws TransportFactoryException;

    List<Transformer> createOutboundTransformers(OutboundEndpoint endpoint) throws TransportFactoryException;

    List<Transformer> createResponseTransformers(InboundEndpoint endpoint) throws TransportFactoryException;

    EndpointURIBuilder createEndpointURIBuilder() throws TransportFactoryException;

    EndpointBuilder createEndpointBuilder(String uri) throws TransportFactoryException;

    void setExceptionMappings(Properties props);

    Properties getExceptionMappings();

    List<MessageExchangePattern> getInboundExchangePatterns() throws TransportServiceException;

    List<MessageExchangePattern> getOutboundExchangePatterns() throws TransportServiceException;
    
    MessageExchangePattern getDefaultExchangePattern() throws TransportServiceException;
}
