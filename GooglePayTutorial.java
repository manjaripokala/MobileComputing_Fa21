

/* 
How to integrate Google Pay, with your online business
Step 1 - Define your Google Pay API version.
Step 2 - Request a payment token for your payment provider.
Step 3 - Define supported payment card networks.
Step 4 - Describe your allowed payment methods.
Step 5 - Create a PaymentsClient instance.
Step 6 - Determine readiness to pay with the Google Pay API.
Step 7 - Create a PaymentDataRequest object.
Step 8 - Register event handler for user gesture.
Step 9 - Handle the response object.
Step 10 - Put it all together. 
*/

//Step 1 - Define your Google Pay API version.
private static JSONObject getBaseRequest() throws JSONException {
    return new JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0);
  }
    
//Step 2 - Request a payment token for your payment provider.  
private static JSONObject getGatewayTokenizationSpecification() throws JSONException {
    return new JSONObject() {{
      put("type", "PAYMENT_GATEWAY");
      put("parameters", new JSONObject() {{
        put("gateway", "example");
        put("gatewayMerchantId", "exampleGatewayMerchantId");
      }});
    }};
  }
  
 //Step 3 - Define supported payment card networks. 
  private static JSONArray getAllowedCardNetworks() {
  return new JSONArray()
      .put("AMEX")
      .put("DISCOVER")
      .put("INTERAC")
      .put("JCB")
      .put("MASTERCARD")
      .put("VISA");
}
private static JSONArray getAllowedCardAuthMethods() {
  return new JSONArray()
      .put("PAN_ONLY")
      .put("CRYPTOGRAM_3DS");
}



//Step 4 - Describe your allowed payment methods.
private static JSONObject getBaseCardPaymentMethod() throws JSONException {
    JSONObject cardPaymentMethod = new JSONObject();
    cardPaymentMethod.put("type", "CARD");

    JSONObject parameters = new JSONObject();
    parameters.put("allowedAuthMethods", getAllowedCardAuthMethods());
    parameters.put("allowedCardNetworks", getAllowedCardNetworks());
    // Optionally, you can add billing address/phone number associated with a CARD payment method.
    parameters.put("billingAddressRequired", true);

    JSONObject billingAddressParameters = new JSONObject();
    billingAddressParameters.put("format", "FULL");

    parameters.put("billingAddressParameters", billingAddressParameters);

    cardPaymentMethod.put("parameters", parameters);

    return cardPaymentMethod;
  }
private static JSONObject getCardPaymentMethod() throws JSONException {
    JSONObject cardPaymentMethod = getBaseCardPaymentMethod();
    cardPaymentMethod.put("tokenizationSpecification", getGatewayTokenizationSpecification());

    return cardPaymentMethod;
  }





//Step 5 - Create a PaymentsClient instance.
public static PaymentsClient createPaymentsClient(Activity activity) {
    Wallet.WalletOptions walletOptions =
        new Wallet.WalletOptions.Builder().setEnvironment(Constants.PAYMENTS_ENVIRONMENT).build();
    return Wallet.getPaymentsClient(activity, walletOptions);
  }



//Step 6 - Determine readiness to pay with the Google Pay API.
public static Optional<JSONObject> getIsReadyToPayRequest() {
    try {
      JSONObject isReadyToPayRequest = getBaseRequest();
      isReadyToPayRequest.put(
          "allowedPaymentMethods", new JSONArray().put(getBaseCardPaymentMethod()));

      return Optional.of(isReadyToPayRequest);

    } catch (JSONException e) {
      return Optional.empty();
    }
  }
  private void possiblyShowGooglePayButton() {

    final Optional<JSONObject> isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest();
    if (!isReadyToPayJson.isPresent()) {
      return;
    }

    // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
    // OnCompleteListener to be triggered when the result of the call is known.
    IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString());
    Task<Boolean> task = paymentsClient.isReadyToPay(request);
    task.addOnCompleteListener(this,
        new OnCompleteListener<Boolean>() {
          @Override
          public void onComplete(@NonNull Task<Boolean> task) {
            if (task.isSuccessful()) {
              setGooglePayAvailable(task.getResult());
            } else {
              Log.w("isReadyToPay failed", task.getException());
            }
          }
        });
  }




