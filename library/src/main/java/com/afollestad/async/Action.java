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

    private Handler mHandler;
    private Pool mPool;
    private int mSelfIndex;
    private boolean mExecuted;
    private boolean mCancelled;
    protected Action mNext;
    private Thread mThread;

    public Action() {
    }

    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public final int index() {
        return mSelfIndex;
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
        if (mExecuted)
            throw new IllegalStateException("This action has already been executed.");
        mExecuted = true;
        if (mPool == null && mHandler == null) {
            LOG(Action.class, "Pool is null, creating action-level handler.");
            mHandler = new Handler();
        }
        LOG(Action.class, "Executing action %d (%s)...", index(), id());
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final RT result;
                try {
                    result = Action.this.run();
                } catch (InterruptedException e) {
                    LOG(Action.class, "Action %d (%s) was cancelled.", index(), id());
                    mCancelled = true;
                    mExecuted = false;
                    return;
                }

                mExecuted = false;
                if (isCancelled()) {
                    LOG(Action.class, "Action %d (%s) was cancelled.", index(), id());
                    return;
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        LOG(Action.class, "Action %d (%s) finished executing!", index(), id());
                        done(result);
                        if (mPool != null) mPool.pop(Action.this, result);
                    }
                });
            }
        });
        mThread.start();
    }

    @WorkerThread
    protected final void post(Runnable runnable) {
        if (mPool == null) {
            mHandler.post(runnable);
        } else {
            mPool.post(runnable);
        }
    }

    public final void cancel() {
        mCancelled = true;
        mThread.interrupt();
    }

    public final boolean isCancelled() {
        return mCancelled;
    }

    protected final void setPool(@Nullable Pool pool, @IntRange(from = -1, to = Integer.MAX_VALUE) int selfIndex) {
        mSelfIndex = selfIndex;
        mCancelled = false;
        mExecuted = false;
        if (pool == null) {
            mPool = null;
            return;
        } else if (mPool != null) {
            throw new IllegalStateException(String.format("Action %s is aleady in use by another Pool.", id()));
        }
        mPool = pool;
    }
}