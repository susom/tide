package com.github.susom.starr.deid;


import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

public class ProcessAnalytics<T> extends PTransform<PCollection<T>, PDone> {

  private final String itemName;
  private final String[] fields;


  public ProcessAnalytics(String itemName, String[] fields){
    this.itemName = itemName;
    this.fields = fields;
  }

  @Override
  public PDone expand(PCollection<T> input) {
//    input.apply("ConvertToRow", ParDo.of(new BuildRowFn()))
//      .apply(TextIO.write().to("counts-"));
//
//    return PDone.in(input.getPipeline());
    return null;
  }


  public static class PrintCounts extends SimpleFunction<KV<String, Long>, String> {
    @Override
    public String apply(KV<String, Long> input) {
      return input.getKey()+":"+input.getValue();
    }
  }
}


