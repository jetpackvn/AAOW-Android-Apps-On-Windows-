package android.app.job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Triển khai tương thích {@code android.app.job.JobScheduler} quản lý danh sách job theo namespace
 * trong tiến trình AAOW.
 */
public abstract class JobScheduler {
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;

    public static final int PENDING_JOB_REASON_EXECUTING = -1;
    public static final int PENDING_JOB_REASON_INVALID_JOB_ID = -2;
    public static final int PENDING_JOB_REASON_UNDEFINED = 0;
    public static final int PENDING_JOB_REASON_APP = 1;
    public static final int PENDING_JOB_REASON_APP_STANDBY = 2;
    public static final int PENDING_JOB_REASON_BACKGROUND_RESTRICTION = 3;
    public static final int PENDING_JOB_REASON_CONSTRAINT_BATTERY_NOT_LOW = 4;
    public static final int PENDING_JOB_REASON_CONSTRAINT_CHARGING = 5;
    public static final int PENDING_JOB_REASON_CONSTRAINT_CONNECTIVITY = 6;
    public static final int PENDING_JOB_REASON_CONSTRAINT_CONTENT_TRIGGER = 7;
    public static final int PENDING_JOB_REASON_CONSTRAINT_DEVICE_IDLE = 8;
    public static final int PENDING_JOB_REASON_CONSTRAINT_MINIMUM_LATENCY = 9;
    public static final int PENDING_JOB_REASON_CONSTRAINT_PREFETCH = 10;
    public static final int PENDING_JOB_REASON_CONSTRAINT_STORAGE_NOT_LOW = 11;
    public static final int PENDING_JOB_REASON_DEVICE_STATE = 12;
    public static final int PENDING_JOB_REASON_JOB_SCHEDULER_OPTIMIZATION = 13;
    public static final int PENDING_JOB_REASON_QUOTA = 14;
    public static final int PENDING_JOB_REASON_USER = 15;
    public static final int PENDING_JOB_REASON_CONSTRAINT_DEADLINE = 16;

    public JobScheduler() {
    }

    public abstract int schedule(JobInfo job);

    public abstract int enqueue(JobInfo job, JobWorkItem work);

    public abstract void cancel(int jobId);

    public abstract void cancelAll();

    public void cancelInAllNamespaces() {
        cancelAll();
    }

    public abstract List<JobInfo> getAllPendingJobs();

    public abstract JobInfo getPendingJob(int jobId);

    public JobScheduler forNamespace(String namespace) {
        return this;
    }

    public String getNamespace() {
        return null;
    }

    public Map<String, List<JobInfo>> getPendingJobsInAllNamespaces() {
        Map<String, List<JobInfo>> out = new LinkedHashMap<>();
        out.put("", getAllPendingJobs());
        return out;
    }

    public int getPendingJobReason(int jobId) {
        return getPendingJob(jobId) != null ? PENDING_JOB_REASON_UNDEFINED : PENDING_JOB_REASON_INVALID_JOB_ID;
    }

    public int[] getPendingJobReasons(int jobId) {
        return new int[]{getPendingJobReason(jobId)};
    }

    public boolean canRunUserInitiatedJobs() {
        return true;
    }

    public static JobScheduler createDefault() {
        return new LocalJobScheduler(new ConcurrentHashMap<>(), null);
    }

    private static final class LocalJobScheduler extends JobScheduler {
        private final Map<String, Map<Integer, JobInfo>> namespaces;
        private final String namespace;

        LocalJobScheduler(Map<String, Map<Integer, JobInfo>> namespaces, String namespace) {
            this.namespaces = namespaces;
            this.namespace = namespace;
        }

        private Map<Integer, JobInfo> bucket() {
            return namespaces.computeIfAbsent(namespace == null ? "" : namespace,
                    ignored -> new ConcurrentHashMap<>());
        }

        @Override
        public int schedule(JobInfo job) {
            if (job == null) return RESULT_FAILURE;
            bucket().put(job.getId(), job);
            return RESULT_SUCCESS;
        }

        @Override
        public int enqueue(JobInfo job, JobWorkItem work) {
            return schedule(job);
        }

        @Override
        public void cancel(int jobId) {
            bucket().remove(jobId);
        }

        @Override
        public void cancelAll() {
            bucket().clear();
        }

        @Override
        public void cancelInAllNamespaces() {
            namespaces.clear();
        }

        @Override
        public List<JobInfo> getAllPendingJobs() {
            return new ArrayList<>(bucket().values());
        }

        @Override
        public JobInfo getPendingJob(int jobId) {
            return bucket().get(jobId);
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

        @Override
        public JobScheduler forNamespace(String ns) {
            if (ns == null || ns.equals(namespace)) return this;
            return new LocalJobScheduler(namespaces, ns);
        }

        @Override
        public Map<String, List<JobInfo>> getPendingJobsInAllNamespaces() {
            Map<String, List<JobInfo>> out = new LinkedHashMap<>();
            for (var e : namespaces.entrySet()) {
                out.put(e.getKey(), new ArrayList<>(e.getValue().values()));
            }
            return out;
        }
    }
}
