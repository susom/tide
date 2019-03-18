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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlpTransformTest {
  String[] fields = new String[]{
      "F0001",
      "F0002",
      "i2b2",
      "NAMES"
  };

  String[] text = new String[]{
      ">>>>>>>>>>>>> more tests: date test Jan 01, 2018, ssn: 874-98-5739",
      "Jose's birth day: 2003-09-18, passport: 56521368, pp2: 56985631 ",
      "i2b2: Record date: 2088-07-03\\\\n\\\\n"
          + "Team 1 Intern Admission Note\\\\n\\\\n\\\\n\\\\nName: Younger, T Eugene\\\\n\\\\n"
          + "MR#: 6381987\\\\n\\\\nAtt: Dr. Gilbert\\\\n\\\\nCards: Dr. Ullrich\\\\n\\\\n"
          + "Neuro: Dr. Donovan\\\\n\\\\nDate of Admission: 7/2/88\\\\n\\\\n\\\\n\\\\n"
          + "CC: Lightheadedness, vertigo, and presyncopal sx x several episodes\\\\n\\\\n\\\\n\\\\n"
          + "HPI:. 64 yoM w/ significant PMH for CAD, HTN, GERD, and past cerebral embolism presents "
          + "w/ 6 hour history of vertiginous symptoms, dizziness, lightheadedness, and feeling \\\""
          + "like [he] was going to pass out\\\".  The pt recalls waking and getting ready for work.  "
          + "He then began having short episodes of vertiginous attacks in which he felt the room was "
          + "\\\"constantly going out of focus\\\" and inability to \\\"lock in on any one thing\\\".  "
          + "The pt had several episodes of these presyncopal attacks w/o LOC.  The pt had no associated "
          + "CP or palpitations, however noted some increased rate of breathing.  The pt also noted some "
          + "reflux sx a/w attacks.  He denies f/c, ns, d/c, diploplia/photophobia.  "
          + "Had associated nausea without vomiting as well as tinnitus, which he usually has.  "
          + "Attacks began to affect driving so he presented to EW.",
      "Inspiration from celebrities could be the reason Luna has shot up 31 places to "
          + "54 after Chrissy Teigen and singer John Legend named their daughter Luna "
          + "as well as actress Penelope Cruz and Uma Thurman who also chose the name "
          + "for their daughters. Harper continues to rise up the charts "
          + "(up 17 places to 22) seven years after The Beckham’s chose the name for their daughter."};

  DeidJob job;

  private static final Logger log = LoggerFactory.getLogger(DlpTransformTest.class);
  @Before
  public void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
      .getResourceAsStream("deid_test_config.yaml"), DeidJobs.class);

    job = jobs.deidJobs[0];
  }

  @Test
  public void dlpInspectRequest() throws IOException, IllegalAccessException {

    DeidResult deidResult = new DeidResult(new String[]{"id"}, new String[]{"0001"}, fields);
    DlpTransform transform = new DlpTransform(job, "som-rit-phi-starr-miner-dev");
    for (int i = 0; i < fields.length; i++) {
      transform.dlpInspectRequest(text[i],fields[i],deidResult,null);
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    for (String textField : deidResult.getTextFields()) {
      String statsDlpString = deidResult.getDataAsString(DeidResultProc.STATS_DLP + textField);
      String dlpResult = deidResult.getDataAsString(DeidResultProc.TEXT_DLP + textField);
      log.info("TextDlp:" + statsDlpString);
      log.info("TEXT_DLP:" + dlpResult);
      JsonNode node = mapper.readTree(statsDlpString);
      Assert.assertTrue(node.isArray() && node.size() > 0);
    }
  }


  @Test
  public void deidRequest() throws IOException {

    //DeidResult deidResult = new DeidResult(fields, text, new String[]{"TEXT"});

    //new DlpTransform(job, "som-rit-phi-starr-miner-dev").dlpDeidRequest(text,"text", deidResult);
    return;
  }


}
