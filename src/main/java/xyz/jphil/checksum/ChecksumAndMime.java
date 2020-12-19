/*
 * Copyright 2019 Ivan Velikanova <ivan@jphil.xyz>
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.tika.Tika;


/**
 *
 * @author Ivan Velikanova <ivan@jphil.xyz>
 */
public final class ChecksumAndMime {
    private final long checksum;
    private final String mimeType;
    private final String title;
    private final long fileSize;
    
    public ChecksumAndMime(Checksum checksum, String mediaType,
            String title,long fileSize) {
        this(checksum==null?-1L:checksum.getValue(),mediaType,title,fileSize);
    }
    
    public ChecksumAndMime(long checksum, String mediaType,
            String title,long fileSize) {
        this.checksum = checksum;
        this.mimeType = mediaType;
        this.title = title;
        this.fileSize = fileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFilename() {
        return title;
    }
    
    public String getTitle() {
        return title;
    }

    public String getChecksum() {
        return Long.toHexString(checksum);
    }
    
    public long getChecksumLong(){
        return checksum;
    }

    public String getMediaType() {
        return mimeType;
    }
    
    public static final class Builder<E> { 
        private long checksum;
        private String mediaType;
        private String title;
        private long fileSize;
        private E caller;
        public static final <Z> Builder<Z> create(){
            return new Builder<>();
        }

        public Builder<E> initializeFrom(ChecksumAndMime c){
            mediaType = c.getMediaType();
            title = c.getTitle();
            fileSize = c.getFileSize();
            checksum = c.checksum;
            return this;
        }
        
        public ChecksumAndMime build(){
            return new ChecksumAndMime(checksum, mediaType, title, fileSize);
        }

        public Builder setChecksum(long checksum) {
            this.checksum = checksum; return this;
        }
        
        public Builder updateFrom(Path localFile, boolean updateTitle) {
            ChecksumAndMime c = calculateChecksumAndMime(localFile.toFile(), null);
            mediaType = c.getMediaType();
            if(updateTitle)title = c.getTitle();
            fileSize = c.getFileSize();
            checksum = c.checksum;
            return this;
        }

        public Builder setMediaType(String mediaType) {
            this.mediaType = mediaType; return this;
        }

        public Builder setTitle(String title) {
            this.title = title; return this;
        }

        public Builder setFileSize(long fileSize) {
            this.fileSize = fileSize; return this;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + (int) (this.checksum ^ (this.checksum >>> 32));
            hash = 89 * hash + Objects.hashCode(this.mediaType);
            hash = 89 * hash + Objects.hashCode(this.title);
            hash = 89 * hash + (int) (this.fileSize ^ (this.fileSize >>> 32));
            return hash;
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
            final Builder other = (Builder) obj;
            if (this.checksum != other.checksum) {
                return false;
            }
            if (this.fileSize != other.fileSize) {
                return false;
            }
            if (!Objects.equals(this.mediaType, other.mediaType)) {
                return false;
            }
            if (!Objects.equals(this.title, other.title)) {
                return false;
            }
            return true;
        }
        
        
    }

    /*public String createJsonComment(){
        final JSONObject jsono = new JSONObject();
        Map<String,Object> map = getAsMap();
        jsono.putAll(map);
        return jsono.toString();
    }*/
    public Map getAsMap(){
        final Map<String,Object> map = new HashMap<>();
        throw new UnsupportedOperationException("Implement neutrally, probably like ruby, pattern matching");
    }
    
    public static ChecksumAndMime calculateChecksumAndMime(
            File f,String overrideName) {
        overrideName = overrideName==null?f.getName():overrideName;
        String mimeType = URLConnection.getFileNameMap()
                .getContentTypeFor(overrideName);
        final Tika t = new Tika();
        final Logger l = Logger.getLogger(ChecksumAndMime.class.getName());
        FileInputStream fis = null;
        try {
            Checksum checksum = new CRC32();
            fis = new FileInputStream(f); //512KB is golden size
            int bufSize = (int) ( Math.min(1024*512, f.length())); 
            final byte[]bytes=new byte[bufSize]; 
            int len; 
            
            String mimeCheckL1 = null;
            //apache Tika is costly, 
            //avoid as long as it can be avoided
            if(mimeType==null && (len=fis.read(bytes))!=-1 ){
                checksum.update(bytes, 0, len);
                mimeCheckL1 = t.detect(bytes,overrideName);
            }
            while( (len=fis.read(bytes))!=-1 ){
                checksum.update(bytes, 0, len);
            }
            fis.close();
            mimeType = mimeType==null?mimeCheckL1:mimeType;
            if(mimeType==null){ // use only in failure not otherwise
                //l.log(Level.INFO, "ApacheTikaPeekMime={0}", mimeCheckL1);
                //l.log(Level.INFO, "URLConnection.getFileNameMap={0}", mimeType);
                mimeType = t.detect(f);
                //l.log(Level.INFO, "ApacheTikaComplete={0}", mimeType);
            }
            return new ChecksumAndMime(checksum, mimeType,overrideName,f.length());
        } catch (Exception ex) {
            Logger.getLogger(ChecksumAndMime.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(ChecksumAndMime.class.getName()).log(Level.SEVERE,null,ex);
            }
        }
        return new ChecksumAndMime(null, mimeType,overrideName,f.length());
    }
    
    public static String justGetMime(File f,String overrideName){
        overrideName = overrideName==null?f.getName():overrideName;
        String mimeType = URLConnection.getFileNameMap()
                .getContentTypeFor(overrideName);
        
        final Tika t = new Tika();
        String mimeCheckL1 = null;
        //apache Tika is costly, 
        //avoid as long as it can be avoided
        if(mimeType==null  ){
            try {
                mimeCheckL1 = t.detect((byte[])null,overrideName);
            } catch (Exception e) {
            }
        }
        mimeType = mimeType==null?mimeCheckL1:mimeType;
        if(mimeType==null){ // use only in failure not otherwise
            final Logger l = Logger.getLogger(ChecksumAndMime.class.getName());
            //l.log(Level.INFO, "ApacheTikaPeekMime={0}", mimeCheckL1);
            //l.log(Level.INFO, "URLConnection.getFileNameMap={0}", mimeType);
            try{
                mimeType = t.detect(f);
            }catch(Exception a){
                l.log(Level.SEVERE, "Exception in getting mime using Tika", a);
                return null;
            }
            //l.log(Level.INFO, "ApacheTikaComplete={0}", mimeType);
        }
        return mimeType;
    }
}
