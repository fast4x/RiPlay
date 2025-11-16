name: 🐛 Bug Report
description: Create a report to help us fix an issue.
title: "[BUG]: "
labels: ["bug"]
assignees: ""

body:
  - type: markdown
    attributes:
      value: |
        Thank you for taking time to file a bug report! Please fill out the fields below as much as you can.

  - type: textarea
    id: bug_description
    attributes:
      label: 📝 Bug Description
      description: A clear and concise description of what the bug is.
      placeholder: e.g., The play/pause button doesn't respond when the screen is off.
    validations:
      required: true

  - type: textarea
    id: steps_to_reproduce
    attributes:
      label: 🔄 Steps to Reproduce the Bug
      description: Describe the steps to reproduce the behavior.
      placeholder: |
        1. Go to '...'
        2. Click on '....'
        3. Scroll down to '....'
        4. See the error
    validations:
      required: true

  - type: textarea
    id: expected_behavior
    attributes:
      label: 🤔 Expected Behavior
      description: Describe what you expected to see or happen.
      placeholder: e.g., The song should pause/resume normally.
    validations:
      required: true

  - type: textarea
    id: actual_behavior
    attributes:
      label: ❌ Actual Behavior
      description: Describe what happened instead. Include exact error messages.
      placeholder: e.g., The button is unresponsive, and I see the following error in the logs:...
    validations:
      required: true

  - type: textarea
    id: screenshots_and_logs
    attributes:
      label: 📸 Screenshots and Logs
      description: |
        If applicable, add screenshots by dragging and dropping them into the issue.
        For logs, please follow the guide in the FAQ: "How do I get log files in case of a crash?" and paste them below.
      placeholder: |
        Paste your logs here, enclosed in triple backticks (```).
        ```
        <!-- PASTE LOGS HERE -->
        ```

  - type: input
    id: app_version
    attributes:
      label: App Version
      description: The version of the app you are using.
      placeholder: e.g., 0.6.46
    validations:
      required: true

  - type: dropdown
    id: android_version
    attributes:
      label: Android Version
      description: The version of Android on your device.
      options:
        - "14 (Upside Down Cake)"
        - "13 (Tiramisu)"
        - "12L"
        - "12 (Snow Cone)"
        - "11 (Red Velvet Cake)"
        - "10 (Q)"
        - "Other (please specify in Additional Context)"
    validations:
      required: true

  - type: input
    id: device_model
    attributes:
      label: Device Model
      description: The model of your device.
      placeholder: e.g., Samsung Galaxy S21
    validations:
      required: true

  - type: input
    id: rom_version
    attributes:
      label: ROM Version (if custom)
      description: If you are using a custom ROM, please specify which one.
      placeholder: e.g., LineageOS 20

  - type: textarea
    id: additional_context
    attributes:
      label: 💡 Additional Context
      description: Add any other context about the problem here.
      placeholder: Any other information that might help us understand the issue.
