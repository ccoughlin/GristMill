/*
 * com.emphysic.myriad.gristmill.demo.BaggerPool
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

import akka.actor.ActorRef;
import com.emphysic.myriad.core.data.roi.ROI;
import com.emphysic.myriad.network.LinkedWorkerPool;
import com.emphysic.myriad.network.messages.ImmutableMessage;
import com.emphysic.myriad.network.messages.ROIMessage;
import com.emphysic.myriad.network.messages.ShutdownMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * BaggerPool - Myriad pool for organizing ROI by source.
 * Created by Chris on 2017-07-04.
 */
@Slf4j
public class BaggerPool extends LinkedWorkerPool {
    /**
     * List of ROIs found for each original source
     */
    Map<String, ROIResults> rois;

    public BaggerPool(int numBaggers, Map<String, ROIResults> roiMap) {
        start(numBaggers, BaggerActor.class);
        this.rois = roiMap;
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof ROIMessage) {
            if (getSender().path().parent() == router.path()) {
                if (((ROIMessage) message).getROI() != null) {
                    ROI roi = ((ROIMessage) message).getROI();
                    String metadata = ((ROIMessage) message).getMetadata();
                    Map<String, String> md = ImmutableMessage.getMetadata(metadata);
                    roi.setMetadata(metadata);
                    String src = md.getOrDefault("source", "");
                    if (src != null && ! src.isEmpty()) {
                        if (rois.containsKey(src)) {
                            rois.get(src).addROI(roi);
                        } else {
                            log.error("Source " + src + " not found in " + rois.keySet());
                        }

                    }
                    tellNextActor(message);
                } else {
                    log.info("No flaw found");
                }
            } else {
                log.info(genMessage("received a ROI, processing"));
                tellRouter(message);
            }
        } else if (message instanceof ActorRef) {
            this.next = (ActorRef) message;
        } else if (message instanceof ShutdownMessage) {
            shutdown();
        }
    }
}
