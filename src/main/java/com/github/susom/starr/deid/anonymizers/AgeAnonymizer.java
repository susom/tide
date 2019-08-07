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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Looks for the patient's age in a medical report and removes if over 89.
 *
 * E.g
 *
 * @author eloh
 */

/**
 * Created by eloh on 11/13/17.
 */

public class AgeAnonymizer implements AnonymizerProcessor {
  public static final String DEFAULT_REPLACEMENT = "[90 year old]";

  // For testing - compile with TEST=true to show text to be deleted as highlighted
  private static final boolean TEST = false;

  // Regular expression syntax notes:
  // (?:X) Denotes a non-capturing group, i.e. the group can not be referenced by \n or $n.
  //       This is used for groups which can be ignored. YEAR is a capturing group so that
  //       it can be referenced in the replacement string.
  // (?i:X) Denotes a case-insensitive non-capturing group.
  //
  // See the JavaDoc for java.util.regex.Pattern for a full description of the regular expression
  // syntax.

  // Preceded by a word boundary
  private static final String PRECEDING = "(?:\\b)";
  // Followed by a word boundary
  private static final String FOLLOWING = "(?:\\b)";

  private static final String YROLD = "(?:year[-|\\s]+old|yr[-|\\s]+old|y[/]?o|Y[,]?|years)";
  //private static final String GENDER = "(?<gender>:male|female|M|F|man|woman|girl|boy)";
  private static final String HYPHEN = "(?:\\s*-?\\s*)?";
  private static final String AGE = "(?:1\\d{2}|9\\d)"; // we only care about 90+ or 100+ ages

  private static final Object[][] SUBSTITUTIONS = new Object[][] {
      { Pattern.compile(PRECEDING + AGE + HYPHEN + YROLD + FOLLOWING, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "[90 year old]"
      }
  };

  private final String replaceWord;
  private final String anonymizerType;

  public AgeAnonymizer(String replaceWord, String anonymizerType) {
    this.anonymizerType = anonymizerType;
    this.replaceWord = replaceWord;
  }

  @Override
  public void find(String inputText, List<AnonymizedItemWithReplacement> findings) {

    for (Object[] substitution : SUBSTITUTIONS) {
      Pattern pattern = (Pattern) substitution[0];
      String replacement = (String) substitution[1];
      Matcher matcher = pattern.matcher(inputText);

      while (matcher.find()) {
        String word =
            inputText.substring(matcher.start(), matcher.end());
        AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
            word, matcher.start(), matcher.end(),
            replacement, "deid-age", this.anonymizerType);
        findings.add(ai);
      }
    }

  }

}

