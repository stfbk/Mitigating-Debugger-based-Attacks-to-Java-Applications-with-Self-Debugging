import org.junit.Test;

import java.util.UUID;

public class HelloTest {

  @Test
  public void testHello () {
    System.out.println("before calling utility");
    Utility u = new Utility();
    System.out.println("after calling utility and before calling randomUUID the first time");
    String random1 = UUID.randomUUID().toString();
    u.evenMoreComplexProtection();
    System.out.println("after calling protection and before calling randomUUID the second time");
    String random2 = UUID.randomUUID().toString();
    if ((random1.equals(random2))) {
      System.out.println("unbelievable");
      throw new AssertionError();
    }
    else {
      System.out.println("assert true below");
      assert (true);
    }
  }
}
