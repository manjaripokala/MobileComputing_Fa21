import React from "react";
import {
  StyleSheet,
  View,
  Text,
  Button,
  ScrollView,
  Image,
  TouchableOpacity,
} from "react-native";

import { createStackNavigator } from "react-navigation-stack";
import styles from "./styles";

class MediaCard extends React.Component {
  render() {
    return (
      <ScrollView style={styles.container}>
        <View>
          <Image
            style={styles.ImgStyle}
            source={{
              uri: "https://m.media-amazon.com/images/I/71Oe3j7ISbL._AC_SL1500_.jpg",
            }}
          />
          <Text style={styles.text}>
            Hi! I am a cute little Mickey Mouse {"\n"}I am a Medium Size of 17in
          </Text>
          <TouchableOpacity
            style={styles.button}
            onPress={() => this.props.navigation.navigate("Payment")}
          >
            <Text style={styles.buttonText}>Buy Me</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    );
  }
}

export default MediaCard;
