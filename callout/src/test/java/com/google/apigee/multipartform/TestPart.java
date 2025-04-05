// Copyright © 2025 Google LLC.
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

package com.google.apigee.multipartform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestPart {
  public static final String CRLF = "\r\n";
  public static final String LF = "\n";

  static byte[] JPEGDATA = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}; // Sample JPEG start

  public static byte[] concatenate(byte[] a, byte[] b) {
    byte[] result = Arrays.copyOf(a, a.length + b.length);
    System.arraycopy(b, 0, result, a.length, b.length);
    return result;
  }

  // Helper to create input bytes with specified line endings, handling empty body correctly
  private byte[] inputBytes(String[] headers, Object body, String lineEnding) {
    // Ensure headers end with a line ending before the blank line separator
    String headerPart =
        Arrays.stream(headers)
                .map(header -> header.trim().isEmpty() ? "" : header.replaceAll("(?<!\r)\n", ""))
                .collect(Collectors.joining(lineEnding))
            + lineEnding
            + lineEnding;
    byte[] headerBytes = headerPart.getBytes(StandardCharsets.UTF_8);
    return concatenate(
        headerBytes,
        (body instanceof String)
            ? ((String) body).getBytes(StandardCharsets.UTF_8)
            : (byte[]) body);
  }

  // Helper for cases with absolutely no headers
  private byte[] createInputNoHeaders(String body) {
    return body.getBytes(
        StandardCharsets.UTF_8); // Input might start directly with body or be empty
  }

  @DataProvider(name = "successDataProvider")
  public Object[][] successDataProvider() {
    byte[] emptyBytes = new byte[0];

    return new Object[][] {
      {
        1,
        "Basic case  CRLF",
        inputBytes(
            new String[] {
              "Content-Disposition: form-data; name=\"fieldName\"", "Content-Type: text/plain"
            },
            "This is the content.",
            CRLF),
        "fieldName",
        "text/plain",
        "This is the content.".getBytes(StandardCharsets.UTF_8)
      },
      {
        2,
        "Basic without quotes on name,  CRLF",
        inputBytes(
            new String[] {
              "Content-Disposition: form-data; name=fieldName", "Content-Type: text/plain"
            },
            "This is the content.",
            CRLF),
        "fieldName",
        "text/plain",
        "This is the content.".getBytes(StandardCharsets.UTF_8)
      },
      {
        4,
        "No Content-Type, name without quotes",
        inputBytes(
            new String[] {"Content-Disposition: form-data; name=fieldName2"},
            "Some other data.",
            CRLF),
        "fieldName2",
        "text/plain",
        "Some other data.".getBytes(StandardCharsets.UTF_8)
      },
      {
        5,
        "Extra headers are ignored",
        inputBytes(
            new String[] {
              "Content-Disposition: form-data; name=\"file1\"",
              "Content-Type: image/jpeg",
              "X-Custom-Header: SomeValue"
            },
            JPEGDATA,
            CRLF),
        "file1",
        "image/jpeg",
        JPEGDATA
      },
      {
        6,
        "headers with leading and trailing spaces",
        inputBytes(
            new String[] {
              " Content-Disposition: form-data; name=\"fieldName\"  ",
              "  Content-Type: text/plain  "
            },
            "This is the content.",
            CRLF),
        "fieldName",
        "text/plain",
        "This is the content.".getBytes(StandardCharsets.UTF_8)
      },
      {
        7,
        "Header names are case-insensitive",
        inputBytes(
            new String[] {
              "content-disposition: form-data; name=\"MixedCase\"", "CONTENT-TYPE: text/html"
            },
            "<html></html>",
            CRLF),
        "MixedCase",
        "text/html",
        "<html></html>".getBytes(StandardCharsets.UTF_8)
      },
      {
        8,
        "Empty content body",
        inputBytes(
            new String[] {"Content-Disposition: form-data; name=\"emptyPart\""},
            "", // Empty body
            CRLF),
        "emptyPart",
        "text/plain",
        emptyBytes
      },
      {
        9,
        "Content-Disposition with extra parameters (ignored)",
        inputBytes(
            new String[] {
              "Content-Disposition: form-data; name=\"upload\"; filename=\"file.txt\"",
              "Content-Type: text/plain"
            },
            "File content.",
            CRLF),
        "upload",
        "text/plain",
        "File content.".getBytes(StandardCharsets.UTF_8)
      },
      {
        10,
        "Header value contains colon (should be handled correctly",
        inputBytes(
            new String[] {
              "Content-Disposition: inline; name=\"json_data:with_colon\"",
              "Content-Type: application/json; charset=utf-8"
            },
            "{\"time\":\"10:30\"}",
            CRLF),
        "json_data:with_colon", // Name should include the colon
        "application/json; charset=utf-8", // Type includes parameters
        "{\"time\":\"10:30\"}".getBytes(StandardCharsets.UTF_8)
      }
    };
  }

  @Test(dataProvider = "successDataProvider")
  public void successCases(
      Integer casenum,
      String label,
      byte[] inputBytes,
      String expectedName,
      String expectedContentType,
      byte[] expectedContent)
      throws IOException {
    // Ensure the actual Part class and its static parse method are used here
    Part result = Part.parse(inputBytes);
    Assert.assertNotNull(result, String.format("case %d, Parsed Part should not be null", casenum));
    Assert.assertEquals(result.getName(), expectedName, "Part name mismatch");
    Assert.assertEquals(result.getContentType(), expectedContentType, "Content type mismatch");
    // Use Arrays.equals for byte array comparison
    Assert.assertEquals(result.getPartContent(), expectedContent, "Part content mismatch");
  }

  @DataProvider(name = "failureDataProvider")
  public Object[][] failureDataProvider() {

    return new Object[][] {
      //
      {
        1,
        "Wrong separator (LF)",
        inputBytes(
            new String[] {
              "Content-Disposition: form-data; name=\"fieldNameLF\"" + "Content-Type: text/plain"
            },
            "Content with LF.",
            LF)
      },
      {
        2,
        "Missing Content-Disposition",
        inputBytes(
            new String[] {"Content-Type: application/octet-stream"},
            "", // Empty body
            CRLF)
      },
      {
        3,
        "Malformed (incomplete) Content-Disposition",
        inputBytes(
            new String[] {"Content-Disposition: ", "Content-Type: application/xml"},
            "<root/>",
            CRLF)
      },
      {
        4,
        "Missing required name within Content-Disposition",
        inputBytes(
            new String[] {
              "Content-Disposition: form-data; filename=\"other.zip\"",
              "Content-Type: application/octet-stream"
            },
            "<root/>",
            CRLF)
      },
      {
        5,
        "Malformed C-D header",
        inputBytes(
            new String[] {
              "Content-Disposition form-data; name=\"badheader\"", "Content-Type: text/plain"
            },
            "Content after bad header.",
            CRLF)
      }
    };
  }

  @Test(dataProvider = "failureDataProvider")
  public void failureCases(Integer casenum, String label, byte[] inputBytes) throws IOException {
    Part result = Part.parse(inputBytes);
    Assert.assertNull(result, String.format("case %d, %s", casenum, label));
  }
}
