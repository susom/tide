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
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
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

  public static final String STATS_DLP = "STATS_DLP_";
  public static final String STATS_CNT_DLP = "STATSCNT_DLP_";
  public static final String TEXT_DLP = "TEXT_DLP_";

  public static final String STATS_DEID = "STATS_DEID_";
  public static final String STATS_CNT_DEID = "STATSCNT_DEID_";
  public static final String TEXT_DEID = "TEXT_DEID_";

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

  public DeidResultProc() {
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

    for (String textField : dt.getTextFields()) {
      String statsDlpString = dt.getDataAsString(DeidResultProc.STATS_DLP + textField);
      if (statsDlpString != null) {
        JsonNode nodes = mapper.readTree(statsDlpString);
        if (nodes.size() == 0) {
          zeroChangeCntStage1.inc();
        }

        totalPhiCntStage1.inc(nodes.size());
        itemPerTextDistributionStage1.update(nodes.size());
        Set<String> phiCat = new HashSet<>();
        for (JsonNode node : nodes) {
          String stat = node.get("type").asText();

          context.output(DeidTransform.statsDlpTag, stat);
          phiCat.add(stat);
        }

        phiCat.forEach(s -> context.output(DeidTransform.statCategoryDlpTag, s));
      } else {
        zeroChangeCntStage1.inc();
      }

      String statsDeidString = dt.getDataAsString(DeidResultProc.STATS_DEID + textField);
      if (statsDeidString != null) {
        Set<String> phiCat = new HashSet<>();
        JsonNode nodes = mapper.readTree(statsDeidString);
        if (nodes.size() == 0) {
          zeroChangeCntStage2.inc();
        }
        totalPhiCntStage2.inc(nodes.size());
        itemPerTextDistributionStage2.update(nodes.size());

        for (JsonNode node : nodes) {
          String stat = node.get("type").asText();
          context.output(DeidTransform.statsDeidTag, stat);
          phiCat.add(stat);
        }

        phiCat.forEach(s -> context.output(DeidTransform.statCategoryDeidTag, s));

      } else {
        zeroChangeCntStage2.inc();
      }
    }
  }

  public static String applyChange(List<AnonymizedItemWithReplacement> items, String inputText) {

    //log.info(String.format("input:[%s] output:[%s]", inputText, output));
    if (items.size() == 0) {
      return inputText;
    }

    items.sort((a,b) -> b.getEnd().compareTo(a.getEnd()));
    StringBuffer sb = new StringBuffer();
    final AtomicInteger lastEnd = new AtomicInteger(Integer.MAX_VALUE);
    final AtomicInteger lastStart = new AtomicInteger(Integer.MAX_VALUE);
    for (int i = 0; i < items.size(); i++) {

      AnonymizedItemWithReplacement item = items.get(i);
//      System.out.println(String.format("~~~~[%s]=>[%s] at [%s:%s]", item.getWord(), item.getReplacement(), item.getStart(), item.getEnd()));

      if (i == 0) {
        //first instance
        sb.insert(0, inputText.substring(item.getEnd()));
        sb.insert(0, item.getReplacement());
//        System.out.println("<<<<<" + inputText);
//        System.out.println(">>>>>" + sb.toString());

        lastEnd.set(item.getEnd());
        lastStart.set(item.getStart());
        if (i == items.size() - 1) {
          sb.insert(0, inputText.substring(0, item.getStart()));
        }
        continue;
      }

      if (item.getEnd() < lastEnd.get() && item.getStart() >= lastEnd.get()) {
        //inside of last change, do nothing
      } else if (item.getEnd() < lastEnd.get() && item.getEnd() >= lastStart.get()) {
        //overlap
//        char[] spaces = new char[lastStart.get()-item.getStart()];
//        sb.insert(0,spaces);
        lastStart.set(item.getStart());
      } else if (item.getEnd() < lastStart.get()) {
        //outside of last change, copy fully
        sb.insert(0, inputText.substring(item.getEnd(), lastStart.get()));
        sb.insert(0, item.getReplacement());
//        System.out.println("<<<<<" + inputText);
//        System.out.println(">>>>>" + sb.toString());

        lastEnd.set(item.getEnd());
        lastStart.set(item.getStart());
      }

      if (i == items.size() - 1) {
        sb.insert(0, inputText.substring(0, item.getStart()));
      }
    }
    return sb.toString();
  }
}
