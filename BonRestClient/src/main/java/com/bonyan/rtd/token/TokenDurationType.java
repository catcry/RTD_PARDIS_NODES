package com.bonyan.rtd.token;

public enum TokenDurationType {

    MILLISECOND, SECOND, MINUTE, HOUR;

    public static TokenDurationType getTypeByName(String typeName) {
        switch (typeName.toLowerCase()) {
            case "millisecond":
                return TokenDurationType.MILLISECOND;
            case "second":
                return TokenDurationType.SECOND;
            case "minute":
                return TokenDurationType.MINUTE;
            case "hour":
                return TokenDurationType.HOUR;
            default:
                return null;
        }
    }
}
