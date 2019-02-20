/*
 * Copyright 2019 The Board of Trustees of The Leland Stanford Junior University.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.github.susom.starr.deid.anonymizers;

import com.github.susom.starr.deid.anonymizers.GeneralAnonymizer;
import edu.stanford.irt.core.facade.AnonymizedItem;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralAnonymizerTest {
  private static final Logger log = LoggerFactory.getLogger(GeneralAnonymizerTest.class);
  String[] textArray = new String[]{
    "https://www.yahoo.com/testpage http://www.yahoo.com/testpage https://yahoo.com/testpage www.yahoo.com/testpage",
    "my phone is 650-876-9087, (650)356-8890 and 1-650-333-4456 ssn test: 345-76-7834 and 234-89-9909-ssn em : wcl@com.com  TEST URL go.com go.gggg and IP: 126.98.0.1 or FE80:0000:0A00:0FE0:0202:B3FF:FE1E:8329, https://www.yahoo.com/testpage ."
  };


  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void scrub() {

    GeneralAnonymizer ga = new GeneralAnonymizer();
    for(String text : textArray){
      List<AnonymizedItem > items = new ArrayList<>();
      String result = ga.scrub(text, "<offset10>" + text, items);
      System.out.println("INPUT:" + text);
      System.out.println("OUTPUT:" + result);

      for (AnonymizedItem item : items) {
        log.info(String.format("item words:%s type:%s span from:%s to:%s verify word:%s",
          item.getWord(), item.getType(), item.getStart(), item.getEnd(), text.substring(item.getStart(), item.getEnd())));
      }
    }


  }
}