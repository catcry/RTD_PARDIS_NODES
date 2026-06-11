package com.bonyan.rtd.token;

import java.util.Date;

public interface Token {

    public String getTokenValue();

    public String setTokenValue(String value);

    public int getTokenDurationTypeCode();

    public void setTokenDurationType(TokenDurationType tokenDurationType);

    public int getRenewalMarginPercentage();

    public void setRenewalMarginPercentage(int renewalMarginPercentage);

    public Date getExpirationTime();

    public void setExpirationTime(Date expirationTime);

    public void setExpirationDuration(int expirationDuration);

    public boolean isTokenValid();

}
