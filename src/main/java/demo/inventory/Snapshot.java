package demo.inventory;

/**
 * An immutable inventory snapshot for one SKU in one warehouse.
 *
 * <p>A snapshot describes the complete stock level observed at one point in
 * time, not a stock delta, and is identified by a version assigned by the
 * producing system. Because the record is immutable, an accepted snapshot is
 * never modified later; a newer version produces a new instance.</p>
 *
 * <p>The compact constructor validates the values, so every constructed
 * instance has a non-negative quantity and version.</p>
 *
 * @param quantity the stock quantity observed in the warehouse; must not be negative
 * @param version the version of the observation, used to order snapshots;
 *        must not be negative
 */
public record Snapshot(int quantity, long version) {
    /**
     * Validates the snapshot's values.
     *
     * @throws IllegalArgumentException if {@code quantity} or {@code version}
     *         is negative
     */
    public Snapshot {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        }
    }
}
