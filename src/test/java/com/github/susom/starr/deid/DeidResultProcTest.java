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

package com.github.susom.starr.deid;

import static org.junit.Assert.*;

import com.github.susom.starr.deid.anonymizers.AnonymizedItemWithReplacement;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeidResultProcTest {

  private static final Logger log = LoggerFactory.getLogger(DeidResultProc.class);
  @Test
  public void applyChange() {
    String text = "Case discussed with "
        + "Dr. Jacory Zaer, Vas"
        + "cular Surgery Fellow"
        + " and Dr. Jimena Fish"
        + ", Vascular Surgery A"
        + "ttending. end of tex";

    List<AnonymizedItemWithReplacement> findings = Arrays.asList(
        new AnonymizedItemWithReplacement(
            "Jacory Zaer", 24, 35,
            "[REMOVED]", "deid-test", "test"),
        new AnonymizedItemWithReplacement(
            "Dr. Jacory Zaer", 20, 35,
            "[REMOVED]", "deid-test", "test"),
        new AnonymizedItemWithReplacement(
            "Jacory", 24, 30,//inside
            "[REMOVED]", "deid-test", "test"),
        new AnonymizedItemWithReplacement(
            "with Dr. Jacory", 15, 30,
            "[REMOVED]", "deid-test", "test"),
        new AnonymizedItemWithReplacement(
            "Jimena", 69, 75,
            "[REMOVED]", "deid-test", "test")

    );

    String result = DeidResultProc.applyChange(findings, text);
    System.out.println("INPUT:" + text);
    System.out.println("OUTPUT:" + result);
    Assert.assertEquals("overlaping findings only removed as a whole", 2,
        StringUtils.countMatches(result, "[REMOVED]"));
  }
}