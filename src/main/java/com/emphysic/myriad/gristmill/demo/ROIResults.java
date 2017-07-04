/*
 * com.emphysic.myriad.gristmill.demo.ROIResults
 *
 * Copyright (c) 2017 Emphysic LLC.
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
package com.emphysic.myriad.gristmill.demo;

import com.emphysic.myriad.core.data.roi.ROI;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * ROIResults - POJO for storing a file and ROI.
 * Created by Chris on 2017-07-04.
 */
@Slf4j
public class ROIResults {
    private String name;
    private File contents;
    private List<ROI> roi;

    public ROIResults(File contents) {
        this.contents = contents;
        try {
            this.name = genHash(contents.getAbsolutePath());
        } catch (Exception e) {
            this.name = Long.toHexString(System.currentTimeMillis());
            log.warn("Unable to generate hash for specified filename using ", name);
        }
        roi = new ArrayList<>();
    }

    public String getName() { return name; }

    public File getContents() { return contents; }

    public List<ROI> getROI() { return roi; }

    public void addROI(ROI newROI) {
        roi.add(newROI);
    }

    /**
     * Convenience method for generating a hash, primarily used to create a key for maps.
     * @param f original string
     * @return hash of f
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String genHash(String f) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] bytesOfMessage = f.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] thedigest = md.digest(bytesOfMessage);
        return Base64.getEncoder().encodeToString(thedigest).replace('=', '_').replace('&', '_');
    }
}
