package Backend;

public class Session {
    public static int userId;
    public static String username;
    public static String fullName;
    public static String role;
    public static boolean mustChangePassword;

    public static void clear() {
        userId = 0;
        username = null;
        fullName = null;
        role = null;
        mustChangePassword = false;
    }
}
