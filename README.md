GristMill
==
GristMill is a Java application based on [Emphysic's Myriad Toolkit](https://gitlab.com/ccoughlin/datareader), designed to find Regions Of Interest (ROI) in sensor data in a distributed processing system.

GristMill designed to run unattended on a server, waiting for [data](http://myrdocs.azurewebsites.net/api/com/emphysic/myriad/network/messages/DatasetMessage.html).  When data are received, GristMill automatically looks for ROI by subsampling and scanning across the data and sending the results to a machine learning model trained to recognize a particular type of ROI.  It's more or less the same procedure shown in this single-process [Myriad sample](https://emphysic.com/myriad/sample-code/roi-detection-pipeline/), but uses [Myriad's concurrency tools](https://emphysic.com/myriad/faq/#concurrency) to build a system that can run across multiple CPUs, in a cluster, etc.
 
Building
==
You'll need to [build the Myriad toolkit](http://myrdocs.azurewebsites.net/install/).

Once that's taken care of, `mvn package` should be all that's required.

Running
==
After you've built the JAR it runs just like every other JAR, but you'll need to provide it with the full path and filename of a configuration file.  The `gristmill.conf` file in the root project folder is a good place to start, just edit as you see fit.

Don't happen to have a trained ROI model handy?  Not a problem, just grab a copy of the [quick start bundle](http://myrdocs.azurewebsites.net/myriad_samples.zip) for Myriad Desktop.  Inside you'll find a machine learning model that has been trained to find indications of damage from ultrasonic scans of important parts.

Don't happen to have ultrasonic scans of important parts handy?  The bundle also just happens to contain some samples for you to try out.
 
Don't particularly give a cracked iron pot about structural integrity of structurally-important parts?  Avail yourself of [Myriad Trainer](https://gitlab.com/ccoughlin/MyriadTrainer), which lets you build and save your models with your own data.  GristMill wouldn't be a very good Region Of Interest finder if the Regions weren't Interesting, after all.

Next, send it some data!  You can use any Myriad-based application to send it data.  If you don't configure a [ReporterPool](http://myrdocs.azurewebsites.net/api/com/emphysic/myriad/network/ReporterActorPool.html), GristMill will default to logging the ROI it finds.

About Myriad
==
Myriad is a library written in Java that provides tools for image / signal processing, machine learning, and fault-tolerant distributed computing. Its primary purpose is to assist with the development of large-scale Region Of Interest (ROI) detection applications by providing the parts required to train a model to detect ROI in large datasets.  

Example applications include face detection, pedestrian tracking, and anomalous signal identification.  Myriad’s [original application for NASA](http://sbir.nasa.gov/SBIR/abstracts/16/sbir/phase1/SBIR-16-1-H13.01-8360.html) was to detect indications of structural damage in [nondestructive evaluation](https://www.asnt.org/MinorSiteSections/AboutASNT/Intro-to-NDT) data.

For more details on Myriad, visit our [Myriad page](https://emphysic.com/myriad/) to get [code samples](https://emphysic.com/myriad/sample-code/), [video demonstrations](https://emphysic.com/myriad/downloads/), and [tutorials](http://myrdocs.azurewebsites.net/).

License
==
[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)

Sister Projects
==
* [Myriad Trainer](https://gitlab.com/ccoughlin/MyriadTrainer) is a cross-platform GUI for training and testing machine learning models
* [Myriad Desktop](https://gitlab.com/ccoughlin/MyriadDesktop) is a cross-platform GUI for constructing and running ROI processing pipelines

More On Myriad
==
* [Installation](http://myrdocs.azurewebsites.net/install/)
* [Frequently Asked Questions](https://emphysic.com/myriad/faq/)
* [API Documentation](http://myrdocs.azurewebsites.net/api/)
* [About Emphysic](https://emphysic.com/about/)

Contact Information
==
Questions? Comments? Suggestions?  [Drop us a line.](https://emphysic.com/contact-us/)
