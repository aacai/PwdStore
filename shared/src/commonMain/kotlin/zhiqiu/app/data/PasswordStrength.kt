package zhiqiu.app.data

enum class PasswordStrength {
    TOO_SHORT,
    WEAK,
    MEDIUM,
    STRONG,
}

object PasswordStrengthMeter {
    fun score(password: String): PasswordStrength {
        if (password.length < 4) return PasswordStrength.TOO_SHORT
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }
}
