package com.afollestad.async;

import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Pool extends Base {

    @IntDef({MODE_PARALLEL, MODE_SERIES})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    public static final int MODE_PARALLEL = 1;
    public static final int MODE_SERIES = 2;

    private final static Object LOCK = new Object();

    private final Handler mHandler;
    @Mode
    private final int mMode;
    private final ArrayList<Action> mQueue;
    private Done mDone;
    private Result mResult;

    @UiThread
    protected Pool(@NonNull Action[] actions, @Mode int mode) {
        mHandler = new Handler();
        mQueue = new ArrayList<>();
        Collections.addAll(mQueue, actions);
        mMode = mode;
        prepare();
    }

    private void prepare() {
        synchronized (LOCK) {
            for (int i = 0; i < mQueue.size(); i++) {
                mQueue.get(i).setPool(this, i);
                if (mMode == MODE_SERIES && i < mQueue.size() - 1)
                    mQueue.get(i).mNext = mQueue.get(i + 1);
            }
            LOG("Prepared %d actions for execution...", mQueue.size());
        }
    }

    @UiThread
    protected Pool execute() {
        synchronized (LOCK) {
            if (mQueue.size() == 0)
                throw new IllegalStateException("This Pool has already completed execution, or it's been cancelled.");
            mResult = new Result();
            if (mMode == MODE_SERIES) {
                LOG("Executing actions in SERIES mode...");
                mQueue.get(0).execute();
            } else {
                LOG("Executing actions in PARALLEL mode...");
                for (Action a : mQueue)
                    a.execute();
            }
            return this;
        }
    }

    @UiThread
    public void cancel() {
        synchronized (LOCK) {
            LOG("Cancelling all actions...");
            for (Action a : mQueue)
                a.cancel();
            mQueue.clear();
            mResult = null;
            Async.pop(this);
        }
    }

    public Pool done(Done done) {
        mDone = done;
        return this;
    }

    @UiThread
    protected void pop(Action action, Object result) {
        LOG("Removing action %d (%s) from pool.", action.index(), action.id());
        action.setPool(null, -1);
        mQueue.remove(action);
        mResult.put(action, result);

        if (mQueue.isEmpty()) {
            LOG("Queue size is empty, assuming all actions are done executing.");
            if (mDone != null) mDone.result(mResult);
            mResult = null;
            Async.pop(this);
        } else if (action.mNext != null) {
            LOG("Executing next action in the series: %d", action.mNext.index());
            action.mNext.execute();
        }
    }

    @WorkerThread
    protected void post(Runnable runnable) {
        mHandler.post(runnable);
    }
}