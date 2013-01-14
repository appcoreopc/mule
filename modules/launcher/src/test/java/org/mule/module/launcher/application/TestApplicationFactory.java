/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import org.mule.MuleCoreExtension;
import org.mule.module.launcher.DeploymentService;

import java.io.IOException;
import java.util.Map;

/**
 * Creates a {@link ApplicationFactory} that returns {@link TestApplicationWrapper}
 * instances in order to simulate errors on application deployment phases.
 */
public class TestApplicationFactory extends ApplicationFactory
{

    private boolean failOnStopApplication;
    private boolean failOnDisposeApplication;

    public TestApplicationFactory(DeploymentService deploymentService, Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> coreExtensions)
    {
        super(deploymentService, coreExtensions);
    }

    @Override
    public Application createApp(String appName) throws IOException
    {
        Application app = super.createApp(appName);

        TestApplicationWrapper testApplicationWrapper = new TestApplicationWrapper(app);
        testApplicationWrapper.setFailOnDisposeApplication(failOnDisposeApplication);
        testApplicationWrapper.setFailOnStopApplication(failOnStopApplication);

        return testApplicationWrapper;
    }

    public void setFailOnDisposeApplication(boolean failOnDisposeApplication)
    {
        this.failOnDisposeApplication = failOnDisposeApplication;
    }

    public void setFailOnStopApplication(boolean failOnStopApplication)
    {
        this.failOnStopApplication = failOnStopApplication;
    }
}
