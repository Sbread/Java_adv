package info.kgeorgiy.ja.osipov.implementor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;


/**
 * Method wrapper for compare methods by signature.
 * <p>
 * Wrapper is used in {@link Implementor} class
 * @author Daniil Osipov
 */
public record CustomMethod(Method method) {


    /**
     * Hashcode method
     * @return hashcode by method signature
     */
    @Override
    public int hashCode() {
        return Objects.hash(method.getName(), Arrays.hashCode(method.getParameterTypes()));
    }


    /**
     * Equals method
     * @param obj another {@code Object}
     * @return true if methods' signatures are equals or links matches
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CustomMethod customMethod = (CustomMethod) obj;
        return method.getName().equals(customMethod.method().getName())
               && Arrays.equals(method.getParameterTypes(), customMethod.method().getParameterTypes());
    }
}
