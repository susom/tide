package com.github.susom.starr.deid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.privacy.dlp.v2.*;


import com.google.protobuf.ByteString;
import edu.stanford.irt.core.facade.AnonymizedItem;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Googld DLP Transform
 * @author wenchengl
 */

public class DlpTransform extends PTransform<PCollection<String>,
  PCollection<DeidResult.DeidText>> {


  private static final Logger log = LoggerFactory.getLogger(DlpTransform.class);

  public static Map<String,String> phiCategoryMap = new HashMap<>();

  public String getPhiCategoryByInfoTypeName(String infoTypeName){
    return phiCategoryMap.containsKey(infoTypeName.toUpperCase())?phiCategoryMap.get(infoTypeName.toUpperCase()):"dlp_"+infoTypeName.toLowerCase();
  }

  Likelihood minLikelihood = Likelihood.LIKELY;
  boolean includeQuote = true;


  List<InfoType> infoTypes = new ArrayList<>();

  CharacterMaskConfig characterMaskConfig =
    CharacterMaskConfig.newBuilder()
//          .setMaskingCharacter(maskingCharacter.toString())
//          .setNumberToMask(numberToMask)
      .build();


  ReplaceWithInfoTypeConfig replaceConf = ReplaceWithInfoTypeConfig.newBuilder().build();
  // Create the deidentification transformation configuration
  PrimitiveTransformation primitiveTransformation =
    PrimitiveTransformation.newBuilder().setCharacterMaskConfig(characterMaskConfig)
      .setReplaceWithInfoTypeConfig(replaceConf).build();

  InfoTypeTransformations.InfoTypeTransformation infoTypeTransformationObject =
    InfoTypeTransformations.InfoTypeTransformation.newBuilder()
      .setPrimitiveTransformation(primitiveTransformation)
      .addAllInfoTypes(infoTypes)
      .build();

  InfoTypeTransformations infoTypeTransformationArray =
    InfoTypeTransformations.newBuilder()
      .addTransformations(infoTypeTransformationObject)
      .build();

  DeidentifyConfig deidentifyConfig =
    DeidentifyConfig.newBuilder()
      .setInfoTypeTransformations(infoTypeTransformationArray)
      .build();


  int maxFindings = 0;
  InspectConfig.FindingLimits findingLimits =
    InspectConfig.FindingLimits.newBuilder().setMaxFindingsPerItem(maxFindings).build();

  private final DeidJob job;
  private final String projectId;
  private InspectConfig inspectConfig;

  public DlpTransform(DeidJob job, String projectId) throws IOException {

    this.projectId = projectId;
    this.job = job;
    for (DeidSpec spec : job.getGoogleDlpInfoTypes()){
      for (String f : spec.fields){
        f = f.trim().toUpperCase();
        if(f.length()>0){
          infoTypes.add(InfoType.newBuilder().setName(f).build());
          phiCategoryMap.put(f,spec.item_name);
          log.info("added infoType: "+ f+" to "+spec.item_name);
        }
      }
    }

    inspectConfig =
      InspectConfig.newBuilder()
        .addAllInfoTypes(infoTypes)
        .setMinLikelihood(minLikelihood)
        .setLimits(findingLimits)
        .setIncludeQuote(includeQuote)
        .build();

  }



  //TODO implement transform if needed
  @Override
  public PCollection<DeidResult.DeidText> expand(PCollection<String> input) {
    return null;
  }

  public void dlpDeidRequest(String text, DeidResult.DeidText deidResult){
    DlpServiceClient dlpServiceClient = null;
    try{


      dlpServiceClient = DlpServiceClient.create();


      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

      int retryCount = 0;
      int maxTry = 12;
      while(retryCount<maxTry){
        try{
//      ContentItem contentItem = ContentItem.newBuilder().setValue(text).build();

//      DeidentifyContentRequest request =
//        DeidentifyContentRequest.newBuilder()
//          .setParent(ProjectName.of(projectId).toString())
//          .setInspectConfig(inspectConfig)
//          .setDeidentifyConfig(deidentifyConfig)
//          .setItem(contentItem)
//          .build();

//      DeidentifyContentResponse response = dlpServiceClient.deidentifyContent(request);

//      java.util.List<com.google.privacy.dlp.v2.TransformationSummary>
//        summaries = response.getOverview().getTransformationSummariesList();

//      List<AnonymizedItem> items = new ArrayList<>();

//      String result = response.getItem().getValue();
//      deidResult.setTextStage2(result);

//      summaries.stream().forEach(s->{
//        AnonymizedItem ai = new AnonymizedItem("", getPhiCategoryByInfoTypeName(s.getInfoType().getName()));
//        items.add(ai);
//      });
//      String stats = mapper.writeValueAsString(items);
//
//      deidResult.setStatsStage2(stats);
//      deidResult.setStatsCntStage2(items.size());


          dlpInspectRequest(text,deidResult,dlpServiceClient ); //TODO ask Google to return stats in deid api response

//        log.info(result);
          break;
        }catch (Exception e) {
          log.info("Error in processing String: " + e.getMessage());
          if(retryCount<8){
            double waitFor = Math.pow(2,retryCount);
            log.info("retry after "+waitFor+" seconds");
            Thread.sleep((long)waitFor*1000L);
          }else {
            log.info("retry after 5 minutes");
            Thread.sleep(300000L);
          }
          if(retryCount==maxTry-1){
            log.error("giving up processing");
          }
        }

        retryCount++;
      }



    } catch (Exception e) {
      log.info("Error in deidentifyWithMask: " + e.getMessage());
    }finally {
      if(dlpServiceClient!=null){
        dlpServiceClient.shutdown();
      }
    }
  }


  public InspectResult dlpInspectRequest(String text, DeidResult.DeidText deidResult, DlpServiceClient dlpServiceClient) throws IOException {

    InspectResult result = null;

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);


    if( dlpServiceClient == null){
      dlpServiceClient = DlpServiceClient.create();
    }

    ByteContentItem byteContentItem =
      ByteContentItem.newBuilder()
        .setType(ByteContentItem.BytesType.TEXT_UTF8)
        .setData(ByteString.copyFromUtf8(text))
        .build();
    ContentItem contentItem = ContentItem.newBuilder().setByteItem(byteContentItem).build();

    InspectContentRequest request =
      InspectContentRequest.newBuilder()
        .setParent(ProjectName.of(projectId).toString())
        .setInspectConfig(inspectConfig)
        .setItem(contentItem)
        .build();

    // Inspect the text for info types
    InspectContentResponse response = dlpServiceClient.inspectContent(request);

    result = response.getResult();
    if (result.getFindingsCount() > 0) {

      List<AnonymizedItem> items = new ArrayList<>();
      byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

      for (Finding finding : result.getFindingsList()) {
        Range r = finding.getLocation().getByteRange();
        byte[] slice = Arrays.copyOfRange(textBytes, (int)r.getStart(), (int)r.getEnd());

        AnonymizedItem ai = new AnonymizedItem(new String(slice,StandardCharsets.UTF_8 ), getPhiCategoryByInfoTypeName(finding.getInfoType().getName()));
        ai.setStart((int)r.getStart());
        ai.setEnd((int)r.getEnd());
        items.add(ai);
      }

      String stats = mapper.writeValueAsString(items);

      deidResult.setStatsStage1(stats);
      deidResult.setStatsCntStage1(items.size());

      if(deidResult.getTextStage1()==null){
        deidResult.setTextStage1(flagTextWithDlpFindings(textBytes,items));
      }

    } else {
//        log.info("No findings.");
      deidResult.setStatsStage1(null);
      deidResult.setStatsCntStage1(0);
      deidResult.setTextStage1(text);
    }

    return result;
  }


  private String flagTextWithDlpFindings(byte[] textBytes, List<AnonymizedItem> items) throws UnsupportedEncodingException {
    final ByteBuffer buf = ByteBuffer.wrap(textBytes);
    items.forEach(i ->{
      byte[] newContent = new byte[i.getEnd()-i.getStart()];
      newContent[0]=91;
      newContent[newContent.length-1]=93;
      int pos = 1;
      while(pos<newContent.length-1){
        newContent[pos]=pos-1<i.getType().length()?(byte)i.getType().charAt(pos-1):32;
        pos++;
      }
      buf.position(i.getStart());
      buf.put(newContent);
    });
    return new String(buf.array(), "UTF-8");
  }


}
