import org.javagrader.Allow;
import org.javagrader.Grade;
import org.junit.jupiter.api.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Grade
public class TimeoutTest {
    // does not fail as the cpu timeout nor the wall clock timeout is exceeded
    @Grade(value = 1, cpuTimeout = 100, unit = TimeUnit.MILLISECONDS)
    @Timeout(2)
    @Test
    @Tag("pass")
    @Allow("java.lang.Thread")
    void sleep200ms() throws Exception {
        Thread.sleep(200);
    }

    // fails as the cpu timeout is exceeded
    @Grade(value = 3, cpuTimeout = 100, unit = TimeUnit.MILLISECONDS)
    @Timeout(2)
    @Test
    void computeFor200ms() {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        long start = thread.getCurrentThreadCpuTime();
        int cnt = 0;
        while (thread.getCurrentThreadCpuTime() - start < 210*1000000L) {
            cnt +=1;
        }
    }

    // @Timeout takes the upper hand and the test fails
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    @Grade(cpuTimeout = 300, unit = TimeUnit.MILLISECONDS)
    @Test
    void naturalTimeout() {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        long start = thread.getCurrentThreadCpuTime();
        int cnt = 0;
        while (thread.getCurrentThreadCpuTime() - start < 200*1000000L) {
            cnt +=1 ;
        }
    }

    // passes as the cpu time is not computed from the spawned thread
    @Tag("pass")
    @Grade(cpuTimeout = 200, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @Test
    @Allow("java.lang.Thread")
    void timeoutInSpawnedThread() throws Exception {
        Thread thread = new Thread(() -> {
            ThreadMXBean t = ManagementFactory.getThreadMXBean();
            long start = t.getCurrentThreadCpuTime();
            int cnt = 0;
            while (t.getCurrentThreadCpuTime() - start < 400*1000000L) {
                cnt +=1 ;
            }
        });
        thread.start();
        thread.join();
    }

    // not setting this threadMode will cause the test to run forever
    @Grade(cpuTimeout = 50, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    @Test
    void timeoutInfiniteLoop() {
        while (true) {
            ;
        }
    }

    @RepeatedTest(2)
    @Grade(cpuTimeout = 100, unit = TimeUnit.MILLISECONDS)
    public void testTwice1(RepetitionInfo info) {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        long start = thread.getCurrentThreadCpuTime();
        int cnt = 0;
        while (thread.getCurrentThreadCpuTime() - start < 210*1000000L) {
            cnt +=1;
        }
    }

    // fails as the wall clock timeout is exceeded
    @RepeatedTest(2)
    @Grade(cpuTimeout = 50, unit = TimeUnit.MILLISECONDS)
    @Allow("java.lang.Thread")
    public void testTwice2(RepetitionInfo info) throws Exception {
        Thread.sleep(200);
    }

}