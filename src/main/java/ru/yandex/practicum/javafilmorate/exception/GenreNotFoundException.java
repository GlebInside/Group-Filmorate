package ru.yandex.practicum.javafilmorate.exception;

public class GenreNotFoundException extends RuntimeException {
    public GenreNotFoundException(String message) {
        super(message);
    }

}
