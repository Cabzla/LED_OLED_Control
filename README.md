# ESP32 LED and Display Controller Android App

## Overview

This project provides an Android application to control LEDs and an OLED display connected to an ESP32. The application allows users to manage and save LED lighting programs and display messages on an OLED screen, simulating a train station display.

## Features

### LED Control
- **Individual LED Control**: Turn on/off individual LEDs (Red, Green, Yellow) with a specified duration.
- **Program Management**: Save and load LED control programs to/from shared preferences.
- **Expandable CardView**: UI includes an expandable card for LED control settings, enhancing user experience.

### OLED Display Control
- **Time and Train Information**: Update and display the current time, train number, train name, track number, information text, and route information.
- **Scrolling Text**: Display scrolling text for information and route details, similar to train station displays.
- **Program Management**: Save and load display control programs to/from shared preferences.
- **Expandable CardView**: UI includes an expandable card for display control settings, enhancing user experience.

### General Features
- **Network Communication**: Uses HTTP requests to communicate with the ESP32, sending commands to control LEDs and update the display.
- **Modern UI**: Implements a dark theme with Material Design components, providing an aesthetically pleasing user interface.

## Setup

### ESP32 Firmware

The firmware for the ESP32 can be found in the following repository: [ESP32 LED and Display Controller Firmware](https://github.com/Cabzla/LED_OLED_ESP32)

### Android App Setup

### Android App Setup

1. **Clone the Repository**:
   - Clone this repository to your local machine.

2. **Open the Project**:
   - Open the project in Android Studio.

3. **Update ESP32 IP Address**:
   - In `MainFragment.kt` and `DisplayFragment.kt`, update the `esp32Ip` variable with the static IP address of your ESP32.

4. **Run the App**:
   - Connect your Android device or use an emulator.
   - Build and run the application.

## Usage

### LED Control
1. Open the app and navigate to the LED Control tab.
2. Expand the "LED Steuerung" card to access LED settings.
3. Select the LEDs to control and enter the duration in seconds.
4. Click "LED(s) Ein" to turn on the LEDs or "LED(s) Aus" to turn them off.
5. Save your settings as a program for later use.

### OLED Display Control
1. Navigate to the Display Control tab.
2. Expand the "Display Steuerung" card to access display settings.
3. Enter the time, train number, train name, track number, information text, and route information.
4. Click "Anzeige aktualisieren" to update the display.
5. Save your settings as a program for later use.

### Program Management
- **Save Programs**: Save your LED or display settings as programs for easy access.
- **Load Programs**: Load and apply saved programs from the list.
- **Program Details**: View detailed information about saved programs and start them directly from the details view.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgements

- This project uses libraries from Adafruit for controlling the OLED display.
- The app's UI is designed using Material Components for Android.

## Contact

For any inquiries or support, please contact Michael Krüger at [kruegermichael90@gmail.com].

