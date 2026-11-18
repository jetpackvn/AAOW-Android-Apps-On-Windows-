package com.google.android.gms.tasks;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/** Cài đặt Task&lt;TResult&gt; tương thích Google Play Services Tasks API (microG-lite). */
public abstract class Task<TResult> {
    public abstract boolean isComplete();

    public abstract boolean isSuccessful();

    public abstract boolean isCanceled();

    public abstract TResult getResult();

    public abstract <X extends Throwable> TResult getResult(Class<X> exceptionType) throws X;

    public abstract Exception getException();

    public Task<TResult> addOnCompleteListener(OnCompleteListener<TResult> listener) {
        return addOnCompleteListener(TaskExecutors.MAIN_THREAD, listener);
    }

    public Task<TResult> addOnCompleteListener(Executor executor, OnCompleteListener<TResult> listener) {
        return this;
    }

    public Task<TResult> addOnCompleteListener(Activity activity, OnCompleteListener<TResult> listener) {
        return addOnCompleteListener(TaskExecutors.MAIN_THREAD, listener);
    }

    public abstract Task<TResult> addOnSuccessListener(OnSuccessListener<? super TResult> listener);

    public abstract Task<TResult> addOnSuccessListener(Executor executor, OnSuccessListener<? super TResult> listener);

    public Task<TResult> addOnSuccessListener(Activity activity, OnSuccessListener<? super TResult> listener) {
        return addOnSuccessListener(TaskExecutors.MAIN_THREAD, listener);
    }

    public abstract Task<TResult> addOnFailureListener(OnFailureListener listener);

    public abstract Task<TResult> addOnFailureListener(Executor executor, OnFailureListener listener);

    public Task<TResult> addOnFailureListener(Activity activity, OnFailureListener listener) {
        return addOnFailureListener(TaskExecutors.MAIN_THREAD, listener);
    }

    public Task<TResult> addOnCanceledListener(OnCanceledListener listener) {
        return addOnCanceledListener(TaskExecutors.MAIN_THREAD, listener);
    }

    public Task<TResult> addOnCanceledListener(Executor executor, OnCanceledListener listener) {
        return this;
    }

    public <TContinuationResult> Task<TContinuationResult> continueWith(Continuation<TResult, TContinuationResult> continuation) {
        return continueWith(TaskExecutors.DIRECT, continuation);
    }

