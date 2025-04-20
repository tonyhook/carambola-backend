package cc.tonyhook.carambola.backend.service.ad;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class KeyService {

    private KeyGenerator keyGenerator = null;

    @PostConstruct
    private void initKeyGenerator() {
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String generateKey() {
        if (keyGenerator == null) {
            return null;
        }

        String encodedKey = null;
        synchronized(keyGenerator) {
            SecretKey originalKey = keyGenerator.generateKey();
            byte[] rawData = originalKey.getEncoded();
            encodedKey = Base64.getEncoder().encodeToString(rawData);
            encodedKey = encodedKey.replace("/", "_").replace("+", "-");
        }

        return encodedKey;
    }

}
