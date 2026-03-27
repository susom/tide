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
 *
 * Mar 6, 2019
 * migrated from github/proc-note-deidentifier (edu.stanford.irt.core.facade)
 *
 * @author Nara
 * @author wenchengli
 */

package com.github.susom.starr.deid.anonymizers;

import com.github.susom.starr.deid.anonymizers.AnonymizedItemWithReplacement;
import com.github.susom.starr.deid.anonymizers.AnonymizerProcessor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Looks for the patient's MRN number in a medical report and removes them.
 *
 * E.g MRN: ###-##-##
 *     MR#: #######
 *     MEDICAL RECORD #: ###-##-##
 *     Globally looks for ###-##-##, #######-#, #########
 *
 * @author Nara
 *
 */
public class MrnAnonymizer implements AnonymizerProcessor {

  public static final String DEFAULT_REPLACEMENT = "[MRN]";

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

  private static final String LABEL = "(?i:MR|MRN|Medical\\s+Record|Medical\\s+Rec|MED\\s+REC)";
  private static final String NUMBER = "(?:\\s+(?i:Number|Num|No))?";
  private static final String COLON = "(?:\\s*(?i:is|:|#|#:))?";
  private static final String DIGITS = "([\\d\\(\\)]*[\\d -\\(\\)]*\\d)";

  private static final Object[][] SUBSTITUTIONS = new Object[][] {
    // nnn-nn-nn-d, nnn-nn-nn
    { Pattern.compile(PRECEDING + "\\d{3}-\\d{2}-\\d{2}(?:-\\d)?" + FOLLOWING),
      TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : REPLACE_WORD
    },
    // nnn-nn-nnnn
    { Pattern.compile(PRECEDING + "\\d{3}-\\d{2}-\\d{4}(?:-\\d)?" + FOLLOWING),
      TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : REPLACE_WORD
    },
    // nnnnnnn-n
    { Pattern.compile(PRECEDING + "\\d{7}-\\d" + FOLLOWING),
      TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : REPLACE_WORD
    },
    // nnnnnnnn
    { Pattern.compile(PRECEDING + "\\d{8}" + FOLLOWING),
      TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : REPLACE_WORD
    },
    // nn-nn-nn Old MRN pattern, note possible conflict with DateAnonymizer
    { Pattern.compile(PRECEDING + "\\d{2}-\\d{2}-\\d{2}" + FOLLOWING),
      TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : REPLACE_WORD
    },
    // MRN Number: nnn-nn-nn
    { Pattern.compile("(" + LABEL + NUMBER + COLON + "\\s*" + ")" + DIGITS + FOLLOWING),
      TEST ? "$1<span style=\"color:red\">&lt;$2&gt;</span>" : "$1" + REPLACE_WORD
    }
  };

  private final String anonymizerType;
  private final String replaceWord;

  public MrnAnonymizer(String replaceWord, String anonymizerType) {
    this.anonymizerType = anonymizerType;
    this.replaceWord = replaceWord;
  }

  @Override
  public void find(String inputText,  List<AnonymizedItemWithReplacement> findings) {
    for (Object[] substitution : SUBSTITUTIONS) {
      Pattern pattern = (Pattern) substitution[0];
      String replacement = (String) substitution[1];
      Matcher matcher = pattern.matcher(inputText);
      while (matcher.find()) {
        String word =
            inputText.substring(matcher.start(), matcher.end());
        AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
            word, matcher.start(), matcher.end(),
            replaceWord, "deid-mrn", this.anonymizerType);

        findings.add(ai);
      }
    }
  }

}
