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

import com.github.susom.starr.deid.anonymizers.NameSurrogate.NameDictionay;
import edu.stanford.irt.core.facade.AnonymizedItem;
import java.io.IOException;
import java.sql.SQLException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class NameSurrogateTest {

  String[] lastNames = new String[]{"Smith","Johnson","Williams","Jones","Brown","Davis","Miller","Wilson","Moor","Taylor","Anderson","Thomas","Jackson"};


  String[] names = new String[]{"Jose","Posada","Arras","ФЭЭЭЭЭ","诚诚诚","Joe","Chris","Posada","Mary","Emma","Christopher","O'BRIEN"};
  NameDictionay[] dic = new NameDictionay[]{NameDictionay.Firstname,NameDictionay.Lastname,NameDictionay.Lastname,NameDictionay.Lastname,NameDictionay.Lastname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Lastname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Firstname,NameDictionay.Firstname};
  String type = "name";

  private static final Logger log = LoggerFactory.getLogger(NameSurrogateTest.class);
  @Test
  public void scrub() throws IOException, SQLException {
    String text = "\t Posada,\tJose,\tMary,\t Joe,\t Emma,\t Chris,\t Christopher,\t ФЭЭЭЭЭ,\t 诚诚诚,\t Hector Sausage-Hausen, O'BRIEN and Mathias d'Arras visited hospital on 11/6/2018. O'BRIEN is the key person";
    List<AnonymizedItem > items = new ArrayList<>();

    NameSurrogate ns = new NameSurrogate(names, "phi-name", dic);

    String result = ns.scrub(text, items);

//    items.forEach(i->{log.info(i.getWord());});
    log.info("scrub input:"+text);
    log.info("scrub output:"+result);

    for (String name : names) {
      assertFalse(result.contains(name));
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
}
