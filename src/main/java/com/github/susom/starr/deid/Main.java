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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import org.apache.beam.repackaged.beam_runners_google_cloud_dataflow_java.com.google.common.io.ByteStreams;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.metrics.DistributionResult;
import org.apache.beam.sdk.metrics.MetricName;
import org.apache.beam.sdk.metrics.MetricNameFilter;
import org.apache.beam.sdk.metrics.MetricQueryResults;
import org.apache.beam.sdk.metrics.MetricResult;
import org.apache.beam.sdk.metrics.MetricResults;
import org.apache.beam.sdk.metrics.MetricsFilter;
import org.apache.beam.sdk.metrics.SourceMetrics;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.util.MimeTypes;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deid Pipeline.
 * @author wenchengl
 */
public class Main implements Serializable {
  private static final Logger log = LoggerFactory.getLogger(Main.class);



  public static void main(String[] args) throws IOException {
    new Main().run(args);
  }


  private void run(String[] args) throws IOException {

    DeidOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation().as(DeidOptions.class);
    log.info(options.toString());

    DeidJobs jobs = null;
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {

      File deidConfigFileHd = new File(options.getDeidConfigFile());
      if (deidConfigFileHd.exists()) {
        log.info("reading configuration from file " + deidConfigFileHd.getAbsolutePath());
        jobs = mapper.readValue(deidConfigFileHd, DeidJobs.class);
      } else {
        log.info("reading configuration from " + options.getDeidConfigFile());
        jobs = mapper.readValue(this.getClass().getClassLoader()
          .getResourceAsStream(options.getDeidConfigFile()), DeidJobs.class);
      }
      log.info("received configuration for " + jobs.name);

    } catch (IOException e) {
      log.error("Could not open or parse deid configuration file in class path",e);
      System.exit(1);
    }

    if (options.getTextIdFields() != null || options.getTextIdFields() != null) {
      //override text field mapping
      for (int i = 0; i < jobs.deidJobs.length;i++) {
        if (options.getTextIdFields() != null) {
          jobs.deidJobs[i].textIdFields = options.getTextIdFields();
        }
        if (options.getTextInputFields() != null) {
          jobs.deidJobs[i].textFields = options.getTextInputFields();
        }
      }
    }

    //start the work
    Pipeline p = Pipeline.create(options);
    PCollectionTuple result =
        (options.getInputType().equals(ResourceType.gcp_bq.name())
          ? InputTransforms.BigQueryRowToJson.withBigQueryLink(p,options.getInputResource())
          : p.apply(TextIO.read().from(options.getInputResource())))

        .apply("Deid", new DeidTransform(jobs.deidJobs[0], options.getDlpProject()))
        .apply("processResult",
          ParDo.of(new DeidResultProc())
            .withOutputTags(DeidTransform.fullResultTag,
                TupleTagList.of(DeidTransform.statsDlpTag)
                .and(DeidTransform.statsDeidTag)
                .and(DeidTransform.statCategoryDlpTag)
                .and(DeidTransform.statCategoryDeidTag)));



    if (jobs.deidJobs[0].googleDlpEnabled) {
      result.get(DeidTransform.statsDlpTag)
        .apply("AnalyzeCategoryStatsDlp", new AnalyzeStatsTransform())
        .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
        .apply(TextIO.write().to(options.getOutputResource() + "/DeidPhiStatsDlp"));


      result.get(DeidTransform.statCategoryDlpTag)
        .apply("AnalyzeTextDlp", new AnalyzeStatsTransform())
        .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
        .apply(TextIO.write().to(options.getOutputResource() + "/DeidTextStatsDlp"));
    }

    result.get(DeidTransform.statsDeidTag)
      .apply("AnalyzeGlobalStage2", new AnalyzeStatsTransform())
      .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
      .apply(TextIO.write().to(options.getOutputResource() + "/DeidPhiStatsStage2"));

    result.get(DeidTransform.statCategoryDeidTag)
      .apply("AnalyzeTextStage2", new AnalyzeStatsTransform())
      .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
      .apply(TextIO.write().to(options.getOutputResource() + "/DeidTextStatsStage2"));


    result.get(DeidTransform.fullResultTag)
      .apply(TextIO.write().to(options.getOutputResource() + "/DeidNote"));

    PipelineResult pipelineResult = p.run();
    pipelineResult.waitUntilFinish();

    //MetricName elementsRead = SourceMetrics.elementsRead().getName();

//    MetricResults metricResults = pipelineResult.metrics();
//    MetricQueryResults mqr = metricResults.queryMetrics(MetricsFilter.builder()
//        .addNameFilter(MetricNameFilter.inNamespace(DeidResultProc.class))
//        .addStep("processResult")
//        .build());


//    ResourceId reportResourceId = FileSystems
//        .matchNewResource(options.getOutputResource() + "/job_report.txt", false);
//
//
//    StringBuffer sb = new StringBuffer();
//
//    Iterator<MetricResult<Long>> itC = mqr.getCounters().iterator();
//    while (itC.hasNext()) {
//      MetricResult<Long> m = itC.next();
//      log.info("Counter " + m.getName() + ":" + m.getCommitted());
//      sb.append(("Counter " + m.getName() + ":" + m.getCommitted() + "\n\r"));
//    }
//
//    Iterator<MetricResult<DistributionResult>> itD = mqr.getDistributions().iterator();
//    while (itD.hasNext()) {
//      MetricResult<DistributionResult> r = itD.next();
//      log.info("Distribution " + r.getName() + ":" + r.getCommitted().toString());
//      sb.append(("Distribution " + r.getName() + ":" + r.getCommitted().toString() + "\n\r"));
//    }
//
//
//    try {
//      try (ByteArrayInputStream in = new ByteArrayInputStream(sb.toString().getBytes());
//           ReadableByteChannel readerChannel = Channels.newChannel(in);
//           WritableByteChannel writerChannel
//                = FileSystems.create(reportResourceId, MimeTypes.TEXT)) {
//        ByteStreams.copy(readerChannel, writerChannel);
//      }
//    } catch (IOException e) {
//      log.error(e.getMessage(),e);
//    }


  }


  public interface DeidOptions extends PipelineOptions {


    @Description("Namen of the Deid configuration, the default is deid_test_config.yaml")
    @Default.String("deid_config_clarity.yaml")
    String getDeidConfigFile();

    void setDeidConfigFile(String value);

    @Description("Path of the file to read from")
    @Validation.Required
    String getInputResource();

    void setInputResource(String value);


    @Description("Path of the file to save to")
    @Validation.Required
    String getOutputResource();

    void setOutputResource(String value);

    @Description("type of the input resouce. gcp_gcs | gcp_bq | local")
    @Default.String("gcp_gcs")
    String getInputType();

    void setInputType(String value);


    @Description("The Google project id that DLP API will be called, optional.")
    @Default.String("")
    String getDlpProject();

    void setDlpProject(String value);

    @Description("override text field, optional.")
    @Default.String("")
    String getTextInputFields();

    void setTextInputFields(String value);

    @Description("override text-id field, optional.")
    @Default.String("")
    String getTextIdFields();

    void setTextIdFields(String value);

  }

}
