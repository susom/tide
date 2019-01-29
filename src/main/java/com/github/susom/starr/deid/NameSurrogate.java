package com.github.susom.starr.deid;

import com.github.susom.starr.Utility;

import edu.stanford.irt.core.facade.AnonymizedItem;
import edu.stanford.irt.core.facade.Anonymizer;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Name identification and surrogate
 * @author wenchengl
 */

public class NameSurrogate implements Anonymizer {

  private static final Logger log = LoggerFactory.getLogger(NameSurrogate.class);
  final private static String defaultReplacementWord = "[REMOVED]";

  final private static AtomicBoolean dbLoaded = new AtomicBoolean(false);



  private java.sql.Connection hsqlcon = null;

  final private static String searchNameQuery = "select name, sex from us_firstname where name = ? limit 1";
  final private static String findFirstNameRangeBySexI12Query = "select range from us_firstname_range where sex = ? and name_i1 = ? and name_i2 = ?  limit 1";
  final private static String findFirstNameBySexI12SeqQuery = "select name,sex from us_firstname where sex = ? and name_i1 = ? and name_i2 = ? and seq = ? limit 1";


  final private static String findLastNameRangeBySexI12Query = "select range from us_lastname_range where name_i1 = ? and name_i2 = ?  limit 1";
  final private static String findLastNameByI12SeqQuery = "select name from us_lastname where name_i1 = ? and name_i2 = ? and seq = ? limit 1";

  private Random random = new Random(UUID.randomUUID().hashCode());

  private String[] names;
  private String[] replacements;
  private String anonymizerType;

  private List<AnonymizedItem> knownNameItems;
  private String zipCode;
  private String gender;
  private Date dob;

  private NameDictionay[] dic;

  public enum NameDictionay{
    Firstname, Lastname
  }

  final public static List<Quintet<String,String, String, String,String>> hsqlTables = new ArrayList<>();
  static {

    //firstname with three char index : '82,79,76,59,F,Rolonda,48'
    hsqlTables.add(Quintet.with(
      "us_firstname","name_i1,name_i2,name_i3,seq,sex,name,occurrences","sex,name_i1,name_i2,seq",
      "create table us_firstname (name_i1 integer,name_i2 integer,name_i3 integer,seq integer, sex varchar(20), name varchar(80), occurrences integer)",
      "firstname_g3.csv"));

    //firstname range for each group (i1,i2)  : '65,65,M,43'
    hsqlTables.add(Quintet.with(
      "us_firstname_range","name_i1,name_i2,sex,range","sex,name_i1,name_i2",
      "create table us_firstname_range (name_i1 integer,name_i2 integer, sex varchar(20), range integer)",
      "firstname_g_range.csv"));


    //lastname with three char index NAME_I1,NAME_I2,NAME_I3,SEQ,NAME,OCCURRENCES: '76,65,70,2048,LAFANS,1'
    hsqlTables.add(Quintet.with(
      "us_lastname","name_i1,name_i2,name_i3,seq,name,occurrences","name_i1,name_i2,seq",
      "create table us_lastname (name_i1 integer,name_i2 integer,name_i3 integer,seq integer,name varchar(80),occurrences integer)",
      "lastname_g3.csv"));
    //lastname range for each group (i1,i2)  : '65,65,43'
    hsqlTables.add(Quintet.with(
      "us_lastname_range","name_i1,name_i2,range","name_i1,name_i2",
      "create table us_lastname_range (name_i1 integer,name_i2 integer, range integer)",
      "lastname_g_range.csv"));

  }

  /**
   *
   * @param names array of input names
   * @param dictionary is Firstname or Lastname for each element
   */
  public NameSurrogate(String[] names, String anonymizerType, NameDictionay[] dictionary)
    throws SQLException {
    this.anonymizerType = anonymizerType;
    this.names = names;
    this.dic = dictionary;

    synchronized (dbLoaded){
      if(!dbLoaded.get()){
        java.sql.Connection _hsqlcon = Utility.getHsqlConnection();
        for(Quintet<String, String, String,String,String> tableDef : hsqlTables){
          Utility.loadHsqlTable(_hsqlcon, tableDef);
        }
        _hsqlcon.close();
        dbLoaded.set(true);
      }
    }
  }

  private void getConnectIfNeeded() throws SQLException {
    if(hsqlcon==null){
      this.hsqlcon = Utility.getHsqlConnection();
    }
  }

  @Override
  public String scrub() {
    return null;
  }

