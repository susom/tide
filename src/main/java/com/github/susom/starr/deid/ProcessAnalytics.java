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

package com.github.susom.starr.deid;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

class ProcessAnalytics<T> extends PTransform<PCollection<T>, PDone> {

  private final String itemName;
  private final String[] fields;

  public ProcessAnalytics(String itemName, String[] fields) {
    this.itemName = itemName;
    this.fields = fields;
  }

  @Override
  public PDone expand(PCollection<T> input) {
    //input.apply("ConvertToRow", ParDo.of(new BuildRowFn()))
    //.apply(TextIO.write().to("counts-"));
    //return PDone.in(input.getPipeline());
    return null;
  }

  public static class PrintCounts extends SimpleFunction<KV<String, Long>, String> {
    @Override
    public String apply(KV<String, Long> input) {
      return input.getKey() + ":" + input.getValue();
    }
  }
}

