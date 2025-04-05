# Apigee Multipart Form Callout

This directory contains the Java source code and pom.xml file required to build
a Java callout for Apigee that creates a multipart form payload, or parses an
inbound payload with MIME-type `multipart/form-data`, as is defined by [IETF RFC
7578](https://www.rfc-editor.org/rfc/rfc7578).


> There is a different callout, created some time ago, available at
[Apigee-Java-Simple-MultipartForm](https://github.com/DinoChiesa/Apigee-Java-Simple-MultipartForm).
> There are some drawbacks. 
> - That one has a few external dependencies, which then brought in dependencies on
>   the Servlet API, and some other things. Those dependencies are less than optimal,
>   for just a stream parsing / consolidation tool.  
> - there are two ways to
>   configure it, and explaining and supporting those two ways was somewhat
>   complicated. 
> - it has not been tested on Apigee X.


This new implementation has none of those drawbacks. It works similarly, but is
simpler. This one has fewer external dependencies - only one for reading/parsing
JSON. It works on Apigee X.


## Disclaimer

This example is not an official Google product, nor is it part of an official
Google product.

## Example payload

The kinds of payloads the callout can parse or generate are like so:

```
POST / HTTP/1.1
Host: my-host-name
User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:29.0) Gecko/20100101 Firefox/29.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
Accept-Language: en-US,en;q=0.5
Accept-Encoding: gzip, deflate
Connection: keep-alive
Content-Type: multipart/form-data; boundary=---------------------------e3d33b704923419d
Content-Length: 554

-----------------------------e3d33b704923419d
Content-Disposition: form-data; name="text"

text default
-----------------------------e3d33b704923419d
Content-Disposition: form-data; name="file1.txt"
Content-Type: text/plain

Content of file1.txt.

-----------------------------e3d33b704923419d
Content-Disposition: form-data; name="file2.png"
Content-Type: image/png

...image bytes here...

-----------------------------e3d33b704923419d--
```

## Using this policy

You do not need to build the source code in order to use the policy in
Apigee.  All you need is the built JAR, and the appropriate
configuration for the policy.  If you want to build it, feel free.  The
instructions are at the bottom of this readme. Even without
instructions, you should be able to figure it out if you know and use
maven.

1. copy the jar file, available in
   target/apigee-multipart-form-20250405.jar , if you have built the
   jar, or in [the repo](bundle/apiproxy/resources/java/apigee-multipart-form-20250405.jar)
   if you have not, to your apiproxy/resources/java directory. You can
   do this offline, or using the graphical Proxy Editor in the Apigee
   Admin UI.

2. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:

   ```xml
    <JavaCallout name='Java-Multipart-Form-1'>
        ...
      <ClassName>com.google.apigee.callouts.MultipartFormCreatorV2</ClassName>
      <ResourceURL>java://apigee-multipart-form-20250404.jar</ResourceURL>
    </JavaCallout>
   ```

3. attach that policy in the appropriate place in your API Proxy.

3. use the Apigee UI, or a command-line tool like
   [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js-examples/blob/main/importAndDeploy.js),
   [apigeecli](https://github.com/apigee/apigeecli)
   or similar to
   import your proxy into an Apigee organization, and then deploy the proxy .
   Examples:
   ```sh
   ORG=my-org
   ENV=eval
   importAndDeploy.js --token $TOKEN -v -o $ORG -e $ENV -d bundle/`
   ```
   or
   ```sh
   ORG=my-org
   ENV=eval
   apigeecli apis create bundle -f ./bundle/apiproxy --name multipart-form -o $ORG  --token $TOKEN
   apigeecli apis deploy --wait --name multipart-form --ovr --rev 1 --org $ORG --env $ENV --token "$TOKEN"
   ```


4. Use a client to generate and send http requests to the proxy you just deployed . Eg,
   ```
   # Apigee Edge
   endpoint=https://$ORG-$ENV.apigee.net
   # Apigee X/hybrid
   endpoint=https://my-custom-endpoint.net
   curl -i -X POST -d '' $endpoint/multipart-form/create-multi
   ```


## Notes on Usage

This repo includes three callout classes,

* com.google.apigee.callouts.MultipartFormCreatorV2 - create a form payload

* com.google.apigee.callouts.MultipartFormParserV2 - parse a form payload

* com.google.apigee.callouts.ContentSetter - set a byte array into message content

## MultipartFormCreatorV2

This callout will create a form payload, using inputs that you specify.

It accepts two properties as input:

| property name   | description                                                                                                 |
|-----------------|-------------------------------------------------------------------------------------------------------------|
| **descriptor**  | required\*. a JSON string, which describes the parts to add to the form. See details below.                 |
| **destination** | optional, a string, the name of a message. If it does not exist, it will be created. Defaults to 'message'. |


An example for creating a form:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="descriptor">
    {
      "part1" : {
        "content-var" :  "variable-holding-text",
        "content-type" : "text/plain",
        "want-b64-decode": false
      },
      "part2.png" : {
        "content-var" :  "base64-encoded-image-bytes",
        "content-type" : "image/png",
        "want-b64-decode": true,
        "file-name": "part2.png"
      }
    }
    </Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.MultipartFormCreatorV2</ClassName>
  <ResourceURL>java://apigee-multipart-form-20250404.jar</ResourceURL>
</JavaCallout>
```

This will tell the policy to create a multipart form, store it in `message`
(because there is no destination property), and include 2 content parts within
that form:

1. the first part will be a field. It will have content-type = text/plain, named part1.
   It will get content from a variable named `variable-holding-text`.

2. the second part will be a file, with content-type = image/png, named part2.png, using
   content from a variable named `base64-image-bytes`.  That string will be base64 decoded
   before being placed into the form.


How you get the data into the specified variables is up to you! The result of
the policy above would be a form with content like this:

```
----------------------G70E38XDL4FRMV
Content-Disposition: form-data; name="part1"
Content-Type: text/plain

Hello, World
----------------------G70E38XDL4FRMV
Content-Disposition: form-data; name="part2.png"; filename="part2.png"
Content-Type: image/png

PNG

...png data here...
----------------------G70E38XDL4FRMV--
```


There is one additional possible field in the descriptor, not shown above
`transfer-encoding`. If it is present, then the policy will add a header
specifying that `content-transfer-encoding` for the given part.  For example, to
send a base64-encoded file and mark it as such, use this as a descriptor:

```
    {
      "part2.png" : {
        "content-var" :  "base64-encoded-image-bytes",
        "content-type" : "image/png",
        "want-b64-decode": false,
        "transfer-encoding": "base64"
      }
    }
```

And the output will be like so:

```
----------------------73B8NBN4LFYLBB
Content-Disposition: form-data; name="image"
Content-Type: image/png
Content-Transfer-Encoding: base64

iVBOR...base64-encoded data here...
----------------------73B8NBN4LFYLBB--
```

The `content-transfer-encoding` header has been deprecated by [RFC
7578](https://www.rfc-editor.org/rfc/rfc7578#section-4.7).


## MultipartFormParserV2

This callout will parse a form, using the content of the specified message as input.

It accepts a single parameter as input:

| property name  | status   | description                                                                                     |
|----------------|----------|-------------------------------------------------------------------------------------------------|
| **source**     | optional | name of a variable containing a message, containing a form. defaults to "message".              |
| **size-limit** | optional | a number expressing the size limit of for parts the callout should parse. defaults to no limit. |

An example for parsing a form:

```xml
<JavaCallout name='Java-CreateMultipartForm'>
  <Properties>
    <Property name="source">message</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.MultipartFormParserV2</ClassName>
  <ResourceURL>java://apigee-multipart-form-20250404.jar</ResourceURL>
</JavaCallout>
```

The inbound message should have headers like this:
```
content-length: 16301
content-type: multipart/form-data; boundary=--------------------9WTvUeO4O5
...
```

The inbound message payload should look something like this:
```
--------------------9WTvUeO4O5
Content-Disposition: form-data; filename="whatever.txt"
Content-Type: text/plain

Hello World
--------------------9WTvUeO4O5
...
```

The callout sets variables in the context containing information about the parts of the inbound form.


| variable name           | description                                                                                     |
|-------------------------|-------------------------------------------------------------------------------------------------|
| **items**               | String, a comma-separated list of file items from the form.                                     |
| **itemcount**           | String, a number indicating the number of  file items found in the form.                        |
| **item_filename_N**     | name of item number N.                                                                          |
| **item_content_N**      | content for item N.  This is a byte array. You may need to decode it using a subsequent policy. |
| **item_content-type_N** | String, the content-type for item N.                                                            |
| **item_size_N**         | String, the size in bytes of the content for item N.                                            |

Subsequent policies can then read these variables and operate on them.

The callout will simply ignore any part that exceeds the configured `size-limit`.


## ContentSetter

This is a companion class. It will set a byte array into a message content. 
This may be useful after parsing a multipart form, if you want to set ONE of the parsed items into the message content of a different message.

Normally you would use the Apigee `AssignMessage` policy to do this, like so:
```
<AssignMessage name="AM-Response">
    <AssignVariable>
        <Name>response.content</Name>
        <Ref>mpf_item_content_1</Ref>
    </AssignVariable>
    <IgnoreUnresolvedVariables>false</IgnoreUnresolvedVariables>
</AssignMessage>
```

...which says to use the output of a prior "parse" callout as the full content for this response message.

But AssignMessage seems to always treat the referenced variable as a string, so the result is that the response.content gets a string like "[B@1cf582a1". Not helpful.

This callout avoids that pitfall.

It accepts two properties as input:

| property name   | description                                                                                                       |
|-----------------|-------------------------------------------------------------------------------------------------------------------|
| **destination** | optional, a string, the name of a message. If it does not exist, it will be created. Defaults to 'message'.       |
| **contentVar**  | required. the name of a context variable, which contains a byte array or string.                                  |
| **contentType** | optional. the value to set into the content-type header of the message. Default: don't set a content-type header. |

Example:

```xml
<JavaCallout name='Java-SetContent>
  <Properties>
    <Property name="destination">message</Property>
    <Property name="contentVar">mpf_item_content_1</Property>
    <Property name="contentType">{mpf_item_content-type_1</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.ContentSetter</ClassName>
  <ResourceURL>java://apigee-multipart-form-20250404.jar</ResourceURL>
</JavaCallout>
```


## Example API Proxy

You can find an example proxy bundle that uses the policy, [here in this repo](bundle/apiproxy).
The example proxy accepts a post.

You must deploy the proxy in order to invoke it.

Invoke it like this:

* Create a form, with multiple parts, inserting into a new message
  ```
    curl -i -X POST -d '' $endpoint/multipart-form/create-multi
  ```

  This flow in the example proxy uses AssignMessage to assign a couple values to
  variables, and then references those variables in the descriptor provided to
  the policy. The flow then invokes the policy, which creates the form payload.
  The proxy then sends the form to a backend system.

  NB: The backend system as of this moment does not correctly handle the
  form.  This is because the backend doesn't handle forms; it's not
  because the form is invalid.


* Create a form with a single part, an image file, using "message":
  ```
    curl -i -X POST -d '' $endpoint/multipart-form/create-1a
  ```

  This flow uses the "one part" option for configuration.
  Within the flow for this request, the proxy assigns a fixed string value to
  a variable, and uses THAT as the contentVar for the policy.  It then
  invokes the policy, which creates the form payload.  The proxy then
  just sends that payload as the response (does not proxy to an upstream system).


* Create a form with a single part, using a new message
  ```
    curl -i -X POST -d '' $endpoint/multipart-form/create-1b
  ```

* Parse a multipart-form request

  You can use curl to create a multi-part form request.  Some examples:

  This form will have 2 parts: both files, one plaintext and one an image:
  ```
  curl -i \
    -F README.md=@README.md \
    -F image.png=@callout/src/test/resources/test-data/Logs_512px.png \
    $endpoint/multipart-form/parse-1
  ```

  This form will have 2 parts: a regular field, as well as one file.
  ```
  curl -i \
    -F field1=value-goes-here \
    -F image.png=@callout/src/test/resources/test-data/Logs_512px.png \
    $endpoint/multipart-form/parse-1

  ```



## Building

Building from source requires Java 1.8, and Maven.

1. unpack (if you can read this, you've already done that).

2. Before building _the first time_, configure the build on your machine by
   loading the Apigee jars into your local cache:

  ```
  ./buildsetup.sh
  ```

3. Build with maven.
  ```
  mvn clean package
  ```

  This will build the jar and also run all the tests, and copy the jar to the
  resource directory in the sample apiproxy bundle.


## License

This material is Copyright © 2018-2025 Google LLC, and is licensed under the
[Apache 2.0 License](LICENSE). This includes the Java code as well as the API
Proxy configuration.

## Bugs

* The automated tests are pretty thin.
* The size limit for the uploaded files enforced by the callout is hard-coded. There is no way to adjust the it, without re-compiling.




