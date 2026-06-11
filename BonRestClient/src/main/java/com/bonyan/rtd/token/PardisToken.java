package com.bonyan.rtd.token;

import java.util.Calendar;
import java.util.Date;

public class PardisToken implements Token {

    private String tokenValue;
    private int tokenDurationTypeCode;
    private int renewalMarginPercentage;
    private Date expirationTime;

    public PardisToken() {
    }

    public PardisToken(String tokenValue) {
        this.tokenValue = tokenValue;
        this.tokenDurationTypeCode = Calendar.SECOND;
        this.renewalMarginPercentage = 0;
        this.expirationTime = calculateExpirationTime(300);
    }

    public PardisToken(TokenAttributes tokenAttributes) {
        this.tokenValue = tokenAttributes.getTokenValue();
        this.renewalMarginPercentage = tokenAttributes.getRenewalMarginPercentage();
        this.expirationTime = calculateExpirationTime(tokenAttributes.getExpirationDuration());
        this.setTokenDurationType(tokenAttributes.getTokenDurationType());
    }

    @Override
    public String getTokenValue() {
        return this.tokenValue;
    }

    @Override
    public String setTokenValue(String value) {
        return "";
    }

    @Override
    public boolean isTokenValid() {
        return false;
    }

    private Date calculateExpirationTime(int expirationDurationInMinutes) {
        int penaltyDuration = expirationDurationInMinutes - (expirationDurationInMinutes * renewalMarginPercentage / 100);
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(this.tokenDurationTypeCode, penaltyDuration);
        return calendar.getTime();
    }

    public boolean isValid() {
        Date now = new Date();
        return now.before(this.expirationTime);
    }

    public int getTokenDurationTypeCode() {
        return tokenDurationTypeCode;
    }

    public void setTokenDurationType(TokenDurationType tokenDurationType) {
        switch (tokenDurationType) {
            case MILLISECOND:
                this.tokenDurationTypeCode = Calendar.MILLISECOND;
                break;
            case MINUTE:
                this.tokenDurationTypeCode = Calendar.MINUTE;
                break;
            case HOUR:
                this.tokenDurationTypeCode = Calendar.HOUR;
                break;
            default:
                this.tokenDurationTypeCode = Calendar.SECOND;
                break;
        }
    }

    public int getRenewalMarginPercentage() {
        return renewalMarginPercentage;
    }

    public void setRenewalMarginPercentage(int renewalMarginPercentage) {
        this.renewalMarginPercentage = renewalMarginPercentage;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    @Override
    public void setExpirationDuration(int expirationDuration) {
        this.calculateExpirationTime(expirationDuration);
    }
}
