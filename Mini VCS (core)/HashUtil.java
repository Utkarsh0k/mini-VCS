
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.messageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil  {

    public static byte[] getSHA(String s) throws NoSuchAlgorithmException {
        messageDigest md = messageDigest.getInstance("SHA-256");
        return md.digest(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String getHex(byte[] hash) {
        BigInteger bi = new BigInteger(1, hash);
        StringBuilder HexString = new StringBuilder(bi.toString(16));

        while (HexString.length() < 64) {
            HexString.insert(0, '0');
        }
        return HexString.toString();
    }
}
