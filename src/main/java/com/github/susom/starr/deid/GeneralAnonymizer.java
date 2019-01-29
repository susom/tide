package com.github.susom.starr.deid;

import com.github.susom.starr.Utility;
import edu.stanford.irt.core.facade.AnonymizedItem;
import edu.stanford.irt.core.facade.Anonymizer;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * process Phone/Fax, Email, URL, IP address, SSN in one generic anonymizer
 * @author wenchengli
 */
public class GeneralAnonymizer implements Anonymizer {


  final private static String typePhone = "general-phone";
  final private static Pattern phoneNumberPattern = Pattern.compile("(?=(\\D|\\S|^))\\b(\\+\\d{1,2}\\s)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]\\d{4}(\\sext\\s\\d{1,5})?" , Pattern.CASE_INSENSITIVE);
  final private static String typeEmail = "general-email";
  final private static Pattern emailPattern = Pattern.compile("\\b\\S+@\\S+\\.\\S+\\b", Pattern.CASE_INSENSITIVE);
  final private static String typeIp = "general-ip";
  final private static Pattern ipAddressPattern = Pattern.compile("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|\\b)){4}\\b", Pattern.CASE_INSENSITIVE);
  final private static String typeIpv6 = "general-ip";
  final private static Pattern ipAddressV6Pattern = Pattern.compile("\\b([A-F0-9]{1,4}:){7}[A-F0-9]{1,4}\\b", Pattern.CASE_INSENSITIVE);
  final private static String typeUrl = "general-url";
//  final private static Pattern urlPattern = Pattern.compile("(https?:\\/\\/|\\b)(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)", Pattern.CASE_INSENSITIVE);
final private static Pattern urlPattern = Pattern.compile("(https?:\\/\\/)(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)", Pattern.CASE_INSENSITIVE);


  final private static String typeSsn = "general-ssn";
  final private static Pattern ssn = Pattern.compile("(?=(\\D|\\S|^))\\b\\d{3}[\\s.-]\\d{2}[\\s.-]\\d{4}\\b", Pattern.CASE_INSENSITIVE);

  final private static Pattern[] pats = new Pattern[]{phoneNumberPattern,   emailPattern,   ipAddressPattern, ipAddressV6Pattern,   urlPattern,   ssn};
  final private static String[] types = new String[]{typePhone,             typeEmail,      typeIp,           typeIpv6,             typeUrl,      typeSsn};
  @Override
  public String scrub() {
    return null;
  }

  @Override
  public String scrub(String text, List<AnonymizedItem> list) {

    String out = text;
    HashSet<Integer> posIndex = new HashSet<>();

    for (int i=0;i< pats.length;i++){
      Pattern p = pats[i];
      String type = types[i];
      Matcher matcher = p.matcher( text );

      while(matcher.find()) {
        String word = text.substring(matcher.start(),matcher.end());
        out = out.replaceAll(Utility.regexStr(word), String.format("[%s]", type));

        if(posIndex.contains(matcher.start())){
          continue;
        }
        posIndex.add(matcher.start());
        AnonymizedItem ai = new AnonymizedItem(word, type);
        ai.setStart(matcher.start()); ai.setEnd(matcher.end());
        list.add(ai);
      }
//      matcher.reset();
    }



    return out;
  }
}
