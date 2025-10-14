package io.github.anjoismysign.bloblibtool;

/**
 * Thrown when a single field definition (type + name) is malformed.
 */
public class InvalidFieldFormatException extends RuntimeException {
    public InvalidFieldFormatException(String message) {
        super(message);
    }
}