  @Override
  public String scrub(String text, List<AnonymizedItem> list) {
    try {
      getConnectIfNeeded();
    } catch (SQLException e) {
      log.error(e.getMessage(),e);
    }

    getNameSurrogate();

    String out = text;

    for (int i = 0; names!=null && i< names.length; i++){
      // Ignore ones has shorter than 3 character length
      if ((names[i] == null) || (names[i].trim().length() <= 2)) {
        continue;
      }
      Matcher r = Pattern.compile("\\b(" + Utility.regexStr(names[i]) + ")\\b", Pattern.CASE_INSENSITIVE).matcher(out);
      while(r.find()) {
        AnonymizedItem ai = new AnonymizedItem(out.substring(r.start(),r.end()), this.anonymizerType);
        ai.setStart(r.start()); ai.setEnd(r.end());
//        log.debug("found:" + ai.getWord() + " at " + r.start() + "-" + r.end());
        list.add(ai);
      }
      try{
        out = r.replaceAll(replacements[i]);

      }catch (IllegalStateException | IllegalArgumentException | IndexOutOfBoundsException | NullPointerException e){
        log.error(e.getMessage(),e);
      }

    }

    //process provided known name item discovered by NLP
    if (this.knownNameItems!=null){
      Set<String> uniqueWords = new HashSet();
      this.knownNameItems.forEach(i->{list.add(i);uniqueWords.add(i.getWord());});

      for(String word: uniqueWords){
        Matcher r = Pattern.compile("\\b(" + Utility.regexStr(word) + ")\\b", Pattern.CASE_INSENSITIVE).matcher(out);
        try {
          out = r.replaceAll(getLastNameSurrogate(word));
        } catch (SQLException e) {
          log.error(e.getMessage(),e);
        }
      }
    }
    //session.close();

    try {
      hsqlcon.close();
    } catch (SQLException e) {
      log.info(e.getMessage());
    }

    return out;
  }


  private void getNameSurrogate()  {
    if(names==null){
      return;
    }
    replacements = new String[names.length];
    for(int i = 0; i < names.length; i++){
      try {
        String repl =
          dic[i] != null && dic[i] == NameDictionay.Lastname ? getLastNameSurrogate(names[i])
            : getFirstNameSurrogate(names[i]);
        replacements[i] = repl;
      }catch (SQLException e){
        log.error(e.getMessage(),e);
        replacements[i] = defaultReplacementWord;
      }
    }
  }

  static Map<String,Integer> lnCache = new HashMap<>();
  static Map<String,Integer> fnCache = new HashMap<>();

  int getFirstNameRange(String sex, int char1, int char2) throws SQLException {
    String key = "FN_"+sex+"_"+char1+"_"+char2;
    if(fnCache.containsKey(key) && fnCache.get(key)!=null){
      return fnCache.get(key);
    }

    getConnectIfNeeded();
    PreparedStatement findGroupRange = hsqlcon.prepareStatement(findFirstNameRangeBySexI12Query);

    ResultSet rangeRs = null;
    int range = -1;
    findGroupRange.setString(1,sex);
    findGroupRange.setInt(2,char1);
    findGroupRange.setInt(3,char2);
    rangeRs = findGroupRange.executeQuery();
    if(rangeRs!=null && rangeRs.next()){
      range = rangeRs.getInt("range");
    }else{
      range = 0;
    }

//    log.info(String.format("found match for %s%s (%s%s), range:%s", char1, char2,  (char)char1, (char)char2, range));
    fnCache.put(key, range);
    rangeRs.close();
    findGroupRange.close();
    return range;
  }


  int getLastNameRange(int char1, int char2) throws SQLException {
    String key = "LN_"+char1+"_"+char2;
    if(lnCache.containsKey(key) && lnCache.get(key)!=null){
      return lnCache.get(key);
    }

    getConnectIfNeeded();
    PreparedStatement findGroupRange = hsqlcon.prepareStatement(findLastNameRangeBySexI12Query);
    ResultSet rangeRs = null;
    int range = -1;
    findGroupRange.setInt(1,char1);
    findGroupRange.setInt(2,char2);
    rangeRs = findGroupRange.executeQuery();

    if(rangeRs!=null && rangeRs.next()){
      range = rangeRs.getInt("range");
    }else{
//      log.info(String.format("did not find match for %s %s, try another random pair", char1, char2));
      range = 0;
    }


//    log.info(String.format("found match for %s%s (%s%s), range:%s", char1, char2,  (char)char1, (char)char2, range));
    lnCache.put(key, range);
    rangeRs.close();
    findGroupRange.close();
    return range;
  }

