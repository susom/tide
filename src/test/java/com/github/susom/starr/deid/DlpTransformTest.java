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
import com.github.susom.starr.core.integration.gcp.GcpIntegration;
import com.github.susom.starr.deid.anonymizers.AnonymizedItemWithReplacement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import org.apache.beam.runners.direct.DirectOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(PipelineExtension.class)
class DlpTransformTest {



  private static final String BASE64REGEX
      = "(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";
  private static final Logger log = LoggerFactory.getLogger(DlpTransformTest.class);


  private String[] noteJsonText = new String[]{
    "{\"note_id\":\"001_J0\",\"jitter\":0,\"note_text\":\"more tests: date test Jan 01, 2018, ssn: 874-98-5739\"}",
    "{\"note_id\":\"001_J1\",\"jitter\":1,\"note_text\":\"Jose's birth day: 2003-09-18, passport: 56521368, pp2: 56985631 credit card number is 4111111111111111 \"}",
    "{\"note_id\":\"001_J2\",\"jitter\":2,\"note_text\":\"i2b2: Record date: 2088-07-03 \"}",
    "{\"note_id\":\"001_J-3\",\"jitter\":-3,\"note_text\":\"Alex has fever on June 4, 2019\\nTeam 1 Intern Admission Note\\nName: Younger, T Eugene\\nMR#: 6381987\\nAtt: Dr. Gilbert\\nCards: Dr. Ullrich\\nNeuro: Dr. Donovan\\nDate of Admission: 7/2/88 CC: Lightheadedness, vertigo, and presyncopal sx x several episodes \"}"
  };


  private static String[] textLines = new String[]{
    ">>>>>>>>>>>>> more tests: date test Jan 01, 2018, ssn: 874-98-5739",
    "Jose's birth day: 2003-09-18, passport: 56521368, pp2: 56985631 address: Palo Alto, CA",
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


  private static DeidJob job;

  private static DlpTransform transform;

  private Pipeline pipeline;

  /**
   * inject pipeline by PipelineExtension
   * @param _pipeline provide pipeline
   */
  public void setPipeline(Pipeline _pipeline) {
    pipeline = _pipeline;
  }

  @BeforeAll
  static void setUp() throws Exception {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    DeidJobs jobs = mapper.readValue(DlpTransformTest.class.getClassLoader()
      .getResourceAsStream("deid_test_config_dlp.yaml"), DeidJobs.class);

    job = jobs.deidJobs[0];

    try {
      transform = new DlpTransform(job,
        GcpIntegration.getDefaultIntegrationTestProject());
    } catch (IOException e) {
      Assert.fail();
    }

  }



  @Test
  @EnabledIfEnvironmentVariable(named = "GCP_INTEGRATION_TEST_TOKEN", matches = BASE64REGEX)
  void testDlpDeidRequest() {
    List<AnonymizedItemWithReplacement> items = new ArrayList<>();

    for (int i = 0; i < textLines.length; i++) {
      DeidResult deidResult =
        new DeidResult(new String[]{"ID"},
            new String[]{String.format(Locale.ROOT,"note_%d",i)},
        new String[]{"NOTE"});
      transform.dlpDeidRequest(String.format(Locale.ROOT,"note_%d",i), items);
    }

    return;
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "GCP_INTEGRATION_TEST_TOKEN", matches = BASE64REGEX)
  void testDlpInspectRequest() {


    try {

      List<AnonymizedItemWithReplacement> items = new ArrayList<>();

      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

      for (int i = 0; i < textLines.length; i++) {
        DeidResult deidResult = new DeidResult(
          new String[]{"ID"},
          new String[]{String.format(Locale.ROOT,"note_%d",i)},
          new String[]{"NOTE"});

        transform.dlpInspectRequest(textLines[i], items, i);

        for (AnonymizedItemWithReplacement item : items) {
          log.info("finding: word:{} star:{} end:{}",
              item.getWord(),item.getStart(),item.getEnd());
        }
        Assert.assertTrue(items.size() > 0);

      }

    } catch (IOException e) {
      log.error(e.getMessage(),e);
      Assert.fail();
    }
  }


  @Test
  @EnabledIfEnvironmentVariable(named = "GCP_INTEGRATION_TEST_TOKEN", matches = BASE64REGEX)
  void testPipelineWithDlp() {
    final List<String> notes = Arrays.asList(noteJsonText);

    PCollection input = pipeline.apply(Create.of(notes)).setCoder( StringUtf8Coder.of());

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    DeidJobs jobs = null;
    DeidTransform fullTransform = null;
    try {
      jobs = mapper.readValue(this.getClass().getClassLoader()
        .getResourceAsStream("deid_test_config_dlp.yaml"), DeidJobs.class);
      fullTransform = new DeidTransform(jobs.getDeidJobs()[0], GcpIntegration.getDefaultIntegrationTestProject());
    } catch (IOException e) {
      log.error(e.getMessage());
      Assert.fail();
    }


    PCollection<DeidResult> deidResults = fullTransform.expand(input);

    PCollectionTuple result = deidResults.apply("processResult",
      ParDo.of(new DeidResultProc())
        .withOutputTags(DeidTransform.fullResultTag,
          TupleTagList.of(DeidTransform.statsDlpPhiTypeTag)
            .and(DeidTransform.statsPhiTypeTag)
            .and(DeidTransform.statPhiFoundByTag)));

    PCollection<String> cleanText = result.get(DeidTransform.fullResultTag)
      .apply(ParDo.of(new PrintResult()));

      PAssert.that(cleanText)
        .containsInAnyOrder(
          "more tests: date test 10/10/2100, ssn: 999-99-9999",
          "Alex has fever on 06/01/2019\nTeam 1 Intern Admission Note\nName: Younger, T Eugene\nMR#: 6381987\nAtt: Dr. Gilbert\nCards: Dr. Ullrich\nNeuro: Dr. Donovan\nDate of Admission: 06/29/1988 CC: Lightheadedness, vertigo, and presyncopal sx x several episodes ",
          "i2b2: Record date: 07/05/2088 ",
          "Jose's birth day: 09/19/2003, passport: 56521368, pp2: 56985631 credit card number is 999999999999999 "
        );

    pipeline.run();
  }


  private static class PrintResult extends DoFn<String,String> {
    final static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @ProcessElement
    public void processElement(ProcessContext c) throws IOException {
      String line = c.element();
      JsonNode node = mapper.readTree(line);
      log.info(line);
      c.output(node.get("TEXT_DEID_note_text").asText());
    }
  }
}
