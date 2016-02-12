/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pcj.biojava;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 *
 * @author faramir
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
                if (fileEnded == true) {
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
