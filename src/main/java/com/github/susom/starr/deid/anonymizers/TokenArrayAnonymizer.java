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

/*
 * Created on Feb 26, 2009
 *
 * (c) 2009 Stanford University School of Medicine Center for Clinical Informatics
 * Information Resources and Technology
 */

package com.github.susom.starr.deid.anonymizers;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Takes in an array of the patient/doctor tokens associated to a report and removes any
 * occurrence of them in the report.
 *
 * migrated from github/proc-note-deidentifier (edu.stanford.irt.core.facade)
 *
 * @author Nara
 * @author wenchengli
 */
public class TokenArrayAnonymizer implements AnonymizerProcessor {
  private static final Pattern PATTERN_NON_ALPHANUMERIC_SPACE
      = Pattern.compile("([^a-zA-Z0-9\\s])");
  private static final Pattern PATTERN_SPACES = Pattern.compile("(\\s+)");

  private final String[] tokens;
  private final String replaceWord;

  private final String anonymizerType;

  /**
   * constructor.
   * @param tokens token arrays. e.g. array of tokens
   * @param replaceWord replace with replacement word
   * @param anonymizerType phi type
   */
  public TokenArrayAnonymizer(String[] tokens, String replaceWord, String anonymizerType) {
    this.tokens = tokens;
    this.replaceWord = replaceWord;
    this.anonymizerType = anonymizerType;
  }

  @Override
  public void find(String inputText, List<AnonymizedItemWithReplacement> findings) {
    if (tokens.length == 0) {
      return;
    }

    StringBuffer sb = new StringBuffer();
    for (String token : tokens) {
      // Ignore tokens or parts of name which are only 1 character
      if ((token == null) || (token.trim().length() <= 1)) {
        continue;
      }

      // Escape all non-alphanumeric and non-space characters in the name
      token = PATTERN_NON_ALPHANUMERIC_SPACE.matcher(token).replaceAll("\\\\$1");
      // Replace all space sequences with \s in the name, and treat dash as match all character
      token = PATTERN_SPACES.matcher(token).replaceAll("\\\\s").replaceAll("\\\\-",".");

      sb.append("|" + token);
    }

    sb.delete(0,1);

    // Match the name surrounded by word boundaries, matching is case insensitive
    Pattern namePattern = Pattern.compile("\\b(" + sb.toString() + ")\\b", Pattern.CASE_INSENSITIVE);
    Matcher matcher = namePattern.matcher(inputText);
    while (matcher.find()) {

      String word =
          inputText.substring(matcher.start(), matcher.end());
      AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
          word, matcher.start(), matcher.end(),
          replaceWord, "deid-token", this.anonymizerType);

      findings.add(ai);

    }

  }

}
