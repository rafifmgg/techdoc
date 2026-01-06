package com.ocmsintranet.cronservice.crud.beans;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SymmetricKeyConstants {

    @Value("${SYMMETRIC.KEY.OPEN}")
    public String openSymmetricKey;

    @Value("${SYMMETRIC.KEY.CLOSE}")
    public String closeSymmetricKey;
}
