package com.github.susom.starr.deid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.avro.generic.GenericRecord;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.SchemaAndRecord;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * generalize input format for BigQuery input
 * TODO use more efficient coder
 *
 * @author wenchengl
 */

public class InputTransforms {


  private static final Logger log = LoggerFactory.getLogger(InputTransforms.class);

  private static final SerializableFunction<SchemaAndRecord, String> bigQueryRowToJsonFn =
    new SerializableFunction<SchemaAndRecord, String>(){

      ObjectMapper mapper = new ObjectMapper();


      @Override
      public String apply(SchemaAndRecord schemaAndRecord) {
        ObjectNode simpleNode = mapper.createObjectNode();
        GenericRecord gr = schemaAndRecord.getRecord();
        TableSchema ts = schemaAndRecord.getTableSchema();

        ts.getFields().forEach(f->{

          String key = f.getName();
          Object value = gr.get(f.getName());
//          log.info(key+":"+value);
          switch (f.getType().toUpperCase()){

            case "STRING":
                simpleNode.put(key, value!=null?value.toString():null);
              break;
            case "BYTES":
              if(key!=null && value!=null)
              simpleNode.put(key, value!=null?(byte[])value:null);
              break;

            case "INTEGER":
            case "INT64":
              if(key!=null && value!=null)
              simpleNode.put(key, value!=null?(Long) value:null); //TODO check why BigQueryIO read this as Long
              break;

            case "FLOAT":
            case "FLOAT64":
              if(key!=null && value!=null)
              simpleNode.put(key, value!=null?(Long) value:null);
              break;
            case "TIMESTAMP":
            case "DATE":
              if(key!=null && value!=null)
              simpleNode.put(key, value!=null?(Long)value:null);
              break;
            case "BOOLEAN":
            case "BOOL":
              if(key!=null && value!=null)
              simpleNode.put(key, value!=null?(Boolean)value:null);
              break;

            default:
              break;
          }

        });

        try {
          return mapper.writeValueAsString(simpleNode);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
          return null;
        }
      }
    };



  static class BigQueryRowToJson{

    public static PCollection<String>  withBigQueryLink (Pipeline pipeline, String resourceLink) {
      return pipeline.apply(
        BigQueryIO
          .read(bigQueryRowToJsonFn)
          .withCoder(StringUtf8Coder.of())
          .from(resourceLink)); //projectId:dataSet.table
    }
  }



//   static class RowToJson extends DoFn<SchemaAndRecord, String> {
//     final ObjectMapper mapper;
//
//    public RowToJson(){
//      mapper = new ObjectMapper();
//      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//    }
//
//     @ProcessElement
//     public void processElement(ProcessContext context) throws IOException {
//       SchemaAndRecord schemaAndRecord = context.element();
//       String out = bigQueryRowToJsonFn.apply(schemaAndRecord);
//       context.output(out);
//     }
//   }


}
