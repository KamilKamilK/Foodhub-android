package pl.foodhub.pos.core.auth

sealed interface SessionState {
    /** Startup, before the token store has been read. */
    data object Unknown : SessionState

    /** No usable session -- the PIN login screen is shown. */
    data object LoggedOut : SessionState

    /** A token pair is present; the terminal can talk to the API. */
    data object Authenticated : SessionState
}
