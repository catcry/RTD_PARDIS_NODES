package com.bonyan.rtd.token;

public class TokenAttributes {

    private String tokenValue;
    private TokenDurationType tokenDurationType;
    private int renewalMarginPercentage;
    private int expirationDuration;

    public TokenAttributes(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public TokenDurationType getTokenDurationType() {
        return tokenDurationType;
    }

    public void setTokenDurationType(TokenDurationType tokenDurationType) {
        this.tokenDurationType = tokenDurationType;
    }

    public int getRenewalMarginPercentage() {
        return renewalMarginPercentage;
    }

    public void setRenewalMarginPercentage(int renewalMarginPercentage) {
        this.renewalMarginPercentage = renewalMarginPercentage;
    }

    public int getExpirationDuration() {
        return expirationDuration;
    }

    public void setExpirationDuration(int expirationDuration) {
        this.expirationDuration = expirationDuration;
    }
}