  String getLastNameSurrogate(String name) throws SQLException {

    org.javatuples.Pair<Integer,Integer> chars = Utility.getRandomChars(name);

    int range = getLastNameRange(chars.getValue0(),chars.getValue1());
    while(range == 0){
//      log.info(String.format("did not find match for %s %s, try another random pair", chars.getValue0(), chars.getValue1()));
      chars = Utility.getRandomChars(name);
      range = getLastNameRange(chars.getValue0(),chars.getValue1());
    }

    java.sql.ResultSet findReplacementRs = null;
    getConnectIfNeeded();
    PreparedStatement stmt = hsqlcon.prepareStatement(findLastNameByI12SeqQuery);
    stmt.setInt(1,chars.getValue0());
    stmt.setInt(2,chars.getValue1());
    stmt.setInt(3,(random.nextInt(Math.min(range, 10))+1));
    findReplacementRs = stmt.executeQuery();

    while(findReplacementRs.next()) {
      String replacementName = findReplacementRs.getString("name");
      findReplacementRs.close();
      return StringUtils.capitalize(replacementName.toLowerCase());
    }

    findReplacementRs.close();
    return defaultReplacementWord;

  }


  String getFirstNameSurrogate(String name) throws SQLException {
    String out;

    org.javatuples.Pair<Integer,Integer> chars = Utility.getRandomChars(name);
    String sex = "F";

    java.sql.ResultSet findReplacementRs = null;

    getConnectIfNeeded();
    PreparedStatement stmt = hsqlcon.prepareStatement(searchNameQuery);
    stmt.setString(1,name);
    java.sql.ResultSet findSenderRs =  stmt.executeQuery();

    if(findSenderRs.next()){
      sex = findSenderRs.getString("sex");
//      log.info(String.format("found name:%s sex:%s", name, sex));
    }

    stmt.close();

    int range = getFirstNameRange(sex, chars.getValue0(), chars.getValue1());
    while(range == 0){
//      log.info(String.format("did not find match for %s %s, try another random pair", chars.getValue0(), chars.getValue1()));
      chars = Utility.getRandomChars(name);
      range = getFirstNameRange(sex, chars.getValue0(), chars.getValue1());
    }
    stmt = hsqlcon.prepareStatement(findFirstNameBySexI12SeqQuery);
    stmt.setString(1,sex);
    stmt.setInt(2,chars.getValue0());
    stmt.setInt(3,chars.getValue1());
    stmt.setInt(4,(random.nextInt(Math.min(range, 10))+1));
    findReplacementRs = stmt.executeQuery();

    if(findReplacementRs.next()) {
      String replacement = StringUtils.capitalize(findReplacementRs.getString("name").toLowerCase());
      findReplacementRs.close();
      stmt.close();
      return replacement;
    }
    findReplacementRs.close();
    stmt.close();
    log.warn(String.format("failed to find match for range:%s for [%s][%s] ", range, chars.getValue0(), chars.getValue1()));
    return defaultReplacementWord;
  }


  public static final class Builder {

    private String[] names;
    private String anonymizerType;
    private String zipCode;
    private String gender;
    private Date dob;
    private NameDictionay[] dic;
    private List<AnonymizedItem> knownNameItems;

    public Builder() {
    }

    public Builder withNames(String[] names) {
      this.names = names;
      return this;
    }

    public Builder withAnonymizerType(String anonymizerType) {
      this.anonymizerType = anonymizerType;
      return this;
    }

    public Builder withZipCode(String zipCode) {
      this.zipCode = zipCode;
      return this;
    }

    public Builder withGender(String gender) {
      this.gender = gender;
      return this;
    }

    public Builder withDob(Date dob) {
      this.dob = dob;
      return this;
    }

    public Builder withDic(NameDictionay[] dic) {
      this.dic = dic;
      return this;
    }

    public Builder withKnownNameItems(List<AnonymizedItem> knownNameItems){
      this.knownNameItems = knownNameItems;
      return this;
    }

    public NameSurrogate build() throws IOException, SQLException {
      NameSurrogate nameSurrogate = new NameSurrogate(names, anonymizerType, null);
      nameSurrogate.zipCode = this.zipCode;
      nameSurrogate.gender = this.gender;
      nameSurrogate.dic = this.dic;
      nameSurrogate.dob = this.dob;
      nameSurrogate.knownNameItems = this.knownNameItems;
      if(nameSurrogate.knownNameItems == null){
        nameSurrogate.knownNameItems = new ArrayList<>();
      }
      return nameSurrogate;
    }
  }
}
