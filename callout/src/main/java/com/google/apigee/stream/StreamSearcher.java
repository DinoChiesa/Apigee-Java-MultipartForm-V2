// Copyright © 2018-2025 Google LLC.
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

package com.google.apigee.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** A stream searching class based on the Knuth-Morris-Pratt algorithm. */
public class StreamSearcher {
  protected byte[] pattern;
  protected int[] borders;
  protected int partLimit;
  public static final int MAX_PATTERN_LENGTH = 512;

  public StreamSearcher(byte[] pattern, int partLimit) {
    if (pattern.length > MAX_PATTERN_LENGTH) {
      throw new IllegalStateException(
          String.format("length of pattern exceeds maximum (%d)", MAX_PATTERN_LENGTH));
    }
    this.pattern = Arrays.copyOf(pattern, pattern.length);
    this.borders = new int[pattern.length + 1];
    this.partLimit = partLimit;
    preProcess();
  }

  public StreamSearcher(byte[] pattern) {
    this(pattern, 0);
  }

  /**
   * Searches for the pattern in the stream, starting from the current stream position. The position
   * of the stream is changed. Callers may want to use a BufferedInputStream, and use mark() and
   * reset() before and after the search if necessary. If a match is found, the stream points to the
   * end of the match: the first byte AFTER the pattern. Else, the stream is entirely consumed.
   *
   * @return bytes consumed if found, -1 otherwise.
   * @throws IOException
   */
  public long search(InputStream stream) throws IOException {
    long bytesRead = 0;
    int b;
    int j = 0;

    while ((b = stream.read()) != -1) {
      bytesRead++;

      if (partLimit > 0 && bytesRead > partLimit) {
        return -1;
      }
      while (j >= 0 && (byte) b != pattern[j]) {
        j = borders[j];
      }
      // Move to the next character in the pattern.
      ++j;

      // If we've matched up to the full pattern length, we found it.
      if (j == pattern.length) {
        return bytesRead;
      }
    }
    return -1;
  }

  public byte[] searchAndExtract(InputStream stream) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    long bytesRead = 0;
    int b;
    int j = 0;

    while ((b = stream.read()) != -1) {
      bytesRead++;
      baos.write(b);

      if (partLimit > 0 && bytesRead > partLimit) {
        return null;
      }
      while (j >= 0 && (byte) b != pattern[j]) {
        j = borders[j];
      }
      // Move to the next character in the pattern.
      ++j;

      // Found.
      if (j == pattern.length) {
        byte[] a = baos.toByteArray();
        // Return the bytes for this part, without the separator, and without
        // the preceding CRLF and the following CRLF.
        return Arrays.copyOfRange(a, 2, a.length - pattern.length - 2);
      }
    }
    return null;
  }

  protected void preProcess() {
    int i = 0;
    int j = -1;
    this.borders[i] = j;
    while (i < this.pattern.length) {
      while (j >= 0 && pattern[i] != pattern[j]) {
        j = borders[j];
      }
      borders[++i] = ++j;
    }
  }
}
