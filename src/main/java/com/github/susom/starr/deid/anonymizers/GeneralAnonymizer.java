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

import com.github.susom.starr.Utility;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * process Phone/Fax, Email, URL, IP address, SSN in one generic anonymizer.
 * @author wenchengli
 */
public class GeneralAnonymizer implements AnonymizerProcessor {

  static final String typePhone = "general-phone";

  private static final String phonePatternPart1 = "\\(?\\d{3}\\)?";
  private static final String phonePatternPart2 = "[\\s.-]?\\d{3}[\\s.-]\\d{4}";
  private static final String phonePatternPart3 = "(\\sext\\s\\d{1,5})?";
  private static final Pattern phoneNumberPattern =
      Pattern.compile(
        "(?=(\\D|\\S|^))\\b(\\+\\d{1,2}\\s)?"
              + phonePatternPart1 + phonePatternPart2 + phonePatternPart3,
        Pattern.CASE_INSENSITIVE);

  static final String typeEmail = "general-email";
  private static final Pattern emailPattern =
        Pattern.compile("\\b\\S+@\\S+\\.\\S+\\b",
            Pattern.CASE_INSENSITIVE);
  static final String typeIp = "general-ip";
  private static final Pattern ipAddressPattern =
        Pattern.compile("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|\\b)){4}\\b",
            Pattern.CASE_INSENSITIVE);
  static final String typeIpv6 = "general-ip";
  private static final Pattern ipAddressV6Pattern =
        Pattern.compile("\\b([A-F0-9]{1,4}:){7}[A-F0-9]{1,4}\\b",
            Pattern.CASE_INSENSITIVE);
  static final String typeUrl = "general-url";

  private static final String urlPatternPart1 = "(https?:\\/\\/)(www\\.)?";
  private static final Pattern urlPattern = Pattern.compile(
      urlPatternPart1
        + "[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)",
        Pattern.CASE_INSENSITIVE);

  static final String typeSsn = "general-ssn";
  private static final Pattern ssn =
      Pattern.compile("(?=(\\D|\\S|^))\\b\\d{3}[\\s.-]\\d{2}[\\s.-]\\d{4}\\b",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern[] pats = new Pattern[]{
      phoneNumberPattern,
      emailPattern,
      ipAddressPattern,
      ipAddressV6Pattern,
      urlPattern,
      ssn};

  private static final String[] types = new String[]{
      typePhone,
      typeEmail,
      typeIp,
      typeIpv6,
      typeUrl,
      typeSsn};

  private static final Map<String,String> replacementMap = new HashMap<>();

  static {
    for (String type : types) {
      replacementMap.put(type, "[" + type + "]");
    }
  }

  public void setReplacementMap(Map<String,String> actionParamMap) {
    if (actionParamMap == null || actionParamMap.size() == 0) {
      return;
    }
    actionParamMap.forEach((k,v) -> {
      if (replacementMap.containsKey(k)) {
        replacementMap.put(k,v);
      }
    });
  }

  @Override
  public void find(String text, List<AnonymizedItemWithReplacement> findings) {

    for (int i = 0;i < pats.length;i++) {
      Pattern p = pats[i];
      String type = types[i];
      Matcher matcher = p.matcher(text);

      while (matcher.find()) {
        String word = text.substring(matcher.start(),matcher.end());
        AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
            word, matcher.start(), matcher.end(), replacementMap.get(type),
            "deid-general", type);

        findings.add(ai);
      }
      //matcher.reset();
    }
  }
}
