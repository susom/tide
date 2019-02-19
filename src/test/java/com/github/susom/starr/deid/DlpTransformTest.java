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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.privacy.dlp.v2.InspectResult;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DlpTransformTest {

  String text = ">>>>>>>>>>>>> more tests: date test Jan 01, 2018, ssn: 874-98-5739, birth day: 2003-09-18, passport: 56521368, pp2: 56985631 ";

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
    DeidResult deidResult = new DeidResult(new String[]{"0001"}, new String[]{text}, new String[]{"TEXT"});
    new DlpTransform(job, "som-rit-phi-starr-miner-dev").dlpInspectRequest(text,"text",deidResult,null);

    for (String textField : deidResult.getTextFields()) {
      String statsDlpString = deidResult.getDataAsString(DeidResultProc.STATS_DLP + textField);
      log.info("TextDlp:");
      log.info(statsDlpString);

      String statsDeidString = deidResult.getDataAsString(DeidResultProc.STATS_DEID + textField);
      log.info("TextDeid:");
      log.info(statsDeidString);
    }
  }


  @Test
  public void deidRequest() throws IOException {

    DeidResult deidResult = new DeidResult(new String[]{"0001"}, new String[]{text}, new String[]{"TEXT"});

    new DlpTransform(job, "som-rit-phi-starr-miner-dev").dlpDeidRequest(text,"text", deidResult);
    return;
  }


}
