package util;

import java.util.List;
import java.util.Random;

import static util.Const.DIM;
import static util.Const.FIELD_SIZE;

/**
 * Created by yutakase on 2016/11/29.
 */
public final class CalcUtil {
    private CalcUtil() {
    }

    /**
     * 乱数を生成し返す
     *
     * @param generator 乱数生成器
     * @param min       最小値
     * @param max       最大値
     * @return 乱数
     */
    public static double generateRandomDouble(Random generator, double min, double max) {
        return generator.nextDouble() * (max - min) + min;
    }
    /**
     * 乱数を生成し返す(正規分布)
     *
     * @param generator 乱数生成器
     * @param mu 平均
     * @param sd 標準偏差
     * @return 乱数
     */
    public static double generateRandomGaussian(Random generator, double mu, double sd) {
        return generator.nextGaussian() * sd + mu;
    }


    /**
     * 距離計算
     *
     * @param posA 座標A
     * @param posB 座標B
     * @return 距離
     */
    public static double calcDist(int[] posA, int[] posB) {
        return calcEuclidDist(posA, posB);
    }

    /**
     * ユークリッド距離計算
     *
     * @param posA 座標A
     * @param posB 座標B
     * @return 距離
     */
    private static double calcEuclidDist(int[] posA, int[] posB) {
        double[] distVector = calcDistVector(posA, posB);
        double dist = 0;
        for (int i = 0; i < DIM; i++) {
            dist += Math.pow(distVector[i], 2);
        }
        return Math.sqrt(dist);
    }

    /**
     * 座標Aから座標Bへの距離ベクトルを計算
     * トーラス世界
     *
     * @param posA 座標A
     * @param posB 座標B
     * @return 距離ベクトル
     */
    public static double[] calcDistVector(int[] posA, int[] posB) {
        double[] distVector = new double[DIM];
        for (int i = 0; i < DIM; i++) {
            distVector[i] = posB[i] - posA[i];
        }

        // 通常距離とトーラス距離を両方求め、短い方を距離とする
        double[] normalDist = new double[DIM];
        double[] torusDist = new double[DIM];
        for (int i = 0; i < DIM; i++) {
            normalDist[i] = Math.abs(distVector[i]);
            torusDist[i] = FIELD_SIZE - normalDist[i];
            // トーラス距離の方が短ければ距離を更新
            if (normalDist[i] > torusDist[i]) {
                // 方向ベクトルを計算
                distVector[i] = (distVector[i] > 0) ? -torusDist[i] : torusDist[i];
            }
        }
        return distVector;
    }

    /**
     * 2つのベクトルの内積を計算
     */
    public static double dotProduct(List<Double> vec1, List<Double> vec2) {
        double res = 0;
        for (int i = 0; i < vec1.size(); i++) {
            res += vec1.get(i) * vec2.get(i);
        }
        return res;
    }
}
