package com.github.susom.starr.deid;

import com.github.susom.starr.Utility;
import edu.stanford.irt.core.facade.AnonymizedItem;
import edu.stanford.irt.core.facade.Anonymizer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Quintet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Location identification and surrogate
 * @author wenchengl
 */

public class LocationSurrogate implements Anonymizer {

  final private static String defaultReplacementWord = "[REMOVED]";

  final private static AtomicBoolean dbLoaded = new AtomicBoolean(false);

  private java.sql.Connection hsqlcon = null;


  final private static String findAddressRangeByI12Query = "select range from us_address_range where name_i1 = ? and name_i2 = ?  limit 1";
  final private static String findAddressByI12Query = "select street_name, street_type, city, zip, statecode from us_address where street_i1 = ? and street_i2 = ? and seq = ? limit 1";


  private static final Logger log = LoggerFactory.getLogger(LocationSurrogate.class);

  static private Random random = new Random(UUID.randomUUID().hashCode());

  private Address[] knownAddr;
  private String anonymizerType;

  private List<AnonymizedItem> knownNameItems;
  private boolean useGeneralLocationMatcher = false;

  final public static List<Quintet<String,String, String, String,String>> hsqlTables = new ArrayList<>();

  static {

    //address with three char index STATECODE,ZIP,STREET_NAME,STREET_I1,STREET_I2,STREET_I3,STREET_TYPE,CITY,SEQ,OCCURRENCES
    //example: NM,87020,E Santa Fe,69,32,83,Ave,Grants,86,2
    hsqlTables.add(Quintet.with(
      "us_address","statecode,zip,street_name,street_i1,street_i2,street_i3,street_type,city,seq,occurrences","street_i1,street_i2,seq",
      "create table us_address (statecode varchar(4),zip varchar(12), street_name varchar(100),street_i1 integer,street_i2 integer,street_i3 integer,street_type varchar(20),city varchar(40),seq integer,occurrences integer)",
      "address_g3.csv"));


    //address range for each group (i1,i2)  : '65,65,43'
    hsqlTables.add(Quintet.with(
      "us_address_range","name_i1,name_i2,range","name_i1,name_i2",
      "create table us_address_range (name_i1 integer,name_i2 integer, range integer)",
      "address_g_range.csv"));


  }

