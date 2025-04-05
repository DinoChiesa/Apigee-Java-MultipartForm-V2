// Copyright © 2024 Google, LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// All rights reserved.

package com.google.apigee.fakes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.apigee.flow.FlowInfo;
import com.apigee.flow.message.Connection;
import com.apigee.flow.message.FlowContext;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.apigee.flow.message.TransportMessage;
import com.apigee.flow.message.TransportMessageFactory;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Mockito;

public class FakeMessageContext implements MessageContext {
  private Map<String, Object> variables;
  private Message message;

  // public FakeMessageContext() {
  //   getVariables();
  // }

  public FakeMessageContext(Message message) {
    this.message = message;
    getVariables();
  }

  private Map<String, Object> getVariables() {
    if (variables == null) {
      variables = new HashMap<String, Object>();
    }
    return variables;
  }

  public <T> T getVariable(final String name) {
    return (T) getVariables().get(name);
  }

  public String getVariableAsString(final String name) {
    return (String) getVariables().get(name);
  }

  public boolean setVariable(final String name, final Object value) {
    System.out.printf("set(%s) = %s\n", name, value.toString());
    getVariables().put(name, value);
    return true;
  }

  public boolean removeVariable(final String name) {
    if (getVariables().containsKey(name)) {
      variables.remove(name);
    }
    return true;
  }

  public Message getMessage(FlowContext flowContext) {
    return message;
  }

  public Message getMessage() {
    return message;
  }

  public Connection getClientConnection() {
    Connection connection = Mockito.mock(Connection.class);
    TransportMessageFactory messageFactory = Mockito.mock(TransportMessageFactory.class);
    TransportMessage message = Mockito.mock(TransportMessage.class);
    doReturn(messageFactory).when(connection).getMessageFactory();
    doReturn(message).when(messageFactory).createRequest(any());
    return connection;
    // throw new UnsupportedOperationException();
  }

  public Message createMessage(TransportMessage transportMessage) {
    return new FakeMessage();
    // throw new UnsupportedOperationException();
  }

  /* ========================================================================= */
  /* Everything below this line is not implemented and not needed in this Fake */

  public void setMessage(FlowContext flowContext, Message message) {
    throw new UnsupportedOperationException();
  }

  public Message getRequestMessage() {
    throw new UnsupportedOperationException();
  }

  public void setRequestMessage(Message message) {
    throw new UnsupportedOperationException();
  }

  public Message getResponseMessage() {
    throw new UnsupportedOperationException();
  }

  public void setResponseMessage(Message message) {
    throw new UnsupportedOperationException();
  }

  public Message getErrorMessage() {
    throw new UnsupportedOperationException();
  }

  public void setErrorMessage(Message message) {
    throw new UnsupportedOperationException();
  }

  public Connection getTargetConnection() {
    throw new UnsupportedOperationException();
  }

  public <T extends FlowInfo> T getFlowInfo(String identifier) {
    throw new UnsupportedOperationException();
  }

  public boolean addFlowInfo(FlowInfo flowInfo) {
    throw new UnsupportedOperationException();
  }

  public void removeFlowInfo(String identifier) {
    throw new UnsupportedOperationException();
  }

  public <T extends Comparable<T>> T get(String name) {
    throw new UnsupportedOperationException();
  }
}
