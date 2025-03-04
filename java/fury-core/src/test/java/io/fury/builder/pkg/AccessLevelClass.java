/*
 * Copyright 2023 The Fury Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fury.builder.pkg;

public class AccessLevelClass {
  static class PkgLevelClass {
    int f1;
  }

  static class PrivateLevelClass {
    int f1;
  }

  PkgLevelClass f1;
  PrivateLevelClass f2;
  private PrivateLevelClass f3;
}
