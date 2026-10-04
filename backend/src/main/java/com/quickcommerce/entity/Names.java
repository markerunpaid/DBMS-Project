package com.quickcommerce.entity;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Names {

    private Names() {
    }

    static String full(String first, String middle, String last) {
        return Stream.of(first, middle, last)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));
    }
}
