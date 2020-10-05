/**
 * Copyright (c) Microsoft Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.playwright.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.*;

import java.util.*;

import static com.microsoft.playwright.Frame.LoadState.*;
import static com.microsoft.playwright.impl.Serialization.deserialize;
import static com.microsoft.playwright.impl.Serialization.serializeArgument;
import static com.microsoft.playwright.impl.Utils.isFunctionBody;

public class FrameImpl extends ChannelOwner implements Frame {
  PageImpl page;
  private final Set<LoadState> loadStates = new HashSet<>();

  FrameImpl(ChannelOwner parent, String type, String guid, JsonObject initializer) {
    super(parent, type, guid, initializer);

    for (JsonElement item : initializer.get("loadStates").getAsJsonArray()) {
      loadStates.add(loadStateFromProtocol(item.getAsString()));
    }
  }

  private static LoadState loadStateFromProtocol(String value) {
    switch (value) {
      case "load": return LOAD;
      case "domcontentloaded": return DOMCONTENTLOADED;
      case "networkidle": return NETWORKIDLE;
      default: throw new RuntimeException("Unexpected value: " + value);
    }
  }

  public <T> T evalTyped(String expression) {
    return (T) evaluate(expression, null, false);
  }

  private Object evaluate(String expression, Object arg, boolean forceExpression) {
    JsonObject params = new JsonObject();
    params.addProperty("expression", expression);
    params.addProperty("world", "main");
    if (!isFunctionBody(expression)) {
      forceExpression = true;
    }
    params.addProperty("isFunction", !forceExpression);
    params.add("arg", new Gson().toJsonTree(serializeArgument(arg)));
    JsonElement json = sendMessage("evaluateExpression", params);
    SerializedValue value = new Gson().fromJson(json.getAsJsonObject().get("value"), SerializedValue.class);
    return deserialize(value);
  }

  @Override
  public ElementHandle querySelector(String selector) {
    return null;
  }

  @Override
  public List<ElementHandle> querySelectorAll(String selector) {
    return null;
  }

  @Override
  public Object evalOnSelector(String selector, String pageFunction, Object arg) {
    JsonObject params = new JsonObject();
    params.addProperty("selector", selector);
    params.addProperty("expression", pageFunction);
    params.addProperty("isFunction", isFunctionBody(pageFunction));
    params.add("arg", new Gson().toJsonTree(serializeArgument(arg)));
    JsonElement json = sendMessage("evalOnSelector", params);
    SerializedValue value = new Gson().fromJson(json.getAsJsonObject().get("value"), SerializedValue.class);
    return deserialize(value);
  }

  @Override
  public Object evalOnSelectorAll(String selector, String pageFunction, Object arg) {
    return null;
  }

  @Override
  public ElementHandle addScriptTag(AddScriptTagOptions options) {
    return null;
  }

  @Override
  public ElementHandle addStyleTag(AddStyleTagOptions options) {
    return null;
  }

  @Override
  public void check(String selector, CheckOptions options) {

  }

  @Override
  public List<Frame> childFrames() {
    return null;
  }


  private static String toProtocol(Mouse.Button button) {
    switch (button) {
      case LEFT: return "left";
      case RIGHT: return "right";
      case MIDDLE: return "middle";
      default: throw new RuntimeException("Unexpected value: " + button);
    }
  }

  private static JsonArray toProtocol(Set<Keyboard.Modifier> modifiers) {
    JsonArray result = new JsonArray();
    if (modifiers.contains(Keyboard.Modifier.ALT)) {
      result.add("Alt");
    }
    if (modifiers.contains(Keyboard.Modifier.CONTROL)) {
      result.add("Control");
    }
    if (modifiers.contains(Keyboard.Modifier.META)) {
      result.add("Meta");
    }
    if (modifiers.contains(Keyboard.Modifier.SHIFT)) {
      result.add("Shift");
    }
    return result;
  }

  @Override
  public void click(String selector, ClickOptions options) {
    if (options == null) {
      options = new ClickOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("selector", selector);

    params.remove("button");
    if (options.button != null) {
      params.addProperty("button", toProtocol(options.button));
    }

    params.remove("modifiers");
    if (options.modifiers != null) {
      params.add("modifiers", toProtocol(options.modifiers));
    }

    sendMessage("click", params);
  }

  @Override
  public String content() {
    return null;
  }

  @Override
  public void dblclick(String selector, DblclickOptions options) {
    if (options == null) {
      options = new DblclickOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("selector", selector);

    params.remove("button");
    if (options.button != null) {
      params.addProperty("button", toProtocol(options.button));
    }

    params.remove("modifiers");
    if (options.modifiers != null) {
      params.add("modifiers", toProtocol(options.modifiers));
    }

    sendMessage("dblclick", params);
  }

  @Override
  public void dispatchEvent(String selector, String type, Object eventInit, DispatchEventOptions options) {

  }

  @Override
  public Object evaluate(String pageFunction, Object arg) {
    return evaluate(pageFunction, arg, false);
  }

  @Override
  public JSHandle evaluateHandle(String pageFunction, Object arg) {
    return null;
  }

  @Override
  public void fill(String selector, String value, FillOptions options) {
    if (options == null) {
      options = new FillOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("selector", selector);
    params.addProperty("value", value);
    sendMessage("fill", params);
  }

  @Override
  public void focus(String selector, FocusOptions options) {

  }

  @Override
  public ElementHandle frameElement() {
    return null;
  }

  @Override
  public String getAttribute(String selector, String name, GetAttributeOptions options) {
    return null;
  }

  @Override
  public ResponseImpl navigate(String url, NavigateOptions options) {
    if (options == null) {
      options = new NavigateOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("url", url);
    JsonElement result = sendMessage("goto", params);
    return connection.getExistingObject(result.getAsJsonObject().getAsJsonObject("response").get("guid").getAsString());
  }

  @Override
  public void hover(String selector, HoverOptions options) {

  }

  @Override
  public String innerHTML(String selector, InnerHTMLOptions options) {
    return null;
  }

  @Override
  public String innerText(String selector, InnerTextOptions options) {
    return null;
  }

  @Override
  public boolean isDetached() {
    return false;
  }

  @Override
  public String name() {
    return null;
  }

  @Override
  public Page page() {
    return null;
  }

  @Override
  public Frame parentFrame() {
    return null;
  }

  @Override
  public void press(String selector, String key, PressOptions options) {

  }

  @Override
  public List<String> selectOption(String selector, String values, SelectOptionOptions options) {
    return null;
  }


  private static String toProtocol(SetContentOptions.WaitUntil waitUntil) {
    if (waitUntil == null) {
      waitUntil = SetContentOptions.WaitUntil.LOAD;
    }
    switch (waitUntil) {
      case DOMCONTENTLOADED: return "domcontentloaded";
      case LOAD: return "load";
      case NETWORKIDLE: return "networkidle";
      default: throw new RuntimeException("Unexpected value: " + waitUntil);
    }
  }

  @Override
  public void setContent(String html, SetContentOptions options) {
    if (options == null) {
      options = new SetContentOptions();
    }
    JsonObject params = new Gson().toJsonTree(options).getAsJsonObject();
    params.addProperty("html", html);
    params.remove("waitUntil");
    params.addProperty("waitUntil", toProtocol(options.waitUntil));
    sendMessage("setContent", params);
  }

  @Override
  public void setInputFiles(String selector, String files, SetInputFilesOptions options) {

  }

  @Override
  public String textContent(String selector, TextContentOptions options) {
    return null;
  }

  @Override
  public String title() {
    JsonElement json = sendMessage("title", new JsonObject());
    return json.getAsJsonObject().get("value").getAsString();
  }

  @Override
  public void type(String selector, String text, TypeOptions options) {

  }

  @Override
  public void uncheck(String selector, UncheckOptions options) {

  }

  @Override
  public String url() {
    return null;
  }

  @Override
  public JSHandle waitForFunction(String pageFunction, Object arg, WaitForFunctionOptions options) {
    return null;
  }

  @Override
  public void waitForLoadState(LoadState state, WaitForLoadStateOptions options) {
    if (state == null) {
      state = LOAD;
    }
    while (!loadStates.contains(state)) {
      // TODO: support timeout!
      connection.processOneMessage();
    }
  }

  @Override
  public Response waitForNavigation(WaitForNavigationOptions options) {
    return null;
  }

  @Override
  public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
    return null;
  }

  @Override
  public void waitForTimeout(int timeout) {

  }

  protected void handleEvent(String event, JsonObject params) {
    if ("loadstate".equals(event)) {
      JsonElement add = params.get("add");
      if (add != null) {
        loadStates.add(loadStateFromProtocol(add.getAsString()));
      }
      JsonElement remove = params.get("remove");
      if (remove != null) {
        loadStates.remove(loadStateFromProtocol(remove.getAsString()));
      }
    }
  }

}