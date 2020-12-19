/*
 * Copyright 2020 Ivan Velikanova  ivan.velikanova@gmail.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.jphil.checksum;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 *
 * @author Ivan Velikanova  ivan.velikanova@gmail.com.
 */
public class MultiPointQuickChecksum {
    private final long value;
    public static final MultiPointQuickChecksum UNDETERMINED = new MultiPointQuickChecksum(-1);
    public MultiPointQuickChecksum(long value) {
        this.value = value;
    }
    public static final String VERSION = "202002180553IST";
    public long getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int)value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MultiPointQuickChecksum other = (MultiPointQuickChecksum) obj;
        if (this.value != other.value) {
            return false;
        }
        return true;
    }    

    public static MultiPointQuickChecksum calculateMultiPointQuickChecksum(
            File f, Params params) {
        final Logger l = Logger.getLogger(MultiPointQuickChecksum.class.getName());
        RandomAccessFile raf = null;
        try {
            Checksum checksum = new CRC32();
            raf = new RandomAccessFile(f, "r");
            final long filesize = f.length();
            final int bufSize = (int) ( Math.min(params.bufsize, filesize)); 
            final byte[]bytes=new byte[bufSize]; 
            final long gap = Math.max(
                    Math.min(params.minimumStepGap,filesize),
                    filesize/params.maxSteps
            );
            int readlen; 
            long position = 0;
            while( (readlen=raf.read(bytes))!=-1 && position < filesize  ){
                int expectedread = bufSize;
                if(position + bufSize >= filesize){
                    expectedread = (int) ( filesize - position );
                    assert expectedread <= bufSize;
                }
                if(readlen!=expectedread)
                    throw new IllegalStateException("Cannot read file actualread="
                            +readlen+" bufSize="+bufSize+" expectedread="+expectedread+" p="+position);
                checksum.update(bytes, 0, readlen);
                position = position + gap;
                raf.seek(position);
            }
            raf.close();

            return new MultiPointQuickChecksum(checksum.getValue());
        } catch (Exception ex) {
            Logger.getLogger(MultiPointQuickChecksum.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if(raf!=null)raf.close();
            } catch (IOException ex) {
                Logger.getLogger(MultiPointQuickChecksum.class.getName()).log(Level.SEVERE,null,ex);
            }
        }
        return UNDETERMINED;
    }
    
    public static final class Params {
        private static final int KB = 1024;
        public final int bufsize;
        public final int maxSteps;
        public final int minimumStepGap;
        public static final Params DEFAULT = new Params();
        private Params() {
            bufsize = 8*KB; //512KB is golden size while reading fully
            // 4KB is sector size as per filesystem
            // 16KB and 32KB are suggested
            // choosing 8KB
            maxSteps = 20; 
            minimumStepGap = 512*KB;// /step
        }
        public Params(int bufsize, int maxSteps, int stepIncrementRate) {
            this.bufsize = bufsize;
            this.maxSteps = maxSteps;
            this.minimumStepGap = stepIncrementRate;
        }
    }
}
