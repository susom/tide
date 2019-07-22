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
import com.github.susom.starr.Utility;
import com.github.susom.starr.deid.anonymizers.AgeAnonymizer;
import com.github.susom.starr.deid.anonymizers.AnonymizedItemWithReplacement;
import com.github.susom.starr.deid.anonymizers.AnonymizerProcessor;
import com.github.susom.starr.deid.anonymizers.DateAnonymizer;
import com.github.susom.starr.deid.anonymizers.GeneralAnonymizer;
import com.github.susom.starr.deid.anonymizers.LocationSurrogate;
import com.github.susom.starr.deid.anonymizers.LocationSurrogate.Address;
import com.github.susom.starr.deid.anonymizers.MrnAnonymizer;
import com.github.susom.starr.deid.anonymizers.NameSurrogate;
import com.github.susom.starr.deid.anonymizers.NameSurrogate.NameType;
import com.github.susom.starr.deid.anonymizers.TokenArrayAnonymizer;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deid Transform.
 * @author wenchengl
 */

public class DeidTransform
    extends PTransform<PCollection<String>, PCollection<DeidResult>> {

  public static final TupleTag<String> fullResultTag = new TupleTag<String>() {};
  public static final TupleTag<String> statsDlpPhiTypeTag = new TupleTag<String>() {};
  public static final TupleTag<String> statsPhiTypeTag = new TupleTag<String>() {};
  public static final TupleTag<String> statPhiFoundByTag = new TupleTag<String>() {};

  public static final int MINIMUM_WORD_LENGTH = 3;

  private static final String wordIgnoreFile = "wordIgnore.txt";
  private static final HashSet<String> ignoreWords = new HashSet<>();

  static {
    Utility.loadFileToMemory(wordIgnoreFile, ignoreWords);
  }

  private final DeidJob job;
  private final DlpTransform dlpTransform;

  private static final Logger log = LoggerFactory.getLogger(DeidTransform.class);

  static StanfordCoreNLP pipeline = null;
  /*
  static NERClassifierCombiner ncc = null;
  TODO need to compare performance difference between these two methods
  */

  /**
   * main deid transform.
   * @param job the job definition
   * @param dlpProjectId Google project id for DLP api request
   * @throws IOException throws from DLP api call
   */
  public DeidTransform(DeidJob job, String dlpProjectId) throws IOException {
    this.job = job;
    if (job.googleDlpEnabled) {
      dlpTransform = new DlpTransform(job, dlpProjectId);
    } else {
      dlpTransform = null;
    }
  }

  static StanfordCoreNLP setupCoreNlpPipeline() {
    Properties serProps = new Properties();
    serProps.setProperty("loadClassifier","classifiers/english.all.3class.distsim.crf.ser.gz");
    //serProps.setProperty("loadClassifier","classifiers/english.conll.4class.distsim.crf.ser.gz");

    serProps.setProperty("annotators", "tokenize,ssplit,pos,ner");
    //serProps.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
    serProps.setProperty("ner.applyFineGrained", "false");
    serProps.setProperty("ner.additional.regexner.ignorecase", "true");
    serProps.setProperty("ner.applyNumericClassifiers", "false");
    serProps.setProperty("ner.useSUTime", "false");
    serProps.setProperty("threads", "1");

    return new StanfordCoreNLP(serProps);
  }

  /**
   * reset CoreNLP pipeline.
   */
  public static void resetNer() {
    if (pipeline != null) {
      pipeline = setupCoreNlpPipeline();
    }

  }

  /**
   * use CoreNLP NER to find name and location entities.
   * @param text input text
   * @param foundNameItems store finding for name entities
   * @param foundLocationItems  store finding for location entities
   */
  public static void findEntiesWithNer(String text,
                                        List<AnonymizedItemWithReplacement> foundNameItems,
                                       List<AnonymizedItemWithReplacement> foundLocationItems) {

    if (text == null || text.length() == 0) {
      return;
    }

    /* with CoreNLP pipeline */
    if (pipeline == null) {
      pipeline = setupCoreNlpPipeline();
    }

    /* or use NCC */
    //if (ncc == null) {
    //  ncc = setupNcc();
    //}

    CoreDocument doc = new CoreDocument(text);

    pipeline.annotate(doc);

    if (doc.entityMentions() != null) {
      for (CoreEntityMention em : doc.entityMentions()) {
        if (em.entityType().equals("PERSON")) {
          String word = Utility.removeTitleFromName(em.text());
          if (word.length() == 0) {
            continue;
          }
          AnonymizedItemWithReplacement item = new AnonymizedItemWithReplacement(
              em.text(),
              em.charOffsets().first(), em.charOffsets().second(),
              null, "deid-person-ner", "ner-" + em.entityType());

          foundNameItems.add(item);
          //log.info("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
        } else if (em.entityType().equals("LOCATION")) {
          String word = em.text();
          if (word.length() == 0) {
            continue;
          }
          AnonymizedItemWithReplacement item = new AnonymizedItemWithReplacement(
              em.text(),
              em.charOffsets().first(), em.charOffsets().second(),
              null, "deid-location-ner", "ner-" + em.entityType());
          foundLocationItems.add(item);
          //log.info("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
        }
      }
    }
  }

  @Override
  public PCollection<DeidResult> expand(PCollection<String> input) {
    PCollection<DeidResult> deidText = input.apply(ParDo.of(new DeidFn()));
    return deidText;
  }

  public class DeidFn extends DoFn<String, DeidResult> {

    public DeidFn() {

    }

    /**
     * process element in the tranform.
     * @param context process context
     * @throws SQLException throws exception from surrogate
     * @throws IOException throws from surrogate or DLP
     */
    @ProcessElement
    public void processElement(ProcessContext context)
        throws SQLException, IOException, IllegalAccessException {

      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      JsonNode node = mapper.readTree(context.element());

      String[] noteIdFields = job.getTextIdFields().get().split(",");
      String[] noteIds = new String[noteIdFields.length];
      for (int i = 0;i < noteIdFields.length;i++) {
        if (node.has(noteIdFields[i])) {
          noteIds[i] = node.get(noteIdFields[i]).asText();
        } else {
          throw new IOException("input data does not have field " + noteIdFields[i]);
        }
      }
      String[] textFields = job.getTextFields().get().replaceAll(" ","").split(",");
      DeidResult deidResult = new DeidResult(noteIdFields,noteIds,textFields);

      for (int textIndex = 0;textIndex < textFields.length;textIndex++) {
        try {

          String orginalText = node.has(textFields[textIndex])
              ? node.get(textFields[textIndex]).asText() : null;

          List<AnonymizedItemWithReplacement> items = new ArrayList<>();

          if (orginalText == null || orginalText.length() == 0) {
            deidResult.addData(DeidResultProc.STATS_CNT_DLP + textFields[textIndex],0);
            deidResult.addData(DeidResultProc.STATS_CNT_DEID + textFields[textIndex],0);
            continue;
          }

          orginalText = orginalText.replaceAll("�", "\n\r"); //� "\ufffd"

          deidResult.addData(DeidResultProc.TEXT_ORGINAL + textFields[textIndex],orginalText);

          List<AnonymizedItemWithReplacement> foundNerNameItems = new ArrayList<>();
          List<AnonymizedItemWithReplacement> foundNerLocationItems = new ArrayList<>();

          final long startTs = new Date().getTime();
          //log.info("start process id:" + Arrays.toString(noteIds));
          //preprocess with CoreDLP to find names and locations
          //with CoreNLP pipeline
          if (job.nerEnabled) {
            //log.info("Start NER");
            try {
              findEntiesWithNer(orginalText, foundNerNameItems, foundNerLocationItems);
            } catch (Exception e) {
              log.error("NER ERROR for text id: " + Arrays.toString(noteIds),e);
              resetNer();
            }
            //log.info("done NER");
          }

          //stage one : Google DLP
          if (dlpTransform != null && orginalText != null) {
            dlpTransform.dlpDeidRequest(orginalText, textFields[textIndex], deidResult);
            log.info(String.format(Locale.ROOT,"DLP result:[%s]",
                deidResult.getDataAsString(DeidResultProc.TEXT_DLP + textFields[textIndex])));
          }

          //stage two : Stanford DeID
          String jitterSeed = (job.getDateJitterSeedField() != null
              && node.has(job.getDateJitterSeedField()))
              ? node.get(job.getDateJitterSeedField()).asText() : null;

          for (DeidSpec spec : job.getSpec()) {

            AnonymizerProcessor anonymizer = null;
            boolean scanCommonWord = false;
            boolean matchWholeWord = false;
            int minimumWordLength = 0;
            switch (spec.action) {
              case replace_strictly_with:
                scanCommonWord = true;
                matchWholeWord = false;
                // fallthru
                // fall through
              case replace_minimumlengthword_with:
                try {
                  if (spec.actionParam != null && spec.actionParam.length > 1) {
                    minimumWordLength = Integer.parseInt(spec.actionParam[1]);
                  } else {
                    minimumWordLength = MINIMUM_WORD_LENGTH;
                  }
                } catch (Exception e) {
                  log.warn(e.getMessage(),e);
                  minimumWordLength = MINIMUM_WORD_LENGTH;
                }
                matchWholeWord = true;
                // fallthru
                // fall through
              case replace_with:
                List<String> words = new ArrayList<>();

                for (String field : spec.fields) {
                  if (node.has(field)) {
                    if (matchWholeWord) {
                      String v = node.get(field).asText();
                      if (v != null && !v.toLowerCase(Locale.ROOT).equals("null")
                          && (scanCommonWord || !ignoreWords.contains(v.toLowerCase(Locale.ROOT)))
                          && (v.length() >= minimumWordLength)) {
                        words.add(v);
                      }
                    } else {
                      String[] fieldValues = node.get(field).asText().split(" |,|-");
                      for (String v : fieldValues) {
                        if (v != null && !v.toLowerCase(Locale.ROOT).equals("null")
                            && (scanCommonWord || !ignoreWords.contains(v.toLowerCase(Locale.ROOT)))
                            && (v.length() >= minimumWordLength)) {
                          words.add(v);
                        }
                      }
                    }
                  }
                }
                if (words.size() == 0) {
                  continue;
                }
                String[] wordArray = new String[words.size()];
                anonymizer = new TokenArrayAnonymizer(words.toArray(wordArray),
                  spec.actionParam[0], spec.itemName);

                break;
              case general:

                anonymizer = new GeneralAnonymizer();
                break;

              case surrogate_address:

                Address[] address = null;
                if (spec.actionParamMap != null) {
                  String fieldAddress1 = (spec.actionParamMap.containsKey("f_address_1")
                      && node.get(spec.actionParamMap.get("f_address_1")) != null)
                      ? node.get(spec.actionParamMap.get("f_address_1")).asText() : null;
                  String fieldAddress2 = (spec.actionParamMap.containsKey("f_address_2")
                      && node.get(spec.actionParamMap.get("f_address_2")) != null)
                      ? node.get(spec.actionParamMap.get("f_address_2")).asText() : null;
                  String fieldCity = (spec.actionParamMap.containsKey("f_city")
                      && node.get(spec.actionParamMap.get("f_city")) != null)
                      ? node.get(spec.actionParamMap.get("f_city")).asText() : null;
                  String fieldZipCode = (spec.actionParamMap.containsKey("f_zip")
                      && node.get(spec.actionParamMap.get("f_zip")) != null)
                      ? node.get(spec.actionParamMap.get("f_zip")).asText() : null;
                  String fieldStateCode = (spec.actionParamMap.containsKey("f_state_code")
                      && node.get(spec.actionParamMap.get("f_state_code")) != null)
                      ? node.get(spec.actionParamMap.get("f_state_code")).asText() : null;


                  if (fieldAddress1 == null && fieldAddress2 == null
                      && fieldCity == null && fieldZipCode == null) {
                    continue;
                  }

                  address = new Address[]{new Address(fieldAddress1,
                    fieldAddress2, null, null, fieldCity, fieldZipCode, fieldStateCode)};

                  anonymizer =  new LocationSurrogate(address, "location", null,  false);

                } else {
                  anonymizer =  new LocationSurrogate(address, "location",
                    foundNerLocationItems,  true);

                }

                break;
              case surrogate_name:
                if (spec.actionParamMap != null) {
                  //{"format":"L","f_zip":"zip","f_gender":"","f_dob":"birth_date"}
                  String[] nameF = spec.actionParamMap.get("format").split(" |,|-");
                  words = new ArrayList<>();
                  List<NameType> nameTypes = new ArrayList<>();
                  for (String field : spec.fields) {
                    if (node.has(field)) {
                      String[] fieldValues = node.get(field).asText().split(" |,|-");
                      int pos = 0;
                      NameType nameType = null;
                      for (String v : fieldValues) {
                        if (v != null && !v.toLowerCase(Locale.ROOT).equals("null")
                            && (!ignoreWords.contains(v.toLowerCase(Locale.ROOT)))
                            && (v.length() >= minimumWordLength)) {
                          words.add(v);
                          if (pos < nameF.length) {
                            switch (nameF[pos].toUpperCase(Locale.ROOT)) {
                              case "L":
                              case "M":
                                nameType = NameType.Lastname;
                                nameTypes.add(nameType);
                                break;
                              case "F":
                                nameType = NameType.Firstname;
                                nameTypes.add(nameType);
                                break;
                              default:
                                nameTypes.add(null);
                            }
                          } else {
                            nameTypes.add(nameType);
                          }
                          pos++;
                        }
                      }
                    }
                  }
                  if (words.size() == 0) {
                    continue;
                  }
                  wordArray = new String[words.size()];
                  NameType[] dictionary = new NameType[nameTypes.size()];
                  NameSurrogate.Builder builder = new NameSurrogate.Builder();
                  builder.withNames(words.toArray(wordArray))
                    .withAnonymizerType(spec.itemName)
                    .withDic(nameTypes.toArray(dictionary));

                  if (spec.actionParamMap.containsKey("f_zip")
                      && spec.actionParamMap.get("f_zip").length() > 0
                      && node.get(spec.actionParamMap.get("f_zip")) != null) {
                    String zipCode = node.get(spec.actionParamMap.get("f_zip")).asText();
                    builder.withZipCode(zipCode);

                  }
                  if (spec.actionParamMap.containsKey("f_gender")
                      && spec.actionParamMap.get("f_gender").length() > 0
                      && node.get(spec.actionParamMap.get("f_gender")) != null) {
                    String gender = node.get(spec.actionParamMap.get("f_gender")).asText();
                    builder.withGender(gender);
                  }

                  if (spec.actionParamMap.containsKey("f_dob")
                      && spec.actionParamMap.get("f_dob").length() > 0
                      && node.get(spec.actionParamMap.get("f_dob")) != null) {
                    String dobStr = node.get(spec.actionParamMap.get("f_dob")).asText();
                    if (dobStr != null && dobStr.length() > 0 && node.has(dobStr)) {
                      Date dob = new Date(node.get(dobStr).asLong());
                      builder.withDob(dob);
                    }

                  }
                  anonymizer = builder.build();
                } else {
                  NameSurrogate.Builder builder = new NameSurrogate.Builder();
                  builder.withAnonymizerType(spec.itemName)
                    .withKnownNameItems(foundNerNameItems);

                  anonymizer = builder.build();
                }


                break;

              case remove_mrn:
                anonymizer = new MrnAnonymizer("[MRN]", spec.itemName);
                break;

              case remove_age:
                anonymizer = new AgeAnonymizer("[AGE]", spec.itemName);
                break;
              case jitter_date_from_field:
                int jitter = 0;
                for (String field : spec.fields) { //take only the first field
                  if (node.has(field)) {
                    jitter = node.get(field).asInt();
                  }
                }
                anonymizer = new DateAnonymizer(jitter, spec.itemName, spec.actionParam[0]);
                break;
              case jitter_date_randomly:
                anonymizer = new DateAnonymizer(
                    Utility.jitterHash(jitterSeed, spec.actionParam[0],
                        job.getDateJitterRange()), spec.itemName, spec.actionParam[1]);
                break;
              case jitter_birth_date:
                for (String field : spec.fields) { //take only the first field
                  if (node.has(field)) {

                    final Date bday = new Date(node.get(field).asLong());
                    anonymizer = new DateAnonymizer(bday, spec.itemName);
                    break;
                  }
                }
                if (anonymizer == null) {
                  continue;
                }

                break;

              default:
                continue;
            }
            anonymizer.find(orginalText, items);
          }

          ObjectMapper resultMapper = new ObjectMapper();
          mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

          String resultText = DeidResultProc.applyChange(items, orginalText);
          String stats = items.size() > 0 ? resultMapper.writeValueAsString(items) : "[]";
          deidResult.addData(DeidResultProc.TEXT_DEID + textFields[textIndex], resultText);
          deidResult.addData(DeidResultProc.STATS_DEID + textFields[textIndex], stats);
          deidResult.addData(DeidResultProc.STATS_CNT_DEID + textFields[textIndex], items.size());

        } catch (IOException e) {
          log.error(e.getMessage(),e);
        }
        //end of text deid
      }

      if (deidResult != null) {
        context.output(deidResult);
      }

    }
  }
}
