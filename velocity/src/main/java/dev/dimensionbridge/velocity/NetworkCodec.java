package dev.dimensionbridge.velocity;

import java.nio.charset.StandardCharsets;

final class NetworkCodec {
    private static final int MAX_DESTINATION_BYTES = 64;

    private NetworkCodec() {
    }

    static String decodeDestination(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Leere Bridge-Nachricht.");
        }

        Cursor cursor = new Cursor();
        int byteLength = readVarInt(data, cursor);
        if (byteLength < 1 || byteLength > MAX_DESTINATION_BYTES) {
            throw new IllegalArgumentException("Ungültige Zielnamenlänge: " + byteLength);
        }
        if (cursor.position + byteLength != data.length) {
            throw new IllegalArgumentException("Ungültige oder zusätzliche Bridge-Daten.");
        }

        return new String(data, cursor.position, byteLength, StandardCharsets.UTF_8);
    }

    private static int readVarInt(byte[] data, Cursor cursor) {
        int value = 0;
        int position = 0;
        byte current;

        do {
            if (cursor.position >= data.length || position >= 5) {
                throw new IllegalArgumentException("Ungültiger VarInt in Bridge-Nachricht.");
            }

            current = data[cursor.position++];
            value |= (current & 0x7F) << (position * 7);
            position++;
        } while ((current & 0x80) != 0);

        return value;
    }

    private static final class Cursor {
        private int position;
    }
}
