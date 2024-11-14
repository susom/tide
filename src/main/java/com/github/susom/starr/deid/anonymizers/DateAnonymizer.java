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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Date anonymizer
 * migrated from github/proc-note-deidentifier (edu.stanford.irt.core.facade)
 *
 * @author dtom2
 * @author wenchengli
 */
public class DateAnonymizer implements AnonymizerProcessor {
  private static final Logger log = LoggerFactory.getLogger(DateAnonymizer.class);
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
  // Followed by a non-number word boundary
  private static final String FOLLOWING_TO_CUTOFF_NUM = "(?=(?:[^0-9]*\\b))";

  // was ?:
  private static final String
        MON = "(?<month>1|2|3|4|5|6|7|8|9|01|02|03|04|05|06|07|08|09|10|11|12)";
  // was ?i:
  private static final String
        MONTH = "(?<month>January|February|March|April|May|June|July|August|September|October"
        + "|November|December|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)";
  // was ?:
  private static final String
        DAY = "(?<day>1|2|3|4|5|6|7|8|9|01|02|03|04|05|06|07|08|09|10|11|12|13|14|15|16|17|18"
        + "|19|20|21|22|23|24|25|26|27|28|29|30|31)";
  private static final String DAY_SUFFIX = "(?i:st|nd|rd|th)?";
  private static final String YEAR = "(?<year>(?:19|20)[0-9][0-9])";

  //private static final String YEAR = "((?:19|20)[0-9][0-9])";
  private static final String
        HOUR = "(?<hour>1|2|3|4|5|6|7|8|9|01|02|03|04|05|06|07|08|09|10|11|12|13|14|15|16|17|18"
        + "|19|20|21|22|23)";
  private static final String MINUTE = "(?<minute>[0-5][0-9])";
  private static final String SECOND = "(?<second>[0-5][0-9])";
  private static final String AMPM = "(?<ampm>AM|A|PM|P)";
  // Some older time stamps are followed by CT
  private static final String TIMEZONE = "(?i:CT)";
  private static final String
        TIME = "(?<time>" + HOUR + ":" + MINUTE + "(?::" + SECOND + ")?"
        + "(?i:\\s*" + AMPM + ")?" + "(?:\\s*" + TIMEZONE + ")?" + "(?i:\\s*h)?" + ")";

