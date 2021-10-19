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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * handling Annotator Specs configuration.
 * @author wenchengl
 */

public class AnnotatorSpecs implements Serializable {
  List<AnnotatorSpec> annotatorSpecList;

  public void setAnnotatorSpecs(AnnotatorSpec[] annotatorSpecs) {
    annotatorSpecList = new ArrayList<AnnotatorSpec>(Arrays.asList(annotatorSpecs));
  }

  public String getAnnotatorLabel(String type) {
    AnnotatorSpec spec = annotatorSpecList.stream()
                .filter(x -> x.type.equals(type)) 
                .findFirst()
                .orElse(null);
    return (spec != null) ? spec.annotatorLabel : type;
  }

}
