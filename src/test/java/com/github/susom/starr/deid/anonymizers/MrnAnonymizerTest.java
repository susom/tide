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

public class MrnAnonymizerTest {
  private String[] positiveTests = new String[]{
      "SSN style example: 123-45-6789",
      "RE: MRN:  166-52-35-2  ",
      "RE: George MR#:  178-83-87-2 DOB:  ",
      "RE: George Medical  Record #: 178-83-87-2",
      "MEDICAL  RECORD #: 191-38-59    ",
      "198-36-47 ",
      "MR#: 1520349-1",
      "MRN #1611634-0 x",
      "RE:  MRN:  0173297-5    ",
      "Medical Record Number: 15851368  Patient's Name:  ",
      "RE: BARBARA  MRN: 15851368  DOB: 02/21/1999  ",
      "Patient's Name: Alice  (MRN 16345362) ",
      "Name: Rosanne  MRN: 12453362  Week: 192  Date: 7-30-13",
      "MR NO: 15786376  DATE OF VISIT: 9/2/2009",
      "Subject: RE: Raymond, MR#14903871- Darb for ren*",
      "This office note has been dictated.  MRN 13903791.",
      "Stanford MRN: 04732775    ",
      "Judith (13206368) is a 81 Y",
      "Her Stanford Medical Record number is   16568107.",
      "Medical Record Number:   112 07 38 8    DOB:  1/01/1970",
      "MRN#:   112-07 38    DOB:  ",
      "Medical Rec #:  8235455",
      "MED REC #: (0000)000099999,"
  };

  private String[] negativeTests = new String[]{
      "plain number 123456789",
      "equation 1234567-345",
      "H12345678"
  };

  @Test
  public void scrub() {
    System.out.println("MRNs to be detected.");

    for (String test : positiveTests) {
      List<AnonymizedItemWithReplacement > items = new ArrayList<>();
      MrnAnonymizer amizer = new MrnAnonymizer("[REMOVED]","MRN");
      amizer.find(test, items);
      String result = DeidResultProc.applyChange(items,test);
      System.out.println(test + " => " + result);
      Assert.assertTrue(result.contains("[REMOVED]"));
    }

    System.out.println("Non-MRNs to be ignored.");
    for (String test : negativeTests) {
      List<AnonymizedItemWithReplacement > items = new ArrayList<>();
      MrnAnonymizer amizer = new MrnAnonymizer("[DELETED]","MRN");
      amizer.find(test, items);
      String result = DeidResultProc.applyChange(items,test);
      System.out.println(test + " => " + result);
      Assert.assertFalse(result.contains("[REMOVED]"));
    }
  }
}
