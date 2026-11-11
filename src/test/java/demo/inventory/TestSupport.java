package demo.inventory;

import java.util.Objects;

/** Small explicit test runner; it does not depend on JVM -ea or third-party jars. */
final class TestSupport {
    private int passed;
    private int failed;

    void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (AssertionError | RuntimeException failure) {
            failed++;
            System.out.println("FAIL  " + name);
            System.out.println("      " + failure.getClass().getSimpleName()
                    + ": " + failure.getMessage());
        }
    }

    int finish(String suite) {
        System.out.printf("%s: tests=%d, passed=%d, failed=%d%n",
                suite, passed + failed, passed, failed);
        return failed == 0 ? 0 : 1;
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void equal(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + "; expected=" + expected + ", actual=" + actual);
        }
    }

    static void rejectsInvalid(Runnable operation) {
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    static Snapshot current(InventoryLedger ledger, String sku, String warehouse) {
        return ledger.find(sku, warehouse).orElseThrow(
                () -> new AssertionError("Missing snapshot for " + sku + " @ " + warehouse));
    }
}
