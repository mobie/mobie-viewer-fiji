package de.embl.cba.mobie.n5.zarr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public List<String> getAxesList() {
        String pattern = "([a-z])";
        List<String> allMatches = new ArrayList<>();
        Matcher m = Pattern.compile(pattern)
                .matcher(axes);
        while (m.find()) {
            allMatches.add(m.group());
        }

        return allMatches;
    }
}
