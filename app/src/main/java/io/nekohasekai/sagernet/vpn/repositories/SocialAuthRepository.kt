package io.nekohasekai.sagernet.vpn.repositories

import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth

object SocialAuthRepository {
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var facebookLoginManager: LoginManager
    lateinit var firebaseAuth: FirebaseAuth
}