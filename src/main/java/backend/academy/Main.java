package backend.academy;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Thread)
public class Main {
    private Student student;
    private Method method;
    private MethodHandle methodHandle;
    private NameInterface lambdaFactoryMethod;
    private static final String TARGET_METHOD = "name";

    @SuppressWarnings("magicnumber")
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(Main.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .forks(1)
            .warmupForks(1)
            .warmupIterations(1)
            .warmupTime(TimeValue.seconds(5))
            .measurementIterations(1)
            .measurementTime(TimeValue.seconds(50))
            .build();

        new Runner(options).run();
    }

    @Setup
    public void setUpBenchmark() throws Throwable {
        student = new Student("Oleynik", "Nikita");

        method = Student.class.getMethod(TARGET_METHOD);

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodType methodType = MethodType.methodType(String.class);
        methodHandle = lookup.findVirtual(Student.class, TARGET_METHOD, methodType).bindTo(student);

        MethodHandles.Lookup privateLookup = MethodHandles.lookup();
        MethodHandle baseHandle = privateLookup.findVirtual(Student.class, TARGET_METHOD, methodType);
        CallSite lambdaSite = LambdaMetafactory.metafactory(
            privateLookup,
            TARGET_METHOD,
            MethodType.methodType(NameInterface.class, Student.class),
            methodType,
            baseHandle,
            methodType
        );

        MethodHandle factoryHandle = lambdaSite.getTarget().bindTo(student);
        lambdaFactoryMethod = (NameInterface) factoryHandle.invoke();
    }

    @Benchmark
    public void benchmarkDirectAccess(Blackhole blackhole) {
        String result = student.name();
        blackhole.consume(result);
    }

    @Benchmark
    public void benchmarkReflection(Blackhole blackhole) throws IllegalAccessException, InvocationTargetException {
        Object result = method.invoke(student);
        blackhole.consume(result);
    }

    @Benchmark
    public void benchmarkMethodHandle(Blackhole blackhole) throws Throwable {
        Object result = methodHandle.invoke();
        blackhole.consume(result);
    }

    @Benchmark
    public void benchmarkLambdaFactory(Blackhole blackhole) {
        Object result = lambdaFactoryMethod.name();
        blackhole.consume(result);
    }

    @FunctionalInterface
    public interface NameInterface {
        String name();
    }

    public record Student(String name, String surname) {
    }
}
