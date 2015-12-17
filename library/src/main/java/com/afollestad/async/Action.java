package com.afollestad.async;

import android.os.Handler;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class Action<RT> extends Base {

    private final Object LOCK = new Object();

    private Handler mHandler;
    private Pool mPool;
    private int mPoolIndex;
    private boolean mExecuting;
    private boolean mCancelled;
    private Thread mThread;
    private boolean mDone;
    private RT mResult;

    public Action() {
    }

    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public final int getPoolIndex() {
        return mPoolIndex;
    }

    @NonNull
    public abstract String id();

    @WorkerThread
    @Nullable
    protected abstract RT run() throws InterruptedException;

    @UiThread
    protected void done(@SuppressWarnings("UnusedParameters") @Nullable RT result) {
        // Optional
    }

    @UiThread
    public final void execute() {
        synchronized (LOCK) {
            if (mExecuting)
                throw new IllegalStateException("This action has already been executed.");
            mExecuting = true;
            mDone = false;
            mCancelled = false;

            if (mPool == null && mHandler == null) {
                LOG(Action.class, "Pool is null, creating action-level handler.");
                mHandler = new Handler();
            }
            LOG(Action.class, "Executing action %d (%s)...", getPoolIndex(), id());

            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mResult = Action.this.run();
                    } catch (InterruptedException e) {
                        LOG(Action.class, "Action %d (%s) was cancelled.", getPoolIndex(), id());
                        mCancelled = true;
                    }

                    mExecuting = false;
                    if (isCancelled()) {
                        LOG(Action.class, "Action %d (%s) was cancelled.", getPoolIndex(), id());
                        return;
                    }
                    post(new Runnable() {
                        @Override
                        public void run() {
                            LOG(Action.class, "Action %d (%s) finished executing!", getPoolIndex(), id());
                            mDone = true;
                            done(mResult);
                            mThread = null;
                            if (mPool != null)
                                mPool.pop(Action.this);
                        }
                    });
                }
            });
            mThread.start();
        }
    }

    public final void waitForExecution() {
        if (!isExecuting())
            throw new IllegalStateException(String.format("Action %d (%s) is not currently executing.", getPoolIndex(), id()));
        while (isExecuting() && !isCancelled()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @WorkerThread
    protected final void post(Runnable runnable) {
        synchronized (LOCK) {
            if (mPool == null) {
                mHandler.post(runnable);
            } else {
                mPool.post(runnable);
            }
        }
    }

    public final void cancel() {
        synchronized (LOCK) {
            mCancelled = true;
            if (mThread != null)
                mThread.interrupt();
            mThread = null;
        }
    }

    public final boolean isExecuting() {
        synchronized (LOCK) {
            return mExecuting;
        }
    }

    public final boolean isCancelled() {
        synchronized (LOCK) {
            return mCancelled;
        }
    }

    public final boolean isDone() {
        return mDone;
    }

    public RT getResult() {
        return mResult;
    }

    protected final void setPool(@Nullable Pool pool, @IntRange(from = -1, to = Integer.MAX_VALUE) int poolIndex) {
        synchronized (LOCK) {
            mPoolIndex = poolIndex;
            mCancelled = false;
            mExecuting = false;
            if (pool == null) {
                mPool = null;
                return;
            } else if (mPool != null) {
                throw new IllegalStateException(String.format("Action %s is already in use by another Pool.", id()));
            }
            mPool = pool;
        }
    }

    @Override
    public String toString() {
        return String.format("%s: %s", id(), mResult);
    }
}