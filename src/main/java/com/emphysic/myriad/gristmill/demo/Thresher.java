/*
 * com.emphysic.myriad.gristmill.demo.Thresher
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

import akka.actor.*;
import akka.util.Timeout;
import com.emphysic.myriad.core.data.io.Dataset;
import com.emphysic.myriad.core.data.util.FileSniffer;
import com.emphysic.myriad.network.DataIngestorPool;
import com.emphysic.myriad.network.messages.FileMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.Await;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Thresher - demonstrates how to "feed" data files into the GristMill system.
 * Created by Chris on 2017-06-17.
 */
@Slf4j
public class Thresher {

    private String sinkHostName = "localhost";
    private Integer sinkHostPort = 9999;
    private String sinkSystem = "MyriadGristMill";
    private String sinkActor = "PyramidPool";

    private String sourceHostName = "localhost";
    private Integer sourceHostPort = 9999;
    private String sourceSystem = "MyriadGristMill";
    private String sourceActor = "ReporterPool";

    /**
     * List of ROI for each source file
     */
    private Map<String, ROIResults> results;

    /**
     * Main Akka system
     */
    private ActorSystem system;
    /**
     * Akka configuration
     */
    private final Config config;

    /**
     * Data file ingestor
     */
    private ActorRef ingestor;

    /**
     * Results receiver
     */
    private ActorRef receiver;

    public Thresher(Config config) {
        this.config = config;
        system = ActorSystem.create("MyriadThresher", config);
        results = new HashMap<>();
    }

    /**
     * Initializes the system
     * @return True if successful, False if a problem occurred
     */
    public boolean init() {
        try {
            configSink();
            configSource();
            log.info("Initializing ingestor");
            ingestor = system.actorOf(Props.create(DataIngestorPool.class, config.getInt("ingestor.number")),
                    "IngestorPool");
            receiver = system.actorOf(Props.create(BaggerPool.class, config.getInt("receiver.number"), results),
                    "Receiver");
            Optional<ActorRef> sink = getActorRef(getSinkURL());
            if (sink.isPresent()) {
                log.info("Found remote system sink, connecting...");
                ingestor.tell(sink.get(), system.guardian());
            } else {
                log.error("Unable to find remote system sink " + getSinkURL());
                return false;
            }

            Optional<ActorRef> source = getActorRef(getSourceURL());
            if (source.isPresent()) {
                log.info("Found remote system source, connecting...");
                source.get().tell(receiver, system.guardian());
            } else {
                log.error("Unable to find remote system source " + getSourceURL());
            }
        } catch (Exception e) {
            log.error("Encountered an error initializing the feeder", e);
            return false;
        }
        return true;
    }

    /**
     * Configures the data "sink," i.e. where we send the data for analysis.
     */
    private void configSink() {
        if (config.hasPath("ingestor.sink.hostname")) {
            String hostname = config.getString("ingestor.sink.hostname");
            if (hostname == null || hostname.isEmpty()) {
                try {
                    hostname = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    log.error("Trying to use localhost for sink: caught an exception trying to retrieve IP address: " + e);
                    hostname = "localhost";
                }
            }
            sinkHostName = hostname;
        }
        log.info("Setting sink hostname to " + sinkHostName);
        if (config.hasPath("ingestor.sink.port")) {
            sinkHostPort = config.getInt("ingestor.sink.port");
        }
        log.info("Setting sink port to " + sinkHostPort);
        if (config.hasPath("ingestor.sink.system")) {
            sinkSystem = config.getString("ingestor.sink.system");
        }
        log.info("Setting sink system to " + sinkSystem);
        if (config.hasPath("ingestor.sink.actor")) {
            sinkActor = config.getString("ingestor.sink.actor");
        }
        log.info("Setting sink Actor to " + sinkActor);
        log.info("Files to be sent to " + getSinkURL());
    }

    /**
     * Configures the data "source," i.e. where we should look for Region Of Interest results
     */
    private void configSource() {
        if (config.hasPath("receiver.source.hostname")) {
            String hostname = config.getString("receiver.source.hostname");
            if (hostname == null || hostname.isEmpty()) {
                try {
                    hostname = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    log.error("Trying to use localhost for source: caught an exception trying to retrieve IP address: " + e);
                    hostname = "localhost";
                }
            }
            sourceHostName = hostname;
        }
        log.info("Setting source hostname to " + sourceHostName);
        if (config.hasPath("receiver.source.port")) {
            sourceHostPort = config.getInt("receiver.source.port");
        }
        log.info("Setting source port to " + sourceHostPort);
        if (config.hasPath("receiver.source.system")) {
            sourceSystem = config.getString("receiver.source.system");
        }
        log.info("Setting source system to " + sourceSystem);
        if (config.hasPath("receiver.source.actor")) {
            sourceActor = config.getString("receiver.source.actor");
        }
        log.info("Setting source Actor to " + sourceActor);
        log.info("Files to be sent to " + getSinkURL());
    }

