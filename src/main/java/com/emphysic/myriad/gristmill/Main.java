/*
 * com.emphysic.myriad.gristmill.Main
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
package com.emphysic.myriad.gristmill;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * Main entry point for the GristMill Myriad app - loads a configuration file and starts up the GristMill Region Of
 * Interest (ROI) finder application.
 * Created by Chris on 2017-06-11.
 */
@Slf4j
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("No configuration file specified, exiting.");
            System.exit(0);
        }
        File configFile = new File(args[0]);
        if (!configFile.canRead()) {
            log.error("Unable to read configuration file '", configFile, ",', exiting.");
            System.exit(1);
        }
        Config config = ConfigFactory.parseFile(configFile);
        GristMill mill = new GristMill(config);
        boolean ready = mill.startup();
        if (ready) {
            log.info("Successfully initiated grist mill.");
        } else {
            log.error("Unable to construct grist mill, please check logs for details.");
            System.exit(1);
        }
    }
}
