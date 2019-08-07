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

import com.github.susom.starr.deid.DeidResultProc;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class AgeAnonymizerTest {

  private String[] positiveTests = new String[] {
      "90 year old",
      "100 Y, M",
      "94-year-old",
      "97 Y F",
      "102 Year-old",
      "100 yo",
      "102y/o"
  };

  String[] negativeTests = new String[] {
      "67 year old"
  };

  @Test
  public void scrub() {
    System.out.println("Ages to be detected.");
    for (String test : positiveTests) {
      List<AnonymizedItemWithReplacement> items = new ArrayList<>();
      AgeAnonymizer amizer = new AgeAnonymizer("[REMOVED]","age");
      amizer.find(test,items);
      String result = DeidResultProc.applyChange(items,test);
      System.out.println(test + " => " + result);
      Assert.assertNotEquals("age should be altered", test, result);
    }

    System.out.println("Ages to be ignored.");
    for (String test : negativeTests) {
      List<AnonymizedItemWithReplacement> items = new ArrayList<>();
      AgeAnonymizer amizer = new AgeAnonymizer("[REMOVED]","age");
      amizer.find(test,items);
      String result = DeidResultProc.applyChange(items,test);
      System.out.println(test + " => " + result);
      Assert.assertFalse(result.contains("[REMOVED]"));
    }

  }

}
