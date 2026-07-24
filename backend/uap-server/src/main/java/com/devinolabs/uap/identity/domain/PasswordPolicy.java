package com.devinolabs.uap.identity.domain;

import java.util.List;

public interface PasswordPolicy {

	PasswordPolicyResult validate(CharSequence rawPassword);

}
