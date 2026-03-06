# Monster Timer Privacy Policy

**Last updated:** February 2026

Monster Timer ("the App") is a parental control tool designed to help parents and guardians manage their children's YouTube Shorts screen time. This Privacy Policy explains how the App handles data and permissions.

## 1. No Data Collection
Monster Timer is designed with privacy-first principles. The App **does not** collect, transmit, share, or sell any personal data, browsing history, screen content, or usage data to any external servers or third parties.

## 2. Local Storage Only
All settings configured by the parent (e.g., PIN, timer duration, selected media) and all tracking data (e.g., usage statistics) are stored locally on the device using EncryptedSharedPreferences. This data cannot be accessed by external parties.

## 3. Accessibility Service Permissions
The App requires the Android Accessibility Service permission (`BIND_ACCESSIBILITY_SERVICE`) to function.
- **Why it's needed:** To detect when the YouTube app is open and specifically when the "Shorts" section is being viewed or navigated to.
- **How it's used:** The App reads the view hierarchy and content descriptions solely to identify the presence of Shorts indicators. It does NOT log keypresses, record passwords, or intercept personal communications.
- **Data handling:** The view data analyzed by the Accessibility Service is processed in real-time within the device's memory and is instantly discarded. No content is saved or transmitted.

## 4. Overlay Permissions
The App requires "Display Over Other Apps" permission to show the full-screen "monster" overlay when the configured screen time limit is reached.

## 5. Media Permissions
If the parent chooses to use custom images or videos for the overlay, the App will request permission to read local media files. These files remain on your device.

## 6. Children's Privacy (COPPA Compliance)
The App is designed as a tool for parents, not for children. The App does not knowingly collect personal information from children under 13 (or any other age). If a parent decides to install this tool on a child's device, the parent acknowledges full responsibility, and the App still fundamentally collects no data.

## 7. Changes to this Policy
We may update this Privacy Policy from time to time. Any changes will be reflected in an updated version of the App and this document.

## 8. Contact
As this is an open-source or locally distributed tool, please refer to the project's repository issues page for any questions or concerns regarding this policy.
