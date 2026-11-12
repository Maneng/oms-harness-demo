package demo.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory ledger of the latest complete inventory snapshot per SKU and
 * warehouse.
 *
 * <p>The ledger is single-threaded and not thread-safe. It keeps all state in a
 * {@link HashMap} keyed by the SKU/warehouse pair; it has no database, message
 * queue or other external dependency.</p>
 *
 * <p>Each event is a full snapshot of the stock level observed at a point in
 * time, identified by a version number. The version guard in
 * {@link #apply(String, String, int, long)} makes sure that a delayed or
 * duplicated event can never replace a newer snapshot.</p>
 */
public final class InventoryLedger {
    private final Map<Key, Snapshot> snapshots = new HashMap<>();

    /** Creates an empty ledger that holds no snapshots. */
    public InventoryLedger() {
    }

    /**
     * Applies a complete inventory snapshot for one SKU in one warehouse.
     *
     * <p>A snapshot carries the full stock level, not a stock delta: an accepted
     * snapshot replaces the previously stored quantity for the pair.</p>
     *
     * <p>The first valid snapshot for a SKU/warehouse pair is accepted,
     * including version {@code 0}. Once a snapshot exists, a later snapshot is
     * accepted only when its version is strictly greater than the stored
     * version. An older version or the same version (even with a different
     * quantity) returns {@code false} and leaves the stored snapshot
     * unchanged.</p>
     *
     * <p>All arguments are validated before the stored state is read or
     * modified, so a rejected call never changes the ledger.</p>
     *
     * @param sku the stock keeping unit; must not be {@code null} or blank
     * @param warehouse the warehouse identifier; must not be {@code null} or blank
     * @param quantity the complete stock quantity to store; must not be negative
     * @param version the snapshot version; must not be negative
     * @return {@code true} if the snapshot was accepted and stored,
     *         {@code false} if an equal or newer version is already stored
     * @throws IllegalArgumentException if {@code sku} or {@code warehouse} is
     *         {@code null} or blank, or if {@code quantity} or {@code version}
     *         is negative
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

    /**
     * Returns the current snapshot for the given SKU and warehouse.
     *
     * @param sku the stock keeping unit; must not be {@code null} or blank
     * @param warehouse the warehouse identifier; must not be {@code null} or blank
     * @return the stored snapshot, or {@link Optional#empty()} when no accepted
     *         snapshot exists for the pair
     * @throws IllegalArgumentException if {@code sku} or {@code warehouse} is
     *         {@code null} or blank
     */
    public Optional<Snapshot> find(String sku, String warehouse) {
        return Optional.ofNullable(snapshots.get(keyOf(sku, warehouse)));
    }

    /**
     * Builds the composite map key for a SKU and warehouse after validating
     * the business key.
     *
     * @param sku the stock keeping unit; must not be {@code null} or blank
     * @param warehouse the warehouse identifier; must not be {@code null} or blank
     * @return the composite key for the pair
     * @throws IllegalArgumentException if {@code sku} or {@code warehouse} is
     *         {@code null} or blank
     */
    private static Key keyOf(String sku, String warehouse) {
        requireNonBlank(sku, "sku");
        requireNonBlank(warehouse, "warehouse");
        return new Key(sku, warehouse);
    }

    /**
     * Rejects identifiers that are {@code null} or contain only whitespace.
     *
     * @param value the identifier value to check
     * @param name the argument name used in the exception message
     * @throws IllegalArgumentException if {@code value} is {@code null} or blank
     */
    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /** Composite key identifying one SKU in one warehouse. */
    private record Key(String sku, String warehouse) {
    }
}
