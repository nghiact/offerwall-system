package com.ctn.offerwall.user.auth;

import com.ctn.offerwall.user.config.AuthProperties;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtKeyProvider {

    private static final int LOCAL_DEV_KEY_SIZE = 2048;

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtKeyProvider(AuthProperties authProperties) {
        AuthProperties.Jwt jwt = authProperties.getJwt();
        if (hasText(jwt.getPrivateKeyPem()) && hasText(jwt.getPublicKeyPem())) {
            this.privateKey = parsePrivateKey(jwt.getPrivateKeyPem());
            this.publicKey = parsePublicKey(jwt.getPublicKeyPem());
        } else if (!hasText(jwt.getPrivateKeyPem()) && !hasText(jwt.getPublicKeyPem())) {
            KeyPair keyPair = generateLocalKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
        } else {
            throw new IllegalStateException("JWT private and public PEM keys must be configured together.");
        }
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    private RSAPrivateKey parsePrivateKey(String privateKeyPem) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] keyBytes = decodePem(privateKeyPem, "PRIVATE KEY");
            return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("JWT private key must be an RSA PKCS#8 PEM key.", exception);
        }
    }

    private RSAPublicKey parsePublicKey(String publicKeyPem) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] keyBytes = decodePem(publicKeyPem, "PUBLIC KEY");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("JWT public key must be an RSA X.509 PEM key.", exception);
        }
    }

    private KeyPair generateLocalKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(LOCAL_DEV_KEY_SIZE);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA key generation is not available.", exception);
        }
    }

    private byte[] decodePem(String pem, String type) {
        String normalized = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
