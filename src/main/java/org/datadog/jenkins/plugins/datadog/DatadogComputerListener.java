package org.datadog.jenkins.plugins.datadog;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class DatadogComputerListener extends ComputerListener {

    private static final Logger logger = Logger.getLogger(DatadogBuildListener.class.getName());
    DatadogBuildListener.DescriptorImpl descriptor = new DatadogBuildListener.DescriptorImpl();
    private Map<String, Long> computers = new HashMap<String, Long>();
    private StatsDClient statsDClient;

    @Override
    public void preLaunch(Computer c, TaskListener taskListener) throws IOException, InterruptedException {
        logger.fine(String.format("DatadogComputerListener.preLaunch: %s, %s", c, taskListener));
        computers.put(c.getName(), System.currentTimeMillis());
    }

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        logger.fine(String.format("DatadogComputerListener.onOnline: %s, %s", c, listener));

        if (computers.containsKey(c.getName())) {
            long startTime = computers.get(c.getName());
            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime);

            logger.log(Level.FINE, "Computer {0} needed {1}ms to bootstrap", new Object[] {c, duration});
            StatsDClient statsd = leaseStatsDClient();
            String hostname = descriptor.getHostname();
            statsd.gauge("bootstrap", duration /1000, "host:" + hostname);
        }
        super.onOnline(c, listener);
    }

    public StatsDClient leaseStatsDClient() {
        String daemonHost = descriptor.getDaemonHost();

        try {
            if (statsDClient == null) {
                statsDClient = new NonBlockingStatsDClient("jenkins.agent", daemonHost.split(":")[0],
                        Integer.parseInt(daemonHost.split(":")[1]));
            } else {
                logger.warning("StatsDClient is already set");
            }
        } catch (Exception e) {
            logger.severe(String.format("Error while configuring StatsDClient. Exception: %s", e.toString()));
        }
        return statsDClient;
    }
}
