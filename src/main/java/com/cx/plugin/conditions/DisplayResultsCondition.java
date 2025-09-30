package com.cx.plugin.conditions;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Galn on 07/06/2017.
 */
import com.atlassian.bamboo.plan.cache.ImmutableBuildable;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.plan.cache.ImmutableJob;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.AbstractResultsSummary;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskIdentifier;
import com.atlassian.plugin.web.Condition;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class DisplayResultsCondition implements Condition {

    private static final Logger log = LoggerFactory.getLogger(DisplayResultsCondition.class);

    public DisplayResultsCondition() {
    }

    @Override
    public void init(final Map<String, String> configParams) {
    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context) {

        log.info("DisplayResultsCondition.shouldDisplay() called");

        try {
            AbstractResultsSummary abstractResultsSummary = (AbstractResultsSummary) context.get("resultSummary");
            if (abstractResultsSummary == null) {
                log.warn("resultSummary is null");
                return false;
            }

            ImmutablePlan plan = (ImmutablePlan) context.get("plan");
            if (plan == null) {
                log.warn("plan is null");
                return false;
            }

            String lifeCycleState = abstractResultsSummary.getLifeCycleState().toString();
            boolean buildFinished = "Finished".equals(lifeCycleState);
            log.info("Build lifecycle state: {}, buildFinished: {}", lifeCycleState, buildFinished);

            boolean hasCxTask = hasCxTask(plan, new IsCxTaskPredicate<TaskDefinition>());
            log.info("Has Checkmarx task: {}", hasCxTask);

            boolean shouldDisplay = buildFinished && hasCxTask;
            log.info("Final shouldDisplay result: {}", shouldDisplay);

            return shouldDisplay;

        } catch (Exception e) {
            log.error("Error in shouldDisplay()", e);
            return false;
        }
    }

    public static boolean hasCxTask(ImmutablePlan plan, Predicate<TaskDefinition> predicate) {
        log.info("Checking for Checkmarx task in plan: {}", plan.getKey());

        if (plan instanceof ImmutableChain) {
            log.info("Plan is ImmutableChain");
            ImmutableJob job = ((ImmutableChain) plan).getAllJobs().get(0);
            log.info("Checking job: {}", job.getKey());

            for (TaskDefinition taskDef : job.getBuildDefinition().getTaskDefinitions()) {
                log.info("Found task: {} with plugin key: {}", taskDef.getPluginKey(), taskDef.getPluginKey());
            }

            if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), predicate)) {
                log.info("Found Checkmarx task in chain");
                return true;
            }
        } else if (plan instanceof ImmutableBuildable) {
            log.info("Plan is ImmutableBuildable");
            ImmutableJob job = (ImmutableJob) plan;
            log.info("Checking job: {}", job.getKey());

            for (TaskDefinition taskDef : job.getBuildDefinition().getTaskDefinitions()) {
                log.info("Found task: {} with plugin key: {}", taskDef.getPluginKey(), taskDef.getPluginKey());
            }

            if (Iterables.any(job.getBuildDefinition().getTaskDefinitions(), predicate)) {
                log.info("Found Checkmarx task in buildable");
                return true;
            }
        }
        log.info("No Checkmarx task found");
        return false;
    }

    private static class IsCxTaskPredicate<TASKDEF extends TaskIdentifier> implements Predicate<TASKDEF> {

        public boolean apply(@javax.annotation.Nullable TASKDEF taskIdentifier) {
            Logger log = LoggerFactory.getLogger(IsCxTaskPredicate.class);
            if (taskIdentifier == null) {
                log.warn("Task identifier is null");
                return false;
            }

            String pluginKey = taskIdentifier.getPluginKey();
            boolean matches = pluginKey.startsWith("com.cx.checkmarx-bamboo-plugin:checkmarx");
            log.info("Task plugin key: {}, matches Checkmarx pattern: {}", pluginKey, matches);

            return matches;
        }

        public boolean test(@javax.annotation.Nullable TASKDEF input) {
            return this.apply(input);
        }

    }
}