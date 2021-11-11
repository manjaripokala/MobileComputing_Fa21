import React from "react";
import { StyleSheet, StatusBar as RnStatusBar } from "react-native";

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: RnStatusBar.currentHeight,
    paddingLeft: 20,
    paddingRight: 20,
    backgroundColor: "#fff",
  },
  ImgStyle: {
    width: "100%",
    height: 200,
    resizeMode: "contain",
    borderRadius: 8,
  },
  button: {
    backgroundColor: "#E6BC20",
    width: 100,
    height: 45,
    alignSelf: "center",
    justifyContent: "center",
    alignItems: "center",
    marginTop: 20,
    borderRadius: 15,
  },
  buttonText: {
    fontSize: 15,
    color: "#4D2820",
    fontWeight: "bold",
    textTransform: "uppercase",
  },
  text: {
    fontSize: 16,
    color: "#4D2820",
    fontWeight: "bold",
    alignSelf: "center",
  },
  inputContainerStyle: {
    backgroundColor: "#fff",
    borderRadius: 5,
  },
  inputStyle: {
    backgroundColor: "#F2D464",
    paddingLeft: 15,
    borderRadius: 5,
    color: "#4D2820",
  },
  labelStyle: {
    color: "#4D2820",
    marginBottom: 5,
    fontSize: 12,
  },
});

export default styles;
