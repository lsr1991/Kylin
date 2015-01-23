package com.kylinolap.job.cube;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.kylinolap.cube.CubeInstance;
import com.kylinolap.cube.CubeManager;
import com.kylinolap.cube.CubeSegment;
import com.kylinolap.job.constant.ExecutableConstants;
import com.kylinolap.job.dao.JobPO;
import com.kylinolap.job.exception.ExecuteException;
import com.kylinolap.job.execution.ExecutableContext;
import com.kylinolap.job.execution.ExecuteResult;
import com.kylinolap.job.impl.threadpool.AbstractExecutable;

/**
 * Created by qianzhou on 1/4/15.
 */
public class UpdateCubeInfoAfterBuildStep extends AbstractExecutable {

    private static final String SEGMENT_ID = "segmentId";
    private static final String CUBE_NAME = "cubeName";
    private static final String CONVERT_TO_HFILE_STEP_ID = "convertToHFileStepId";
    private static final String BASE_CUBOID_STEP_ID = "baseCuboidStepId";
    private static final String CREATE_FLAT_TABLE_STEP_ID = "createFlatTableStepId";
    private static final String CUBING_JOB_ID = "cubingJobId";

    public UpdateCubeInfoAfterBuildStep() {
    }

    public UpdateCubeInfoAfterBuildStep(JobPO job) {
        super(job);
    }

    public void setCubeName(String cubeName) {
        this.setParam(CUBE_NAME, cubeName);
    }

    private String getCubeName() {
        return getParam(CUBE_NAME);
    }

    public void setSegmentId(String segmentId) {
        this.setParam(SEGMENT_ID, segmentId);
    }

    private String getSegmentId() {
        return getParam(SEGMENT_ID);
    }

    public void setConvertToHFileStepId(String id) {
        setParam(CONVERT_TO_HFILE_STEP_ID, id);
    }

    private String getConvertToHfileStepId() {
        return getParam(CONVERT_TO_HFILE_STEP_ID);
    }

    public void setBaseCuboidStepId(String id) {
        setParam(BASE_CUBOID_STEP_ID, id);
    }

    private String getBaseCuboidStepId() {
        return getParam(BASE_CUBOID_STEP_ID);
    }

    public void setCreateFlatTableStepId(String id) {
        setParam(CREATE_FLAT_TABLE_STEP_ID, id);
    }

    private String getCreateFlatTableStepId() {
        return getParam(CREATE_FLAT_TABLE_STEP_ID);
    }

    public void setCubingJobId(String id) {
        setParam(CUBING_JOB_ID, id);
    }

    private String getCubingJobId() {
        return getParam(CUBING_JOB_ID);
    }

    @Override
    protected ExecuteResult doWork(ExecutableContext context) throws ExecuteException {
        final CubeManager cubeManager = CubeManager.getInstance(context.getConfig());
        final CubeInstance cube = cubeManager.getCube(getCubeName());
        final CubeSegment segment = cube.getSegmentById(getSegmentId());

        String sourceRecordsSize = jobService.getOutput(getCreateFlatTableStepId()).getExtra().get(ExecutableConstants.SOURCE_RECORDS_SIZE);
        Preconditions.checkState(StringUtils.isNotEmpty(sourceRecordsSize), "Can't get cube source record size.");
        long sourceSize = Long.parseLong(sourceRecordsSize);

        String sourceRecordsCount = jobService.getOutput(getBaseCuboidStepId()).getExtra().get(ExecutableConstants.SOURCE_RECORDS_COUNT);
        Preconditions.checkState(StringUtils.isNotEmpty(sourceRecordsCount), "Can't get cube source record count.");
        long sourceCount = Long.parseLong(sourceRecordsCount);

        String cubeSizeString = jobService.getOutput(getConvertToHfileStepId()).getExtra().get(ExecutableConstants.HDFS_BYTES_WRITTEN);
        Preconditions.checkState(StringUtils.isNotEmpty(cubeSizeString), "Can't get cube segment size.");
        long size = Long.parseLong(cubeSizeString) / 1024;

        segment.setLastBuildJobID(getCubingJobId());
        segment.setLastBuildTime(System.currentTimeMillis());
        segment.setSizeKB(size);
        segment.setSourceRecords(sourceCount);
        segment.setSourceRecordsSize(sourceSize);

        try {
            cubeManager.promoteNewlyBuiltSegments(cube, segment);
            return new ExecuteResult(ExecuteResult.State.SUCCEED, "succeed");
        } catch (IOException e) {
            logger.error("fail to update cube after build", e);
            return new ExecuteResult(ExecuteResult.State.ERROR, e.getLocalizedMessage());
        }
    }
}