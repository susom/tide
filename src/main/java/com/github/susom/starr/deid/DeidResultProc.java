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
import com.github.susom.starr.deid.anonymizers.AnonymizedItemWithReplacement;
import com.github.susom.starr.deid.anonymizers.AnonymizerProcessor;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeidResultProc extends DoFn<DeidResult,String> {

  private static final Logger log = LoggerFactory.getLogger(DeidResultProc.class);
  public static final String TEXT_ORGINAL = "";

  public static final String STATS_DEID = "FINDING_";
  public static final String STATS_CNT_DEID = "FINDING_CNT_";
  public static final String TEXT_DEID = "TEXT_DEID_";

  private boolean analytics = true;

  private final Counter totalPhiCntStage1 =
      Metrics.counter(DeidResultProc.class, "totalPhiCntStage1");

  private final Counter totalPhiCntStage2 =
      Metrics.counter(DeidResultProc.class, "totalPhiCntStage2");

  private final Counter zeroChangeCntStage1 =
      Metrics.counter(DeidResultProc.class, "zeroChangeCount_Stage1");

  private final Counter zeroChangeCntStage2 =
      Metrics.counter(DeidResultProc.class, "zeroChangeCount_Stage2");

  private final Distribution itemPerTextDistributionStage1 =
      Metrics.distribution(DeidResultProc.class, "ItemPerTextDistribution_stage1");

  private final Distribution itemPerTextDistributionStage2 =
      Metrics.distribution(DeidResultProc.class, "ItemPerTextDistribution_stage2");

  public DeidResultProc(boolean analytics) {
    this.analytics = analytics;
  }

  /**
   * transform action.
   * @param context process context
   * @throws IOException from json reading failure
   */
  @ProcessElement
  public void processElement(ProcessContext context) throws IOException, IllegalAccessException {
    ObjectMapper mapper = new ObjectMapper();

    DeidResult dt = context.element();
    String dtString = mapper.writeValueAsString(dt);
    context.output(DeidTransform.fullResultTag, dtString);

    if (!this.analytics) {
      return;
    }

    for (String textField : dt.getTextFields()) {

      String statsDeidString = dt.getDataAsString(DeidResultProc.STATS_DEID + textField);
      if (statsDeidString != null) {
        Set<String> phiCat = new HashSet<>();
        JsonNode nodes = mapper.readTree(statsDeidString);
        if (nodes.size() == 0) {
          zeroChangeCntStage2.inc();
        }

        int cnt1 = 0;
        int cnt2 = 0;
        for (JsonNode node : nodes) {
          String stat = node.get("type").asText();
          String foundBy = node.get("foundBy").asText();
          if (foundBy.equals("google-dlp")) {
            cnt1++;
          } else {
            cnt2++;
          }
          context.output(DeidTransform.statsPhiTypeTag, stat);
          context.output(DeidTransform.statPhiFoundByTag, foundBy);
        }

        totalPhiCntStage1.inc(cnt1);
        totalPhiCntStage2.inc(cnt2);
        itemPerTextDistributionStage1.update(cnt1);
        itemPerTextDistributionStage2.update(cnt2);
      } else {
        zeroChangeCntStage2.inc();
      }
    }
  }

  /**
   * based on findings from earlier processes, this step actually applies the changes
   * and create deided text.
   * @param items findings
   * @param inputText original input text
   * @return deided text
   */
  public static String applyChange(List<AnonymizedItemWithReplacement> items, String inputText) {

    //log.info(String.format("input:[%s] output:[%s]", inputText, output));
    if (items.size() == 0) {
      return inputText;
    }

    items.sort((a,b) -> {
      int c1 = b.getEnd().compareTo(a.getEnd());
      if (c1 != 0) {
        return c1;
      }

      if (a.getReplacement() == null && b.getReplacement() == null) {
        return 0;
      }

      if (a.getReplacement() != null && b.getReplacement() != null) {
        return b.getReplacement().compareTo(a.getReplacement());
//        if (!a.getFoundBy().equals("google-dlp")) {
//          return -1;
//        } else {
//          return 1;
//        }
      }

      if (a.getReplacement() == null) {
        return 1;
      } else {
        return -1;
      }

    });

    StringBuilder sb = new StringBuilder();
    long lastEnd = Long.MAX_VALUE;
    long lastStart = Long.MAX_VALUE;
    boolean doneWithTheFirst = false;
    for (int i = 0; i < items.size(); i++) {

      AnonymizedItemWithReplacement item = items.get(i);
      if (item.getStart() < 0 || item.getStart() > inputText.length()
          || item.getEnd() < 0 || item.getEnd() > inputText.length()) {
        log.warn("invalid finding s:{} e:{} foundby:{} [orginalTextLen:{}]", item.getStart(), item.getEnd(),item.getFoundBy(), inputText.length());
        continue;
      }

      if (!doneWithTheFirst) {
        //first instance
        sb.insert(0, inputText.substring(Math.toIntExact(item.getEnd())));
        sb.insert(0, item.getReplacement() != null
            ? item.getReplacement() :
            AnonymizerProcessor.REPLACE_WORD);

        lastEnd = item.getEnd();
        lastStart = item.getStart();
        if (i == items.size() - 1) {
          sb.insert(0, inputText.substring(0, Math.toIntExact(item.getStart())));
        }
        doneWithTheFirst = true;
        continue;
      }

      if (item.getEnd() < lastEnd && item.getStart() >= lastStart) {
        //inside of last change, do nothing
      } else if (item.getEnd() < lastEnd && item.getEnd() >= lastStart) {
        //overlap
        //char[] spaces = new char[lastStart.get()-item.getStart()];
        //sb.insert(0,spaces);
        lastStart = item.getStart();
      } else if (item.getEnd() < lastStart) {
        //outside of last change, copy fully
        sb.insert(0,
            inputText.substring(Math.toIntExact(item.getEnd()), Math.toIntExact(lastStart)));
        sb.insert(0,
            item.getReplacement() != null
              ? item.getReplacement() :
                AnonymizerProcessor.REPLACE_WORD);

        lastEnd = item.getEnd();
        lastStart = item.getStart();
      }

      if (i == items.size() - 1) {
        sb.insert(0, inputText.substring(0, Math.toIntExact(item.getStart())));
      }
    }
    return sb.toString();
  }
}
