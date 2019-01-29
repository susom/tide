package com.github.susom.starr.deid;

import static org.junit.Assert.*;

import edu.stanford.irt.core.facade.AnonymizedItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class GeneralAnonymizerTest {

  String[] textArray = new String[]{
    "https://www.yahoo.com/testpage http://www.yahoo.com/testpage https://yahoo.com/testpage www.yahoo.com/testpage",
    "Dear Dr. Aure,    I had the pleasure of seeing your patient Quentin Augostini in the cardiology clinic today.  my phone is 650-876-9087. and (650)356-8890, 1-650-333-4456 s test: 345-76-7834 and 234-89-9909-ssn LOCATION OF OUTPATIENT CONSULTATION: San Ramon pediatric cardiology office. 730 Ezra RD)  em : wcl@com.com  730 Ezra Rd  San Jose CA  94304-1503 and San Ramon in US. TEST URL go.com go.gggg and IP: 126.98.0.1 or FE80:0000:0A00:0FE0:0202:B3FF:FE1E:8329, https://www.yahoo.com/testpage ."
  };


  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void scrub() {
    List<AnonymizedItem > items = new ArrayList<>();
    GeneralAnonymizer ga = new GeneralAnonymizer();
    for(String text : textArray){
      String result = ga.scrub(text, items);
      System.out.println("INPUT:" + text);
      System.out.println("OUTPUT:" + result);
    }


  }
}
