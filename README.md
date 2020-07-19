## SETTING UP AND OPERATING THE MOBILE APPLICATION

*   Install the mobile application with the APK file (app-release.apk) in [this](https://github.com/RISC-NYUAD/spaigh/blob/master/app/release/app-release.apk) github repository. Download the file as shown below into your smartphone and then install it after downloading.

*   Make sure that the smartphone and the server are connected to the same network.

*   On opening the application, after installation, type **<code>[server_ip_address/phone_1](server_ip_address/phone_1)</code></strong>  inside the text box hinting server address. Note that one has to replace <strong><code>server_ip_address</code></strong> in the URL above with the IP address that we got when setting up the server. See image description below.

*   Now click on the **Start Service** button to start the application and watch out for the toasts that pop up.

*   One will be prompted to grant some permissions to the application. Click accept. The application will not run properly without these permissions being granted.

*   After granting the permissions, the application should say **SERVICE IS RUNNING** at the top of the and a **foreground notification** should show up at the top of the screen.

*   The application is now fully set and one can head over to the browser and type URL <code>[http://server_ip_address/phone_1](http://server_ip_address/phone_1)</code> to check the current state of the phone at any period. Again, replace <strong><code>server_ip_address</code></strong> with the IP address used for the server in the prior steps.

*   Also, the application User Interface allows one to see the state of the device in real-time.

*   Note that one can close the application and the application will run as a notification in the foreground.

*   To stop the application, open the application, and click <strong>stop service.</strong>  