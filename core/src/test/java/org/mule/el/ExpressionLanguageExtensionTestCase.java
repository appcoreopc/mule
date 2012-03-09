/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.el.context.AbstractELTestCase;
import org.mule.el.context.AppContext;
import org.mule.el.mvel.MVELExpressionLanguage;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class ExpressionLanguageExtensionTestCase extends AbstractELTestCase
{

    public ExpressionLanguageExtensionTestCase(Variant variant)
    {
        super(variant);
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SimpleConfigurationBuilder(Collections.singletonMap("key1", new TestExtension()));
    }

    @Override
    protected ExpressionLanguage getExpressionLanguage() throws RegistrationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        return mvel;
    }

    @Test
    public void importClass() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals(Calendar.class, expressionLanguage.evaluate("Calendar"));
    }

    @Test
    public void importClassWithName() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals(Calendar.class, expressionLanguage.evaluate("CAL"));
    }

    @Test
    public void importStaticMethod() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals(DateFormat.getInstance(), expressionLanguage.evaluate("dateFormat()"));
    }

    @Test
    public void variable() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals("hi", expressionLanguage.evaluate("a"));
    }

    @Test
    public void assignValueToVariable() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals("1", expressionLanguage.evaluate("a=1"));
    }

    @Test
    public void finalVariable() throws RegistrationException, InitialisationException
    {
        Assert.assertEquals("hi", expressionLanguage.evaluate("b"));
    }

    @Test
    public void assignValueToFinalVariable() throws RegistrationException, InitialisationException
    {
        assertImmutableVariable("b=1");
    }

    @Test
    public void testShortcutVariable() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();

        Assert.assertEquals(muleContext.getConfiguration().getId(), mvel.evaluate("appShortcut.name"));
    }

    @Test
    public void testVariableAlias() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();

        MuleMessage message = new DefaultMuleMessage("foo", muleContext);

        Assert.assertEquals("foo", mvel.evaluate("p", message));
    }

    @Test
    public void testAssignValueToVariableAlias() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();

        MuleMessage message = new DefaultMuleMessage("foo", muleContext);

        Assert.assertEquals("bar", mvel.evaluate("p='bar'", message));
    }

    @Test
    public void testFunction() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();

        Assert.assertEquals("called param[0]=one,param[1]=two,app.name="
                            + muleContext.getConfiguration().getId(), mvel.evaluate("f('one','two')"));
    }

    @Test(expected = ExpressionRuntimeException.class)
    public void testFunctionInvalidParams() throws RegistrationException, InitialisationException
    {
        MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();
        mvel.evaluate("f('one')");
    }

    class TestExtension implements ExpressionLanguageExtension
    {

        @Override
        public void configureContext(ExpressionLanguageContext context)
        {
            context.importClass(Calendar.class);
            context.importClass("CAL", Calendar.class);
            try
            {
                context.importStaticMethod("dateFormat",
                    DateFormat.class.getMethod("getInstance", new Class[]{}));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            context.addVariable("a", "hi");
            context.addFinalVariable("b", "hi");
            context.addVariable("appShortcut", context.getVariable("app"));
            context.addAlias("p", "message.payload");
            context.declareFunction("f", new ExpressionLanguageFunction()
            {

                @Override
                public void validateParams(Object[] params)
                {
                    if (params.length != 2)
                    {
                        throw new RuntimeException();
                    }
                }

                @Override
                public Object call(Object[] params, ExpressionLanguageContext context)
                {
                    return "called param[0]=" + params[0] + ",param[1]=" + params[1] + ",app.name="
                           + ((AppContext) context.getVariable("app")).getName();
                }
            });
        }
    }
}
