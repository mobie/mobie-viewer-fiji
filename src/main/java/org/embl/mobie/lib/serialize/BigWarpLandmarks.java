package org.embl.mobie.lib.serialize;

import com.google.gson.Gson;
import java.util.List;
import java.util.Objects;

public class BigWarpLandmarks {
    private String type;
    private int numDimensions;
    private List<List<Double>> movingPoints;
    private List<List<Double>> fixedPoints;
    private List<Boolean> active;
    private List<String> names;

    public BigWarpLandmarks() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNumDimensions() {
        return numDimensions;
    }

    public void setNumDimensions(int numDimensions) {
        this.numDimensions = numDimensions;
    }

    public List<List<Double>> getMovingPoints() {
        return movingPoints;
    }

    public void setMovingPoints(List<List<Double>> movingPoints) {
        this.movingPoints = movingPoints;
    }

    public List<List<Double>> getFixedPoints() {
        return fixedPoints;
    }

    public void setFixedPoints(List<List<Double>> fixedPoints) {
        this.fixedPoints = fixedPoints;
    }

    public List<Boolean> getActive() {
        return active;
    }

    public void setActive(List<Boolean> active) {
        this.active = active;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public static BigWarpLandmarks fromJson(String json) {
        return new Gson().fromJson(json, BigWarpLandmarks.class);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return "BigWarpLandmarks{" +
                "type='" + type + '\'' +
                ", numDimensions=" + numDimensions +
                ", movingPoints=" + movingPoints +
                ", fixedPoints=" + fixedPoints +
                ", active=" + active +
                ", names=" + names +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BigWarpLandmarks that = (BigWarpLandmarks) o;
        return numDimensions == that.numDimensions &&
                Objects.equals(type, that.type) &&
                Objects.equals(movingPoints, that.movingPoints) &&
                Objects.equals(fixedPoints, that.fixedPoints) &&
                Objects.equals(active, that.active) &&
                Objects.equals(names, that.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, numDimensions, movingPoints, fixedPoints, active, names);
    }
}
