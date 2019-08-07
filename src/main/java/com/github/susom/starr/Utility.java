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

package com.github.susom.starr;

import com.github.susom.database.Database;
import com.github.susom.database.DatabaseProvider;
import com.github.susom.database.SqlInsert;

import com.google.common.base.Charsets;
import edu.stanford.nlp.util.StreamGobbler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.CharSet;
import org.javatuples.Pair;
import org.javatuples.Quintet;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;

public class Utility {

  private static final Logger log = LoggerFactory.getLogger(Utility.class);
  private static Random random = new Random(UUID.randomUUID().hashCode());
  private static double biasFactor = Math.exp(-2.0);

  public static final String inProcessDbUrl = "jdbc:hsqldb:mem:data";

  /**
   * generate jittered integer value for replacing sensitive data.
   * @param seedString seed
   * @param salt salt
   * @param range jitter range
   * @return jitterred integer
   */
  public static int jitterHash(String seedString, String salt, int range) {
    if (seedString == null) {
      seedString = "fixed-" + random.nextInt(1000);
    }

    int result =  Math.abs((seedString + salt).hashCode()) % (range * 2) - range;
    //long result =  Long.valueOf(BaseEncoding.base16().lowerCase().encode(Hashing.sha256()
    // .hashString(seedString+salt, StandardCharsets.UTF_8).asBytes()),16)
    // .longValue() % (range*2) - range;
    return result != 0 ? result : range / 2;
  }

  public static String regexStr(String in) {
    return Pattern.quote(in);
  }

  private static final String titlePatternString1
      = "MD|MR|MS|MRS|MISS|MASTER|MAID|MADAM|AUNT|AUNTIE|UNCLE";
  private static final String titlePatternString2
      = "DR|professor|PROF\\.?|DOC\\.?|DOCTOR|Docent|PhD|Dphil|DBA|EdD|PharmD|LLD";
  private static final String titlePatternString3
      = "HE|HIM|HIS|HER|SHE|HERS|THEY|THEM|THEIR|THEIRS";
  private static final Pattern removeTitleFromNamePattern =
      Pattern.compile("(?i)\\b("
          + titlePatternString1
          + "|" + titlePatternString2
          + "|" + titlePatternString3
          + ")\\b");

  public static String removeTitleFromName(String in) {
    return removeTitleFromNamePattern.matcher(in).replaceAll("");
  }

  /**
   * return tuple of two random upper case chars that starts differently from provided avoidWord.
   * @param avoidWord word to avoid, usually the surrogate target
   * @return tuple of two random upper case chars
   */
  public static Pair<Integer,Integer> getRandomChars(String avoidWord) {
    int random1 = 65 + random.nextInt(26);
    if (avoidWord != null && avoidWord.length() > 0
        && random1 == avoidWord.toUpperCase(Locale.ROOT).charAt(0)) {
      random1 = (random1 - 65 + 1) % 26 + 65;
    }
    int random2 = 65 + random.nextInt(26);
    return org.javatuples.Pair.with(random1,random2);
  }

  /**
   * return position in a given range with probability in skew Gaussian distribution.
   * @param range number range
   * @return random position
   */
  public static int getGaussianRandomPositionInRange(int range, int skew) {
    if (range == 0) {
      return range;
    }

    int randomRange = random.nextInt(range);
    //double position = Math.abs(random.nextGaussian()/100) * Math.max(range - 1,0) + 1;
    //double position =
    // range / 2.0 + range *
    // (biasFactor / (biasFactor + Math.exp(-random.nextGaussian() / 100.0) - 0.5));
    double position = range
        * (randomRange / (double)range  * Math.abs(random.nextGaussian() / (double)skew)) + 1;

    //log.info(String.format("range:%s randomRange:%s skew:%s position:%s",
    //      range,randomRange, skew, position));
    return Math.min((int)Math.abs(Math.floor(position)), range);
  }

  /**
   * return three random upper case chars that starts differently from provided avoidWord.
   * @param avoidWord word to avoid
   * @return eturn three random upper case chars
   */
  public static Triplet<Integer,Integer,Integer> getRandomTripleChars(String avoidWord) {
    int random1 = 65 + random.nextInt(26);
    if (random1 == avoidWord.charAt(0)) {
      random1 = (random1 - 65 + 1) % 26 + 65;
    }
    int random2 = 65 + random.nextInt(26);
    int random3 = 65 + random.nextInt(26);
    return org.javatuples.Triplet.with(random1,random2,random3);
  }

