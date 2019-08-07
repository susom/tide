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

package com.github.susom.starr.deid.anonymizers;

import com.github.susom.database.DatabaseException;
import com.github.susom.database.DatabaseProvider;
import com.github.susom.database.DatabaseProvider.Builder;
import com.github.susom.starr.Utility;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
 * Location identification and surrogate.
 * @author wenchengl
 */

public class LocationSurrogate implements AnonymizerProcessor {

  private static final String defaultReplacementWord = "[REMOVED]";

  private static final AtomicBoolean dbLoaded = new AtomicBoolean(false);

  String inProcessDbUrl = "jdbc:hsqldb:mem:data";
  Builder inProcessDbBuilder = null;

  private static final String findAddressRangeByI12Query
      = "select range from us_address_range "
          + "where name_i1 = ? and name_i2 = ?  limit 1";

  private static final String findAddressByI12Query
      = "select street_name, street_type, city, zip, statecode "
          + "from us_address where street_i1 = ? and street_i2 = ? and seq = ? limit 1";

  private static final Logger log = LoggerFactory.getLogger(LocationSurrogate.class);

  private static Random random = new Random(UUID.randomUUID().hashCode());

  private Address[] knownAddr;
  private String anonymizerType;

  private List<AnonymizedItemWithReplacement> knownNameItems;
  private boolean useGeneralLocationMatcher = false;

  public static final List<Quintet<String,String, String, String,String>> hsqlTables
        = new ArrayList<>();

  static {

    //address with three char index
    //STATECODE,ZIP,STREET_NAME,STREET_I1,STREET_I2,STREET_I3,STREET_TYPE,CITY,SEQ,OCCURRENCES
    //example: NM,87020,E Santa Fe,69,32,83,Ave,Grants,86,2
    hsqlTables.add(Quintet.with(
        "us_address",
        "statecode,zip,street_name,street_i1,street_i2,street_i3,street_type,city,seq,occurrences",
        "street_i1,street_i2,seq",
        "create table us_address ("
            + "statecode varchar(4),zip varchar(12), street_name varchar(100),"
            + "street_i1 integer,street_i2 integer,street_i3 integer,"
            + "street_type varchar(20),city varchar(40),seq integer,occurrences integer"
            + ")",
        "address_g3.csv"));

    //address range for each group (i1,i2)  : '65,65,43'
    hsqlTables.add(Quintet.with(
        "us_address_range","name_i1,name_i2,range","name_i1,name_i2",
        "create table us_address_range (name_i1 integer,name_i2 integer, range integer)",
        "address_g_range.csv"));
  }

  /**
   * location surrogate constructor.
   * @param knownAddr known addresses
   * @param anonymizerType phi type
   * @param knownNameItems name entities discovered by NLP
   * @param useGeneralLocationMatcher if use regex location pattern matching
   * @throws SQLException could fail over getting surrogate replacement
   */
  public LocationSurrogate(Address[] knownAddr, String anonymizerType,
                           List<AnonymizedItemWithReplacement> knownNameItems, boolean useGeneralLocationMatcher)
        throws SQLException {
    this.anonymizerType = anonymizerType;
    this.knownAddr = knownAddr;
    this.knownNameItems = knownNameItems;
    if (this.knownNameItems == null) {
      this.knownNameItems = new ArrayList<>();
    }

    this.useGeneralLocationMatcher = useGeneralLocationMatcher;

    synchronized (dbLoaded) {
      if (!dbLoaded.get()) {
        for (Quintet<String, String, String,String,String> tableDef : hsqlTables) {
          Utility.loadHsqlTable(tableDef);
        }
        dbLoaded.set(true);
      }
    }

    inProcessDbBuilder = DatabaseProvider.fromDriverManager(inProcessDbUrl);
  }

