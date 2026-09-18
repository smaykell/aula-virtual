package io.github.smaykell.aulavirtual.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String name();

    String prefix();

    HttpStatus status();

    String message();

    default String code() {
        return prefix() + "_" + name();
    }

    default String format(Object... args) {
        return args.length == 0 ? message() : message().formatted(args);
    }
}
