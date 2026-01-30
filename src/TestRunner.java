import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestRunner {
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("Running Tests...");
        
        runTests(FireEventTest.class);
        runTests(DroneResultTest.class);
        runTests(SchedulerTest.class);
        runTests(SubsystemsTest.class); 

        System.out.println("\n--------------------------------------------------");
        System.out.println("Test Summary:");
        System.out.println("Total: " + testsRun);
        System.out.println("Passed: " + testsPassed);
        System.out.println("Failed: " + testsFailed);
        System.out.println("--------------------------------------------------");
        
        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    public static void runTests(Class<?> testClass) {
        System.out.println("\nExecuting: " + testClass.getSimpleName());
        
        try {
            Object instance = testClass.getDeclaredConstructor().newInstance();
            for (Method method : testClass.getDeclaredMethods()) {
                if (method.getName().startsWith("test")) {
                    testsRun++;
                    try {
                        System.out.print("  - " + method.getName() + "... ");
                        method.invoke(instance);
                        System.out.println("PASS");
                        testsPassed++;
                    } catch (Exception e) {
                        System.out.println("FAIL");
                        testsFailed++;
                        e.getCause().printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not instantiate " + testClass.getSimpleName());
            e.printStackTrace();
        }
    }

    // Assertion Helpers
    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message + " Expected: <" + expected + "> but was: <" + actual + ">");
        }
    }
    
    public static void assertNotNull(Object actual, String message) {
        if (actual == null) {
            throw new AssertionError(message + " Expected not null");
        }
    }
}
