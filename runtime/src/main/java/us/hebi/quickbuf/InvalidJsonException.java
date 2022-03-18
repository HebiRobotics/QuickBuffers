package us.hebi.quickbuf;

/**
 * @author Florian Enner
 * @since 18 Mär 2022
 */
public class InvalidJsonException extends InvalidProtocolBufferException {

    private static final long serialVersionUID = 1L;

    public InvalidJsonException(String description) {
        super(description);
    }

    static InvalidJsonException truncatedMessage() {
        return new InvalidJsonException(
                "While parsing a protocol message, the input ended unexpectedly " +
                        "in the middle of a field. This could mean either than the " +
                        "input has been truncated or that the input is invalid.");
    }

    static InvalidJsonException illegalNumberFormat() {
        return new InvalidJsonException("Illegal number format.");
    }

}
