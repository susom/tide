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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deid Output Transform.
 * @author wenchengl
 */

public class DeidOutputTransform
    extends PTransform<PCollection<String>, PCollection<String>> {

  private static final Logger log = LoggerFactory.getLogger(DeidOutputTransform.class);
  String outputResource;
  /**
   * main deid transform.
   * @param outputResource output resource
   * @throws IOException
   */
  public DeidOutputTransform(String outputResource) {
    this.outputResource = outputResource;
  }

  @Override
  public PCollection<String> expand(PCollection<String> input) {
    return input.apply(ParDo.of(new CreateOutput()));
  }

  class CreateOutput extends DoFn<String, String> {
    CreateOutput() {
    }

    /**
     * process element in the tranform.
     * @param context process context
     * @throws IOException throws from surrogate or DLP
     */
    @ProcessElement
    public void processElement(ProcessContext context)
        throws IOException {
      try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        JsonNode nodes = mapper.readTree(context.element());
        String id = nodes.get(TextTag.id.name()).asText();
        String textDeidNote = nodes.get(DeidResultProc.TEXT_DEID + TextTag.note.name()).asText();

        Path newFilePath = Files.createDirectories(Paths.get(outputResource));

        Files.write(Paths.get(newFilePath + "/" + id), textDeidNote.getBytes());
      }
      catch(Exception e) {
        e.printStackTrace();
      }
    }
  }
}
