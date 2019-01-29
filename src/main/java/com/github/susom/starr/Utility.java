package com.github.susom.starr;

import com.github.susom.starr.deid.DeidTransform.DeidFn;
import com.google.api.client.http.HttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

//import static com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport;

public class Utility {

  private static final Logger log = LoggerFactory.getLogger(Utility.class);
  static private Random random = new Random(UUID.randomUUID().hashCode());

//  final private static HikariDataSource dataSource = Utility.getHsqlConnectionPool();

  public static int jitterHash(String seedString, String salt, int range){

    if(seedString==null){
      seedString = "fixed-"+random.nextInt(1000);
    }

    int result =  Math.abs((seedString+salt).hashCode())% (range*2) - range;
//    long result =  Long.valueOf(BaseEncoding.base16().lowerCase().encode(Hashing.sha256().hashString(seedString+salt, StandardCharsets.UTF_8).asBytes()),16).longValue() % (range*2) - range;
    return result!=0?result:range/2;
  }

  public static String regexStr(String in){
    return Pattern.quote(in);
//    return in!=null?in.replaceAll("[^a-zA-Z0-9 ]","."):"";
  }

  final private static Pattern removeTitleFromNamePattern = Pattern.compile("(?i)\\b(MD|MR|MS|MRS|MISS|MASTER|MAID|MADAM|AUNT|AUNTIE|UNCLE|DR|professor|PROF\\.?|DOC\\.?|DOCTOR|Docent|PhD|Dphil|DBA|EdD|PharmD|LLD|HE|HIM|HIS|HER|SHE|HERS|THEY|THEM|THEIR|THEIRS)\\b");

  public static String removeTitleFromName(String in){
    return removeTitleFromNamePattern.matcher(in).replaceAll("");
  }

  /**
   * return tuple of two random upper case chars that starts differently from provided avoidWord
   * @param avoidWord
   * @return
   */
  public static Pair<Integer,Integer> getRandomChars(String avoidWord){
    avoidWord = avoidWord.toUpperCase();
    int random1 = 65 + random.nextInt(26);
    if(avoidWord.length()>0 && random1 == avoidWord.charAt(0)){
      random1 = (random1 - 65 +1 ) % 26 + 65;
    }
    int random2 = 65 + random.nextInt(26);
    return org.javatuples.Pair.with(random1,random2);
  }

  /**
   * return three random upper case chars that starts differently from provided avoidWord
   * @param avoidWord
   * @return
   */
  public static org.javatuples.Triplet<Integer,Integer,Integer> getRandomTripleChars(String avoidWord){
    int random1 = 65 + random.nextInt(26);
    if(random1 == avoidWord.charAt(0)){
      random1 = (random1 - 65 +1 ) % 26 + 65;
    }
    int random2 = 65 + random.nextInt(26);
    int random3 = 65 + random.nextInt(26);
    return org.javatuples.Triplet.with(random1,random2,random3);
  }

  public static void loadFileToMemory(String classPathResource, HashSet<String> hSet){
    try(Scanner s = new Scanner(DeidFn.class.getClassLoader().getResourceAsStream(classPathResource))){
      while (s.hasNext()) {
        hSet.add(s.nextLine());
      }
    }catch (NullPointerException e){
      log.error("Can not find file at "+classPathResource);
    }
  }
  public static void loadFileToMemory(String classPathResource, Map<String, String> map, boolean reverse){
    try(Scanner s = new Scanner(DeidFn.class.getClassLoader().getResourceAsStream(classPathResource))){
      while (s.hasNext()) {
        String[] parts = s.nextLine().split(",");
        if(reverse){
          map.put(parts[1].trim(), parts[0].trim());
        }else {
          map.put(parts[0].trim(), parts[1].trim());
        }
      }
    }catch (NullPointerException e){
      log.error("Can not find file at "+classPathResource);
    }
  }

  public static void loadFileToMemory(String classPathResource, Consumer<String[]> handler){
    try(Scanner s = new Scanner(Utility.class.getClassLoader().getResourceAsStream(classPathResource))){
      while (s.hasNext()) {
        String[] parts = s.nextLine().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        handler.accept(parts);
      }
      log.info("completed loading data from "+classPathResource);
    }catch (NullPointerException e){
      log.error("Can not find file at "+classPathResource);
      throw e;
    }
  }
//  private static HikariDataSource getHsqlConnectionPool(){
//    HikariConfig config = new HikariConfig();
//    config.setJdbcUrl("jdbc:hsqldb:mem:data");
//
//    return new HikariDataSource(config);
//  }

  public static Connection getHsqlConnection() throws SQLException {
//    return dataSource.getConnection();
    return DriverManager.getConnection("jdbc:hsqldb:mem:data", null, null);
  }

  public static void loadHsqlTable(Connection hsqlcon, Quintet<String, String, String,String,String> tableDef)
    throws SQLException {
    log.info("loading data into HSQL surrogate db");
    try {
      PreparedStatement ps = hsqlcon.prepareStatement(tableDef.getValue3()); //create table
      boolean result = ps.execute();
      log.info( String.format("create table %s ",tableDef.getValue0()));

      if(tableDef.getValue2()!=null && tableDef.getValue2().length()>0){
        Statement statement = hsqlcon.createStatement();
        statement.execute(String.format("CREATE UNIQUE INDEX index_%s ON %s ( %s  )", tableDef.getValue0(), tableDef.getValue0(), tableDef.getValue2()));
        statement.close();
        log.info( String.format("create index for table %s ",tableDef.getValue0()));
      }

      ps.close();

      ps = hsqlcon.prepareStatement("SELECT * FROM "+tableDef.getValue0());
      result = ps.execute();

      String[] names =  tableDef.getValue1().split(",");
      List<String> fields = Arrays.asList(names);

      ResultSetMetaData metadata = ps.getMetaData();

      int[] types = new int[names.length];

      for (int i = 0; i < names.length; i++) {
        types[i] = metadata.getColumnType(fields.indexOf(names[i])+1); //currently only supports 12, 4
      }

      ps.close();

      final PreparedStatement _ps = hsqlcon.prepareStatement(
        "Insert into " + tableDef.getValue0() + " ("+tableDef.getValue1()+") values (" + tableDef.getValue1().replaceAll("([^,]+)","?") + ")");
      Utility.loadFileToMemory(tableDef.getValue4(), values->{
        try {
          for(int i=0;i<values.length;i++){
            int pos = fields.indexOf(names[i]);
            switch (types[pos]){
              case 4:
                _ps.setInt(pos+1, Integer.parseInt(values[i]));
                break;
              case 12:
                _ps.setString(pos+1, values[i]);
                break;

            }
          }
          _ps.execute();
        } catch (SQLException e) {
          log.error(e.getMessage());
        }

      });
      _ps.close();
    }catch (org.hsqldb.HsqlException | java.sql.SQLSyntaxErrorException e){
      if(e.getMessage().contains("already exists")){
        log.info("table exists already");
      }else{
        throw e;
      }
    }
  }
}