  public LocationSurrogate(Address[] knownAddr, String anonymizerType, List<AnonymizedItem> knownNameItems, boolean useGeneralLocationMatcher)
    throws SQLException {
    this.anonymizerType = anonymizerType;
    this.knownAddr = knownAddr;
    this.knownNameItems = knownNameItems;
    if(this.knownNameItems == null){
      this.knownNameItems = new ArrayList<>();
    }

    this.useGeneralLocationMatcher = useGeneralLocationMatcher;

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


  public static String getSimpleLocationRegex(Address address){
    String separaters = "|";
    StringBuffer sb = new StringBuffer();
    if(address.getStreetNumber()!=null && address.getStreetNumber().length()>0){
      sb.append(separaters).append(address.getStreetNumber().replaceAll("[^a-zA-Z0-9 ]","."));
    }

    if(address.getStreetName()!=null && address.getStreetName().length()>0){
      sb.append(separaters).append(address.getStreetName().replaceAll("[^a-zA-Z0-9 ]","."));
    }

    if(address.getCity()!=null && address.getCity().length()>0){
      sb.append(separaters).append(address.getCity().replaceAll("[^a-zA-Z0-9 ]","."));
    }
    if(address.getZipCode()!=null && address.getZipCode().length()>0){
      sb.append(separaters).append(address.getZipCode().replaceAll("[^a-zA-Z0-9 ]","."));
    }

    if(sb.indexOf(separaters)==0){
      sb.delete(0, separaters.length());
    }else{
      return null;
    }

    return sb.toString();
  }

  @Override
  public String scrub() {
    return null;
  }

  @Override
  public String scrub(String text, List<AnonymizedItem> list) {

    try {
      hsqlcon = Utility.getHsqlConnection();
    } catch (SQLException e) {
      log.error(e.getMessage(),e);
    }

    String out = text;

    HashSet<Integer> posIndex = new HashSet<>();
    HashSet<String> wordChanged = new HashSet<>();

    StringBuffer sb = new StringBuffer();
    for (int i = 0; knownAddr!=null &&  i< knownAddr.length; i++){
      // Ignore ones has shorter than 3 character length, only do simple find and replace
      if ((knownAddr[i] == null) || (knownAddr[i].getStreetNumber()==null ) || (knownAddr[i].getStreetName()==null ) || (knownAddr[i].getStreetName().trim().length() <= 2)) {

        String patternStr= LocationSurrogate.getSimpleLocationRegex(knownAddr[i]);
        if (patternStr != null && !wordChanged.contains(patternStr)){
          out = out.replaceAll(patternStr,defaultReplacementWord);
          wordChanged.add(patternStr);
        }
        continue;
      }
      //do broader find and replace
      Matcher r = Pattern.compile("(?i)\\b(" + Utility.regexStr(knownAddr[i].getStreetNumber()) + ")(\\s*)([N|W|S|E]{0,1})\\W{0,5}\\s*(" + Utility.regexStr(knownAddr[i].getStreetName())
        + "|)\\W{0,5}\\s*(" + Utility.regexStr(knownAddr[i].getStreetType()) + "|)\\W{0,5}\\s*(" + Utility.regexStr(knownAddr[i].getAddressLn2()) + "|)\\W{0,5}\\s*(" + Utility.regexStr(knownAddr[i].getCity()) + "|)\\W{0,5}\\s*(" + Utility.regexStr(knownAddr[i].getStateCode()) + "|\\b)\\W{0,5}(" + Utility.regexStr(knownAddr[i].getZipCode()) + ")\\W{1,5}\\s*\\d{0,5}", Pattern.CASE_INSENSITIVE).matcher(out);


      while(r.find()) {

        if(posIndex.contains(r.start())){
          continue;
        }
        boolean hasStreetNum = false, hasStreet=false, hasCity=false, hasState=false, hasZip=false;

        hasStreetNum = r.groupCount() >=1 && r.group(1)!=null;
        hasStreet = r.groupCount() >=2 && r.group(2)!=null;
        hasCity = r.groupCount() >=3 && r.group(3)!=null;
        hasState = r.groupCount() >=4 && r.group(4)!=null;
        hasZip = r.groupCount() >=5 && r.group(5)!=null;

        String format = createFormatString( hasStreetNum, hasStreet, hasCity,  hasState, hasZip);

        String replacement = getExactLocationSurrogate(format, knownAddr[i].getStreetName(), knownAddr[i].stateCode);

        AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(out.substring(r.start(),r.end()), this.anonymizerType,replacement, "knownAddress");
        posIndex.add(r.start());
        ai.setStart(r.start()); ai.setEnd(r.end());
        log.info("found known address:" + ai.getWord() + " at " + r.start() + "-" + r.end());
        list.add(ai);
        r.appendReplacement(sb, replacement);
      }
      r.appendTail(sb);
      out = sb.toString();
    }

    //regex with general address pattern against original text
    if(this.useGeneralLocationMatcher){
      Matcher matcher = locationPattern.matcher(text);

      while(matcher.find()) {
        if(posIndex.contains(matcher.start())){
//          log.info("["+text.substring(matcher.start(),matcher.end())+ "] already found before, ignoring it.");
          continue;
        }
        posIndex.add(matcher.start());
        AnonymizedItem ai = new AnonymizedItem(text.substring(matcher.start(),matcher.end()), this.anonymizerType);
        ai.setStart(matcher.start()); ai.setEnd(matcher.end());
//        log.info("found general address:" + ai.getWord() + " at " + matcher.start() + "-" + matcher.end());
        list.add(ai);


        boolean hasStreetNum = false, hasStreet=false, hasCity=false, hasState=false, hasZip=false;

        hasStreetNum = matcher.groupCount() >=1 &&  matcher.group(1)!=null;
        hasStreet = matcher.groupCount() >=2 && matcher.group(2)!=null;
        hasCity = matcher.groupCount() >=3 && matcher.group(3)!=null;
        hasState = matcher.groupCount() >=4 && matcher.group(4)!=null;
        hasZip = matcher.groupCount() >=5 && matcher.group(5)!=null;
        String format = createFormatString( hasStreetNum, hasStreet, hasCity,  hasState, hasZip);

        if(hasStreet){
          String replacement = getExactLocationSurrogate(format, matcher.group(2),matcher.group(4));
          try {
            out = out.replaceAll(Utility.regexStr(ai.getWord()), replacement);
          }catch (PatternSyntaxException e){
            log.error(e.getMessage(),e);
          }
        }
      }
    }


    //replace for NLP discovered items
    if (this.knownNameItems!=null){
      Set<String> uniqueWords = new HashSet();
      for (AnonymizedItem i : this.knownNameItems) {
        String word = i.getWord();
        if (!isStateOrCountry(word)) {
          list.add(i);
          uniqueWords.add(word);
        }
      }

      for(String word: uniqueWords){
        Matcher r = Pattern.compile("\\b(" + Utility.regexStr(word) + ")\\b", Pattern.CASE_INSENSITIVE).matcher(out);
        try {
          out = r.replaceAll(getLocationSurrogateForNerEntity(word));
        } catch (SQLException e) {
          log.error(e.getMessage(),e);
        }
      }
    }


    try {
      hsqlcon.close();
    } catch (SQLException e) {
      log.info(e.getMessage());
    }
    return out;
  }

  private static String createFormatString(boolean hasStreetNum,boolean hasStreet,boolean hasCity, boolean hasState,boolean hasZip){
    return (hasStreetNum?"${streetNumber} ":"")
      + (hasStreet?"${streetName} ${streetType}, ":"")
      + (hasCity?"${city} ":"")
      + (hasState?"${stateCode} ":"")
      + (hasZip?"${zip} ":"")
      ;
  }

  String getLocationSurrogateForNerEntity(String originAddress) throws SQLException {

    if(stateToStateCode.containsKey(StringUtils.capitalize(originAddress)) || StateCodeToState.containsKey(originAddress.toUpperCase())){
      return originAddress; //no need to replace just state alone
    }

    String state = "CA";
    Matcher matcher = locationPatternBroad.matcher(originAddress);
    boolean hasStreetNum = false, hasStreet=false, hasCity=false, hasState=false, hasZip=false;
    while (matcher.find()){
      hasCity = matcher.groupCount()>=1 && matcher.group(1)!=null && matcher.group(1).length()>0;
      hasState = matcher.groupCount()>=2 && matcher.group(2)!=null && matcher.group(2).length()>0;
      hasZip = matcher.groupCount()>=4 && matcher.group(4)!=null && matcher.group(4).length()>0;
    }

    if(!hasCity && !hasState && !hasZip){
      hasCity = true; //treat as city by default
    }
    String format = createFormatString( hasStreetNum, hasStreet, hasCity,  hasState, hasZip);
    return getExactLocationSurrogate(format, originAddress, state);

  }


  static Map<String,Integer> locCache = new HashMap<>();


  int getLocationRange(int char1, int char2) throws SQLException {
    String key = "LOC_"+char1+"_"+char2;
    if(locCache.containsKey(key)){
      return locCache.get(key);
    }
    PreparedStatement findGroupRange = hsqlcon.prepareStatement(findAddressRangeByI12Query);
    ResultSet rangeRs = null;
    int range = -1;

    findGroupRange.setInt(1, char1);
    findGroupRange.setInt(2, char2);
    rangeRs = findGroupRange.executeQuery();

    if(rangeRs!=null && rangeRs.next()){
      range = rangeRs.getInt("range");
    }else{
//      log.info(String.format("did not find match for %s %s, try another random pair", char1, char2));
      range = 0;
    }


//    log.info(String.format("found match for %s%s (%s%s), range:%s", char1, char2,  (char)char1, (char)char2, range));
    locCache.put(key, range);
    rangeRs.close();
    findGroupRange.close();
    return range;
  }


  private String getExactLocationSurrogate(String format, String originStreetName, String originalStateCode)  {

    org.javatuples.Pair<Integer,Integer> chars = Utility.getRandomChars(originStreetName);
    int range = -1;

    try {
      range = getLocationRange(chars.getValue0(),chars.getValue1());
      while(range == 0){
//        log.info(String.format("did not find match for %s %s, try another random pair", chars.getValue0(), chars.getValue1()));
        chars = Utility.getRandomChars(originStreetName);
        range = getLocationRange(chars.getValue0(),chars.getValue1());
      }
      PreparedStatement stmt = hsqlcon.prepareStatement(findAddressByI12Query);
      stmt.setInt(1,chars.getValue0());
      stmt.setInt(2,chars.getValue1());
      stmt.setInt(3,(random.nextInt(Math.min(range, 10))+1));
      java.sql.ResultSet findReplacementRs = stmt.executeQuery();

      if(findReplacementRs.next()){
        String streetName = findReplacementRs.getString("street_name");
        String streetType = findReplacementRs.getString("street_type");
        String city = findReplacementRs.getString("city");
        String zip = findReplacementRs.getString("zip");
        String stateCode = findReplacementRs.getString("statecode");
        findReplacementRs.close();
        stmt.close();
        return format.replaceAll("\\$\\{streetNumber\\}",""+random.nextInt(1000))
          .replaceAll("\\$\\{streetName\\}",StringUtils.capitalize(streetName))
          .replaceAll("\\$\\{streetType\\}",StringUtils.capitalize(streetType))
          .replaceAll("\\$\\{city\\}",StringUtils.capitalize(city))
          .replaceAll("\\$\\{stateCode\\}",stateCode)
          .replaceAll("\\$\\{zip\\}",zip);
      }
      findReplacementRs.close();
      stmt.close();
    } catch (SQLException e) {
      log.error(e.getMessage(), e);
    }

    return defaultReplacementWord;
  }


  final private static Pattern streetAddressPattern = Pattern.compile("(?i)\\b(p\\.?\\s?o\\.?\\b|post office|\\d{1,6})\\s*(([^(\\[\\]]){0,100})\\b");

  final private static Pattern locationPatternBroad = Pattern.compile("(?i)(.*)\\b(AK|Alaska|AL|Alabama|AR|Arkansas|AZ|Arizona|CA|California|CO|Colorado|CT|Connecticut|DC|Washington\\sDC|Washington\\D\\.C\\.|DE|Delaware|FL|Florida|GA|Georgia|GU|Guam|HI|Hawaii|IA|Iowa|ID|Idaho|IL|Illinois|IN|Indiana|KS|Kansas|KY|Kentucky|LA|Louisiana|MA|Massachusetts|MD|Maryland|ME|Maine|MI|Michigan|MN|Minnesota|MO|Missouri|MS|Mississippi|MT|Montana|NC|North\\sCarolina|ND|North\\sDakota|NE|New\\sEngland|NH|New\\sHampshire|NJ|New\\sJersey|NM|New\\sMexico|NV|Nevada|NY|New\\sYork|OH|Ohio|OK|Oklahoma|OR|Oregon|PA|Pennsylvania|RI|Rhode\\sIsland|SC|South\\sCarolina|SD|South\\sDakota|TN|Tennessee|TX|Texas|UT|Utah|VA|Virginia|VI|Virgin\\sIslands|VT|Vermont|WA|Washington|WI|Wisconsin|WV|West\\sVirginia|WY|Wyoming)(\\s|,|)*(\\d{0,5}-*\\d{0,4})$");

  final private static Pattern locationPattern = Pattern.compile("(?i)\\b(p\\.?\\s?o\\.?\\b|post office|\\d{1,6})\\s*([^(\\[\\]]{0,40})(AK|Alaska|AL|Alabama|AR|Arkansas|AZ|Arizona|CA|California|CO|Colorado|CT|Connecticut|DC|Washington\\sDC|Washington\\D\\.C\\.|DE|Delaware|FL|Florida|GA|Georgia|GU|Guam|HI|Hawaii|IA|Iowa|ID|Idaho|IL|Illinois|IN|Indiana|KS|Kansas|KY|Kentucky|LA|Louisiana|MA|Massachusetts|MD|Maryland|ME|Maine|MI|Michigan|MN|Minnesota|MO|Missouri|MS|Mississippi|MT|Montana|NC|North\\sCarolina|ND|North\\sDakota|NE|New\\sEngland|NH|New\\sHampshire|NJ|New\\sJersey|NM|New\\sMexico|NV|Nevada|NY|New\\sYork|OH|Ohio|OK|Oklahoma|OR|Oregon|PA|Pennsylvania|RI|Rhode\\sIsland|SC|South\\sCarolina|SD|South\\sDakota|TN|Tennessee|TX|Texas|UT|Utah|VA|Virginia|VI|Virgin\\sIslands|VT|Vermont|WA|Washington|WI|Wisconsin|WV|West\\sVirginia|WY|Wyoming)\\s+(\\d{5}[-]{0,10}\\d{0,4})");

  final private static String stateMappingFile = "stateToStateCode.txt";
  final private static String countryMappingFile = "countries.txt";

  final private static Map<String,String> stateToStateCode = new HashMap<>();
  final private static Map<String,String> StateCodeToState = new HashMap<>();
  final private static Map<String,String> countryCodeToCountry = new HashMap<>();
  final private static Map<String,String> countryToCountryCode = new HashMap<>();

  static {
    log.info("loading state name mappings");
    Utility.loadFileToMemory(stateMappingFile,stateToStateCode, false);
    Utility.loadFileToMemory(stateMappingFile,StateCodeToState, true);
    Utility.loadFileToMemory(countryMappingFile, countryCodeToCountry, false);
    Utility.loadFileToMemory(countryMappingFile, countryToCountryCode, true);

  }

  public static boolean isStateOrCountry(String text){
    String textUpper = text.toUpperCase();
    String textCap = StringUtils.capitalize(text.toLowerCase());
    return countryCodeToCountry.containsKey(textUpper)
      || countryToCountryCode.containsKey(textCap)
      || stateToStateCode.containsKey(textCap)
      || StateCodeToState.containsKey(textUpper);
  }


  public static class Address{
    private String streetName;
    private String streetType;
    private String streetNumber;
    private String addressLn2;
    private String city;
    private String zipCode;
    private String stateCode;

    public Address(String streetName, String addressLn2, String streetNumber, String streetType, String city, String zipCode,
      String stateCode) {
      this.streetName = streetName;
      this.addressLn2 = addressLn2;
      this.streetType = streetType;
      this.streetNumber = streetNumber;
      this.city = city;
      this.zipCode = zipCode;
      this.stateCode = stateCode;
      formatStateCode();
      getStreetNumberFromStreetName();
    }

    public Address(String streetName, String stateCode){
      this.streetName = streetName;
      this.stateCode = stateCode;
      formatStateCode();
      getStreetNumberFromStreetName();
    }

    private void getStreetNumberFromStreetName(){
      if(this.streetNumber!=null && this.streetType!=null){
        return;
      }

      if(this.streetName==null || this.streetName.length()==0){
        return;
      }

      Matcher matcher = streetAddressPattern.matcher(this.streetName);
      while (matcher.find()){
        if(this.streetNumber==null && matcher.group(1)!=null){
          this.streetName = matcher.group(1);
        }
        if(this.streetType==null && matcher.group(2)!=null){
          String[] words = matcher.group(2).split(" ");
          this.streetType = words[words.length-1];
        }
      }
    }
    private void formatStateCode(){
      if(this.stateCode == null){
        return;
      }
      if(this.stateCode.length()>2){
        this.stateCode = stateToStateCode.get(StringUtils.capitalize(this.stateCode));
      }else{
        this.stateCode = this.stateCode.toUpperCase();
      }
    }



    public String getStreetName() {
      return streetName;
    }

    public void setStreetName(String streetName) {
      this.streetName = streetName;
    }

    public String getStreetNumber() {
      return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
      this.streetNumber = streetNumber;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }

    public String getZipCode() {
      return zipCode;
    }

    public void setZipCode(String zipCode) {
      this.zipCode = zipCode;
    }

    public String getStateCode() {
      return stateCode;
    }

    public void setStateCode(String stateCode) {
      this.stateCode = stateCode;
      formatStateCode();
    }

    public String getStreetType() {
      return streetType;
    }

    public void setStreetType(String streetType) {
      this.streetType = streetType;
    }

    public String getAddressLn2() {
      return addressLn2;
    }

    public void setAddressLn2(String addressLn2) {
      this.addressLn2 = addressLn2;
    }

  }

}
