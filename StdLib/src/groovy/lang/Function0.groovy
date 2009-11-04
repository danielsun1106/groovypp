package groovy.lang

@Trait
abstract class Function0<R> {

    abstract R apply ()

    public <R1> Function0<R1> andThen (Function1<R,R1> g) {
        { -> g.apply(apply()) }
    }
}