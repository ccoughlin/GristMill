/*
 * com.emphysic.myriad.gristmill.demo.ROIResultGenerator
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

import com.emphysic.myriad.core.data.io.Dataset;
import com.emphysic.myriad.core.data.ops.math.Stats;
import com.emphysic.myriad.core.data.roi.ROI;
import com.emphysic.myriad.core.data.util.DatasetUtils;
import com.emphysic.myriad.network.messages.ImmutableMessage;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ROIResultGenerator - combines ROI with the source data to create a visual
 * presentation of the results.  Based on Myriad Trainer's ROIResultGenerator
 * com.emphysic.myriad.ui.desktop.controllers.ROIResultGenerator
 * @author chris
 */
@Slf4j
public class ROIResultGenerator {
    /**
     * How ROI are represented in the original data.
     * ALL - every ROI bounding box is drawn in the data
     *
     * ENHANCE - ROI region amplitudes are increased.  When ROI overlap it's a
     * visual representation of confidence.
     *
     * ALL_ENHANCE - draw every bounding box and enhance interiors
     *
     * UNION - ROI that intersect are replaced with their union, i.e. a single
     * bounding box that contains both ROI.  Tends to make large ROI.
     *
     * INTERSECTION - ROI that intersect are replaced with their intersection,
     * i.e. a single bounding box that covers their overlap area.  Tends to make
     * small ROI.
     *
     * OVERLAP - ROI that overlap by more than a specified amount are replaced
     * by their intersection.
     */
    public enum REPTYPE {
        ALL,
        ENHANCE,
        ALL_ENHANCE,
        UNION,
        INTERSECTION,
        OVERLAP,
        NON_MAX_SUPPRESS,
        NMS_ENHANCE
    };

    /**
     * Enhancement factor - ROI region amplitudes are multiplied by this value
     */
    private double enhanceFactor;
    /**
     * Overlap threshold - overlap area / area of bounding box.  Overlaps greater
     * than this threshold are replaced with their intersection.
     */
    private double overlapThreshold;

    /**
     * Creates a new ROIResultGenerator
     * @param enhanceFactor ROI regions multiplication factor
     * @param overlapThreshold  threshold for considering two regions as overlapping
     */
    public ROIResultGenerator(double enhanceFactor, double overlapThreshold) {
        this.enhanceFactor = enhanceFactor;
        this.overlapThreshold = overlapThreshold;
    }

    /**
     * Creates a new ROIResultGenerator with an enhancement factor of 1.5 and
     * an overlap threshold of 0.25.
     */
    public ROIResultGenerator() {
        this(1.5, 0.25);
    }

    /**
     * Generate a visual representation of ROI results.
     * @param input original source data
     * @param regions list of ROI found in the data
     * @param representationType type of representation to use
     * @return a copy of the source data with its ROI added
     */
    public Dataset generate(Dataset input, List<ROI> regions, REPTYPE representationType) {
        Dataset res;
        switch (representationType) {
            case ENHANCE:
                res = enhance(input, regions);
                break;
            case UNION:
            case INTERSECTION:
            case OVERLAP:
                res = merge(input, regions, representationType);
                break;
            case NMS_ENHANCE:
            case NON_MAX_SUPPRESS:
                List<ROI> suppressed = nonMaxSuppression(
                        regions,
                        input.getWidth(),
                        input.getHeight()
                );
                res = all(input, suppressed);
                if (representationType == REPTYPE.NMS_ENHANCE) {
                    res = enhance(res, suppressed);
                }
                break;
            case ALL:
            case ALL_ENHANCE:
            default:
                res = all(input, regions);
                if (representationType == REPTYPE.ALL_ENHANCE) {
                    res = enhance(res, regions);
                }
                break;
        }
        return res;
    }

    /**
     * Generate a visual representation of ROI results.  Each ROI's bounding box
     * is drawn and the interiors are emphasized.
     * @param input original source data
     * @param regions list of ROI found in the data
     * @return a copy of the source data with its ROI added
     */
    public Dataset generate(Dataset input, List<ROI> regions) {
        return generate(input, regions, REPTYPE.ALL_ENHANCE);
    }

