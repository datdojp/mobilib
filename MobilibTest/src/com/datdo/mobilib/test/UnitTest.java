package com.datdo.mobilib.test;

import java.util.UUID;

import com.datdo.mobilib.event.MblEventCenter;
import com.datdo.mobilib.event.MblStrongEventListener;

public class UnitTest {

    public static void run() {

        // CASE 1: test MblEventCenter cuncurrency
        String name = UUID.randomUUID().toString();
        MblEventCenter.addListener(new MblStrongEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                terminate();
            }
        }, name);
        MblEventCenter.addListener(new MblStrongEventListener() {
            @Override
            public void onEvent(Object sender, String name, Object... args) {
                terminate();
            }
        }, name);
        MblEventCenter.postEvent(null, name);
    }
}
