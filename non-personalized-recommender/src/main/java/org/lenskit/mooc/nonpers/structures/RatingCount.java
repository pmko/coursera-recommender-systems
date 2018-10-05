package org.lenskit.mooc.nonpers.structures;

public class RatingCount {
    private long itemId;
    private double sum;
    private long count;

    public RatingCount()
    {
        this.sum = 0;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long value) {
        this.itemId = value;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double value) {
        this.sum = value;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long value) {
        this.count = value;
    }

    public void incrementCount() {
        this.count++;
    }

    public void runningTotal(double value) {
        this.sum = this.sum + value;
    }

    public boolean itemEquals(long value) {
        if (value == this.itemId) {
            return true;
        } else {
            return false;
        }
    }
}
