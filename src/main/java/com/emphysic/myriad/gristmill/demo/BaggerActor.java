/*
 * com.emphysic.myriad.gristmill.demo.BaggerActor
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

import akka.actor.UntypedActor;
import com.emphysic.myriad.core.data.roi.ROI;
import com.emphysic.myriad.network.messages.ROIMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * BaggerActor - handles Region Of Interest (ROI) messages.
 * Created by Chris on 2017-07-04.
 */
@Slf4j
public class BaggerActor extends UntypedActor {
    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof ROIMessage) {
            ROI roi = ((ROIMessage) message).getROI();
            if (roi != null) {
                // TODO: replace NOOP - ?
                getSender().tell(message, getSelf());
            }
        }

    }
}
