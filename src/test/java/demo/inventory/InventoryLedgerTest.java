package demo.inventory;

import static demo.inventory.TestSupport.check;
import static demo.inventory.TestSupport.current;
import static demo.inventory.TestSupport.equal;
import static demo.inventory.TestSupport.rejectsInvalid;

public final class InventoryLedgerTest {
    public static void main(String[] args) {
        TestSupport tests = new TestSupport();
        tests.run("first snapshot is accepted", InventoryLedgerTest::firstSnapshot);
        tests.run("newer version replaces current snapshot", InventoryLedgerTest::newerVersion);
        tests.run("older version is ignored without changing state", InventoryLedgerTest::olderVersion);
        tests.run("duplicate version returns false", InventoryLedgerTest::duplicateVersion);
        tests.run("different SKUs remain independent", InventoryLedgerTest::skuIsolation);
        tests.run("different warehouses remain independent", InventoryLedgerTest::warehouseIsolation);
        tests.run("negative quantity is rejected without changing state", InventoryLedgerTest::negativeQuantity);
        System.exit(tests.finish("Baseline suite"));
    }

    private static void firstSnapshot() {
        InventoryLedger ledger = new InventoryLedger();
        check(ledger.apply("SKU-A", "WH-US", 12, 7), "First snapshot must be accepted");
        equal(new Snapshot(12, 7), current(ledger, "SKU-A", "WH-US"), "Stored first snapshot");
    }

    private static void newerVersion() {
        InventoryLedger ledger = new InventoryLedger();
        ledger.apply("SKU-A", "WH-US", 12, 7);
        check(ledger.apply("SKU-A", "WH-US", 8, 8), "Newer version must be accepted");
        equal(new Snapshot(8, 8), current(ledger, "SKU-A", "WH-US"), "Stored newer snapshot");
    }

    private static void olderVersion() {
        InventoryLedger ledger = new InventoryLedger();
        ledger.apply("SKU-A", "WH-US", 8, 8);
        boolean accepted = ledger.apply("SKU-A", "WH-US", 99, 7);
        equal(new Snapshot(8, 8), current(ledger, "SKU-A", "WH-US"),
                "Older snapshot must not overwrite inventory");
        check(!accepted, "Older version must return false");
    }

    private static void duplicateVersion() {
        InventoryLedger ledger = new InventoryLedger();
        ledger.apply("SKU-A", "WH-US", 8, 8);
        check(!ledger.apply("SKU-A", "WH-US", 8, 8), "Duplicate version must return false");
        equal(new Snapshot(8, 8), current(ledger, "SKU-A", "WH-US"), "Duplicate leaves state unchanged");
    }

    private static void skuIsolation() {
        InventoryLedger ledger = new InventoryLedger();
        check(ledger.apply("SKU-A", "WH-US", 12, 10), "SKU-A first event");
        check(ledger.apply("SKU-B", "WH-US", 25, 1), "SKU-B has its own version history");
        equal(new Snapshot(12, 10), current(ledger, "SKU-A", "WH-US"), "SKU-A inventory");
        equal(new Snapshot(25, 1), current(ledger, "SKU-B", "WH-US"), "SKU-B inventory");
    }

    private static void warehouseIsolation() {
        InventoryLedger ledger = new InventoryLedger();
        check(ledger.apply("SKU-A", "WH-US", 12, 10), "WH-US first event");
        check(ledger.apply("SKU-A", "WH-UK", 25, 1), "WH-UK has its own version history");
        equal(new Snapshot(12, 10), current(ledger, "SKU-A", "WH-US"), "WH-US inventory");
        equal(new Snapshot(25, 1), current(ledger, "SKU-A", "WH-UK"), "WH-UK inventory");
    }

    private static void negativeQuantity() {
        InventoryLedger ledger = new InventoryLedger();
        rejectsInvalid(() -> ledger.apply("SKU-A", "WH-US", -1, 0));
        check(ledger.find("SKU-A", "WH-US").isEmpty(), "Invalid first event must not create state");

        ledger.apply("SKU-A", "WH-US", 12, 10);
        rejectsInvalid(() -> ledger.apply("SKU-A", "WH-US", -1, 11));
        equal(new Snapshot(12, 10), current(ledger, "SKU-A", "WH-US"),
                "Invalid later event must preserve current state");
    }
}
