/*
 * com.emphysic.myriad.gristmill.GristMill
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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.emphysic.myriad.core.data.ops.GaussianPyramidOperation;
import com.emphysic.myriad.core.data.roi.ROIBundle;
import com.emphysic.myriad.network.PyramidActorPool;
import com.emphysic.myriad.network.ROIFinderPool;
import com.emphysic.myriad.network.SlidingWindowPool;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * GristMill - grinds DatasetMessages into ROI, i.e. runs a scale-invariant scan of raw data looking for Regions Of
 * Interest (ROI).
 * Created by Chris on 2017-06-11.
 */
@Slf4j
public class GristMill {
    /**
     * Scale space Actor pool
     */
    private ActorRef pyramidActorPool;
    /**
     * Sliding window Actor pool
     */
    private ActorRef slidingWindowPool;
    /**
     * Region Of Interest (ROI) finder pool
     */
    private ActorRef finderPool;
    /**
     * The ROI finder and its preprocessing operation(s)
     */
    private ROIBundle roiBundle;
    /**
     * The Gaussian pyramid operation used to scale the input data
     */
    private GaussianPyramidOperation gpo;

    /**
     * Main Akka system
     */
    private ActorSystem system;
    /**
     * Akka configuration
     */
    private final Config config;

    public GristMill(Config config) {
        this.config = config;
        system = ActorSystem.create("MyriadGristMill", config);
    }

    /**
     * Constructs the processing pipeline.
     * @return true if configuration of each stage was successful, false otherwise.
     */
    boolean startup() {
        try {
            log.info("Creating processing pipeline");
            if (gpo == null) {
                log.info("No pyramid operation specfied, using defaults");
                configurePyramid();
            }
            pyramidActorPool = system.actorOf(Props.create(
                    PyramidActorPool.class,
                    config.getInt("pyramid.number"),
                    gpo),
                    "PyramidPool");
            slidingWindowPool = system.actorOf(Props.create(
                    SlidingWindowPool.class,
                    config.getInt("slider.number"),
                    config.getInt("slider.step"),
                    config.getInt("slider.window.width"),
                    config.getInt("slider.window.height")),
                    "SlidingWindowPool");
            if (roiBundle == null) {
                log.info("No ROIBundle specified, attempting to read from config");
                String bundlePath = config.getString("roi.bundle");
                if (bundlePath == null || bundlePath.isEmpty()) {
                    log.error("No ROIBundle path specified!");
                    throw new IllegalArgumentException("No ROIBundle path specified");
                }
                File bundleFile = new File(bundlePath);
                if (!bundleFile.canRead()) {
                    log.error("Unable to read ROIBundle file ", bundleFile, "");
                    throw new IllegalArgumentException("Unable to read ROIBundle file");
                }
                roiBundle = new ROIBundle();
                roiBundle.load(bundleFile);
            }
            finderPool = system.actorOf(Props.create(
                    ROIFinderPool.class,
                    config.getInt("roi.number"),
                    roiBundle),
                    "ROIFinderPool");
            pyramidActorPool.tell(slidingWindowPool, system.guardian());
            slidingWindowPool.tell(finderPool, pyramidActorPool);
            return true;
        } catch (Exception e) {
            log.error("An error occurred constructing the pipeline: ", e);
        }
        return false;
    }

    /**
     * Shuts the Akka system down.
     */
    public void shutdown() {
        if (system != null) {
            system.shutdown();
        }
    }

    /**
     * Shuts the Akka system down and exits the application.
     * @param errorcode error code to return on System.exit call
     */
    public void shutdown(int errorcode) {
        shutdown();
        System.exit(errorcode);
    }

    /**
     * Attempts to instantiate a default pyramid operation based on the configuration.
     */
    private void configurePyramid() {
        gpo = new GaussianPyramidOperation(
                config.getInt("pyramid.scalefactor"),
                config.getInt("pyramid.windowsize")
        );
    }

    /**
     * Returns the current ROIBundle - a package containing the Region Of Interest (ROI) finder, its preprocessing
     * operation(s), and associated metadata
     * @return current ROIBundle
     */
    public ROIBundle getRoiBundle() {
        return roiBundle;
    }

    /**
     * Sets the ROIBundle.
     * @param roiBundle ROIBundle
     */
    public void setRoiBundle(ROIBundle roiBundle) {
        this.roiBundle = roiBundle;
    }

    /**
     * Returns the current scale space operation
     * @return Gaussian pyramid operation
     */
    public GaussianPyramidOperation getGpo() {
        return gpo;
    }

    /**
     * Sets the scale space operaion
     * @param gpo new Gaussian pyramid operation
     */
    public void setGpo(GaussianPyramidOperation gpo) {
        this.gpo = gpo;
    }

    /**
     * Returns the current system configuration
     * @return current configuration
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Returns the current scale space pool
     * @return current scale space pool
     */
    public ActorRef getPyramidActorPool() {
        return pyramidActorPool;
    }

    /**
     * Sets the scale space pool
     * @param pyramidActorPool reference to new space pool
     */
    public void setPyramidActorPool(ActorRef pyramidActorPool) {
        this.pyramidActorPool = pyramidActorPool;
    }

    /**
     * Returns the current sliding window pool
     * @return current sliding window pool
     */
    public ActorRef getSlidingWindowPool() {
        return slidingWindowPool;
    }

    /**
     * Sets the sliding window pool
     * @param slidingWindowPool reference to new sliding window pool
     */
    public void setSlidingWindowPool(ActorRef slidingWindowPool) {
        this.slidingWindowPool = slidingWindowPool;
    }

    /**
     * Returns the current ROIFinder pool
     * @return reference to current ROIFinder pool
     */
    public ActorRef getFinderPool() {
        return finderPool;
    }

    /**
     * Sets the ROIFinder pool
     * @param finderPool reference to new ROIFinder pool
     */
    public void setFinderPool(ActorRef finderPool) {
        this.finderPool = finderPool;
    }
}
