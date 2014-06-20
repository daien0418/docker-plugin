package com.nirima.jenkins.plugins.docker;

import com.google.common.base.Objects;
import com.nirima.jenkins.plugins.docker.action.DockerBuildAction;
import hudson.model.*;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.OfflineCause;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by magnayn on 09/01/2014.
 */
public class DockerComputer extends AbstractCloudComputer<DockerSlave> {
    private static final Logger LOGGER = Logger.getLogger(DockerComputer.class.getName());


    private boolean haveWeRunAnyJobs = false;


    public DockerComputer(DockerSlave dockerSlave) {
        super(dockerSlave);
    }

    public DockerCloud getCloud() {
        return getNode().getCloud();
    }

    public boolean haveWeRunAnyJobs() {
        return haveWeRunAnyJobs;
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        super.taskAccepted(executor, task);
        LOGGER.fine(" Computer " + this + " taskAccepted");
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        super.taskCompleted(executor, task, durationMS);

        haveWeRunAnyJobs = true;

        Queue.Executable executable = executor.getCurrentExecutable();
        if( executable instanceof Run) {
            Run build = (Run) executable;
            DockerSlave slave = getNode();

            if( slave == null ) {
                LOGGER.log(Level.FINE, " Ignoring TaskCompleted for " + this + " as node has already been removed.");
            } else {
                slave.setRun(build);
            }

        }
        LOGGER.log(Level.FINE, " Computer " + this + " taskCompleted");

    }



    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
        LOGGER.log(Level.FINE, " Computer " + this + " taskCompletedWithProblems");
    }

    @Override
    public boolean isAcceptingTasks() {

        boolean result = !haveWeRunAnyJobs && super.isAcceptingTasks();

        // Quit quickly if we aren't accepting tasks
        if( !result )
            return false;

        // Update
        updateAcceptingTasks();

        // Are we still accepting tasks?
        result = !haveWeRunAnyJobs && super.isAcceptingTasks();

        return result;
    }

    private void updateAcceptingTasks() {
        try {
            DockerSlave node = getNode();
            if( getOfflineCause() != null) {
                setAcceptingTasks(false);
                LOGGER.log(Level.INFO, " Offline " + this + " due to " + getOfflineCause() );
            } else if( !node.containerExistsInCloud() ) {
                setAcceptingTasks(false);
            }
        } catch(Exception ex) {
            LOGGER.log(Level.INFO, " Computer " + this + " error getting node");
            setAcceptingTasks(false);
        }
    }

    public void onConnected(){
        DockerSlave node = getNode();
        if (node != null) {
            node.onConnected();
        }
    }



    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", super.getName())
                .add("slave", getNode())
                .toString();
    }
}
