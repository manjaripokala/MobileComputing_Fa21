import React from "react";
import { createAppContainer } from "react-navigation";
import { createStackNavigator } from "react-navigation-stack";
import MediaCard from "./src/components/MediaCard";
import Payment from "./src/components/Payment";

class App extends React.Component {
  render() {
    return <AppContainer />;
  }
}
const AppNavigator = createStackNavigator({
  Home: {
    screen: MediaCard,
  },
  Payment: {
    screen: Payment,
  },
});

export default AppContainer = createAppContainer(AppNavigator);
