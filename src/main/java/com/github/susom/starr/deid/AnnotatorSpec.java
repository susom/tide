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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.Serializable;

/**
 * Annotator specification.
 * @author wenchengl
 */

@JsonDeserialize(using = AnnotatorSpec.SpecDeserializer.class)
public class AnnotatorSpec implements Serializable {
  String type;
  String annotatorLabel;

  public static final class AnnotatorSpecBuilder {
    String type;
    String annotatorLabel;

    private AnnotatorSpecBuilder() {
    }

    public static AnnotatorSpecBuilder getAnnotatorSpecBuilder() {
      return new AnnotatorSpecBuilder();
    }

    public AnnotatorSpecBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public AnnotatorSpecBuilder withAnnotatorLabel(String annotatorLabel) {
      this.annotatorLabel = annotatorLabel;
      return this;
    }

    /**
     * build AnnotatorSpec object.
     * @return AnnotatorSpec
     */
    public AnnotatorSpec build() {
      AnnotatorSpec annotatorSpec = new AnnotatorSpec();
      annotatorSpec.type = this.type;
      annotatorSpec.annotatorLabel = this.annotatorLabel;
      return annotatorSpec;
    }
  }

  static class SpecDeserializer extends StdDeserializer<AnnotatorSpec> {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public SpecDeserializer() {
      this(null);
    }

    public SpecDeserializer(Class<?> c) {
      super(c);
    }

    @Override
    public AnnotatorSpec deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);

      AnnotatorSpecBuilder builder = AnnotatorSpecBuilder.getAnnotatorSpecBuilder();
      builder.withType(node.get("type").asText());
      builder.withAnnotatorLabel(node.get("annotatorLabel").asText());

      return builder.build();
    }
  }
}
