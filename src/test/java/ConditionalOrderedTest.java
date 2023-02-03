import org.javagrader.ConditionalOrderingExtension;
import org.javagrader.Grade;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConditionalOrderingExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Grade
public class ConditionalOrderedTest {

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
