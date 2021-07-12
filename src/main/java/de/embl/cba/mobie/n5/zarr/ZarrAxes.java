package de.embl.cba.mobie.n5.zarr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.stream.Stream;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public enum ZarrAxes {
    YX("[\"y\",\"x\"]"),
    CYX("[\"c\",\"y\",\"x\"]"),
    TYX("[\"t\",\"y\",\"x\"]"),
    ZYX("[\"z\",\"y\",\"x\"]"),
    CZYX("[\"c\",\"z\",\"y\",\"x\"]"),
    TZYX("[\"t\",\"z\",\"y\",\"x\"]"),
    TCZYX("[\"t\",\"c\",\"z\",\"y\",\"x\"]");

    private final String axes;

    @JsonCreator
    public static ZarrAxes decode(final String axes) {
        return Stream.of(ZarrAxes.values()).filter(targetEnum -> targetEnum.axes.equals(axes)).findFirst().orElse(null);
    }

    ZarrAxes(String axes) {
        this.axes = axes;
    }
}
