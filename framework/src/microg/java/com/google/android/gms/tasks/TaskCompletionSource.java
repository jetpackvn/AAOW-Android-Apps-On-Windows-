package com.google.android.gms.tasks;

public class TaskCompletionSource<TResult> {
    private final Task.SimpleTask<TResult> task = new Task.SimpleTask<>();

    public Task<TResult> getTask() {
        return task;
    }

    public void setResult(TResult result) {
        if (!task.trySetResult(result)) {
            throw new IllegalStateException("Task is already complete");
        }
    }

    public boolean trySetResult(TResult result) {
        return task.trySetResult(result);
    }

    public void setException(Exception exception) {
        if (!task.trySetException(exception)) {
            throw new IllegalStateException("Task is already complete");
        }
    }

    public boolean trySetException(Exception exception) {
        return task.trySetException(exception);
    }
}
