/*
 * Copyright 2009-2011 MBTE Sweden AB.
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

package groovypp.concurrent

import org.codehaus.groovy.util.AbstractConcurrentMap

@Trait abstract class AtomicMapEntry<K,V> implements AbstractConcurrentMap.Entry<K,V> {
    K key

    V getValue () { this }

    int hash

    boolean isEqual(K key, int hash) {
        this.hash == hash && this.key == key
    }

    void setValue(V value) {}

    boolean isValid() { true }
}