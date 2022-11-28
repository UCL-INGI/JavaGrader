package org.javagrader.student;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class UnauthorizedCode {

    public static void staticMethodWithThread() {
        System.out.println("starting method");
        try {
            Thread thread = new Thread(() -> {
                System.out.println("started thread");
                ThreadMXBean t = ManagementFactory.getThreadMXBean();
                long start = t.getCurrentThreadCpuTime();
                int cnt = 0;
                while (t.getCurrentThreadCpuTime() - start < 400 * 1000000L) {
                    cnt += 1;
                }
                System.out.println("finished thread");
            });
            thread.start();
            thread.join();
        } catch (Exception e) {

        }
        System.out.println("finished method");
    }

    public void methodWithThread() {
        Thread thread = Thread.currentThread();
    }

}
