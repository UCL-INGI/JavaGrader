# JavaGrader: grading java made simple

Simply grade student assignments made in Java or anything that runs on the JVM (Scala/Kotlin/Jython/...).

This project is a continuation of [JavaGrading](https://github.com/GuillaumeDerval/JavaGrading), compatible with junit 5 and java 8+

```java
@Test
@Grade(value = 5, cpuTimeout=2)
@GradeFeedback(message="Are you sure your code is in O(n) ?", on=TIMEOUT)
@GradeFeedback(message="Sorry, something is wrong with your algorithm", on=FAIL)
void yourtest() {
    //a test for the student's code
}
```

Features:
- CPU timeouts on the code
- Text/RST reporting
- Custom feedback, both from outside the test (on fail, timeout, ...) but also from inside (see below).
- Allow / Disable loading libraries
- Can stop the test suite on the first detected failure
- Compatible with junit 5 features (repeated test, parametrized tests, tags, ...)

This library is best used with an autograder, such as [INGInious](https://github.com/UCL-INGI/INGInious).

## Example

Register the `GraderExtension` and add the `@Grade` annotation on your JUnit 5 tests like this:

```java
@Grade //or use @ExtendWith(GraderExtension.class)
public class MyTests {

    @Test
    @Grade(value = 5)
    void mytest1() {
        //this works
        something();
    }

    @Test
    @Grade(value = 3)
    @GradeFeedback(message = "You forgot to consider this particular case [...]", on = FAIL)
    void mytest2() {
        //this doesn't
        somethingElse();
    }
}
```
Note that we demonstrate here the usage of the `@GradeFeedback` annotation, that allows to give feedback to the students.

By running the tests, this will print on the standard output

```
--- GRADE ---
- MyTests ❌ **Failed** 5/8
	mytest1() ✅️ Success 5/5
	mytest2() ❌ **Failed** 0/3
		You forgot to consider this particular case [...]
TOTAL 5/8
TOTAL WITHOUT ABORTED 5/8
--- END GRADE ---
```

## Documentation & installation

Everything needed is located inside the files:

- [Grade.java](https://github.com/augustindelecluse/javagrader/blob/main/src/main/java/org/javagrader/Grade.java)
- [GradeFeedback.java](https://github.com/augustindelecluse/javagrader/blob/main/src/main/java/org/javagrader/GradeFeedback.java)
- [GraderExtension.java](https://github.com/augustindelecluse/javagrader/blob/main/src/main/java/org/javagrader/GraderExtension.java)
- [ConditionalOrderingExtension.java](https://github.com/augustindelecluse/javagrader/blob/main/src/main/java/org/javagrader/ConditionalOrderingExtension.java)
- [CustomGradingResult.java](https://github.com/augustindelecluse/javagrader/blob/main/src/main/java/org/javagrader/CustomGradingResult.java)
- [Allow.java](https://github.com/augustindelecluse/javagrader/blob/main/src/main/java/org/javagrader/Allow.java)
- [Forbid.java](https://github.com/augustindelecluse/javagrader/blob/main/src/main/java/org/javagrader/Forbid.java)

To add it as a dependency of your project, you can add this to your pom.xml in maven:
```xml
<dependency>
  <groupId>xxx</groupId>
  <artifactId>JavaGrader</artifactId>
  <version>xxx</version>
</dependency>
```

If you are not using maven, [search.maven](https://search.maven.org/artifact/xxx) probably has the line of code you need.

## Advanced examples

### Cpu timeout
It is (strongly) advised when using an autograder (did I already say that [INGInious](https://github.com/UCL-INGI/INGInious) is a very nice one?)
to put a maximum time to run a test:
```java
@Test
@Grade(value = 5, cpuTimeout=500, units = TimeUnit.MILLISECONDS)
void yourtest() {
    //a test for the student's code
}
```

If the test runs for more than 500 milliseconds, it will receive a TimeoutException and receive a grade of 0/5.

**Note that if the students create new thread(s), the time taken in the new
thread(s) won't be taken into account!**

It is also possible to add a wall-clock-time timeout, via JUnit:
```java
@Test
@Timeout(3) //kills the test after 3s in real, wall-clock time
@Grade(value = 5)
void yourtest() {
    //a test for the student's code
}
```

**By default, setting a CPU timeout also sets a wall-clock timeout at three times the cpu timeout.**
If you want to override that, set a different value by using `@Timeout` from Junit.


### Disabling tests
Disabled tests are supported:

```java
@ExtendWith(GraderExtension.class)
public class RunTests {

    @Test
    @Grade
    void passingTest() {}

    @Test
    @Grade
    @Disabled
    void disabledTest() {}

    @Test
    @Grade
    void abortedTest() {
        Assumptions.abort();
    }
}
```

The total grade without aborted and disabled tests can also be retrieved

```
--- GRADE ---
- RunTests ❌ **Failed** 1/3
	disabledTest() 🚫 Disabled 0/1
	abortedTest() 🚫 Aborted 0/1
	passingTest() ✅️ Success 1/1
TOTAL 1/3
TOTAL WITHOUT IGNORED 1/1
--- END GRADE ---
```

You thus need to prevent yourself from students throwing a `TestAbortedException` inside their code

This can easily be done by configuring your packages such that junit is not exposed to them

### Conditional execution - Stop the tests as soon as one fails

JavaGrader also provides a `ConditionalOrderingExtension`. This ensures that the test suite will disable all remaining tests as soon as one failure happens.
This is best combined with `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and `@Order` methods.

```
@ExtendWith(ConditionalOrderingExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Grade
public class OrderedTest {

    @Test
    @Order(1)
    public void test1() {
        System.out.println(1);
    }

    @Test
    @Order(2)
    public void test2() {
        fail();
    }
    
    @Test
    @Order(3)
    public void test3() {
        System.out.println(3);
    }
    
}
```

This gives the following results

```
Test ignored as the last one failed
Max timeout = 0:00:00
Max cpu timeout = 0:00:00
--- GRADE ---
- OrderedTest ❌ **Failed** 0.33/1
	test1() ✅️ Success 0.33/0.33
	test2() ❌ **Failed** 0/0.33
	test3() 🚫 Disabled 0/0.33
TOTAL 0.33/1
TOTAL WITHOUT IGNORED 0.33/0.67
--- END GRADE ---
```

### Custom feedback (outside the test)
Use the `@GradeFeedback` annotation to give feedback about specific type of errors
```java
@Test
@Grade(value = 5)
@GradeFeedback(message = "Congrats!", on=SUCCESS)
@GradeFeedback(message = "Something is wrong", on=FAIL)
@GradeFeedback(message = "Too slow!", on=TIMEOUT)
@GradeFeedback(message = "We chose to disable this test", on=DISABLED)
@GradeFeedback(message = "We chose to abort this test", on=ABORTED)
void yourtest() {
    //
}
```

### RST output
When using an autograder (I may already have told you that [INGInious](https://github.com/UCL-INGI/INGInious) is very nice)
you might want to output something nice (i.e. not text) for the students. JavaGrader can output a nice
RestructuredText table (which is the default behavior):

![Screenshot of the RST output](https://raw.githubusercontent.com/UCL-INGI/JavaGrader/blob/main/rst_screenshot.png "Screenshot of the RST output")

### Grading a class

The `@Grade` annotation allows setting an overall max grade for the whole class and timeout for all tests
(avoiding to put `@Grade` on all methods). `@Grade` annotations put on method will override the default settings

### Parameterized / Repeated tests
JUnit's parameterized and repeated tests are also supported:

```java
@Grade
public class MultipleGradeTests {

    @Grade
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void testWithValueSource(int argument) {
        assertEquals(0, argument % 2);
    }

    @RepeatedTest(5)
    @Grade
    public void testTwice(RepetitionInfo info) {
        assertEquals(0, info.getCurrentRepetition() % 2);
    }
}
```

output:
```
--- GRADE ---
- MultipleGradeTests ❌ **Failed** 3/8
	[1] 1 ❌ **Failed** 0/1
	[2] 2 ✅️ Success 1/1
	[3] 3 ❌ **Failed** 0/1
	repetition 1 of 5 ❌ **Failed** 0/1
	repetition 2 of 5 ✅️ Success 1/1
	repetition 3 of 5 ❌ **Failed** 0/1
	repetition 4 of 5 ✅️ Success 1/1
	repetition 5 of 5 ❌ **Failed** 0/1
TOTAL 3/8
TOTAL WITHOUT ABORTED 3/8
--- END GRADE ---
```

### Custom permissions - Allow and Forbid libraries

JavaGrader overrides the `ClassLoader` that would normally be used within the tests. This ensures that a custom one is used, forbidding some imports through a `@Forbid`.

```
@Test
@Grade
@Forbid("java.lang.Thread")
public void invalidImportTest() {
    UnauthorizedCode.staticMethodWithThread();
}
```

Some imports are always forbidden, such as `Thread` or `ClassLoader`. You can optionally allow such imports (bypassing a `@Forbid`)with `@Allow`

```
@Test
@Grade
@Allow("java.lang.Thread")
public void validImportTest() {
    // thread is now allowed
    UnauthorizedCode.staticMethodWithThread();
}
```

Overriding the `ClassLoader` does have some side effects. If you find yourself in trouble when running tests, you can disable this feature with `@Grade(noSecurity = true)`

```
@Test
@Grade(noSecurity = true)
@Forbid("java.lang.Thread")
public void testWithoutSecurity() {
    // works even though thread is forbidden, as the security is disabled
    UnauthorizedCode.staticMethodWithThread();
}
```

### Tagging and filtering

All `@Grade` tests come with a tag `@Tag("grade")` by default. 
This can be used to filter the tests, for instance with the following command line if the [surefire plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) is used

```
mvn -Dtests=grade test
```

Note that JavaGrader *does* run all unfiltered tests if it is registered.
This means that the following tests will be run (although no result for it will be printed as there is no `@Grade` annotation)

```java
@ExtendWith(GraderExtension.class)
public class RunTests {

    @Test
    void mytest1() {
        //executed as it is as test but not reported as it is not @Grade
        something();
    }

}
```