/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package java.util.regex;

public class Pattern {

    public static final int DOTALL = 0x20;
    public static final int CASE_INSENSITIVE = 0x02;
    public static final int MULTILINE = 0x08;

    public static Pattern compile(String regex) {
        return null;
    }

    public Matcher matcher(CharSequence input) {
        return null;
    }

    public static Pattern compile(String regex, int flags) {
        return null;
    }

    public String[] split(CharSequence input, int limit) {
        return null;
    }
}