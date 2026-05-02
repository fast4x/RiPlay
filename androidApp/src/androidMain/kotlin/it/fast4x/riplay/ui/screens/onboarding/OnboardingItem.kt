package it.fast4x.riplay.ui.screens.onboarding

data class OnboardingItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val status: PermissionStatus,
    val onRequest: () -> Unit
)

data class OnboardingSection (
    val title: String,
)

enum class PermissionStatus {
    GRANTED,
    NOT_REQUESTED,
    DENIED,
    PERMANENTLY_DENIED
}