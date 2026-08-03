package com.saeanyang.management.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 문자열 PII(이름·전화번호 등)를 저장 시 암호화, 조회 시 복호화한다. */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return PiiCipherHolder.require().encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return PiiCipherHolder.require().decrypt(dbData);
  }
}
