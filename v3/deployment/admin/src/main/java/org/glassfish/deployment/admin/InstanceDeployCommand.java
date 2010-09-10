/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.deployment.admin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.SnifferManager;
import org.glassfish.deployment.common.DeploymentUtils;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.Transaction;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Application;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;


/**
 * Trivial example of deployment-for-instance command
 * @author hzhang_jn
 * @author tjquinn
 */
@Service(name="_deploy")
@Scoped(PerLookup.class)
@ExecuteOn(value={RuntimeType.INSTANCE})
public class InstanceDeployCommand extends InstanceDeployCommandParameters implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(InstanceDeployCommand.class);
    private final static String LS = System.getProperty("line.separator");

    @Inject
    Deployment deployment;

    @Inject
    SnifferManager snifferManager;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject(name= ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Applications applications;

    @Inject
    ServerEnvironment env;

    @Inject(name= ServerEnvironment.DEFAULT_INSTANCE_NAME)
    protected Server server;

    @Override
    public void execute(AdminCommandContext ctxt) {

        long operationStartTime = Calendar.getInstance().getTimeInMillis();
        final ActionReport report = ctxt.getActionReport();
        final Logger logger = ctxt.getLogger();
        ReadableArchive archive = null;

        this.origin = Origin.deploy_instance;

        this.previousContextRoot = preservedcontextroot;

        if (previousVirtualServers != null) {
            String vs = previousVirtualServers.getProperty(target);
            if (vs != null) {
                this.virtualservers = vs;  
            }
        }

        if (previousEnabledAttributes != null) {
            String enabledAttr = previousEnabledAttributes.getProperty(target);
            if (enabledAttr != null) {
                String enabledAttrForApp = previousEnabledAttributes.getProperty(DeploymentUtils.DOMAIN_TARGET_NAME);
                this.enabled = Boolean.valueOf(enabledAttr) && Boolean.valueOf(enabledAttrForApp);
            }
        }

        try {
            if (!path.exists()) {
                report.setMessage(localStrings.getLocalString("fnf","File not found", path.getAbsolutePath()));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            if (snifferManager.hasNoSniffers()) {
                String msg = localStrings.getLocalString("nocontainer", "No container services registered, done...");
                report.failure(logger,msg);
                return;
            }

            archive = archiveFactory.openArchive(path, this);
            ArchiveHandler archiveHandler = deployment.getArchiveHandler(archive);
            if (archiveHandler==null) {
                report.failure(logger,localStrings.getLocalString("deploy.unknownarchivetype","Archive type of {0} was not recognized",path.getName()));
                return;
            }

            // clean up any left over repository files
            if ( ! keepreposdir.booleanValue()) {
                FileUtils.whack(new File(env.getApplicationRepositoryPath(), name));
            }

            ExtendedDeploymentContext deploymentContext = deployment.getBuilder(logger, this, report).source(archive).build();

            // clean up any remaining generated files
            deploymentContext.clean();

            deploymentContext.getAppProps().putAll(appprops);

            processGeneratedContent(generatedcontent, deploymentContext, logger);
             
            Transaction t = null;
            Application application = applications.getApplication(name);
            if (application != null) {
                // application element already been synchronized over
                t = new Transaction();
            } else {
                t = deployment.prepareAppConfigChanges(deploymentContext);
            }

            ApplicationInfo appInfo;
            if (type==null) {
                appInfo = deployment.deploy(deploymentContext);
            } else {
                appInfo = deployment.deploy(deployment.prepareSniffersForOSGiDeployment(type, deploymentContext), deploymentContext);
            }

            if (report.getActionExitCode()==ActionReport.ExitCode.SUCCESS) {
                try {
                    // register application information in domain.xml
                    if (application != null)  {
                        // application element already synchronized over
                        // just write application-ref
                        deployment.registerAppInDomainXML(appInfo, deploymentContext, t, true);
                    } else {
                        // write both application and application-ref
                        deployment.registerAppInDomainXML(appInfo, deploymentContext, t);
                    }
                } catch (Exception e) {
                    // roll back the deployment and re-throw the exception
                    deployment.undeploy(name, deploymentContext);
                    deploymentContext.clean();
                    throw e;
                }
            } 

            if (report.getActionExitCode()==ActionReport.ExitCode.FAILURE) {
                String msg = localStrings.getLocalString("failToLoadOnInstance",  "Failed to load the application on instance {0} : {1}", server.getName(), report.getMessage());
                report.setMessage(msg); 
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            report.failure(logger,localStrings.getLocalString(
                    "failToLoadOnInstance",
                    "Failed to load the application on instance {0} : {1}", server.getName(), e.getMessage()),null);
        } finally {
            try {
                if (archive != null)  {
                    archive.close();
                }
            } catch(IOException e) {
                logger.log(Level.INFO, localStrings.getLocalString(
                        "errClosingArtifact",
                        "Error while closing deployable artifact : ",
                        path.getAbsolutePath()), e);
            }
            logger.info(localStrings.getLocalString(
                        "deploy.done",
                        "Deployment of {0} done is {1} ms",
                        name,
                        (Calendar.getInstance().getTimeInMillis() - operationStartTime)));
        }

    }

    private void processGeneratedContent(
            final File generatedContentParam,
            final ExtendedDeploymentContext deploymentContext,
            final Logger logger) throws IOException {
        if (generatedContentParam == null) {
            return;
        }

        final File baseDir = deploymentContext.getScratchDir("xml").getParentFile().getParentFile();
        baseDir.mkdirs();

        final URI baseURI = baseDir.toURI();
        final ZipFile zipFile = new ZipFile(generatedContentParam);
        for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
            final ZipEntry zipEntry = entries.nextElement();
            final URI outputFileURI = baseURI.resolve(zipEntry.getName());
            final File outputFile = new File(outputFileURI);
            if (zipEntry.isDirectory()) {
                outputFile.mkdirs();
            } else {
                final FileOutputStream os = new FileOutputStream(outputFile);
                FileUtils.copy(zipFile.getInputStream(zipEntry), os, zipEntry.getSize());
            }
        }
    }
}
