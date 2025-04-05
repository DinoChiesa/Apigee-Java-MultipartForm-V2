// Copyright © 2016 Apigee Corp, 2017-2025 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ------------------------------------------------------------------

package com.google.apigee.callouts;

import com.google.apigee.fakes.FakeExecutionContext;
import com.google.apigee.fakes.FakeMessage;
import com.google.apigee.fakes.FakeMessageContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.BeforeMethod;

public abstract class TestBase {
  protected static final String testDataDir = "src/test/resources/test-data";
  protected static final boolean verbose = true;

  FakeMessage message;
  FakeMessageContext msgCtxt;
  FakeExecutionContext exeCtxt;

  @BeforeMethod()
  public void beforeMethod(Method method) throws Exception {
    String methodName = method.getName();
    String className = method.getDeclaringClass().getName();
    System.out.printf("\n\n==================================================================\n");
    System.out.printf("TEST %s.%s()\n", className, methodName);

    message = new FakeMessage();
    msgCtxt = new FakeMessageContext(message);
    exeCtxt = new FakeExecutionContext();
    msgCtxt.setVariable("message", message);
  }

  private static byte[] readAll(InputStream is) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = is.read(data, 0, data.length)) != -1) {
        os.write(data, 0, nRead);
      }
      os.flush();
      byte[] b = os.toByteArray();
      return b;
    } catch (Exception ex1) {
      return null;
    }
  }

  protected static byte[] loadImageBytes(String filename) throws IOException {
    Path path = Paths.get(testDataDir, filename);
    if (!Files.exists(path)) {
      throw new IOException("file(" + path.toString() + ") not found");
    }
    InputStream imageInputStream = Files.newInputStream(path);
    byte[] imageBytes = readAll(imageInputStream);
    return imageBytes;
  }
}