    public <TContinuationResult> Task<TContinuationResult> continueWith(Executor executor, Continuation<TResult, TContinuationResult> continuation) {
        TaskCompletionSource<TContinuationResult> tcs = new TaskCompletionSource<>();
        addOnCompleteListener(executor, t -> {
            try {
                tcs.setResult(continuation.then(t));
            } catch (Exception e) {
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Continuation<TResult, Task<TContinuationResult>> continuation) {
        return continueWithTask(TaskExecutors.DIRECT, continuation);
    }

    public <TContinuationResult> Task<TContinuationResult> continueWithTask(Executor executor, Continuation<TResult, Task<TContinuationResult>> continuation) {
        TaskCompletionSource<TContinuationResult> tcs = new TaskCompletionSource<>();
        addOnCompleteListener(executor, t -> {
            try {
                Task<TContinuationResult> next = continuation.then(t);
                next.addOnSuccessListener(TaskExecutors.DIRECT, tcs::setResult);
                next.addOnFailureListener(TaskExecutors.DIRECT, tcs::setException);
                next.addOnCanceledListener(TaskExecutors.DIRECT, () -> ((SimpleTask<TContinuationResult>) tcs.getTask()).cancel());
            } catch (Exception e) {
                tcs.setException(e);
            }
        });
        return tcs.getTask();
    }

    public <TContinuationResult> Task<TContinuationResult> onSuccessTask(SuccessContinuation<TResult, TContinuationResult> continuation) {
        return onSuccessTask(TaskExecutors.DIRECT, continuation);
    }

    public <TContinuationResult> Task<TContinuationResult> onSuccessTask(Executor executor, SuccessContinuation<TResult, TContinuationResult> continuation) {
        TaskCompletionSource<TContinuationResult> tcs = new TaskCompletionSource<>();
        addOnCompleteListener(executor, t -> {
            if (t.isSuccessful()) {
                try {
                    Task<TContinuationResult> next = continuation.then(t.getResult());
                    next.addOnSuccessListener(TaskExecutors.DIRECT, tcs::setResult);
                    next.addOnFailureListener(TaskExecutors.DIRECT, tcs::setException);
                } catch (Exception e) {
                    tcs.setException(e);
                }
            } else if (t.isCanceled()) {
                ((SimpleTask<TContinuationResult>) tcs.getTask()).cancel();
            } else {
                tcs.setException(t.getException());
            }
        });
        return tcs.getTask();
    }

    static final class TaskExecutors {
        static final Executor DIRECT = Runnable::run;
        private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
        static final Executor MAIN_THREAD = r -> {
            if (Looper.getMainLooper().isCurrentThread()) r.run();
            else MAIN_HANDLER.post(r);
        };
    }

    static final class SimpleTask<TResult> extends Task<TResult> {
        private final Object lock = new Object();
        private final CountDownLatch latch = new CountDownLatch(1);
        private final List<Runnable> listeners = new ArrayList<>();
        private boolean complete;
        private boolean canceled;
        private TResult result;
        private Exception exception;

        void await() throws InterruptedException {
            latch.await();
        }

        boolean await(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        boolean trySetResult(TResult res) {
            List<Runnable> pending;
            synchronized (lock) {
                if (complete) return false;
                complete = true;
                result = res;
                pending = new ArrayList<>(listeners);
                listeners.clear();
            }
            latch.countDown();
            pending.forEach(Runnable::run);
            return true;
        }

        boolean trySetException(Exception ex) {
            List<Runnable> pending;
            synchronized (lock) {
                if (complete) return false;
                complete = true;
                exception = ex != null ? ex : new RuntimeException("Unknown task error");
                pending = new ArrayList<>(listeners);
                listeners.clear();
            }
            latch.countDown();
            pending.forEach(Runnable::run);
            return true;
        }

        boolean cancel() {
            List<Runnable> pending;
            synchronized (lock) {
                if (complete) return false;
                complete = true;
                canceled = true;
                pending = new ArrayList<>(listeners);
                listeners.clear();
            }
            latch.countDown();
            pending.forEach(Runnable::run);
            return true;
        }

        private void enqueue(Executor exec, Runnable action) {
            Executor e = exec != null ? exec : TaskExecutors.DIRECT;
            boolean done;
            synchronized (lock) {
                done = complete;
                if (!done) {
                    listeners.add(() -> e.execute(action));
                }
            }
            if (done) {
                e.execute(action);
            }
        }

        @Override
        public boolean isComplete() {
            synchronized (lock) {
                return complete;
            }
        }

        @Override
        public boolean isSuccessful() {
            synchronized (lock) {
                return complete && !canceled && exception == null;
            }
        }

        @Override
        public boolean isCanceled() {
            synchronized (lock) {
                return canceled;
            }
        }

        @Override
        public TResult getResult() {
            synchronized (lock) {
                if (!complete) throw new IllegalStateException("Task is not yet complete");
                if (canceled) throw new java.util.concurrent.CancellationException("Task was canceled");
                if (exception != null) throw new RuntimeException(exception);
                return result;
            }
        }

        @Override
        public <X extends Throwable> TResult getResult(Class<X> exceptionType) throws X {
            synchronized (lock) {
                if (!complete) throw new IllegalStateException("Task is not yet complete");
                if (canceled) throw new java.util.concurrent.CancellationException("Task was canceled");
                if (exceptionType != null && exceptionType.isInstance(exception)) {
                    throw exceptionType.cast(exception);
                }
                if (exception != null) throw new RuntimeException(exception);
                return result;
            }
        }

        @Override
        public Exception getException() {
            synchronized (lock) {
                return exception;
            }
        }

        @Override
        public Task<TResult> addOnCompleteListener(Executor executor, OnCompleteListener<TResult> listener) {
            if (listener != null) {
                enqueue(executor, () -> listener.onComplete(this));
            }
            return this;
        }

        @Override
        public Task<TResult> addOnSuccessListener(OnSuccessListener<? super TResult> listener) {
            return addOnSuccessListener(TaskExecutors.MAIN_THREAD, listener);
        }

        @Override
        public Task<TResult> addOnSuccessListener(Executor executor, OnSuccessListener<? super TResult> listener) {
            if (listener != null) {
                enqueue(executor, () -> {
                    if (isSuccessful()) listener.onSuccess(getResult());
                });
            }
            return this;
        }

        @Override
        public Task<TResult> addOnFailureListener(OnFailureListener listener) {
            return addOnFailureListener(TaskExecutors.MAIN_THREAD, listener);
        }

        @Override
        public Task<TResult> addOnFailureListener(Executor executor, OnFailureListener listener) {
            if (listener != null) {
                enqueue(executor, () -> {
                    if (!isSuccessful() && !isCanceled()) listener.onFailure(getException());
                });
            }
            return this;
        }

        @Override
        public Task<TResult> addOnCanceledListener(Executor executor, OnCanceledListener listener) {
            if (listener != null) {
                enqueue(executor, () -> {
                    if (isCanceled()) listener.onCanceled();
                });
            }
            return this;
        }
    }
}
