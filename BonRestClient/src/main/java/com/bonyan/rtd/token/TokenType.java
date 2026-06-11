package com.bonyan.rtd.token;

public enum TokenType {


    PARDIS_TOKEN("com.bonyan.rtd.token.PardisToken"),
    NORMAL_TOKEN("com.bonyan.rtd.token.NormalToken");

    private String className;

    private TokenType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public static TokenType getTokenType(String tokenType) {
        tokenType = tokenType + "_TOKEN";
        for (TokenType type: values()) {
            if (type.name().equals(tokenType)){
                return type;
            }
        }
        return NORMAL_TOKEN;
    }
}
