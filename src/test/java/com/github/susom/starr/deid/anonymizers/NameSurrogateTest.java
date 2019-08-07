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

import com.github.susom.starr.deid.DeidResultProc;
import com.github.susom.starr.deid.anonymizers.NameSurrogate.NameType;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class NameSurrogateTest {

  private String[] fullNames = new String[]{"Joe Smith","Mary Johnson","Williams, Arras","Peter Jones","Rice Brown","Davis, Emma","Miller, Christopher","Wilson","Moor","Taylor","Anderson","Thomas","Jackson"};

  private String[] lastNames = new String[]{"Smith","Johnson","Williams","Jones","Brown","Davis","Miller","Wilson","Moor","Taylor","Anderson","Thomas","Jackson"};

  private String[] names = new String[]{"Jose","Posada","Arras","ФЭЭЭЭЭ","诚诚诚","Joe","Chris","Posada","Mary","Emma","Christopher","O'BRIEN"};
  private NameType[] dic = new NameType[]{NameType.Firstname,NameType.Lastname,NameType.Lastname,NameType.Lastname,NameType.Lastname,NameType.Firstname,NameType.Firstname,NameType.Lastname,NameType.Firstname,NameType.Firstname,NameType.Firstname,NameType.Firstname,NameType.Firstname};
  String type = "name";

  private static final Logger log = LoggerFactory.getLogger(NameSurrogateTest.class);
  @Test
  public void scrub() throws IOException, SQLException {
    String text = "\t Posada,\tJose,\tMary,\t Joe,\t Emma,\t Chris,\t Christopher,\t ФЭЭЭЭЭ,\t 诚诚诚,\t Hector Sausage-Hausen, O'BRIEN and Mathias d'Arras visited hospital on 11/6/2018. O'BRIEN is the key person";
    List<AnonymizedItemWithReplacement > items = new ArrayList<>();

    NameSurrogate ns = new NameSurrogate(names, "phi-name", dic);

    ns.find(text, items);
    items = items.stream().map(i -> {
      i.setReplacement("[SURROGATED]");
      return i;
    }).collect(Collectors.toList());

    String resultText = DeidResultProc.applyChange(items,text);

    log.info("find input:"+text);
    log.info("find output:"+resultText);

    for (String name : names) {
      assertFalse(resultText.contains(name));
    }

  }

  @Test
  public void getFirstNameSurrogateTest() throws IOException, SQLException {
    NameSurrogate ns = new NameSurrogate(null, "phi-name", dic);

    for (String name : names) {
      String out = ns.getFirstNameSurrogate(name);
      log.info("first name Surrogate input: "+ name+" => output:"+out);
      assertNotEquals("firstname should be surrogated", name, out);
    }

  }

  @Test
  public void getLastNameSurrogateTest() throws IOException, SQLException {
    NameSurrogate ns = new NameSurrogate(null, "phi-name", dic);

    for (String name : lastNames) {
      String out = ns.getLastNameSurrogate(name);
      log.info("Last name Surrogate input: "+ name+" => output:"+out);
      assertNotEquals("lastname should be surrogated", name, out);
    }
  }

  @Test
  public void getFullNameSurrogate() throws SQLException {
    NameSurrogate ns = new NameSurrogate(null, "phi-name", dic);
    for (String name : fullNames) {
      String out = ns.getFullNameSurrogate(name);
      log.info("full name Surrogate input: "+ name+" => output:"+out);
      assertNotEquals("fullname should be surrogated", name, out);
    }

  }
}
