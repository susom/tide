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

package com.github.susom.starr;

import java.util.Locale;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class UtilityTest {

  private static final Logger log = LoggerFactory.getLogger(UtilityTest.class);

  @Test
  public void jitterHash() {
    String seedString;
    String salt = "batch000001";
    int result;

    for (int i = 0; i < 100; i++) {
      seedString = "PAT" + i;
      result = Utility.jitterHash(seedString, salt, 30);
      log.info("jitter:" + result);
      assertFalse(result == 0 || result > 30 || result < -30);
    }
  }


  @Test
  public void getGaussianRandomPositionInRange() {
    int testTotal = 100000000;
    int count95 = 0;
    int count10 = 0;
    for (int j = 0; j < testTotal; j++) {
      int pos = Utility.getGaussianRandomPositionInRange(100, 4);
      if (pos > 95) {
        count95 ++;
        //log.info(String.format("range:%s pos:%s", 100, pos));
      } else if (pos < 10){
        count10 ++;
        //log.info(String.format("range:%s pos:%s", 100, pos));
      }

    }
    log.info(String.format(Locale.ROOT,"total occurrence for 10 percent above : %s, which is about %s %%", count10,
      (double)count10 / (double)testTotal * 100));
    log.info(String.format(Locale.ROOT,"total occurrence for 95 percent above : %s, which is about %s %%", count95,
          (double)count95 / (double)testTotal * 100));

    int countOthers = testTotal - count10 - count95;
    assertTrue(count10 > countOthers && countOthers > count95);
  }



}
