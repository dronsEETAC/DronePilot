# DronePilot
DronePilot is a mobile application developed in Kotlin, designed with the purpose of enriching the Drone Engineering Ecosystem. Its purpose is to demonstrate the perfect fit of Native Android in this ecosystem, through the implementation of two functionalities that allow to control the drone. 
It includes:
    - Control of the drone through gesture recognition: A model of 9 gestures has been created, which when recognised by the application, sends the drone to the address associated with that gesture.
    ![Gestures assigned to drone movements] (/app/src/main/res/drawable-v24/gesture_commands.png)
    - Control of the drone through the movements of the mobile device, using the gyroscope: The gyroscope is used to recognise the movements of the mobile device and pilot the drone according to those movements.
    ![Movements of the device to move the drone] (/app/src/main/res/drawable-v24/com_mov.png)

# Demo
This video is a  demo of the functionalities of the Mobile APP.
[Demo DronePilot](https://www.youtube.com/watch?v=EVA7vC1wVa4)

The video explains the main functionalities of the application, details all the components it contains, such as buttons, toolbar, iconographic details, as well as the different screens that compose it. Finally, a demonstration of how the application works in a simulation environment using Mission Planner.

# Contribution and installation
In case you want to contribute to this project, all you have to do is install Android Studio on your computer and clone this repository. Once downloaded, open the project in Android Studio and to run it you must generate a mobile device in the emulator and press the run button. It is recommended to install the application on a physical device to be able to access the full functionality of the application, as in an emulator you will not be able to test any of the functionalities. To connect a mobile device with Android Studio, first of all the mobile must have Android operating system, and you must enable the developer options on it, each mobile has a different configuration in this regard. Once the developer options are enabled, you only have to connect the mobile to the computer using its cable and select the physical mobile device when running the application.