    /**
     * Draws each ROI's bounding box on the source data.
     * @param input original source data
     * @param regions list of ROI found in the source data
     * @return copy of original data with ROI drawn
     */
    public Dataset all(Dataset input, List<ROI> regions) {
        Dataset res = new Dataset(input);
        Double max = Stats.max(input);
        regions.stream().map((r) -> new Coords(r, input.getWidth(), input.getHeight())).map((roiBB) -> {
            // Highlight the ROI region - bright border around bounding box
            for (int i=roiBB.lowX; i<=roiBB.highX; i++) {
                res.set(i, roiBB.lowY, max);
                res.set(i, roiBB.highY, max);
            }
            return roiBB;
        }).forEach((roiBB) -> {
            for (int i=roiBB.lowY; i<=roiBB.highY; i++) {
                res.set(roiBB.lowX, i, max);
                res.set(roiBB.highX, i, max);
            }
        });
        return res;
    }

    /**
     * Represent ROI in source data by emphasizing their contents.  Points in the
     * source data within an ROI are multiplied by an enhancement factor; result
     * is a kind of visual indication of confidence (more overlapping ROI result
     * in higher amplitudes).
     * @param input original source data
     * @param regions ROI found in the data
     * @return copy of input data with enhanced ROI regions
     */
    public Dataset enhance(Dataset input, List<ROI> regions) {
        Dataset res = new Dataset(input);
        Double max = Stats.max(input);
        regions.stream().map((region) -> new Coords(region, input.getWidth(), input.getHeight())).forEach((coords) -> {
            for (int i=coords.lowX; i<coords.highX; i++) {
                for (int j=coords.lowY; j<coords.highY; j++) {
                    res.set(i, j, res.get(i, j) * enhanceFactor);
                }
            }
        });
        return res;
    }

    /**
     * Returns the intersection between two rectangles
     * @param o1 first rectangle
     * @param o2 second rectangle
     * @return the intersection between o1 and o2, the outer rectangle if one completely contains the other, or null if no intersection.
     */
    private Rectangle intersect(Rectangle o1, Rectangle o2) {
        if (o1.intersects(o2)) {
            return o1.intersection(o2);
        } else if (o1.contains(o2)) {
            return o1;
        } else if (o2.contains(o1)) {
            return o2;
        }
        return null;
    }

    /**
     * Returns the union between two rectangles if they intersect.
     * @param o1 first rectangle
     * @param o2 second rectangle
     * @return the union between the two, the outer if one completely contains the other, or null if no intersection.
     */
    private Rectangle union(Rectangle o1, Rectangle o2) {
        if (o1.intersects(o2)) {
            return o1.union(o2);
        } else if (o1.contains(o2)) {
            return o1;
        } else if (o2.contains(o1)) {
            return o2;
        }
        return null;
    }

    /**
     * Returns the intersection between two rectangles if the ratio of the overlap
     * area to the area of the first rectangle exceeds a threshold.
     * @param o1 first rectangle
     * @param o2 second rectangle
     * @return intersection between the two, the outer if one completely contains the other, or null if no intersection.
     */
    private Rectangle overlap(Rectangle o1, Rectangle o2) {
        Rectangle intersection = intersect(o1, o2);
        Rectangle result = null;
        if (intersection != null) {
            double overlap = (intersection.getWidth() * intersection.getHeight()) /
                    (o1.getWidth() * o1.getHeight());
            if (overlap > overlapThreshold) {
                result = intersection;
            }
        }
        return result;
    }

