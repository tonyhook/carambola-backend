package cc.tonyhook.carambola.backend.service.shared;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Map;

public class HashHelperService {

    private static Map<String, Integer> hashLength = Map.of(
        "MD5", 32,
        "SHA1", 40,
        "SHA256", 64
    );

    public static String hash(byte[] content, String algorithm) {
        if (!hashLength.containsKey(algorithm)) {
            return null;
        }

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm.toUpperCase());
            byte[] messageDigest = md.digest(content);
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            while (hashtext.length() < hashLength.get(algorithm.toUpperCase())) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
