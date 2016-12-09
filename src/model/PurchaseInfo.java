package model;

import java.io.Serializable;

/**
 * サービス購入の情報
 * <p>
 * Created by yutakase on 2016/12/04.
 */
public class PurchaseInfo implements Serializable {
    // 提供ActorのID
    private int providerId;
    // 購入によって生じた利得
    private double profit;
    // 支払った金額
    private int price;

    public PurchaseInfo(int providerId, double profit, int price) {
        this.providerId = providerId;
        this.profit = profit;
        this.price = price;
    }

    public int getProviderId() {
        return this.providerId;
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
    }

    public double getProfit() {
        return this.profit;
    }

    public void setProfit(double profit) {
        this.profit = profit;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
