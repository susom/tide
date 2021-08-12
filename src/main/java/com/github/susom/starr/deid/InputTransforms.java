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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.bigquery.model.TableSchema;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.avro.generic.GenericRecord;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.SchemaAndRecord;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.nio.file.Files;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.beam.sdk.transforms.Create;

/**
 * generalize input format for BigQuery input.
 * TODO use more efficient coder
 *
 * @author wenchengl
 */

public class InputTransforms {

  private static final Logger log = LoggerFactory.getLogger(InputTransforms.class);

  private static final SerializableFunction<SchemaAndRecord, String> bigQueryRowToJsonFn =
      new SerializableFunction<SchemaAndRecord, String>() {

      final ObjectMapper mapper = new ObjectMapper();

      @Override
      public String apply(SchemaAndRecord schemaAndRecord) {
        ObjectNode simpleNode = mapper.createObjectNode();
        GenericRecord gr = schemaAndRecord.getRecord();
        TableSchema ts = schemaAndRecord.getTableSchema();

        ts.getFields().forEach(f -> {

          String key = f.getName();
          Object value = gr.get(f.getName());
          switch (f.getType().toUpperCase(Locale.ROOT)) {

            case "STRING":
              simpleNode.put(key, value != null ? value.toString() : null);
              break;
            case "BYTES":
              if (key != null && value != null) {
                simpleNode.put(key, value != null ? (byte[])value : null);
              }
              break;
            case "INTEGER":
            case "INT64":
              if (key != null && value != null) {
                simpleNode.put(key, value != null ? (Long) value : null);
                //TODO check why BigQueryIO read this as Long
              }
              break;

            case "FLOAT":
            case "FLOAT64":
              if (key != null && value != null) {
                simpleNode.put(key, value != null ? (Long) value : null);
              }
              break;
            case "TIMESTAMP":
            case "DATE":
              if (key != null && value != null) {
                simpleNode.put(key, value != null ? (Long)value : null);
              }
              break;
            case "BOOLEAN":
            case "BOOL":
              if (key != null && value != null) {
                simpleNode.put(key, value != null ? (Boolean)value : null);
              }
              break;

            default:
              break;
          }

        });

        try {
          return mapper.writeValueAsString(simpleNode);
        } catch (JsonProcessingException e) {
          log.error(e.getMessage(),e);
          return null;
        }
      }
    };

  static class BigQueryRowToJson {

    public static PCollection<String>  withBigQueryLink(
        Pipeline pipeline, ValueProvider<String> resourceLink) {
      return pipeline.apply(
        BigQueryIO
          .read(bigQueryRowToJsonFn)
          .withMethod(Method.DIRECT_READ)
          .withCoder(StringUtf8Coder.of())
          .from(resourceLink)); //projectId:dataSet.table
    }
  }

  static class TextToJson {
    public static PCollection<String> withPhiAssociation(Pipeline pipeline, ValueProvider<String> resourceLink, String phiResource, String personFileSource) throws IOException {
      List<String> jsonStrings = new ArrayList<>();

      /*Input source of notes, passed in the args*/
      String inputResource = resourceLink != null ? resourceLink.get() : null;
      if (null == inputResource) {
        log.error("inputResource is required for inputType of text");
        System.exit(1);
      }
      File inputFileOrDirectory = new File(inputResource);
      Path path = Paths.get(inputFileOrDirectory.getAbsolutePath());

      /*PHI data*/
      final List<Map<String, String>> phiForPerson = readCsv(phiResource);

      final List<Map<String, String>> personToNote = readCsv(personFileSource);

      /* form the json strings */
      if (inputFileOrDirectory.isFile()) {
        jsonStrings.add(processFile(path, getPhiDataCorresspondingToNote(personToNote, phiForPerson, inputFileOrDirectory.getName())));
      }
      else if (inputFileOrDirectory.isDirectory()) {
        List<Path> fileResult = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(path)) {
          fileResult = walk.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        fileResult.parallelStream().sequential().forEach(file -> {
          jsonStrings.add(processFile(file, getPhiDataCorresspondingToNote(personToNote, phiForPerson, file.getFileName().toString())));
        });
      }
      return pipeline.apply(Create.of(jsonStrings)).setCoder(StringUtf8Coder.of());
    }

