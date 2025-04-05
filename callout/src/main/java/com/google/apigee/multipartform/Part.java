// Copyright © 2021-2025 Google LLC.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Part {
  private byte[] partContent;

  public void setPartContent(byte[] value) {
    this.partContent = value;
  }

  public byte[] getPartContent() {
    return partContent;
  }

  public int getSize() {
    return partContent.length;
  }

  private String fileName;

  public void setFileName(String value) {
    this.fileName = value;
  }

  public String getFileName() {
    return fileName;
  }

  private String transferEncoding;

  public void setTransferEncoding(String value) {
    this.transferEncoding = value;
  }

  public String getTransferEncoding() {
    return transferEncoding;
  }

  private String contentType;

  public void setContentType(String value) {
    this.contentType = value;
  }

  public String getContentType() {
    return contentType;
  }

  private String name;

  public String getName() {
    return this.name;
  }

  public Part(String partName) {
    this.name = partName;
  }

  private static String lineFrom(InputStream in) throws IOException {
    byte[] buf = new byte[256];
    int pos = 0;
    int prev = 0;
    int cur = 0;
    for (; ; ) {
      cur = in.read();
      if (cur == -1) break;
      // https://www.rfc-editor.org/rfc/rfc2046#section-5.1 says always CRLF
      if (cur == '\n' && prev == '\r') break;
      buf[pos++] = (byte) cur;
      if (pos == buf.length) {
        // expand
        buf = Arrays.copyOf(buf, pos + 256);
      }
      prev = cur;
    }
    // omit trailing CR

    return (pos > 0) ? new String(Arrays.copyOf(buf, pos - 1), "UTF-8") : null;
  }

  public static Part parse(byte[] bytes) throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    String partName = null;
    String ctype = null;
    for (; ; ) {
      String hdr = lineFrom(bis);
      if (hdr == null || hdr.length() == 0) break; // end of headers
      List<String> components =
          Arrays.stream(hdr.split(":", 2)).map(s -> s.trim()).collect(Collectors.toList());

      String headerName = components.get(0).toLowerCase();
      if (headerName.equals("content-disposition")) {
        Pattern pattern = Pattern.compile("\\bname=(['\"]?)([^'\"]+)\\1(?:;|\\sb|$)");
        Matcher matcher = pattern.matcher(components.get(1));
        if (matcher.find()) {
          partName = matcher.group(2);
        }
      } else if (headerName.equals("content-type")) {
        ctype = components.get(1);
      }
    }

    if (partName == null) {
      return null;
    }
    // https://www.rfc-editor.org/rfc/rfc7578.html#section-4.4 default to text/plain
    Part part = new Part(partName).withContentType(ctype == null ? "text/plain" : ctype);

    // remaining data is content
    int b;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    while ((b = bis.read()) != -1) {
      baos.write(b);
    }
    part.setPartContent(baos.toByteArray());
    return part;
  }

  public Part withContentType(String contentType) {
    if (contentType == null) {
      this.contentType = "text/plain";
    } else {
      this.contentType = contentType;
    }
    return this;
  }

  public Part withPartContent(byte[] partContent) {
    this.partContent = partContent;
    return this;
  }

  public Part withTransferEncoding(String transferEncoding) {
    this.transferEncoding = transferEncoding;
    return this;
  }

  public Part withFileName(String fileName) {
    this.fileName = fileName;
    return this;
  }
}
