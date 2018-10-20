package cn.edu.pku.sei.intellide.graph.extraction.mail_to_neo4j.utils;

/**************************************************************
 Licensed to the Apache Software Foundation (ASF) under one   *
 or more contributor license agreements.  See the NOTICE file *
 distributed with this work for additional information        *
 regarding copyright ownership.  The ASF licenses this file   *
 to you under the Apache License, Version 2.0 (the            *
 "License"); you may not use this file except in compliance   *
 with the License.  You may obtain a copy of the License at   *

 http://www.apache.org/licenses/LICENSE-2.0                 *

 Unless required by applicable law or agreed to in writing,   *
 software distributed under the License is distributed on an  *
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 KIND, either express or implied.  See the License for the    *
 specific language governing permissions and limitations      *
 under the License.                                           *
 */

import java.io.CharConversionException;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MboxIterator implements Iterable<CharBufferWrapper>, Closeable {

    private final FileInputStream theFile;
    private final CharBuffer mboxCharBuffer;
    private Matcher fromLineMatcher;
    private boolean fromLineFound;
    private final MappedByteBuffer byteBuffer;
    private final CharsetDecoder DECODER;
    /**
     * Flag to signal end of input to {@link CharsetDecoder#decode(java.nio.ByteBuffer)}
     * .
     */
    private boolean endOfInputFlag = false;
    private final int maxMessageSize;
    private final Pattern MESSAGE_START;
    private int findStart = -1;
    private int findEnd = -1;
    private final File mbox;

    private MboxIterator(final File mbox,
                         final Charset charset,
                         final String regexpPattern,
                         final int regexpFlags,
                         final int MAX_MESSAGE_SIZE)
            throws IOException {
        //TODO: do better exception handling - try to process some of them maybe?
        this.maxMessageSize = MAX_MESSAGE_SIZE;
        this.MESSAGE_START = Pattern.compile(regexpPattern, regexpFlags);
        this.DECODER = charset.newDecoder();
        this.mboxCharBuffer = CharBuffer.allocate(MAX_MESSAGE_SIZE);
        this.mbox = mbox;
        this.theFile = new FileInputStream(mbox);
        this.byteBuffer = theFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, theFile.getChannel().size());
        initMboxIterator();
    }

    /**
     * initialize the Mailbox iterator
     */
    private void initMboxIterator() throws IOException {
        decodeNextCharBuffer();
        fromLineMatcher = MESSAGE_START.matcher(mboxCharBuffer);
        fromLineFound = fromLineMatcher.find();
        if (fromLineFound) {
            saveFindPositions(fromLineMatcher);
        } else if (fromLineMatcher.hitEnd()) {
            String path = "";
            if (mbox != null)
                path = mbox.getPath();
            throw new IOException("File " + path + " does not contain From_ lines that match the pattern '"
                    + MESSAGE_START.pattern() + "'! Maybe not be a valid Mbox or wrong matcher.");
        }
    }

    private void decodeNextCharBuffer() throws CharConversionException {
        CoderResult coderResult = DECODER.decode(byteBuffer, mboxCharBuffer, endOfInputFlag);
        updateEndOfInputFlag();
        mboxCharBuffer.flip();
    }

    private void updateEndOfInputFlag() {
        if (byteBuffer.remaining() <= maxMessageSize) {
            endOfInputFlag = true;
        }
    }

    private void saveFindPositions(Matcher lineMatcher) {
        findStart = lineMatcher.start();
        findEnd = lineMatcher.end();
    }

    @Override
    public Iterator<CharBufferWrapper> iterator() {
        return new MessageIterator();
    }

    @Override
    public void close() throws IOException {
        theFile.close();
    }

    private class MessageIterator implements Iterator<CharBufferWrapper> {

        @Override
        public boolean hasNext() {
            if (!fromLineFound) {
                try {
                    close();
                } catch (IOException e) {
                    throw new RuntimeException("Exception closing file!");
                }
            }
            return fromLineFound;
        }

        /**
         * Returns a CharBuffer instance that contains a message between position and limit.
         * The array that backs this instance is the whole block of decoded messages.
         *
         * @return CharBuffer instance
         */
        @Override
        public CharBufferWrapper next() {
            final CharBuffer message;
            fromLineFound = fromLineMatcher.find();
            if (fromLineFound) {
                message = mboxCharBuffer.slice();
                message.position(findEnd + 1);
                saveFindPositions(fromLineMatcher);
                message.limit(fromLineMatcher.start());
            } else {
                /* We didn't find other From_ lines this means either:
                 *  - we reached end of mbox and no more messages
                 *  - we reached end of CharBuffer and need to decode another batch.
                 */
                if (byteBuffer.hasRemaining()) {
                    // decode another batch, but remember to copy the remaining chars first
                    CharBuffer oldData = mboxCharBuffer.duplicate();
                    mboxCharBuffer.clear();
                    oldData.position(findStart);
                    while (oldData.hasRemaining()) {
                        mboxCharBuffer.put(oldData.get());
                    }
                    try {
                        decodeNextCharBuffer();
                    } catch (CharConversionException ex) {
                        throw new RuntimeException(ex);
                    }
                    fromLineMatcher = MESSAGE_START.matcher(mboxCharBuffer);
                    fromLineFound = fromLineMatcher.find();
                    if (fromLineFound) {
                        saveFindPositions(fromLineMatcher);
                    }
                    message = mboxCharBuffer.slice();
                    message.position(fromLineMatcher.end() + 1);
                    fromLineFound = fromLineMatcher.find();
                    if (fromLineFound) {
                        saveFindPositions(fromLineMatcher);
                        message.limit(fromLineMatcher.start());
                    }
                } else {
                    message = mboxCharBuffer.slice();
                    message.position(findEnd + 1);
                    message.limit(message.capacity());
                }
            }
            return new CharBufferWrapper(message);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public static Builder fromFile(File filePath) {
        return new Builder(filePath);
    }

    public static Builder fromFile(String file) {
        return new Builder(file);
    }

    public static class Builder {

        private final File file;
        private Charset charset = Charset.forName("UTF-8");
        private String regexpPattern = FromLinePatterns.DEFAULT;
        private int flags = Pattern.MULTILINE;
        /**
         * Default max message size in chars: ~ 10MB chars. If the mbox file contains larger
         * messages they will not be decoded correctly.
         */
        private int maxMessageSize = 10 * 1024 * 1024;

        private Builder(String filePath) {
            this(new File(filePath));
        }

        private Builder(File file) {
            this.file = file;
        }

        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder fromLine(String fromLine) {
            this.regexpPattern = fromLine;
            return this;
        }

        public Builder flags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder maxMessageSize(int maxMessageSize) {
            this.maxMessageSize = maxMessageSize;
            return this;
        }

        public MboxIterator build() throws IOException {
            return new MboxIterator(file, charset, regexpPattern, flags, maxMessageSize);
        }
    }

    /**
     * Utility method to log important details about buffers.
     */
    public static String bufferDetailsToString(final Buffer buffer) {
        return "Buffer details: " + "\ncapacity:\t" + buffer.capacity() +
                "\nlimit:\t" + buffer.limit() +
                "\nremaining:\t" + buffer.remaining() +
                "\nposition:\t" + buffer.position() +
                "\nbuffer:\t" + buffer.isReadOnly() +
                "\nclass:\t" + buffer.getClass();
    }
}