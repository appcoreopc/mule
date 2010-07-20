/*
 * $Id:XmlMuleMessageTransformersTestCase.java 5937 2007-04-09 22:35:04Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Set;

public class XmlMuleMessageTransformersTestCase extends AbstractMuleTestCase
{
    public void testMessageSerialization() throws Exception
    {
        ObjectToXml t1 = createObject(ObjectToXml.class);
        t1.setAcceptMuleMessage(true);

        MuleMessage msg = new DefaultMuleMessage("test", muleContext);
        msg.setEncoding("UTF-8");
        msg.setCorrelationId("1234");
        msg.setInvocationProperty("number", 1);
        msg.setOutboundProperty("object", new Apple());
        msg.setOutboundProperty("string", "hello");

        String xml = (String) t1.transform(msg);
        assertNotNull(xml);

        XmlToObject t2 = createObject(XmlToObject.class);

        Object result = t2.transform(xml);
        assertNotNull(result);
        assertTrue(result instanceof MuleMessage);

        msg = (MuleMessage) result;

        assertEquals("test", msg.getPayloadAsString());
        assertEquals(new Apple(), msg.getOutboundProperty("object"));
        //with different case
        assertEquals(new Apple(), msg.getOutboundProperty("oBjeCt"));
        //Make sure we don't have the property in a different scope
        assertNull(msg.getInboundProperty("oBjeCt"));
        assertNull(msg.getInvocationProperty("oBjeCt"));
        assertNull(msg.getProperty("oBjeCt", PropertyScope.SESSION));

        assertEquals("hello", msg.getOutboundProperty("string"));
        //with different case
        assertEquals("hello", msg.getOutboundProperty("String"));
        //Make sure we don't have the property in a different scope
        assertNull(msg.getInboundProperty("string"));
        assertNull(msg.getInvocationProperty("string"));
        assertNull(msg.getProperty("string", PropertyScope.SESSION));

        assertEquals(1, msg.getInvocationProperty("number"));
        //with different case
        assertEquals(1, msg.getInvocationProperty("NUMBER"));
        //Make sure we don't have the property in a different scope
        assertNull(msg.getInboundProperty("number"));
        assertNull(msg.getOutboundProperty("number"));
        assertNull(msg.getProperty("number", PropertyScope.SESSION));

        assertEquals("1234", msg.getCorrelationId());
        assertEquals("UTF-8", msg.getEncoding());

        Set<String> keys = msg.getPropertyNames();
        assertEquals(5, keys.size());

        //Remove Mule properties
        keys.remove(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        keys.remove(MuleProperties.MULE_ENCODING_PROPERTY);

        for (String key : keys)
        {
            assertTrue(key.equals("number") || key.equals("string") || key.equals("object"));
            assertFalse(key.equals("NUMBER") || key.equals("STRING") || key.equals("OBJECT"));
        }
    }
}
