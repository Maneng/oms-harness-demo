package demo.inventory;

/** A warehouse's immutable inventory snapshot for one SKU. */
public record Snapshot(int quantity, long version) {
    public Snapshot {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        }
    }
}
