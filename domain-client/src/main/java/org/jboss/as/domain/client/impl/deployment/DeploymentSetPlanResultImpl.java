/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.client.impl.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jboss.as.domain.client.api.ServerUpdateResult;
import org.jboss.as.domain.client.api.deployment.DeploymentActionResult;
import org.jboss.as.domain.client.api.deployment.DeploymentSetPlan;
import org.jboss.as.domain.client.api.deployment.DeploymentSetPlanResult;
import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentActionResult;
import org.jboss.as.domain.client.api.deployment.ServerGroupDeploymentPlanResult;

/**
 * Default implementation of {@link DeploymentSetPlanResult}.
 *
 * @author Brian Stansberry
 */
public class DeploymentSetPlanResultImpl implements DeploymentSetPlanResult {

    private final DeploymentSetPlan plan;
    private final Map<UUID, DeploymentActionResult> results;
    private Map<String, ServerGroupDeploymentPlanResult> resultsByServerGroup;

    public DeploymentSetPlanResultImpl(final DeploymentSetPlan plan, final Map<UUID, DeploymentActionResult> results) {
        assert plan != null : "plan is null";
        assert results != null : "results is null";
        this.plan = plan;
        this.results = results;
    }

    @Override
    public Map<UUID, DeploymentActionResult> getDeploymentActionResults() {
        return Collections.unmodifiableMap(results);
    }

    @Override
    public UUID getDeploymentSetId() {
        return plan.getId();
    }

    @Override
    public DeploymentSetPlan getDeploymentSetPlan() {
        return plan;
    }

    @Override
    public synchronized Map<String, ServerGroupDeploymentPlanResult> getServerGroupResults() {
        if (resultsByServerGroup == null) {
            this.resultsByServerGroup = buildServerGroupResults(results);
        }
        return Collections.unmodifiableMap(resultsByServerGroup);
    }

    // Builds the data structures that show the effects of the plan by server group
    private static Map<String, ServerGroupDeploymentPlanResult> buildServerGroupResults(Map<UUID, DeploymentActionResult> deploymentActionResults) {
        Map<String, ServerGroupDeploymentPlanResult> serverGroupResults = new HashMap<String, ServerGroupDeploymentPlanResult>();

        for (Map.Entry<UUID, DeploymentActionResult> entry : deploymentActionResults.entrySet()) {

            UUID actionId = entry.getKey();
            DeploymentActionResult actionResult = entry.getValue();

            Map<String, ServerGroupDeploymentActionResult> actionResultsByServerGroup = actionResult.getResultsByServerGroup();
            for (ServerGroupDeploymentActionResult serverGroupActionResult : actionResultsByServerGroup.values()) {
                String serverGroupName = serverGroupActionResult.getServerGroupName();

                ServerGroupDeploymentPlanResultImpl sgdpr = (ServerGroupDeploymentPlanResultImpl) serverGroupResults.get(serverGroupName);
                if (sgdpr == null) {
                    sgdpr = new ServerGroupDeploymentPlanResultImpl(serverGroupName);
                    serverGroupResults.put(serverGroupName, sgdpr);
                }

                for (Map.Entry<String, ServerUpdateResult<Void>> serverEntry : serverGroupActionResult.getResultByServer().entrySet()) {
                    String serverName = serverEntry.getKey();
                    ServerUpdateResult<Void> sud = serverEntry.getValue();
                    ServerDeploymentPlanResultImpl sdpr = (ServerDeploymentPlanResultImpl) sgdpr.getServerResult(serverName);
                    if (sdpr == null) {
                        sdpr = new ServerDeploymentPlanResultImpl(serverName);
                        sgdpr.storeServerResult(serverName, sdpr);
                    }
                    sdpr.storeServerUpdateResult(actionId, sud);
                }
            }
        }
        return serverGroupResults;
    }

}
