package org.datadog.jenkins.plugins.datadog;

import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.*;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;

import java.util.logging.Logger;

//takes a flowexecution and walks the graph, setting durations (e.g. FlowDuration), that can be retrieved with individual getters
public class DatadogBuildGraphWalker {

    private long FlowDuration;
    private FlowExecution fe;
    private static final Logger logger = Logger.getLogger(DatadogBuildGraphWalker.class.getName());

    public DatadogBuildGraphWalker(FlowExecution flow) {
        this.fe = flow;
        walkGraph();
    }

    //retrieve the overall flow duration of the graph
    public long getFlowDuration() {
        return FlowDuration;
    }

    //walks the graph and sets durations.
    //logs individual steps as they are analyed to this class logger (org.datadog.jenkins.plugins.datadog.DatadogBuildGraphWalker)
    private void walkGraph() {
        ForkScanner scanner = new ForkScanner();
        scanner.setup(this.fe.getCurrentHeads());
        long lastNodeStartTime = 0;
        long lastBodyStartTime = 0;
        while(scanner.hasNext()) {
            FlowNode node=scanner.next();
            //FlowStartNode starts an overall pipeline
            if (node instanceof FlowStartNode) {
                long flowStart = node.getAction(TimingAction.class).getStartTime();
                long flowEnd = node.getExecution().getEndNode((BlockStartNode) node).getAction(TimingAction.class).getStartTime();

                this.FlowDuration = flowEnd - flowStart;
                logger.fine("build duration " + this.FlowDuration);
            //StepStartNode starts a block
            } else if (node instanceof StepStartNode) {

                long blockEndTime = node.getExecution().getEndNode((BlockStartNode) node).getAction(TimingAction.class).getStartTime();
                long blockStartTime = node.getAction(TimingAction.class).getStartTime();
                if (((StepStartNode) node).isBody()) {
                    lastBodyStartTime = blockStartTime;
                }

                logger.fine(String.format("%s %s block duration %s\n", node.getDisplayName(), ((StepStartNode)node).getDescriptor(), blockEndTime-blockStartTime));
                if (!((StepStartNode) node).isBody()) {
                    logger.fine(String.format("%s %s block allocation %s\n", node.getDisplayName(), ((StepStartNode) node).getDescriptor(), lastBodyStartTime - blockStartTime));
                }
                logger.fine("---\n");



            //StepAtomNode is a single entity of a step (without a block executing as part of it)
            } else if (node instanceof StepAtomNode) {
                long startTime = node.getAction(TimingAction.class).getStartTime();
                logger.fine(String.format("%s duration: %s\n", ((StepAtomNode) node).getDescriptor(), lastNodeStartTime-startTime));
                logger.fine("---");
                TimingAction t = node.getAction(TimingAction.class);
                if (t != null) {
                    t.getStartTime();
                }
                logger.fine(node.toString());
            } else {
              logger.fine("not handling " + node);
            }
            lastNodeStartTime = node.getAction(TimingAction.class).getStartTime();
        }
    }

}
