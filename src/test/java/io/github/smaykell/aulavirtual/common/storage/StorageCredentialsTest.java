package io.github.smaykell.aulavirtual.common.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

class StorageCredentialsTest {

    @Test
    void explicit_keys_are_used_as_static_credentials() {
        assertThat(StorageConfig.credentialsOf(propertiesWith("access", "secret")))
                .isInstanceOf(StaticCredentialsProvider.class);
    }

    @Test
    void blank_keys_fall_back_to_the_default_chain() {
        assertThat(StorageConfig.credentialsOf(propertiesWith("", "")))
                .isInstanceOf(DefaultCredentialsProvider.class);
    }

    @Test
    void a_key_without_its_secret_falls_back_to_the_default_chain() {
        assertThat(StorageConfig.credentialsOf(propertiesWith("access", null)))
                .isInstanceOf(DefaultCredentialsProvider.class);
    }

    private static StorageProperties propertiesWith(String accessKey, String secretKey) {
        return new StorageProperties("", "us-east-1", "aula-virtual", accessKey, secretKey,
                false, Duration.ofMinutes(15), Duration.ofMinutes(10));
    }
}
