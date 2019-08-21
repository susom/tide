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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * process Acc. #: 999999 Order #: 99999,Acct#: 99999999 in one generic anonymizer.
 * @author wenchengli
 */
public class GeneralNumberAnonymizer implements AnonymizerProcessor {

  private static final String NUMBER = "(Number|Num|No|#)?";
  private static final String COLON = "(is|:)?";
  private static final String DIGITS = "(\\d[\\d -]*\\d)";
  private static final String DIGITS_W_PREFIX = "([a-zA-Z-]{0,5}\\d[\\d -]*\\d)";
  private static final String SPACES = "\\s*";

  static final String typeAccount = "general-account";

  private static final Pattern accountNumberPattern =
      Pattern.compile(
        "\\b((Acc\\.|Acct\\.|Acct|Account)" + SPACES + NUMBER + SPACES + COLON + SPACES + ")" + DIGITS + "\\b",
        Pattern.CASE_INSENSITIVE);

  static final String typeOrder = "general-order";
  private static final Pattern orderPattern =
    Pattern.compile(
      "\\b((Order|Ord\\.)" + SPACES + NUMBER + SPACES + COLON + SPACES + "\\s*" + ")" + DIGITS + "\\b",
      Pattern.CASE_INSENSITIVE);

  static final String typeAccession = "general-accession";
  private static final Pattern accessionPattern =
    Pattern.compile(
      "\\b((ACCESSION|ACCE\\.)" + SPACES + NUMBER + SPACES + COLON + SPACES + "\\s*" + ")" + DIGITS_W_PREFIX + "\\b",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern[] pats = new Pattern[]{
      accountNumberPattern,
      orderPattern,
      accessionPattern
  };

  private static final String[] types = new String[]{
      typeAccount,
      typeOrder,
      typeAccession
  };

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
            "deid-number-general", type);

        findings.add(ai);
      }
      //matcher.reset();
    }
  }
}