  private static final Object[][] SUBSTITUTIONS = new Object[][] {
      // 25-NOV-2008 10:17 dd-mon-yyyy hh:mm:ss am
      {
        Pattern.compile(PRECEDING + DAY + "(/|-)" + MONTH + "\\2" + YEAR + "(?:\\s*" + TIME + ")?"
              + FOLLOWING, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      },
      //2018-01-01
      {
        Pattern.compile(PRECEDING + YEAR + "(/|-)" + MON + "\\2" + DAY + "(?:\\s*" + TIME + ")?"
          + FOLLOWING, Pattern.CASE_INSENSITIVE),
        TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      },
      //This pattern interfere with mon/dd/yyyy e.g. wrong result for 9/5/2018
      //25-11-2008 10:17 dd-mon-yyyy hh:mm:ss am
      //{ Pattern.compile(PRECEDING + DAY + "(/|-)" + MON + "\\2" + YEAR + "(?:\\s*" + TIME + ")?"
      // + FOLLOWING, Pattern.CASE_INSENSITIVE),
      //  TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      //},
      // Sept/29/2018
      {
        Pattern.compile(PRECEDING + MONTH + "(/|-)" + DAY + "\\2" + YEAR + "(?:\\s*" + TIME + ")?"
            + FOLLOWING_TO_CUTOFF_NUM, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      },
      // 4-digit year mm/dd/yyyy hh:mm:ss am
      {
        Pattern.compile(PRECEDING + MON + "(/|-)" + DAY + "\\2" + YEAR + "(?:\\s*" + TIME + ")?"
            + FOLLOWING_TO_CUTOFF_NUM, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      },
      // 2-digit year mm/dd/yy hh:mm:ss am --> 20yy for yy between 00-29
      {
        Pattern.compile(PRECEDING + MON + "(/|-)" + DAY + "\\2" + "(?<year>[0-2][0-9])"
            + "(?:\\s*" + TIME + ")?" + FOLLOWING_TO_CUTOFF_NUM, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/20${year}"
      },
      // 2-digit year mm/dd/yy hh:mm:ss am --> 19yy for yy between 30-99
      {
        Pattern.compile(PRECEDING + MON + "(/|-)" + DAY + "\\2" + "(?<year>[3-9][0-9])"
            + "(?:\\s*" + TIME + ")?" + FOLLOWING, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/19${year}"
      },
      // January 1st, 2010
      {
        Pattern.compile(PRECEDING + MONTH + " +" + DAY + DAY_SUFFIX
            + "(,|\\s)+" + YEAR + "(?:\\s*" + TIME + ")?" + FOLLOWING, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      },

      // mm/yyyy
      { Pattern.compile(PRECEDING + MON + "(?:/|-)" + YEAR + FOLLOWING, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${year}"
      },
      // March of 2010
      { Pattern.compile(PRECEDING + MONTH + "\\s+(?:of\\s+)?" + YEAR, Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${year}"
      },
      // March 10 - this pattern ignored for anonymizing with bday
      {
        Pattern.compile(PRECEDING + MONTH + "\\s+" + DAY + DAY_SUFFIX + "?\\b",
            Pattern.CASE_INSENSITIVE),
          TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}"
      },
      // 2008-08-09T15:11:00 
      {
        Pattern.compile(PRECEDING + YEAR + "(/|-)" + MON + "\\2" + DAY + "(T?:\\s*" + TIME + ")?",
            Pattern.CASE_INSENSITIVE),
        TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      },
      // 20080809151100
      {
        Pattern.compile(PRECEDING + YEAR + MON + DAY + "(?:\\s*" + TIME + ")?",
            Pattern.CASE_INSENSITIVE),
        TEST ? "<span style=\"color:red\">&lt;$0&gt;</span>" : "${month}/${day}/${year}"
      }
  };

  private Date knownDate;
  private Integer jitter;

  private final String anonymizerType;
  private String defaultReplacement;

  /**
   * constructor.
   * @param date a known date for example birthday
   * @param anonymizerType phi type
   */
  public DateAnonymizer(Date date, String anonymizerType) {
    this.anonymizerType = anonymizerType;
    this.knownDate = date;
  }

  /**
   * constructor that takes a jitter value.
   * @param jitter a int value for jitter offset
   * @param anonymizerType phi type
   * @param defaultReplacement if jitter is not provided, or jitter is zero
   */
  public DateAnonymizer(int jitter, String anonymizerType, String defaultReplacement) {
    this.anonymizerType = anonymizerType;
    this.jitter = jitter;
    this.defaultReplacement = defaultReplacement;
  }

  /**
   * main find function.
   * <pre>
   *     We look for date pattern based on the format in
   *     <a href="https://en.wikipedia.org/wiki/Date_format_by_country">https://en.wikipedia.org/wiki/Date_format_by_country</a>
   *
   *     m/d/yy
   *     m/d/yyyy
   *     d mmm(m) yyyy
   *     yyyy-mm-dd
   *
   * </pre>
   * @param inputText input text
   */
  @Override
  public void find(String inputText, List<AnonymizedItemWithReplacement> findings) {

    boolean hasYear = false;
    boolean hasDay = false;

    // process the string 6 times to check for each potential pattern
    // replace all occurrences matching the pattern individually in the inner matcher.find() loop
    for (int patternIndex = 0; patternIndex < SUBSTITUTIONS.length; patternIndex++) {
      Object[] substitution = SUBSTITUTIONS[patternIndex];
      Pattern pattern = (Pattern) substitution[0];
      String replacement = "DATE";
      Matcher matcher = pattern.matcher(inputText);

      while (matcher.find()) {

        //skip if this finding is within the range of earlier ones
        boolean overlapped = false;
        for (AnonymizedItemWithReplacement f : findings) {
          if (matcher.start() >= f.getStart() && matcher.start() <=  f.getEnd()
            && f.getReplacement() != null) {
            overlapped = true;
            break;
          }
        }

        if (overlapped) {
          continue;
        }

        Date dateOfSvc = null;
        String monthFormatStr = "MMMM";
        String month = matcher.group("month");

        // set month format for simpledateparser.  defaults to full text
        if (month.length() < 3) {
          monthFormatStr = "M";
        } else if (month.length() == 3) {
          monthFormatStr = "MMM";
        } else {
          //regulate month string
          monthFormatStr = "MMM";
          month = month.substring(0,3);
        }

        String yearStr = "" + Calendar.getInstance(TimeZone.getTimeZone("UTC"),
            Locale.ROOT).get(Calendar.YEAR);
        try {
          yearStr = matcher.group("year");
          hasYear = true;
          // if only have two digits of year, compare to last two digits of current year
          // if less than or equal current year, assume 2000s
          // otherwise assume 1900s
          if (yearStr.length() == 2) {
            int currentYear = Calendar.getInstance(TimeZone.getTimeZone("UTC"),
                Locale.ROOT).get(Calendar.YEAR) % 100;
            Integer year = Integer.parseInt(yearStr);
            if (year <= currentYear) {
              yearStr = "20" + yearStr;
            } else {
              yearStr = "19" + yearStr;
            }
          }
        } catch (IllegalArgumentException e) {
          // if no year, default to current year
        }

        String dayStr = "15";
        try {
          dayStr = matcher.group("day");
          hasDay = true;
        } catch (IllegalArgumentException e) {
          // if no day, default to middle of the month
        }

        try {
          SimpleDateFormat sdf = new SimpleDateFormat(monthFormatStr + "/d/yyyy", Locale.ROOT);
          sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

          dateOfSvc = sdf.parse(month + '/' + dayStr + '/' + yearStr);
          if (jitter != null && jitter != 0) {
            //not sure if we should do something different here if no day or
            //no year is present in original date string?
            replacement = getJitteredDate(jitter, dateOfSvc);
          } else if (knownDate != null && hasYear) {
            replacement = getAgeAndYearString(knownDate, dateOfSvc);
          } else {
            replacement = this.defaultReplacement;
          }

          // else replacement defaults to "DATE"
        } catch (ParseException e) {
          log.error("ERROR parsing " + e.getMessage());
        }

        String word =
            inputText.substring(matcher.start(), matcher.end());
        AnonymizedItemWithReplacement ai = new AnonymizedItemWithReplacement(
            word, matcher.start(), matcher.end(),
            replacement, "deid-date", this.anonymizerType);
        findings.add(ai);
      }
    }
  }

  /**
   * see also the equivalent implementation in mercury..ConversionHelper.
   * used to find date of death in the patient list
   * this implementation is used to find dates from the dates of service in the clinical data list
   *
   * @param birthDate patient birthday
   * @param dateOfService service date
   * @return age and year string
   */

  public static String getAgeAndYearString(Date birthDate, Date dateOfService) {
    SimpleDateFormat yrsdf = new SimpleDateFormat("yyyy", Locale.ROOT);
    yrsdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

    if (birthDate == null) {
      if (dateOfService != null) {
        return yrsdf.format(dateOfService);
      } else {
        return "";
      }
    }
    if (dateOfService == null) {
      return "date unknown";
    }
    long patientAgeAtDeath = (dateOfService.getTime() - birthDate.getTime()) / (60 * 60 * 24);
    float ageInYears = patientAgeAtDeath / (float) 365000;
    //float ageInWeeks = patientAgeAtDeath / (float) 52000;
    if (ageInYears > 0) {
      //return (String.format("age %.3f(y)/%.2f(wk) in %s",
      // ageInYears, ageInWeeks, YRSDF.format(dateOfService)));
      return (String.format(Locale.ROOT,"age %.3f in %s", ageInYears, yrsdf.format(dateOfService)));
    } else {
      //if the 'date of service' passed in happens to be their date of birth
      return yrsdf.format(dateOfService); // year of birth - their age will be 0, which looks odd
    }
  }

  private static final long milsecPerDay = 24L * 60L * 60L * 1000L;

  public static String getJitteredDate(Integer jitter, Date dateOfService) {
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy",Locale.ROOT);
    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

    Date newDate = new Date(dateOfService.getTime() + jitter * milsecPerDay);
    return sdf.format(newDate);
  }

}
