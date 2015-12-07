package com.afollestad.async;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import java.util.ArrayList;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Async extends Base {

    private static Async mInstance;
    private static final Object LOCK = new Object();

    private ArrayList<Pool> mPools;

    private Async() {
        mPools = new ArrayList<>();
    }

    @UiThread
    protected static Async instance() {
        if (mInstance == null)
            mInstance = new Async();
        return mInstance;
    }

    @UiThread
    public static Pool parallel(@NonNull Action... actions) {
        synchronized (LOCK) {
            Pool pool = new Pool(actions, Pool.MODE_PARALLEL).execute();
            instance().mPools.add(pool);
            return pool;
        }
    }

    @UiThread
    public static Pool series(@NonNull Action... actions) {
        synchronized (LOCK) {
            Pool pool = new Pool(actions, Pool.MODE_SERIES).execute();
            instance().mPools.add(pool);
            return pool;
        }
    }

    public static void cancelAll() {
        synchronized (LOCK) {
            for (Pool p : instance().mPools)
                p.cancel();
        }
    }

    protected static void pop(Pool pool) {
        LOG(Async.class, "Popping pool...");
        synchronized (LOCK) {
            instance().mPools.remove(pool);
        }
    }
}