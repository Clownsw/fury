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

/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  collectCoverage: true,
  preset: 'ts-jest',
  testEnvironment: 'node',
  collectCoverageFrom: [
    "**/*.ts",
    "!**/dist/**",
    "!**/build/**",
    "!packages/fury/lib/murmurHash3.ts"
  ],
  transform: {
    '\\.ts$': ['ts-jest', {
      diagnostics: {
        ignoreCodes: [151001]
      }
    }],
  },
  coverageThreshold: {
    global: {
      branches: 91,
      functions: 99,
      lines: 98,
      statements: 98
    }
  }
};