  /**
   * tool to load csv file into a hashset.
   * @param classPathResource filename of the file
   * @param hashSet target hashset
   */
  public static void loadFileToMemory(String classPathResource, HashSet<String> hashSet) {
    try (Scanner s =
           new Scanner(
             Utility.class.getClassLoader().getResourceAsStream(classPathResource), "UTF-8")) {
      while (s.hasNext()) {
        hashSet.add(s.nextLine());
      }
    } catch (NullPointerException e) {
      log.error("Can not find file at " + classPathResource);
    }
  }

  /**
   * load csv file into HashMap, with option of reverse mapping of key,value.
   * @param classPathResource csv filename
   * @param map target hashmap
   * @param reverse if reverse key and value
   */
  public static void loadFileToMemory(String classPathResource,
                                      Map<String, String> map, boolean reverse) {
    try (Scanner s =
            new Scanner(
              Utility.class.getClassLoader().getResourceAsStream(classPathResource), "UTF-8")) {
      while (s.hasNext()) {
        String[] parts = s.nextLine().split(",");
        if (reverse) {
          map.put(parts[1].trim(), parts[0].trim());
        } else {
          map.put(parts[0].trim(), parts[1].trim());
        }
      }
    } catch (NullPointerException e) {
      log.error("Can not find file at " + classPathResource);
    }
  }

  /**
   * load csv file into in-memeory database.
   * @param classPathResource csv filename
   * @param handler handles insert to db
   */
  private static void loadFileToMemory(String classPathResource, Consumer<String[]> handler) {
    try (Scanner s =
            new Scanner(
              Utility.class.getClassLoader().getResourceAsStream(classPathResource), "UTF-8")) {
      while (s.hasNext()) {
        String[] parts = s.nextLine().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        handler.accept(parts);
      }
      log.info("completed loading data from " + classPathResource);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  public static Connection getHsqlConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:hsqldb:mem:data", null, null);
  }

  /**
   * load csv file to Hsql database.
   * @param tableDef table definition
   * @throws SQLException from insertion
   */
  public static void loadHsqlTable(Quintet<String, String, String,String,String> tableDef)
                          throws SQLException {
    log.info("loading data into HSQL surrogate db");

    DatabaseProvider.Builder inProcessDbBuilder
        = DatabaseProvider.fromDriverManager(inProcessDbUrl);

    inProcessDbBuilder.transact(db -> {
      db.get().toUpdate(tableDef.getValue3()).update();
    });

    if (tableDef.getValue2() != null && tableDef.getValue2().length() > 0) {
      String query = String.format(Locale.ROOT,"CREATE UNIQUE INDEX index_%s ON %s ( %s  )",
          tableDef.getValue0(), tableDef.getValue0(), tableDef.getValue2());
      log.info(String.format(Locale.ROOT, "create index for table %s ",tableDef.getValue0()));
      inProcessDbBuilder.transact(db -> {
        db.get().toUpdate(query).update();
      });
    }

    String[] names =  tableDef.getValue1().split(",");
    List<String> fields = Arrays.asList(names);
    int[] types = inProcessDbBuilder.transactReturning(
      dbs -> dbs.get().toSelect("SELECT * FROM " + tableDef.getValue0()).query(r -> {

      ResultSetMetaData metadata = r.getMetadata();
      int[] fieldTypes = new int[names.length];

      for (int i = 0; i < names.length; i++) {
        fieldTypes[i] = metadata.getColumnType(fields.indexOf(names[i]) + 1);
        //currently only supports 12, 4
      }
      return fieldTypes;
    }));

    inProcessDbBuilder.transact(dbs -> {

      String insertQuery = "Insert into " + tableDef.getValue0()
          + " (" + tableDef.getValue1() + ") values ("
          + tableDef.getValue1().replaceAll("([^,]+)","?") + ")";

      Database db = dbs.get();

      Utility.loadFileToMemory(tableDef.getValue4(), values -> {
        SqlInsert insert = db.toInsert(insertQuery);
        for (int i = 0; i < values.length; i++) {
          int pos = fields.indexOf(names[i]);
          switch (types[pos]) {
            case 4:
              insert = insert.argInteger(Integer.parseInt(values[i]));
              break;
            case 12:
              insert = insert.argString(values[i]);
              break;
            default:
              log.warn("not supported");
          }
        }

        insert.insert();
      });
    });

  }

}
