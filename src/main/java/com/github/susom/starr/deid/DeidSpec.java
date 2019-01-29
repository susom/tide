package com.github.susom.starr.deid;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * Deid task specification
 * @author wenchengl
 */

@JsonDeserialize(using = SpecDeserializer.class)
public class DeidSpec implements Serializable {
  String item_name;
  Action action;
  String[] action_param;
  Map<String,String> action_param_map;
  String[] fields;


  public static final class DeidSpecBuilder {
    String item_name;
    Action action;
    String[] action_param;
    Map<String,String> action_param_map;
    String[] fields;

    private DeidSpecBuilder() {
    }

    public static DeidSpecBuilder aDeidSpec() {
      return new DeidSpecBuilder();
    }

    public DeidSpecBuilder withItem_name(String item_name) {
      this.item_name = item_name;
      return this;
    }

    public DeidSpecBuilder withAction(Action action) {
      this.action = action;
      return this;
    }

    public DeidSpecBuilder withAction_param(String[] action_param) {
      this.action_param = action_param;
      return this;
    }

    public DeidSpecBuilder withAction_param_map(Map<String,String> action_param_map) {
      this.action_param_map = action_param_map;
      return this;
    }


    public DeidSpecBuilder withFields(String[] fields) {
      this.fields = fields;
      return this;
    }

    public DeidSpec build() {
      DeidSpec deidSpec = new DeidSpec();
      deidSpec.action_param = this.action_param;
      deidSpec.fields = this.fields;
      deidSpec.item_name = this.item_name;
      deidSpec.action_param_map = this.action_param_map;
      deidSpec.action = this.action;
      return deidSpec;
    }
  }
}


enum DateJitterMode{
  local,
  stanford_health_api
}

enum Action{
  general, // general pattern matching deid with GeneralAnonymizer
  surrogate_name, //surrogate name using NameSurrogate
  surrogate_address, //surrogate address using LocationSurrogate
  jitter_date, //DateAnonymizer
  jitter_birth_date, //DateAnonymizer
  remove_age, //AgeAnonymizer
  remove_mrn, //MrnAnonymizer
  replace_minimumlengthword_with,  //ActualNameAnonymizer for words with minimum word length of 5
  replace_with,  //ActualNameAnonymizer for word longer than 2 characters, and not in common vocabulary
  replace_strictly_with, ////ActualNameAnonymizer applied strictly regardless word length, and if in common vocabulary
  remove,
  keep  //nothing or NoopAnonymizer
}

class SpecDeserializer extends StdDeserializer<DeidSpec> {
  private static ObjectMapper mapper = new ObjectMapper();
  static {
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  public SpecDeserializer(){
    this(null);
  }


  public SpecDeserializer(Class<?> c){
    super(c);
  }

  @Override
  public DeidSpec deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonNode node = p.getCodec().readTree(p);

    DeidSpec spec = new DeidSpec();
    DeidSpec.DeidSpecBuilder builder = DeidSpec.DeidSpecBuilder.aDeidSpec();
    builder.withItem_name(node.get("item_name").asText());

    if(node.get("action")!=null) {
      builder.withAction(Action.valueOf(node.get("action").asText()));
    }

    if(node.get("action_param")!=null) {
      builder.withAction_param(node.get("action_param").asText().split(" "));
    }

    //{"format":"L","f_zip":"zip","f_gender":"","f_dob":"birth_date"}
    if(node.get("action_param_map")!=null) {
      JsonNode paramNode = node.get("action_param_map");
      Map<String,String> m = new HashMap<>();
      Iterator it = paramNode.fieldNames();
      while (it.hasNext()){
        String k = (String)it.next();
        m.put(k, paramNode.get(k).asText());
      }

      builder.withAction_param_map(m);
    }

    if(node.get("fields")!=null) {
      builder.withFields(node.get("fields").asText().split(","));
    }

    return builder.build();
  }
}