//Step 7 - Create a PaymentDataRequest object.
private static JSONObject getTransactionInfo(String price) throws JSONException {
    JSONObject transactionInfo = new JSONObject();
    transactionInfo.put("totalPrice", price);
    transactionInfo.put("totalPriceStatus", "FINAL");
    transactionInfo.put("countryCode", Constants.COUNTRY_CODE);
    transactionInfo.put("currencyCode", Constants.CURRENCY_CODE);
    transactionInfo.put("checkoutOption", "COMPLETE_IMMEDIATE_PURCHASE");

    return transactionInfo;
  }
  private static JSONObject getMerchantInfo() throws JSONException {
    return new JSONObject().put("merchantName", "Example Merchant");
  }
public static Optional<JSONObject> getPaymentDataRequest(long priceCents) {

    final String price = PaymentsUtil.centsToString(priceCents);

    try {
      JSONObject paymentDataRequest = PaymentsUtil.getBaseRequest();
      paymentDataRequest.put(
          "allowedPaymentMethods", new JSONArray().put(PaymentsUtil.getCardPaymentMethod()));
      paymentDataRequest.put("transactionInfo", PaymentsUtil.getTransactionInfo(price));
      paymentDataRequest.put("merchantInfo", PaymentsUtil.getMerchantInfo());

      /* An optional shipping address requirement is a top-level property of the PaymentDataRequest
      JSON object. */
      paymentDataRequest.put("shippingAddressRequired", true);

      JSONObject shippingAddressParameters = new JSONObject();
      shippingAddressParameters.put("phoneNumberRequired", false);

      JSONArray allowedCountryCodes = new JSONArray(Constants.SHIPPING_SUPPORTED_COUNTRIES);

      shippingAddressParameters.put("allowedCountryCodes", allowedCountryCodes);
      paymentDataRequest.put("shippingAddressParameters", shippingAddressParameters);
      return Optional.of(paymentDataRequest);

    } catch (JSONException e) {
      return Optional.empty();
    }
  }




//Step 8 - Register event handler for user gesture.
 googlePayButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            requestPayment(view);
          }
        });




//Step 9 - Handle the response object.
 public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      // value passed in AutoResolveHelper
      case LOAD_PAYMENT_DATA_REQUEST_CODE:
        switch (resultCode) {

          case Activity.RESULT_OK:
            PaymentData paymentData = PaymentData.getFromIntent(data);
            handlePaymentSuccess(paymentData);
            break;

          case Activity.RESULT_CANCELED:
            // The user cancelled the payment attempt
            break;

          case AutoResolveHelper.RESULT_ERROR:
            Status status = AutoResolveHelper.getStatusFromIntent(data);
            handleError(status.getStatusCode());
            break;
        }

        // Re-enables the Google Pay payment button.
        googlePayButton.setClickable(true);
    }
  }




//Step 10 - Put it all together. 
package com.google.android.gms.samples.wallet.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.samples.wallet.databinding.ActivityCheckoutBinding;
import com.google.android.gms.samples.wallet.util.PaymentsUtil;
import com.google.android.gms.samples.wallet.R;
import com.google.android.gms.samples.wallet.util.Json;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;

import java.util.Locale;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Checkout implementation for the app
 */
public class CheckoutActivity extends AppCompatActivity {

  // Arbitrarily-picked constant integer you define to track a request for payment data activity.
  private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;

  private static final long SHIPPING_COST_CENTS = 90 * PaymentsUtil.CENTS_IN_A_UNIT.longValue();

  // A client for interacting with the Google Pay API.
  private PaymentsClient paymentsClient;

  private ActivityCheckoutBinding layoutBinding;
  private View googlePayButton;

  private JSONArray garmentList;
  private JSONObject selectedGarment;

