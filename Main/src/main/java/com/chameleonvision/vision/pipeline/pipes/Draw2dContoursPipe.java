package com.chameleonvision.vision.pipeline.pipes;

import com.chameleonvision.vision.camera.CaptureStaticProperties;
import com.chameleonvision.util.Helpers;
import com.chameleonvision.vision.image.CaptureProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Draw2dContoursPipe implements Pipe<Pair<Mat, List<RotatedRect>>, Mat> {

    private final Draw2dContoursSettings settings;
    private CaptureStaticProperties camProps;

    private Mat processBuffer = new Mat();
    private Mat outputMat = new Mat();

    public Draw2dContoursPipe(Draw2dContoursSettings settings, CaptureStaticProperties camProps) {
        this.settings = settings;
        this.camProps = camProps;
    }

    public void setConfig(CaptureStaticProperties captureProps) {
        camProps = captureProps;
    }

    @Override
    public Pair<Mat, Long> run(Pair<Mat, List<RotatedRect>> input) {
        long processStartNanos = System.nanoTime();

        if (settings.showCrosshair || settings.showCentroid || settings.showMaximumBox || settings.showRotatedBox) {
            input.getLeft().copyTo(processBuffer);

            if (input.getRight().size() > 0) {
                for (RotatedRect r : input.getRight()) {
                    if (r == null) continue;

                    List<MatOfPoint> drawnContour = new ArrayList<>();
                    Point[] vertices = new Point[4];
                    r.points(vertices);
                    MatOfPoint contour = new MatOfPoint(vertices);
                    drawnContour.add(contour);

                    if (settings.showCentroid) {
                        Imgproc.circle(processBuffer, r.center, 3, Helpers.colorToScalar(settings.centroidColor));
                    }

                    if (settings.showRotatedBox) {
                        Imgproc.drawContours(processBuffer, drawnContour, 0, Helpers.colorToScalar(settings.rotatedBoxColor), settings.boxOutlineSize);
                    }

                    if (settings.showMaximumBox) {
                        Rect box = Imgproc.boundingRect(contour);
                        Imgproc.rectangle(processBuffer, new Point(box.x, box.y), new Point((box.x + box.width), (box.y + box.height)), Helpers.colorToScalar(settings.maximumBoxColor), settings.boxOutlineSize);
                    }
                }
            }

            if (settings.showCrosshair) {
                Point xMax = new Point(camProps.centerX + 10, camProps.centerY);
                Point xMin = new Point(camProps.centerX - 10, camProps.centerY);
                Point yMax = new Point(camProps.centerX, camProps.centerY + 10);
                Point yMin = new Point(camProps.centerX, camProps.centerY - 10);
                Imgproc.line(processBuffer, xMax, xMin, Helpers.colorToScalar(settings.crosshairColor), 2);
                Imgproc.line(processBuffer, yMax, yMin, Helpers.colorToScalar(settings.crosshairColor), 2);
            }

            processBuffer.copyTo(outputMat);
            processBuffer.release();
        } else {
            input.getLeft().copyTo(outputMat);
        }

        long processTime = System.nanoTime() - processStartNanos;
        return Pair.of(outputMat, processTime);
    }

    public static class Draw2dContoursSettings {
        public boolean showCentroid = false;
        public boolean showCrosshair = false;
        public int boxOutlineSize = 0;
        public boolean showRotatedBox = false;
        public boolean showMaximumBox = false;
        public Color centroidColor = Color.GREEN;
        public Color crosshairColor = Color.GREEN;
        public Color rotatedBoxColor = Color.BLUE;
        public Color maximumBoxColor = Color.RED;
    }
}