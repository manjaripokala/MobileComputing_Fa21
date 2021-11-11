import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  Image,
  StatusBar,
  Component,
  TouchableHighlight,
  Button,
  Alert,
} from "react-native";
import { CreditCardInput } from "react-native-credit-card-input";
import { Secret_key, Publishable_Key } from "../../stripeKeys";
import * as LocalAuthentication from "expo-local-authentication";
import styles from "./styles";

function getCreditCardToken(creditCardData) {
  const card = {
    "card[number]": creditCardData.values.number.replace(/ /g, ""),
    "card[exp_month]": creditCardData.values.expiry.split("/")[0],
    "card[exp_year]": creditCardData.values.expiry.split("/")[1],
    "card[cvc]": creditCardData.values.cvc,
  };

  return fetch("https://api.stripe.com/v1/tokens", {
    headers: {
      Accept: "application/json",
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: `Bearer ${Publishable_Key}`,
    },
    method: "post",
    body: Object.keys(card)
      .map((key) => key + "=" + card[key])
      .join("&"),
  })
    .then((response) => response.json())
    .catch((error) => console.log(error));
}

export default function Payment({ navigation }) {
  const [CardInput, setCardInput] = useState({});
  const currency = "USD";
  var cardToken = null;

  const onSubmit = async () => {
    if (CardInput.valid == false || typeof CardInput.valid == "undefined") {
      alert("Invalid Credit Card");
      return false;
    }

    let creditCardToken;
    creditCardToken = await getCreditCardToken(CardInput);
    if (creditCardToken.error) {
      console.log(creditCardToken.error);
      alert("Unable to process card: creditCardToken error");
      return;
    }

    try {
      await handleBiometricAuth();
      await passcodeAuth(creditCardToken);
    } catch (e) {
      console.log("e", e);
      alert("Invalid passcode, please try again");
      return;
    }

    //Initiates a request to stripe with credit card token
    let payment_data = await initiatePayment();
    if (payment_data.status == "succeeded") {
      alert("Payment Success");
    } else {
      alert("Payment failed");
    }
  };

  //This function returns passcode authenticated promise
  function passcodeAuth(creditCardToken) {
    return new Promise((resolve) => {
      console.log("Credit card token\n", creditCardToken);
      cardToken = creditCardToken.id;
      setTimeout(() => {
        resolve({ status: true });
      }, 4000);
    });
  }

  const initiatePayment = async () => {
    const data = {
      amount: 50,
      currency: currency,
      source: cardToken,
      description: "Course Project Test",
    };

    return fetch("https://api.stripe.com/v1/charges", {
      headers: {
        Accept: "application/json",
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Bearer ${Secret_key}`,
      },
      method: "post",
      body: Object.keys(data) //format body
        .map((key) => key + "=" + data[key])
        .join("&"),
    }).then((response) => response.json());
  };

  const _onChange = (data) => {
    setCardInput(data);
  };

  const fallbackToPasscodeAuth = async () => {
    await LocalAuthentication.authenticateAsync({
      //disableDeviceFallback is false by default, fallbacks to passcode
      promptMessage: "Authenticate using phone auth",
    });
  };

  const alertComponent = (title, message, buttonText, buttonOnPressFunc) => {
    return Alert.alert(title, message, [
      {
        text: buttonText,
        onPress: buttonOnPressFunc,
      },
    ]);
  };

  const handleBiometricAuth = async () => {
    // Check if biometrics is supported by device
    const isBiometricsSupported = await LocalAuthentication.hasHardwareAsync();
    if (!isBiometricsSupported)
      return alertComponent(
        "Biometric Authentication not supported",
        "Please enter your passcode",
        "OK",
        () => fallbackToPasscodeAuth()
      );
    console.log({ isBiometricsSupported });

    let authTypesAvailable;
    if (isBiometricsSupported)
      authTypesAvailable =
        await LocalAuthentication.supportedAuthenticationTypesAsync();
    console.log({ authTypesAvailable });

    const areBioEnrolled = await LocalAuthentication.isEnrolledAsync(); //if bio/facial data saved
    if (!areBioEnrolled)
      return alertComponent(
        "Biometric record not found",
        "Please login with your passcode",
        "OK",
        () => fallbackToPasscodeAuth()
      );
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#2471A3" />
      <Text style={styles.text}>
        {"\n"}Please enter your card details{"\n"}
      </Text>
      <CreditCardInput
        inputContainerStyle={styles.inputContainerStyle}
        inputStyle={styles.inputStyle}
        labelStyle={styles.labelStyle}
        placeholderColor="#E7AC9F"
        onChange={_onChange}
      />

      <TouchableOpacity onPress={onSubmit} style={styles.button}>
        <Text style={styles.buttonText}>Pay Now</Text>
      </TouchableOpacity>
    </View>
  );
}

// References:
// 1. https://github.com/mdrajibsk8/Stripe-Payment-Gateway
// 2. https://github.com/ejirocodes/React-Native-Local-Authentication-using-Biometrics
