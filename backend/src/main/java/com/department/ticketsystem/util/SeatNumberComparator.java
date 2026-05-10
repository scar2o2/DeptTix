package com.department.ticketsystem.util;

import com.department.ticketsystem.model.Seat;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeatNumberComparator {

    private static final Pattern SEAT_PATTERN = Pattern.compile("^([A-Za-z]+)(\\d+)$");

    public static final Comparator<Seat> BY_SEAT_NUMBER =
            Comparator.comparing(Seat::getSeatNumber, SeatNumberComparator::compare);

    private SeatNumberComparator() {
    }

    public static int compare(String left, String right) {
        Matcher leftMatcher = SEAT_PATTERN.matcher(left == null ? "" : left.trim());
        Matcher rightMatcher = SEAT_PATTERN.matcher(right == null ? "" : right.trim());
        if (leftMatcher.matches() && rightMatcher.matches()) {
            int rowCompare = leftMatcher.group(1).compareToIgnoreCase(rightMatcher.group(1));
            if (rowCompare != 0) {
                return rowCompare;
            }
            return Integer.compare(Integer.parseInt(leftMatcher.group(2)), Integer.parseInt(rightMatcher.group(2)));
        }
        return String.CASE_INSENSITIVE_ORDER.compare(left == null ? "" : left, right == null ? "" : right);
    }
}
