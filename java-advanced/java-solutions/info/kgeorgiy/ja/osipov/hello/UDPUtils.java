package info.kgeorgiy.ja.osipov.hello;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UDPUtils {

    public static final int DATAGRAM_SOCKET_TIMEOUT = 100;
    public static final int SELECTOR_TIMEOUT = 100;
    public static final Pattern PATTERN = Pattern.compile("\\D*(\\d+)\\D+(\\d+)\\D*");

    /**
     * Check {@code port} and {@code threads} have valid values
     *
     * @param port    - port value to be checked
     * @param threads - threads value to be checked
     * @return true if {@code port} and {@code threads} are valid, else false
     */
    public static boolean checkPortAndThreads(final int port, final int threads) {
        if (port < 0 || port > 65535) {
            System.err.println("Invalid port number: " + port);
            return false;
        }
        if (threads < 1) {
            System.err.println("Expected at least one thread");
            return false;
        }
        return true;
    }

    /**
     * Check {@code port}, {@code threads} and {@code requests} have valid values
     *
     * @param port     - port value to be checked
     * @param threads  - threads value to be checked
     * @param requests - requests value to be checked
     * @return true if {@code port}, {@code threads} and {@code requests} are valid, else false
     */
    public static boolean checkPortThreadsRequests(final int port, final int threads, final int requests) {
        if (requests < 0) {
            System.err.println("Expected not less than zero requests");
            return false;
        }
        return checkPortAndThreads(port, threads);
    }

    /**
     * Extract {@link DatagramPacket} received message to {@link String}
     *
     * @param packet - received packet to extract message from
     * @return extracted message represented by {@link String}
     */
    public static String getReceivedMessage(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static String makeRequestMessage(final String prefix, final int threadId, final int requestId) {
        return prefix + threadId + '_' + requestId;
    }

    public static boolean checkReceivedMessage(final String message, final int threadId, final int requestId) {
        StringBuilder builder = new StringBuilder();
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < message.length(); ++i) {
            char ch = message.charAt(i);
            if (Character.isDigit(ch)) {
                number.append(ch);
            } else {
                if (!number.isEmpty()) {
                    try {
                        builder.append(Integer.parseInt(number.toString()));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    number.setLength(0);
                }
                builder.append(ch);
            }
        }
        if (!number.isEmpty()) {
            try {
                builder.append(Integer.parseInt(number.toString()));
            } catch (NumberFormatException e) {
                return false;
            }
            number.setLength(0);
        }
        final Matcher matcher = UDPUtils.PATTERN.matcher(builder.toString());
        return matcher.matches()
                && threadId == Integer.parseInt(matcher.group(1))
                && requestId == Integer.parseInt(matcher.group(2));
    }
}
