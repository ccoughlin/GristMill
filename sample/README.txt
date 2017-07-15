Introduction
============
We've prepared a demonstration package to help users get a feel for the workflow in Myriad Desktop.  This package contains a Myriad model that has been trained to detect indications of structural damage in C-scan data, as well as some sample data to try it out.

Contents
========
* The models/ subfolder contains pre-trained defect detection models:
    * The model sobel_pa_model has been trained to detect flaw signals in C-scan indices.  It expects to receive 15x15 element "windows" and performs Sobel edge detection on the data prior to characterizing it.
* The test/ subfolder contains slices from ultrasonic C-scans exported as CSV files:
	* 140, 155, 172, 190 and 210 are select indices from a single immersion tank scan of a calibration standard.  These files were used to train the model sobel_pa_model.myr in the models/ subfolder.  
    * 10mhzUTidx350 is a C-scan index taken from an ultrasonic scan of a different calibration standard.  The model sobel_pa_model was not trained on this file.

Usage
=====
To use the sobel_pa_model to search for flaw signals in the sample files, please follow these steps.

The Myriad library must be installed prior to using the Desktop defect detection tool.  Please refer to https://gitlab.com/ccoughlin/myriad-docs/blob/master/docs/install.md for details on how to build and install Myriad and Desktop.

Copy the models/ and test/ subfolders to a convenient location on the computer.  

Start the Myriad Desktop tool by double-clicking its JAR file, or by running "java -jar <path>/desktop-1.0-SNAPSHOT.jar" at the command line.

In the "Data" tab of Desktop, press the '+' button and add some or all of the sample files found in the test/ subfolder.


In the "Sliding Window" tab, ensure that the Window Size is 15 x 15 elements as this is the size of input the sobel_pa_model is expecting.  To perform an exhaustive search of input files, reduce the Step Size to 1.  To perform a faster search for flaws, increase the Step Size to 5 or greater.

In the "Region Of Interest Detection" tab, select "Internal" from the ROI Detector drop-down box and browse to the location of the sobel_pa_model file by pressing the "Choose..." button.

(Optional) In the "Scale Space" tab, increase or decrease the Blur Radius.  In general the smaller the blur radius the more flaws will be detected, as fewer artifacts are smoothed out at progressive scale reductions.  Increasing the blur radius tends to reduce the number of flaws detected as more artifacts are smoothed out as the input data is scaled.

(Optional) Adjust the "Number Of Workers" fields in each of the tabs.  This is the number of workers that are available in the worker pool for the given stage of processing.  In a production environment it will be necessary to experiment with these values to optimize performance; but for the purposes of this demonstration a suggested setup would be the following.

* Data ingestion workers = 1
* Scale Space workers = 2
* Data Preprocessing workers = 4
* Sliding Window workers = 8
* Region Of Interest Detection workers = 16
* Reporting workers = 4

Press the "Update Pipeline" button to configure your processing pipeline.  Desktop should respond with a message indicating the pipeline is ready to process data.  Switch to the "Reporting" tab to watch flaws as they are received, then press the "Run!" button.  Each of the files specified in the Data tab will be read and scanned for flaws; when a flaw is found it will be added to the list in the Reporting tab.  Double-clicking on any flaw in the tab will pop-up a plot of the data as seen by the model, i.e. in this case a 15x15 element data subset that has had Sobel edge detection applied.

To compile all the flaws into a single presentation, switch to the "Results" tab and press "Compile Results."  For each input file, if one or more flaws were found Desktop will add them to the list.  Double-clicking on any entry in the Results list will display the original input file with all the detected flaw signals; Desktop offers several different presentation methods.

To re-run the flaw detection, clear both the "Results" and the "Reporting" lists and press the Run! button.

Further Information
===================
A more detailed discussion of Desktop including a video that demonstrates the same procedure outlined here is available from https://gitlab.com/ccoughlin/myriad-docs/blob/master/docs/desktop.md .

Get the latest news on Desktop and the Myriad project from https://emphysic.com/myriad/ .

Get in touch with us: https://emphysic.com/contact-us/ .
