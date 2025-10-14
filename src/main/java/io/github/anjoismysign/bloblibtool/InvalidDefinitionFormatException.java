package io.github.anjoismysign.bloblibtool;

/**
 * Thrown when the overall definition (split by ':') is invalid.
 */
public class InvalidDefinitionFormatException extends RuntimeException {
    public InvalidDefinitionFormatException(String message) {
        super(message);
    }
}
