package com.bonyan.rtd.token;

import java.util.MissingFormatArgumentException;
import java.util.Objects;

public class TokenFactory {

    private TokenFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static Token buildToken(TokenType type, TokenAttributes tokenAttributes) throws MissingFormatArgumentException {
        if (Objects.requireNonNull(type) == TokenType.PARDIS_TOKEN) {
            return new PardisToken(tokenAttributes);
        }
        throw new MissingFormatArgumentException("missing or invalid token type");
    }

}
