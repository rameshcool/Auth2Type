package com.example.auth2type



import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log.d
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.example.auth2type.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.lang.Exception
import java.util.logging.Logger


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var account: Auth0
    private var appJustLaunched = true
    private var userIsAuthenticated = false
    private var userLoggedIn = false

    private var wifiList: ListView? = null
    private var wifiManager: WifiManager? = null
    private val MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1

    var receiverWifi: WiFiReceiver? = null

    private var user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasNetworkAvailable(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        account = Auth0(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )

        binding.buttonLogin.setOnClickListener {
            if (!hasNetworkAvailable(this@MainActivity)) {
                !userLoggedIn
                binding.buttonLogin.isEnabled = false
                showSnackBar(getString(R.string.wifi_switch_off))
            } else {
                binding.buttonLogin.isEnabled
                login()
            }
        }
        binding.buttonLogout.setOnClickListener {
            logout()
        }

    }

    private fun login() {
        WebAuthProvider
            .login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(this, object : Callback<Credentials, AuthenticationException> {

                override fun onFailure(exception: AuthenticationException) {
                    // The user either pressed the “Cancel” button
                    // on the Universal Login screen or something
                    // unusual happened.
                    showSnackBar(getString(R.string.login_failure_message))

                }

                override fun onSuccess(credentials: Credentials) {
                    // The user successfully logged in.
                    val idToken = credentials.idToken
                    user = User(idToken)
                    userIsAuthenticated = true
                    showSnackBar(getString(R.string.login_success_message, user.name))
                    updateUI()
                }

            })
    }

    private fun logout() {
        WebAuthProvider
            .logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(this, object : Callback<Void?, AuthenticationException> {

                override fun onFailure(exception: AuthenticationException) {
                    // For some reason, logout failed.
                    showSnackBar(getString(R.string.general_failure_with_exception_code,
                        exception.getCode()))
                }

                override fun onSuccess(payload: Void?) {
                    // The user successfully logged out.
                    user = User()
                    userIsAuthenticated = false
                    updateUI()
                }

            })
    }

    private fun updateUI() {

        if (appJustLaunched) {
            binding.textviewTitle.text = getString(R.string.initial_title)
            appJustLaunched = false
        } else {
            binding.textviewTitle.text = if (userIsAuthenticated) {
                getString(R.string.logged_in_title)
            } else {
                getString(R.string.logged_out_title)
            }
        }

        binding.buttonLogin.isEnabled = !userIsAuthenticated
        binding.buttonLogout.isEnabled = userIsAuthenticated

        binding.wifiList.isVisible = userIsAuthenticated
        binding.scanButton.isVisible = userIsAuthenticated

        wifiScan()

    }

    private fun showSnackBar(text: String) {
        Snackbar
            .make(
                binding.root,
                text,
                Snackbar.LENGTH_LONG
            ).show()
    }

    private fun hasNetworkAvailable(context: Context): Boolean {
        val service = Context.CONNECTIVITY_SERVICE
        val manager = context.getSystemService(service) as ConnectivityManager?
        val network = manager?.activeNetworkInfo
        //Logger.d(classTag, "hasNetworkAvailable: ${(network != null)}")
        return (network != null)
    }
    private fun wifiScan() {
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager!!.isWifiEnabled) {
            Toast.makeText(this, "Turning Wifi ON...", Toast.LENGTH_LONG).show()
            wifiManager!!.isWifiEnabled
        }
        if (ActivityCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                MY_PERMISSIONS_ACCESS_COARSE_LOCATION
            )

        }

        binding.scanButton.setOnClickListener {
            wifiManager!!.startScan()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        //receiverWifi = WiFiReceiver(wifiManager!!, wifiList!!)
        receiverWifi = wifiManager?.let { wifiList?.let { it1 -> WiFiReceiver(it, it1) } }
        val intentFilter = IntentFilter()
        intentFilter.addAction(
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
        )
        try {
            registerReceiver(receiverWifi, intentFilter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        wifiScan()

    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(receiverWifi)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSIONS_ACCESS_COARSE_LOCATION
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()

        } else {
            Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_LONG).show()
        }
    }
}