  /**
   * Initialize the Google Pay API on creation of the activity
   *
   * @see Activity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initializeUi();

    // Set up the mock information for our item in the UI.
    try {
      selectedGarment = fetchRandomGarment();
      displayGarment(selectedGarment);
    } catch (JSONException e) {
      throw new RuntimeException("The list of garments cannot be loaded");
    }

    // Initialize a Google Pay API client for an environment suitable for testing.
    // It's recommended to create the PaymentsClient object inside of the onCreate method.
    paymentsClient = PaymentsUtil.createPaymentsClient(this);
    possiblyShowGooglePayButton();
  }

  /**
   * Handle a resolved activity from the Google Pay payment sheet.
   *
   * @param requestCode Request code originally supplied to AutoResolveHelper in requestPayment().
   * @param resultCode  Result code returned by the Google Pay API.
   * @param data        Intent from the Google Pay API containing payment or error data.
   * @see <a href="https://developer.android.com/training/basics/intents/result">Getting a result
   * from an Activity</a>
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      // value passed in AutoResolveHelper
      case LOAD_PAYMENT_DATA_REQUEST_CODE:
        switch (resultCode) {

          case Activity.RESULT_OK:
            PaymentData paymentData = PaymentData.getFromIntent(data);
            handlePaymentSuccess(paymentData);
            break;

          case Activity.RESULT_CANCELED:
            // The user cancelled the payment attempt
            break;

          case AutoResolveHelper.RESULT_ERROR:
            Status status = AutoResolveHelper.getStatusFromIntent(data);
            handleError(status.getStatusCode());
            break;
        }

        // Re-enables the Google Pay payment button.
        googlePayButton.setClickable(true);
    }
  }

  private void initializeUi() {

    // Use view binding to access the UI elements
    layoutBinding = ActivityCheckoutBinding.inflate(getLayoutInflater());
    setContentView(layoutBinding.getRoot());

    // The Google Pay button is a layout file – take the root view
    googlePayButton = layoutBinding.googlePayButton.getRoot();
    googlePayButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            requestPayment(view);
          }
        });
  }

  private void displayGarment(JSONObject garment) throws JSONException {
    layoutBinding.detailTitle.setText(garment.getString("title"));
    layoutBinding.detailPrice.setText(
        String.format(Locale.getDefault(), "$%.2f", garment.getDouble("price")));

    final String escapedHtmlText = Html.fromHtml(
        garment.getString("description"), Html.FROM_HTML_MODE_COMPACT).toString();
    layoutBinding.detailDescription.setText(Html.fromHtml(
        escapedHtmlText, Html.FROM_HTML_MODE_COMPACT));

    final String imageUri = String.format("@drawable/%s", garment.getString("image"));
    final int imageResource = getResources().getIdentifier(imageUri, null, getPackageName());
    layoutBinding.detailImage.setImageResource(imageResource);
  }

  /**
   * Determine the viewer's ability to pay with a payment method supported by your app and display a
   * Google Pay payment button.
   *
   * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/wallet/
   * PaymentsClient.html#isReadyToPay(com.google.android.gms.wallet.
   * IsReadyToPayRequest)">PaymentsClient#IsReadyToPay</a>
   */
  private void possiblyShowGooglePayButton() {

    final Optional<JSONObject> isReadyToPayJson = PaymentsUtil.getIsReadyToPayRequest();
    if (!isReadyToPayJson.isPresent()) {
      return;
    }

    // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
    // OnCompleteListener to be triggered when the result of the call is known.
    IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString());
    Task<Boolean> task = paymentsClient.isReadyToPay(request);
    task.addOnCompleteListener(this,
        new OnCompleteListener<Boolean>() {
          @Override
          public void onComplete(@NonNull Task<Boolean> task) {
            if (task.isSuccessful()) {
              setGooglePayAvailable(task.getResult());
            } else {
              Log.w("isReadyToPay failed", task.getException());
            }
          }
        });
  }

  /**
   * If isReadyToPay returned {@code true}, show the button and hide the "checking" text. Otherwise,
   * notify the user that Google Pay is not available. Please adjust to fit in with your current
   * user flow. You are not required to explicitly let the user know if isReadyToPay returns {@code
   * false}.
   *
   * @param available isReadyToPay API response.
   */
  private void setGooglePayAvailable(boolean available) {
    if (available) {
      googlePayButton.setVisibility(View.VISIBLE);
    } else {
      Toast.makeText(this, R.string.googlepay_status_unavailable, Toast.LENGTH_LONG).show();
    }
  }

  /**
   * PaymentData response object contains the payment information, as well as any additional
   * requested information, such as billing and shipping address.
   *
   * @param paymentData A response object returned by Google after a payer approves payment.
   * @see <a href="https://developers.google.com/pay/api/android/reference/
   * object#PaymentData">PaymentData</a>
   */
  private void handlePaymentSuccess(PaymentData paymentData) {

    // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
    final String paymentInfo = paymentData.toJson();
    if (paymentInfo == null) {
      return;
    }

    try {
      JSONObject paymentMethodData = new JSONObject(paymentInfo).getJSONObject("paymentMethodData");
      // If the gateway is set to "example", no payment information is returned - instead, the
      // token will only consist of "examplePaymentMethodToken".

      final JSONObject tokenizationData = paymentMethodData.getJSONObject("tokenizationData");
      final String token = tokenizationData.getString("token");
      final JSONObject info = paymentMethodData.getJSONObject("info");
      final String billingName = info.getJSONObject("billingAddress").getString("name");
      Toast.makeText(
          this, getString(R.string.payments_show_name, billingName),
          Toast.LENGTH_LONG).show();

      // Logging token string.
      Log.d("Google Pay token: ", token);

    } catch (JSONException e) {
      throw new RuntimeException("The selected garment cannot be parsed from the list of elements");
    }
  }

  /**
   * At this stage, the user has already seen a popup informing them an error occurred. Normally,
   * only logging is required.
   *
   * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
   *                   WalletConstants.ERROR_CODE_* constants.
   * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/wallet/
   * WalletConstants#constant-summary">Wallet Constants Library</a>
   */
  private void handleError(int statusCode) {
    Log.e("loadPaymentData failed", String.format("Error code: %d", statusCode));
  }

  public void requestPayment(View view) {

    // Disables the button to prevent multiple clicks.
    googlePayButton.setClickable(false);

    // The price provided to the API should include taxes and shipping.
    // This price is not displayed to the user.
    try {
      double garmentPrice = selectedGarment.getDouble("price");
      long garmentPriceCents = Math.round(garmentPrice * PaymentsUtil.CENTS_IN_A_UNIT.longValue());
      long priceCents = garmentPriceCents + SHIPPING_COST_CENTS;

      Optional<JSONObject> paymentDataRequestJson = PaymentsUtil.getPaymentDataRequest(priceCents);
      if (!paymentDataRequestJson.isPresent()) {
        return;
      }

      PaymentDataRequest request =
          PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString());

      // Since loadPaymentData may show the UI asking the user to select a payment method, we use
      // AutoResolveHelper to wait for the user interacting with it. Once completed,
      // onActivityResult will be called with the result.
      if (request != null) {
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            this, LOAD_PAYMENT_DATA_REQUEST_CODE);
      }

    } catch (JSONException e) {
      throw new RuntimeException("The price cannot be deserialized from the JSON object.");
    }
  }

  private JSONObject fetchRandomGarment() {

    // Only load the list of items if it has not been loaded before
    if (garmentList == null) {
      garmentList = Json.readFromResources(this, R.raw.tshirts);
    }

    // Take a random element from the list
    int randomIndex = Math.toIntExact(Math.round(Math.random() * (garmentList.length() - 1)));
    try {
      return garmentList.getJSONObject(randomIndex);
    } catch (JSONException e) {
      throw new RuntimeException("The index specified is out of bounds.");
    }
  }
}




