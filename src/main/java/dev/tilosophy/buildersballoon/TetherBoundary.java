package dev.tilosophy.buildersballoon;

public record TetherBoundary(double x, double y, double z, int range) {
    public TetherBoundary {
        if (!(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && range >= 8 && range <= 64)) {
            throw new IllegalArgumentException("Invalid tether boundary");
        }
    }

    public double clampX(double value) {
        return this.clamp(value, this.x);
    }

    public double clampY(double value) {
        return this.clamp(value, this.y);
    }

    public double clampZ(double value) {
        return this.clamp(value, this.z);
    }

    private double clamp(double value, double centre) {
        if (!Double.isFinite(value)) {
            return value;
        }
        return Math.max(centre - (double)this.range, Math.min(centre + (double)this.range, value));
    }

    public boolean contains(double px, double py, double pz) {
        return Double.isFinite(px) && Double.isFinite(py) && Double.isFinite(pz) && px >= this.x - (double)this.range && px <= this.x + (double)this.range && py >= this.y - (double)this.range && py <= this.y + (double)this.range && pz >= this.z - (double)this.range && pz <= this.z + (double)this.range;
    }

    public double movementX(double from, double delta) {
        return this.movement(from, delta, this.x);
    }

    public double movementY(double from, double delta) {
        return this.movement(from, delta, this.y);
    }

    public double movementZ(double from, double delta) {
        return this.movement(from, delta, this.z);
    }

    private double movement(double from, double delta, double centre) {
        double lo = centre - (double)this.range;
        double hi = centre + (double)this.range;
        if (from < lo) {
            return Math.max(0.0, Math.min(hi - from, delta));
        }
        if (from > hi) {
            return Math.min(0.0, Math.max(lo - from, delta));
        }
        return Math.max(lo - from, Math.min(hi - from, delta));
    }
}