    /**
     * Merge ROIs into fewer regions, then visually indicate on source data.
     * @param input original source data
     * @param regions list of ROI found in the source data
     * @param merge the type of merge operation to perform
     * @return a copy of the original source data with the merged ROI visually indicated
     */
    public Dataset merge(Dataset input, List<ROI> regions, REPTYPE merge) {
        Dataset res = new Dataset(input);
        Double max = Stats.max(input);
        List<Rectangle> boxes = new ArrayList<>();
        // TODO: consider sorting ROI here for true non-max suppression
        regions.stream().map((region) -> new Coords(region, res.getWidth(), res.getHeight())).map((bb) -> new Rectangle(bb.lowX,
                bb.lowY,
                bb.highX - bb.lowX,
                bb.highY - bb.lowY)).forEach((Rectangle newBB) -> {
            boolean newRegion = true;
            for (int i = 0; i < boxes.size(); i++) {
                Rectangle current = boxes.get(i);
                Rectangle overlap = null;
                switch (merge) {
                    case UNION:
                        overlap = union(current, newBB);
                        break;
                    case INTERSECTION:
                        overlap = intersect(current, newBB);
                        break;
                    case OVERLAP:
                        overlap = overlap(current, newBB);
                        break;
                }
                if (overlap != null) {
                    boxes.set(i, overlap);
                    newRegion = false;
                } else {
                    newRegion = true;
                }
            }
            if (newRegion) {
                boxes.add(newBB);
            }
        });
        boxes.stream().forEach((box) -> {
            int left = (int) box.getX();
            int top = (int) box.getY();
            int right = (int) (box.getX() + box.getWidth());
            int bottom = (int) (box.getY() + box.getHeight());
            for (int x = left; x < right; x++) {
                res.set(x, top, max);
                res.set(x, bottom, max);
            }
            for (int y = top; y < bottom; y++) {
                res.set(left, y, max);
                res.set(right, y, max);
            }
        });
        return res;
    }

    /**
     * Non-Maximum Suppression (NMS) of ROI: recursively eliminates ROI that
     * share significant overlap
     * @param regions original list of ROI
     * @param width width of original Dataset
     * @param height height of original Dataset
     * @return a new list of ROI with overlapping regions dropped
     */
    public List<ROI> nonMaxSuppression(List<ROI> regions, int width, int height) {
        List<ROI> boxes = new ArrayList<>();
        List<Integer> bottom = new ArrayList();
        List<Double> area = new ArrayList();
        regions.stream().map((r) -> new Coords(r, width, height)).map((coords) -> {
            bottom.add(coords.highY);
            return coords;
        }).forEach((coords) -> {
            area.add((double)(coords.highX - coords.lowX) * (double)(coords.highY - coords.lowY));
        });
        ArrayIndexComparator<Integer> comparator = new ArrayIndexComparator<>(bottom);
        List<Integer> idxs = comparator.createIndexArray();
        List<Integer> picks = new ArrayList<>();
        Collections.sort(idxs, comparator);
        bottom.clear();
        while (idxs.size() > 0) {
            int last = idxs.size() - 1;
            Integer i = idxs.remove(last);
            ROI r1 = regions.get(i);
            Coords c1 = new Coords(r1, width, height);
            picks.add(i);
            List<Integer> suppress = new ArrayList();
            for (int pos=0; pos<last; pos++) {
                Integer j = idxs.get(pos);
                ROI r2 = regions.get(j);
                Coords c2 = new Coords(r2, width, height);
                int xx1 = Math.max(c1.lowX, c2.lowX);
                int yy1 = Math.max(c1.lowY, c2.lowY);
                int xx2 = Math.min(c1.highX, c2.highX);
                int yy2 = Math.min(c1.highY, c2.highY);
                int w = Math.max(0, xx2 - xx1);
                int h = Math.max(0, yy2 - yy1);
                double overlap = ((double)(w * h)) / area.get(j);
                if (overlap > overlapThreshold) {
                    suppress.add(pos);
                }
            }
            suppress.stream().forEach((toRemove) -> {
                idxs.remove(toRemove);
            });
        }
        picks.stream().forEach((pick) -> {
            boxes.add(regions.get(pick));
        });
        return boxes;
    }

    /**
     * @return the enhanceFactor
     */
    public double getEnhanceFactor() {
        return enhanceFactor;
    }

