package com.wizzardo.epoll.readable;

import java.nio.ByteBuffer;

/**
 * @author: wizzardo
 * Date: 7/25/14
 */
public class ReadableBuilder extends ReadableData {
    private ReadableByteArray[] parts = new ReadableByteArray[20];
    private int partsCount = 0;
    private int position = 0;

    public ReadableBuilder() {
    }

    public ReadableBuilder(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public ReadableBuilder(byte[] bytes, int offset, int length) {
        partsCount = 1;
        parts[0] = new ReadableByteArray(bytes, offset, length);
    }

    public ReadableBuilder append(byte[] bytes) {
        return append(bytes, 0, bytes.length);
    }

    public ReadableBuilder append(byte[] bytes, int offset, int length) {
        if (partsCount + 1 == parts.length)
            increaseSize();

        parts[partsCount] = new ReadableByteArray(bytes, offset, length);
        partsCount++;

        return this;
    }

    private void increaseSize() {
        ReadableByteArray[] temp = new ReadableByteArray[partsCount * 3 / 2];
        System.arraycopy(parts, 0, temp, 0, partsCount);
        parts = temp;
    }

    @Override
    public int read(ByteBuffer byteBuffer) {
        if (position >= partsCount)
            return 0;

        int r = parts[position].read(byteBuffer);
        while (position < partsCount - 1 && parts[position].isComplete() && byteBuffer.hasRemaining()) {
            r += parts[++position].read(byteBuffer);
        }

        return r;
    }

    @Override
    public void unread(int i) {
        while (i > 0) {
            ReadableByteArray part = parts[position];
            int unread = (int) Math.min(i, part.complete());
            parts[position].unread(unread);
            i -= unread;
            if (i > 0)
                position--;
        }
    }

    @Override
    public boolean isComplete() {
        return position == partsCount - 1 && parts[position].isComplete();
    }

    @Override
    public long complete() {
        long l = 0;
        for (int i = 0; i <= position; i++) {
            ReadableByteArray part = parts[i];
            if (parts[i].isComplete())
                l += parts[i].length();
            else
                l += part.complete();
        }
        return l;
    }

    @Override
    public long length() {
        long l = 0;
        for (int i = 0; i <= partsCount; i++) {
            l += parts[i].length();
        }
        return l;
    }

    @Override
    public long remains() {
        long l = 0;
        for (int i = partsCount - 1; i >= 0; i--) {
            if (parts[i].isComplete())
                break;
            l += parts[i].remains();
        }
        return l;
    }

    public ReadableByteArray toReadableByteArray() {
        byte[] data = new byte[(int) length()];
        int offset = 0;
        for (int i = 0; i < partsCount; i++) {
            ReadableByteArray part = parts[i];
            System.arraycopy(part.bytes, part.offset, data, offset, part.length);
            offset += part.length;
        }
        return new ReadableByteArray(data);
    }
}
