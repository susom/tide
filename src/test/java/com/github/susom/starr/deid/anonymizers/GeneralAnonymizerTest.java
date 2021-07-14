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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.susom.starr.deid.DeidResultProc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralAnonymizerTest {
  private static final Logger log = LoggerFactory.getLogger(GeneralAnonymizerTest.class);
  private String[] textArray = new String[]{
    "https://www.yahoo.com/testpage http://www.yahoo.com/testpage https://yahoo.com/testpage www.yahoo.com/testpage",
    "my phone is 650-876-9087, (650)356-8890 and 1-650-333-4456 ssn testSet: 345-76-7834 and 234-89-9909-ssn em : wcl@com.com  TEST URL go.com go.gggg and IP: 126.98.0.1 or FE80:0000:0A00:0FE0:0202:B3FF:FE1E:8329 ."
  };

  private int[] phiCount = new int[] {3, 8};

  @Test
  public void scrub() {

    GeneralAnonymizer generalAnonymizer = new GeneralAnonymizer();

    IntStream.range(0, textArray.length).forEach(i->{
      List<AnonymizedItemWithReplacement> items = new ArrayList<>();
      generalAnonymizer.find(textArray[i], items);
      String result = DeidResultProc.applyChange(items,textArray[i]);
      log.info("INPUT:" + textArray[i]);
      log.info("OUTPUT:" + result);

      for (AnonymizedItemWithReplacement item : items) {
        String clipFromOriginal = textArray[i].substring(Math.toIntExact(item.getStart()), Math.toIntExact(item.getEnd()));
        assertEquals("span start and end should be based on original, not on processed text with offset", item.getWord(), clipFromOriginal );
      }

      assertTrue(items.size() == phiCount[i]);
    });

    for(String text : textArray){
      List<AnonymizedItemWithReplacement > items = new ArrayList<>();
      generalAnonymizer.find(text, items);
      String result = DeidResultProc.applyChange(items,text);
      log.info("INPUT:" + text);
      log.info("OUTPUT:" + result);

      for (AnonymizedItemWithReplacement item : items) {
        Assert.assertNotEquals("text should be replaced", item.getWord(), item.replacement );
      }
    }

  }



  @Test
  public void scrubWithReplacementMap() throws JsonProcessingException {
    String configStr = "{\"general-phone\":\"999-999-9999\",\"general-ip\":\"000.000.000.000\",\"general-url\":\"www.example.com\"}";
    GeneralAnonymizer generalAnonymizer = new GeneralAnonymizer();
    HashMap replacementMap = new ObjectMapper().readValue(configStr, HashMap.class);
    generalAnonymizer.setReplacementMap(replacementMap);
    generalAnonymizer.includeTypesInMapOnly(true);

    for(String text : textArray){
      List<AnonymizedItemWithReplacement > items = new ArrayList<>();
      generalAnonymizer.find(text, items);
      String result = DeidResultProc.applyChange(items,text);
      log.info("INPUT:" + text);
      log.info("OUTPUT:" + result);

      for (AnonymizedItemWithReplacement item : items) {
        Assert.assertTrue("only process types in provided map", replacementMap.containsKey(item.getType()));
        Assert.assertNotEquals("text should be replaced", item.getWord(), item.replacement );
      }
    }

  }
}
