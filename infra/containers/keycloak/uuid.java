import java.util.UUID;

public class uuid {
    public static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString().toUpperCase();
        System.out.println(randomUUIDString);
    }
}