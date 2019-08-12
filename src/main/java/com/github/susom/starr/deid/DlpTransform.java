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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.susom.starr.deid.anonymizers.AnonymizedItemWithReplacement;
import com.github.susom.starr.deid.anonymizers.AnonymizerProcessor;
import com.github.susom.starr.deid.anonymizers.DateAnonymizer;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.dlp.v2.DlpServiceSettings;
import com.google.privacy.dlp.v2.ByteContentItem;
import com.google.privacy.dlp.v2.CharacterMaskConfig;
import com.google.privacy.dlp.v2.ContentItem;
import com.google.privacy.dlp.v2.DateTime;
import com.google.privacy.dlp.v2.DeidentifyConfig;
import com.google.privacy.dlp.v2.DeidentifyContentRequest;
import com.google.privacy.dlp.v2.DeidentifyContentResponse;
import com.google.privacy.dlp.v2.Finding;
import com.google.privacy.dlp.v2.InfoType;
import com.google.privacy.dlp.v2.InfoTypeTransformations;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.InspectContentRequest;
import com.google.privacy.dlp.v2.InspectContentResponse;
import com.google.privacy.dlp.v2.InspectResult;
import com.google.privacy.dlp.v2.Likelihood;
import com.google.privacy.dlp.v2.PrimitiveTransformation;
import com.google.privacy.dlp.v2.ProjectName;
import com.google.privacy.dlp.v2.QuoteInfo.ParsedQuoteCase;
import com.google.privacy.dlp.v2.Range;
import com.google.privacy.dlp.v2.ReplaceWithInfoTypeConfig;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;

/**
 * Googld DLP Transform.
 * @author wenchengl
 */

