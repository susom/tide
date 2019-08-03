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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import org.apache.beam.runners.direct.DirectOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineExtension
    implements BeforeEachCallback, BeforeAllCallback,
    AfterAllCallback, TestInstancePostProcessor {

  public static final String GCP_INTEGRATION_TEST_TOKEN_B64
    = "GCP_INTEGRATION_TEST_TOKEN";
  public static final String GCP_CREDENTIAL_FILE
    = "GOOGLE_APPLICATION_CREDENTIALS";

  static Path credentialFile;
  private static final Logger log = LoggerFactory.getLogger("PipelineExtension");

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {

  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    //setup google credential using credential token
    //for local test, use `base64 -i <key file>` to create token
    //and set it as value of GCP_INTEGRATION_TEST_TOKEN
    //also GCP_INTEGRATION_TEST_PROJECT need to be set to gcp project id
    String gcpCredencialToken = System.getenv(GCP_INTEGRATION_TEST_TOKEN_B64);
    if (gcpCredencialToken != null ) {
      credentialFile  = Paths.get(System.getenv(GCP_CREDENTIAL_FILE));
      if (!credentialFile.toFile().exists()) {
        Files.write(credentialFile,
          Base64.getDecoder().decode(gcpCredencialToken.getBytes("UTF-8")));
      }
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (credentialFile != null) {
      credentialFile.toFile().delete();
    }
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context)
    throws Exception {
    Pipeline pipeline
        = TestPipeline.create(PipelineOptionsFactory.as(DirectOptions.class));

    testInstance.getClass()
      .getMethod("setPipeline", Pipeline.class)
      .invoke(testInstance, pipeline);

  }
}
