package org.datadog.jenkins.plugins.datadog;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.pipelinegraphanalysis.StatusAndTiming;
import org.junit.*;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;

import java.util.Arrays;
import java.util.logging.Level;

public class DatadogBuildGraphWalkerTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule public LoggerRule l = new LoggerRule();


    WorkflowRun SIMPLE_JOB;

    @Before
    public void setup() throws Exception {
        jenkinsRule.jenkins.getInjector().injectMembers(this);
        DatadogBuildListener listener = new DatadogBuildListener();


        WorkflowJob job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "Test Job");


        String script = "pipeline {\n" +
                "   agent any\n" +
                "\n" +
                "   stages {\n" +
                "      stage('HelloStage') {\n" +
                "         steps {\n" +
                "            echo 'Hello World'\n" +
                "            sleep 2\n" +
                "         }\n" +
                "      }\n" +
                "   }\n" +
                "}";
//        String script = "node {" +
//                "   stage ('Build') { " +
//                "       stage ('BuildA') {" +
//                "         echo ('Building A'); " +
//                "       } \n" +
//                "       stage ('BuildB') {" +
//                "         echo ('Building B'); " +
//                "       } \n" +
//                "   } \n" +
//                "   stage ('Test') { " +
//                "     echo ('Testing'); " +
//                "   } \n" +
//                "   stage ('Deploy') { " +
//                "     writeFile file: 'file.txt', text:'content'; " +
//                "     archive(includes: 'file.txt'); " +
//                "     echo ('Deploying'); " +
//                "   } \n" +
//                "}";

        // System.out.println(script);
        job.setDefinition(new CpsFlowDefinition(script, true));

        WorkflowRun a = jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        this.SIMPLE_JOB = a;
    }

    @Test
    public void testGraphWalker() throws Exception {
        //capture up to 100 lines from the DataDogBuildGraphWalker
        l.capture(100).record("org.datadog.jenkins.plugins.datadog.DatadogBuildGraphWalker", Level.ALL);
        DatadogBuildGraphWalker a=new DatadogBuildGraphWalker(this.SIMPLE_JOB.getExecution());
        //StatusAndTiming.printNodes(this.SIMPLE_JOB, true, true);
        Assert.assertTrue(a.getFlowDuration() > 0);
        //print all log entries for debugging for now
        System.out.println(Arrays.toString(l.getRecords().toArray()));
    }
}
