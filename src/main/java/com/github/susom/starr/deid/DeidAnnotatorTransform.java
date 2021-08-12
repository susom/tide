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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deid Output Transform.
 * @author wenchengl
 */

public class DeidAnnotatorTransform
    extends PTransform<PCollection<String>, PCollection<String>> {

  private static final Logger log = LoggerFactory.getLogger(DeidAnnotatorTransform.class);
  //AnnotatorSpecs annotatorSpecs;
  String outputResource;

  /**
   * main deid transform.
   * @param annotatorSpecs annotator specs
   */
  public DeidAnnotatorTransform(/*AnnotatorSpecs annotatorSpecs, */String outputResource) {    
    //this.annotatorSpecs = annotatorSpecs;
    this.outputResource = outputResource;
  }

  @Override
  public PCollection<String> expand(PCollection<String> input) {
    return input.apply(ParDo.of(new UpdateMapping()));
  }

  class UpdateMapping extends DoFn<String, String> {
    UpdateMapping() {
    }

    /**
     * process element in the tranform.
     * @param context process context
     * @throws IOException throws from surrogate or DLP
     */
    @ProcessElement
    public void processElement(ProcessContext context)
        throws IOException {
      
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      JsonNode nodes = mapper.readTree(context.element());
      String id = nodes.get(TextTag.id.name()).asText();
      try {
        String note = nodes.get(TextTag.note.name()).asText();
        JsonNode findingNoteNode = nodes.get(DeidResultProc.STATS_DEID + TextTag.note.name());
        Map<String, String> labelNodeMap = new HashMap<>();
        if (findingNoteNode != null) {
          String findingNote = findingNoteNode.asText();
          JsonNode nodeLabel = mapper.readTree(findingNote);
          for (JsonNode node : nodeLabel) {
            String start = node.get(TextTag.start.name()).asText();
            String end = node.get(TextTag.end.name()).asText();
            String type = node.get(TextTag.type.name()).asText();
            //String label = annotatorSpecs.getLabel(type);
            labelNodeMap.put(String.format("%s,%s", start, end), String.format("[%s,%s,\"%s\"]", start, end, type));
            //labelNodeTexts.add(String.format("[%s,%s,\"%s\"]", start, end, type)); //label == null ? type : label));
          }
        }
        Set<String> labelNodeTexts = labelNodeMap.values().stream().collect(Collectors.toSet());

        JsonNode annotatorNode = JsonNodeFactory.instance.objectNode();
        ((ObjectNode) annotatorNode).put(TextTag.text.name(), note);
        JSONArray array4 = new JSONArray();
        array4.put(String.join(",", labelNodeTexts));
        JsonNode jsonNode4 = mapper.readTree(array4.toString());
        ((ObjectNode) annotatorNode).set(TextTag.label.name(), jsonNode4);
        ((ObjectNode) annotatorNode).put(TextTag.id.name(), id);

        String annotator = mapper.writeValueAsString(annotatorNode);
        annotator = annotator.replace("\"label\":[\"[", "\"label\":[[").replace("\\\"", "\"").replace("\"]\"],", "\"]],");

        Path newFilePath = Files.createDirectories(Paths.get(outputResource));
        Files.write(Paths.get(newFilePath + "/" + id + ".json"), annotator.getBytes());
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
  }
}
