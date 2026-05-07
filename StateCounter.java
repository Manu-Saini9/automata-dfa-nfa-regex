public class StateCounter {
    private static int next = 0;
    public static int newState() {
        return next++;
    }
    public static void reset() {
        next = 0;
    }
}