    /**
     * @param enhanceFactor the enhanceFactor to set
     */
    public void setEnhanceFactor(double enhanceFactor) {
        this.enhanceFactor = enhanceFactor;
    }

    /**
     * @return the overlapThreshold
     */
    public double getOverlapThreshold() {
        return overlapThreshold;
    }

    /**
     * @param overlapThreshold the overlapThreshold to set
     */
    public void setOverlapThreshold(double overlapThreshold) {
        this.overlapThreshold = overlapThreshold;
    }

    /**
     * Coords - convenience class for calculating and containing 2D ROI
     * coordinates.
     */
    public class Coords {
        /**
         * X-coordinate of upper left corner
         */
        public int lowX;
        /**
         * Y-coordinate of upper left corner
         */
        public int lowY;
        /**
         * X-coordinate of lower right corner
         */
        public int highX;
        /**
         * Y-coordinate of lower right corner
         */
        public int highY;

        /**
         * Metadata key for sliding window parameters
         */
        private final String WINKEY = "window";
        /**
         * Regular expression for finding sliding window position relative to original data
         */
        private final String winRE = "(xoff)([0-9]+)(yoff)([0-9]+)(w)([0-9]+)(h)([0-9]+)";
        /**
         * Compiled regex for parsing sliding window position
         */
        private final Pattern winpat = Pattern.compile(winRE);

        /**
         * Metadata key for pyramid parameters
         */
        private final String PYRKEY = "pyramid";
        /**
         * Regular expression for parsing pyramid configuration
         */
        private final String pyrRE = "(pscale)([0-9]+)(pwsize)([0-9]+)(pstep)([0-9]+)";
        /**
         * Compiled regex for parsing pyramid parameters
         */
        private final Pattern pyrpat = Pattern.compile(pyrRE);

        /**
         * Constructor
         * @param r Region of Interest
         * @param width width of original data
         * @param height height of original data
         */
        public Coords(ROI r, int width, int height) {
            int sizeFactor = 0;
            int scalingFactor = 0;
            int xoff = 0;
            int yoff = 0;
            int w = 0;
            int h = 0;
            String meta = r.getMetadata();
            Map<String, String> md = ImmutableMessage.getMetadata(meta);
            if (md.containsKey(PYRKEY)) {
                Matcher m = pyrpat.matcher(md.get(PYRKEY));
                if (m.matches()) {
                    scalingFactor = Integer.parseInt(m.group(2));
                    sizeFactor = Integer.parseInt(m.group(6));
                }
            }
            if (md.containsKey(WINKEY)) {
                Matcher m = winpat.matcher(md.get(WINKEY));
                if (m.matches()) {
                    xoff = Integer.parseInt(m.group(2));
                    yoff = Integer.parseInt(m.group(4));
                    w = Integer.parseInt(m.group(6));
                    h = Integer.parseInt(m.group(8));
                }
            }
            int scaler = scalingFactor * sizeFactor;
            lowX = DatasetUtils.safeIdx(scaler * xoff, width);
            lowY = DatasetUtils.safeIdx(scaler * yoff, height);
            highX = DatasetUtils.safeIdx(lowX + scaler * w, width);
            highY = DatasetUtils.safeIdx(lowY + scaler * h, height);
        }
    }

    /**
     * ArrayIndexComparator - used to determine order of indices in an array
     * if a sort was performed, analogous to NumPy's argsort function.
     * @param <T> type of element
     */
    public class ArrayIndexComparator<T extends Comparable> implements Comparator<Integer> {

        private final List<T> array;

        public ArrayIndexComparator(List<T> array) {
            this.array = array;
        }

        public List<Integer> createIndexArray() {
            List<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                indexes.add(i);
            }
            return indexes;
        }

        @Override
        public int compare(Integer index1, Integer index2) {
            // Autounbox from Integer to int to use as array indexes
            return array.get(index1).compareTo(array.get(index2));
        }
    }
}
