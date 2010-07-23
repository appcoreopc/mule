/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnector;
import org.mule.transport.AbstractMessageReceiver;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.BayeuxService;

/**
 * Registers a receiver service with Bayeux.  
 * The {@link AjaxMessageReceiver.ReceiverService#route(dojox.cometd.Client, Object)}
 * is invoked when a message is received on the subscription channel
 */
public class AjaxMessageReceiver extends AbstractMessageReceiver
{
    private AbstractBayeux bayeux;

    @SuppressWarnings("unused")
    private ReceiverService service;

    public AjaxMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, flowConstruct, endpoint); 
    }

    public class ReceiverService extends BayeuxService
    {
        private final ImmutableEndpoint endpoint;

        public ReceiverService(String channel, Bayeux bayeux, ImmutableEndpoint endpoint)
        {
            super(bayeux, channel /*, connector.getReceiverThreadingProfile().getMaxThreadsActive(), endpoint.isSynchronous()*/);
            this.endpoint = endpoint;
            //this.setSeeOwnPublishes(true);
            subscribe(channel, "route");
        }

        public Object route(Client client, Object data) throws Exception
        {
            AbstractConnector connector = (AbstractConnector) getConnector();
            MuleMessage messageToRoute = createMuleMessage(data, endpoint.getEncoding());

            Object replyTo = messageToRoute.getReplyTo();
            MuleMessage message = AjaxMessageReceiver.this.routeMessage(messageToRoute);
            //If a replyTo channel is set the client is expecting a response.
            //Mule does not invoke the replyTo handler if an error occurs, but in this case we want it to.
            if ((message != null && message.getExceptionPayload() == null) && replyTo != null)
            {
                connector.getReplyToHandler((InboundEndpoint) endpoint).processReplyTo(RequestContext.getEvent(), message, replyTo);
            }
            return null;
        }
    }

    public AbstractBayeux getBayeux()
    {
        return bayeux;
    }

    public void setBayeux(AbstractBayeux bayeux)
    {
        this.bayeux = bayeux;
    }

    @Override
    protected void doStart() throws MuleException
    {
        //Register our listener service with Bayeux
        service = new ReceiverService(endpoint.getEndpointURI().getPath(), getBayeux(), getEndpoint());
    }
}

