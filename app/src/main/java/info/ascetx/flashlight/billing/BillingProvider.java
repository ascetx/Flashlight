package info.ascetx.flashlight.billing;

/**
 * Created by JAYESH on 14-08-2018.
 */

/**
 * An interface that provides an access to BillingLibrary methods
 */
public interface BillingProvider {
    BillingManager getBillingManager();
    boolean isPremiumPurchased();
//    boolean isGoldMonthlySubscribed();
//    boolean isTankFull();
//    boolean isGoldYearlySubscribed();
}