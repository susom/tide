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
      assertNotEquals(result,0);
    }
  }


  @Test
  public void getGaussianRandomPositionInRange() {
    int count = 0;
    for (int j = 0; j < 100000000; j++) {
      int pos = Utility.getGaussianRandomPositionInRange(100, 4);
      if (pos > 95) {
        count ++;
        log.info(String.format("range:%s pos:%s", 100, pos));
      }

    }

    log.info(String.format("total count : %s", count / 100000000.0));
  }



}