    /**
     * Retrieves the data sink URL
     * @return Myriad URL
     */
    public String getSinkURL() {
        return getActorURL(sinkSystem, sinkHostName, sinkHostPort, sinkActor);
    }

    /**
     * Retrieves the data source URL
     * @return Myriad URL
     */
    public String getSourceURL() {
        return getActorURL(sourceSystem, sourceHostName, sourceHostPort, sourceActor);
    }

    public String getActorURL(String system, String host, Integer port, String actorName) {
        return "akka.tcp://" + system + "@" + host + ":" + port + "/user/" + actorName;
    }

    /**
     * Returns an Akka ActorRef from a (possibly remote) machine
     * @param remotePath path to the Actor
     * @return Optional ActorRef
     * @throws Exception if an error occurs e.g. timeout
     */
    public Optional<ActorRef> getActorRef(String remotePath) throws Exception {
        try {
            //akka.<protocol>://<actorsystemname>@<hostname>:<port>/<actor path>
            //ActorSelection selection =getContext().actorSelection("akka.tcp://app@10.0.0.1:2552/user/serviceA/worker");
            ActorSelection selection = system.actorSelection(remotePath);
            Timeout timeout = new Timeout(5000, TimeUnit.MILLISECONDS);
            scala.concurrent.Future<ActorRef> future = selection.resolveOne(timeout);
            ActorRef remote = Await.result(future, timeout.duration());
            return Optional.of(remote);
        } catch (ActorNotFound e) {
            log.info("No reference found for " + remotePath);
            return Optional.empty();
        }
    }

    /**
     * Adds a file for processing
     * @param f name of file to ingest
     */
    public void ingest(File f) {
        if (ingestor != null) {
            log.info("Sending " + f + " through pipeline");
            ROIResults newResult = new ROIResults(f);
            String hashedName = newResult.getName();
            results.put(hashedName, newResult);
            ingestor.tell(new FileMessage(f, FileMessage.genMetadata(null, "source", hashedName)),
                    system.guardian());
        } else {
            log.error("No ingestor configured - are you sure you called startup() ?");
        }
    }

    /**
     * Pops the ROI results for a given key
     * @param key name of results to return
     * @return list of ROIResults or null if key was not found
     */
    public Dataset getResults(String key) {
        Dataset res = null;
        try {
            if (results.containsKey(key)) {
                ROIResults result = results.remove(key);
                if (result != null) {
                    ROIResultGenerator resultGenerator = new ROIResultGenerator();
                    res = resultGenerator.generate(FileSniffer.read(result.getContents(), true),
                            result.getROI(),
                            ROIResultGenerator.REPTYPE.ALL);
                }
            }
        } catch (IOException ioe) {
            log.error("Unable to retrieve results for key=" + key + " error was: " + ioe);
        }
        return res;
    }

    /**
     * Writes all the results to disk.
     */
    public void getResults() {
        for (String k : results.keySet()) {
            Dataset res = getResults(k);
            if (res != null) {
                try {
                    String path = System.getProperty("user.dir");
                    if (config.hasPath("output.folder")) {
                        String pth = config.getString("output.folder");
                        if (pth != null && !pth.isEmpty()) {
                            path = pth;
                        }
                    }
                    File outFolder = new File(path);
                    File outFile = new File(outFolder, k + ".txt");
                    log.info("Writing results to " + outFile);
                    res.write(outFile);
                } catch (IOException e) {
                    log.error("Unable to write results, error was: " + e);
                }
            }
        }
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
     * Adds a file for processing
     * @param f pathname of file to ingest
     */
    public void ingest(String f) {
        ingest(new File(f));
    }

    /**
     * Demo application
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            log.error("No configuration file specified, exiting.");
            System.exit(0);
        }
        File configFile = new File(args[0]);
        if (!configFile.canRead()) {
            log.error("Unable to read configuration file '" + configFile, ",' exiting.");
            System.exit(1);
        }
        Config config = ConfigFactory.parseFile(configFile);
        Thresher thresher = new Thresher(config);
        boolean ready = thresher.init();
        if (ready) {
            for (int i=1; i<args.length; i++) {
                thresher.ingest(args[i]);
            }
        } else {
            log.error("Unable to construct pipeline, please check log files for further details.");
            thresher.shutdown(1);
        }
        log.info("Waiting for completion");
        TimeUnit.MINUTES.sleep(args.length - 1);
        thresher.getResults();
        thresher.shutdown(0);
    }
}
