package com.afollestad.async;

import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Result {

    private HashMap<String, Object> mMap;

    protected Result() {
        mMap = new HashMap<>();
    }

    public Object get(String id) {
        return mMap.get(id);
    }

    protected void put(Action action, Object result) {
        //noinspection ConstantConditions
        if (action.id() == null) return;
        mMap.put(action.id(), result);
    }

    public String[] getIds() {
        return mMap.keySet().toArray(new String[mMap.keySet().size()]);
    }
}