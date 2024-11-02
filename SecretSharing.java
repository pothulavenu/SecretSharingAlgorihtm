import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class SecretSharing {

    static class Coordinate {
        int x;
        BigInteger y;

        Coordinate(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) {
        // Input JSON files containing encoded points for secret reconstruction
        String[] jsonFilePaths = { "input1.json", "input2.json" };

        // Process each file to reconstruct the secret
        for (String path : jsonFilePaths) {
            parseAndComputeSecret(path);
        }
    }

    private static void parseAndComputeSecret(String jsonFilePath) {
        try (FileReader reader = new FileReader(jsonFilePath)) {
            JSONObject data = new JSONObject(new JSONTokener(reader));

            // Get 'n' (total points) and 'k' (threshold)
            int totalPoints = data.getJSONObject("keys").getInt("n");
            int threshold = data.getJSONObject("keys").getInt("k");

            // Extract the points
            List<Coordinate> coordinates = new ArrayList<>();
            for (int i = 1; i <= totalPoints; i++) {
                if (data.has(String.valueOf(i))) {
                    JSONObject pointData = data.getJSONObject(String.valueOf(i));
                    int base = pointData.getInt("base");
                    String encodedY = pointData.getString("value");
                    int xCoord = i;
                    BigInteger yCoord = new BigInteger(encodedY, base);
                    coordinates.add(new Coordinate(xCoord, yCoord));
                }
            }

            // Perform Lagrange interpolation to compute the secret
            BigInteger secret = interpolateAtZero(coordinates, threshold);
            System.out.println("Reconstructed secret from " + jsonFilePath + ": " + secret);

        } catch (IOException e) {
            System.err.println("File error: " + jsonFilePath);
            e.printStackTrace();
        }
    }

    // Computes the secret using Lagrange interpolation at x = 0
    private static BigInteger interpolateAtZero(List<Coordinate> coordinates, int threshold) {
        BigInteger result = BigInteger.ZERO;

        for (int i = 0; i < threshold; i++) {
            Coordinate current = coordinates.get(i);
            BigInteger term = current.y;

            for (int j = 0; j < threshold; j++) {
                if (i != j) {
                    Coordinate other = coordinates.get(j);
                    BigInteger numerator = BigInteger.valueOf(-other.x);
                    BigInteger denominator = BigInteger.valueOf(current.x - other.x);

                    // Perform modular inversion for the denominator
                    BigInteger invertedDenominator = denominator.modInverse(BigInteger.TEN.pow(10));
                    term = term.multiply(numerator).multiply(invertedDenominator);
                }
            }
            result = result.add(term);
        }

        return result;
    }
}
