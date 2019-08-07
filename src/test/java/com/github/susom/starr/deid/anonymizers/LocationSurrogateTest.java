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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.susom.starr.deid.DeidJob;
import com.github.susom.starr.deid.DeidJobs;
import com.github.susom.starr.deid.DeidResultProc;
import com.github.susom.starr.deid.DeidTransform;
import com.github.susom.starr.deid.anonymizers.LocationSurrogate.Address;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;

public class LocationSurrogateTest {
  private static final Logger log = LoggerFactory.getLogger(LocationSurrogateTest.class);

  private final String[] textArray = new String[]{
    "Kate MD, [ 123 Porter Ave, Palo Alto, CA 89093 ] ",
    "Stanford and Palo Alto ",
    "Starbucks in SF : 5290 Diamond Heights Blvd, San Francisco, CA 94131 ",
    "Starbucks at 5290 Diamond Heights, San Francisco ",
    "LOCATION OF OUTPATIENT CONSULTATION: San Ramon pediatric cardiology office."
    + "730 Ezra Rd  San Jose CA  94304-1503 and San Ramon in US"
  };

  private DeidJob job;


  @Before
  public void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
      .getResourceAsStream("deid_test_config.yaml"), DeidJobs.class);

    job = jobs.getDeidJobs()[0];
  }

  @Test
  public void scrub() throws IOException, ClassNotFoundException, SQLException {



    Address[] knownAddr = new Address[]{ new Address("Porter", "room 2", "123", "Ave", "Palo Alto","89093", "CA"), new Address("Stanford","building Y", "456", "Ave", "Palo Alto", "98739", "CA")};

    for(String text : textArray){

      List<AnonymizedItemWithReplacement > items = new ArrayList<>();

      List<AnonymizedItemWithReplacement> foundLocationItems = new ArrayList<>();
      List<AnonymizedItemWithReplacement> foundNameItems  = new ArrayList<>();

      DeidTransform.findEntiesWithNer(text, foundNameItems, foundLocationItems);

      LocationSurrogate locationSurrogate = new LocationSurrogate(knownAddr, "location",
        foundLocationItems, true);
      locationSurrogate.find(text, items);
      String result = DeidResultProc.applyChange(items,text);
      System.out.println("INPUT:" + text);
      System.out.println("OUTPUT:" + result);


      for (AnonymizedItemWithReplacement item : items) {
        log.info(String.format(Locale.ROOT,"word:[%s] verify:[%s] type:[%s] span[%s:%s] replacement:[%s]",
          item.getWord(), text.substring(Math.toIntExact(item.getStart()), Math.toIntExact(item.getEnd())), item.getType(), item.getStart(), item.getEnd(), item.getReplacement()));

        Assert.assertTrue(item.getReplacement() != null
            && !item.getReplacement().equals(item.getWord()));
      }
    }

  }

  @Test
  public void getCityLevelLocationRegex() {
    Address[] knownAddr = new Address[]{
        new Address("Porter", "room 2", "123", "Ave", "Palo Alto","89093", "CA"),
        new Address("Diamond Heights", "", "5290", "Blvd", "San Francisco","94131", "CA"),
        new Address("Stanford","building Y", "", "Ave", "Palo+Alto", "98739", "CA")};

    for(String text : textArray){
      for(Address address : knownAddr){
        String patternStr= LocationSurrogate.getCityLevelLocationRegex(address);
        System.out.println("CityLevelLocationRegex:" + patternStr);

        System.out.println("INPUT:" + text);
        System.out.println("OUTPUT:" + text.replaceAll(patternStr, "[replaced]"));

      }

    }


  }
}