package com.google.android.gms.samples.wallet.util;

import android.app.Activity;

import com.google.android.gms.samples.wallet.Constants;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains helper static methods for dealing with the Payments API.
 *
 * <p>Many of the parameters used in the code are optional and are set here merely to call out their
 * existence. Please consult the documentation to learn more and feel free to remove ones not
 * relevant to your implementation.
 */
public class PaymentsUtil {

  public static final BigDecimal CENTS_IN_A_UNIT = new BigDecimal(100d);

  /**
   * Create a Google Pay API base request object with properties used in all requests.
   *
   * @return Google Pay API base request object.
   * @throws JSONException
   */
  private static JSONObject getBaseRequest() throws JSONException {
    return new JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0);
  }

  /**
   * Creates an instance of {@link PaymentsClient} for use in an {@link Activity} using the
   * environment and theme set in {@link Constants}.
   *
   * @param activity is the caller's activity.
   */
  public static PaymentsClient createPaymentsClient(Activity activity) {
    Wallet.WalletOptions walletOptions =
        new Wallet.WalletOptions.Builder().setEnvironment(Constants.PAYMENTS_ENVIRONMENT).build();
    return Wallet.getPaymentsClient(activity, walletOptions);
  }

  /**
   * Gateway Integration: Identify your gateway and your app's gateway merchant identifier.
   *
   * <p>The Google Pay API response will return an encrypted payment method capable of being charged
   * by a supported gateway after payer authorization.
   *
   * <p>TODO: Check with your gateway on the parameters to pass and modify them in Constants.java.
   *
   * @return Payment data tokenization for the CARD payment method.
   * @throws JSONException
   * @see <a href=
   * "https://developers.google.com/pay/api/android/reference/object#PaymentMethodTokenizationSpecification">PaymentMethodTokenizationSpecification</a>
   */
  private static JSONObject getGatewayTokenizationSpecification() throws JSONException {
    return new JSONObject() {{
      put("type", "PAYMENT_GATEWAY");
      put("parameters", new JSONObject() {{
        put("gateway", "example");
        put("gatewayMerchantId", "exampleGatewayMerchantId");
      }});
    }};
  }

  /**
   * {@code DIRECT} Integration: Decrypt a response directly on your servers. This configuration has
   * additional data security requirements from Google and additional PCI DSS compliance complexity.
   *
   * <p>Please refer to the documentation for more information about {@code DIRECT} integration. The
   * type of integration you use depends on your payment processor.
   *
   * @return Payment data tokenization for the CARD payment method.
   * @throws JSONException
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#PaymentMethodTokenizationSpecification">PaymentMethodTokenizationSpecification</a>
   */
  private static JSONObject getDirectTokenizationSpecification()
      throws JSONException, RuntimeException {
    if (Constants.DIRECT_TOKENIZATION_PARAMETERS.isEmpty()
        || Constants.DIRECT_TOKENIZATION_PUBLIC_KEY.isEmpty()
        || Constants.DIRECT_TOKENIZATION_PUBLIC_KEY == null
        || Constants.DIRECT_TOKENIZATION_PUBLIC_KEY == "REPLACE_ME") {
      throw new RuntimeException(
          "Please edit the Constants.java file to add protocol version & public key.");
    }
    JSONObject tokenizationSpecification = new JSONObject();

    tokenizationSpecification.put("type", "DIRECT");
    JSONObject parameters = new JSONObject(Constants.DIRECT_TOKENIZATION_PARAMETERS);
    tokenizationSpecification.put("parameters", parameters);

    return tokenizationSpecification;
  }

  /**
   * Card networks supported by your app and your gateway.
   *
   * <p>TODO: Confirm card networks supported by your app and gateway & update in Constants.java.
   *
   * @return Allowed card networks
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#CardParameters">CardParameters</a>
   */
  private static JSONArray getAllowedCardNetworks() {
    return new JSONArray(Constants.SUPPORTED_NETWORKS);
  }

  /**
   * Card authentication methods supported by your app and your gateway.
   *
   * <p>TODO: Confirm your processor supports Android device tokens on your supported card networks
   * and make updates in Constants.java.
   *
   * @return Allowed card authentication methods.
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#CardParameters">CardParameters</a>
   */
  private static JSONArray getAllowedCardAuthMethods() {
    return new JSONArray(Constants.SUPPORTED_METHODS);
  }

  /**
   * Describe your app's support for the CARD payment method.
   *
   * <p>The provided properties are applicable to both an IsReadyToPayRequest and a
   * PaymentDataRequest.
   *
   * @return A CARD PaymentMethod object describing accepted cards.
   * @throws JSONException
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#PaymentMethod">PaymentMethod</a>
   */
  private static JSONObject getBaseCardPaymentMethod() throws JSONException {
    JSONObject cardPaymentMethod = new JSONObject();
    cardPaymentMethod.put("type", "CARD");

    JSONObject parameters = new JSONObject();
    parameters.put("allowedAuthMethods", getAllowedCardAuthMethods());
    parameters.put("allowedCardNetworks", getAllowedCardNetworks());
    // Optionally, you can add billing address/phone number associated with a CARD payment method.
    parameters.put("billingAddressRequired", true);

    JSONObject billingAddressParameters = new JSONObject();
    billingAddressParameters.put("format", "FULL");

    parameters.put("billingAddressParameters", billingAddressParameters);

    cardPaymentMethod.put("parameters", parameters);

    return cardPaymentMethod;
  }

  /**
   * Describe the expected returned payment data for the CARD payment method
   *
   * @return A CARD PaymentMethod describing accepted cards and optional fields.
   * @throws JSONException
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#PaymentMethod">PaymentMethod</a>
   */
  private static JSONObject getCardPaymentMethod() throws JSONException {
    JSONObject cardPaymentMethod = getBaseCardPaymentMethod();
    cardPaymentMethod.put("tokenizationSpecification", getGatewayTokenizationSpecification());

    return cardPaymentMethod;
  }

  /**
   * An object describing accepted forms of payment by your app, used to determine a viewer's
   * readiness to pay.
   *
   * @return API version and payment methods supported by the app.
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#IsReadyToPayRequest">IsReadyToPayRequest</a>
   */
  public static Optional<JSONObject> getIsReadyToPayRequest() {
    try {
      JSONObject isReadyToPayRequest = getBaseRequest();
      isReadyToPayRequest.put(
          "allowedPaymentMethods", new JSONArray().put(getBaseCardPaymentMethod()));

      return Optional.of(isReadyToPayRequest);

    } catch (JSONException e) {
      return Optional.empty();
    }
  }

  /**
   * Provide Google Pay API with a payment amount, currency, and amount status.
   *
   * @return information about the requested payment.
   * @throws JSONException
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#TransactionInfo">TransactionInfo</a>
   */
  private static JSONObject getTransactionInfo(String price) throws JSONException {
    JSONObject transactionInfo = new JSONObject();
    transactionInfo.put("totalPrice", price);
    transactionInfo.put("totalPriceStatus", "FINAL");
    transactionInfo.put("countryCode", Constants.COUNTRY_CODE);
    transactionInfo.put("currencyCode", Constants.CURRENCY_CODE);
    transactionInfo.put("checkoutOption", "COMPLETE_IMMEDIATE_PURCHASE");

    return transactionInfo;
  }

  /**
   * Information about the merchant requesting payment information
   *
   * @return Information about the merchant.
   * @throws JSONException
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#MerchantInfo">MerchantInfo</a>
   */
  private static JSONObject getMerchantInfo() throws JSONException {
    return new JSONObject().put("merchantName", "Example Merchant");
  }

  /**
   * An object describing information requested in a Google Pay payment sheet
   *
   * @return Payment data expected by your app.
   * @see <a
   * href="https://developers.google.com/pay/api/android/reference/object#PaymentDataRequest">PaymentDataRequest</a>
   */
  public static Optional<JSONObject> getPaymentDataRequest(long priceCents) {

    final String price = PaymentsUtil.centsToString(priceCents);

    try {
      JSONObject paymentDataRequest = PaymentsUtil.getBaseRequest();
      paymentDataRequest.put(
          "allowedPaymentMethods", new JSONArray().put(PaymentsUtil.getCardPaymentMethod()));
      paymentDataRequest.put("transactionInfo", PaymentsUtil.getTransactionInfo(price));
      paymentDataRequest.put("merchantInfo", PaymentsUtil.getMerchantInfo());

      /* An optional shipping address requirement is a top-level property of the PaymentDataRequest
      JSON object. */
      paymentDataRequest.put("shippingAddressRequired", true);

      JSONObject shippingAddressParameters = new JSONObject();
      shippingAddressParameters.put("phoneNumberRequired", false);

      JSONArray allowedCountryCodes = new JSONArray(Constants.SHIPPING_SUPPORTED_COUNTRIES);

      shippingAddressParameters.put("allowedCountryCodes", allowedCountryCodes);
      paymentDataRequest.put("shippingAddressParameters", shippingAddressParameters);
      return Optional.of(paymentDataRequest);

    } catch (JSONException e) {
      return Optional.empty();
    }
  }

  /**
   * Converts cents to a string format accepted by {@link PaymentsUtil#getPaymentDataRequest}.
   *
   * @param cents value of the price in cents.
   */
  public static String centsToString(long cents) {
    return new BigDecimal(cents)
        .divide(CENTS_IN_A_UNIT, RoundingMode.HALF_EVEN)
        .setScale(2, RoundingMode.HALF_EVEN)
        .toString();
  }
}