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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Deid task specification.
 * @author wenchengl
 */

@JsonDeserialize(using = DeidSpec.SpecDeserializer.class)
public class DeidSpec implements Serializable {
  String itemName;
  Action action;
  String[] actionParam;
  Map<String,String> actionParamMap;
  String[] fields;
  String[] infoTypes;

  public static final class DeidSpecBuilder {
    String itemName;
    Action action;
    String[] actionParam;
    Map<String,String> actionParamMap;
    String[] fields;
    String[] infoTypes;

    private DeidSpecBuilder() {
    }

    public static DeidSpecBuilder getDeidSpecBuilder() {
      return new DeidSpecBuilder();
    }

    public DeidSpecBuilder withItemName(String itemName) {
      this.itemName = itemName;
      return this;
    }

    public DeidSpecBuilder withAction(Action action) {
      this.action = action;
      return this;
    }

    public DeidSpecBuilder withActionParam(String[] actionParam) {
      this.actionParam = actionParam;
      return this;
    }

    public DeidSpecBuilder withActionParamMap(Map<String,String> actionParamMap) {
      this.actionParamMap = actionParamMap;
      return this;
    }

    public DeidSpecBuilder withFields(String[] fields) {
      this.fields = fields;
      return this;
    }

    public DeidSpecBuilder withInfoTypes(String[] infoTypes) {
      this.infoTypes = infoTypes;
      return this;
    }

    /**
     * build DeidSpec object.
     * @return DeidSpec
     */
    public DeidSpec build() {
      DeidSpec deidSpec = new DeidSpec();
      deidSpec.actionParam = this.actionParam;
      deidSpec.fields = this.fields;
      deidSpec.infoTypes = this.infoTypes;
      deidSpec.itemName = this.itemName;
      deidSpec.actionParamMap = this.actionParamMap;
      deidSpec.action = this.action;
      return deidSpec;
    }
  }

  enum DateJitterMode {
    local,
    stanford_health_api
  }

  enum Action {
    general, // general pattern matching deid with GeneralAnonymizer
    general_number, //general order and account number
    surrogate_name, //surrogate name using NameSurrogate
    surrogate_address, //surrogate address using LocationSurrogate
    jitter_date_from_field, //DateAnonymizer with jitter provided in a field
    jitter_birth_date, //DateAnonymizer
    jitter_date_randomly, //randomly generate jitter using hash function
    remove_age, //AgeAnonymizer
    remove_mrn, //MrnAnonymizer
    replace_ner_name, //replace ner findings directly

    /* ActualNameAnonymizer for words with minimum word length of 5 */
    replace_minimumlengthword_with,

    /* ActualNameAnonymizer for word longer than 2 characters, and not in common vocabulary */
    replace_with,

    /*ActualNameAnonymizer applied strictly regardless word length, and if in common vocabulary */
    replace_strictly_with,

    remove,

    /* nothing or NoopAnonymizer */
    keep
  }

  static class SpecDeserializer extends StdDeserializer<DeidSpec> {
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
    public DeidSpec deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);

      DeidSpec spec = new DeidSpec();
      DeidSpecBuilder builder = DeidSpecBuilder.getDeidSpecBuilder();
      builder.withItemName(node.get("itemName").asText());

      if (node.get("action") != null) {
        builder.withAction(Action.valueOf(node.get("action").asText()));
      }

      if (node.get("actionParam") != null) {
        builder.withActionParam(node.get("actionParam").asText().split(" "));
      }

      //{"format":"L","f_zip":"zip","f_gender":"","f_dob":"birth_date"}
      if (node.get("actionParamMap") != null) {
        JsonNode paramNode = node.get("actionParamMap");
        Map<String,String> m = new HashMap<>();
        Iterator it = paramNode.fieldNames();
        while (it.hasNext()) {
          String k = (String)it.next();
          m.put(k, paramNode.get(k).asText());
        }

        builder.withActionParamMap(m);
      }

      if (node.get("fields") != null) {
        builder.withFields(node.get("fields").asText().split(","));
      }

      if (node.get("infoTypes") != null) {
        builder.withInfoTypes(node.get("infoTypes").asText().split(","));
      }
      return builder.build();
    }
  }
}
