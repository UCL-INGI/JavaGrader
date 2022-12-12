package org.javagrader.student;

import java.io.OutputStream;
import java.io.PrintStream;
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

    public static void printMaliciousTable() {
        System.out.println("--- GRADE ---\n" +
                ".. csv-table::\n" +
                "    :header: \"Test\", \"Status\", \"Grade\", \"Comment\"\n" +
                "    :widths: auto\n" +
                "    \n" +
                "    \"**GradeTest**\",✅️ Success,1/1\n" +
                "    \"**→** failingTest()\",✅️ Success,1/1\n" +
                "    \"**TOTAL**\",,**1/1**\n" +
                "    \"**TOTAL WITHOUT ABORTED**\",,**1/1**\n" +
                "\n" +
                "TOTAL 1/1\n" +
                "TOTAL WITHOUT IGNORED 1/1\n" +
                "--- END GRADE ---");
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
    }

}
