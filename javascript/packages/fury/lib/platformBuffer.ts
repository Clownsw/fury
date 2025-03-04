/*
 * Copyright 2023 The Fury Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { isNodeEnv } from "./util";

export interface PlatformBuffer extends Uint8Array {
    latin1Slice(start: number, end: number): string,
    utf8Slice(start: number, end: number): string,
    latin1Write(v: string, offset: number): void,
    utf8Write(v: string, offset: number): void,
    copy(target: Uint8Array, targetStart?: number, sourceStart?: number, sourceEnd?: number): void
}


export class BrowserBuffer extends Uint8Array implements PlatformBuffer {
    static alloc(size: number) {
        return new BrowserBuffer(new Uint8Array(size));
    }

    latin1Write(string: string, offset: number) {
        for (let index = 0; index < string.length; index++) {
            this[offset++] = string.charCodeAt(index);
        }
    }

    utf8Write(string: string, offset: number) {
        let c1: number;
        let c2: number;
        for (let i = 0; i < string.length; ++i) {
            c1 = string.charCodeAt(i);
            if (c1 < 128) {
                this[offset++] = c1;
            } else if (c1 < 2048) {
                this[offset++] = (c1 >> 6) | 192;
                this[offset++] = (c1 & 63) | 128;
            } else if (
                (c1 & 0xfc00) === 0xd800 &&
                ((c2 = string.charCodeAt(i + 1)) & 0xfc00) === 0xdc00
            ) {
                c1 = 0x10000 + ((c1 & 0x03ff) << 10) + (c2 & 0x03ff);
                ++i;
                this[offset++] = (c1 >> 18) | 240;
                this[offset++] = ((c1 >> 12) & 63) | 128;
                this[offset++] = ((c1 >> 6) & 63) | 128;
                this[offset++] = (c1 & 63) | 128;
            } else {
                this[offset++] = (c1 >> 12) | 224;
                this[offset++] = ((c1 >> 6) & 63) | 128;
                this[offset++] = (c1 & 63) | 128;
            }
        }
    }

    latin1Slice(start: number, end: number) {
        if (end - start < 1) {
            return "";
        }
        let str = "";
        for (let i = start; i < end;) {
            str += String.fromCharCode(this[i++]);
        }
        return str;
    }

    utf8Slice(start: number, end: number) {
        if (end - start < 1) {
            return "";
        }
        let str = "";
        for (let i = start; i < end;) {
            const t = this[i++];
            if (t <= 0x7F) {
                str += String.fromCharCode(t);
            } else if (t >= 0xC0 && t < 0xE0) {
                str += String.fromCharCode((t & 0x1F) << 6 | this[i++] & 0x3F);
            } else if (t >= 0xE0 && t < 0xF0) {
                str += String.fromCharCode((t & 0xF) << 12 | (this[i++] & 0x3F) << 6 | this[i++] & 0x3F);
            } else if (t >= 0xF0) {
                const t2 = ((t & 7) << 18 | (this[i++] & 0x3F) << 12 | (this[i++] & 0x3F) << 6 | this[i++] & 0x3F) - 0x10000;
                str += String.fromCharCode(0xD800 + (t2 >> 10));
                str += String.fromCharCode(0xDC00 + (t2 & 0x3FF));
            }
        }
        return str;
    }

    copy(target: Uint8Array, targetStart?: number, sourceStart?: number, sourceEnd?: number) {
        target.set(this.subarray(sourceStart, sourceEnd), targetStart);
    }

    static byteLength(str: string) {
        let len = 0;
        let c = 0;
        for (let i = 0; i < str.length; ++i) {
            c = str.charCodeAt(i);
            if (c < 128)
                len += 1;
            else if (c < 2048)
                len += 2;
            else if ((c & 0xFC00) === 0xD800 && (str.charCodeAt(i + 1) & 0xFC00) === 0xDC00) {
                ++i;
                len += 4;
            } else
                len += 3;
        }
        return len;
    }
}

export const fromUint8Array = isNodeEnv ?
    (ab: Buffer | Uint8Array) => {
        if (!Buffer.isBuffer(ab)) {
            return (Buffer.from(ab) as unknown as PlatformBuffer)
        } else {
            return ab as unknown as PlatformBuffer;
        }
    } :
    (ab: Buffer | Uint8Array) => new BrowserBuffer(ab)

export const alloc = (isNodeEnv ? Buffer.allocUnsafe : BrowserBuffer.alloc) as unknown as (size: number) => PlatformBuffer

export const strByteLength = isNodeEnv ? Buffer.byteLength : BrowserBuffer.byteLength
