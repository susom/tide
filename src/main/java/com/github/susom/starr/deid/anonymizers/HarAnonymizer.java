package com.github.susom.starr.deid.anonymizers;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HarAnonymizer implements AnonymizerProcessor {

  private static final boolean TEST = false;

  // Preceded by a word boundary
  private static final String PRECEDING = "(?:\\b)";
  // Followed by a word boundary
  private static final String FOLLOWING = "(?:\\b)";

  private static final String HAR = "(\\bHAR:[\\s]?\\b\\d+[ ]{1,})";
  /*
   * HAR:(optional space) followed by any number of digits, followed by one or more spaces, case
   * insensitive
   */

  private static final Object[][] SUBSTITUTIONS =
      new Object[][] {{Pattern.compile(PRECEDING + HAR + FOLLOWING, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "00000000000 "}};

  private final String replaceWord;
  private final String anonymizerType;

  public HarAnonymizer(String replaceWord, String anonymizerType) {
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
        String word = inputText.substring(matcher.start(), matcher.end());
        AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(word, matcher.start(),
            matcher.end(), replacement, "deid-harNum", this.anonymizerType);
        findings.add(ai);
      }
    }

  }

}
