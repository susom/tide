package com.github.susom.starr.deid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.ByteStreams;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.metrics.*;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.util.MimeTypes;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

/**
 * Deid Pipeline
 * @author wenchengl
 */
public class Main implements Serializable {
  private static final Logger log = LoggerFactory.getLogger(Main.class);



  public static void main(String[] args) throws IOException { new Main().run(args);}


  public void run(String[] args) throws IOException {

    DeidOptions options = PipelineOptionsFactory.fromArgs(args).withValidation().as(DeidOptions.class);
    log.info(options.toString());

    DeidJobs jobs = null;
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {

      File deidConfigFileHd = new File(options.getDeidConfigFile());
      if(deidConfigFileHd.exists()){
        log.info("reading configuration from file "+ deidConfigFileHd.getAbsolutePath());
        jobs = mapper.readValue(deidConfigFileHd, DeidJobs.class);
      }else{
        log.info("reading configuration from "+options.getDeidConfigFile());
        jobs = mapper.readValue(this.getClass().getClassLoader()
          .getResourceAsStream(options.getDeidConfigFile()), DeidJobs.class);
      }



      log.info("received configuration for "+jobs.name);
    } catch (IOException e) {
      log.error("Could not open or parse deid configuration file in class path",e);
      System.exit(1);
    }

    if(options.getTextIdField()!=null || options.getTextIdField()!=null){
      //override text field mapping
      for(int i=0;i<jobs.deid_jobs.length;i++){
        if(options.getTextIdField()!=null){
          jobs.deid_jobs[i].text_id_field = options.getTextIdField();
        }
        if(options.getTextInputField()!=null){
          jobs.deid_jobs[i].text_field = options.getTextInputField();
        }

      }
    }



    //start the work
    Pipeline p = Pipeline.create(options);
    PCollectionTuple result =
      (options.getInputType().equals(ResourceType.gcp_bq.name())?
        InputTransforms.BigQueryRowToJson.withBigQueryLink(p,options.getInputResource())
        :p.apply(TextIO.read().from(options.getInputResource())))

      .apply("Deid", new DeidTransform(jobs.deid_jobs[0], options.getDlpProject()))
      .apply("processResult",
        ParDo.of(new DeidResult.DeidResultProc())
          .withOutputTags(DeidTransform.fullResultTag, TupleTagList.of(DeidTransform.statGlobalStage1Tag).and(DeidTransform.statGlobalStage2Tag).and(DeidTransform.statPerTextStage1Tag).and(DeidTransform.statPerTextStage2Tag)));



    if(jobs.deid_jobs[0].googleDlpEnabled){
      result.get(DeidTransform.statGlobalStage1Tag)
        .apply("AnalyzeGlobalStage1", new DeidResult.AnalyzeStatsByPhi())
        .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
        .apply(TextIO.write().to(options.getOutputResource()+"/DeidPhiStatsStage1"));


      result.get(DeidTransform.statPerTextStage1Tag)
        .apply("AnalyzeTextStage1", new DeidResult.AnalyzeStatsByPhi())
        .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
        .apply(TextIO.write().to(options.getOutputResource()+"/DeidTextStatsStage1"));
    }

    result.get(DeidTransform.statGlobalStage2Tag)
      .apply("AnalyzeGlobalStage1", new DeidResult.AnalyzeStatsByPhi())
      .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
      .apply(TextIO.write().to(options.getOutputResource()+"/DeidPhiStatsStage2"));

    result.get(DeidTransform.statPerTextStage2Tag)
      .apply("AnalyzeTextStage2", new DeidResult.AnalyzeStatsByPhi())
      .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
      .apply(TextIO.write().to(options.getOutputResource()+"/DeidTextStatsStage2"));


    result.get(DeidTransform.fullResultTag)
      .apply(TextIO.write().to(options.getOutputResource()+"/DeidNote"));

    PipelineResult pResult = p.run();
    pResult.waitUntilFinish();

    MetricName elementsRead = SourceMetrics.elementsRead().getName();

    MetricResults mResults = pResult.metrics();
    MetricQueryResults mqr = mResults.queryMetrics(MetricsFilter.builder()
      .addNameFilter(MetricNameFilter.inNamespace(DeidResult.DeidResultProc.class))
      .addStep("processResult")
      .build());


    ResourceId reportResourceId = FileSystems
      .matchNewResource(options.getOutputResource()+"/job_report.txt", false);


    StringBuffer sb = new StringBuffer();

    Iterator<MetricResult<Long>> itC = mqr.getCounters().iterator();
    while (itC.hasNext()){
      MetricResult<Long> m = itC.next();
      log.info("Counter "+m.getName()+":"+m.getCommitted());
      sb.append(("Counter "+m.getName()+":"+m.getCommitted()+"\n\r"));
    }

    Iterator<MetricResult<DistributionResult>> itD = mqr.getDistributions().iterator();
    while (itD.hasNext()){
      MetricResult<DistributionResult> r = itD.next();
      log.info("Distribution "+r.getName()+":"+r.getCommitted().toString());
      sb.append(("Distribution "+r.getName()+":"+r.getCommitted().toString()+"\n\r"));
    }


    try {
      try (ByteArrayInputStream in = new ByteArrayInputStream(sb.toString().getBytes());
           ReadableByteChannel readerChannel = Channels.newChannel(in);
           WritableByteChannel writerChannel = FileSystems.create(reportResourceId, MimeTypes.TEXT)) {

        ByteStreams.copy(readerChannel, writerChannel);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }


  }


  public interface DeidOptions extends PipelineOptions {


    @Description("Namen of the Deid configuration in the classpath, the default setting is deid_test_config.yaml")
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
    String getTextInputField();
    void setTextInputField(String value);

    @Description("override text-id field, optional.")
    @Default.String("")
    String getTextIdField();
    void setTextIdField(String value);

  }

}
