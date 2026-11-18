package android.os;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cài đặt thật android.os.AsyncTask chạy tác vụ nền trên daemon ThreadPoolExecutor
 * và đẩy onProgressUpdate / onPostExecute về Main Looper (Swing EDT).
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AsyncTask<Params, Progress, Result> {
    private static final String LOG_TAG = "AsyncTask";

    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "AsyncTask #" + count.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    };

    private static final BlockingQueue<Runnable> POOL_WORK_QUEUE = new LinkedBlockingQueue<>(128);

    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                POOL_WORK_QUEUE, THREAD_FACTORY);
        executor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = executor;
    }

    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    private static final InternalHandler INTERNAL_HANDLER = new InternalHandler();

    public enum Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    private static final class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        Runnable active;

        @Override
        public synchronized void execute(final Runnable r) {
            tasks.offer(() -> {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            });
            if (active == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((active = tasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(active);
            }
        }
    }

    private static class InternalHandler extends Handler {
        InternalHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT -> result.task.finish(result.data[0]);
                case MESSAGE_POST_PROGRESS -> result.task.onProgressUpdate(result.data);
                default -> {
                }
            }
        }
    }

    private static class AsyncTaskResult<Data> {
        final AsyncTask task;
        final Data[] data;

        AsyncTaskResult(AsyncTask task, Data... data) {
            this.task = task;
            this.data = data;
        }
    }

    private static abstract class WorkerRunnable<Params, Result> implements Callable<Result> {
        Params[] params;
    }

    private final WorkerRunnable<Params, Result> worker;
    private final FutureTask<Result> future;
    private volatile Status status = Status.PENDING;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean taskInvoked = new AtomicBoolean();

    public AsyncTask() {
        worker = new WorkerRunnable<>() {
            @Override
            public Result call() {
                taskInvoked.set(true);
                Result result = null;
                try {
                    result = doInBackground(params);
                } catch (Throwable tr) {
                    cancelled.set(true);
                    throw tr;
                } finally {
                    postResult(result);
                }
                return result;
            }
        };

        future = new FutureTask<>(worker) {
            @Override
            protected void done() {
                try {
                    postResultIfNotInvoked(get());
                } catch (InterruptedException e) {
                    Log.w(LOG_TAG, e.toString());
                } catch (ExecutionException e) {
                    throw new RuntimeException("An error occurred while executing doInBackground()", e.getCause());
                } catch (CancellationException e) {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    private void postResultIfNotInvoked(Result result) {
        if (!taskInvoked.get()) {
            postResult(result);
        }
    }

    private Result postResult(Result result) {
        Message message = INTERNAL_HANDLER.obtainMessage(
                MESSAGE_POST_RESULT, new AsyncTaskResult<>(this, result));
        message.sendToTarget();
        return result;
    }

    public final Status getStatus() {
        return status;
    }

    protected abstract Result doInBackground(Params... params);

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onProgressUpdate(Progress... values) {
    }

    protected void onCancelled(Result result) {
        onCancelled();
    }

    protected void onCancelled() {
    }

    public final boolean isCancelled() {
        return cancelled.get();
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
        cancelled.set(true);
        return future.cancel(mayInterruptIfRunning);
    }

    public final Result get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    public final Result get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    public final AsyncTask<Params, Progress, Result> execute(Params... params) {
        return executeOnExecutor(SERIAL_EXECUTOR, params);
    }

    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor exec, Params... params) {
        if (status != Status.PENDING) {
            switch (status) {
                case RUNNING -> throw new IllegalStateException("Cannot execute task: the task is already running.");
                case FINISHED -> throw new IllegalStateException("Cannot execute task: the task has already been executed.");
            }
        }
        status = Status.RUNNING;
        onPreExecute();
        worker.params = params;
        exec.execute(future);
        return this;
    }

    public static void execute(Runnable runnable) {
        SERIAL_EXECUTOR.execute(runnable);
    }

    protected final void publishProgress(Progress... values) {
        if (!isCancelled()) {
            INTERNAL_HANDLER.obtainMessage(
                    MESSAGE_POST_PROGRESS, new AsyncTaskResult<>(this, values)).sendToTarget();
        }
    }

    private void finish(Result result) {
        if (isCancelled()) {
            onCancelled(result);
        } else {
            onPostExecute(result);
        }
        status = Status.FINISHED;
    }
}
