package com.github.tezvn.starpvp.core.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadWorker {

    public static ExecutorService THREAD = Executors.newFixedThreadPool(10);

    public static void submit(Runnable runnable) {
        THREAD.submit(runnable);
    }
}
