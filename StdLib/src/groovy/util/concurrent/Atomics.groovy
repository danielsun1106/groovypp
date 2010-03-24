package groovy.util.concurrent

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicLongFieldUpdater

/**
 * Operations on atomic primitives
 */
@Typed class Atomics {
    static <S> S apply (AtomicReference<S> self, Function1<S,S> mutation) {
        for (;;) {
            def s = self.get()
            def newState = mutation(s)
            if (self.compareAndSet(s, newState))
                return newState
        }
    }

    static <T,S> S apply (AtomicReferenceFieldUpdater<T,S> self, T obj, Function1<S,S> mutation) {
        for (;;) {
            def s = self.get(obj)
            def newState = mutation(s)
            if (self.compareAndSet(obj, s, newState))
                return newState
        }
    }

    static <S> S apply (AtomicReferenceArray<S> self, int index, Function1<S,S> mutation) {
        for (;;) {
            def s = self.get(index)
            def newState = mutation(s)
            if (self.compareAndSet(index, s, newState))
                return newState
        }
    }

    static <S,R> R apply (AtomicReference<S> self, Function1<S,R> mutator, Function1<R,S> extractor) {
        for (;;) {
            def oldState = self.get()
            def mutated = mutator(oldState)
            if (self.compareAndSet(oldState, extractor(mutated)))
                return mutated
        }
    }

    static int apply (AtomicInteger self, Function1<Integer,Integer> mutation) {
        for (;;) {
            def s = self.get()
            def newState = mutation(s)
            if (self.compareAndSet(s, newState))
                return newState
        }
    }

    static <T> int apply (AtomicIntegerFieldUpdater<T> self, T obj, Function1<Integer,Integer> mutation) {
        for (;;) {
            def s = self.get(obj)
            def newState = mutation(s)
            if (self.compareAndSet(obj, s, newState))
                return newState
        }
    }

    static int apply (AtomicIntegerArray self, int index, Function1<Integer,Integer> mutation) {
        for (;;) {
            def s = self.get(index)
            def newState = mutation(s)
            if (self.compareAndSet(index, s, newState))
                return newState
        }
    }

    static long apply (AtomicLong self, Function1<Long,Long> mutation) {
        for (;;) {
            def s = self.get()
            def newState = mutation(s)
            if (self.compareAndSet(s, newState))
                return newState
        }
    }

    static <T> long apply (AtomicLongFieldUpdater self, T obj, Function1<Long,Long> mutation) {
        for (;;) {
            def s = self.get(obj)
            def newState = mutation(s)
            if (self.compareAndSet(obj, s, newState))
                return newState
        }
    }

    static long apply (AtomicLongArray self, int index, Function1<Long,Long> mutation) {
        for (;;) {
            def s = self.get(index)
            def newState = mutation(s)
            if (self.compareAndSet(index, s, newState))
                return newState
        }
    }

    static boolean apply (AtomicBoolean self, Function1<Boolean,Boolean> mutation) {
        for (;;) {
            def s = self.get()
            def newState = mutation(s)
            if (self.compareAndSet(s, newState))
                return newState
        }
    }

    static <S> boolean tryApply (AtomicReference<S> state, Function1<S,S> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }

    static <T,S> boolean tryApply (AtomicReferenceFieldUpdater<T,S> state, T obj, Function1<S,S> mutation) {
        def s = state.get(obj)
        state.compareAndSet(obj, s, mutation(s))
    }

    static boolean tryApply (AtomicInteger state, Function1<Integer,Integer> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }

    static <T> boolean tryApply (AtomicIntegerFieldUpdater<T> state, T obj, Function1<Integer,Integer> mutation) {
        def s = state.get(obj)
        state.compareAndSet(obj, s, mutation(s))
    }

    static boolean tryApply (AtomicLong state, Function1<Long,Long> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }

    static <T> boolean tryApply (AtomicLongFieldUpdater<T> state, T obj, Function1<Long,Long> mutation) {
        def s = state.get(obj)
        state.compareAndSet(obj, s, mutation(s))
    }

    static boolean tryApply (AtomicBoolean state, Function1<Boolean,Boolean> mutation) {
        def s = state.get()
        state.compareAndSet(s, mutation(s))
    }
}
