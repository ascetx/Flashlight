package info.ascetx.flashlight.helper;

/**
 * Created by JAYESH on 14-08-2018.
 */

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetails;

/**
 * A model for SkusAdapter's row
 */
public class SkuRowData {
    private String sku, title, price, description;
    private int type;
    private String billingType;

    public SkuRowData(SkuDetails details,
                      @BillingClient.SkuType String billingType) {
        this.sku = details.getSku();
        this.title = details.getTitle();
        this.price = details.getPrice();
        this.description = details.getDescription();
//        this.type = rowType;
        this.billingType = billingType;
    }

    public SkuRowData(String title) {
        this.title = title;
//        this.type = SkusAdapter.TYPE_HEADER;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public int getRowType() {
        return type;
    }

    public @BillingClient.SkuType
    String getSkuType() {
        return billingType;
    }
}

