/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.util.concurrent

@Typed class FListSerialTest extends FSerialTestCase {

    void testEmpty() {
        def res = fromBytes(toBytes(FList.emptyList))
        assert res instanceof FList
        FList r = res
        assert ((FList)res).size() == 0
        assert FList.emptyList === r
    }

    void testOneEl() {
        def res = fromBytes(toBytes(FList.emptyList + "one"))
        assert res instanceof FList
        FList r = res
        assert r.size() == 1
        assert r.head == "one"
    }

    void testSeveralEl() {
        def res = fromBytes(toBytes(FList.emptyList + "one" + "two" + "three"))
        assert res instanceof FList
        FList r = res
        assert r.size() == 3
        assert r.head == "three"
        assert r.tail.head == "two"
        assert r.tail.tail.head == "one"
    }
}
