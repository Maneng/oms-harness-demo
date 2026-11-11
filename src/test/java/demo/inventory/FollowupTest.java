package demo.inventory;

import static demo.inventory.TestSupport.check;
import static demo.inventory.TestSupport.current;
import static demo.inventory.TestSupport.equal;

/** Additional acceptance checks for a follow-up in the same task/thread. */
public final class FollowupTest {
    public static void main(String[] args) {
        TestSupport tests = new TestSupport();
        tests.run("same version with different quantity is still ignored", FollowupTest::conflictingDuplicate);
        tests.run("version zero is a valid first snapshot", FollowupTest::initialVersionZero);
        tests.run("newer version is accepted after an older event was rejected", FollowupTest::newerAfterRejectedOlder);
        System.exit(tests.finish("Follow-up suite"));
    }

    private static void conflictingDuplicate() {
        InventoryLedger ledger = new InventoryLedger();
        ledger.apply("SKU-A", "WH-US", 12, 10);
        boolean accepted = ledger.apply("SKU-A", "WH-US", 999, 10);
        equal(new Snapshot(12, 10), current(ledger, "SKU-A", "WH-US"),
                "A changed quantity does not make the same version newer");
        check(!accepted, "Conflicting duplicate must return false");
    }

    private static void initialVersionZero() {
        InventoryLedger ledger = new InventoryLedger();
        check(ledger.apply("SKU-ZERO", "WH-US", 0, 0), "Initial version zero must be accepted");
        equal(new Snapshot(0, 0), current(ledger, "SKU-ZERO", "WH-US"), "Zero version and quantity are valid");
    }

    private static void newerAfterRejectedOlder() {
        InventoryLedger ledger = new InventoryLedger();
        ledger.apply("SKU-A", "WH-US", 12, 10);
        check(!ledger.apply("SKU-A", "WH-US", 99, 9), "Older event must be rejected");
        equal(new Snapshot(12, 10), current(ledger, "SKU-A", "WH-US"), "Rejected event preserves state");
        check(ledger.apply("SKU-A", "WH-US", 7, 11), "A subsequent newer version must still be accepted");
        equal(new Snapshot(7, 11), current(ledger, "SKU-A", "WH-US"), "Latest snapshot after recovery");
    }
}
