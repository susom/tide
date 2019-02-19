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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;

public class DeidResultProc extends DoFn<DeidResult,String> {

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
}
