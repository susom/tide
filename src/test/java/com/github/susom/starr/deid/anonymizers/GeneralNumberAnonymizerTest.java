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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GeneralNumberAnonymizerTest {
  private static final Logger log = LoggerFactory.getLogger(GeneralNumberAnonymizerTest.class);
  private String[] textArray = new String[]{
    "process Acc. #: 222222 XYZ",
    "Order #: 22222 XYZ",
    "Acct#: 22222222 XYZ",
    "Accession No: SHS-99-99999",
    "ACCESSION: VS-99-99999"
  };

  @Test
  void find() {
    GeneralNumberAnonymizer gna = new GeneralNumberAnonymizer();
      for (String text : textArray) {
        List<AnonymizedItemWithReplacement> items = new ArrayList<>();
        gna.find(text, items);

        for ( AnonymizedItemWithReplacement item : items) {
          log.info("orginal text :{}  found: {} replace:{}", text, item.getWord(), item.getReplacement());
        }
      }
  }
}
