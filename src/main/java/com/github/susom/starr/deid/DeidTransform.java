package com.github.susom.starr.deid;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.susom.starr.Utility;
import com.github.susom.starr.deid.LocationSurrogate.Address;
import com.github.susom.starr.deid.NameSurrogate.NameDictionay;
import edu.stanford.irt.core.facade.*;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.sql.SQLException;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


/**
 * Deid Transform
 * @author wenchengl
 */

public class DeidTransform
  extends PTransform<PCollection<String>,
      PCollection<DeidResult.DeidText>> {


  public static final TupleTag<String> fullResultTag = new TupleTag<String>(){};
  public static final TupleTag<String> statGlobalStage1Tag = new TupleTag<String>(){};
  public static final TupleTag<String> statGlobalStage2Tag = new TupleTag<String>(){};
  public static final TupleTag<String> statPerTextStage1Tag = new TupleTag<String>(){};
  public static final TupleTag<String> statPerTextStage2Tag = new TupleTag<String>(){};

  final public static int MINIMUM_WORD_LENGTH = 3;

  final static private String wordIgnoreFile = "wordIgnore.txt";
  final static private HashSet<String> ignoreWords = new HashSet<>();

  static {
    Utility.loadFileToMemory(wordIgnoreFile, ignoreWords);
  }

  private final DeidJob job;
  private final DlpTransform dlpTransform;

  private static final Logger log = LoggerFactory.getLogger(DeidTransform.class);

  static StanfordCoreNLP pipeline = null;
//  static NERClassifierCombiner ncc = null; //TODO need to compare performance difference between these two methods


  public DeidTransform(DeidJob job, String dlpProjectId) throws IOException {
    this.job = job;
    if(job.googleDlpEnabled){
      dlpTransform = new DlpTransform(job, dlpProjectId);
    }else {
      dlpTransform = null;
    }
  }

  static StanfordCoreNLP setupCoreNlpPipeline(){
    Properties serProps = new Properties();
    serProps.setProperty("loadClassifier","classifiers/english.all.3class.distsim.crf.ser.gz");
//    serProps.setProperty("loadClassifier","classifiers/english.conll.4class.distsim.crf.ser.gz");

    serProps.setProperty("annotators", "tokenize,ssplit,pos,ner");
//    serProps.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
    serProps.setProperty("ner.applyFineGrained", "false");
    serProps.setProperty("ner.additional.regexner.ignorecase", "true");
    serProps.setProperty("ner.applyNumericClassifiers", "false");
    serProps.setProperty("ner.useSUTime", "false");
    serProps.setProperty("threads", "1");




    return new StanfordCoreNLP(serProps);
  }



  public static void resetNer(){
    if(pipeline != null){
      pipeline = setupCoreNlpPipeline();
    }

  }
  public static void findEntiesWithNer(String text, List<AnonymizedItem> foundNameItems, List<AnonymizedItem> foundLocationItems){

    if(text == null || text.length()==0){
      return;
    }


    /* with CoreNLP pipeline */
    if(pipeline == null){
      pipeline = setupCoreNlpPipeline();
    }

    /* or use NCC */
//    if (ncc == null){
//      ncc = setupNcc();
//    }

    CoreDocument doc = new CoreDocument(text);

    pipeline.annotate(doc);

    if(doc.entityMentions()!=null ){
      for (CoreEntityMention em : doc.entityMentions()) {
        if (em.entityType().equals("PERSON")) {
          String word = Utility.removeTitleFromName(em.text());
          if (word.length() == 0) {
            continue;
          }

          AnonymizedItem item = new AnonymizedItem(em.text(), "ner-" + em.entityType());
          item.setStart(em.charOffsets().first);
          item.setEnd(em.charOffsets().second);
          foundNameItems.add(item);
//          log.info("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
        } else if (em.entityType().equals("LOCATION")) {
          String word = em.text();
          if (word.length() == 0) {
            continue;
          }
          AnonymizedItem item = new AnonymizedItem(em.text(), "ner-" + em.entityType());
          item.setStart(em.charOffsets().first);
          item.setEnd(em.charOffsets().second);
          foundLocationItems.add(item);
//          log.info("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
        }
      }
    }

  }


  @Override
  public PCollection<DeidResult.DeidText> expand(PCollection<String> input) {
    PCollection<DeidResult.DeidText> deidText = input.apply(ParDo.of(new DeidFn()));
    return deidText;
  }

  public class DeidFn extends DoFn<String, DeidResult.DeidText>{

    final private HashMap<String, Anonymizer> cache = new HashMap<>();
    private String getAnonymizerCacheKey(DeidSpec spec){
      return spec.item_name+"_"+spec.action+"_"+(spec.action_param!=null?Arrays.toString(spec.action_param):"");
    }

    public DeidFn(){
    }

    @ProcessElement
    public void processElement(ProcessContext context)
      throws SQLException, ClassNotFoundException, IOException {


      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      JsonNode node = mapper.readTree(context.element());

      List<AnonymizedItem > items = new ArrayList<>();
      String[] noteIdFields = job.getText_id_field().split(",");
      String[] noteIds = new String[noteIdFields.length];
      for(int i=0;i<noteIdFields.length;i++){
        noteIds[i] = node.get(noteIdFields[i]).asText();
      }

      DeidResult.DeidText deidResult = new DeidResult.DeidText(noteIds);

      String[] textValues = job.getText_field().replaceAll(" ","").split(",");
      for (int textIndex=0;textIndex<textValues.length;textIndex++){
        try {

          String note = node.has(textValues[textIndex])? node.get(textValues[textIndex]).asText():null;

          if(note==null || note.length()==0){
            switch (textIndex){
              case 0:
                deidResult.setStatsCntStage1(0);
                deidResult.setStatsCntStage2(0);
                break;
              case 1:
                deidResult.setStats2CntStage1(0);
                deidResult.setStats2CntStage2(0);
                break;
            }
            continue;
          }




          List<AnonymizedItem> foundNameItems = new ArrayList<>();
          List<AnonymizedItem> foundLocationItems = new ArrayList<>();

          long startTs = new Date().getTime();
//        log.info("start process id:"+Arrays.toString(noteIds));
          //preprocess with CoreDLP to find names and locations
          //with CoreNLP pipeline
          if(job.nerEnabled){
//          log.info("Start NER");
            try {
              findEntiesWithNer(note, foundNameItems, foundLocationItems);
            }catch (Exception e){
              log.error("NER ERROR for text id: "+Arrays.toString(noteIds),e);
              resetNer();
            }
//          log.info("done NER");
          }


          //stage one : Google DLP
          if(dlpTransform!=null && note!=null){
            dlpTransform.dlpDeidRequest(note, deidResult);
            if(deidResult.getTextStage1()!=null) {
              note = deidResult.getTextStage1();
            }else{
              log.error("DLP returned null result for text id: "+Arrays.toString(noteIds));
            }

          }

          //stage two : Stanford DeID
          String jitterSeed = (job.getDate_jitter_seed_field()!=null && node.has(job.getDate_jitter_seed_field()))?node.get(job.getDate_jitter_seed_field()).asText():null;

          for (DeidSpec spec : job.getSpec()){

            Anonymizer anonymizer = null;
            boolean scanCommonWord = false;
            boolean matchWholeWord = false;
            int minimumWordLength = 0;
            switch (spec.action) {
              case replace_strictly_with:
                scanCommonWord = true;
                matchWholeWord = false;
              case replace_minimumlengthword_with:
                try{
                  if(spec.action_param!=null && spec.action_param.length>1){
                    minimumWordLength = Integer.parseInt(spec.action_param[1]);
                  }else
                    minimumWordLength = MINIMUM_WORD_LENGTH;
                }catch (Exception e){
                  e.printStackTrace();
                  minimumWordLength = MINIMUM_WORD_LENGTH;
                }
                matchWholeWord = true;
              case replace_with:
                List<String> words = new ArrayList<>();

                for (String field : spec.fields){
                  if(node.has(field)){
                    if(matchWholeWord){
                      String v = node.get(field).asText();
                      if(v!=null && !v.toLowerCase().equals("null")
                        && (scanCommonWord || !ignoreWords.contains(v.toLowerCase()))
                        && ( v.length() >= minimumWordLength ) ){
                        words.add(v);
                      }
                    }else{
                      String[] fieldValues = node.get(field).asText().split(" |,|-");
                      for (String v : fieldValues) {
                        if(v!=null && !v.toLowerCase().equals("null")
                          && (scanCommonWord || !ignoreWords.contains(v.toLowerCase()))
                          && ( v.length() >= minimumWordLength ) ){
                          words.add(v);
                        }
                      }
                    }
                  }
                }
                if(words.size()==0){
                  continue;
                }
                String[] wordArray = new String[words.size()];
                anonymizer = new ActualNameAnonymizer(words.toArray(wordArray),
                  "["+spec.action_param[0]+"]", spec.item_name);

                break;
              case general:

                anonymizer = new GeneralAnonymizer();
                break;

              case surrogate_address:

                Address[] address = null;
                if(spec.action_param_map!=null){ //{"f_address_1":"tmp_addr_line_1","f_address_2":"tmp_addr_line_2", "f_city":"tmp_city", "f_zip":"tmp_zip"}
                  String f_address_1 = (spec.action_param_map.containsKey("f_address_1") && node.get(spec.action_param_map.get("f_address_1"))!=null)?
                    node.get(spec.action_param_map.get("f_address_1")).asText():null;
                  String f_address_2 = (spec.action_param_map.containsKey("f_address_2") && node.get(spec.action_param_map.get("f_address_2"))!=null)?
                    node.get(spec.action_param_map.get("f_address_2")).asText():null;
                  String f_city = (spec.action_param_map.containsKey("f_city") && node.get(spec.action_param_map.get("f_city"))!=null)?
                    node.get(spec.action_param_map.get("f_city")).asText():null;
                  String f_zipCode = (spec.action_param_map.containsKey("f_zip") && node.get(spec.action_param_map.get("f_zip"))!=null)?
                    node.get(spec.action_param_map.get("f_zip")).asText():null;
                  String f_stateCode = (spec.action_param_map.containsKey("f_state_code") && node.get(spec.action_param_map.get("f_state_code"))!=null)?
                    node.get(spec.action_param_map.get("f_state_code")).asText():null;


                  if(f_address_1==null && f_address_2==null && f_city==null && f_zipCode==null){
                    continue;
                  }

                  address = new Address[]{new Address(f_address_1,
                    f_address_2, null, null, f_city, f_zipCode, f_stateCode)};

                  anonymizer =  new LocationSurrogate( address, "location", null,  false);

                }else{
                  anonymizer =  new LocationSurrogate( address, "location", foundLocationItems,  true);

                }

                break;
              case surrogate_name:
                if(spec.action_param_map!=null){
                  //{"format":"L","f_zip":"zip","f_gender":"","f_dob":"birth_date"}
                  String[] nameF = spec.action_param_map.get("format").split(" |,|-");
                  words = new ArrayList<>();
                  List<NameDictionay> dict = new ArrayList<>();
                  for (String field : spec.fields){
                    if(node.has(field)){
                      String[] fieldValues = node.get(field).asText().split(" |,|-");
                      int pos = 0;
                      NameDictionay lastDict = null;
                      for (String v : fieldValues) {
                        if(v!=null && !v.toLowerCase().equals("null")
                          && (!ignoreWords.contains(v.toLowerCase()))
                          && ( v.length() >= minimumWordLength ) ){
                          words.add(v);
                          if(pos<nameF.length){
                            switch (nameF[pos].toUpperCase()){
                              case "L":
                              case "M":
                                lastDict = NameDictionay.Lastname;
                                dict.add(lastDict);
                                break;
                              case "F":
                                lastDict = NameDictionay.Firstname;
                                dict.add(lastDict);
                                break;
                              default:
                                dict.add(null);
                            }
                          }else{
                            dict.add(lastDict);
                          }
                          pos++;
                        }
                      }
                    }
                  }
                  if(words.size()==0){
                    continue;
                  }
                  wordArray = new String[words.size()];
                  NameDictionay[] dictionary = new NameDictionay[dict.size()];
                  NameSurrogate.Builder builder = new NameSurrogate.Builder();
                  builder.withNames(words.toArray(wordArray))
                    .withAnonymizerType(spec.item_name)
                    .withDic(dict.toArray(dictionary));

                  if(spec.action_param_map.containsKey("f_zip")  && spec.action_param_map.get("f_zip").length()>0 && node.get(spec.action_param_map.get("f_zip"))!=null ){
                    String zipCode = node.get(spec.action_param_map.get("f_zip")).asText();
                    builder.withZipCode(zipCode);

                  }
                  if(spec.action_param_map.containsKey("f_gender") && spec.action_param_map.get("f_gender").length()>0 && node.get(spec.action_param_map.get("f_gender"))!=null){
                    String gender = node.get(spec.action_param_map.get("f_gender")).asText();
                    builder.withGender(gender);
                  }

                  if(spec.action_param_map.containsKey("f_dob") && spec.action_param_map.get("f_dob").length()>0 && node.get(spec.action_param_map.get("f_dob"))!=null){
                    String dobStr = node.get(spec.action_param_map.get("f_dob")).asText();
                    if(dobStr!=null && dobStr.length()>0 && node.has(dobStr)){
                      Date dob = new Date(node.get(dobStr).asLong());
                      builder.withDob(dob);
                    }

                  }
                  anonymizer = builder.build();
                }else {
                  NameSurrogate.Builder builder = new NameSurrogate.Builder();
                  builder.withAnonymizerType(spec.item_name)
                    .withKnownNameItems(foundNameItems);

                  anonymizer = builder.build();
                }


                break;

              case remove_mrn:
                anonymizer = cache.computeIfAbsent(getAnonymizerCacheKey(spec), (k)->{
                  return new MrnAnonymizer("[MRN]", spec.item_name);
                });
                break;

              case remove_age:
                anonymizer = cache.computeIfAbsent(getAnonymizerCacheKey(spec), (k)-> {
                  return new AgeAnonymizer("[AGE]", spec.item_name);
                });
                break;
              case jitter_date:
                anonymizer = cache.computeIfAbsent(getAnonymizerCacheKey(spec), (k)-> {
                  return new DateAnonymizer(
                    Utility.jitterHash(jitterSeed, spec.action_param[0], job.getDate_jitter_range()), spec.item_name);
                });
                break;
              case jitter_birth_date:
                for (String field : spec.fields){ //take only the first field
                  if(node.has(field)){

                    final Date bday = new Date(node.get(field).asLong());
                    anonymizer = cache.computeIfAbsent(getAnonymizerCacheKey(spec), (k)-> {
                      return new DateAnonymizer(bday, spec.item_name);
                    });
                    break;
                  }
                }
                if(anonymizer==null){
                  continue;
                }

                break;

              default:
                continue;
            }
            //TODO AnonymizedItem has inaccurate range because input is previous process result from earlier srub. Need to separate find and replacing
//          log.info("scrub "+ anonymizer.getClass());

            long anoymizerStartTs = new Date().getTime();

            note = anonymizer.scrub(note, items);
//          log.info("done scrub "+ anonymizer.getClass());
            long anoymizerTimeTook = (new Date().getTime() - anoymizerStartTs)/1000;

            if(anoymizerTimeTook>3){
              log.warn("end process id:"+Arrays.toString(noteIds)+" with "+anonymizer.getClass() +" time:"+anoymizerTimeTook +"s SLOW!");
            }
          }

          long timeTook = (new Date().getTime() - startTs)/1000;
          if(timeTook>5){
            log.warn("end process id:"+Arrays.toString(noteIds) +" time:"+timeTook +"s SLOW!");
          }
//        else{
//          log.info("end process id:"+Arrays.toString(noteIds) +" time:"+timeTook +"s");
//        }
          if((noteIds[0].equals("4136139") && noteIds[1].equals("425")) || (noteIds[0].equals("2458703") && noteIds[1].equals("1331"))){
            log.info("process id:"+Arrays.toString(noteIds));
          }
          log.info("process id:"+Arrays.toString(noteIds));

          ObjectMapper resultMapper = new ObjectMapper();
          mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);


          String stats = items.size()>0?resultMapper.writeValueAsString(items):"[]";
          switch (textIndex){
            case 0:
              deidResult.setText_1(note);
              deidResult.setTextStage2(note);
              deidResult.setStatsStage2(stats);
              deidResult.setStatsCntStage2(items.size());
              break;
            case 1:
              deidResult.setText_2(note);
              deidResult.setText2Stage2(note);
              deidResult.setStats2Stage2(stats);
              deidResult.setStats2CntStage2(items.size());
              break;
          }



        } catch (IOException e) {
          e.printStackTrace();
        }


        //end of text deid
      }
      if (deidResult.getText_1()!=null || deidResult.getText_2()!=null){
        context.output(deidResult);
      }


    }
  }

}
