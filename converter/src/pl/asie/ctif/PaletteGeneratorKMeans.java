package pl.asie.ctif;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PaletteGeneratorKMeans {
    record Result(Color[] colors, double error) {
    }

    public class Worker implements Runnable {
        public Result result;

        @Override
        public void run() {
            result = generateKMeans();
        }
    }

    private final int colors;
    private final BufferedImage image;
    private final Color[] base;
    private final Random random = new Random();
    private final Map<float[], Integer> pointsWeight = new HashMap<>();
    private final float[][] centroids;
    private final Map<float[], Double> knownBestError = new HashMap<>();
    private final Map<float[], Integer> knownBestCentroid = new HashMap<>();

    public PaletteGeneratorKMeans(BufferedImage image, Color[] base, int colors, int samplingRes) {
        this.colors = colors;
        this.image = image;
        this.base = base;
        this.centroids = new float[base.length][];

        Map<Integer, float[]> pointsAdded = new HashMap<>();
        if (samplingRes > 0) {
            int maximum = samplingRes;
            float stepX = (float) image.getWidth() / maximum;
            float stepY = (float) image.getHeight() / maximum;
            int stepIX = (int) Math.ceil(stepX);
            int stepIY = (int) Math.ceil(stepY);
            for (int jy = 0; jy < maximum; jy++) {
                for (int jx = 0; jx < maximum * 2; jx++) {
                    int i = image.getRGB(random.nextInt(stepIX) + (int) ((jx % maximum) * stepX), random.nextInt(stepIY) + (int) (jy * stepY));
                    if (!pointsAdded.containsKey(i)) {
                        float[] key = Main.COLORSPACE.fromRGB(i);
                        pointsAdded.put(i, key);
                        pointsWeight.put(key, 1);
                    } else {
                        pointsWeight.put(pointsAdded.get(i), pointsWeight.get(pointsAdded.get(i)) + 1);
                    }
                }
            }
        } else {
            if (Main.OPTIMIZATION_LEVEL >= 3 && (image.getWidth() * image.getHeight() >= 4096)) {
                for (int jy = 0; jy < image.getHeight(); jy += 4) {
                    int my = Math.min(4, image.getHeight() - jy);
                    for (int jx = 0; jx < image.getWidth(); jx += 4) {
                        int mx = Math.min(4, image.getWidth() - jx);

                        int i = image.getRGB(random.nextInt(mx) + jx, random.nextInt(my) + jy);
                        if (!pointsAdded.containsKey(i)) {
                            float[] key = Main.COLORSPACE.fromRGB(i);
                            pointsAdded.put(i, key);
                            pointsWeight.put(key, 1);
                        } else {
                            pointsWeight.put(pointsAdded.get(i), pointsWeight.get(pointsAdded.get(i)) + 1);
                        }
                    }
                }
            } else {
                for (int i : Utils.getRGB(image)) {
                    if (!pointsAdded.containsKey(i)) {
                        float[] key = Main.COLORSPACE.fromRGB(i);
                        pointsAdded.put(i, key);
                        pointsWeight.put(key, 1);
                    } else {
                        pointsWeight.put(pointsAdded.get(i), pointsWeight.get(pointsAdded.get(i)) + 1);
                    }
                }
            }
        }

        for (int i = colors; i < centroids.length; i++) {
            centroids[i] = Main.COLORSPACE.fromRGB(base[i].getRGB());
        }

        for (Map.Entry<float[], Integer> weight : pointsWeight.entrySet()) {
            double bestError = Float.MAX_VALUE;
            int bestCentroid = 0;
            for (int i = colors; i < centroids.length; i++) {
                double err = Utils.getColorDistanceSq(weight.getKey(), centroids[i]);
                if (err < bestError) {
                    bestError = err;
                    bestCentroid = i;
                    if (err == 0) break;
                }
            }
            knownBestError.put(weight.getKey(), bestError);
            knownBestCentroid.put(weight.getKey(), bestCentroid);
        }
    }

    public Color[] generate(int threads) {
        Result bestResult = null;
        Worker[] workers = new Worker[20 / (Main.OPTIMIZATION_LEVEL + 1)];
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker();
            executorService.submit(workers[i]);
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < workers.length; i++) {
            Result result = workers[i].result;
            if (Main.DEBUG) {
                System.out.println("Palette generator worker #" + (i + 1) + " error = " + result.error);
            }
            if (bestResult == null || bestResult.error > result.error) {
                bestResult = result;
            }
        }

        if (Main.DEBUG) {
            System.out.println("Palette generator error = " + bestResult.error);
        }

        return bestResult.colors;
    }

    private Result generateKMeans() {
        for (int i = 0; i < colors; i++) {
            centroids[i] = Main.COLORSPACE.fromRGB(image.getRGB(random.nextInt(image.getWidth()), random.nextInt(image.getHeight())));
        }

        double totalError = 0;

        for (int reps = 0; reps < 128; reps++) {
            float[][] means = new float[centroids.length][3];
            int[] meanDivs = new int[centroids.length];

            totalError = 0;
            for (Map.Entry<float[], Integer> weight : pointsWeight.entrySet()) {
                double bestError = knownBestError.get(weight.getKey());
                int bestCentroid = knownBestCentroid.get(weight.getKey());
                int mul = weight.getValue();

                for (int i = 0; i < colors; i++) {
                    double err = Utils.getColorDistanceSq(weight.getKey(), centroids[i]);
                    if (err < bestError) {
                        bestError = err;
                        bestCentroid = i;
                        if (err == 0) break;
                    }
                }

                totalError += bestError * mul;
                means[bestCentroid][0] += weight.getKey()[0] * mul;
                means[bestCentroid][1] += weight.getKey()[1] * mul;
                means[bestCentroid][2] += weight.getKey()[2] * mul;
                meanDivs[bestCentroid] += mul;
            }

            boolean changed = false;
            for (int i = 0; i < colors; i++) {
                if (meanDivs[i] > 0) {
                    float n0 = means[i][0] / meanDivs[i];
                    float n1 = means[i][1] / meanDivs[i];
                    float n2 = means[i][2] / meanDivs[i];
                    if (n0 != centroids[i][0] || n1 != centroids[i][1] || n2 != centroids[i][2]) {
                        centroids[i][0] = n0;
                        centroids[i][1] = n1;
                        centroids[i][2] = n2;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }

        Color[] out = Arrays.copyOf(base, base.length);
        for (int k = 0; k < colors; k++) {
            out[k] = new Color(Main.COLORSPACE.toRGB(centroids[k]) | 0xFF000000);
        }
        return new Result(out, totalError);
    }
}