public class DlpTransform extends PTransform<PCollection<String>,
    PCollection<DeidResult>> {

  private static final Logger log = LoggerFactory.getLogger(DlpTransform.class);

  public static final int DLP_CONTENT_LIMIT = 524288;

  public final Map<String,String> phiCategoryMap = new HashMap<>();
  public final Map<String,String> phiReplacementMap = new HashMap<>();

  public  String dateJitterField;

  /**
   * unify category mapping with other deid methods.
   * @param infoTypeName phi group name
   * @return mapped phi group type, otherwise use prefixed infoType
   */
  public String getPhiCategoryByInfoTypeName(String infoTypeName) {
    String key = infoTypeName.toUpperCase(Locale.ROOT);
    return phiCategoryMap.containsKey(key)
      ? phiCategoryMap.get(key)
      : "dlp_" + infoTypeName.toLowerCase(Locale.ROOT);
  }

  public String getReplacementByInfoType(String infoType) {
    String key = infoType.toUpperCase(Locale.ROOT);
    return phiReplacementMap.get(key);
  }

  final Likelihood minLikelihood = Likelihood.LIKELY;
  final boolean includeQuote = true;

  final List<InfoType> infoTypes = new ArrayList<>();

  final CharacterMaskConfig characterMaskConfig =
      CharacterMaskConfig.newBuilder()
      //.setMaskingCharacter(maskingCharacter.toString())
      //.setNumberToMask(numberToMask)
      .build();

  final ReplaceWithInfoTypeConfig replaceConf = ReplaceWithInfoTypeConfig.newBuilder().build();
  // Create the deidentification transformation configuration
  final PrimitiveTransformation primitiveTransformation =
      PrimitiveTransformation.newBuilder().setCharacterMaskConfig(characterMaskConfig)
        .setReplaceWithInfoTypeConfig(replaceConf).build();

  final InfoTypeTransformations.InfoTypeTransformation infoTypeTransformationObject =
      InfoTypeTransformations.InfoTypeTransformation.newBuilder()
        .setPrimitiveTransformation(primitiveTransformation)
        .addAllInfoTypes(infoTypes)
        .build();

  final InfoTypeTransformations infoTypeTransformationArray =
      InfoTypeTransformations.newBuilder()
        .addTransformations(infoTypeTransformationObject)
        .build();

  final DeidentifyConfig deidentifyConfig =
      DeidentifyConfig.newBuilder()
        .setInfoTypeTransformations(infoTypeTransformationArray)
        .build();

  final int maxFindings = 1000;
  InspectConfig.FindingLimits findingLimits =
      InspectConfig.FindingLimits.newBuilder().setMaxFindingsPerItem(maxFindings).build();

  private final DeidJob job;
  private final String projectId;
  private final InspectConfig inspectConfig;

  //private DlpServiceSettings dlpServiceSettings;

  /**
   * DLP transform contructor.
   * @param job job spec
   * @param projectId DLP project id
   * @throws IOException when DLP api call fails
   */
  public DlpTransform(DeidJob job, String projectId) throws IOException {

    this.projectId = projectId;
    this.job = job;
    for (DeidSpec spec : job.getGoogleDlpInfoTypes()) {
      for (String f : spec.infoTypes) {
        f = f.trim().toUpperCase(Locale.ROOT);
        if (f.length() > 0) {
          infoTypes.add(InfoType.newBuilder().setName(f).build());
          phiCategoryMap.put(f,spec.itemName);
          phiReplacementMap.put(f, spec.actionParam != null && spec.actionParam.length > 0
            ? spec.actionParam[0]
            : ("[" + f.toLowerCase(Locale.ROOT).replaceAll("_"," ") +  "]"));
          log.info("added infoType: " +  f + " to   " + spec.itemName);
        }

        if (f.equals("DATE") && spec.fields != null && spec.fields.length > 0) {
          dateJitterField = spec.fields[0];
        }
      }
    }

    inspectConfig =
      InspectConfig.newBuilder()
        .addAllInfoTypes(infoTypes)
        .setMinLikelihood(minLikelihood)
        //.setLimits(findingLimits)
        .setIncludeQuote(includeQuote)
        .build();

    //DlpServiceSettings.Builder dlpServiceSettingsBuilder =
    //  DlpServiceSettings.newBuilder();
    //
    //dlpServiceSettingsBuilder.inspectContentSettings().getRetrySettings().toBuilder()
    //  .setTotalTimeout(Duration.ofSeconds(10));
    //
    //dlpServiceSettings = dlpServiceSettingsBuilder.build();
  }

  //TODO implement transform if needed
  @Override
  public PCollection<DeidResult> expand(PCollection<String> input) {
    return null;
  }

  /**
   * call DLP request api.
   * @param text input text
   * @param items store findings
   * @return deided text
   */
  public String dlpDeidRequest(String text,  List<AnonymizedItemWithReplacement> items) {

    String result = null;

    try (DlpServiceClient dlpServiceClient = DlpServiceClient.create()) { //or with dlpServiceSettings

      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

      int retryCount = 0;
      int maxTry = 4;
      while (retryCount < maxTry) {
        try {
          ContentItem contentItem = ContentItem.newBuilder().setValue(text).build();

          DeidentifyContentRequest request =
          DeidentifyContentRequest.newBuilder()
            .setParent(ProjectName.of(projectId).toString())
            .setInspectConfig(inspectConfig)
            .setDeidentifyConfig(deidentifyConfig)
            .setItem(contentItem)
            .build();

          DeidentifyContentResponse response = dlpServiceClient.deidentifyContent(request);

          java.util.List<com.google.privacy.dlp.v2.TransformationSummary>
          summaries = response.getOverview().getTransformationSummariesList();
          result = response.getItem().getValue();

          summaries.stream().forEach(s -> {
            AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
              null,
              -1, -1,
              null,
              "google-dlp",
              getPhiCategoryByInfoTypeName(s.getInfoType().getName()));

            items.add(ai);
          });

          break;
        } catch (Exception e) {
          log.info("Error from DLP request", e);
          if (retryCount < maxTry - 1) {
            double waitFor = Math.pow(2,retryCount);
            log.info("retry after " + waitFor + " seconds");
            Thread.sleep((long)waitFor * 1000L);
          } else {
            log.error("give up DLP request");
            return null;
          }
        }

        retryCount++;
      }

    } catch (Exception e) {
      log.info("Failed to call DLP API ",e);
    }

    return result;
  }

  /**
   * call DLP Inspect API.
   * @param text input text
   * @param items store findings
   * @return inpectResult from DLP api
   * @throws IOException if api call fails
   */
  public InspectResult dlpInspectRequest(String text, List<AnonymizedItemWithReplacement> items, int jitter) throws IOException {

    InspectResult result = null;

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    try (DlpServiceClient dlpServiceClient = DlpServiceClient.create()) {

      //DLP accepts byte array, and return finding position of the array.
      //We need to convert array position to character position in the orignal text, otherwise
      // there will be incorrect offset because of non-utf 8 characters
      //option one: only support English. replace all non-utf8 characters with placeholder *
      text = text.replaceAll("[^\\x00-\\x7F]", "*");

      //option two: send orginal text and correct position after
      ByteString bs = ByteString.copyFromUtf8(text);
//      boolean needCorrection = text.length() != bs.size();

      //limit the request content size to under 524288, the limit of the DLP api
      if (bs.size() > DLP_CONTENT_LIMIT ) {
        bs = bs.substring(0, DLP_CONTENT_LIMIT);
        //avoid break word
        int pos = DLP_CONTENT_LIMIT - 1;
        for (int i = bs.size() - 1; i >= 0; i--) {
          if (bs.byteAt(i) == (byte) 32) {
            pos = i;
            break;
          }
        }
        bs = bs.substring(0, pos + 1);
      }

      ByteContentItem byteContentItem =
        ByteContentItem.newBuilder()
          .setType(ByteContentItem.BytesType.TEXT_UTF8)
          .setData(bs)
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
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);

        for (Finding finding : result.getFindingsList()) {
          String replacement = null;
          InfoType it = finding.getInfoType();
          replacement = getReplacementByInfoType(it.getName());

          ParsedQuoteCase findingCase = finding.getQuoteInfo().getParsedQuoteCase();
          if (findingCase == ParsedQuoteCase.DATE_TIME && jitter != 0) {
            DateTime dateFound = finding.getQuoteInfo().getDateTime();
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
            cal.set(Calendar.YEAR, dateFound.getDate().getYear());
            cal.set(Calendar.MONTH, dateFound.getDate().getMonth() - 1);
            cal.set(Calendar.DAY_OF_MONTH, dateFound.getDate().getDay());

            replacement = DateAnonymizer.getJitteredDate(jitter, cal.getTime());
          }

          Range r = finding.getLocation().getByteRange();
          byte[] slice = Arrays.copyOfRange(textBytes, (int)r.getStart(), (int)r.getEnd());

          AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
            new String(slice,StandardCharsets.UTF_8),
            r.getStart(), r.getEnd(),
            replacement,
            "google-dlp",
            getPhiCategoryByInfoTypeName(finding.getInfoType().getName()));
          items.add(ai);
        }
      }
    }

    return result;
  }

//  private String flagTextWithDlpFindings(byte[] textBytes,
//                                         List<AnonymizedItemWithReplacement> items)
//                              throws UnsupportedEncodingException {
//    final ByteBuffer buf = ByteBuffer.wrap(textBytes);
//    items.forEach(i -> {
//      byte[] newContent = new byte[i.getEnd() - i.getStart()];
//      newContent[0] = 91;
//      newContent[newContent.length - 1] = 93;
//      int pos = 1;
//      while (pos < newContent.length - 1) {
//        newContent[pos] = pos - 1 < i.getType().length() ? (byte)i.getType().charAt(pos - 1) : 32;
//        pos++;
//      }
//      buf.position(i.getStart());
//      buf.put(newContent);
//    });
//    return new String(buf.array(), "UTF-8");
//  }

}
