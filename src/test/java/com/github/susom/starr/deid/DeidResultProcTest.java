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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.susom.starr.deid.anonymizers.AnonymizedItemWithReplacement;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeidResultProcTest {

  private static final Logger log = LoggerFactory.getLogger(DeidResultProc.class);

  @Rule
  public final transient TestPipeline pipeline = TestPipeline.create();

  @Test
  public void testApplyChange() {
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

  @Test
  public void testSurrogate() throws IOException {
    String[] noteTexts = new String[]{
      "{\"note_id\":\"001\",\"EMP_NAME\":\"Alex\",\"note_text\":\"PATIENT Account number 898 , Medical Rec #:  8235455. Alex has fever on June 4, 2019\"}",
      "{\"note_id\":\"002\",\"EMP_NAME\":\"Tom\",\"note_text\":\"Tom visited on 10/18/2018 2:02 PM\"}",
      //Testing whitelist: Huntington exists in UMLS dictionary, we should let it pass through
      "{\"note_id\":\"003\",\"EMP_NAME\":\"Crohn\",\"note_text\":\"Dr. Huntington is a disease. Crohn should be removed. \"}"
    };
    Set<String> notExpecting = new HashSet<>(Arrays.asList(
      "Alex has fever on June 4, 2019",
      "Tom visited on 10/18/2018 2:02 PM",
      "Dr. Huntington is a disease. Crohn should be removed. "));

    final List<String> notes = Arrays.asList(noteTexts);

    PCollection input = pipeline.apply(Create.of(notes)).setCoder( StringUtf8Coder.of());

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
      .getResourceAsStream("deid_test_config.yaml"), DeidJobs.class);

    DeidTransform transform = new DeidTransform(jobs.getDeidJobs()[0], null);

    PCollection<DeidResult> deidResults = transform.expand(input);

    PCollectionTuple result = deidResults.apply("processResult",
      ParDo.of(new DeidResultProc(true))
        .withOutputTags(DeidTransform.fullResultTag,
          TupleTagList.of(DeidTransform.statsDlpPhiTypeTag)
            .and(DeidTransform.statsPhiTypeTag)
            .and(DeidTransform.statPhiFoundByTag)));

    PCollection<String> cleanText = result.get(DeidTransform.fullResultTag)
      .apply(ParDo.of(new PrintResult()));

    PAssert.that(cleanText).satisfies(it ->{
      for (String value : it) {
        Assert.assertFalse(notExpecting.contains(value));
      }
      return null;
    });

    pipeline.run();

  }

  @Test
  public void testDeidFn() throws IOException {

    String[] noteTexts = new String[]{
      "{\"note_id\":\"001_J0\",\"jitter\":0,\"note_text\":\"Alex has fever on June 4, 2019\"}",
      "{\"note_id\":\"001_J1\",\"jitter\":1,\"note_text\":\"Alex has fever on June 4, 2019\"}",
      "{\"note_id\":\"001_J2\",\"jitter\":2,\"note_text\":\"Alex has fever on June 4, 2019\"}",
      "{\"note_id\":\"001_J-3\",\"jitter\":-3,\"note_text\":\"Alex has fever on June 4, 2019\"}",
      "{\"note_id\":\"002_J1\",\"jitter\":1,\"note_text\":\"Bob's birthday is 06/04/1980\"}",
      "{\"note_id\":\"003_J2\",\"jitter\":2,\"note_text\":\"Bob's birthday is Jun 04,1980\"}",
      "{\"note_id\":\"004_J3\",\"jitter\":3,\"note_text\":\"Bob's birthday is June 4,     1980\"}",
      "{\"note_id\":\"005_J4\",\"jitter\":4,\"note_text\":\"Bob's birthday is June 04 1980\"}",
      "{\"note_id\":\"006_JNULL\",\"jitter\":null,\"note_text\":\"Bob's birthday is Jun 4, 1980\"}",
      "{\"note_id\":\"007_J1\",\"jitter\":1,\"note_text\":\"Tom visited on 10/18/2018 2:02 PM\"}"
    };

    final List<String> notes = Arrays.asList(noteTexts);

    PCollection input = pipeline.apply(Create.of(notes)).setCoder( StringUtf8Coder.of());

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
      .getResourceAsStream("deid_test_config_no_ner.yaml"), DeidJobs.class);

    DeidTransform transform = new DeidTransform(jobs.getDeidJobs()[0], null);

    PCollection<DeidResult> deidResults = transform.expand(input);

    PCollectionTuple result = deidResults.apply("processResult",
      ParDo.of(new DeidResultProc(true))
        .withOutputTags(DeidTransform.fullResultTag,
          TupleTagList.of(DeidTransform.statsDlpPhiTypeTag)
            .and(DeidTransform.statsPhiTypeTag)
            .and(DeidTransform.statPhiFoundByTag)));

    PCollection<String> cleanText = result.get(DeidTransform.fullResultTag)
        .apply(ParDo.of(new PrintResult()));

    PAssert.that(cleanText)
      .containsInAnyOrder(
        "Bob's birthday is 06/05/1980",
        "Bob's birthday is 06/06/1980",
        "Bob's birthday is 06/07/1980",
        "Bob's birthday is 06/08/1980",
        "Alex has fever on 06/05/2019",
        "Alex has fever on 06/06/2019",
        "Alex has fever on 06/01/2019",
        "Alex has fever on [DATE_TESTING]",//jitter is zero
        "Bob's birthday is [DATE_TESTING]",//jitter is null
        "Tom visited on 10/19/2018"
      );

    pipeline.run();
  }


  @Test
  public void testNerReplaceDeidFn() throws IOException {

    String[] noteTexts = new String[]{
        "{\"note_id\":\"001_J0\",\"jitter\":0,\"note_text\":\"Alex has fever on June 4, 2019\"}",
        "{\"note_id\":\"007_J1\",\"jitter\":1,\"note_text\":\"Tom visited on 10/18/2018 2:02 PM\"}"
    };

    final List<String> notes = Arrays.asList(noteTexts);

    PCollection input = pipeline.apply(Create.of(notes)).setCoder( StringUtf8Coder.of());

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
        .getResourceAsStream("deid_test_config_with_ner.yaml"), DeidJobs.class);

    DeidTransform transform = new DeidTransform(jobs.getDeidJobs()[0], null);

    PCollection<DeidResult> deidResults = transform.expand(input);

    PCollectionTuple result = deidResults.apply("processResult",
        ParDo.of(new DeidResultProc(true))
            .withOutputTags(DeidTransform.fullResultTag,
                TupleTagList.of(DeidTransform.statsDlpPhiTypeTag)
                    .and(DeidTransform.statsPhiTypeTag)
                    .and(DeidTransform.statPhiFoundByTag)));

    PCollection<String> cleanText = result.get(DeidTransform.fullResultTag)
        .apply(ParDo.of(new PrintResult()));

    PAssert.that(cleanText)
        .containsInAnyOrder(
            "[NAME] visited on 10/18/2018 2:02 PM",
            "[NAME] has fever on June 4, 2019"
        );

    pipeline.run();
  }

  @Test
  public void testTokenArrayDeidFn() throws IOException {

    String[] noteTexts = new String[]{
      "{\"note_id\":\"001_J0\",\"phi_a\":\"123-45-6789 22-334-5555, 4785-9876\",\"phi_w\":\"SHC-555-66-7777 888-99-0000,uuuoooppp\",\"note_text\":\"Alex phone number is 123-6789-45 (should be removed) and SHC 555-66-7777 (should be removed) and 66-555-7777 (should not be removed)\"}"
    };

    final List<String> notes = Arrays.asList(noteTexts);

    PCollection input = pipeline.apply(Create.of(notes)).setCoder( StringUtf8Coder.of());

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    DeidJobs jobs = mapper.readValue(this.getClass().getClassLoader()
      .getResourceAsStream("deid_test_config_tokens.yaml"), DeidJobs.class);

    DeidTransform transform = new DeidTransform(jobs.getDeidJobs()[0], null);

    PCollection<DeidResult> deidResults = transform.expand(input);

    PCollectionTuple result = deidResults.apply("processResult",
      ParDo.of(new DeidResultProc(true))
        .withOutputTags(DeidTransform.fullResultTag,
          TupleTagList.of(DeidTransform.statsDlpPhiTypeTag)
            .and(DeidTransform.statsPhiTypeTag)
            .and(DeidTransform.statPhiFoundByTag)));

    PCollection<String> cleanText = result.get(DeidTransform.fullResultTag)
      .apply(ParDo.of(new PrintResult()));

    PAssert.that(cleanText)
      .containsInAnyOrder(
        "Alex phone number is [removed]-[removed]-[removed] (should be removed) and 99999999 (should be removed) and 66-555-7777 (should not be removed)"
      );

    pipeline.run();
  }

  private static class PrintResult extends   DoFn<String,String> {
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
