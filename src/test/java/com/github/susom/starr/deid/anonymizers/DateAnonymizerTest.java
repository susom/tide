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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateAnonymizerTest {

  private static final Logger log = LoggerFactory.getLogger(DateAnonymizerTest.class);

  private static String[] positiveTests = new String[]{
      "THE CHEST: 07/24/2017,CLINICAL HISTORY",
      "2088-07-03, 25-NOV-2008 10:17:35 , 04/18/2012, 3/29/2012, 02/10/2006 9:24 a, 09/08/2008 01:26 P CT",
      "25-NOV-2008 10:17:35",
      "25-11-2008 10:17:35",
      "04/18/2012",
      "04/19/2012",
      "04/20/2012",
      " 3/29/2012",
      "Date:7-30-2013 ",
      "05/10/2012 18:46:00",
      "02/10/2006 9:24 a",
      "05/29/200110:31 A",
      "09/08/2008 01:26 P CT",
      "02/25/2009 4:33PM",
      "12/23/05 7/4/20",
      "7-30-13",
      "10/5/35",
      "3/2012",
      "4/2009-9/2012 5/2013 7/1999 2/1989 2/5/1989 April 5, 1970",
      "MARCH of 2010",
      "December 2010",
      "April 18, 2012",
      "MAY 1st, 2009",
      "June 25TH, 2010",
      "July 8 2010",
      "NOVEMBER 1,\n2004",
      "Mar 4, 2014 18:00h",
      "4/2/2010 5/2/2014 6-4-14 march 10th, 2010",
      "May 10",     // Was not originally handled
      "DOB: 12/31/2000LOC: ABC" //when connects to surrounding
  };

  static String[] negativeTests = new String[]{
      "123-23-13"
//      "5/10",        // Not currently handled. Could be confused with non-date context.
//      "02/41/1970",
//      "2001-02-31"
  };

//  static String[] sortingByAge = new String[]{
//      "age 40.189 in 2010",
//      "age 40.942 in 2010",
//      "age 42.323 in 2012",
//      "age 42.326 in 2012",
//      "age 42.329 in 2012",
//      "age 42.268 in 2012",
//      "age 43.605 in 2013",
//      "age 36.134 in 2006",
//      "age 31.427 in 2001",
//      "age 38.712 in 2008"
//  };


  @Test
  public void scrub() throws ParseException {

    final Date bdate = new SimpleDateFormat("M/d/yyyy", Locale.ROOT).parse("01/01/1970");

//    Arrays.sort(sortingByAge);

//    for (String test : sortingByAge) {
//      log.info(test);
//    }

    log.info("Non-dates to be ignored");
    for (String test : positiveTests) {
      DateAnonymizer bdayAmizer = new DateAnonymizer(bdate,"date");
      DateAnonymizer jitterAmizer = new DateAnonymizer(15,"date", "");

      List<AnonymizedItemWithReplacement> items = new ArrayList<>();
      bdayAmizer.find(test, items);
      String result = DeidResultProc.applyChange(items,test);
      log.info("BirthDay Amizer " + test + " => " + result);

      items.clear();
      jitterAmizer.find(test, items);
      result = DeidResultProc.applyChange(items,test);
      log.info("Jitter Amizer " + test + " => " + result);
      Assert.assertNotEquals("should be altered",test, result );
    }



    log.info("Dates to be detected");
    for (String test : negativeTests) {
      DateAnonymizer bdayAmizer = new DateAnonymizer(bdate,"date");
      DateAnonymizer jitterAmizer = new DateAnonymizer(15,"date", "");

      List<AnonymizedItemWithReplacement> items = new ArrayList<>();
      bdayAmizer.find(test, items);
      String result = DeidResultProc.applyChange(items,test);
      log.info("BDayAmizer " + test + " => " + result);
//      Assert.assertEquals("should not be altered",test, result );

      items.clear();
      jitterAmizer.find(test, items);
      result = DeidResultProc.applyChange(items,test);
      log.info("JitterAmizer " + test + " => " + result);
      Assert.assertEquals("should not be altered",test, result );
    }


  }
}
