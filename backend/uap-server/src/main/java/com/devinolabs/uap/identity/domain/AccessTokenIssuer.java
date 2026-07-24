package com.devinolabs.uap.identity.domain;

public interface AccessTokenIssuer {

	IssuedAccessToken issue(AccountId accountId);

	AccessTokenClaims verify(String accessToken);

}