    private static List<Map<String, String>> readCsv(String inputResource){
      if (inputResource != null)
      {
        File input = new File(inputResource);
        try {
          CsvSchema csvSchema = CsvSchema.emptySchema().withHeader();
          MappingIterator<Map<String, String>> mappingIterator = new CsvMapper().readerFor(HashMap.class).with(csvSchema).readValues(input);
          List<Map<String, String>> data = mappingIterator.readAll();
          return data;
        } catch(IOException e){
          log.debug("Could not read inputResource at path {}", inputResource);
        }
      }
      return null;
    }

    private static Map<String, String> getPhiDataCorresspondingToNote(List<Map<String, String>> noteToPerson, List<Map<String, String>> phiForPerson, String fileName){
      Map<String, String> person = noteToPerson.stream().filter(phiMap -> phiMap.get("note_id").equals(fileName)).findFirst().orElse(null);
      if (person != null) {
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~person.get-person_id=" + person.get("person_id")); 
        //TextTag.id.name()
        Map<String, String> data = phiForPerson.stream().filter(phiMap -> phiMap.get("person_id").equals(person.get("person_id"))).findFirst().orElse(null);
        if (data != null) {
          for ( String key : data.keySet() ) {
            System.out.println( key );
          }
          System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~filename=" + fileName 
          + ", person-personid=" + person.get("person_id") 
          + ", person-noteid=" + person.get("note_id") 
          //+ ", data-pat_name=" + data.get("pat_name")
          //+ ", data-id=" + data.get("id")
          //+ ", data-jitter=" + data.get("JITTER")
          );
        }
        else {
          System.out.println("data is null for " + fileName);
        }
        return data;
      }
      else
        System.out.println("person is null for " + fileName);
      return null;
    }

    private static String processFile(Path path, Map<String, String> phi) {
      try {
        try (FileInputStream fstream = new FileInputStream(path.toString())) {
          InputStreamReader inputStreamReader = new InputStreamReader(fstream, StandardCharsets.UTF_8);
          return createData(path, inputStreamReader, phi);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    private static String createData(Path path, InputStreamReader is, Map<String, String> phi_input) throws IOException {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode noteNode = JsonNodeFactory.instance.objectNode();
      ((ObjectNode) noteNode).put(TextTag.note.name(), new BufferedReader(is)
          .lines().collect(Collectors.joining(System.lineSeparator())));
      ((ObjectNode) noteNode).put(TextTag.id.name(), path.getFileName().toString());
      String noteData = mapper.writeValueAsString(noteNode);
      //System.out.println("=================================================(before association) noteData.toString()=" + path.getFileName() + " " + noteData.substring(0,30));

      if (phi_input != null) {
        JSONObject json = new JSONObject();
        try {
          Map<String, String> map1 = mapper.readValue(noteData, Map.class);
          Map<String, String> phi = new HashMap<>();
          phi.putAll(map1);
          phi.putAll(phi_input);
          
          json = new JSONObject(phi);          
          System.out.println("=================================================(in association) noteData.toString()=" + path.getFileName() 
            + " phi.id=" + phi.get("id") 
            + " phi.pat_name=" + phi.get("pat_name") 
            + " phi.JITTER=" + phi.get("JITTER") 
            + " output=" + json.toString().substring(0, 35));
        } catch (IOException e) {
          log.debug("populateList failed due to {} , with message {}", e.getCause(), e.getMessage());
          e.printStackTrace();
        }

        //System.out.println("================================================= (after association) json.toString()=" + path.getFileName() + " " + json.toString().substring(0, 30));

        //Path newFilePath = Files.createDirectories(Paths.get("./local_deid/"));

        //Files.write(Paths.get(newFilePath + "/" + path.getFileName().toString() + ".txt"), json.toString().getBytes());
        
        return json.toString();
      }
      else
        System.out.println("=================================================(in association) phi is null" + path.getFileName()); 

      //System.out.println("================================================= (no association) $$$$$$$$$$$$$$$$ noteData=" + path.getFileName() + " " + noteData.substring(0, 30));

      return noteData;
    }

  }
  //static class RowToJson extends DoFn<SchemaAndRecord, String> {
  // final ObjectMapper mapper;
  //
  //public RowToJson(){
  //  mapper = new ObjectMapper();
  //  mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  //}
  //
  // @ProcessElement
  // public void processElement(ProcessContext context) throws IOException {
  //   SchemaAndRecord schemaAndRecord = context.element();
  //   String out = bigQueryRowToJsonFn.apply(schemaAndRecord);
  //   context.output(out);
  // }
  //}
}
