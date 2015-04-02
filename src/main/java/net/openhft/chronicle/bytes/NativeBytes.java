/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NativeBytesStore.nativeStoreWithFixedCapacity;
import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

/**
 * Created by peter.lawrey on 24/02/15.
 */
public class NativeBytes<Underlying> extends ZeroedBytes<Underlying> {

    NativeBytes(BytesStore store) {
        super(store, UnderflowMode.PADDED);
    }

    public static NativeBytes nativeBytes() {
        return new NativeBytes(noBytesStore());
    }

    public static NativeBytes nativeBytes(long initialCapacity) {
        return new NativeBytes(nativeStoreWithFixedCapacity(initialCapacity));
    }

    @Override
    protected long writeCheckOffset(long offset, long adding) {
        if (!bytesStore.inStore(offset))
            checkResize(offset);
        return offset;
    }

    private void checkResize(long offset) {
        if (isElastic())
            resize(offset);
        else
            throw new BufferOverflowException();
    }

    private void resize(long offset) {
        if (offset < 0)
            throw new IllegalArgumentException();
        // grow by 50% rounded up to the next pages size
        long ps = OS.pageSize();
        long size = (Math.max(offset, bytesStore.capacity() * 3 / 2) + ps) & ~(ps - 1);
        NativeBytesStore store;
        if (bytesStore.underlyingObject() instanceof ByteBuffer) {
            store = NativeBytesStore.elasticByteBuffer(Maths.toInt32(size));
        } else {
            store = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(size);
        }
        bytesStore.copyTo(store);
        bytesStore.release();
        bytesStore = store;
    }

    @Override
    public long capacity() {
        return 1L << 40;
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    @Override
    public void ensureCapacity(long size) {
        writeCheckOffset(size, 0);
    }

    @Override
    public boolean isNative() {
        return true;
    }
}
