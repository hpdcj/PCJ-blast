/* 
 * Copyright (c) 2017, Marek Nowicki
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.pcj.blast;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 *
 * @author Marek Nowicki
 */
public class XmlSplitter {

    private final Reader reader;
    private char[] remainingCharacters = new char[0];
    private boolean globalFileEnded = false;

    public XmlSplitter(Reader reader) {
        this.reader = reader;
    }

    public Reader nextXml() {
        if (globalFileEnded) {
            return null;
        }
        return new Reader() {
            private boolean fileEnded = false;
            private boolean start = true;

            @Override
            public int read(char[] cbuf, int off, int _len) throws IOException {
                if (fileEnded) {
                    return -1;
                }

                int len = _len + 5;
                char[] buf = new char[len];
                int bytesCopied = Math.min(remainingCharacters.length, len);
                System.arraycopy(remainingCharacters, 0, buf, 0, bytesCopied);

                if (buf.length > bytesCopied) {
                    int bytesRead = reader.read(buf, bytesCopied, buf.length - bytesCopied);
                    if (bytesRead > 0) {
                        bytesCopied += bytesRead;
                    } else {
                        globalFileEnded = true;
                    }
                }
                if (bytesCopied == 0) {
                    fileEnded = true;
                    return -1;
                }

                int limit = Math.min(bytesCopied, _len);
                for (int i = 0; i < limit; ++i) {
                    if (!start && i + 5 < bytesCopied
                            && buf[i + 1] == '?'
                            && buf[i + 0] == '<'
                            && buf[i + 2] == 'x'
                            && buf[i + 3] == 'm'
                            && buf[i + 4] == 'l') {
                        fileEnded = true;
                        if (buf.length < remainingCharacters.length) {
                            remainingCharacters = Arrays.copyOfRange(remainingCharacters, i, remainingCharacters.length);
                        } else {
                            remainingCharacters = Arrays.copyOfRange(buf, i, bytesCopied);
                        }
                        return i == 0 ? -1 : i;
                    } else {
                        start = false;
                        cbuf[off + i] = buf[i];
                    }
                }
                if (buf.length < remainingCharacters.length) {
                    remainingCharacters = Arrays.copyOfRange(remainingCharacters, limit, remainingCharacters.length);
                } else {
                    remainingCharacters = Arrays.copyOfRange(buf, limit, bytesCopied);
                }

                return limit;
            }

            @Override
            public void close() throws IOException {
                fileEnded = true;
            }
        };
    }
}
