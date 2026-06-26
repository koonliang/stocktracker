package com.stocktracker.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class SecretsManagerPasswordConfigSourceTest {
  @Test
  void staysInactiveWithoutSecretArn() {
    var source = new SecretsManagerPasswordConfigSource();

    assertTrue(source.getPropertyNames().isEmpty());
    assertTrue(source.getProperties().isEmpty());
    assertEquals(null, source.getValue("quarkus.datasource.password"));
  }

  @Test
  void extractsPasswordFieldFromSecretJson() throws Exception {
    var source = new SecretsManagerPasswordConfigSource();
    Method method =
        SecretsManagerPasswordConfigSource.class.getDeclaredMethod("extractPassword", String.class);
    method.setAccessible(true);

    assertEquals("db-pass", method.invoke(source, "{\"password\":\"db-pass\"}"));
  }

  @Test
  void rejectsSecretJsonWithoutPassword() throws Exception {
    var source = new SecretsManagerPasswordConfigSource();
    Method method =
        SecretsManagerPasswordConfigSource.class.getDeclaredMethod("extractPassword", String.class);
    method.setAccessible(true);

    assertThrows(Exception.class, () -> method.invoke(source, "{\"username\":\"admin\"}"));
  }
}
