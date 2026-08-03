package com.saeanyang.management.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

/** 생년월일(LocalDate)을 ISO-8601 문자열로 직렬화한 뒤 암호화해 저장한다. */
@Converter
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

  @Override
  public String convertToDatabaseColumn(LocalDate attribute) {
    if (attribute == null) {
      return null;
    }
    return PiiCipherHolder.require().encrypt(attribute.toString());
  }

  @Override
  public LocalDate convertToEntityAttribute(String dbData) {
    String decrypted = PiiCipherHolder.require().decrypt(dbData);
    return decrypted == null ? null : LocalDate.parse(decrypted);
  }
}
