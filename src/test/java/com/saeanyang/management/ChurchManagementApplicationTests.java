package com.saeanyang.management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// PiiCryptoConfig가 fail-fast하므로 컨텍스트 로딩용 테스트 키를 주입한다(base64 32바이트).
@SpringBootTest(properties = "app.security.pii-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class ChurchManagementApplicationTests {

  @Test
  void contextLoads() {}
}
