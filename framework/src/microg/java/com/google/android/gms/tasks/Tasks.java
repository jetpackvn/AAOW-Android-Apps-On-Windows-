package com.google.android.gms.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public final class Tasks {
    private Tasks() {
    }

    public static <TResult> Task<TResult> forResult(TResult result) {
        TaskCompletionSource<TResult> tcs = new TaskCompletionSource<>();
        tcs.setResult(result);
        return tcs.getTask();
    }

    public static <TResult> Task<TResult> forException(Exception ex) {
        TaskCompletionSource<TResult> tcs = new TaskCompletionSource<>();
        tcs.setException(ex);
        return tcs.getTask();
    }

    public static <TResult> Task<TResult> forCanceled() {
        Task.SimpleTask<TResult> t = new Task.SimpleTask<>();
        t.cancel();
        return t;
    }

    public static <TResult> Task<TResult> call(Callable<TResult> callable) {
        return call(Task.TaskExecutors.DIRECT, callable);
    }

    public static <TResult> Task<TResult> call(Executor executor, Callable<TResult> callable) {
        TaskCompletionSource<TResult> tcs = new TaskCompletionSource<>();
        Executor ex = executor != null ? executor : Task.TaskExecutors.DIRECT;
        ex.execute(() -> {
            try {
                tcs.setResult(callable.call());
            } catch (Exception e) {
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public static <TResult> TResult await(Task<TResult> task) throws ExecutionException, InterruptedException {
        if (task == null) throw new NullPointerException("Task must not be null");
        if (!task.isComplete()) {
            if (task instanceof Task.SimpleTask<TResult> st) {
                st.await();
            }
        }
        return unwrapResult(task);
    }

    public static <TResult> TResult await(Task<TResult> task, long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        if (task == null) throw new NullPointerException("Task must not be null");
        if (!task.isComplete() && task instanceof Task.SimpleTask<TResult> st) {
            if (!st.await(timeout, unit)) {
                throw new TimeoutException("Timed out waiting for Task");
            }
        }
        return unwrapResult(task);
    }

    public static Task<Void> whenAll(Collection<? extends Task<?>> tasks) {
        if (tasks == null || tasks.isEmpty()) return forResult(null);
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        AtomicInteger remaining = new AtomicInteger(tasks.size());
        for (Task<?> t : tasks) {
            t.addOnCompleteListener(Task.TaskExecutors.DIRECT, done -> {
                if (remaining.decrementAndGet() == 0) {
                    tcs.trySetResult(null);
                }
            });
        }
        return tcs.getTask();
    }

    public static Task<Void> whenAll(Task<?>... tasks) {
        return whenAll(tasks != null ? Arrays.asList(tasks) : List.of());
    }

    public static Task<List<Task<?>>> whenAllComplete(Collection<? extends Task<?>> tasks) {
        if (tasks == null || tasks.isEmpty()) return forResult(List.of());
        List<Task<?>> snapshot = new ArrayList<>(tasks);
        return whenAll(snapshot).continueWith(t -> snapshot);
    }

    private static <TResult> TResult unwrapResult(Task<TResult> task) throws ExecutionException {
        if (task.isSuccessful()) return task.getResult();
        if (task.isCanceled()) throw new CancellationException("Task is already canceled");
        throw new ExecutionException(task.getException());
    }
}
