/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.unine.vauchers.fuseerasure.codes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * A class with the information of a raid codec.
 * A raid codec has the information of
 * 1. Which ErasureCode used
 * 2. Stripe and parity length
 * 3. Parity directory location
 * 4. Codec priority
 */
public class Codec implements Serializable {

    public static final Log LOG = LogFactory.getLog(Codec.class);

    public static final String ERASURE_CODE_KEY_PREFIX = "hdfs.raid.erasure.code.";

    /**
     * Used by ErasureCode.init() to get Code specific extra parameters.
     */
    public final JSONObject json;

    /**
     * id of the codec. Used by policy in raid.xml
     */
    public final String id;

    /**
     * Number of blocks in one stripe
     */
    public final int stripeLength;

    /**
     * Number of parity blocks of the codec for one stripe
     */
    public final int parityLength;

    /**
     * The full class name of the ErasureCode used
     */
    public final String erasureCodeClass;

    /**
     * Human readable description of the codec
     */
    public final String description;

    /**
     * Where to store the parity files
     */
    public final String parityDirectory;

    /**
     * Where to store the temp parity files
     */
    public final String tmpParityDirectory;

    /**
     * Where to store the temp har files
     */
    public final String tmpHarDirectory;

    /**
     * Simulate the block fix or not
     */
    public final boolean simulateBlockFix;

    /**
     * Priority of the codec.
     *
     * Purge parity files:
     *   When parity files of two Codecs exists, the parity files of the lower
     *   priority codec will be purged.
     *
     * Generating parity files:
     *   When a source files are under two policies, the policy with a higher
     *   codec priority will be triggered.
     */
    public final int priority;

    /**
     * Is file-level raiding or directory-level raiding
     */
    public boolean isDirRaid;

    public Codec(JSONObject json) throws JSONException {
        this.json = json;
        this.id = json.getString("id");
        this.parityLength = json.getInt("parity_length");
        this.stripeLength = json.getInt("stripe_length");
        this.erasureCodeClass = json.getString("erasure_code");
        this.parityDirectory = json.getString("parity_dir");
        this.priority = json.getInt("priority");
        this.description = getJSONString(json, "description", "");
        this.isDirRaid = Boolean.parseBoolean(getJSONString(json, "dir_raid", "false"));
        this.tmpParityDirectory = getJSONString(
                json, "tmp_parity_dir", "/tmp" + this.parityDirectory);
        this.tmpHarDirectory = getJSONString(
                json, "tmp_har_dir", "/tmp" + this.parityDirectory + "_har");
        this.simulateBlockFix = json.getBoolean("simulate_block_fix");
        checkDirectory(parityDirectory);
        checkDirectory(tmpParityDirectory);
        checkDirectory(tmpHarDirectory);
    }

    /**
     * Make sure the direcotry string has the format "/a/b/c"
     */
    private void checkDirectory(String d) {
        if (!d.startsWith("/")) {
            throw new IllegalArgumentException("Bad directory:" + d);
        }
        if (d.endsWith("/")) {
            throw new IllegalArgumentException("Bad directory:" + d);
        }
    }

    static private String getJSONString(
            JSONObject json, String key, String defaultResult) {
        String result = defaultResult;
        try {
            result = json.getString(key);
        } catch (JSONException e) {
        }
        return result;
    }

    @Override
    public String toString() {
        if (json == null) {
            return "Test codec " + id;
        } else {
            return json.toString();
        }
    }

    public String getParityPrefix() {
        String prefix = this.parityDirectory;
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix;
    }

    /**
     * Used by unit test only
     */
    Codec(String id,
          int parityLength,
          int stripeLength,
          String erasureCodeClass,
          String parityDirectory,
          int priority,
          String description,
          String tmpParityDirectory,
          String tmpHarDirectory,
          boolean isDirRaid,
          boolean simulateBlockFix) {
        this.json = null;
        this.id = id;
        this.parityLength = parityLength;
        this.stripeLength = stripeLength;
        this.erasureCodeClass = erasureCodeClass;
        this.parityDirectory = parityDirectory;
        this.priority = priority;
        this.description = description;
        this.tmpParityDirectory = tmpParityDirectory;
        this.tmpHarDirectory = tmpHarDirectory;
        this.isDirRaid = isDirRaid;
        this.simulateBlockFix = simulateBlockFix;
    }
}
