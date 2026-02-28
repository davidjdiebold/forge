package forge.util;

public class Hasher {
    public static int hashCode(Object b, Object c) {
        int result = 1;
        result = 31 * result + (b == null ? 0 : b.hashCode());
        result = 31 * result + (c == null ? 0 : c.hashCode());
        return result;
    }

    public static int hashCode(Object b, Object c, Object d) {
        int result = 1;
        result = 31 * result + (b == null ? 0 : b.hashCode());
        result = 31 * result + (c == null ? 0 : c.hashCode());
        result = 31 * result + (d == null ? 0 : d.hashCode());
        return result;
    }



}
