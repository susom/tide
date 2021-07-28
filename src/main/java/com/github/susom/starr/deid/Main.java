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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider;
import org.apache.beam.sdk.options.ValueProvider.StaticValueProvider;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTagList;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.google.gson.Gson;

/**
 * Deid Pipeline.
 * @author wenchengl
 */

@SuppressWarnings("java:S125")
public class Main implements Serializable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
  private static final long TIME_STAMP_MILLIS = Instant.now().toEpochMilli();

  public static void main(String[] args) throws IOException {
    new Main().run(args);
  }

  private void run(String[] args) throws IOException {

    DeidOptions options = getOptions(args);

    LOGGER.debug("options : {}", options);

    DeidJobs jobs = null;
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {

      File deidConfigFileHd = new File(options.getDeidConfigFile());
      if (deidConfigFileHd.exists()) {
        LOGGER.info("reading configuration from file : {}", deidConfigFileHd.getAbsolutePath());
        jobs = mapper.readValue(deidConfigFileHd, DeidJobs.class);
      } else {
        LOGGER.info("reading configuration from {}", options.getDeidConfigFile());
        jobs = mapper.readValue(this.getClass().getClassLoader()
          .getResourceAsStream(options.getDeidConfigFile()), DeidJobs.class);
      }
      LOGGER.info("received configuration for {}",  jobs.name);

    } catch (IOException e) {
      LOGGER.error("Could not open or parse deid configuration file in class path due to {}", e.getMessage());
      System.exit(1);
    }
/*
    if (options.getGoogleDlpEnabled() != null) {
      log.info("overwriting GoogleDlpEnabled property with {}", options.getGoogleDlpEnabled());
      boolean enableDlp = options.getGoogleDlpEnabled().toLowerCase(Locale.ROOT).equals("true");
      for (int i = 0; i < jobs.deidJobs.length;i++) {
        jobs.deidJobs[i].googleDlpEnabled = enableDlp;
      }
    }
*/
    List<String> jsonStrings = new ArrayList<>();
    List<Path> fileResult = new ArrayList<>();
    String inputType = options.getInputType();

    if (null != inputType && inputType.equals(ResourceType.txt.name())) {

      options.setTextIdFields(StaticValueProvider.of("id"));
      options.setTextInputFields(StaticValueProvider.of("note"));

      /* This original output path is required to create the latest.txt file after completion of pipeline*/
      options.setOrignalOutputPath(options.getOutputResource().get());

      /*Input source of notes, passed in the args*/
      String inputResource = options.getInputResource() != null ? options.getInputResource().get() : null;
      if (null == inputResource) {
        LOGGER.error("inputResource is required for inputType of txt");
        System.exit(1);
      }
      File inputFile = new File(inputResource);
      Path path = Paths.get(inputFile.getAbsolutePath());

      /*PHI data*/
      String phiResource = options.getPhiFileName().get();
      List<Map<String, String>> phiData = getPhiData(phiResource);

      /* form the json strings */
      Gson gson = new Gson();
      if (inputFile.isFile()) {
        Map<String, String> phi = getPhiDataCorresspondingToNote(phiData, inputFile.getName());
        jsonStrings.add(processFile(gson, path, phi));
      }
      else if (inputFile.isDirectory()) {
        try (Stream<Path> walk = Files.walk(path)) {
          fileResult = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        fileResult.parallelStream().forEach(file -> {
          Map<String, String> phi = getPhiDataCorresspondingToNote(phiData, inputFile.getName());
          jsonStrings.add(processFile(gson, file, phi));
        });
      }

      /*output source*/
      String outputResource = options.getOutputResource() != null ? options.getOutputResource().get() + "/" + TIME_STAMP_MILLIS : null;
      options.setOutputResource(StaticValueProvider.of(outputResource));

    } else {

      //if inputType is BQ
      //doBQWork(options, jobs);
    }

    /* update text field options*/
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
    doPipelineWork(args, jobs, options, jsonStrings, inputType, fileResult);
  }

  private void doPipelineWork(String[] args, DeidJobs jobs, DeidOptions options, List<String> jsonStrings,
          String inputType, List<Path> fileResult) {

    try {
    /*start the pipeline work */
    Pipeline p = Pipeline.create(options);
    PCollectionTuple result =
        (options.getInputType().equals(ResourceType.gcp_bq.name())
          ? InputTransforms.BigQueryRowToJson.withBigQueryLink(p,options.getInputResource())
            : p.apply(Create.of(jsonStrings)).setCoder(StringUtf8Coder.of()))

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

    PipelineResult pipelineResult = p.run();

    if (!isTemplateCreationRun(args)) {
      pipelineResult.waitUntilFinish();
    }
      if (null != inputType && inputType.equals(ResourceType.txt.name())) {
        String outputResource = options.getOutputResource().get();
        File outputFile = new File(outputResource);
        List<InputOutputObject> lstTextOutput = new ArrayList<>();

        if (outputFile.isDirectory()) {
          try (Stream<Path> walk = Files.walk(Paths.get(outputFile.getAbsolutePath()))) {
            fileResult = walk.filter(Files::isRegularFile).collect(Collectors.toList());
          }
          fileResult.parallelStream().forEach(file -> {
            try (FileInputStream fstream = new FileInputStream(file.toString())){
              InputStreamReader inputStreamReader = new InputStreamReader(fstream, "UTF-8");
              lstTextOutput.addAll(tranformOutput(inputStreamReader));
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            } catch (IOException e1) {
              e1.printStackTrace();
            }
          });
        }
        lstTextOutput.removeIf(Objects::isNull);
        lstTextOutput.parallelStream().forEach(f -> {
          File file = new File(outputFile.getAbsolutePath() + File.separator + f.getId());
          try {

            FileWriter fileWriter = new FileWriter(file);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(f.getNote());
            printWriter.close();
            //Add latest folder's timestamp in latest.txt file
            printWriter = new PrintWriter(options.getOrignalOutputPath() + "/latest.txt");
            printWriter.print("");
            printWriter.print(TIME_STAMP_MILLIS);
            printWriter.close();

          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      }
    } catch(IOException e){
      LOGGER.debug("Error reading the input file resource {}", e.getMessage());
    }
  }

  private String processFile(Gson gson, Path path, Map<String, String> phi) {
    try {
      try (FileInputStream fstream = new FileInputStream(path.toString())) {
        InputStreamReader inputStreamReader = new InputStreamReader(fstream, StandardCharsets.UTF_8);
        return populateList(gson, path, inputStreamReader, phi);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private String populateList(Gson gson, Path path, InputStreamReader is, Map<String, String> phi) {

    String jsonInString = gson.toJson(new InputOutputObject(path.getFileName().toString(), new BufferedReader(is)
        .lines().collect(Collectors.joining(System.lineSeparator())))); //"\n"

    if (phi != null) {
      ObjectMapper mapper = new ObjectMapper();
      JSONObject json = new JSONObject();
      try {
        Map<String, String> map1 = mapper.readValue(jsonInString, Map.class);
        phi.putAll(map1);
        json = new JSONObject(phi);
      } catch (IOException e) {
        LOGGER.debug("populateList failed due to {} , with message {}", e.getCause(), e.getMessage());
        e.printStackTrace();
      }
      return json.toString();
    }

    return jsonInString;
  }

  //output format requirement
  //{"text": "non deid data", "labels": [ [1097, 1104, "ner-PERSON"], [312, 319, "ner-PERSON"], [14, 22, "phi_date"]  ], "id":"note_1.txt"}

  @SuppressWarnings("unchecked")
  private List<InputOutputObject> tranformOutput(InputStreamReader is) {

    List<InputOutputObject> textOutput = new ArrayList<>();
    String collect = new BufferedReader(is).lines().collect(Collectors.joining(System.lineSeparator())); //"\n"
    List<String> split =  Arrays.asList(collect.split(System.lineSeparator())); //"\\r?\\n"

    split.stream().forEach(sp -> {
      LOGGER.info("sp %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% {}", sp);
      try {
        Map<String, String> response = new ObjectMapper().readValue(sp, HashMap.class);

        if (response.containsKey("TEXT_DEID_note")) {

          String deidData = response.get("TEXT_DEID_note");

          if (null != deidData) {
            LOGGER.info("deidData %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% {}", deidData);
            InputOutputObject obj = new InputOutputObject(response.get("id"), deidData);
            textOutput.add(obj);
            LOGGER.info("textOutput %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% {}", textOutput.size());
          }
        }
      } catch (JsonParseException e) {
        LOGGER.warn("JsonParseException caused due to {} , with message {}", e.getCause(), e.getMessage());
        e.printStackTrace();
      } catch (JsonMappingException e) {
        LOGGER.warn("JsonMappingException caused due to {} , with message {}", e.getCause(), e.getMessage());
        e.printStackTrace();
      } catch (IOException e) {
        LOGGER.warn("IOException caused due to {} , with message {}", e.getCause(), e.getMessage());
        e.printStackTrace();
      }
      //InputOutputObject fromJson = gson.fromJson(sp, InputOutputObject.class);
      //textOutput.add(fromJson);
    });
    return textOutput;

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
    /*
      Tide Open source change - following is temporarily commented

    if (options.getGcpCredentialsKeyFile().trim().length() > 0) {
      GoogleCredentials credentials =
          GoogleCredentials.fromStream(new FileInputStream(options.getGcpCredentialsKeyFile()))
          .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
      options.setGcpCredential(credentials);
    }
    */

    return options;
  }

  private  Map<String, String> getPhiDataCorresspondingToNote(List<Map<String, String>> phiData, String fileName){

    /*TODO:: instead of file name, the match should be based on id of the note*/
    return phiData.stream().filter(phiMap -> phiMap.get("id").equals(fileName)).findFirst().orElse(null);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, String>> getPhiData(String phiResource){

    /*
      Read PHI file -- ID column must be present in the PHI file.
      ID column corresponds to the name of note file.
    */
    List<Map<String, String>> phiData = new ArrayList<>();
    try{
      try (
        FileReader reader = new FileReader(phiResource);
        BufferedReader bufferedReader = new BufferedReader(reader);
      )
      {
        String currentLine;
        while((currentLine = bufferedReader.readLine()) != null) {
            Map<String, String> response = new ObjectMapper().readValue(currentLine, HashMap.class);
            phiData.add(response);
        }
      }
    }catch(IOException e){
      LOGGER.debug("No phi source found at path {}", phiResource);
    }
    return phiData;
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

   /*
      private void doBQWork(DeidOptions options, DeidJobs jobs){
        if (options.getGoogleDlpEnabled() != null) {
          LOGGER.info("overwriting GoogleDlpEnabled property with {}", options.getGoogleDlpEnabled());
          boolean enableDlp = options.getGoogleDlpEnabled().toLowerCase(Locale.ROOT).equals("true");
          for (int i = 0; i < jobs.deidJobs.length;i++) {
            jobs.deidJobs[i].googleDlpEnabled = enableDlp;
          }
        }
      }
  */
}
