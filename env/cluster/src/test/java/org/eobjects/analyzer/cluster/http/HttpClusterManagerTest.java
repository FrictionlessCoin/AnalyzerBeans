/**
 * eobjects.org AnalyzerBeans
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.analyzer.cluster.http;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eobjects.analyzer.cluster.ClusterTestHelper;
import org.eobjects.analyzer.configuration.AnalyzerBeansConfiguration;

public class HttpClusterManagerTest extends TestCase {

    private Server server1;
    private Server server2;
    private Server server3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        server1 = createServer(8882, false);
        server2 = createServer(8883, false);
        server3 = createServer(8884, true);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        server1.stop();
        server2.stop();
        server3.stop();
    }

    public void testVanilla() throws Throwable {
        final List<String> slaveEndpoints = new ArrayList<String>();
        slaveEndpoints.add("http://localhost:8882/slave_endpoint");
        slaveEndpoints.add("http://localhost:8883/slave_endpoint");
        slaveEndpoints.add("http://localhost:8884/slave_endpoint");

        final HttpClusterManager clusterManager = new HttpClusterManager(slaveEndpoints);

        final AnalyzerBeansConfiguration configuration = ClusterTestHelper.createConfiguration(getClass().getSimpleName() + "_" + getName(), false);
        ClusterTestHelper.runConcatAndInsertJob(configuration, clusterManager);
    }

    private Server createServer(int port, boolean multiThreaded) throws Exception {
        final String testName = getClass().getSimpleName() + "_" + getName();
        final AnalyzerBeansConfiguration configuration = ClusterTestHelper.createConfiguration(testName, multiThreaded);

        final SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);

        final WebAppContext webApp = new WebAppContext();
        webApp.setAttribute(SlaveServlet.SERVLET_CONTEXT_ATTRIBUTE_CONFIGURATION, configuration);
        webApp.setContextPath("/");
        webApp.setWar("src/test/resources/jetty_webapp_folder");

        final Server server = new Server();
        server.addConnector(connector);
        server.setHandler(webApp);
        server.start();

        System.out.println("Jetty server started on port " + port);

        return server;
    }
}
