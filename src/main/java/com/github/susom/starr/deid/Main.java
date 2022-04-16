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
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
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
    //test test
    log.debug("dguan2-testing: starting run from maven repo");
    System.out.println("dguan2-testing: starting run from maven repo");
    DeidOptions options = getOptions(args);

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
      log.error("Could not open or parse deid configuration file in class path", e);
      System.exit(1);
    }

    AnnotatorSpecs annotatorSpecs = null;
    try {
      File annotatorConfigFileHd = new File(options.getAnnotatorConfigFile()); //"annotator_config.yaml"
      if (annotatorConfigFileHd.exists()) {        
        log.info("reading annotator configuration from file : {}", annotatorConfigFileHd.getAbsolutePath());
        annotatorSpecs = mapper.readValue(annotatorConfigFileHd, AnnotatorSpecs.class);       
      }
      else {
        log.info("reading annotator configuration from " + options.getAnnotatorConfigFile());
        annotatorSpecs = mapper.readValue(this.getClass().getClassLoader()
          .getResourceAsStream(options.getAnnotatorConfigFile()), AnnotatorSpecs.class);
      }
    } catch (IOException e) {
      log.error("Could not open or parse annotator configuration file in class path", e);
      System.exit(1);
    }

    if (options.getGoogleDlpEnabled() != null) {
      log.info("overwriting GoogleDlpEnabled property with {}", options.getGoogleDlpEnabled());
      boolean enableDlp = options.getGoogleDlpEnabled().toLowerCase(Locale.ROOT).equals("true");
      for (int i = 0; i < jobs.deidJobs.length;i++) {
        jobs.deidJobs[i].googleDlpEnabled = enableDlp;
      }
    }

    if (options.getTextIdFields() != null) {
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
    PCollection<String> collection = null;

    if (options.getInputType().equals(ResourceType.gcp_bq.name())) {
      collection = InputTransforms.BigQueryRowToJson.withBigQueryLink(p, options.getInputResource());
    }
    else if (options.getInputType().equals(ResourceType.text.name())) {
      //collection = p.apply(Create.of(jsonStrings)).setCoder(StringUtf8Coder.of());

      //for individual note(s), file name will be used as id and contents of the file will be used as note
      options.setTextIdFields(StaticValueProvider.of(TextTag.id.name()));
      options.setTextInputFields(StaticValueProvider.of(TextTag.note.name()));
      /*output source*/
      long TIME_STAMP_MILLIS = Instant.now().toEpochMilli();
      String outputResource = options.getOutputResource() != null ? options.getOutputResource().get() + "/" + TIME_STAMP_MILLIS : null;
      options.setOutputResource(StaticValueProvider.of(outputResource));

      collection = InputTransforms.TextToJson.withPhiAssociation(p, options.getInputResource(), options.getPhiFileName(), options.getPersonFile());
    }
    else {
      collection = p.apply(TextIO.read().from(options.getInputResource()));
    }

    PCollectionTuple result = collection
        .apply("Deid", new DeidTransform(jobs.deidJobs[0], options.getDlpProject()))
        .apply("processResult",
          ParDo.of(new DeidResultProc(jobs.deidJobs[0].analytic))
            .withOutputTags(DeidTransform.fullResultTag,
                TupleTagList.of(DeidTransform.statsDlpPhiTypeTag)
                .and(DeidTransform.statsPhiTypeTag)
                .and(DeidTransform.statPhiFoundByTag)));

    if (jobs.deidJobs[0].analytic) {

      if (jobs.deidJobs[0].googleDlpEnabled) {
        result.get(DeidTransform.statsDlpPhiTypeTag)
            .apply("AnalyzeCategoryStatsDlp", new AnalyzeStatsTransform())
            .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
            .apply(TextIO.write().to(
              NestedValueProvider.of(options.getOutputResource(),
                new AppendSuffixSerializableFunction("/statsDlpPhiType")))
          );
      }

      result.get(DeidTransform.statsPhiTypeTag)
          .apply("AnalyzeGlobalStage2", new AnalyzeStatsTransform())
          .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
          .apply(TextIO.write().to(
            NestedValueProvider.of(options.getOutputResource(),
                new AppendSuffixSerializableFunction("/statsPhiTypeStage2")))
        );

      result.get(DeidTransform.statPhiFoundByTag)
          .apply("AnalyzeFoundbyStats", new AnalyzeStatsTransform())
          .apply(MapElements.via(new ProcessAnalytics.PrintCounts()))
          .apply(TextIO.write().to(
            NestedValueProvider.of(options.getOutputResource(),
                new AppendSuffixSerializableFunction("/statPhiFoundBy")))
        );
    }

    result.get(DeidTransform.fullResultTag)
        .apply(TextIO.write().to(
          NestedValueProvider.of(options.getOutputResource(),
              new AppendSuffixSerializableFunction("/DeidNote")))
      );

    if (options.getInputType().equals(ResourceType.text.name()) && jobs.deidJobs[0].isAnnotatorOutputEnabled()) {
      result.get(DeidTransform.fullResultTag)
        .apply("DeidAnnotatorOutput", new DeidAnnotatorTransform(options.getOutputResource().get() + "/annotator", annotatorSpecs));
    }

    if (options.getInputType().equals(ResourceType.text.name())) {
      result.get(DeidTransform.fullResultTag)
        .apply("DeidIndividualOutput", new DeidOutputTransform(options.getOutputResource().get() + "/individual"));
    }
    
    PipelineResult pipelineResult = p.run();

    if (!isTemplateCreationRun(args)) {
      pipelineResult.waitUntilFinish();
    }
  }

  private static boolean isTemplateCreationRun(String[] args) {
    for (String arg : args) {
      if (arg.contains("--templateLocation")) {
        return true;
      }
    }
    return false;
  }

  private static DeidOptions getOptions(String[] args) throws IOException {
    DeidOptions options = PipelineOptionsFactory.fromArgs(args)
        .withValidation().as(DeidOptions.class);

    if (options.getGcpCredentialsKeyFile().trim().length() > 0) {
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(new FileInputStream(options.getGcpCredentialsKeyFile()))
          .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
      options.setGcpCredential(credentials);
    }

    return options;
  }

  private static class AppendSuffixSerializableFunction
      implements SerializableFunction<String, String> {
    private String suffix;

    public AppendSuffixSerializableFunction(String suffix) {
      this.suffix = suffix;
    }

    @Override
    public String apply(String prefix) {
      return prefix + suffix;
    }
  }

  public interface DeidOptions extends PipelineOptions {
    @Description("Set GCP credentials key file if using DataflowRunner.")
    @Default.String("")
    String getGcpCredentialsKeyFile();

    void setGcpCredentialsKeyFile(String value);

    void setGcpCredential(Credentials value);

    @Description("Namen of the Deid configuration, the default is deid_test_config.yaml")
    @Default.String("deid_config_clarity.yaml")
    String getDeidConfigFile();

    void setDeidConfigFile(String value);

    @Description("Path of the file to read from")
    @Validation.Required
    ValueProvider<String> getInputResource();

    void setInputResource(ValueProvider<String> value);

    @Description("Path of the file to save to")
    @Validation.Required
    ValueProvider<String> getOutputResource();

    void setOutputResource(ValueProvider<String> value);

    @Description("type of the input resouce. gcp_gcs | gcp_bq | local | text")
    @Default.String("gcp_gcs")
    String getInputType();

    void setInputType(String value);

    @Description("The Google project id that DLP API will be called, optional.")
    @Default.String("")
    String getDlpProject();

    void setDlpProject(String value);

    @Description("Turn on/off Google DLP")
    String getGoogleDlpEnabled();

    void setGoogleDlpEnabled(String value);

    @Description("override text field, optional.")
    ValueProvider<String> getTextInputFields();

    void setTextInputFields(ValueProvider<String> value);

    @Description("override text-id field, optional.")
    ValueProvider<String> getTextIdFields();

    void setTextIdFields(ValueProvider<String> value);

    @Description("Path of the PHI file to read from, optional.")
    @Default.String("")
    String getPhiFileName();

    void setPhiFileName(String value);

    @Description("Path of the person file to read from, optional.")
    @Default.String("")
    String getPersonFile();

    void setPersonFile(String value);

    @Description("Namen of the Annotator configuration, Optional")
    @Default.String("annotator_config.yaml")
    String getAnnotatorConfigFile();

    void setAnnotatorConfigFile(String value);
  }

}
