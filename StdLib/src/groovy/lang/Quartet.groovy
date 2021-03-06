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
package groovy.lang

@Typed class Quartet<T1,T2,T3,T4,T> implements Externalizable{
    T1 first
    T2 second
    T3 third
    T4 forth

    Quartet () {}

    Quartet(T1 first, T2 second, T3 third, T4 forth) {
      this.first = first;
      this.second = second
      this.third = third
      this.forth = forth
    }

    boolean equals(obj) {
      this === obj || (obj instanceof Quartet && eq(first, ((Quartet) obj).first) && eq(second, ((Quartet) obj).second) && eq(third, ((Quartet) obj).third) && eq(forth, ((Quartet) obj).forth))
    }

    private boolean eq(obj1, obj2) {
      obj1 ? obj2 == null : obj1.equals(obj2)
    }

    int hashCode() {
      31*(31*(31 * first?.hashCode () + second?.hashCode ()) + third?.hashCode())+ forth?.hashCode()
    }

    String toString() {
        "[first: $first, second: $second, third: $third, forth: $forth]"
    }
}
