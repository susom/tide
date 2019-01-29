package starr;

import com.github.susom.starr.Utility;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotEquals;

public class UtilityTest {
  private static final Logger log = LoggerFactory.getLogger(UtilityTest.class);
  @Test
  public void jitterHash() {
    String seedString;
    String salt = "batch000001";
    int result;

    for (int i=0;i<100; i++){
      seedString = "PAT"+i;
      result = Utility.jitterHash(seedString, salt, 30);
      log.info("jitter:"+result);
      assertNotEquals(result,0);
    }
  }
}
