package demo.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory, single-threaded inventory synchronization exercise.
 * This starting implementation intentionally contains the two version-handling
 * defects described by the assignment. It is not production software.
 */
public final class InventoryLedger {
    private final Map<Key, Snapshot> snapshots = new HashMap<>();

    /**
     * Applies a complete snapshot, not a stock delta.
     *
     * Contract: the first valid snapshot is accepted, including version zero.
     * A later snapshot is accepted only if its version is strictly greater.
     * Returns true only when the snapshot is accepted.
     * Invalid arguments throw IllegalArgumentException before state changes.
     */
    public boolean apply(String sku, String warehouse, int quantity, long version) {
        Key key = keyOf(sku, warehouse);
        Snapshot incoming = new Snapshot(quantity, version);

        Snapshot current = snapshots.get(key);
        if (current != null && version <= current.version()) {
            return false;
        }

        snapshots.put(key, incoming);
        return true;
    }

    public Optional<Snapshot> find(String sku, String warehouse) {
        return Optional.ofNullable(snapshots.get(keyOf(sku, warehouse)));
    }

    private static Key keyOf(String sku, String warehouse) {
        requireNonBlank(sku, "sku");
        requireNonBlank(warehouse, "warehouse");
        return new Key(sku, warehouse);
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private record Key(String sku, String warehouse) {
    }
}
