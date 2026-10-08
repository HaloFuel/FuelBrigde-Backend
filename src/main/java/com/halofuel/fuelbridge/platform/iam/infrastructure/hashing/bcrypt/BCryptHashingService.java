package com.halofuel.fuelbridge.platform.iam.infrastructure.hashing.bcrypt;

import com.halofuel.fuelbridge.platform.iam.application.internal.outboundservices.hashing.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface BCryptHashingService extends HashingService, PasswordEncoder {
}
