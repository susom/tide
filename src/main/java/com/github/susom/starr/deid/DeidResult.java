package com.github.susom.starr.deid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.sdk.values.TypeDescriptors;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class DeidResult implements Serializable {

  @DefaultCoder(AvroCoder.class)
  static class DeidText implements Serializable{

    @Nullable String text_id_1;
    @Nullable String text_id_2;
    @Nullable String text_id_3;
    @Nullable String text_id_4;

    @Nullable String text_1;
    @Nullable String text_2;

    @Nullable String textStage1;
    @Nullable String statsStage1;
    @Nullable Integer statsCntStage1 = 0 ;
    @Nullable String textStage2;
    @Nullable String statsStage2;
    @Nullable Integer statsCntStage2 = 0 ;

    @Nullable String text2Stage1;
    @Nullable String stats2Stage1;
    @Nullable Integer stats2CntStage1 = 0 ;
    @Nullable String text2Stage2;
    @Nullable String stats2Stage2;
    @Nullable Integer stats2CntStage2 = 0 ;


    public DeidText(String[] text_id){
      if(text_id.length>0){
        this.text_id_1 = text_id[0];
      }
      if(text_id.length>1){
        this.text_id_2 = text_id[1];
      }
      if(text_id.length>2){
        this.text_id_3 = text_id[2];
      }
      if(text_id.length>3){
        this.text_id_4 = text_id[3];
      }
    }

    public DeidText(String[] text_id, String text, String stats){
      if(text_id.length>0){
        this.text_id_1 = text_id[0];
      }
      if(text_id.length>1){
        this.text_id_2 = text_id[1];
      }
      if(text_id.length>2){
        this.text_id_3 = text_id[2];
      }
      if(text_id.length>3){
        this.text_id_4 = text_id[3];
      }
      this.textStage1 = text;
      this.statsStage1 = stats;

    }
    public DeidText(String[] text_id, String textStage1, String statsStage1,Integer statsCntStage1,
                    String textStage2, String statsStage2,Integer statsCntStage2){
      if(text_id.length>0){
        this.text_id_1 = text_id[0];
      }
      if(text_id.length>1){
        this.text_id_2 = text_id[1];
      }
      if(text_id.length>2){
        this.text_id_3 = text_id[2];
      }
      if(text_id.length>3){
        this.text_id_4 = text_id[3];
      }

      this.textStage1 = textStage1;
      this.statsStage1 = statsStage1;
      this.statsCntStage1 = statsCntStage1;
      this.textStage2 = textStage2;
      this.statsStage2 = statsStage2;
      this.statsCntStage2 = statsCntStage2;
    }

    public String getText2Stage1() {
      return text2Stage1;
    }

    public void setText2Stage1(String text2Stage1) {
      this.text2Stage1 = text2Stage1;
    }

    public String getStats2Stage1() {
      return stats2Stage1;
    }

    public void setStats2Stage1(String stats2Stage1) {
      this.stats2Stage1 = stats2Stage1;
    }

    public Integer getStats2CntStage1() {
      return stats2CntStage1;
    }

    public void setStats2CntStage1(Integer stats2CntStage1) {
      this.stats2CntStage1 = stats2CntStage1;
    }

    public String getText2Stage2() {
      return text2Stage2;
    }

    public void setText2Stage2(String text2Stage2) {
      this.text2Stage2 = text2Stage2;
    }

    public String getStats2Stage2() {
      return stats2Stage2;
    }

    public void setStats2Stage2(String stats2Stage2) {
      this.stats2Stage2 = stats2Stage2;
    }

    public Integer getStats2CntStage2() {
      return stats2CntStage2;
    }

    public void setStats2CntStage2(Integer stats2CntStage2) {
      this.stats2CntStage2 = stats2CntStage2;
    }

    public String getText_1() {
      return text_1;
    }

    public void setText_1(String text_1) {
      this.text_1 = text_1;
    }

    public String getText_2() {
      return text_2;
    }

    public void setText_2(String text_2) {
      this.text_2 = text_2;
    }

    public DeidText(){

    }
    public String getText_id_1() {
      return text_id_1;
    }

    public void setText_id_1(String text_id_1) {
      this.text_id_1 = text_id_1;
    }

    public String getText_id_2() {
      return text_id_2;
    }

    public void setText_id_2(String text_id_2) {
      this.text_id_2 = text_id_2;
    }

    public String getText_id_3() {
      return text_id_3;
    }

    public void setText_id_3(String text_id_3) {
      this.text_id_3 = text_id_3;
    }

    public String getText_id_4() {
      return text_id_4;
    }

    public void setText_id_4(String text_id_4) {
      this.text_id_4 = text_id_4;
    }

    public String getTextStage1() {
      return textStage1;
    }

    public void setTextStage1(String textStage1) {
      this.textStage1 = textStage1;
    }

    public String getStatsStage1() {
      return statsStage1;
    }

    public void setStatsStage1(String statsStage1) {
      this.statsStage1 = statsStage1;
    }

    public String getTextStage2() {
      return textStage2;
    }

    public void setTextStage2(String textStage2) {
      this.textStage2 = textStage2;
    }

    public String getStatsStage2() {
      return statsStage2;
    }

    public void setStatsStage2(String statsStage2) {
      this.statsStage2 = statsStage2;
    }

    public Integer getStatsCntStage1() {
      return statsCntStage1;
    }

    public void setStatsCntStage1(Integer statsCntStage1) {
      this.statsCntStage1 = statsCntStage1;
    }

    public Integer getStatsCntStage2() {
      return statsCntStage2;
    }

    public void setStatsCntStage2(Integer statsCntStage2) {
      this.statsCntStage2 = statsCntStage2;
    }
  }




  public static class DeidResultProc extends DoFn<DeidText,String> {
    private final Counter totalPhiCnt_stage1 =
      Metrics.counter(DeidResultProc.class, "totalPhiCnt_stage1");


    private final Counter totalPhiCnt_stage2 =
      Metrics.counter(DeidResultProc.class, "totalPhiCnt_stage2");

    private final Counter zeroChangeCnt_stage1 =
      Metrics.counter(DeidResultProc.class, "zeroChangeCount_Stage1");


    private final Counter zeroChangeCnt_stage2 =
      Metrics.counter(DeidResultProc.class, "zeroChangeCount_Stage2");


    private final Distribution item_names_stage1 = Metrics.distribution(DeidResultProc.class, "ItemPerTextDistribution_stage1");

    private final Distribution item_names_stage2 = Metrics.distribution(DeidResultProc.class, "ItemPerTextDistribution_stage2");


    public DeidResultProc(){
    }

    @ProcessElement
    public void processElement(ProcessContext context) throws IOException {
      ObjectMapper mapper = new ObjectMapper();

      DeidText dt = context.element();
      String dtString = mapper.writeValueAsString(dt);
      context.output(DeidTransform.fullResultTag, dtString);

      if(dt.statsStage1!=null){
        JsonNode nodes = mapper.readTree(dt.statsStage1);
        if(nodes.size()==0){
          zeroChangeCnt_stage1.inc();
        }

        totalPhiCnt_stage1.inc(nodes.size());
        item_names_stage1.update(nodes.size());
        Set<String> phiCat = new HashSet<>();
        for (JsonNode node : nodes){
          String stat = node.get("type").asText();

          context.output(DeidTransform.statGlobalStage1Tag, stat);
          phiCat.add(stat);
        }

        phiCat.forEach(s -> context.output(DeidTransform.statPerTextStage1Tag, s));
      }else{
        zeroChangeCnt_stage1.inc();
      }




      if(dt.statsStage2!=null){
        Set<String> phiCat = new HashSet<>();
        JsonNode nodes = mapper.readTree(dt.statsStage2);
        if(nodes.size()==0){
          zeroChangeCnt_stage2.inc();
        }
        totalPhiCnt_stage2.inc(nodes.size());
        item_names_stage2.update(nodes.size());

        for (JsonNode node : nodes){
          String stat = node.get("type").asText();
          context.output(DeidTransform.statGlobalStage2Tag, stat);
          phiCat.add(stat);
        }

        phiCat.forEach(s -> context.output(DeidTransform.statPerTextStage2Tag, s));

      }else{
        zeroChangeCnt_stage2.inc();
      }

    }
  }



  public static class AnalyzeStatsByPhi
    extends PTransform<PCollection<String>, PCollection<KV<String, Long>>> {

    public AnalyzeStatsByPhi(){

    }

    @Override
    public PCollection<KV<String, Long>> expand(PCollection<String> input) {

      PCollection<KV<String, Long>> result = input.apply(Count.perElement());
      return result;
    }
  }
}
