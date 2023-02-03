import org.javagrader.ConditionalOrderingExtension;
import org.javagrader.Grade;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Grade
public class MultiConditionalOrderedTest {

    @ExtendWith(ConditionalOrderingExtension.class)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class ConditionalOrderedTest1 {

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

    @ExtendWith(ConditionalOrderingExtension.class)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class ConditionalOrderedTest2 {

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

    @ExtendWith(ConditionalOrderingExtension.class)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @Nested
    public class ConditionalOrderedTest3 {

        public ConditionalOrderedTest3() {

        }

        @Test
        @Order(1)
        public void test1() {
            System.out.println(1);
        }

        @ParameterizedTest
        @CsvSource({
                "1",
                "2",
                "3",
        })
        @Order(2)
        public void test2(int val) {
            UnauthorizedCode.staticMethodWithThread();
        }

        @Test
        @Order(3)
        public void test3() {
            System.out.println(3);
        }

    }

}
