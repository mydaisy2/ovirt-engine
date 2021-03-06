package org.ovirt.engine.core.bll.gluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.job.ExecutionContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.job.JobRepositoryFactory;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeParameters;
import org.ovirt.engine.core.common.asynctasks.gluster.GlusterAsyncTask;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.job.ExternalSystemType;
import org.ovirt.engine.core.common.job.JobExecutionStatus;
import org.ovirt.engine.core.common.job.Step;
import org.ovirt.engine.core.common.job.StepEnum;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.job.ExecutionMessageDirector;
import org.ovirt.engine.core.utils.lock.EngineLock;

public abstract class GlusterAsyncCommandBase<T extends GlusterVolumeParameters> extends GlusterVolumeCommandBase<T> {

    private Step asyncTaskStep;

    public GlusterAsyncCommandBase(T params) {
        super(params);
    }

    @Override
    protected boolean canDoAction() {
        GlusterVolumeEntity glusterVolume = getGlusterVolume();
        if (!super.canDoAction()) {
            return false;
        }

        if (!glusterVolume.isOnline()) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_GLUSTER_VOLUME_SHOULD_BE_STARTED);
        }

        return true;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(VdcBllMessages.VAR__TYPE__GLUSTER_VOLUME);
        addCanDoActionMessage(String.format("$volumeName %1$s", getGlusterVolumeName()));
        addCanDoActionMessage(String.format("$vdsGroup %1$s", getVdsGroupName()));
    }

    protected Map<String, String> getStepMessageMap(JobExecutionStatus status) {
        Map<String, String> values = new HashMap<String, String>();
        values.put(GlusterConstants.CLUSTER, getVdsGroupName());
        values.put(GlusterConstants.VOLUME, getGlusterVolumeName());
        values.put("status", status.toString());
        values.put("info", " ");
        return values;
    }

    /**
     *
     * @return the StepEnum associated with this command. This is used to start a sub step for the executing step.
     */
    protected abstract StepEnum getStepType();

    protected void startSubStep() {
        asyncTaskStep =
                ExecutionHandler.addSubStep(this.getExecutionContext(),
                        getExecutionContext().getJob().getStep(StepEnum.EXECUTING),
                        getStepType(),
                        ExecutionMessageDirector.resolveStepMessage(getStepType(),
                                getStepMessageMap(JobExecutionStatus.STARTED)));
    }

    protected void endStepJob() {
        GlusterAsyncTask asyncTask = getGlusterVolume().getAsyncTask();
        // Gluster Volume Rebalance Task will have only one step ( REBALANCING_VOLUME )
        Step step = getStepDao().getStepsByExternalId(asyncTask.getTaskId()).get(0);
        step.markStepEnded(JobExecutionStatus.ABORTED);
        step.setDescription(ExecutionMessageDirector.resolveStepMessage(getStepType(),
                getStepMessageMap(JobExecutionStatus.ABORTED)));
        JobRepositoryFactory.getJobRepository().updateStep(step);

        ExecutionContext finalContext = ExecutionHandler.createFinalizingContext(step.getId());
        ExecutionHandler.endTaskJob(finalContext, false);
    }

    protected GlusterAsyncTask handleTaskReturn(GlusterAsyncTask asyncTask) {
        Guid externalTaskId = asyncTask.getTaskId();
        asyncTaskStep.setStatus(JobExecutionStatus.STARTED);
        ExecutionHandler.updateStepExternalId(asyncTaskStep,
                externalTaskId,
                ExternalSystemType.GLUSTER);
        getExecutionContext().getJob().setStatus(JobExecutionStatus.STARTED);
        asyncTask.setStepId(asyncTaskStep.getId());
        return asyncTask;
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.singletonMap(getGlusterVolumeId().toString(),
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.GLUSTER,
                        VdcBllMessages.ACTION_TYPE_FAILED_VOLUME_OPERATION_IN_PROGRESS));
    }

    protected void releaseVolumeLock() {
        EngineLock lock = new EngineLock(getExclusiveLocks(), getSharedLocks());
        setLock(lock);
        freeLock();
    }

}