  /**
   * produce regex string for given address.
   * @param address known address
   * @return regex pattern string
   */
  public static String getCityLevelLocationRegex(Address address) {

    if ((address.getCity() == null || address.getCity().length() == 0)) {
      return null;
    }

    String separater = "[\\s.,]+";
    StringBuilder sb = new StringBuilder();

    sb.append(address.getCity().replaceAll("[^a-zA-Z0-9]","."));

    if (address.getStateCode() != null && address.getStateCode().length() > 0) {
      sb.append(separater).append("|").append(address.getStateCode()
          .replaceAll("[^a-zA-Z0-9]","."));
    }

    if (address.getZipCode() != null && address.getZipCode().length() > 0) {
      sb.append(separater).append(address.getZipCode()
          .replaceAll("[^a-zA-Z0-9]","."));
    }
    return sb.toString();
  }

  @Override
  public void find(String text, List<AnonymizedItemWithReplacement> findings) {

    for (int i = 0; knownAddr != null &&  i < knownAddr.length; i++) {
      Address addr = knownAddr[i];
      if (addr == null || (
          (addr.getStreetNumber() == null || addr.getStreetNumber().length() == 0)
          && (addr.getStreetName() == null || addr.getStreetName().trim().length() < 3)
              && (addr.getCity() == null || addr.getCity().trim().length() < 3)
          )) {
        continue; //ignore address without street and city
      }

      if (((addr == null) || (addr.getStreetNumber() == null)
          || (addr.getStreetName() == null)
          || (addr.getStreetName().trim().length() <= 2))) {

        //address without street
        String patternStr = LocationSurrogate.getCityLevelLocationRegex(addr);
        if (patternStr == null) {
          continue;
        }

        Matcher locationPatternFromKnownAddress = Pattern.compile(patternStr,Pattern.CASE_INSENSITIVE).matcher(text);
        while (locationPatternFromKnownAddress.find()) {
          String format = createFormatString(false, false,
              (addr.getCity() != null && addr.getCity().trim().length() > 1),
              (addr.getStateCode() != null && addr.getStateCode().trim().length() > 1),
              (addr.getZipCode() != null && addr.getZipCode().trim().length() > 1));

          String replacement = getExactLocationSurrogate(format,
              addr.getStreetName(), addr.stateCode);
          String word =
              text.substring(locationPatternFromKnownAddress.start(), locationPatternFromKnownAddress.end());
          AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
              word, locationPatternFromKnownAddress.start(), locationPatternFromKnownAddress.end(),
              replacement, "deid-location-knownaddr", this.anonymizerType);

          findings.add(ai);
        }
        continue;
      }
      //do broader find and replace
      String regexStr = "(?i)\\b("
          + Utility.regexStr(addr.getStreetNumber())
          + ")(\\s*)([N|W|S|E]{0,1})\\W{0,5}\\s*("
          + Utility.regexStr(addr.getStreetName())
          + "|)\\W{0,5}\\s*("
          + Utility.regexStr(addr.getStreetType())
          + "|)\\W{0,5}\\s*("
          + Utility.regexStr(addr.getAddressLn2())
          + "|)\\W{0,5}\\s*("
          + Utility.regexStr(addr.getCity())
          + "|)\\W{0,5}\\s*("
          + Utility.regexStr(addr.getStateCode())
          + "|\\b)\\W{0,5}("
          + Utility.regexStr(addr.getZipCode())
          + ")\\W{1,5}\\s*\\d{0,5}";

      Matcher r = Pattern.compile(regexStr,
          Pattern.CASE_INSENSITIVE).matcher(text);

      while (r.find()) {

        boolean hasStreetNum = r.groupCount() >= 1 && r.group(1) != null;
        boolean hasStreet = r.groupCount() >= 2 && r.group(2) != null;
        boolean hasCity = r.groupCount() >= 3 && r.group(3) != null;
        boolean hasState = r.groupCount() >= 4 && r.group(4) != null;
        boolean hasZip = r.groupCount() >= 5 && r.group(5) != null;

        String format = createFormatString(hasStreetNum, hasStreet, hasCity,  hasState, hasZip);

        String replacement = getExactLocationSurrogate(format,
            addr.getStreetName(), addr.stateCode);
        String word = text.substring(r.start(), r.end());
        AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
            word,
            r.start(), r.end(),
            replacement, "deid-location-knownaddr", this.anonymizerType);
        log.info("found known address:" + ai.getWord() + " at " + r.start() + "-" + r.end());
        findings.add(ai);
      }
    }

    //regex with general address pattern against original text
    if (this.useGeneralLocationMatcher) {
      Matcher matcher = locationPattern.matcher(text);

      while (matcher.find()) {
        boolean hasStreetNum = matcher.groupCount() >= 1 &&  matcher.group(1) != null;
        boolean hasStreet = matcher.groupCount() >= 2 && matcher.group(2) != null;
        boolean hasCity = matcher.groupCount() >= 3 && matcher.group(3) != null;
        boolean hasState = matcher.groupCount() >= 4 && matcher.group(4) != null;
        boolean hasZip = matcher.groupCount() >= 5 && matcher.group(5) != null;
        String format = createFormatString(hasStreetNum, hasStreet, hasCity,  hasState, hasZip);

        if (hasStreet) {
          String replacement = getExactLocationSurrogate(format, matcher.group(2),matcher.group(4));
          String word = text.substring(matcher.start(), matcher.end());
          AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
              word,
              matcher.start(), matcher.end(),
              replacement, "deid-location-general", this.anonymizerType);
          findings.add(ai);
        }
      }
    }
    //replace for NLP discovered items
    if (this.knownNameItems != null) {
      for (AnonymizedItemWithReplacement i : this.knownNameItems) {
        String word = i.getWord();
        if (!isStateOrCountry(word)) {

          try {
            String replacement = getLocationSurrogateForNerEntity(word);
            i.setReplacement(replacement);

          } catch (SQLException e) {
            log.error(e.getMessage(),e);
            i.setReplacement(AnonymizerProcessor.REPLACE_WORD);
          }
          findings.add(i);
        }
      }
    }
  }

  private static String createFormatString(
            boolean hasStreetNum,boolean hasStreet,
            boolean hasCity, boolean hasState,boolean hasZip) {
    return (hasStreetNum ? "${streetNumber} " : "")
      + (hasStreet ? "${streetName} ${streetType}, " : "")
      + (hasCity ? "${city} " : "")
      + (hasState ? "${stateCode} " : "")
      + (hasZip ? "${zip} " : "")
      ;
  }

  String getLocationSurrogateForNerEntity(String originAddress) throws SQLException {

    if (stateToStateCode.containsKey(StringUtils.capitalize(originAddress))
        || StateCodeToState.containsKey(originAddress.toUpperCase(Locale.ROOT))) {
      return originAddress; //no need to replace just state alone
    }

    String state = "CA";
    Matcher matcher = locationPatternBroad.matcher(originAddress);
    boolean hasStreetNum = false;
    boolean hasStreet = false;
    boolean hasCity = false;
    boolean hasState = false;
    boolean hasZip = false;
    while (matcher.find()) {
      hasCity = matcher.groupCount() >= 1
            && matcher.group(1) != null && matcher.group(1).length() > 0;
      hasState = matcher.groupCount() >= 2
            && matcher.group(2) != null && matcher.group(2).length() > 0;
      hasZip = matcher.groupCount() >= 4
            && matcher.group(4) != null && matcher.group(4).length() > 0;
    }

    if (!hasCity && !hasState && !hasZip) {
      hasCity = true; //treat as city by default
    }
    String format = createFormatString(hasStreetNum, hasStreet, hasCity,  hasState, hasZip);
    return getExactLocationSurrogate(format, originAddress, state);
  }

  static Map<String,Integer> locCache = new HashMap<>();

  int getLocationRange(int char1, int char2) {

    String key = "LOC_" + char1 + "_" + char2;
    if (locCache.containsKey(key)) {
      return locCache.get(key);
    }

    Integer range = inProcessDbBuilder.transactReturning(db -> {
      Integer result = db.get().toSelect(findAddressRangeByI12Query)
          .argInteger(char1)
          .argInteger(char2)
          .queryOneOrNull(r -> r.getIntegerOrNull("range"));

      return result != null ? result : 0;
    });

    if (range != null) {
      locCache.put(key, range);
      return range;
    }
    return 0;
  }

  private String getExactLocationSurrogate(String format,
                                           String originStreetName, String originalStateCode)  {

    org.javatuples.Pair<Integer,Integer> chars = Utility.getRandomChars(originStreetName);
    int range;

    try {
      range = getLocationRange(chars.getValue0(),chars.getValue1());
      while (range == 0) {
        chars = Utility.getRandomChars(originStreetName);
        range = getLocationRange(chars.getValue0(),chars.getValue1());
      }

      final org.javatuples.Pair<Integer,Integer> charParam = chars;
      final Integer rangeParam = range;
      String surrogateLocation = inProcessDbBuilder.transactReturning(db ->
        db.get().toSelect(findAddressByI12Query)
        .argInteger(charParam.getValue0())
        .argInteger(charParam.getValue1())
        .argInteger(random.nextInt(Math.min(rangeParam, 10)) + 1)
        .queryFirstOrNull(r -> {
          String streetName = r.getStringOrEmpty("street_name");
          String streetType = r.getStringOrEmpty("street_type");
          String city = r.getStringOrEmpty("city");
          String zip = r.getStringOrEmpty("zip");
          String stateCode = r.getStringOrEmpty("statecode");
          return format.replaceAll("\\$\\{streetNumber\\}","" + random.nextInt(1000))
            .replaceAll("\\$\\{streetName\\}",StringUtils.capitalize(streetName))
            .replaceAll("\\$\\{streetType\\}",StringUtils.capitalize(streetType))
            .replaceAll("\\$\\{city\\}",StringUtils.capitalize(city))
            .replaceAll("\\$\\{stateCode\\}",stateCode)
            .replaceAll("\\$\\{zip\\}",zip);
        }));

      if (surrogateLocation != null) {
        return surrogateLocation;
      }

    } catch (DatabaseException e) {
      log.error(e.getMessage(), e);
    }
    return defaultReplacementWord;
  }

  private static final String streeAddressPatternPart1 = "(p\\.?\\s?o\\.?\\b|post office|\\d{1,6})";
  private static final Pattern streetAddressPattern
      = Pattern.compile("(?i)\\b" + streeAddressPatternPart1 + "\\s*(([^(\\[\\]]) {0,100})\\b");

  private static final String stateString = "AK|Alaska|AL|Alabama|AR|Arkansas|AZ|Arizona"
        + "|CA|California|CO|Colorado|CT|Connecticut|DC|Washington\\sDC|Washington\\D\\.C\\."
        + "|DE|Delaware|FL|Florida|GA|Georgia|GU|Guam|HI|Hawaii|IA|Iowa|ID|Idaho|IL|Illinois"
        + "|IN|Indiana|KS|Kansas|KY|Kentucky|LA|Louisiana|MA|Massachusetts|MD|Maryland|ME"
        + "|Maine|MI|Michigan|MN|Minnesota|MO|Missouri|MS|Mississippi|MT|Montana"
        + "|NC|North\\sCarolina|ND|North\\sDakota|NE|New\\sEngland|NH|New\\sHampshire"
        + "|NJ|New\\sJersey|NM|New\\sMexico|NV|Nevada|NY|New\\sYork|OH|Ohio|OK|Oklahoma"
        + "|OR|Oregon|PA|Pennsylvania|RI|Rhode\\sIsland|SC|South\\sCarolina|SD|South\\sDakota"
        + "|TN|Tennessee|TX|Texas|UT|Utah|VA|Virginia|VI|Virgin\\sIslands|VT|Vermont"
        + "|WA|Washington|WI|Wisconsin|WV|West\\sVirginia|WY|Wyoming";

  private static final Pattern locationPatternBroad =
      Pattern.compile("(?i)(.*)\\b(" + stateString + ")(\\s|,|)*(\\d{0,5}-*\\d{0,4})$");

  private static final Pattern locationPattern =
      Pattern.compile("(?i)\\b" + streeAddressPatternPart1
          + "\\s*([^(\\[\\]]{0,40})(" + stateString + ")\\s+(\\d{5}[-]{0,10}\\d{0,4})");

  private static final String stateMappingFile = "stateToStateCode.txt";
  private static final String countryMappingFile = "countries.txt";

  private static final Map<String,String> stateToStateCode = new HashMap<>();
  private static final Map<String,String> StateCodeToState = new HashMap<>();
  private static final Map<String,String> countryCodeToCountry = new HashMap<>();
  private static final Map<String,String> countryToCountryCode = new HashMap<>();

  static {
    log.info("loading state name mappings");
    Utility.loadFileToMemory(stateMappingFile,stateToStateCode, false);
    Utility.loadFileToMemory(stateMappingFile,StateCodeToState, true);
    Utility.loadFileToMemory(countryMappingFile, countryCodeToCountry, false);
    Utility.loadFileToMemory(countryMappingFile, countryToCountryCode, true);
  }

  /**
   * check if a word is a state name or state code.
   * @param text input word
   * @return true if is a state name or code
   */
  public static boolean isStateOrCountry(String text) {
    String textUpper = text.toUpperCase(Locale.ROOT);
    String textCap = StringUtils.capitalize(text.toLowerCase(Locale.ROOT));
    return countryCodeToCountry.containsKey(textUpper)
      || countryToCountryCode.containsKey(textCap)
      || stateToStateCode.containsKey(textCap)
      || StateCodeToState.containsKey(textUpper);
  }

  public static class Address {
    private String streetName;
    private String streetType;
    private String streetNumber;
    private String addressLn2;
    private String city;
    private String zipCode;
    private String stateCode;

    /**
     * constructor for Address.
     * @param streetName street name
     * @param addressLn2 address
     * @param streetNumber street number
     * @param streetType street type, e.g. rd, ave
     * @param city city name
     * @param zipCode zip code
     * @param stateCode state code, e.g. CA
     */
    public Address(String streetName, String addressLn2, String streetNumber,
                    String streetType, String city, String zipCode, String stateCode) {
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

    /**
     * constructor for Address.
     * @param streetName street name
     * @param stateCode state code, e.g. CA
     */
    public Address(String streetName, String stateCode) {
      this.streetName = streetName;
      this.stateCode = stateCode;
      formatStateCode();
      getStreetNumberFromStreetName();
    }

    private void getStreetNumberFromStreetName() {
      if (this.streetNumber != null && this.streetType != null) {
        return;
      }

      if (this.streetName == null || this.streetName.length() == 0) {
        return;
      }

      Matcher matcher = streetAddressPattern.matcher(this.streetName);
      while (matcher.find()) {
        if (this.streetNumber == null && matcher.group(1) != null) {
          this.streetName = matcher.group(1);
        }
        if (this.streetType == null && matcher.group(2) != null) {
          String[] words = matcher.group(2).split(" ");
          if (words.length > 0) {
            this.streetType = words[words.length - 1];
          }
        }
      }
    }

    private void formatStateCode() {
      if (this.stateCode == null) {
        return;
      }
      if (this.stateCode.length() > 2) {
        this.stateCode = stateToStateCode.get(StringUtils.capitalize(this.stateCode));
      } else {
        this.stateCode = this.stateCode.toUpperCase(Locale.ROOT);
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
