name: ✨ Feature Request
description: Suggest an idea for this app.
title: "[FEATURE]: "
labels: ["enhancement"]
assignees: ""

body:
  - type: markdown
    attributes:
      value: |
        Thank you for taking time to suggest a new feature! Please fill out the fields below.

  - type: textarea
    id: feature_description
    attributes:
      label: 🚀 Feature Description
      description: A clear and concise description of the feature you'd like.
      placeholder: e.g., A dark mode toggle in the settings menu.
    validations:
      required: true

  - type: textarea
    id: problem_motivation
    attributes:
      label: 💭 Problem/Motivation
      description: What problem are you trying to solve with this feature? Why would this feature be useful to you or other users?
      placeholder: e.g., "I'm always frustrated when I have to manually switch between my downloaded and online playlists. A unified queue would solve this."
    validations:
      required: true

  - type: textarea
    id: proposed_solution
    attributes:
      label: 💡 Proposed Solution
      description: Describe how you would like this feature to be implemented. You can include sketches, mockups, or examples of other apps that already do this.
      placeholder: e.g., Add a new "Unified Queue" option in the navigation drawer that combines songs from both local and online sources.
    validations:
      required: true

  - type: textarea
    id: alternatives
    attributes:
      label: 🔄 Alternatives Considered
      description: Have you considered any alternative solutions or features? Describe why your proposed solution is the best one.
      placeholder: e.g., I considered using a third-party app to manage playlists, but having it integrated into the app would provide a much smoother experience.

  - type: textarea
    id: additional_context
    attributes:
      label: 📋 Additional Context
      description: Add any other information, screenshots, or examples that can help us understand the request.
      placeholder: Any other information that might help us understand the request.
